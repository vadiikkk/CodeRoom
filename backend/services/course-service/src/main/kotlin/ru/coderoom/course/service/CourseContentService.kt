package ru.coderoom.course.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.coderoom.course.content.ContentClient
import ru.coderoom.course.content.ContentProperties
import ru.coderoom.course.domain.AssignmentEntity
import ru.coderoom.course.domain.AssignmentType
import ru.coderoom.course.domain.AssignmentWorkType
import ru.coderoom.course.domain.AttachmentEntity
import ru.coderoom.course.domain.CourseItemEntity
import ru.coderoom.course.domain.CourseItemType
import ru.coderoom.course.domain.MaterialEntity
import ru.coderoom.course.domain.RoleInCourse
import ru.coderoom.course.repo.AssignmentRepository
import ru.coderoom.course.repo.AttachmentRepository
import ru.coderoom.course.repo.CourseBlockRepository
import ru.coderoom.course.repo.CourseItemRepository
import ru.coderoom.course.repo.MaterialRepository
import ru.coderoom.course.repo.SubmissionAttachmentRepository
import ru.coderoom.course.repo.SubmissionMemberRepository
import ru.coderoom.course.repo.SubmissionRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class CourseContentService(
    private val access: CourseAccessService,
    private val blocks: CourseBlockRepository,
    private val items: CourseItemRepository,
    private val materials: MaterialRepository,
    private val assignments: AssignmentRepository,
    private val attachments: AttachmentRepository,
    private val submissions: SubmissionRepository,
    private val submissionMembers: SubmissionMemberRepository,
    private val submissionAttachments: SubmissionAttachmentRepository,
    private val contentClient: ContentClient,
    private val contentProperties: ContentProperties,
) {
    @Transactional
    fun createMaterial(
        courseId: UUID,
        userId: UUID,
        title: String,
        description: String?,
        body: String?,
        blockId: UUID?,
        position: Int,
        isVisible: Boolean,
        attachmentIds: List<UUID>,
    ): MaterialAggregate {
        access.requireTeacher(courseId, userId)
        access.requireCourse(courseId)
        validateBlock(courseId, blockId)

        val now = Instant.now()
        val materialId = UUID.randomUUID()
        val item = createStructureItem(
            courseId = courseId,
            blockId = blockId,
            itemType = CourseItemType.MATERIAL,
            refId = materialId,
            position = position,
            isVisible = isVisible,
            now = now,
        )
        val material = materials.save(
            MaterialEntity(
                materialId = materialId,
                courseId = courseId,
                itemId = item.itemId,
                title = title,
                description = description,
                body = body,
                createdAt = now,
                updatedAt = now,
            ),
        )
        validateMaterialHasContent(body = material.body, attachmentIds = attachmentIds)
        val linkedAttachments = replaceMaterialAttachments(courseId, material.materialId, attachmentIds)
        return MaterialAggregate(material = material, item = item, attachments = linkedAttachments)
    }

    @Transactional(readOnly = true)
    fun listMaterials(courseId: UUID, userId: UUID): List<MaterialAggregate> {
        val role = requireReadableCourse(courseId, userId)
        val itemMap = items.findAllByCourseIdOrderByPositionAsc(courseId).associateBy { it.itemId }
        val blockVisibility = blocks.findAllByCourseIdOrderByPositionAsc(courseId).associate { it.blockId to it.isVisible }
        return materials.findAllByCourseIdOrderByCreatedAtAsc(courseId)
            .mapNotNull { material ->
                val item = itemMap[material.itemId] ?: return@mapNotNull null
                if (!canReadItem(role, item, blockVisibility)) {
                    return@mapNotNull null
                }
                MaterialAggregate(
                    material = material,
                    item = item,
                    attachments = attachments.findAllByMaterialIdOrderBySortOrderAscCreatedAtAsc(material.materialId),
                )
            }
    }

    @Transactional(readOnly = true)
    fun getMaterial(materialId: UUID, userId: UUID): MaterialAggregate {
        val material = materials.findById(materialId).orElseThrow { notFound("Material not found") }
        val item = items.findById(material.itemId).orElseThrow { notFound("Material not found") }
        val role = requireReadableCourse(material.courseId, userId)
        val blockVisibility = blockVisibilityMap(material.courseId)
        if (!canReadItem(role, item, blockVisibility)) {
            throw notFound("Material not found")
        }
        return MaterialAggregate(
            material = material,
            item = item,
            attachments = attachments.findAllByMaterialIdOrderBySortOrderAscCreatedAtAsc(material.materialId),
        )
    }

    @Transactional
    fun updateMaterial(
        materialId: UUID,
        userId: UUID,
        title: String?,
        description: String?,
        body: String?,
        blockId: UUID?,
        clearBlock: Boolean,
        position: Int?,
        isVisible: Boolean?,
        attachmentIds: List<UUID>?,
    ): MaterialAggregate {
        val material = materials.findById(materialId).orElseThrow { notFound("Material not found") }
        access.requireTeacher(material.courseId, userId)

        val item = items.findById(material.itemId).orElseThrow { notFound("Material not found") }
        validateBlock(material.courseId, blockId)

        title?.let { material.title = it }
        if (description != null) material.description = description
        if (body != null) material.body = body

        if (clearBlock) {
            item.blockId = null
        } else {
            blockId?.let { item.blockId = it }
        }
        position?.let { item.position = it }
        isVisible?.let { item.isVisible = it }

        val now = Instant.now()
        material.updatedAt = now
        item.updatedAt = now

        val savedItem = items.save(item)
        val savedMaterial = materials.save(material)
        val linkedAttachments = attachmentIds?.let {
            validateMaterialHasContent(body = savedMaterial.body, attachmentIds = it)
            replaceMaterialAttachments(savedMaterial.courseId, savedMaterial.materialId, it)
        } ?: attachments.findAllByMaterialIdOrderBySortOrderAscCreatedAtAsc(savedMaterial.materialId)

        validateMaterialHasContent(body = savedMaterial.body, attachmentIds = linkedAttachments.map { it.attachmentId })
        return MaterialAggregate(material = savedMaterial, item = savedItem, attachments = linkedAttachments)
    }

    @Transactional
    fun deleteMaterial(materialId: UUID, userId: UUID) {
        val material = materials.findById(materialId).orElseThrow { notFound("Material not found") }
        access.requireTeacher(material.courseId, userId)
        deleteAttachmentObjects(attachments.findAllByMaterialIdOrderBySortOrderAscCreatedAtAsc(material.materialId))
        items.deleteById(material.itemId)
    }

    @Transactional
    fun createAssignment(
        courseId: UUID,
        userId: UUID,
        title: String,
        description: String?,
        assignmentType: AssignmentType,
        workType: AssignmentWorkType,
        deadlineAt: Instant?,
        weight: BigDecimal,
        blockId: UUID?,
        position: Int,
        isVisible: Boolean,
        attachmentIds: List<UUID>,
    ): AssignmentAggregate {
        access.requireTeacher(courseId, userId)
        access.requireCourse(courseId)
        validateBlock(courseId, blockId)
        validateAssignmentType(assignmentType)
        validateAssignmentWeight(weight)

        val now = Instant.now()
        val assignmentId = UUID.randomUUID()
        val item = createStructureItem(
            courseId = courseId,
            blockId = blockId,
            itemType = CourseItemType.ASSIGNMENT,
            refId = assignmentId,
            position = position,
            isVisible = isVisible,
            now = now,
        )
        val assignment = assignments.save(
            AssignmentEntity(
                assignmentId = assignmentId,
                courseId = courseId,
                itemId = item.itemId,
                title = title,
                description = description,
                assignmentType = assignmentType,
                workType = workType,
                deadlineAt = deadlineAt,
                weight = weight,
                createdAt = now,
                updatedAt = now,
            ),
        )
        validateAssignmentHasContent(
            assignmentType = assignment.assignmentType,
            description = assignment.description,
            attachmentIds = attachmentIds,
        )
        val linkedAttachments = replaceAssignmentAttachments(courseId, assignment.assignmentId, attachmentIds)
        return AssignmentAggregate(assignment = assignment, item = item, attachments = linkedAttachments)
    }

    @Transactional(readOnly = true)
    fun listAssignments(courseId: UUID, userId: UUID): List<AssignmentAggregate> {
        val role = requireReadableCourse(courseId, userId)
        val itemMap = items.findAllByCourseIdOrderByPositionAsc(courseId).associateBy { it.itemId }
        val blockVisibility = blocks.findAllByCourseIdOrderByPositionAsc(courseId).associate { it.blockId to it.isVisible }
        return assignments.findAllByCourseIdOrderByCreatedAtAsc(courseId)
            .mapNotNull { assignment ->
                val item = itemMap[assignment.itemId] ?: return@mapNotNull null
                if (!canReadItem(role, item, blockVisibility)) {
                    return@mapNotNull null
                }
                AssignmentAggregate(
                    assignment = assignment,
                    item = item,
                    attachments = attachments.findAllByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignment.assignmentId),
                )
            }
    }

    @Transactional(readOnly = true)
    fun getAssignment(assignmentId: UUID, userId: UUID): AssignmentAggregate {
        val assignment = assignments.findById(assignmentId).orElseThrow { notFound("Assignment not found") }
        val item = items.findById(assignment.itemId).orElseThrow { notFound("Assignment not found") }
        val role = requireReadableCourse(assignment.courseId, userId)
        val blockVisibility = blockVisibilityMap(assignment.courseId)
        if (!canReadItem(role, item, blockVisibility)) {
            throw notFound("Assignment not found")
        }
        return AssignmentAggregate(
            assignment = assignment,
            item = item,
            attachments = attachments.findAllByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignment.assignmentId),
        )
    }

    @Transactional
    fun updateAssignment(
        assignmentId: UUID,
        userId: UUID,
        title: String?,
        description: String?,
        assignmentType: AssignmentType?,
        workType: AssignmentWorkType?,
        deadlineAt: Instant?,
        clearDeadline: Boolean,
        weight: BigDecimal?,
        blockId: UUID?,
        clearBlock: Boolean,
        position: Int?,
        isVisible: Boolean?,
        attachmentIds: List<UUID>?,
    ): AssignmentAggregate {
        val assignment = assignments.findById(assignmentId).orElseThrow { notFound("Assignment not found") }
        access.requireTeacher(assignment.courseId, userId)

        val item = items.findById(assignment.itemId).orElseThrow { notFound("Assignment not found") }
        validateBlock(assignment.courseId, blockId)

        title?.let { assignment.title = it }
        if (description != null) assignment.description = description
        assignmentType?.let {
            validateAssignmentType(it)
            assignment.assignmentType = it
        }
        workType?.let { assignment.workType = it }
        weight?.let {
            validateAssignmentWeight(it)
            assignment.weight = it
        }
        if (clearDeadline || deadlineAt != null) {
            assignment.deadlineAt = deadlineAt
        }

        if (clearBlock) {
            item.blockId = null
        } else {
            blockId?.let { item.blockId = it }
        }
        position?.let { item.position = it }
        isVisible?.let { item.isVisible = it }

        val now = Instant.now()
        assignment.updatedAt = now
        item.updatedAt = now

        val savedItem = items.save(item)
        val savedAssignment = assignments.save(assignment)
        val linkedAttachments = attachmentIds?.let {
            validateAssignmentHasContent(
                assignmentType = savedAssignment.assignmentType,
                description = savedAssignment.description,
                attachmentIds = it,
            )
            replaceAssignmentAttachments(savedAssignment.courseId, savedAssignment.assignmentId, it)
        } ?: attachments.findAllByAssignmentIdOrderBySortOrderAscCreatedAtAsc(savedAssignment.assignmentId)

        validateAssignmentHasContent(
            assignmentType = savedAssignment.assignmentType,
            description = savedAssignment.description,
            attachmentIds = linkedAttachments.map { it.attachmentId },
        )
        return AssignmentAggregate(assignment = savedAssignment, item = savedItem, attachments = linkedAttachments)
    }

    @Transactional
    fun deleteAssignment(assignmentId: UUID, userId: UUID) {
        val assignment = assignments.findById(assignmentId).orElseThrow { notFound("Assignment not found") }
        access.requireTeacher(assignment.courseId, userId)
        deleteAttachmentObjects(attachments.findAllByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignment.assignmentId))
        items.deleteById(assignment.itemId)
    }

    @Transactional
    fun createUploadAttachment(
        courseId: UUID,
        userId: UUID,
        fileName: String,
        contentType: String?,
        fileSize: Long,
    ): AttachmentUpload {
        requireReadableCourse(courseId, userId)
        val attachmentId = UUID.randomUUID()
        val objectKey = buildObjectKey(courseId, attachmentId, fileName)
        val now = Instant.now()
        val attachment = attachments.save(
            AttachmentEntity(
                attachmentId = attachmentId,
                courseId = courseId,
                storageBucket = contentProperties.bucket,
                objectKey = objectKey,
                originalFilename = fileName,
                contentType = contentType,
                fileSize = fileSize,
                uploadedByUserId = userId,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val upload = contentClient.presignUpload(objectKey = objectKey, contentType = contentType)
        return AttachmentUpload(attachment = attachment, uploadUrl = upload.url, method = upload.method)
    }

    @Transactional(readOnly = true)
    fun presignDownloadAttachment(attachmentId: UUID, userId: UUID): PresignedAttachmentDownload {
        val attachment = attachments.findById(attachmentId).orElseThrow { notFound("Attachment not found") }
        val role = requireReadableCourse(attachment.courseId, userId)
        val blockVisibility = if (role == RoleInCourse.STUDENT) blockVisibilityMap(attachment.courseId) else emptyMap()
        if (role == RoleInCourse.STUDENT) {
            when {
                attachment.materialId != null -> {
                    val material = materials.findById(attachment.materialId!!).orElseThrow { notFound("Attachment not found") }
                    val item = items.findById(material.itemId).orElseThrow { notFound("Attachment not found") }
                    if (!canReadItem(role, item, blockVisibility)) {
                        throw notFound("Attachment not found")
                    }
                }
                attachment.assignmentId != null -> {
                    val assignment = assignments.findById(attachment.assignmentId!!).orElseThrow { notFound("Attachment not found") }
                    val item = items.findById(assignment.itemId).orElseThrow { notFound("Attachment not found") }
                    if (!canReadItem(role, item, blockVisibility)) {
                        throw notFound("Attachment not found")
                    }
                }
                submissionAttachments.findByAttachmentId(attachment.attachmentId) != null -> {
                    val link = submissionAttachments.findByAttachmentId(attachment.attachmentId)!!
                    val submission = submissions.findById(link.submissionId).orElseThrow { notFound("Attachment not found") }
                    if (!submissionMembers.existsBySubmissionIdAndUserId(submission.submissionId, userId)) {
                        throw notFound("Attachment not found")
                    }
                    val assignment = assignments.findById(submission.assignmentId).orElseThrow { notFound("Attachment not found") }
                    val item = items.findById(assignment.itemId).orElseThrow { notFound("Attachment not found") }
                    if (!canReadItem(role, item, blockVisibility)) {
                        throw notFound("Attachment not found")
                    }
                }
                else -> throw notFound("Attachment not found")
            }
        }

        val download = contentClient.presignDownload(
            objectKey = attachment.objectKey,
            fileName = attachment.originalFilename,
        )
        return PresignedAttachmentDownload(
            attachment = attachment,
            downloadUrl = download.url,
            method = download.method,
        )
    }

    private fun createStructureItem(
        courseId: UUID,
        blockId: UUID?,
        itemType: CourseItemType,
        refId: UUID,
        position: Int,
        isVisible: Boolean,
        now: Instant,
    ): CourseItemEntity =
        items.save(
            CourseItemEntity(
                itemId = UUID.randomUUID(),
                courseId = courseId,
                blockId = blockId,
                itemType = itemType,
                refId = refId,
                position = position,
                isVisible = isVisible,
                createdAt = now,
                updatedAt = now,
            ),
        )

    private fun replaceMaterialAttachments(courseId: UUID, materialId: UUID, attachmentIds: List<UUID>): List<AttachmentEntity> {
        val current = attachments.findAllByMaterialIdOrderBySortOrderAscCreatedAtAsc(materialId)
        val desired = resolveAttachments(courseId, attachmentIds, materialId = materialId, assignmentId = null)
        val desiredIds = desired.map { it.attachmentId }.toSet()

        current.filter { !desiredIds.contains(it.attachmentId) }.forEach {
            it.materialId = null
            it.sortOrder = 0
            it.updatedAt = Instant.now()
            attachments.save(it)
        }

        desired.forEachIndexed { index, attachment ->
            attachment.materialId = materialId
            attachment.assignmentId = null
            attachment.sortOrder = index
            attachment.updatedAt = Instant.now()
            attachments.save(attachment)
        }
        return attachments.findAllByMaterialIdOrderBySortOrderAscCreatedAtAsc(materialId)
    }

    private fun replaceAssignmentAttachments(courseId: UUID, assignmentId: UUID, attachmentIds: List<UUID>): List<AttachmentEntity> {
        val current = attachments.findAllByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignmentId)
        val desired = resolveAttachments(courseId, attachmentIds, materialId = null, assignmentId = assignmentId)
        val desiredIds = desired.map { it.attachmentId }.toSet()

        current.filter { !desiredIds.contains(it.attachmentId) }.forEach {
            it.assignmentId = null
            it.sortOrder = 0
            it.updatedAt = Instant.now()
            attachments.save(it)
        }

        desired.forEachIndexed { index, attachment ->
            attachment.materialId = null
            attachment.assignmentId = assignmentId
            attachment.sortOrder = index
            attachment.updatedAt = Instant.now()
            attachments.save(attachment)
        }
        return attachments.findAllByAssignmentIdOrderBySortOrderAscCreatedAtAsc(assignmentId)
    }

    private fun resolveAttachments(
        courseId: UUID,
        attachmentIds: List<UUID>,
        materialId: UUID?,
        assignmentId: UUID?,
    ): List<AttachmentEntity> {
        val uniqueIds = attachmentIds.distinct()
        if (uniqueIds.isEmpty()) return emptyList()

        val found = attachments.findAllByAttachmentIdIn(uniqueIds)
        if (found.size != uniqueIds.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Some attachments were not found")
        }

        val ordered = uniqueIds.map { id -> found.first { it.attachmentId == id } }
        ordered.forEach { attachment ->
            if (attachment.courseId != courseId) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment does not belong to course")
            }
            val attachedToOtherMaterial = attachment.materialId != null && attachment.materialId != materialId
            val attachedToOtherAssignment = attachment.assignmentId != null && attachment.assignmentId != assignmentId
            val attachedToSubmission = submissionAttachments.findByAttachmentId(attachment.attachmentId) != null
            if (attachedToOtherMaterial || attachedToOtherAssignment || attachedToSubmission) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment is already linked to another entity")
            }
        }
        return ordered
    }

    private fun deleteAttachmentObjects(toDelete: List<AttachmentEntity>) {
        toDelete.forEach { attachment ->
            contentClient.deleteObject(attachment.objectKey)
        }
    }

    private fun validateBlock(courseId: UUID, blockId: UUID?) {
        if (blockId != null && !blocks.existsByBlockIdAndCourseId(blockId, courseId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Block does not belong to course")
        }
    }

    private fun requireReadableCourse(courseId: UUID, userId: UUID): RoleInCourse {
        val role = access.requireMember(courseId, userId)
        val course = access.requireCourse(courseId)
        if (role == RoleInCourse.STUDENT && !course.isVisible) {
            throw notFound("Course not found")
        }
        return role
    }

    private fun canReadItem(
        role: RoleInCourse,
        item: CourseItemEntity,
        blockVisibility: Map<UUID, Boolean>,
    ): Boolean {
        if (role != RoleInCourse.STUDENT) {
            return true
        }
        if (!item.isVisible) {
            return false
        }
        val blockId = item.blockId ?: return true
        return blockVisibility[blockId] == true
    }

    private fun blockVisibilityMap(courseId: UUID): Map<UUID, Boolean> =
        blocks.findAllByCourseIdOrderByPositionAsc(courseId).associate { it.blockId to it.isVisible }

    private fun buildObjectKey(courseId: UUID, attachmentId: UUID, fileName: String): String {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "file" }
        return "courses/$courseId/attachments/$attachmentId/$safeName"
    }

    private fun validateMaterialHasContent(body: String?, attachmentIds: List<UUID>) {
        if (body.isNullOrBlank() && attachmentIds.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Material must contain text or attachments")
        }
    }

    private fun validateAssignmentHasContent(assignmentType: AssignmentType, description: String?, attachmentIds: List<UUID>) {
        if (assignmentType == AssignmentType.CODE) {
            return
        }
        if (description.isNullOrBlank() && attachmentIds.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment must contain description or attachments")
        }
    }

    private fun validateAssignmentType(@Suppress("UNUSED_PARAMETER") assignmentType: AssignmentType) {}

    private fun validateAssignmentWeight(weight: BigDecimal) {
        if (weight < BigDecimal.ZERO || weight > BigDecimal.ONE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "weight must be between 0 and 1")
        }
    }

    private fun notFound(message: String): ResponseStatusException =
        ResponseStatusException(HttpStatus.NOT_FOUND, message)
}

data class MaterialAggregate(
    val material: MaterialEntity,
    val item: CourseItemEntity,
    val attachments: List<AttachmentEntity>,
)

data class AssignmentAggregate(
    val assignment: AssignmentEntity,
    val item: CourseItemEntity,
    val attachments: List<AttachmentEntity>,
)

data class AttachmentUpload(
    val attachment: AttachmentEntity,
    val uploadUrl: String,
    val method: String,
)

data class PresignedAttachmentDownload(
    val attachment: AttachmentEntity,
    val downloadUrl: String,
    val method: String,
)
