#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

#include "arduino_secrets.h"

// ============================================================
// STRESS-LESS HUB ESP32
// Funciones:
// - Conexión WiFi
// - Conexión MQTT/TLS
// - Publica status/devices
// - Recibe commands
// - Ejecuta acciones básicas
// - Publica COMMAND_ACK
// ============================================================

// Cambia a 0 si aún no hay LCD I2C conectado.
#define USE_LCD 0

// Pines del prototipo
#define PIN_LED_R 25
#define PIN_LED_G 26
#define PIN_LED_B 27
#define PIN_FAN   14
#define PIN_BUZZER 12

// PWM ESP32
#define PWM_FREQ 5000
#define PWM_RESOLUTION 8
#define PWM_CH_R 0
#define PWM_CH_G 1
#define PWM_CH_B 2
#define PWM_CH_FAN 3
#define PWM_CH_BUZZER 4

// MQTT topics
String topicStatus;
String topicDevices;
String topicCommands;
String topicEvents;

// Clientes
WiFiClientSecure secureClient;
PubSubClient mqttClient(secureClient);

#if USE_LCD
LiquidCrystal_I2C lcd(0x27, 16, 2);
#endif

unsigned long lastHeartbeat = 0;
const unsigned long HEARTBEAT_INTERVAL_MS = 30000;

bool mqttWasConnected = false;

void publishEvent(const char* eventType, const char* severity, const char* description);

// ============================================================
// Utilidades
// ============================================================

String nowIsoPlaceholder() {
  // El backend puede usar received_at si el timestamp no es confiable.
  return "1970-01-01T00:00:00Z";
}

void printLine() {
  Serial.println("--------------------------------------------------");
}

void showLcdMessage(const String& line1, const String& line2 = "") {
#if USE_LCD
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(line1.substring(0, 16));
  lcd.setCursor(0, 1);
  lcd.print(line2.substring(0, 16));
#else
  Serial.print("[LCD] ");
  Serial.print(line1);
  Serial.print(" | ");
  Serial.println(line2);
#endif
}

int clampInt(int value, int minValue, int maxValue) {
  if (value < minValue) return minValue;
  if (value > maxValue) return maxValue;
  return value;
}

// ============================================================
// Actuadores
// ============================================================

void setupActuators() {
  ledcAttachChannel(PIN_LED_R, PWM_FREQ, PWM_RESOLUTION, PWM_CH_R);
  ledcAttachChannel(PIN_LED_G, PWM_FREQ, PWM_RESOLUTION, PWM_CH_G);
  ledcAttachChannel(PIN_LED_B, PWM_FREQ, PWM_RESOLUTION, PWM_CH_B);
  ledcAttachChannel(PIN_FAN, PWM_FREQ, PWM_RESOLUTION, PWM_CH_FAN);
  ledcAttachChannel(PIN_BUZZER, PWM_FREQ, PWM_RESOLUTION, PWM_CH_BUZZER);

  ledcWriteChannel(PWM_CH_R, 0);
  ledcWriteChannel(PWM_CH_G, 0);
  ledcWriteChannel(PWM_CH_B, 0);
  ledcWriteChannel(PWM_CH_FAN, 0);
  ledcWriteChannel(PWM_CH_BUZZER, 0);
}

void setRgb(int r, int g, int b) {
  r = clampInt(r, 0, 255);
  g = clampInt(g, 0, 255);
  b = clampInt(b, 0, 255);

  ledcWriteChannel(PWM_CH_R, r);
  ledcWriteChannel(PWM_CH_G, g);
  ledcWriteChannel(PWM_CH_B, b);

  Serial.printf("[ACTUATOR] LED RGB -> R=%d G=%d B=%d\n", r, g, b);
}

