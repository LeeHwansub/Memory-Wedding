"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { AddressSearchField } from "@/components/ui/AddressSearchField";
import { apiFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import type { WeddingProject } from "@/types";

export default function NewProjectPage() {
  const router = useRouter();
  const [groomName, setGroomName] = useState("");
  const [brideName, setBrideName] = useState("");
  const [weddingAt, setWeddingAt] = useState("");
  const [venueName, setVenueName] = useState("");
  const [venueAddress, setVenueAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
    }
  }, [router]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const res = await apiFetch<WeddingProject>("/api/projects", {
        method: "POST",
        body: JSON.stringify({
          groomName,
          brideName,
          weddingAt: new Date(weddingAt).toISOString(),
          venueName: venueName || undefined,
          venueAddress: venueAddress || undefined,
        }),
      });

      if (!res.data) {
        throw new Error(res.message ?? "생성 실패");
      }

      router.replace(`/dashboard/projects/${res.data.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "생성에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto min-h-screen max-w-xl px-6 py-16">
      <Link href="/dashboard" className="text-sm text-muted hover:underline">
        ← 대시보드
      </Link>
      <h1
        className="mt-4 mb-8 text-3xl font-light"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        Wedding Project 만들기
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

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
        >
          {submitting ? "생성 중..." : "생성하기"}
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
