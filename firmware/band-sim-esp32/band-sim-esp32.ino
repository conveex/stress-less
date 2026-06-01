#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include "MAX30100_PulseOximeter.h"
#include <LiquidCrystal.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>

#include "arduino_secrets.h"

// ============================================================
// STRESS-LESS BAND SIM ESP32 — Bloque 11
// MAX30100 + GSR simulado por potenciómetro + MQTT
// ============================================================

// Periodos
#define SENSOR_REPORTING_PERIOD_MS 1000
#define LCD_UPDATE_MS 5000
#define MQTT_PUBLISH_MS 5000
#define MQTT_RECONNECT_MS 5000

// LCD paralelo: LiquidCrystal(rs, enable, d4, d5, d6, d7)
LiquidCrystal lcd(13, 12, 14, 27, 26, 25);

// MAX30100
PulseOximeter pox;

// MPU6050 en segundo bus I2C
#define MPU_SDA 18
#define MPU_SCL 19
#define MPU_I2C_ADDR 0x68

TwoWire I2C_MPU = TwoWire(1);
Adafruit_MPU6050 mpu;
bool mpuOk = false;

float lastAccelMagnitude = 0.0;

// GSR simulado
#define PIN_GSR 34

// MQTT
WiFiClientSecure secureClient;
PubSubClient mqttClient(secureClient);

String topicBiometrics;

// Variables de sensores
float bpm = 0;
float spo2 = 0;
float gsr = 500;
float movement = 0.08;
int battery = 85;

uint32_t tsLastSensorReport = 0;
uint32_t tsLastLCD = 0;
uint32_t tsLastMqttPublish = 0;
uint32_t tsLastMqttReconnect = 0;

bool maxSensorOk = false;

// ============================================================
// Utilidades
// ============================================================

String nowIsoPlaceholder() {
  // Sin NTP por ahora. El backend puede usar received_at si necesita.
  return "1970-01-01T00:00:00Z";
}

void printLine() {
  Serial.println("--------------------------------------------------");
}

float clampFloat(float value, float minValue, float maxValue) {
  if (value < minValue) return minValue;
  if (value > maxValue) return maxValue;
  return value;
}

int clampInt(int value, int minValue, int maxValue) {
  if (value < minValue) return minValue;
  if (value > maxValue) return maxValue;
  return value;
}

void onBeatDetected() {
  Serial.println("[MAX30100] Latido detectado");
}

// ============================================================
// WiFi
// ============================================================

void connectWiFi() {
  printLine();
  Serial.printf("[WIFI] Connecting to SSID: %s\n", WIFI_SSID);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int attempts = 0;

  while (WiFi.status() != WL_CONNECTED && attempts < 40) {
    delay(500);
    Serial.print(".");
    attempts++;

    // Importante: mantener MAX30100 actualizado incluso esperando.
    if (maxSensorOk) {
      pox.update();
    }
  }

  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("[WIFI] Connected");
    Serial.print("[WIFI] IP: ");
    Serial.println(WiFi.localIP());

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("WiFi conectado");
    lcd.setCursor(0, 1);
    lcd.print(WiFi.localIP());
  } else {
    Serial.println("[WIFI] Failed to connect");

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("WiFi error");
    lcd.setCursor(0, 1);
    lcd.print("Revisar config");
  }

  printLine();
}

// ============================================================
// MQTT
// ============================================================

void configureTopics() {
  topicBiometrics = "stressless/band/" + String(BAND_ID) + "/biometrics";
}

