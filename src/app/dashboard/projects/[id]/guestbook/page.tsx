"use client";

import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import {
  GUESTBOOK_PAGE_SIZES,
  type GuestbookPage,
  type GuestbookPageSize,
} from "@/types/guestbook";

export default function ProjectGuestbookManagePage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [pageData, setPageData] = useState<GuestbookPage | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<GuestbookPageSize>(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const loadEntries = useCallback(async () => {
    const res = await apiFetch<GuestbookPage>(
      `/api/projects/${params.id}/guestbook?page=${page}&size=${pageSize}`,
    );
    setPageData(res.data);
  }, [params.id, page, pageSize]);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    setLoading(true);
    loadEntries()
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [loadEntries, router]);

  async function handleDelete(entryId: number) {
    if (!confirm("이 방명록을 삭제할까요?")) return;
    setDeletingId(entryId);
    setError(null);
    try {
      await apiFetch(`/api/projects/${params.id}/guestbook/${entryId}`, {
        method: "DELETE",
      });
      await loadEntries();
    } catch (err) {
      setError(err instanceof Error ? err.message : "삭제 실패");
    } finally {
      setDeletingId(null);
    }
  }

  const entries = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;

  if (loading && !pageData) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-xl px-6 py-10 sm:py-14">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1
            className="text-3xl font-light"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            방명록 관리
          </h1>
          <p className="mt-1 text-sm text-muted">총 {totalElements}건</p>
        </div>
        <label className="flex items-center gap-2 text-xs text-muted">
          보기
          <select
            value={pageSize}
            onChange={(e) => {
              setPage(0);
              setPageSize(Number(e.target.value) as GuestbookPageSize);
            }}
            className="rounded-lg border border-accent/30 bg-white px-2 py-1 text-xs"
          >
            {GUESTBOOK_PAGE_SIZES.map((size) => (
              <option key={size} value={size}>
                {size}개씩
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      {entries.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-accent/30 bg-white/50 p-8 text-center text-sm text-muted">
          아직 등록된 방명록이 없습니다.
        </p>
      ) : (
        <ul className="space-y-4">
          {entries.map((entry) => (
            <li
              key={entry.id}
              className="rounded-2xl border border-accent/20 bg-white/70 p-5"
            >
              <div className="mb-2 flex items-start justify-between gap-3">
                <div>
                  <p className="font-medium">{entry.guestName}</p>
                  <p className="text-xs text-muted">
                    {formatEntryTime(entry.createdAt)} · ♥ {entry.likeCount}
                  </p>
                </div>
                <button
                  type="button"
                  disabled={deletingId === entry.id}
                  onClick={() => handleDelete(entry.id)}
                  className="shrink-0 rounded-full border border-red-200 px-3 py-1 text-xs text-red-600 transition hover:bg-red-50 disabled:opacity-60"
                >
                  {deletingId === entry.id ? "삭제 중..." : "삭제"}
                </button>
              </div>
              <p className="whitespace-pre-wrap text-sm leading-relaxed text-[#2C2420]">
                {entry.message}
              </p>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="mt-8 flex items-center justify-center gap-3">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
            className="rounded-full border border-accent/30 px-4 py-2 text-xs disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-xs text-muted">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((prev) => prev + 1)}
            className="rounded-full border border-accent/30 px-4 py-2 text-xs disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </main>
  );
}

function formatEntryTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}