void setBrightness(int brightness) {
  brightness = clampInt(brightness, 0, 100);
  int pwm = map(brightness, 0, 100, 0, 255);

  // Brillo blanco aproximado usando los 3 canales.
  ledcWriteChannel(PWM_CH_R, pwm);
  ledcWriteChannel(PWM_CH_G, pwm);
  ledcWriteChannel(PWM_CH_B, pwm);

  Serial.printf("[ACTUATOR] LED brightness -> %d%%\n", brightness);
}

void setColorHex(String hex) {
  hex.trim();

  if (hex.startsWith("#")) {
    hex.remove(0, 1);
  }

  if (hex.length() != 6) {
    Serial.println("[WARN] Invalid HEX color");
    return;
  }

  long number = strtol(hex.c_str(), NULL, 16);

  int r = (number >> 16) & 0xFF;
  int g = (number >> 8) & 0xFF;
  int b = number & 0xFF;

  setRgb(r, g, b);
}

void setFanSpeedFromString(String speed) {
  speed.toUpperCase();

  int pwm = 0;

  if (speed == "LOW") {
    pwm = 85;
  } else if (speed == "MEDIUM") {
    pwm = 170;
  } else if (speed == "HIGH") {
    pwm = 255;
  } else {
    pwm = 0;
  }

  ledcWriteChannel(PWM_CH_FAN, pwm);
  Serial.printf("[ACTUATOR] Fan speed -> %s PWM=%d\n", speed.c_str(), pwm);
}

void setFanSpeedPercent(int percent) {
  percent = clampInt(percent, 0, 100);
  int pwm = map(percent, 0, 100, 0, 255);

  ledcWriteChannel(PWM_CH_FAN, pwm);
  Serial.printf("[ACTUATOR] Fan speed percent -> %d%%\n", percent);
}

void turnDeviceOn(const String& deviceKey) {
  if (deviceKey == "led-rgb-001") {
    setBrightness(80);
  } else if (deviceKey == "fan-001") {
    setFanSpeedPercent(70);
  } else if (deviceKey == "buzzer-001") {
    ledcWriteChannel(PWM_CH_BUZZER, 128);
  } else if (deviceKey == "display-001") {
    showLcdMessage("Display ON", "");
  }

  Serial.printf("[ACTUATOR] TURN_ON -> %s\n", deviceKey.c_str());
}

void turnDeviceOff(const String& deviceKey) {
  if (deviceKey == "led-rgb-001") {
    setRgb(0, 0, 0);
  } else if (deviceKey == "fan-001") {
    ledcWriteChannel(PWM_CH_FAN, 0);
  } else if (deviceKey == "buzzer-001") {
    ledcWriteChannel(PWM_CH_BUZZER, 0);
  } else if (deviceKey == "display-001") {
    showLcdMessage("", "");
  }

  Serial.printf("[ACTUATOR] TURN_OFF -> %s\n", deviceKey.c_str());
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
  }

  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("[WIFI] Connected");
    Serial.print("[WIFI] IP: ");
    Serial.println(WiFi.localIP());
    showLcdMessage("WiFi conectado", WiFi.localIP().toString());
  } else {
    Serial.println("[WIFI] Failed to connect");
    showLcdMessage("WiFi error", "Revisar config");
  }

  printLine();
}

// ============================================================
// MQTT publish helpers
// ============================================================

void publishStatus(const char* status, const char* operationalState, bool retained) {
  StaticJsonDocument<512> doc;

  doc["hubId"] = HUB_ID;
  doc["status"] = status;
  doc["operational_state"] = operationalState;
  doc["firmware_version"] = FIRMWARE_VERSION;
  doc["ip_address"] = WiFi.localIP().toString();
  doc["free_heap"] = ESP.getFreeHeap();
  doc["uptime_seconds"] = millis() / 1000;
  doc["timestamp"] = nowIsoPlaceholder();

  char buffer[512];
  size_t n = serializeJson(doc, buffer);

  bool ok = mqttClient.publish(
    topicStatus.c_str(),
    (const uint8_t*)buffer,
    n,
    retained
  );

  Serial.printf("[MQTT] Publish status retained=%s ok=%s\n", retained ? "true" : "false", ok ? "true" : "false");
  Serial.println(buffer);
}

