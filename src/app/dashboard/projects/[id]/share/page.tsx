"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import type { WeddingProject } from "@/types";

export default function ShareProjectPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [project, setProject] = useState<WeddingProject | null>(null);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  const inviteUrl = useMemo(() => {
    if (!project) return "";
    const origin = typeof window !== "undefined" ? window.location.origin : "";
    return `${origin}${project.guestPath ?? `/w/${project.slug}`}`;
  }, [project]);

  const qrUrl = useMemo(() => {
    if (!inviteUrl) return "";
    return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(inviteUrl)}`;
  }, [inviteUrl]);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    apiFetch<WeddingProject>(`/api/projects/${params.id}`)
      .then((res) => setProject(res.data))
      .finally(() => setLoading(false));
  }, [params.id, router]);

  async function copyLink() {
    if (!inviteUrl) return;
    await navigator.clipboard.writeText(inviteUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  async function toggleInvite(active: boolean) {
    const res = await apiFetch<WeddingProject>(`/api/projects/${params.id}/invite-link`, {
      method: "PATCH",
      body: JSON.stringify({ active }),
    });
    setProject(res.data);
  }

  if (loading || !project) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-xl px-6 py-16">
      <Link
        href={`/dashboard/projects/${params.id}`}
        className="text-sm text-muted hover:underline"
      >
        ← Project
      </Link>
      <h1
        className="mt-4 mb-8 text-3xl font-light"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        하객 초대
      </h1>

      <label className="mb-6 block text-sm">
        <span className="mb-1 block text-muted">초대 링크</span>
        <div className="flex gap-2">
          <input
            readOnly
            value={inviteUrl}
            className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3 text-sm"
          />
          <button
            type="button"
            onClick={copyLink}
            className="rounded-xl border border-accent/40 px-4 text-sm whitespace-nowrap transition hover:bg-accent-soft"
          >
            {copied ? "복사됨" : "복사"}
          </button>
        </div>
      </label>

      <div className="mb-8 flex justify-center">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={qrUrl}
          alt="Invite QR"
          width={240}
          height={240}
          className="rounded-2xl border border-accent/20 bg-white p-3"
        />
      </div>

      <div className="flex gap-3">
        <button
          type="button"
          onClick={() => toggleInvite(true)}
          disabled={project.inviteActive}
          className="rounded-full bg-accent px-5 py-2 text-sm text-white disabled:opacity-50"
        >
          링크 활성화
        </button>
        <button
          type="button"
          onClick={() => toggleInvite(false)}
          disabled={!project.inviteActive}
          className="rounded-full border border-accent/40 px-5 py-2 text-sm disabled:opacity-50"
        >
          링크 비활성화
        </button>
      </div>

      <p className="mt-6 text-xs text-muted">
        현재 상태: {project.inviteActive ? "활성" : "비활성"} · 청첩장 페이지는 다음 단계에서 연결됩니다.
      </p>
    </main>
  );
}
