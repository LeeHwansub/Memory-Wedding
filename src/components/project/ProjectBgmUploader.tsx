"use client";

import { useRef, useState } from "react";
import { apiAssetUrl, apiFetch, apiUpload } from "@/lib/api";

export type ProjectBgm = {
  uploaded: boolean;
  originalFilename?: string | null;
  mimeType?: string | null;
  fileSize?: number | null;
  contentPath?: string | null;
};

type ProjectBgmUploaderProps = {
  projectId: string | number;
  value?: ProjectBgm | null;
  onChange?: (bgm: ProjectBgm) => void;
  disabled?: boolean;
  hint?: string;
};

export function ProjectBgmUploader({
  projectId,
  value,
  onChange,
  disabled,
  hint = "하이라이트 영상·청첩장에서 함께 사용됩니다. (MP3/M4A/AAC/WAV, 15MB 이하)",
}: ProjectBgmUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleUpload(file: File | null) {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const res = await apiUpload<ProjectBgm>(`/api/projects/${projectId}/bgm`, form);
      if (res.data) {
        onChange?.(res.data);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "업로드 실패");
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  async function handleDelete() {
    if (!confirm("등록된 BGM을 삭제할까요?")) return;
    setBusy(true);
    setError(null);
    try {
      await apiFetch(`/api/projects/${projectId}/bgm`, { method: "DELETE" });
      onChange?.({ uploaded: false });
    } catch (err) {
      setError(err instanceof Error ? err.message : "삭제 실패");
    } finally {
      setBusy(false);
    }
  }

  const uploaded = Boolean(value?.uploaded && value.contentPath);

  return (
    <div className="rounded-2xl border border-accent/20 bg-white/70 p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-medium">배경음악 (BGM)</p>
          <p className="mt-1 text-xs text-muted">{hint}</p>
          {uploaded ? (
            <p className="mt-2 truncate text-xs text-accent">
              {value?.originalFilename ?? "업로드됨"}
              {value?.fileSize ? ` · ${(value.fileSize / (1024 * 1024)).toFixed(1)}MB` : ""}
            </p>
          ) : (
            <p className="mt-2 text-xs text-muted">아직 업로드된 음원이 없습니다.</p>
          )}
        </div>
        <div className="flex shrink-0 flex-col gap-2">
          <button
            type="button"
            disabled={disabled || busy}
            onClick={() => inputRef.current?.click()}
            className="rounded-full border border-accent/30 px-3 py-1.5 text-xs disabled:opacity-50"
          >
            {busy ? "처리 중..." : uploaded ? "교체" : "업로드"}
          </button>
          {uploaded && (
            <button
              type="button"
              disabled={disabled || busy}
              onClick={handleDelete}
              className="rounded-full border border-red-200 px-3 py-1.5 text-xs text-red-600 disabled:opacity-50"
            >
              삭제
            </button>
          )}
        </div>
      </div>
      {uploaded && value?.contentPath && (
        <audio
          className="mt-3 w-full"
          controls
          preload="none"
          src={apiAssetUrl(value.contentPath)}
        />
      )}
      {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
      <input
        ref={inputRef}
        type="file"
        accept="audio/mpeg,audio/mp4,audio/aac,audio/wav,audio/x-m4a,.mp3,.m4a,.aac,.wav"
        className="hidden"
        onChange={(e) => void handleUpload(e.target.files?.[0] ?? null)}
      />
    </div>
  );
}
