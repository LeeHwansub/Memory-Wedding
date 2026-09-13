import type { CSSProperties } from "react";
import type {
  GalleryLayout,
  InvitationTemplate,
  MainPhotoPlacement,
  MediaDisplaySize,
} from "@/types/invitation";

export type InvitationThemeTokens = {
  bg: string;
  fg: string;
  accent: string;
  muted: string;
  soft: string;
  line: string;
  heroOverlay: string;
};

export type InvitationTemplateDef = {
  id: InvitationTemplate;
  name: string;
  description: string;
  theme: InvitationThemeTokens;
  presets: {
    galleryLayout: GalleryLayout;
    galleryColumns: number;
    galleryImageSize: MediaDisplaySize;
    mainPhotoSize: MediaDisplaySize;
    mainPhotoPlacement: MainPhotoPlacement;
    mainBrightness: number;
    mainSaturation: number;
  };
};

export const INVITATION_TEMPLATES: InvitationTemplateDef[] = [
  {
    id: "CLASSIC",
    name: "Classic",
    description: "따뜻한 골드 포인트",
    theme: {
      bg: "#FAF8F5",
      fg: "#2C2420",
      accent: "#C9A87C",
      muted: "#8A7F78",
      soft: "#F3EBE0",
      line: "rgba(201,168,124,0.35)",
      heroOverlay: "linear-gradient(to top, rgba(0,0,0,0.45), rgba(0,0,0,0.15), transparent)",
    },
    presets: {
      galleryLayout: "SLIDER",
      galleryColumns: 2,
      galleryImageSize: "MD",
      mainPhotoSize: "LG",
      mainPhotoPlacement: "TOP",
      mainBrightness: 1,
      mainSaturation: 1,
    },
  },
  {
    id: "IVORY",
    name: "Ivory",
    description: "담백한 아이보리·미니멀",
    theme: {
      bg: "#FBF9F4",
      fg: "#2F2A24",
      accent: "#8B7E6A",
      muted: "#9A9188",
      soft: "#F1EBE1",
      line: "rgba(139,126,106,0.3)",
      heroOverlay: "linear-gradient(to top, rgba(47,42,36,0.4), rgba(47,42,36,0.12), transparent)",
    },
    presets: {
      galleryLayout: "VERTICAL",
      galleryColumns: 2,
      galleryImageSize: "LG",
      mainPhotoSize: "FULL",
      mainPhotoPlacement: "TOP",
      mainBrightness: 1.05,
      mainSaturation: 0.9,
    },
  },
  {
    id: "NOIR",
    name: "Noir",
    description: "다크 톤의 단정한 대비",
    theme: {
      bg: "#151311",
      fg: "#F4EFE8",
      accent: "#D4B896",
      muted: "#A79B90",
      soft: "#221E1B",
      line: "rgba(212,184,150,0.35)",
      heroOverlay: "linear-gradient(to top, rgba(0,0,0,0.7), rgba(0,0,0,0.25), transparent)",
    },
    presets: {
      galleryLayout: "SLIDER",
      galleryColumns: 2,
      galleryImageSize: "LG",
      mainPhotoSize: "LG",
      mainPhotoPlacement: "TOP",
      mainBrightness: 0.95,
      mainSaturation: 0.85,
    },
  },
  {
    id: "SAGE",
    name: "Sage",
    description: "세이지 그린 내추럴",
    theme: {
      bg: "#F4F6F2",
      fg: "#243028",
      accent: "#6F8F78",
      muted: "#7E8A82",
      soft: "#E7EEE8",
      line: "rgba(111,143,120,0.35)",
      heroOverlay: "linear-gradient(to top, rgba(36,48,40,0.45), rgba(36,48,40,0.15), transparent)",
    },
    presets: {
      galleryLayout: "COLLAGE",
      galleryColumns: 3,
      galleryImageSize: "MD",
      mainPhotoSize: "MD",
      mainPhotoPlacement: "MIDDLE",
      mainBrightness: 1.05,
      mainSaturation: 1.05,
    },
  },
  {
    id: "ROSE",
    name: "Rose",
    description: "소프트 로즈 포인트",
    theme: {
      bg: "#FBF6F5",
      fg: "#3A2A2C",
      accent: "#C48B8E",
      muted: "#8F7C7E",
      soft: "#F4E8E7",
      line: "rgba(196,139,142,0.35)",
      heroOverlay: "linear-gradient(to top, rgba(58,42,44,0.42), rgba(58,42,44,0.12), transparent)",
    },
    presets: {
      galleryLayout: "SLIDER",
      galleryColumns: 2,
      galleryImageSize: "MD",
      mainPhotoSize: "LG",
      mainPhotoPlacement: "MIDDLE",
      mainBrightness: 1.05,
      mainSaturation: 1.1,
    },
  },
];

export function getInvitationTemplate(
  id?: InvitationTemplate | null,
): InvitationTemplateDef {
  return (
    INVITATION_TEMPLATES.find((item) => item.id === id) ?? INVITATION_TEMPLATES[0]
  );
}

export function invitationThemeStyle(
  id?: InvitationTemplate | null,
): CSSProperties {
  const theme = getInvitationTemplate(id).theme;
  return {
    ["--inv-bg" as string]: theme.bg,
    ["--inv-fg" as string]: theme.fg,
    ["--inv-accent" as string]: theme.accent,
    ["--inv-muted" as string]: theme.muted,
    ["--inv-soft" as string]: theme.soft,
    ["--inv-line" as string]: theme.line,
    ["--inv-hero-overlay" as string]: theme.heroOverlay,
    backgroundColor: theme.bg,
    color: theme.fg,
  };
}
