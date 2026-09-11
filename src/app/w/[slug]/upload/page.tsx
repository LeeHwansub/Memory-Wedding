"use client";

import Link from "next/link";
import { FormEvent, useMemo, useState } from "react";
import { useParams } from "next/navigation";
import { apiPublicUpload } from "@/lib/api";
import type { UploadFile } from "@/types/upload";

type UploadItem = {
  id: string;
  file: File;
  status: "queued" | "uploading" | "done" | "error";
  message?: string;
};

export default function GuestUploadPage() {
  const params = useParams<{ slug: string }>();
  const [guestName, setGuestName] = useState("");
  const [items, setItems] = useState<UploadItem[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [doneCount, setDoneCount] = useState(0);

  const remaining = useMemo(
    () => items.filter((item) => item.status === "queued" || item.status === "uploading").length,
    [items],
  );

  function onFilesSelected(fileList: FileList | null) {
    if (!fileList?.length) return;
    const next = Array.from(fileList).map((file) => ({
      id: `${file.name}-${file.size}-${file.lastModified}-${Math.random()}`,
      file,
      status: "queued" as const,
    }));
    setItems((prev) => [...prev, ...next]);
    setError(null);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!guestName.trim()) {
      setError("이름을 입력해 주세요.");
      return;
    }
    if (items.length === 0) {
      setError("업로드할 파일을 선택해 주세요.");
      return;
    }

    setSubmitting(true);
    setError(null);
    let success = 0;

    for (const item of items) {
      if (item.status === "done") continue;
      setItems((prev) =>
        prev.map((row) =>
          row.id === item.id ? { ...row, status: "uploading", message: undefined } : row,
        ),
      );

      const formData = new FormData();
      formData.append("guestName", guestName.trim());
      formData.append("file", item.file);

      try {
        await apiPublicUpload<UploadFile>(`/api/public/w/${params.slug}/upload`, formData);
        success += 1;
        setItems((prev) =>
          prev.map((row) =>
            row.id === item.id ? { ...row, status: "done", message: "완료" } : row,
          ),
        );
      } catch (err) {
        const message = err instanceof Error ? err.message : "업로드 실패";
        setItems((prev) =>
          prev.map((row) =>
            row.id === item.id ? { ...row, status: "error", message } : row,
          ),
        );
      }
    }

    setDoneCount((prev) => prev + success);
    setSubmitting(false);
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
            Upload
          </p>
          <span className="mx-auto mb-5 block h-px w-10 bg-[#C9A87C]/50" aria-hidden />
          <h1
            className="text-3xl font-light"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            사진·영상 올리기
          </h1>
          <p className="mt-2 text-sm text-[#8A7F78]">
            소중한 순간을 남겨 주세요 (사진 30MB / 영상 300MB)
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
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
            <span className="mb-1 block text-[#8A7F78]">파일</span>
            <input
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp,image/heic,image/heif,video/mp4,video/quicktime,video/webm,.heic,.heif"
              onChange={(e) => onFilesSelected(e.target.files)}
              className="w-full rounded-xl border border-dashed border-accent/40 bg-white px-4 py-6 text-sm"
            />
          </label>

          {items.length > 0 && (
            <ul className="space-y-2 rounded-2xl border border-accent/20 bg-white/70 p-4 text-sm">
              {items.map((item) => (
                <li key={item.id} className="flex items-start justify-between gap-3">
                  <span className="min-w-0 break-all">{item.file.name}</span>
                  <span
                    className={`shrink-0 text-xs ${
                      item.status === "done"
                        ? "text-green-700"
                        : item.status === "error"
                          ? "text-red-600"
                          : "text-[#8A7F78]"
                    }`}
                  >
                    {item.status === "queued" && "대기"}
                    {item.status === "uploading" && "업로드 중..."}
                    {item.status === "done" && "완료"}
                    {item.status === "error" && (item.message ?? "실패")}
                  </span>
                </li>
              ))}
            </ul>
          )}

          {error && <p className="text-sm text-red-600">{error}</p>}
          {doneCount > 0 && remaining === 0 && (
            <p className="text-sm text-green-700">
              {doneCount}개 업로드 완료 ·{" "}
              <Link href={`/w/${params.slug}/guestbook`} className="underline">
                방명록도 남겨보세요
              </Link>
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
          >
            {submitting ? "업로드 중..." : "업로드하기"}
          </button>
        </form>
      </section>
    </main>
  );
}
