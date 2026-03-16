package ru.coderoom.course.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.course.domain.CodeAssignmentEntity
import java.util.UUID

interface CodeAssignmentRepository : JpaRepository<CodeAssignmentEntity, UUID>
