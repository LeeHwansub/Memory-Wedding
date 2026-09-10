export type MemberRole = "USER" | "ADMIN";

export type ProjectStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";

export type FileType = "PHOTO" | "VIDEO";

export type UploadStatus = "PENDING" | "UPLOADING" | "COMPLETED" | "FAILED";

export type WeddingProject = {
  id: number;
  slug: string;
  groomName: string;
  brideName: string;
  weddingAt: string;
  venueName?: string;
  status: ProjectStatus;
};
