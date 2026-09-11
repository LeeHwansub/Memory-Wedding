"use client";

import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { InvitationPreviewModal } from "@/components/preview/InvitationPreviewModal";
import { apiFetch } from "@/lib/api";
import { PROJECT_NAV } from "@/lib/project-nav";
import type { WeddingProject } from "@/types";

type PreviewContextValue = {
  openPreview: () => void;
};

const PreviewContext = createContext<PreviewContextValue | null>(null);

export function useProjectPreview() {
  const ctx = useContext(PreviewContext);
  if (!ctx) {
    throw new Error("useProjectPreview must be used within ProjectShell");
  }
  return ctx;
}

export function ProjectShell({ children }: { children: React.ReactNode }) {
  const params = useParams<{ id: string }>();
  const pathname = usePathname();
  const projectId = params.id;
  const [project, setProject] = useState<WeddingProject | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);

  useEffect(() => {
    if (!projectId) return;
    apiFetch<WeddingProject>(`/api/projects/${projectId}`)
      .then((res) => setProject(res.data))
      .catch(() => setProject(null));
  }, [projectId]);

  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  const openPreview = useCallback(() => setPreviewOpen(true), []);
  const previewValue = useMemo(() => ({ openPreview }), [openPreview]);

  const title = project
    ? `${project.groomName} ♥ ${project.brideName}`
    : "Project";

  return (
    <PreviewContext.Provider value={previewValue}>
      <header className="sticky top-0 z-40 border-b border-accent/20 bg-[#FAF8F5]/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between gap-3 px-4 sm:px-6">
          <div className="flex min-w-0 items-center gap-3">
            <Link
              href="/dashboard"
              className="shrink-0 text-xs tracking-[0.12em] text-muted uppercase"
              style={{ fontFamily: "var(--font-playfair), serif" }}
            >
              Memory
            </Link>
            <span className="hidden text-muted sm:inline" aria-hidden>
              /
            </span>
            <Link
              href={`/dashboard/projects/${projectId}`}
              className="truncate text-sm font-medium"
            >
              {title}
            </Link>
          </div>

          <nav className="hidden items-center gap-1 lg:flex">
            {PROJECT_NAV.map((item) => {
              const active =
                item.key === "home"
                  ? pathname === `/dashboard/projects/${projectId}`
                  : item.href
                    ? pathname.startsWith(item.href(projectId))
                    : false;
              if (item.action === "preview") {
                return (
                  <button
                    key={item.key}
                    type="button"
                    onClick={openPreview}
                    className="rounded-full px-3 py-1.5 text-xs text-muted transition hover:bg-accent-soft hover:text-foreground"
                  >
                    {item.label}
                  </button>
                );
              }
              const href = item.href?.(projectId) ?? "#";
              return (
                <Link
                  key={item.key}
                  href={href}
                  className={`rounded-full px-3 py-1.5 text-xs transition ${
                    active
                      ? "bg-foreground text-background"
                      : "text-muted hover:bg-accent-soft hover:text-foreground"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>

          <button
            type="button"
            className="rounded-full border border-accent/30 px-3 py-1.5 text-xs lg:hidden"
            onClick={() => setMenuOpen((v) => !v)}
            aria-expanded={menuOpen}
            aria-label="메뉴"
          >
            메뉴
          </button>
        </div>

        {menuOpen && (
          <div className="border-t border-accent/15 bg-[#FAF8F5] lg:hidden">
            <nav className="mx-auto grid max-w-5xl grid-cols-3 gap-2 px-4 py-3 sm:grid-cols-4">
              {PROJECT_NAV.map((item) => {
                if (item.action === "preview") {
                  return (
                    <button
                      key={item.key}
                      type="button"
                      onClick={() => {
                        setMenuOpen(false);
                        openPreview();
                      }}
                      className="rounded-xl border border-accent/20 bg-white/80 px-2 py-3 text-center text-xs"
                    >
                      {item.label}
                    </button>
                  );
                }
                const href = item.href?.(projectId) ?? "#";
                const active =
                  item.key === "home"
                    ? pathname === `/dashboard/projects/${projectId}`
                    : pathname.startsWith(href);
                return (
                  <Link
                    key={item.key}
                    href={href}
                    className={`rounded-xl border px-2 py-3 text-center text-xs ${
                      active
                        ? "border-foreground bg-foreground text-background"
                        : "border-accent/20 bg-white/80"
                    }`}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>
        )}
      </header>

      {children}

      <InvitationPreviewModal
        open={previewOpen}
        onClose={() => setPreviewOpen(false)}
        slug={project?.slug ?? null}
        title={title}
      />
    </PreviewContext.Provider>
  );
}
