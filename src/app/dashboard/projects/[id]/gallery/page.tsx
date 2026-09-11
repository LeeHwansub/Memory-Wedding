"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StatusModal } from "@/components/ui/StatusModal";
import { apiFetch, apiFetchBlob } from "@/lib/api";
import { getToken } from "@/lib/auth";
import type {
  FileType,
  UploadFile,
  UploadFolderTree,
  UploadGuestFolder,
  UploadPage,
} from "@/types/upload";

type View =
  | { level: "root" }
  | { level: "type"; fileType: FileType }
  | { level: "guest"; fileType: FileType; guestName: string };

type ProjectDriveStatus = {
  driveConnected: boolean;
  googleAccountEmail: string | null;
  pendingSyncCount: number;
  syncedCount: number;
};

type SyncModal =
  | null
  | { kind: "progress"; step: number; description?: string }
  | { kind: "done"; description: string };

const SYNC_STEPS = ["Drive 연결 확인", "폴더 준비", "파일 사본 업로드"];

export default function ProjectGalleryPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [view, setView] = useState<View>({ level: "root" });
  const [tree, setTree] = useState<UploadFolderTree | null>(null);
  const [pageData, setPageData] = useState<UploadPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [previews, setPreviews] = useState<Record<number, string>>({});
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [drive, setDrive] = useState<ProjectDriveStatus | null>(null);
  const [syncBusy, setSyncBusy] = useState(false);
  const [syncModal, setSyncModal] = useState<SyncModal>(null);

  const loadTree = useCallback(async () => {
    const res = await apiFetch<UploadFolderTree>(
      `/api/projects/${params.id}/uploads/folders`,
    );
    setTree(res.data);
    return res.data;
  }, [params.id]);

  const loadDrive = useCallback(async () => {
    const res = await apiFetch<ProjectDriveStatus>(`/api/projects/${params.id}/drive`);
    setDrive(res.data);
    return res.data;
  }, [params.id]);

  const loadGuestFiles = useCallback(async () => {
    if (view.level !== "guest") return null;
    const q = new URLSearchParams({
      page: String(page),
      size: "24",
      fileType: view.fileType,
      guestName: view.guestName,
    });
    const res = await apiFetch<UploadPage>(
      `/api/projects/${params.id}/uploads?${q.toString()}`,
    );
    setPageData(res.data);
    return res.data;
  }, [params.id, page, view]);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }
    setLoading(true);
    setError(null);
    const task =
      view.level === "guest"
        ? Promise.all([loadGuestFiles(), loadDrive()])
        : Promise.all([
            loadTree().then(() => {
              setPageData(null);
              setPage(0);
            }),
            loadDrive(),
          ]);
    task
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [loadDrive, loadGuestFiles, loadTree, router, view]);

  useEffect(() => {
    const files = pageData?.content ?? [];
    let cancelled = false;
    const created: string[] = [];

    async function loadPreviews() {
      const next: Record<number, string> = {};
      for (const file of files) {
        if (file.uploadStatus !== "COMPLETED" || file.fileType !== "PHOTO") continue;
        try {
          const blob = await apiFetchBlob(
            `/api/projects/${params.id}/uploads/${file.id}/content`,
          );
          if (cancelled) return;
          const url = URL.createObjectURL(blob);
          created.push(url);
          next[file.id] = url;
        } catch {
          // skip broken preview
        }
      }
      if (!cancelled) setPreviews(next);
    }

    if (view.level === "guest") {
      loadPreviews();
    } else {
      setPreviews({});
    }

    return () => {
      cancelled = true;
      created.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [pageData, params.id, view.level]);

  async function handleDelete(file: UploadFile) {
    if (!confirm(`「${file.originalFilename}」을 삭제할까요?`)) return;
    setDeletingId(file.id);
    try {
      await apiFetch(`/api/projects/${params.id}/uploads/${file.id}`, { method: "DELETE" });
      await Promise.all([loadGuestFiles(), loadTree(), loadDrive()]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "삭제 실패");
    } finally {
      setDeletingId(null);
    }
  }

  async function handleSyncPending() {
    if (!drive?.driveConnected) {
      setError("먼저 기본 정보에서 Google Drive를 연결해 주세요.");
      return;
    }
    setSyncBusy(true);
    setError(null);
    setSyncModal({
      kind: "progress",
      step: 0,
      description: "Drive 연결 상태를 확인합니다.",
    });
    try {
      setSyncModal({
        kind: "progress",
        step: 1,
        description: "프로젝트 폴더를 준비하고 있습니다.",
      });
      await apiFetch(`/api/projects/${params.id}/drive/folders`, { method: "POST" });
      setSyncModal({
        kind: "progress",
        step: 2,
        description: "대기 중인 파일을 Drive로 복사하고 있습니다.",
      });
      const res = await apiFetch<{ synced: number; failed: number }>(
        `/api/projects/${params.id}/drive/sync-pending`,
        { method: "POST" },
      );
      const result = res.data;
      const description = result
        ? `성공 ${result.synced}건` +
          (result.failed > 0 ? ` · 실패 ${result.failed}건` : "")
        : "동기화가 완료되었습니다.";
      setSyncModal({ kind: "done", description });
      await Promise.all([
        loadDrive(),
        loadTree(),
        view.level === "guest" ? loadGuestFiles() : Promise.resolve(null),
      ]);
    } catch (err) {
      setSyncModal(null);
      setError(err instanceof Error ? err.message : "동기화 실패");
    } finally {
      setSyncBusy(false);
    }
  }

  function openType(fileType: FileType) {
    setPage(0);
    setView({ level: "type", fileType });
  }

  function openGuest(fileType: FileType, guestName: string) {
    setPage(0);
    setView({ level: "guest", fileType, guestName });
  }

  function goRoot() {
    setPage(0);
    setView({ level: "root" });
  }

  function goType(fileType: FileType) {
    setPage(0);
    setView({ level: "type", fileType });
  }

  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const entries = pageData?.content ?? [];
  const guestFolders: UploadGuestFolder[] =
    view.level === "type"
      ? view.fileType === "PHOTO"
        ? (tree?.photos ?? [])
        : (tree?.videos ?? [])
      : [];

  if (loading && !tree && view.level !== "guest") {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-3xl px-6 py-10 sm:py-14">
      <div className="mb-2 flex flex-wrap items-end justify-between gap-3">
        <h1
          className="text-3xl font-light"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          업로드 갤러리
        </h1>
        <button
          type="button"
          disabled={syncBusy}
          onClick={handleSyncPending}
          className="rounded-full border border-accent/30 px-4 py-2 text-xs disabled:opacity-50"
        >
          {drive?.driveConnected
            ? `Drive 동기화${(drive.pendingSyncCount ?? 0) > 0 ? ` (${drive.pendingSyncCount})` : ""}`
            : "Drive 동기화"}
        </button>
      </div>
      <Breadcrumb view={view} onRoot={goRoot} onType={goType} />
      <p className="mb-8 text-sm text-muted">
        사진·영상 → 하객 이름 순으로 모아 둡니다.
        {!drive?.driveConnected && (
          <>
            {" "}
            Drive 연결은{" "}
            <Link
              href={`/dashboard/projects/${params.id}/edit`}
              className="underline underline-offset-2"
            >
              기본 정보
            </Link>
            에서 할 수 있습니다.
          </>
        )}
      </p>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      {view.level === "root" && (
        <ul className="grid gap-4 sm:grid-cols-2">
          <FolderCard
            title="사진"
            count={tree?.photoCount ?? 0}
            onClick={() => openType("PHOTO")}
          />
          <FolderCard
            title="영상"
            count={tree?.videoCount ?? 0}
            onClick={() => openType("VIDEO")}
          />
        </ul>
      )}

      {view.level === "type" &&
        (guestFolders.length === 0 ? (
          <EmptyState label="이 폴더에 파일이 없습니다." />
        ) : (
          <ul className="grid gap-3 sm:grid-cols-2">
            {guestFolders.map((folder) => (
              <FolderCard
                key={folder.guestName}
                title={folder.guestName}
                count={folder.fileCount}
                onClick={() => openGuest(view.fileType, folder.guestName)}
              />
            ))}
          </ul>
        ))}

      {view.level === "guest" &&
        (loading && !pageData ? (
          <p className="text-sm text-muted">로딩 중...</p>
        ) : entries.length === 0 ? (
          <EmptyState label="아직 업로드된 파일이 없습니다." />
        ) : (
          <>
            <p className="mb-4 text-xs text-muted">{totalElements}건</p>
            <ul className="grid gap-4 sm:grid-cols-2">
              {entries.map((file) => (
                <li
                  key={file.id}
                  className="overflow-hidden rounded-2xl border border-accent/20 bg-white/70"
                >
                  <div className="flex aspect-[4/3] items-center justify-center bg-[#F3EBE0]/60">
                    {previews[file.id] ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={previews[file.id]}
                        alt={file.originalFilename}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <p className="px-4 text-center text-xs text-muted">
                        {file.fileType === "VIDEO" ? "VIDEO" : file.uploadStatus}
                        <br />
                        {file.originalFilename}
                      </p>
                    )}
                  </div>
                  <div className="space-y-2 p-4 text-sm">
                    <p className="break-all text-xs text-muted">{file.originalFilename}</p>
                    <p className="text-xs text-muted">
                      {formatBytes(file.fileSize)} · {file.uploadStatus}
                      {file.driveSynced ? " · Drive 동기화됨" : ""}
                    </p>
                    <button
                      type="button"
                      disabled={deletingId === file.id}
                      onClick={() => handleDelete(file)}
                      className="rounded-full border border-red-200 px-3 py-1 text-xs text-red-600 disabled:opacity-60"
                    >
                      {deletingId === file.id ? "삭제 중..." : "삭제"}
                    </button>
                  </div>
                </li>
              ))}
            </ul>

            {totalPages > 1 && (
              <div className="mt-8 flex items-center justify-center gap-3">
                <button
                  type="button"
                  disabled={page <= 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
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
                  onClick={() => setPage((p) => p + 1)}
                  className="rounded-full border border-accent/30 px-4 py-2 text-xs disabled:opacity-40"
                >
                  다음
                </button>
              </div>
            )}
          </>
        ))}

      <StatusModal
        open={syncModal?.kind === "progress"}
        title="Drive 동기화"
        description={syncModal?.kind === "progress" ? syncModal.description : undefined}
        steps={SYNC_STEPS}
        activeStep={syncModal?.kind === "progress" ? syncModal.step : 0}
      />
      <StatusModal
        open={syncModal?.kind === "done"}
        title="동기화 완료"
        description={syncModal?.kind === "done" ? syncModal.description : undefined}
        steps={SYNC_STEPS}
        done
        onConfirm={() => setSyncModal(null)}
      />
    </main>
  );
}

function Breadcrumb({
  view,
  onRoot,
  onType,
}: {
  view: View;
  onRoot: () => void;
  onType: (fileType: FileType) => void;
}) {
  const typeLabel = (t: FileType) => (t === "PHOTO" ? "사진" : "영상");

  return (
    <nav className="mb-2 flex flex-wrap items-center gap-1 text-sm text-muted">
      <button type="button" onClick={onRoot} className="hover:underline">
        전체
      </button>
      {view.level !== "root" && (
        <>
          <span>/</span>
          {view.level === "type" ? (
            <span className="text-foreground">{typeLabel(view.fileType)}</span>
          ) : (
            <button
              type="button"
              onClick={() => onType(view.fileType)}
              className="hover:underline"
            >
              {typeLabel(view.fileType)}
            </button>
          )}
        </>
      )}
      {view.level === "guest" && (
        <>
          <span>/</span>
          <span className="text-foreground">{view.guestName}</span>
        </>
      )}
    </nav>
  );
}

function FolderCard({
  title,
  count,
  onClick,
}: {
  title: string;
  count: number;
  onClick: () => void;
}) {
  return (
    <li>
      <button
        type="button"
        onClick={onClick}
        className="flex w-full items-center gap-4 rounded-2xl border border-accent/20 bg-white/70 px-5 py-5 text-left transition hover:border-accent/40 hover:bg-white"
      >
        <span
          className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#F3EBE0] text-lg text-[#8B7355]"
          aria-hidden
        >
          ▤
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate font-medium">{title}</span>
          <span className="text-xs text-muted">{count}개 파일</span>
        </span>
        <span className="text-muted" aria-hidden>
          ›
        </span>
      </button>
    </li>
  );
}

function EmptyState({ label }: { label: string }) {
  return (
    <p className="rounded-2xl border border-dashed border-accent/30 p-10 text-center text-sm text-muted">
      {label}
    </p>
  );
}

function formatBytes(size: number) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}
