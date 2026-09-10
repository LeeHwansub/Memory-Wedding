"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { AddressSearchField } from "@/components/ui/AddressSearchField";
import { apiFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import type { ProjectStatus, WeddingProject } from "@/types";

export default function EditProjectPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [groomName, setGroomName] = useState("");
  const [brideName, setBrideName] = useState("");
  const [weddingAt, setWeddingAt] = useState("");
  const [venueName, setVenueName] = useState("");
  const [venueAddress, setVenueAddress] = useState("");
  const [status, setStatus] = useState<ProjectStatus>("DRAFT");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    apiFetch<WeddingProject>(`/api/projects/${params.id}`)
      .then((res) => {
        const project = res.data;
        if (!project) return;
        setGroomName(project.groomName);
        setBrideName(project.brideName);
        setWeddingAt(toLocalInputValue(project.weddingAt));
        setVenueName(project.venueName ?? "");
        setVenueAddress(project.venueAddress ?? "");
        setStatus(project.status);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [params.id, router]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await apiFetch(`/api/projects/${params.id}`, {
        method: "PUT",
        body: JSON.stringify({
          groomName,
          brideName,
          weddingAt: new Date(weddingAt).toISOString(),
          venueName: venueName || undefined,
          venueAddress: venueAddress || undefined,
          status,
        }),
      });
      router.replace(`/dashboard/projects/${params.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "수정 실패");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
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
        기본 정보 수정
      </h1>

      <form onSubmit={handleSubmit} className="space-y-4">
        <Field label="신랑 이름" value={groomName} onChange={setGroomName} required />
        <Field label="신부 이름" value={brideName} onChange={setBrideName} required />
        <label className="block text-sm">
          <span className="mb-1 block text-muted">예식 일시</span>
          <input
            type="datetime-local"
            required
            value={weddingAt}
            onChange={(e) => setWeddingAt(e.target.value)}
            className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
          />
        </label>
        <Field label="예식장명" value={venueName} onChange={setVenueName} />
        <AddressSearchField
          value={venueAddress}
          onChange={setVenueAddress}
          onBuildingName={(buildingName) => {
            if (!venueName.trim()) {
              setVenueName(buildingName);
            }
          }}
        />
        <label className="block text-sm">
          <span className="mb-1 block text-muted">상태</span>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as ProjectStatus)}
            className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
          >
            <option value="DRAFT">DRAFT</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="ARCHIVED">ARCHIVED</option>
          </select>
        </label>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
        >
          {submitting ? "저장 중..." : "저장하기"}
        </button>
      </form>
    </main>
  );
}

function Field({
  label,
  value,
  onChange,
  required,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
}) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block text-muted">{label}</span>
      <input
        required={required}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
      />
    </label>
  );
}

function toLocalInputValue(iso: string) {
  const date = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
