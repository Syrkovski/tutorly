package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.SubjectPresetDao
import com.tutorly.models.SubjectPreset
import com.tutorly.domain.repo.SubjectPresetsRepository

class RoomSubjectPresetsRepository(
    private val dao: SubjectPresetDao
) : SubjectPresetsRepository {
    override suspend fun all(): List<SubjectPreset> = dao.all()
    override suspend fun upsert(preset: SubjectPreset): Long = dao.upsert(preset)
}
