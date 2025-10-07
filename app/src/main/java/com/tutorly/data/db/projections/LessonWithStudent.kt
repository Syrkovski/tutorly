package com.tutorly.data.db.projections

import androidx.room.Embedded
import androidx.room.Relation
import com.tutorly.models.Lesson
import com.tutorly.models.Student
import com.tutorly.models.SubjectPreset

data class LessonWithStudent(
    @Embedded val lesson: Lesson,
    @Relation(parentColumn = "studentId", entityColumn = "id")
    val student: Student,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val subject: SubjectPreset?
)

data class StudentWithLessons(
    @Embedded val student: Student,
    @Relation(parentColumn = "id", entityColumn = "studentId")
    val lessons: List<Lesson>
)
