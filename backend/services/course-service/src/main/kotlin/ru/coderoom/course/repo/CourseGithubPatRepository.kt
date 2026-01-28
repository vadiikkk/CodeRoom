package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CourseGithubPatEntity
import java.util.UUID

interface CourseGithubPatRepository : JpaRepository<CourseGithubPatEntity, UUID>
