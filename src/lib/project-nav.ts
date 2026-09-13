export type ProjectNavKey =
  | "home"
  | "edit"
  | "invitation"
  | "design"
  | "guestbook"
  | "gallery"
  | "ai"
  | "share"
  | "preview";

export type ProjectNavItem = {
  key: ProjectNavKey;
  label: string;
  href?: (projectId: string) => string;
  action?: "preview";
};

export const PROJECT_NAV: ProjectNavItem[] = [
  { key: "home", label: "홈", href: (id) => `/dashboard/projects/${id}` },
  { key: "edit", label: "기본정보", href: (id) => `/dashboard/projects/${id}/edit` },
  { key: "invitation", label: "청첩장", href: (id) => `/dashboard/projects/${id}/invitation` },
  {
    key: "design",
    label: "디자인",
    href: (id) => `/dashboard/projects/${id}/invitation/design`,
  },
  { key: "guestbook", label: "방명록", href: (id) => `/dashboard/projects/${id}/guestbook` },
  { key: "gallery", label: "갤러리", href: (id) => `/dashboard/projects/${id}/gallery` },
  { key: "ai", label: "AI", href: (id) => `/dashboard/projects/${id}/ai` },
  { key: "share", label: "초대", href: (id) => `/dashboard/projects/${id}/share` },
  { key: "preview", label: "미리보기", action: "preview" },
];

export function isProjectNavActive(
  item: ProjectNavItem,
  pathname: string,
  projectId: string,
): boolean {
  if (item.key === "home") {
    return pathname === `/dashboard/projects/${projectId}`;
  }
  if (item.key === "invitation") {
    return pathname === `/dashboard/projects/${projectId}/invitation`;
  }
  if (!item.href) return false;
  return pathname.startsWith(item.href(projectId));
}

export const DEVICE_PRESETS = [
  { id: "iphone-se", label: "iPhone SE", width: 375, height: 667 },
  { id: "iphone-14", label: "iPhone 14", width: 390, height: 844 },
  { id: "pixel-5", label: "Pixel 5", width: 393, height: 851 },
  { id: "galaxy-s8", label: "Galaxy S8", width: 360, height: 740 },
] as const;
