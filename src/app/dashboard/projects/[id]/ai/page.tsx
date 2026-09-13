"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, apiFetchBlob } from "@/lib/api";
import { getToken } from "@/lib/auth";
import {
  SCENE_LABELS,
  isVideoResult,
  type AiDashboard,
  type AiJobStatus,
  type AiPhotoResult,
  type AiVideoJob,
  type SceneCategory,
} from "@/types/ai";

const HIGHLIGHT_STEPS = [
  "사진·영상 준비",
  "장면 편집",
  "영상 합치기",
  "클라우드 저장",
] as const;

const POLL_MS = 1500;

function isActive(status?: AiJobStatus | null) {
  return status === "PENDING" || status === "PROCESSING";
}

function analysisPercent(job: AiDashboard["latestJob"]) {
  if (!job || job.totalFiles <= 0) return 0;
  return Math.min(100, Math.round((job.processedFiles / job.totalFiles) * 100));
}

function highlightStep(job: AiVideoJob | null | undefined) {
  if (!job || job.status === "COMPLETED") return HIGHLIGHT_STEPS.length;
  if (job.status === "FAILED") return 0;
  const clips = job.processedClips ?? 0;
  const total = Math.max(1, job.clipCount || 1);
  if (clips <= 0) return 0;
  if (clips < total) return 1;
  return 2;
}

