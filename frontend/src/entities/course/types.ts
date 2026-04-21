export type RoleInCourse = 'TEACHER' | 'ASSISTANT' | 'STUDENT'
export type CourseItemType = 'MATERIAL' | 'ASSIGNMENT'
export type AssignmentType = 'TEXT' | 'FILE' | 'CODE'
export type WorkType = 'INDIVIDUAL' | 'GROUP'
export type SubmissionOwnerType = 'USER' | 'GROUP'
export type SubmissionStatus = 'SUBMITTED' | 'GRADED'
export type GraderType = 'MANUAL' | 'AUTO'
export type CodeAttemptStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'ERROR'

export interface AttachmentResponse {
  attachmentId: string
  courseId: string
  fileName: string
  contentType?: string
  fileSize: number
  createdAt: string
}

export interface CourseResponse {
  courseId: string
  ownerUserId: string
  title: string
  description?: string
  isVisible: boolean
  githubPatConfigured: boolean
  myRoleInCourse: RoleInCourse
  createdAt: string
  updatedAt: string
}

export interface BlockResponse {
  blockId: string
  courseId: string
  title: string
  position: number
  isVisible: boolean
  createdAt: string
  updatedAt: string
}

export interface ItemResponse {
  itemId: string
  courseId: string
  blockId?: string
  itemType: CourseItemType
  refId: string
  position: number
  isVisible: boolean
  createdAt: string
  updatedAt: string
}

export interface BlockWithItems {
  block: BlockResponse
  items: ItemResponse[]
}

export interface CourseStructureResponse {
  blocks: BlockWithItems[]
  rootItems: ItemResponse[]
}

export interface MyMembershipResponse {
  courseId: string
  userId: string
  roleInCourse: RoleInCourse
}

export interface MaterialResponse {
  materialId: string
  courseId: string
  itemId: string
  title: string
  description?: string
  body?: string
  blockId?: string
  position: number
  isVisible: boolean
  attachments: AttachmentResponse[]
  createdAt: string
  updatedAt: string
}

export interface CodeAssignmentResponse {
  language: 'GO' | 'PYTHON' | 'JAVA'
  repositoryName: string
  repositoryFullName: string
  repositoryUrl: string
  defaultBranch: string
  maxAttempts: number
  repositoryPrivate: boolean
  publishedAt?: string
  starterConfig: string
  privateTestsAttachment?: AttachmentResponse
}

export interface AssignmentResponse {
  assignmentId: string
  courseId: string
  itemId: string
  title: string
  description?: string
  assignmentType: AssignmentType
  workType: WorkType
  deadlineAt?: string
  weight: number
  blockId?: string
  position: number
  isVisible: boolean
  attachments: AttachmentResponse[]
  code?: CodeAssignmentResponse
  createdAt: string
  updatedAt: string
}

export interface CreateCourseRequest {
  title: string
  description?: string
  isVisible: boolean
}

export interface UpdateCourseRequest {
  title?: string
  description?: string
  isVisible?: boolean
}

export interface PresignUploadAttachmentRequest {
  courseId: string
  fileName: string
  contentType?: string
  fileSize: number
}

export interface PresignUploadAttachmentResponse {
  attachmentId: string
  courseId: string
  fileName: string
  contentType?: string
  fileSize: number
  uploadUrl: string
  method: string
  createdAt: string
}

export interface PresignDownloadAttachmentRequest {
  attachmentId: string
}

export interface PresignDownloadAttachmentResponse {
  attachmentId: string
  fileName: string
  contentType?: string
  fileSize: number
  downloadUrl: string
  method: string
}

export interface CreateSubmissionRequest {
  textAnswer?: string
  attachmentIds: string[]
}

export interface SubmissionResponse {
  submissionId: string
  courseId: string
  assignmentId: string
  ownerType: SubmissionOwnerType
  ownerUserId?: string
  ownerGroupId?: string
  ownerGroupName?: string
  memberUserIds: string[]
  textAnswer?: string
  attachments: AttachmentResponse[]
  status: SubmissionStatus
  score?: number
  comment?: string
  gradedByUserId?: string
  gradedAt?: string
  graderType?: GraderType
  autogradeStatus?: string
  externalCheckStatus?: string
  submittedAt: string
  updatedAt: string
}

export interface CreateCodeAttemptRequest {
  pullRequestUrl: string
}

