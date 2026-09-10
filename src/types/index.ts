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
  venueName?: string | null;
  venueAddress?: string | null;
  status: ProjectStatus;
  inviteToken?: string | null;
  inviteActive?: boolean;
  guestPath?: string;
  createdAt?: string;
};

export type CreateProjectInput = {
  groomName: string;
  brideName: string;
  weddingAt: string;
  venueName?: string;
  venueAddress?: string;
};

export type UpdateProjectInput = CreateProjectInput & {
  status?: ProjectStatus;
};
