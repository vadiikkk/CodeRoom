import type {
  AddGroupMemberRequest,
  AssignmentResponse,
  CodeAttemptLogDownloadResponse,
  CodeAttemptResponse,
  CourseGradebookResponse,
  CreateAssignmentRequest,
  CreateBlockRequest,
  CreateGroupRequest,
  CreateItemRequest,
  CourseResponse,
  CourseStructureResponse,
  CreateCodeAttemptRequest,
  CreateCourseRequest,
  CreateMaterialRequest,
  CreateSubmissionRequest,
  EnrollmentResponse,
  GithubPatStatusResponse,
  GradeSubmissionRequest,
  GroupResponse,
  ItemResponse,
  MaterialResponse,
  MyGradebookResponse,
  MyMembershipResponse,
  PresignDownloadAttachmentResponse,
  PresignUploadAttachmentRequest,
  PresignUploadAttachmentResponse,
  SetGithubPatRequest,
  SubmissionResponse,
  UpdateAssignmentRequest,
  UpdateBlockRequest,
  UpdateCourseRequest,
  UpdateGroupRequest,
  UpdateItemRequest,
  UpdateMaterialRequest,
  UpsertEnrollmentRequest,
  UpsertEnrollmentsByEmailRequest,
  UpsertEnrollmentsByEmailResponse,
} from '@/entities/course/types'
import { ApiError, apiRequest } from '@/shared/api/http'

