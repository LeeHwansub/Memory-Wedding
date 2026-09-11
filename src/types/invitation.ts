export type AccountEntry = {
  relation: string;
  bankName: string;
  accountNumber: string;
};

export type GalleryLayout = "SLIDER" | "COLLAGE" | "VERTICAL";
export type MediaDisplaySize = "SM" | "MD" | "LG" | "FULL";
export type MainPhotoPlacement = "TOP" | "MIDDLE";
export type InvitationMediaType = "MAIN" | "GALLERY";

export type InvitationMedia = {
  id: number;
  mediaType: InvitationMediaType | string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  sortOrder: number;
  contentPath: string;
};

export type InvitationMediaSettings = {
  galleryLayout: GalleryLayout;
  galleryColumns: number;
  galleryImageSize: MediaDisplaySize;
  mainPhotoSize: MediaDisplaySize;
  mainPhotoPlacement: MainPhotoPlacement;
  mainBrightness: number;
  mainSaturation: number;
  mainFocalX: number;
  mainFocalY: number;
  mainPhoto?: InvitationMedia | null;
  gallery: InvitationMedia[];
};

export type Invitation = {
  id: number;
  projectId: number;
  title?: string | null;
  greetingMessage?: string | null;
  published: boolean;
  venueName?: string | null;
  venueAddress?: string | null;
  mapUrl?: string | null;
  accounts: AccountEntry[];
  groomName: string;
  brideName: string;
  weddingAt: string;
  slug: string;
  guestPath: string;
} & InvitationMediaSettings;

export type PublicInvitation = {
  title?: string | null;
  greetingMessage?: string | null;
  mapUrl?: string | null;
  accounts: AccountEntry[];
  groomName: string;
  brideName: string;
  weddingAt: string;
  venueName?: string | null;
  venueAddress?: string | null;
  slug: string;
} & InvitationMediaSettings;
