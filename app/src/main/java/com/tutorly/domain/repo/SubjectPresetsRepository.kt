package com.tutorly.domain.repo

import com.tutorly.models.SubjectPreset

interface SubjectPresetsRepository {
    suspend fun all(): List<SubjectPreset>
    suspend fun getById(id: Long): SubjectPreset?
    suspend fun upsert(preset: SubjectPreset): Long
}
