package com.tutorly.ui.screens

data class StudentEditorFormState(
    val name: String = "",
    val phone: String = "",
    val messenger: String = "",
    val note: String = "",
    val isArchived: Boolean = false,
    val isActive: Boolean = true,
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
)