void connectMQTT() {
  if (mqttClient.connected()) {
    return;
  }

  uint32_t now = millis();

  if (now - tsLastMqttReconnect < MQTT_RECONNECT_MS) {
    return;
  }

  tsLastMqttReconnect = now;

  Serial.printf("[MQTT] Connecting to %s:%d as %s\n", MQTT_HOST, MQTT_PORT, BAND_ID);

  secureClient.setInsecure();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setBufferSize(1024);

  bool connected = mqttClient.connect(
    BAND_ID,
    MQTT_USERNAME,
    MQTT_PASSWORD
  );

  if (connected) {
    Serial.println("[MQTT] Connected");

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("MQTT conectado");
    lcd.setCursor(0, 1);
    lcd.print(BAND_ID);
  } else {
    Serial.print("[MQTT] Failed, rc=");
    Serial.println(mqttClient.state());

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("MQTT error");
    lcd.setCursor(0, 1);
    lcd.print("Reintentando");
  }
}

void publishBiometrics() {
  if (!mqttClient.connected()) {
    Serial.println("[MQTT] Not connected. Biometrics not published.");
    return;
  }

  StaticJsonDocument<512> doc;

  doc["bandId"] = BAND_ID;
  doc["hubId"] = HUB_ID;
  doc["bpm"] = bpm;
  doc["gsr"] = gsr;
  doc["movement"] = movement;
  doc["battery"] = battery;
  doc["source"] = "SIMULATED";
  doc["timestamp"] = nowIsoPlaceholder();

  char buffer[512];
  size_t n = serializeJson(doc, buffer);

  // Contrato MQTT: biometrics QoS 0, retained false.
  bool ok = mqttClient.publish(
    topicBiometrics.c_str(),
    (const uint8_t*)buffer,
    n,
    false
  );

  Serial.printf("[MQTT] Publish biometrics ok=%s topic=%s\n", ok ? "true" : "false", topicBiometrics.c_str());
  Serial.println(buffer);
}

// ============================================================
// Sensores
// ============================================================

void setupSensors() {
  // MAX30100 en bus I2C principal
  Wire.begin(21, 22);

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Iniciando");
  lcd.setCursor(0, 1);
  lcd.print("MAX30100");

  if (!pox.begin()) {
    Serial.println("[MAX30100] NO encontrado");
    maxSensorOk = false;

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Error MAX30100");
    lcd.setCursor(0, 1);
    lcd.print("Modo simulado");

    delay(2000);
  } else {
    Serial.println("[MAX30100] Sensor OK");
    maxSensorOk = true;

    pox.setIRLedCurrent(MAX30100_LED_CURR_7_6MA);
    pox.setOnBeatDetectedCallback(onBeatDetected);

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("MAX30100 OK");
    lcd.setCursor(0, 1);
    lcd.print("Sensor listo");

    delay(1500);
  }

  // MPU6050 en segundo bus I2C
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Iniciando");
  lcd.setCursor(0, 1);
  lcd.print("MPU6050");

  I2C_MPU.begin(MPU_SDA, MPU_SCL, 400000);

  if (!mpu.begin(MPU_I2C_ADDR, &I2C_MPU)) {
    Serial.println("[MPU6050] NO encontrado");
    mpuOk = false;

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Error MPU6050");
    lcd.setCursor(0, 1);
    lcd.print("Mov simulado");

    delay(1500);
  } else {
    Serial.println("[MPU6050] Sensor OK");
    mpuOk = true;

    mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
    mpu.setGyroRange(MPU6050_RANGE_500_DEG);
    mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("MPU6050 OK");
    lcd.setCursor(0, 1);
    lcd.print("Movimiento OK");

    delay(1500);
  }

  pinMode(PIN_GSR, INPUT);
}