export default function ProjectAiPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [data, setData] = useState<AiDashboard | null>(null);
  const [previews, setPreviews] = useState<Record<number, string>>({});
  const [highlightUrl, setHighlightUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [makingVideo, setMakingVideo] = useState(false);
  const [trackedVideoId, setTrackedVideoId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [justCompletedVideoId, setJustCompletedVideoId] = useState<number | null>(null);

  async function load() {
    const res = await apiFetch<AiDashboard>(`/api/projects/${params.id}/ai`);
    setData(res.data);
    return res.data;
  }

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }
    load()
      .then((dash) => {
        if (isActive(dash?.latestJob?.status)) setRunning(true);
        if (isActive(dash?.latestVideoJob?.status)) {
          setMakingVideo(true);
          setTrackedVideoId(dash?.latestVideoJob?.id ?? null);
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [params.id, router]);

  useEffect(() => {
    if (!running && !makingVideo) return;
    let cancelled = false;
    const timer = window.setInterval(() => {
      void (async () => {
        try {
          const dash = await load();
          if (cancelled || !dash) return;

          if (running) {
            const status = dash.latestJob?.status;
            if (status === "COMPLETED") {
              setRunning(false);
              setMessage("AI 분석이 완료되었습니다.");
            } else if (status === "FAILED") {
              setRunning(false);
              setError(dash.latestJob?.errorMessage ?? "분석 실패");
            }
          }

          if (makingVideo) {
            const video = dash.latestVideoJob;
            if (trackedVideoId != null && video?.id !== trackedVideoId) return;
            if (video?.status === "COMPLETED") {
              setMakingVideo(false);
              setJustCompletedVideoId(video.id);
              const driveNote = video.driveSynced ? " Drive에도 저장했습니다." : "";
              setMessage("하이라이트 영상 생성이 완료되었습니다." + driveNote);
            } else if (video?.status === "FAILED") {
              setMakingVideo(false);
              setError(video.errorMessage ?? "영상 생성 실패");
            }
          }
        } catch {
          // keep polling
        }
      })();
    }, POLL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [running, makingVideo, trackedVideoId, params.id]);

  useEffect(() => {
    if (!data?.results?.length) return;
    let cancelled = false;
    const targets = [...data.bestShots, ...data.results]
      .filter((item) => !isVideoResult(item))
      .slice(0, 24);

    (async () => {
      const next: Record<number, string> = {};
      for (const item of targets) {
        if (cancelled || previews[item.uploadFileId]) continue;
        try {
          const blob = await apiFetchBlob(item.contentPath);
          next[item.uploadFileId] = URL.createObjectURL(blob);
        } catch {
          // skip
        }
      }
      if (!cancelled && Object.keys(next).length) {
        setPreviews((prev) => ({ ...prev, ...next }));
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data?.latestJob?.id, data?.latestJob?.status]);

  useEffect(() => {
    const path = data?.latestVideoJob?.contentPath;
    if (!path || data?.latestVideoJob?.status !== "COMPLETED") {
      setHighlightUrl(null);
      return;
    }
    let cancelled = false;
    let objectUrl: string | null = null;
    (async () => {
      try {
        const blob = await apiFetchBlob(path);
        objectUrl = URL.createObjectURL(blob);
        if (!cancelled) setHighlightUrl(objectUrl);
      } catch {
        if (!cancelled) setHighlightUrl(null);
      }
    })();
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [data?.latestVideoJob?.id, data?.latestVideoJob?.status, data?.latestVideoJob?.contentPath]);

  async function runAnalyze() {
    setRunning(true);
    setError(null);
    setMessage(null);
    try {
      const res = await apiFetch<AiDashboard>(`/api/projects/${params.id}/ai/analyze`, {
        method: "POST",
      });
      setData(res.data);
      setMessage(res.message ?? "AI 분석을 시작했습니다.");
    } catch (err) {
      setRunning(false);
      setError(err instanceof Error ? err.message : "분석 실패");
    }
  }

  async function runHighlight() {
    const existing = data?.latestVideoJob;
    if (existing?.status === "COMPLETED") {
      const ok = window.confirm(
        "이미 하이라이트 영상이 있습니다. 새로 다시 생성할까요?\n(이전 서버 파일은 유지되며, Drive에는 새 파일이 추가됩니다)",
      );
      if (!ok) return;
    }

    setMakingVideo(true);
    setError(null);
    setMessage(null);
    setJustCompletedVideoId(null);
    try {
      const res = await apiFetch<AiVideoJob>(`/api/projects/${params.id}/ai/video`, {
        method: "POST",
      });
      setTrackedVideoId(res.data?.id ?? null);
      const dash = await load();
      if (dash) {
        setData({ ...dash, latestVideoJob: res.data ?? dash.latestVideoJob });
      }
      setMessage(res.message ?? "하이라이트 영상 생성을 시작했습니다.");
    } catch (err) {
      setMakingVideo(false);
      setTrackedVideoId(null);
      setError(err instanceof Error ? err.message : "영상 생성 실패");
    }
  }

  if (loading) {
    return (
      <main className="flex min-h-[50vh] items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  const grouped = groupByScene(data?.results ?? []);
  const hasCompletedVideo = data?.latestVideoJob?.status === "COMPLETED";
  const progressStep = highlightStep(data?.latestVideoJob);
  const analyzePct = analysisPercent(data?.latestJob ?? null);

  return (
    <main className="mx-auto min-h-screen max-w-4xl px-6 py-10 sm:py-14">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1
            className="text-3xl font-light"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            AI 분석
          </h1>
          <p className="mt-2 text-sm text-muted">
            하객이 올린 사진·영상을 장면별로 나누고, 대표 컷으로 하이라이트 영상을 만듭니다.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => void runAnalyze()}
            disabled={running || makingVideo}
            className="rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
          >
            {running ? "분석 중..." : "AI 분석 실행"}
          </button>
          <button
            type="button"
            onClick={() => void runHighlight()}
            disabled={running || makingVideo || !data?.bestShots?.length}
            className="rounded-full border border-accent/40 bg-white px-6 py-3 text-sm text-accent transition hover:bg-accent/5 disabled:opacity-60"
          >
            {makingVideo
              ? "영상 생성 중..."
              : hasCompletedVideo
                ? "하이라이트 다시 생성"
                : "하이라이트 영상 생성"}
          </button>
        </div>
      </div>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}
      {message && <p className="mb-4 text-sm text-green-700">{message}</p>}

      {justCompletedVideoId != null &&
        data?.latestVideoJob?.id === justCompletedVideoId &&
        data.latestVideoJob.status === "COMPLETED" && (
          <div className="mb-6 rounded-2xl border border-green-200 bg-green-50/80 px-4 py-3 text-sm text-green-800">
            하이라이트 영상이 준비되었습니다. 아래에서 바로 볼 수 있습니다.
            {data.latestVideoJob.driveSynced ? " Google Drive에도 저장했어요." : ""}
          </div>
        )}

      {running && data?.latestJob && (
        <div className="mb-8 rounded-2xl border border-accent/20 bg-white/70 p-4">
          <p className="mb-2 text-sm font-medium">분석 중 {analyzePct}%</p>
          <div className="h-2 overflow-hidden rounded-full bg-accent/10">
            <div
              className="h-full rounded-full bg-accent transition-all"
              style={{ width: `${analyzePct}%` }}
            />
          </div>
        </div>
      )}

      {makingVideo && (
        <div className="mb-8 rounded-2xl border border-accent/20 bg-white/70 p-4">
          <p className="mb-3 text-sm font-medium">영상 만드는 중</p>
          <ol className="space-y-2 text-sm">
            {HIGHLIGHT_STEPS.map((label, index) => {
              const done = index < progressStep;
              const current = index === progressStep;
              return (
                <li
                  key={label}
                  className={
                    done
                      ? "text-accent"
                      : current
                        ? "font-medium text-foreground"
                        : "text-muted"
                  }
                >
                  {done ? "✓ " : current ? "→ " : "· "}
                  {label}
                  {current ? "…" : ""}
                </li>
              );
            })}
          </ol>
        </div>
      )}

      {data?.latestJob?.status === "FAILED" && data.latestJob.errorMessage && (
        <p className="mb-6 text-sm text-red-600">{data.latestJob.errorMessage}</p>
      )}

      {!data?.latestJob && (
        <p className="mb-10 text-sm text-muted">
          아직 분석 결과가 없습니다. 갤러리에 사진·영상이 있다면 분석을 실행해 보세요.
        </p>
      )}

      {data?.latestVideoJob && (
        <section className="mb-10 rounded-2xl border border-accent/20 bg-white/70 p-4">
          <h2 className="mb-2 text-lg font-medium">하이라이트 영상</h2>
          {data.latestVideoJob.errorMessage && (
            <p className="mb-2 text-sm text-red-600">{data.latestVideoJob.errorMessage}</p>
          )}
          {highlightUrl && data.latestVideoJob.status === "COMPLETED" ? (
            <video
              key={highlightUrl}
              src={highlightUrl}
              controls
              className="w-full max-w-xl rounded-xl bg-black"
            />
          ) : data.latestVideoJob.status === "COMPLETED" ? (
            <p className="text-sm text-muted">영상 불러오는 중...</p>
          ) : data.latestVideoJob.status === "PROCESSING" ||
            data.latestVideoJob.status === "PENDING" ? (
            <p className="text-sm text-muted">영상을 만들고 있습니다…</p>
          ) : data.latestVideoJob.status === "FAILED" ? null : (
            <p className="text-sm text-muted">아직 생성된 영상이 없습니다.</p>
          )}
        </section>
      )}

      {data?.bestShots && data.bestShots.length > 0 && (
        <section className="mb-12">
          <h2 className="mb-4 text-lg font-medium">대표 컷</h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {data.bestShots.map((item) => (
              <ResultCard
                key={item.id}
                item={item}
                preview={previews[item.uploadFileId]}
                badge
              />
            ))}
          </div>
        </section>
      )}

      {Object.entries(grouped).map(([scene, items]) => (
        <section key={scene} className="mb-10">
          <h2 className="mb-3 text-lg font-medium">
            {SCENE_LABELS[scene as SceneCategory] ?? scene}
            <span className="ml-2 text-sm font-normal text-muted">{items.length}</span>
          </h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {items.map((item) => (
              <ResultCard
                key={item.id}
                item={item}
                preview={previews[item.uploadFileId]}
              />
            ))}
          </div>
        </section>
      ))}
    </main>
  );
}

function groupByScene(results: AiPhotoResult[]) {
  return results.reduce<Record<string, AiPhotoResult[]>>((acc, item) => {
    const key = item.sceneCategory;
    if (!acc[key]) acc[key] = [];
    acc[key].push(item);
    return acc;
  }, {});
}

function ResultCard({
  item,
  preview,
  badge,
}: {
  item: AiPhotoResult;
  preview?: string;
  badge?: boolean;
}) {
  const video = isVideoResult(item);

  return (
    <article className="overflow-hidden rounded-xl border border-accent/20 bg-white">
      <div className="relative aspect-square bg-accent-soft">
        {video ? (
          <div className="flex h-full flex-col items-center justify-center gap-1 px-3 text-center">
            <span className="rounded-full bg-accent/15 px-2 py-0.5 text-[10px] text-accent">
              영상
            </span>
            <p className="line-clamp-3 text-[11px] text-muted">{item.originalFilename}</p>
          </div>
        ) : preview ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={preview} alt="" className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-[10px] text-muted">
            미리보기
          </div>
        )}
        {(badge || item.bestShot) && (
          <span className="absolute left-2 top-2 rounded-full bg-accent px-2 py-0.5 text-[10px] text-white">
            대표
          </span>
        )}
      </div>
      <div className="space-y-0.5 p-2">
        <p className="truncate text-xs">{item.originalFilename}</p>
        {item.guestName && <p className="text-[10px] text-muted">{item.guestName}</p>}
      </div>
    </article>
  );
}
