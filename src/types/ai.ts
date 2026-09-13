export type AiJobStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

/** Notion FR-AI-004 장면 분류 */
export type SceneCategory =
  | "ENTRANCE"
  | "SONG"
  | "GROUP_PHOTO"
  | "RECEPTION"
  | "OTHER";

export type AiJob = {
  id: number;
  status: AiJobStatus;
  totalFiles: number;
  processedFiles: number;
  errorMessage?: string | null;
  analyzerMode?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt?: string | null;
};

export type AiPhotoResult = {
  id: number;
  uploadFileId: number;
  originalFilename: string;
  guestName: string;
  contentPath: string;
  sceneCategory: SceneCategory;
  bestShot: boolean;
  confidence?: number | null;
  people?: string[];
  objects?: string[];
  place?: string | null;
  analyzedAt?: string | null;
};

export type AiDashboard = {
  latestJob: AiJob | null;
  results: AiPhotoResult[];
  bestShots: AiPhotoResult[];
  analyzerMode: "gemini" | "mock" | string;
};

export const SCENE_LABELS: Record<SceneCategory, string> = {
  ENTRANCE: "입장",
  SONG: "축가",
  GROUP_PHOTO: "단체사진",
  RECEPTION: "피로연",
  OTHER: "기타",
};