float readMovementFromMPU6050() {
  if (!mpuOk) {
    return 0.08;
  }

  sensors_event_t accel;
  sensors_event_t gyro;
  sensors_event_t temp;

  mpu.getEvent(&accel, &gyro, &temp);

  float ax = accel.acceleration.x;
  float ay = accel.acceleration.y;
  float az = accel.acceleration.z;

  // Magnitud de aceleración en m/s².
  float accelMagnitude = sqrt((ax * ax) + (ay * ay) + (az * az));

  // En reposo debe estar cerca de 9.8 por la gravedad.
  // Usamos la diferencia contra la lectura anterior para estimar movimiento.
  float delta = abs(accelMagnitude - lastAccelMagnitude);

  if (lastAccelMagnitude == 0.0) {
    delta = 0.0;
  }

  lastAccelMagnitude = accelMagnitude;

  // Convertimos delta a rango 0.0 - 1.0 para el contrato MQTT.
  // Ajuste inicial:
  // - 0.00 a 0.10 aprox reposo
  // - 0.10 a 0.40 movimiento leve
  // - 0.40+ movimiento fuerte
  float normalizedMovement = delta / 5.0;
  normalizedMovement = clampFloat(normalizedMovement, 0.0, 1.0);

  Serial.print("[MPU6050] ax: ");
  Serial.print(ax);
  Serial.print(" ay: ");
  Serial.print(ay);
  Serial.print(" az: ");
  Serial.print(az);
  Serial.print(" mag: ");
  Serial.print(accelMagnitude);
  Serial.print(" movement: ");
  Serial.println(normalizedMovement);

  return normalizedMovement;
}

void readSensors() {
  if (maxSensorOk) {
    bpm = pox.getHeartRate();
    spo2 = pox.getSpO2();

    if (bpm <= 30 || isnan(bpm)) {
      bpm = 70;
    }
  } else {
    bpm = 70;
    spo2 = 0;
  }

  int rawGsr = analogRead(PIN_GSR);
  gsr = rawGsr;

  movement = readMovementFromMPU6050();

  // Modo híbrido de prueba con potenciómetro
  if (gsr > 2800) {
    bpm = 115;
    gsr = 890;
    movement = 0.04;
    Serial.println("[TEST] HIGH_STRESS simulado por potenciómetro");
  } else if (gsr > 1200) {
    bpm = 90;
    gsr = 700;
    movement = 0.08;
    Serial.println("[TEST] MODERATE_STRESS simulado por potenciómetro");
  }

  battery = 85;

  Serial.print("[SENSOR] BPM: ");
  Serial.println(bpm);

  Serial.print("[SENSOR] SpO2: ");
  Serial.println(spo2);

  Serial.print("[SENSOR] GSR raw: ");
  Serial.println(gsr);

  Serial.print("[SENSOR] Movement: ");
  Serial.println(movement);

  Serial.println();
}

void updateLCD() {
  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("BPM:");
  lcd.print((int)bpm);

  lcd.setCursor(8, 0);
  lcd.print("O2:");
  lcd.print((int)spo2);

  lcd.setCursor(0, 1);
  lcd.print("G:");
  lcd.print((int)gsr);

  lcd.setCursor(8, 1);
  lcd.print("M:");
  lcd.print(movement, 2);
}

// ============================================================
// Arduino lifecycle
// ============================================================

void setup() {
  Serial.begin(115200);
  delay(1000);

  printLine();
  Serial.println("STRESS-LESS BAND SIM ESP32");
  Serial.printf("Firmware: %s\n", FIRMWARE_VERSION);
  printLine();

  lcd.begin(16, 2);
  lcd.setCursor(0, 0);
  lcd.print("Stress-Less");
  lcd.setCursor(0, 1);
  lcd.print("Banda init");
  delay(1500);

  configureTopics();
  setupSensors();
  connectWiFi();
  connectMQTT();

  lcd.clear();
}

void loop() {
  // IMPORTANTISIMO para MAX30100
  if (maxSensorOk) {
    pox.update();
  }

  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
  }

  if (!mqttClient.connected()) {
    connectMQTT();
  }

  mqttClient.loop();

  uint32_t now = millis();

  if (now - tsLastSensorReport >= SENSOR_REPORTING_PERIOD_MS) {
    readSensors();
    tsLastSensorReport = now;
  }

  if (now - tsLastLCD >= LCD_UPDATE_MS) {
    updateLCD();
    tsLastLCD = now;
  }

  if (now - tsLastMqttPublish >= MQTT_PUBLISH_MS) {
    publishBiometrics();
    tsLastMqttPublish = now;
  }
}