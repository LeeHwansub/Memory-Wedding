import type { ProjectStatus, UploadStatus } from "@/types";
import type { AiJobStatus } from "@/types/ai";

export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  DRAFT: "작성 중",
  ACTIVE: "진행 중",
  ARCHIVED: "보관",
};

export const AI_JOB_STATUS_LABELS: Record<AiJobStatus, string> = {
  PENDING: "대기 중",
  PROCESSING: "진행 중",
  COMPLETED: "완료",
  FAILED: "실패",
};

export const UPLOAD_STATUS_LABELS: Record<UploadStatus, string> = {
  PENDING: "대기",
  UPLOADING: "올리는 중",
  COMPLETED: "완료",
  FAILED: "실패",
};
