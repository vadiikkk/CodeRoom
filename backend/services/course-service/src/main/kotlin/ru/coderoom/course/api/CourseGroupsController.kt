package ru.coderoom.course.api

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.coderoom.course.api.dto.AddGroupMemberRequest
import ru.coderoom.course.api.dto.CreateGroupRequest
import ru.coderoom.course.api.dto.GroupMemberResponse
import ru.coderoom.course.api.dto.GroupResponse
import ru.coderoom.course.api.dto.UpdateGroupRequest
import ru.coderoom.course.security.RequestUser
import ru.coderoom.course.service.CourseGroupAggregate
import ru.coderoom.course.service.CourseGroupService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/courses/{courseId}/groups")
class CourseGroupsController(
    private val groups: CourseGroupService,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
    ): List<GroupResponse> =
        groups.listGroups(courseId, user.userId).map(::toResponse)

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @Valid @RequestBody req: CreateGroupRequest,
    ): GroupResponse =
        toResponse(groups.createGroup(courseId, user.userId, req.name.trim()))

    @PutMapping("/{groupId}")
    fun update(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable groupId: UUID,
        @Valid @RequestBody req: UpdateGroupRequest,
    ): GroupResponse =
        toResponse(groups.updateGroup(courseId, user.userId, groupId, req.name.trim()))

    @DeleteMapping("/{groupId}")
    fun delete(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable groupId: UUID,
    ) {
        groups.deleteGroup(courseId, user.userId, groupId)
    }

    @PostMapping("/{groupId}/members")
    fun addMember(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable groupId: UUID,
        @Valid @RequestBody req: AddGroupMemberRequest,
    ): GroupResponse =
        toResponse(groups.addMember(courseId, user.userId, groupId, req.userId))

    @DeleteMapping("/{groupId}/members/{memberUserId}")
    fun removeMember(
        @AuthenticationPrincipal user: RequestUser,
        @PathVariable courseId: UUID,
        @PathVariable groupId: UUID,
        @PathVariable memberUserId: UUID,
    ): GroupResponse =
        toResponse(groups.removeMember(courseId, user.userId, groupId, memberUserId))

    private fun toResponse(aggregate: CourseGroupAggregate): GroupResponse =
        GroupResponse(
            groupId = aggregate.group.groupId,
            courseId = aggregate.group.courseId,
            name = aggregate.group.name,
            members = aggregate.members.map {
                GroupMemberResponse(
                    userId = it.userId,
                    createdAt = it.createdAt,
                )
            },
            createdAt = aggregate.group.createdAt,
            updatedAt = aggregate.group.updatedAt,
        )
}
