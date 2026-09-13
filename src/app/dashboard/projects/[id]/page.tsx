"use client";

import Link from "next/link";
import { useParams, usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useProjectPreview } from "@/components/shell/ProjectShell";
import { apiFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import { formatWeddingDateTime } from "@/lib/datetime";
import { isProjectNavActive, PROJECT_NAV } from "@/lib/project-nav";
import type { WeddingProject } from "@/types";

const ICONS: Record<string, React.ReactNode> = {
  home: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1v-10.5Z" />
    </svg>
  ),
  edit: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Z" />
      <path d="m13.5 6.5 3 3" />
    </svg>
  ),
  invitation: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="m3 7 9 6 9-6" />
    </svg>
  ),
  design: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <circle cx="9" cy="10" r="1.5" />
      <path d="m21 15-4.5-4.5L8 19" />
      <path d="M14 8h4M14 11h2" />
    </svg>
  ),
  guestbook: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M7 4h10a2 2 0 0 1 2 2v14l-4-2-4 2-4-2-4 2V6a2 2 0 0 1 2-2Z" />
      <path d="M8 9h8M8 13h5" />
    </svg>
  ),
  gallery: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <circle cx="9" cy="10" r="1.5" />
      <path d="m21 15-4.5-4.5L8 19" />
    </svg>
  ),
  ai: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <circle cx="12" cy="12" r="3" />
      <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4" />
    </svg>
  ),
  share: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <circle cx="6" cy="12" r="2.5" />
      <circle cx="18" cy="6" r="2.5" />
      <circle cx="18" cy="18" r="2.5" />
      <path d="m8.2 10.8 7.6-3.6M8.2 13.2l7.6 3.6" />
    </svg>
  ),
  preview: (
    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="8" y="2" width="8" height="20" rx="2" />
      <path d="M11 18h2" />
    </svg>
  ),
};

export default function ProjectDetailPage() {
  const params = useParams<{ id: string }>();
  const pathname = usePathname();
  const router = useRouter();
  const { openPreview } = useProjectPreview();
  const [project, setProject] = useState<WeddingProject | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    apiFetch<WeddingProject>(`/api/projects/${params.id}`)
      .then((res) => setProject(res.data))
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [params.id, router]);

  async function handleDelete() {
    if (!confirm("Wedding Project를 삭제할까요?")) return;

    try {
      await apiFetch(`/api/projects/${params.id}`, { method: "DELETE" });
      router.replace("/dashboard");
    } catch (err) {
      alert(err instanceof Error ? err.message : "삭제 실패");
    }
  }

  if (loading) {
    return (
      <main className="flex min-h-[50vh] items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  if (error || !project) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16">
        <p className="text-red-600">{error ?? "Project를 찾을 수 없습니다."}</p>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-3xl px-6 py-10 sm:py-14">
      <h1
        className="mb-2 text-3xl font-light sm:text-4xl"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        {project.groomName} ♥ {project.brideName}
      </h1>
      <p className="mb-8 text-muted">
        {formatWeddingDateTime(project.weddingAt)}
        {project.venueName ? ` · ${project.venueName}` : ""}
      </p>

      <div className="mb-10 grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
        {PROJECT_NAV.filter((item) => item.key !== "home").map((item) => {
          if (item.action === "preview") {
            return (
              <TileButton
                key={item.key}
                label={item.label}
                icon={ICONS[item.key]}
                onClick={openPreview}
              />
            );
          }
          const href = item.href?.(String(project.id)) ?? "#";
          const active = isProjectNavActive(item, pathname, String(project.id));
          return (
            <TileLink
              key={item.key}
              href={href}
              label={item.label}
              icon={ICONS[item.key]}
              active={active}
            />
          );
        })}
      </div>

      <dl className="mb-8 space-y-3 rounded-2xl border border-accent/20 bg-white/60 p-6 text-sm">
        <Row label="상태" value={project.status} />
        <Row label="Slug" value={project.slug} />
        <Row label="하객 경로" value={project.guestPath ?? `/w/${project.slug}`} />
        <Row label="주소" value={project.venueAddress || "-"} />
        <Row label="초대 링크" value={project.inviteActive ? "활성" : "비활성"} />
      </dl>

      <button
        type="button"
        onClick={handleDelete}
        className="rounded-full border border-red-300 px-5 py-2 text-sm text-red-600 transition hover:bg-red-50"
      >
        Project 삭제
      </button>
    </main>
  );
}

function TileLink({
  href,
  label,
  icon,
  active,
}: {
  href: string;
  label: string;
  icon: React.ReactNode;
  active?: boolean;
}) {
  return (
    <Link
      href={href}
      className={`flex aspect-square flex-col items-center justify-center gap-2 rounded-2xl border text-center transition ${
        active
          ? "border-foreground bg-foreground text-background"
          : "border-accent/20 bg-white/70 text-foreground hover:border-accent/40 hover:bg-white"
      }`}
    >
      <span className={active ? "text-background" : "text-[#8B7355]"}>{icon}</span>
      <span className="text-xs font-medium sm:text-sm">{label}</span>
    </Link>
  );
}

function TileButton({
  label,
  icon,
  onClick,
}: {
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex aspect-square flex-col items-center justify-center gap-2 rounded-2xl border border-accent/20 bg-white/70 text-center transition hover:border-accent/40 hover:bg-white"
    >
      <span className="text-[#8B7355]">{icon}</span>
      <span className="text-xs font-medium sm:text-sm">{label}</span>
    </button>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-muted">{label}</dt>
      <dd className="text-right break-all">{value}</dd>
    </div>
  );
}