export const coursesApi = {
  listCourses() {
    return apiRequest<CourseResponse[]>('/api/v1/courses')
  },

  createCourse(payload: CreateCourseRequest) {
    return apiRequest<CourseResponse>('/api/v1/courses', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateCourse(courseId: string, payload: UpdateCourseRequest) {
    return apiRequest<CourseResponse>(`/api/v1/courses/${courseId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  getCourse(courseId: string) {
    return apiRequest<CourseResponse>(`/api/v1/courses/${courseId}`)
  },

  getMembership(courseId: string) {
    return apiRequest<MyMembershipResponse>(
      `/api/v1/courses/${courseId}/membership/me`,
    )
  },

  getStructure(courseId: string) {
    return apiRequest<CourseStructureResponse>(
      `/api/v1/courses/${courseId}/structure`,
    )
  },

  listMaterials(courseId: string) {
    return apiRequest<MaterialResponse[]>(`/api/v1/courses/${courseId}/materials`)
  },

  getMaterial(materialId: string) {
    return apiRequest<MaterialResponse>(`/api/v1/materials/${materialId}`)
  },

  listAssignments(courseId: string) {
    return apiRequest<AssignmentResponse[]>(
      `/api/v1/courses/${courseId}/assignments`,
    )
  },

  getAssignment(assignmentId: string) {
    return apiRequest<AssignmentResponse>(`/api/v1/assignments/${assignmentId}`)
  },

  getMySubmission(assignmentId: string) {
    return apiRequest<SubmissionResponse>(
      `/api/v1/assignments/${assignmentId}/submissions/me`,
    ).catch((error) => {
      if (error instanceof ApiError && error.status === 404) {
        return null
      }

      throw error
    })
  },

  createOrUpdateSubmission(assignmentId: string, payload: CreateSubmissionRequest) {
    return apiRequest<SubmissionResponse>(
      `/api/v1/assignments/${assignmentId}/submissions`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    )
  },

  presignUploadAttachment(payload: PresignUploadAttachmentRequest) {
    return apiRequest<PresignUploadAttachmentResponse>(
      '/api/v1/attachments/presign-upload',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    )
  },

  presignDownloadAttachment(attachmentId: string) {
    return apiRequest<PresignDownloadAttachmentResponse>(
      '/api/v1/attachments/presign-download',
      {
        method: 'POST',
        body: JSON.stringify({ attachmentId }),
      },
    )
  },

  getMyCodeAttempts(assignmentId: string) {
    return apiRequest<CodeAttemptResponse[]>(
      `/api/v1/assignments/${assignmentId}/code-attempts/me`,
    )
  },

  createCodeAttempt(assignmentId: string, payload: CreateCodeAttemptRequest) {
    return apiRequest<CodeAttemptResponse>(
      `/api/v1/assignments/${assignmentId}/code-attempts`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    )
  },

  getCodeAttempt(attemptId: string) {
    return apiRequest<CodeAttemptResponse>(`/api/v1/code-attempts/${attemptId}`)
  },

  getCodeAttemptLog(attemptId: string) {
    return apiRequest<CodeAttemptLogDownloadResponse>(
      `/api/v1/code-attempts/${attemptId}/log`,
    )
  },

  getGithubPatStatus(courseId: string) {
    return apiRequest<GithubPatStatusResponse>(`/api/v1/courses/${courseId}/github-pat`)
  },

  setGithubPat(courseId: string, payload: SetGithubPatRequest) {
    return apiRequest<void>(`/api/v1/courses/${courseId}/github-pat`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  clearGithubPat(courseId: string) {
    return apiRequest<void>(`/api/v1/courses/${courseId}/github-pat`, {
      method: 'DELETE',
    })
  },

  createBlock(courseId: string, payload: CreateBlockRequest) {
    return apiRequest(`/api/v1/courses/${courseId}/blocks`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateBlock(courseId: string, blockId: string, payload: UpdateBlockRequest) {
    return apiRequest(`/api/v1/courses/${courseId}/blocks/${blockId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteBlock(courseId: string, blockId: string) {
    return apiRequest<void>(`/api/v1/courses/${courseId}/blocks/${blockId}`, {
      method: 'DELETE',
    })
  },

  createMaterial(courseId: string, payload: CreateMaterialRequest) {
    return apiRequest<MaterialResponse>(`/api/v1/courses/${courseId}/materials`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateMaterial(materialId: string, payload: UpdateMaterialRequest) {
    return apiRequest<MaterialResponse>(`/api/v1/materials/${materialId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteMaterial(materialId: string) {
    return apiRequest<void>(`/api/v1/materials/${materialId}`, {
      method: 'DELETE',
    })
  },

  createAssignment(courseId: string, payload: CreateAssignmentRequest) {
    return apiRequest<AssignmentResponse>(`/api/v1/courses/${courseId}/assignments`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateAssignment(assignmentId: string, payload: UpdateAssignmentRequest) {
    return apiRequest<AssignmentResponse>(`/api/v1/assignments/${assignmentId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteAssignment(assignmentId: string) {
    return apiRequest<void>(`/api/v1/assignments/${assignmentId}`, {
      method: 'DELETE',
    })
  },

  publishAssignment(assignmentId: string) {
    return apiRequest<AssignmentResponse>(`/api/v1/assignments/${assignmentId}/publish`, {
      method: 'POST',
    })
  },

  listAllSubmissions(assignmentId: string) {
    return apiRequest<SubmissionResponse[]>(
      `/api/v1/assignments/${assignmentId}/submissions`,
    )
  },

  listAllCodeAttempts(assignmentId: string) {
    return apiRequest<CodeAttemptResponse[]>(
      `/api/v1/assignments/${assignmentId}/code-attempts`,
    )
  },

  gradeSubmission(submissionId: string, payload: GradeSubmissionRequest) {
    return apiRequest<SubmissionResponse>(`/api/v1/submissions/${submissionId}/grade`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  retryCodeAttempt(attemptId: string) {
    return apiRequest<CodeAttemptResponse>(`/api/v1/code-attempts/${attemptId}/retry`, {
      method: 'POST',
    })
  },

  getGradebook(courseId: string) {
    return apiRequest<CourseGradebookResponse>(
      `/api/v1/courses/${courseId}/gradebook`,
    )
  },

  getMyGradebook(courseId: string) {
    return apiRequest<MyGradebookResponse>(
      `/api/v1/courses/${courseId}/gradebook/me`,
    )
  },

  getGradebookCsvUrl(courseId: string) {
    return `/api/v1/courses/${courseId}/gradebook/csv`
  },

  listEnrollments(courseId: string) {
    return apiRequest<EnrollmentResponse[]>(
      `/api/v1/courses/${courseId}/enrollments`,
    )
  },

  upsertEnrollment(courseId: string, payload: UpsertEnrollmentRequest) {
    return apiRequest<void>(`/api/v1/courses/${courseId}/enrollments`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  deleteEnrollment(courseId: string, userId: string) {
    return apiRequest<void>(`/api/v1/courses/${courseId}/enrollments/${userId}`, {
      method: 'DELETE',
    })
  },

  upsertEnrollmentsByEmail(courseId: string, payload: UpsertEnrollmentsByEmailRequest) {
    return apiRequest<UpsertEnrollmentsByEmailResponse>(
      `/api/v1/courses/${courseId}/enrollments/by-email`,
      { method: 'POST', body: JSON.stringify(payload) },
    )
  },

  deleteCourse(courseId: string) {
    return apiRequest<void>(`/api/v1/courses/${courseId}`, { method: 'DELETE' })
  },

  listGroups(courseId: string) {
    return apiRequest<GroupResponse[]>(`/api/v1/courses/${courseId}/groups`)
  },

  createGroup(courseId: string, payload: CreateGroupRequest) {
    return apiRequest<GroupResponse>(`/api/v1/courses/${courseId}/groups`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateGroup(courseId: string, groupId: string, payload: UpdateGroupRequest) {
    return apiRequest<GroupResponse>(`/api/v1/courses/${courseId}/groups/${groupId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteGroup(courseId: string, groupId: string) {
    return apiRequest<void>(`/api/v1/courses/${courseId}/groups/${groupId}`, {
      method: 'DELETE',
    })
  },

  addGroupMember(courseId: string, groupId: string, payload: AddGroupMemberRequest) {
    return apiRequest<GroupResponse>(
      `/api/v1/courses/${courseId}/groups/${groupId}/members`,
      { method: 'POST', body: JSON.stringify(payload) },
    )
  },

  removeGroupMember(courseId: string, groupId: string, memberUserId: string) {
    return apiRequest<GroupResponse>(
      `/api/v1/courses/${courseId}/groups/${groupId}/members/${memberUserId}`,
      { method: 'DELETE' },
    )
  },

  createItem(courseId: string, payload: CreateItemRequest) {
    return apiRequest<ItemResponse>(`/api/v1/courses/${courseId}/items`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateItem(courseId: string, itemId: string, payload: UpdateItemRequest) {
    return apiRequest<ItemResponse>(`/api/v1/courses/${courseId}/items/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteItem(courseId: string, itemId: string) {
    return apiRequest<void>(`/api/v1/courses/${courseId}/items/${itemId}`, {
      method: 'DELETE',
    })
  },
}
