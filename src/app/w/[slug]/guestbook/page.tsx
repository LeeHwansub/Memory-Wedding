"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { GuestbookLikeButton } from "@/components/guestbook/GuestbookLikeButton";
import { apiPublicFetch } from "@/lib/api";
import { getGuestbookClientKey } from "@/lib/guestbook-client";
import {
  GUESTBOOK_PAGE_SIZES,
  type GuestbookEntry,
  type GuestbookPage,
  type GuestbookPageSize,
} from "@/types/guestbook";

export default function GuestGuestbookPage() {
  const params = useParams<{ slug: string }>();
  const [pageData, setPageData] = useState<GuestbookPage | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<GuestbookPageSize>(10);
  const [guestName, setGuestName] = useState("");
  const [message, setMessage] = useState("");
  const [editName, setEditName] = useState("");
  const [editMessage, setEditMessage] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const myEntry = pageData?.myEntry ?? null;

  const loadEntries = useCallback(async () => {
    const clientKey = getGuestbookClientKey();
    const res = await apiPublicFetch<GuestbookPage>(
      `/api/public/w/${params.slug}/guestbook?page=${page}&size=${pageSize}&clientKey=${encodeURIComponent(clientKey)}`,
    );
    setPageData(res.data);
    return res.data;
  }, [params.slug, page, pageSize]);

  useEffect(() => {
    try {
      getGuestbookClientKey();
    } catch {
      // ignore until user interacts
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    setError(null);
    loadEntries()
      .catch((err) => setError(err instanceof Error ? err.message : "불러오기 실패"))
      .finally(() => setLoading(false));
  }, [loadEntries]);

  function startEdit(entry: GuestbookEntry) {
    setEditingId(entry.id);
    setEditName(entry.guestName);
    setEditMessage(entry.message);
    setError(null);
    setSuccess(null);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditName("");
    setEditMessage("");
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      const clientKey = getGuestbookClientKey();
      await apiPublicFetch<GuestbookEntry>(`/api/public/w/${params.slug}/guestbook`, {
        method: "POST",
        body: JSON.stringify({
          guestName: guestName.trim(),
          message: message.trim(),
          clientKey,
        }),
      });
      setGuestName("");
      setMessage("");
      setSuccess("따뜻한 마음이 전달되었습니다.");
      if (page === 0) {
        await loadEntries();
      } else {
        setPage(0);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "등록에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleUpdate(event: FormEvent) {
    event.preventDefault();
    if (editingId == null) return;

    setSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      const clientKey = getGuestbookClientKey();
      await apiPublicFetch<GuestbookEntry>(
        `/api/public/w/${params.slug}/guestbook/${editingId}`,
        {
          method: "PUT",
          body: JSON.stringify({
            guestName: editName.trim(),
            message: editMessage.trim(),
            clientKey,
          }),
        },
      );
      setSuccess("방명록이 수정되었습니다.");
      cancelEdit();
      await loadEntries();
    } catch (err) {
      setError(err instanceof Error ? err.message : "수정에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  const entries = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;

  if (loading && !pageData) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#FAF8F5]">
        <p className="text-muted">방명록을 불러오는 중...</p>
      </main>
    );
  }

  if (error && !pageData) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#FAF8F5] px-6">
        <div className="max-w-md text-center">
          <h1
            className="mb-3 text-2xl font-light"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            방명록을 볼 수 없습니다
          </h1>
          <p className="mb-6 text-sm text-muted">{error}</p>
          <Link href={`/w/${params.slug}`} className="text-sm underline">
            청첩장으로 돌아가기
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#FAF8F5] text-[#2C2420]">
      <section className="mx-auto max-w-lg px-6 py-14">
        <Link
          href={`/w/${params.slug}`}
          className="text-sm text-[#8A7F78] transition hover:underline"
        >
          ← 청첩장
        </Link>

        <div className="mt-6 mb-10 text-center">
          <p
            className="mb-3 text-[11px] tracking-[0.28em] text-[#C9A87C] uppercase"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            Guestbook
          </p>
          <span className="mx-auto mb-5 block h-px w-10 bg-[#C9A87C]/50" aria-hidden />
          <h1
            className="text-3xl font-light"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            방명록
          </h1>
          <p className="mt-2 text-sm text-[#8A7F78]">
            {myEntry
              ? "이미 작성하셨습니다. 아래 내 글에서 수정할 수 있습니다"
              : "따뜻한 한마디를 남겨 주세요 (1회 작성)"}
          </p>
        </div>

        {!myEntry && (
          <form onSubmit={handleCreate} className="mb-12 space-y-4">
            <label className="block text-sm">
              <span className="mb-1 block text-[#8A7F78]">이름</span>
              <input
                required
                maxLength={50}
                value={guestName}
                onChange={(e) => setGuestName(e.target.value)}
                placeholder="홍길동"
                className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block text-[#8A7F78]">메시지</span>
              <textarea
                required
                maxLength={1000}
                rows={4}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="두 분의 결혼을 진심으로 축하드립니다."
                className="w-full resize-y rounded-xl border border-accent/30 bg-white px-4 py-3 leading-relaxed"
              />
            </label>

            {error && <p className="text-sm text-red-600">{error}</p>}
            {success && <p className="text-sm text-green-700">{success}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
            >
              {submitting ? "등록 중..." : "축하 메시지 남기기"}
            </button>
          </form>
        )}

        {myEntry && (error || success) && (
          <div className="mb-6 space-y-1 text-center text-sm">
            {error && <p className="text-red-600">{error}</p>}
            {success && <p className="text-green-700">{success}</p>}
          </div>
        )}

        <div className="mb-4 flex items-center justify-between gap-3">
          <p className="text-xs tracking-[0.2em] text-[#8A7F78] uppercase">
            Messages · {totalElements}
          </p>
          <label className="flex items-center gap-2 text-xs text-[#8A7F78]">
            보기
            <select
              value={pageSize}
              onChange={(e) => {
                setPage(0);
                setPageSize(Number(e.target.value) as GuestbookPageSize);
              }}
              className="rounded-lg border border-accent/30 bg-white px-2 py-1 text-xs text-[#2C2420]"
            >
              {GUESTBOOK_PAGE_SIZES.map((size) => (
                <option key={size} value={size}>
                  {size}개씩
                </option>
              ))}
            </select>
          </label>
        </div>

        {entries.length === 0 ? (
          <p className="py-8 text-center text-sm text-[#8A7F78]">
            아직 남겨진 메시지가 없습니다.
          </p>
        ) : (
          <ul className="space-y-5">
            {entries.map((entry) => {
              const isEditing = editingId === entry.id;
              return (
                <li
                  key={entry.id}
                  className="border-b border-[#C9A87C]/20 pb-5 last:border-0 last:pb-0"
                >
                  <div className="mb-2 flex items-baseline justify-between gap-3">
                    <p className="text-sm font-medium tracking-wide">
                      {entry.guestName}
                      {entry.mine && (
                        <span className="ml-2 text-[11px] font-normal text-[#C9A87C]">
                          내 글
                        </span>
                      )}
                    </p>
                    <div className="flex items-center gap-3">
                      <time className="shrink-0 text-[11px] text-[#8A7F78]">
                        {formatEntryTime(entry.createdAt)}
                      </time>
                      <GuestbookLikeButton
                        slug={params.slug}
                        entry={entry}
                        onUpdated={(updated) => {
                          setPageData((prev) =>
                            prev
                              ? {
                                  ...prev,
                                  content: prev.content.map((item) =>
                                    item.id === updated.id ? { ...item, ...updated } : item,
                                  ),
                                  myEntry:
                                    prev.myEntry?.id === updated.id
                                      ? { ...prev.myEntry, ...updated }
                                      : prev.myEntry,
                                }
                              : prev,
                          );
                        }}
                      />
                    </div>
                  </div>

                  {isEditing ? (
                    <form onSubmit={handleUpdate} className="mt-3 space-y-3">
                      <input
                        required
                        maxLength={50}
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        className="w-full rounded-xl border border-accent/30 bg-white px-4 py-2.5 text-sm"
                      />
                      <textarea
                        required
                        maxLength={1000}
                        rows={4}
                        value={editMessage}
                        onChange={(e) => setEditMessage(e.target.value)}
                        className="w-full resize-y rounded-xl border border-accent/30 bg-white px-4 py-3 text-sm leading-relaxed"
                      />
                      <div className="flex gap-2">
                        <button
                          type="submit"
                          disabled={submitting}
                          className="rounded-full bg-accent px-4 py-2 text-xs text-white disabled:opacity-60"
                        >
                          {submitting ? "저장 중..." : "저장"}
                        </button>
                        <button
                          type="button"
                          onClick={cancelEdit}
                          disabled={submitting}
                          className="rounded-full border border-accent/30 px-4 py-2 text-xs text-[#8A7F78]"
                        >
                          취소
                        </button>
                      </div>
                    </form>
                  ) : (
                    <>
                      <p className="whitespace-pre-wrap text-sm leading-7 text-[#2C2420]/90">
                        {entry.message}
                      </p>
                      {entry.mine && (
                        <div className="mt-3 flex justify-end">
                          <button
                            type="button"
                            onClick={() => startEdit(entry)}
                            className="text-xs tracking-wide text-[#C9A87C] underline-offset-4 transition hover:underline"
                          >
                            수정
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </li>
              );
            })}
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
            <span className="text-xs text-[#8A7F78]">
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
      </section>
    </main>
  );
}

function formatEntryTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}
