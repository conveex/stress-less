package com.stressless.app.util

fun physiologicalStateLabel(state: String): String {
    return when (state) {
        "HIGH_STRESS" -> "ESTRÉS ALTO"
        "MODERATE_STRESS" -> "ESTRÉS MODERADO"
        "NORMAL" -> "NORMAL"
        "RELAXED" -> "RELAJADO"
        "MODERATE_RELAXED" -> "RELAJACIÓN MODERADA"
        "NO_DATA" -> "SIN DATOS"
        else -> state
    }
}

fun operationalStateLabel(state: String): String {
    return when (state) {
        "ACTIVE" -> "AUTOMÁTICO"
        "PAUSED" -> "PAUSADO"
        "MANUAL" -> "MANUAL"
        "EXIT_MODE" -> "MODO SALIDA"
        else -> state
    }
}