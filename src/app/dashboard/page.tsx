"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { clearToken, getToken } from "@/lib/auth";
import { formatWeddingDateTime } from "@/lib/datetime";
import type { WeddingProject } from "@/types";

type Member = {
  id: number;
  email: string;
  displayName: string;
  role: string;
  providers: string[];
};

export default function DashboardPage() {
  const router = useRouter();
  const [member, setMember] = useState<Member | null>(null);
  const [projects, setProjects] = useState<WeddingProject[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    Promise.all([
      apiFetch<Member>("/api/members/me"),
      apiFetch<WeddingProject[]>("/api/projects"),
    ])
      .then(([memberRes, projectRes]) => {
        setMember(memberRes.data);
        setProjects(projectRes.data ?? []);
      })
      .catch(() => router.replace("/login"))
      .finally(() => setLoading(false));
  }, [router]);

  function handleLogout() {
    clearToken();
    router.replace("/login");
  }

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-3xl px-6 py-16">
      <div className="mb-8 flex items-start justify-between gap-4">
        <div>
          <p className="mb-2 text-sm tracking-[0.3em] text-muted uppercase">
            Dashboard
          </p>
          <h1
            className="text-4xl font-light"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            안녕하세요, {member?.displayName}님
          </h1>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="rounded-full border border-accent/40 px-4 py-2 text-sm transition hover:bg-accent-soft"
        >
          로그아웃
        </button>
      </div>

      <section className="mb-10">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-medium">Wedding Project</h2>
          {projects.length === 0 && (
            <Link
              href="/dashboard/projects/new"
              className="rounded-full bg-accent px-5 py-2 text-sm text-white transition hover:opacity-90"
            >
              Project 만들기
            </Link>
          )}
        </div>

        {projects.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-accent/30 bg-white/50 p-10 text-center">
            <p className="mb-2 text-muted">아직 등록된 결혼식이 없습니다.</p>
            <p className="text-sm text-muted">첫 Wedding Project를 만들어 보세요.</p>
          </div>
        ) : (
          <ul className="space-y-3">
            {projects.map((project) => (
              <li key={project.id}>
                <Link
                  href={`/dashboard/projects/${project.id}`}
                  className="block rounded-2xl border border-accent/20 bg-white/70 p-5 transition hover:border-accent/40"
                >
                  <div className="mb-1 flex items-center justify-between gap-3">
                    <p className="text-lg font-medium">
                      {project.groomName} ♥ {project.brideName}
                    </p>
                    <span className="text-xs tracking-wide text-muted uppercase">
                      {project.status}
                    </span>
                  </div>
                  <p className="text-sm text-muted">
                    {formatWeddingDateTime(project.weddingAt)}
                    {project.venueName ? ` · ${project.venueName}` : ""}
                  </p>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <p className="text-xs text-muted">MVP: 회원당 Project 1개까지 생성 가능</p>
    </main>
  );
}
