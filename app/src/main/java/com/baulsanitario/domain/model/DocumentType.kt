package com.baulsanitario.domain.model

enum class DocumentType(val displayName: String) {
    RECIPE("Receta"),
    MEDICAL_RECORD("Registro médico"),
    EXAM("Examen"),
    RECEIPT("Boleta"),
    ORDER("Orden médica")
}
