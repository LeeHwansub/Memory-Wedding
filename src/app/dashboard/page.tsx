"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { clearToken, getToken } from "@/lib/auth";

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
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    apiFetch<Member>("/api/members/me")
      .then((res) => setMember(res.data))
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
      <p className="mb-2 text-sm tracking-[0.3em] text-muted uppercase">
        Dashboard
      </p>
      <h1
        className="mb-8 text-4xl font-light"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        안녕하세요, {member?.displayName}님
      </h1>

      <div className="mb-8 rounded-2xl border border-accent/20 bg-white/60 p-6">
        <dl className="space-y-3 text-sm">
          <div className="flex justify-between">
            <dt className="text-muted">이메일</dt>
            <dd>{member?.email}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted">연결된 OAuth</dt>
            <dd>{member?.providers.join(", ")}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted">Role</dt>
            <dd>{member?.role}</dd>
          </div>
        </dl>
      </div>

      <div className="flex gap-4">
        <button
          type="button"
          onClick={handleLogout}
          className="rounded-full border border-accent/40 px-6 py-2 text-sm transition hover:bg-accent-soft"
        >
          로그아웃
        </button>
        <Link
          href="/"
          className="rounded-full bg-accent px-6 py-2 text-sm text-white transition hover:opacity-90"
        >
          홈으로
        </Link>
      </div>
    </main>
  );
}