void publishDevices() {
  StaticJsonDocument<1536> doc;

  doc["hubId"] = HUB_ID;

  JsonArray devices = doc.createNestedArray("devices");

  JsonObject led = devices.createNestedObject();
  led["deviceKey"] = "led-rgb-001";
  led["name"] = "LED RGB";
  led["type"] = "LIGHT";
  JsonArray ledCaps = led.createNestedArray("capabilities");
  ledCaps.add("ON_OFF");
  ledCaps.add("BRIGHTNESS");
  ledCaps.add("COLOR");
  JsonObject ledState = led.createNestedObject("currentState");
  ledState["on"] = true;
  ledState["brightness"] = 80;
  ledState["color"] = "#FFFFFF";

  JsonObject fan = devices.createNestedObject();
  fan["deviceKey"] = "fan-001";
  fan["name"] = "Ventilador";
  fan["type"] = "FAN";
  JsonArray fanCaps = fan.createNestedArray("capabilities");
  fanCaps.add("ON_OFF");
  fanCaps.add("SPEED");
  JsonObject fanState = fan.createNestedObject("currentState");
  fanState["on"] = false;

  JsonObject display = devices.createNestedObject();
  display["deviceKey"] = "display-001";
  display["name"] = "LCD 16x2";
  display["type"] = "DISPLAY";
  JsonArray displayCaps = display.createNestedArray("capabilities");
  displayCaps.add("ON_OFF");
  displayCaps.add("MESSAGE");
  JsonObject displayState = display.createNestedObject("currentState");
  displayState["on"] = true;
  displayState["message"] = "Iniciando...";

  JsonObject buzzer = devices.createNestedObject();
  buzzer["deviceKey"] = "buzzer-001";
  buzzer["name"] = "Buzzer audio";
  buzzer["type"] = "AUDIO";
  JsonArray buzzerCaps = buzzer.createNestedArray("capabilities");
  buzzerCaps.add("ON_OFF");
  buzzerCaps.add("VOLUME");
  JsonObject buzzerState = buzzer.createNestedObject("currentState");
  buzzerState["on"] = false;

  doc["timestamp"] = nowIsoPlaceholder();

  char buffer[1536];
  size_t n = serializeJson(doc, buffer);

  bool ok = mqttClient.publish(
    topicDevices.c_str(),
    (const uint8_t*)buffer,
    n,
    true
  );

  Serial.printf("[MQTT] Publish devices retained=true ok=%s\n", ok ? "true" : "false");
  Serial.println(buffer);
}

void publishCommandAck(
  const String& commandId,
  const JsonArray& executed,
  const JsonArray& failed,
  bool hasFailures
) {
  StaticJsonDocument<1024> doc;

  doc["hubId"] = HUB_ID;
  doc["eventType"] = hasFailures ? "COMMAND_FAILED" : "COMMAND_ACK";
  doc["severity"] = hasFailures ? "WARN" : "INFO";
  doc["commandId"] = commandId;
  doc["description"] = hasFailures ? "Comando ejecutado con errores parciales" : "Comandos ejecutados correctamente";

  JsonObject metadata = doc.createNestedObject("metadata");

  JsonArray executedOut = metadata.createNestedArray("executed");
  for (JsonVariant item : executed) {
    executedOut.add(item.as<String>());
  }

  JsonArray failedOut = metadata.createNestedArray("failed");
  for (JsonVariant item : failed) {
    failedOut.add(item.as<String>());
  }

  doc["timestamp"] = nowIsoPlaceholder();

  char buffer[1024];
  size_t n = serializeJson(doc, buffer);

  bool ok = mqttClient.publish(
    topicEvents.c_str(),
    (const uint8_t*)buffer,
    n,
    false
  );

  Serial.printf("[MQTT] Publish command ACK ok=%s\n", ok ? "true" : "false");
  Serial.println(buffer);
}

