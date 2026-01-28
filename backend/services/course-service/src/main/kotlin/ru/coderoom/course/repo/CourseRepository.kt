package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CourseEntity
import java.util.UUID

interface CourseRepository : JpaRepository<CourseEntity, UUID>
