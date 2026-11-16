export type FileType = "PHOTO" | "VIDEO";
export type UploadStatus = "PENDING" | "UPLOADING" | "COMPLETED" | "FAILED";

export type UploadFile = {
  id: number;
  fileType: FileType;
  guestName: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  uploadStatus: UploadStatus;
  failureReason?: string | null;
  driveSynced: boolean;
  createdAt: string;
};

export type UploadPage = {
  content: UploadFile[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type UploadGuestFolder = {
  guestName: string;
  fileCount: number;
};

export type UploadFolderTree = {
  photoCount: number;
  videoCount: number;
  photos: UploadGuestFolder[];
  videos: UploadGuestFolder[];
};
