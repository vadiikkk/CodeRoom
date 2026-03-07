package ru.coderoom.autograding.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.coderoom.autograding.domain.TestRunEntity
import java.util.UUID

interface TestRunRepository : JpaRepository<TestRunEntity, UUID> {
    fun findByAttemptId(attemptId: UUID): TestRunEntity?
}
