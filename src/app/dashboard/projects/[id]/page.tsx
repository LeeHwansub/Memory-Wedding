"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import { formatWeddingDateTime } from "@/lib/datetime";
import type { WeddingProject } from "@/types";

export default function ProjectDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
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
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  if (error || !project) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16">
        <p className="text-red-600">{error ?? "Project를 찾을 수 없습니다."}</p>
        <Link href="/dashboard" className="mt-4 inline-block text-sm underline">
          대시보드로
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-3xl px-6 py-16">
      <Link href="/dashboard" className="text-sm text-muted hover:underline">
        ← 대시보드
      </Link>

      <h1
        className="mt-4 mb-2 text-4xl font-light"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        {project.groomName} ♥ {project.brideName}
      </h1>
      <p className="mb-8 text-muted">
        {formatWeddingDateTime(project.weddingAt)}
        {project.venueName ? ` · ${project.venueName}` : ""}
      </p>

      <div className="mb-8 grid gap-3 sm:grid-cols-2">
        <NavCard href={`/dashboard/projects/${project.id}/edit`} title="기본 정보 수정" />
        <NavCard href={`/dashboard/projects/${project.id}/invitation`} title="청첩장 편집" />
        <NavCard href={`/dashboard/projects/${project.id}/guestbook`} title="방명록 관리" />
        <NavCard href={`/dashboard/projects/${project.id}/share`} title="하객 초대 / QR" />
        <NavCard href={`/w/${project.slug}`} title="청첩장 미리보기" />
      </div>

      <dl className="mb-8 space-y-3 rounded-2xl border border-accent/20 bg-white/60 p-6 text-sm">
        <Row label="상태" value={project.status} />
        <Row label="Slug" value={project.slug} />
        <Row label="하객 경로" value={project.guestPath ?? `/w/${project.slug}`} />
        <Row label="주소" value={project.venueAddress || "-"} />
        <Row
          label="초대 링크"
          value={project.inviteActive ? "활성" : "비활성"}
        />
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

function NavCard({ href, title }: { href: string; title: string }) {
  return (
    <Link
      href={href}
      className="rounded-2xl border border-accent/20 bg-white/70 px-5 py-4 text-sm font-medium transition hover:border-accent/40"
    >
      {title} →
    </Link>
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