export interface CodeAttemptResponse {
  attemptId: string
  courseId: string
  assignmentId: string
  studentUserId: string
  language: 'GO' | 'PYTHON' | 'JAVA'
  attemptNumber: number
  pullRequestUrl: string
  pullRequestNumber: number
  pullRequestHeadSha?: string
  repositoryFullName: string
  status: CodeAttemptStatus
  score?: number
  comment?: string
  resultSummary?: string
  logObjectKey?: string
  exitCode?: number
  testsPassed?: number
  testsTotal?: number
  scoringMode?: string
  queuedAt: string
  startedAt?: string
  finishedAt?: string
  createdAt: string
  updatedAt: string
}

export interface CodeAttemptLogDownloadResponse {
  attemptId: string
  fileName: string
  downloadUrl: string
  method: string
}

export interface GithubPatStatusResponse {
  configured: boolean
  updatedAt?: string
}

export interface SetGithubPatRequest {
  token: string
}

export interface CreateBlockRequest {
  title: string
  position: number
  isVisible: boolean
}

export interface UpdateBlockRequest {
  title?: string
  position?: number
  isVisible?: boolean
}

export interface CreateMaterialRequest {
  title: string
  description?: string
  body?: string
  blockId?: string
  position: number
  isVisible: boolean
  attachmentIds: string[]
}

export interface UpdateMaterialRequest {
  title?: string
  description?: string
  body?: string
  blockId?: string
  clearBlock: boolean
  position?: number
  isVisible?: boolean
  attachmentIds?: string[]
}

export interface CreateCodeAssignmentRequest {
  repositoryName: string
  repositoryDescription?: string
  language: 'GO' | 'PYTHON' | 'JAVA'
  maxAttempts: number
  privateTestsAttachmentId?: string
  githubPat?: string
}

export interface UpdateCodeAssignmentRequest {
  maxAttempts?: number
  privateTestsAttachmentId?: string
  clearPrivateTestsAttachment: boolean
}

export interface CreateAssignmentRequest {
  title: string
  description?: string
  assignmentType: AssignmentType
  workType: WorkType
  deadlineAt?: string
  weight: number
  blockId?: string
  position: number
  isVisible: boolean
  attachmentIds: string[]
  code?: CreateCodeAssignmentRequest
}

export interface UpdateAssignmentRequest {
  title?: string
  description?: string
  assignmentType?: AssignmentType
  workType?: WorkType
  deadlineAt?: string
  clearDeadline: boolean
  weight?: number
  blockId?: string
  clearBlock: boolean
  position?: number
  isVisible?: boolean
  attachmentIds?: string[]
  code?: UpdateCodeAssignmentRequest
}

export interface GradeSubmissionRequest {
  score: number
  comment?: string
}

export interface EnrollmentResponse {
  userId: string
  roleInCourse: RoleInCourse
  createdAt: string
}

export interface UpsertEnrollmentRequest {
  userId: string
  roleInCourse: RoleInCourse
}

export interface GradebookAssignmentResponse {
  assignmentId: string
  title: string
  assignmentType: AssignmentType
  weight: number
}

export interface GradebookEntryResponse {
  assignmentId: string
  submissionId?: string
  codeAttemptId?: string
  status?: string
  score?: number
  weightedScore?: number
  comment?: string
  ownerType?: SubmissionOwnerType
  ownerGroupId?: string
  ownerGroupName?: string
  submittedAt?: string
  gradedAt?: string
}

export interface GradebookStudentRowResponse {
  userId: string
  entries: GradebookEntryResponse[]
  totalWeightedScore: number
}

export interface CourseGradebookResponse {
  assignments: GradebookAssignmentResponse[]
  rows: GradebookStudentRowResponse[]
}

export interface MyGradebookResponse {
  userId: string
  assignments: GradebookAssignmentResponse[]
  entries: GradebookEntryResponse[]
  totalWeightedScore: number
}

export interface LookupUserDto {
  userId: string
  email: string
}

export interface GroupMemberResponse {
  userId: string
  createdAt: string
}

export interface GroupResponse {
  groupId: string
  courseId: string
  name: string
  members: GroupMemberResponse[]
  createdAt: string
  updatedAt: string
}

export interface CreateGroupRequest {
  name: string
}

export interface UpdateGroupRequest {
  name: string
}

export interface AddGroupMemberRequest {
  userId: string
}

export interface CreateItemRequest {
  blockId?: string
  itemType: CourseItemType
  refId: string
  position: number
  isVisible?: boolean
}

export interface UpdateItemRequest {
  blockId?: string
  itemType?: CourseItemType
  refId?: string
  position?: number
  isVisible?: boolean
}

export interface UpsertEnrollmentsByEmailRequest {
  emails: string[]
  roleInCourse: RoleInCourse
}

export interface UpsertEnrollmentsByEmailResponse {
  addedOrUpdated: number
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