// ============================================================
// Command execution
// ============================================================

bool executeAction(JsonObject actionObj, String& errorMessage) {
  String deviceKey = actionObj["deviceKey"] | "";
  String action = actionObj["action"] | "";

  if (deviceKey.length() == 0 || action.length() == 0) {
    errorMessage = "Missing deviceKey or action";
    return false;
  }

  Serial.printf("[COMMAND] deviceKey=%s action=%s\n", deviceKey.c_str(), action.c_str());

  if (action == "TURN_ON") {
    turnDeviceOn(deviceKey);
    return true;
  }

  if (action == "TURN_OFF") {
    turnDeviceOff(deviceKey);
    return true;
  }

  if (action == "SET_BRIGHTNESS") {
    int value = actionObj["value"] | 0;
    setBrightness(value);
    return true;
  }

  if (action == "SET_COLOR_RGB") {
    JsonObject color = actionObj["value"].as<JsonObject>();

    int r = color["r"] | 0;
    int g = color["g"] | 0;
    int b = color["b"] | 0;

    setRgb(r, g, b);
    return true;
  }

  if (action == "SET_COLOR_HEX") {
    String hex = actionObj["value"] | "#000000";
    setColorHex(hex);
    return true;
  }

  if (action == "SET_SPEED") {
    if (actionObj["value"].is<const char*>()) {
      String speed = actionObj["value"] | "LOW";
      setFanSpeedFromString(speed);
    } else {
      int percent = actionObj["value"] | 0;
      setFanSpeedPercent(percent);
    }

    return true;
  }

  if (action == "SET_VOLUME") {
    int volume = actionObj["value"] | 0;
    volume = clampInt(volume, 0, 100);
    int pwm = map(volume, 0, 100, 0, 255);
    ledcWriteChannel(PWM_CH_BUZZER, pwm);
    Serial.printf("[ACTUATOR] Buzzer volume -> %d%%\n", volume);
    return true;
  }

  if (action == "SHOW_MESSAGE") {
    String message = actionObj["value"] | "";
    showLcdMessage(message, "");
    Serial.printf("[ACTUATOR] LCD message -> %s\n", message.c_str());
    return true;
  }

  errorMessage = "Unsupported action: " + action;
  return false;
}

void handleCommandMessage(char* topic, byte* payload, unsigned int length) {
  Serial.println();
  printLine();
  Serial.printf("[MQTT] Message received topic=%s length=%u\n", topic, length);

  String payloadText;
  payloadText.reserve(length + 1);

  for (unsigned int i = 0; i < length; i++) {
    payloadText += (char)payload[i];
  }

  Serial.println("[MQTT] Payload:");
  Serial.println(payloadText);

  StaticJsonDocument<4096> doc;
  DeserializationError error = deserializeJson(doc, payloadText);

  if (error) {
    Serial.print("[ERROR] JSON parse failed: ");
    Serial.println(error.c_str());
    return;
  }

  String commandId = doc["commandId"] | "";
  String hubId = doc["hubId"] | "";

  if (hubId != HUB_ID) {
    Serial.printf("[WARN] Command ignored. Payload hubId=%s local hubId=%s\n", hubId.c_str(), HUB_ID);
    return;
  }

  JsonArray actions = doc["actions"].as<JsonArray>();

  if (actions.isNull() || actions.size() == 0) {
    Serial.println("[WARN] Command has no actions");
    return;
  }

  StaticJsonDocument<512> ackData;
  JsonArray executed = ackData.createNestedArray("executed");
  JsonArray failed = ackData.createNestedArray("failed");

  bool hasFailures = false;

  for (JsonObject actionObj : actions) {
    String deviceKey = actionObj["deviceKey"] | "";
    String errorMessage;

    bool ok = executeAction(actionObj, errorMessage);

    if (ok) {
      executed.add(deviceKey);
    } else {
      hasFailures = true;

      String failedItem = deviceKey + ": " + errorMessage;
      failed.add(failedItem);

      Serial.printf("[ERROR] Action failed: %s\n", failedItem.c_str());
    }
  }

  publishCommandAck(commandId, executed, failed, hasFailures);

  printLine();
}

