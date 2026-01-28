package ru.coderoom.course.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.crypto.GithubPatCrypto
import ru.coderoom.course.crypto.EncryptedBytes
import ru.coderoom.course.domain.CourseBlockEntity
import ru.coderoom.course.domain.CourseEntity
import ru.coderoom.course.domain.CourseEnrollmentEntity
import ru.coderoom.course.domain.CourseGithubPatEntity
import ru.coderoom.course.domain.CourseItemEntity
import ru.coderoom.course.domain.CourseItemType
import ru.coderoom.course.domain.RoleInCourse
import ru.coderoom.course.repo.CourseBlockRepository
import ru.coderoom.course.repo.CourseEnrollmentRepository
import ru.coderoom.course.repo.CourseGithubPatRepository
import ru.coderoom.course.repo.CourseItemRepository
import ru.coderoom.course.repo.CourseRepository
import java.time.Instant
import java.util.UUID

@Service
class CourseAppService(
    private val access: CourseAccessService,
    private val courses: CourseRepository,
    private val enrollments: CourseEnrollmentRepository,
    private val blocks: CourseBlockRepository,
    private val items: CourseItemRepository,
    private val githubPats: CourseGithubPatRepository,
    private val patCrypto: GithubPatCrypto,
) {
    @Transactional
    fun createCourse(ownerUserId: UUID, title: String, description: String?, isVisible: Boolean): CourseEntity {
        val now = Instant.now()
        val course = CourseEntity(
            courseId = UUID.randomUUID(),
            ownerUserId = ownerUserId,
            title = title,
            description = description,
            isVisible = isVisible,
            createdAt = now,
            updatedAt = now,
        )
        courses.save(course)
        enrollments.save(
            CourseEnrollmentEntity(
                courseId = course.courseId,
                userId = ownerUserId,
                roleInCourse = RoleInCourse.TEACHER,
                createdAt = now,
            ),
        )
        return course
    }

    @Transactional(readOnly = true)
    fun listMyCourses(userId: UUID): List<CourseEntity> {
        val my = enrollments.findAllByUserId(userId).map { it.courseId }.distinct()
        if (my.isEmpty()) return emptyList()
        return courses.findAllById(my).sortedBy { it.createdAt }
    }

    @Transactional(readOnly = true)
    fun getCourseForMember(courseId: UUID, userId: UUID): CourseEntity {
        access.requireMember(courseId, userId)
        return access.requireCourse(courseId)
    }

    @Transactional
    fun updateCourse(courseId: UUID, userId: UUID, title: String?, description: String?, isVisible: Boolean?): CourseEntity {
        access.requireTeacher(courseId, userId)
        val course = access.requireCourse(courseId)
        title?.let { course.title = it }
        if (description != null) course.description = description.ifBlank { null }
        isVisible?.let { course.isVisible = it }
        course.updatedAt = Instant.now()
        return courses.save(course)
    }

    @Transactional
    fun deleteCourse(courseId: UUID, userId: UUID) {
        access.requireTeacher(courseId, userId)
        courses.deleteById(courseId)
    }

    @Transactional(readOnly = true)
    fun listEnrollments(courseId: UUID, requesterUserId: UUID): List<CourseEnrollmentEntity> {
        access.requireMember(courseId, requesterUserId)
        return enrollments.findAllByCourseId(courseId).sortedBy { it.createdAt }
    }

    @Transactional
    fun upsertEnrollment(courseId: UUID, requesterUserId: UUID, targetUserId: UUID, roleInCourse: RoleInCourse) {
        access.requireTeacher(courseId, requesterUserId)
        access.requireCourse(courseId)

        if (roleInCourse == RoleInCourse.TEACHER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign TEACHER via this endpoint")
        }

        val existing = enrollments.findByCourseIdAndUserId(courseId, targetUserId)
        if (existing != null) {
            existing.roleInCourse = roleInCourse
            enrollments.save(existing)
            return
        }
        enrollments.save(
            CourseEnrollmentEntity(
                courseId = courseId,
                userId = targetUserId,
                roleInCourse = roleInCourse,
                createdAt = Instant.now(),
            ),
        )
    }

    @Transactional
    fun removeEnrollment(courseId: UUID, requesterUserId: UUID, targetUserId: UUID) {
        access.requireTeacher(courseId, requesterUserId)
        val existing = enrollments.findByCourseIdAndUserId(courseId, targetUserId)
            ?: return
        if (existing.roleInCourse == RoleInCourse.TEACHER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove TEACHER enrollment")
        }
        enrollments.delete(existing)
    }

    @Transactional
    fun createBlock(courseId: UUID, userId: UUID, title: String, position: Int, isVisible: Boolean): CourseBlockEntity {
        access.requireTeacher(courseId, userId)
        access.requireCourse(courseId)
        val now = Instant.now()
        val block = CourseBlockEntity(
            blockId = UUID.randomUUID(),
            courseId = courseId,
            title = title,
            position = position,
            isVisible = isVisible,
            createdAt = now,
            updatedAt = now,
        )
        return blocks.save(block)
    }

    @Transactional
    fun updateBlock(courseId: UUID, userId: UUID, blockId: UUID, title: String?, position: Int?, isVisible: Boolean?): CourseBlockEntity {
        access.requireTeacher(courseId, userId)
        if (!blocks.existsByBlockIdAndCourseId(blockId, courseId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found")
        }
        val block = blocks.findById(blockId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found") }
        title?.let { block.title = it }
        position?.let { block.position = it }
        isVisible?.let { block.isVisible = it }
        block.updatedAt = Instant.now()
        return blocks.save(block)
    }

    @Transactional
    fun deleteBlock(courseId: UUID, userId: UUID, blockId: UUID) {
        access.requireTeacher(courseId, userId)
        if (!blocks.existsByBlockIdAndCourseId(blockId, courseId)) {
            return
        }
        blocks.deleteById(blockId)
    }

    @Transactional
    fun createItem(
        courseId: UUID,
        userId: UUID,
        blockId: UUID?,
        itemType: CourseItemType,
        refId: UUID,
        position: Int,
        isVisible: Boolean,
    ): CourseItemEntity {
        access.requireTeacher(courseId, userId)
        access.requireCourse(courseId)
        if (blockId != null && !blocks.existsByBlockIdAndCourseId(blockId, courseId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Block does not belong to course")
        }
        val now = Instant.now()
        val item = CourseItemEntity(
            itemId = UUID.randomUUID(),
            courseId = courseId,
            blockId = blockId,
            itemType = itemType,
            refId = refId,
            position = position,
            isVisible = isVisible,
            createdAt = now,
            updatedAt = now,
        )
        return items.save(item)
    }

    @Transactional
    fun updateItem(
        courseId: UUID,
        userId: UUID,
        itemId: UUID,
        blockId: UUID?,
        itemType: CourseItemType?,
        refId: UUID?,
        position: Int?,
        isVisible: Boolean?,
    ): CourseItemEntity {
        access.requireTeacher(courseId, userId)
        if (!items.existsByItemIdAndCourseId(itemId, courseId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")
        }
        if (blockId != null && !blocks.existsByBlockIdAndCourseId(blockId, courseId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Block does not belong to course")
        }
        val item = items.findById(itemId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found") }
        blockId?.let { item.blockId = it }
        itemType?.let { item.itemType = it }
        refId?.let { item.refId = it }
        position?.let { item.position = it }
        isVisible?.let { item.isVisible = it }
        item.updatedAt = Instant.now()
        return items.save(item)
    }

    @Transactional
    fun deleteItem(courseId: UUID, userId: UUID, itemId: UUID) {
        access.requireTeacher(courseId, userId)
        if (!items.existsByItemIdAndCourseId(itemId, courseId)) {
            return
        }
        items.deleteById(itemId)
    }

    @Transactional(readOnly = true)
    fun getStructure(courseId: UUID, userId: UUID): CourseStructure {
        access.requireMember(courseId, userId)
        val courseBlocks = blocks.findAllByCourseIdOrderByPositionAsc(courseId)
        val courseItems = items.findAllByCourseIdOrderByPositionAsc(courseId)
        return CourseStructure(blocks = courseBlocks, items = courseItems)
    }

    @Transactional
    fun setGithubPat(courseId: UUID, userId: UUID, token: String) {
        access.requireTeacher(courseId, userId)
        access.requireCourse(courseId)
        val encrypted = patCrypto.encrypt(token)
        upsertGithubPat(courseId, encrypted)
    }

    @Transactional
    fun clearGithubPat(courseId: UUID, userId: UUID) {
        access.requireTeacher(courseId, userId)
        githubPats.deleteById(courseId)
    }

    @Transactional(readOnly = true)
    fun githubPatStatus(courseId: UUID, userId: UUID): GithubPatStatus {
        access.requireTeacher(courseId, userId)
        val entity = githubPats.findById(courseId).orElse(null)
        return GithubPatStatus(
            configured = entity != null,
            updatedAt = entity?.updatedAt,
        )
    }

    @Transactional(readOnly = true)
    fun githubPatConfigured(courseId: UUID): Boolean =
        githubPats.existsById(courseId)

    private fun upsertGithubPat(courseId: UUID, encrypted: EncryptedBytes) {
        val now = Instant.now()
        val existing = githubPats.findById(courseId).orElse(null)
        if (existing == null) {
            githubPats.save(
                CourseGithubPatEntity(
                    courseId = courseId,
                    iv = encrypted.iv,
                    ciphertext = encrypted.ciphertext,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            existing.iv = encrypted.iv
            existing.ciphertext = encrypted.ciphertext
            existing.updatedAt = now
            githubPats.save(existing)
        }
    }
}

data class CourseStructure(
    val blocks: List<CourseBlockEntity>,
    val items: List<CourseItemEntity>,
)

data class GithubPatStatus(
    val configured: Boolean,
    val updatedAt: Instant?,
)