// ============================================================
// MQTT connection
// ============================================================

void configureTopics() {
  topicStatus = "stressless/hub/" + String(HUB_ID) + "/status";
  topicDevices = "stressless/hub/" + String(HUB_ID) + "/devices";
  topicCommands = "stressless/hub/" + String(HUB_ID) + "/commands";
  topicEvents = "stressless/hub/" + String(HUB_ID) + "/events";
}

void connectMQTT() {
  if (mqttClient.connected()) {
    return;
  }

  Serial.printf("[MQTT] Connecting to %s:%d as %s\n", MQTT_HOST, MQTT_PORT, HUB_ID);

  secureClient.setInsecure();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(handleCommandMessage);
  mqttClient.setBufferSize(4096);

  while (!mqttClient.connected()) {
    bool connected = mqttClient.connect(
      HUB_ID,
      MQTT_USERNAME,
      MQTT_PASSWORD,
      topicStatus.c_str(),
      1,
      true,
      "{\"hubId\":\"hub-001\",\"status\":\"OFFLINE\",\"operational_state\":\"ERROR\",\"timestamp\":null,\"reason\":\"LWT\"}"
    );

    if (connected) {
      Serial.println("[MQTT] Connected");

      mqttClient.subscribe(topicCommands.c_str(), 1);
      Serial.printf("[MQTT] Subscribed to %s\n", topicCommands.c_str());

      publishStatus("ACTIVE", "ACTIVE", true);
      publishDevices();

      showLcdMessage("MQTT conectado", HUB_ID);

      if (mqttWasConnected) {
        publishEvent("MQTT_RECONNECTED", "INFO", "Reconexión MQTT exitosa");
      }

      mqttWasConnected = true;
    } else {
      Serial.print("[MQTT] Failed, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" retrying in 5 seconds");

      showLcdMessage("MQTT error", "Reintentando...");
      delay(5000);
    }
  }
}

void publishEvent(const char* eventType, const char* severity, const char* description) {
  StaticJsonDocument<512> doc;

  doc["hubId"] = HUB_ID;
  doc["eventType"] = eventType;
  doc["severity"] = severity;
  doc["description"] = description;
  doc["timestamp"] = nowIsoPlaceholder();

  char buffer[512];
  size_t n = serializeJson(doc, buffer);

  mqttClient.publish(
    topicEvents.c_str(),
    (const uint8_t*)buffer,
    n,
    false
  );

  Serial.printf("[MQTT] Event published: %s\n", eventType);
}

// ============================================================
// Arduino lifecycle
// ============================================================

void setup() {
  Serial.begin(115200);
  delay(1000);

  printLine();
  Serial.println("STRESS-LESS HUB ESP32");
  Serial.printf("Firmware: %s\n", FIRMWARE_VERSION);
  printLine();

#if USE_LCD
  Wire.begin(21, 22);
  lcd.init();
  lcd.backlight();
  showLcdMessage("STRESS-LESS", "Iniciando...");
#endif

  setupActuators();
  configureTopics();
  connectWiFi();
  connectMQTT();

  publishEvent("STARTUP", "INFO", "Hub iniciado correctamente");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[WIFI] Disconnected. Reconnecting...");
    connectWiFi();
  }

  if (!mqttClient.connected()) {
    connectMQTT();
  }

  mqttClient.loop();

  unsigned long now = millis();

  if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
    lastHeartbeat = now;
    publishStatus("ACTIVE", "ACTIVE", true);
  }
}