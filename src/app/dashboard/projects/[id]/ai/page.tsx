"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, apiFetchBlob } from "@/lib/api";
import { getToken } from "@/lib/auth";
import {
  SCENE_LABELS,
  isVideoResult,
  type AiDashboard,
  type AiPhotoResult,
  type SceneCategory,
} from "@/types/ai";

export default function ProjectAiPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [data, setData] = useState<AiDashboard | null>(null);
  const [previews, setPreviews] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

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
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [params.id, router]);

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
  }, [data?.latestJob?.id]);

  async function runAnalyze() {
    setRunning(true);
    setError(null);
    setMessage(null);
    try {
      const res = await apiFetch<AiDashboard>(`/api/projects/${params.id}/ai/analyze`, {
        method: "POST",
      });
      setData(res.data);
      setMessage(res.message ?? "AI 분석이 완료되었습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "분석 실패");
    } finally {
      setRunning(false);
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
            하객 업로드 사진·영상을 분석해 입장·축가·단체사진·피로연 등으로 분류하고 Best Shot을
            고릅니다. 일치율(confidence)이 낮은 항목은 분류에서 제외됩니다.
            {data?.analyzerMode === "mock"
              ? " (현재 mock 모드 — GEMINI_API_KEY 설정 시 Gemini 사용)"
              : " (Gemini 연동)"}
          </p>
        </div>
        <button
          type="button"
          onClick={() => void runAnalyze()}
          disabled={running}
          className="rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
        >
          {running ? "분석 중..." : "AI 분석 실행"}
        </button>
      </div>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}
      {message && <p className="mb-4 text-sm text-green-700">{message}</p>}

      {data?.latestJob && (
        <div className="mb-8 rounded-2xl border border-accent/20 bg-white/70 p-4 text-sm">
          <p>
            최근 Job #{data.latestJob.id} · {data.latestJob.status} ·{" "}
            {data.latestJob.processedFiles}/{data.latestJob.totalFiles}
          </p>
          <p className="mt-1 text-muted">
            분류 기준 confidence ≥ {Math.round((data.minConfidence ?? 0.6) * 100)}%
            {data.excludedCount > 0
              ? ` · 제외 ${data.excludedCount}건 (원본 갤러리/Drive 유지)`
              : ""}
          </p>
          {data.latestJob.errorMessage && (
            <p className="mt-1 text-red-600">{data.latestJob.errorMessage}</p>
          )}
        </div>
      )}

      {!data?.latestJob && (
        <p className="mb-10 text-sm text-muted">
          아직 분석 결과가 없습니다. 갤러리에 사진·영상이 있다면 분석을 실행해 보세요.
        </p>
      )}

      {data?.bestShots && data.bestShots.length > 0 && (
        <section className="mb-12">
          <h2 className="mb-4 text-lg font-medium">Best Shot</h2>
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
              VIDEO
            </span>
            <p className="line-clamp-3 text-[11px] text-muted">{item.originalFilename}</p>
            {item.frameCount != null && (
              <p className="text-[10px] text-muted">{item.frameCount}프레임 분석</p>
            )}
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
            Best
          </span>
        )}
      </div>
      <div className="space-y-0.5 p-2">
        <p className="truncate text-xs">{item.originalFilename}</p>
        <p className="text-[10px] text-muted">
          {item.guestName}
          {item.confidence != null ? ` · ${(item.confidence * 100).toFixed(0)}%` : ""}
        </p>
        {(item.people?.length || item.objects?.length || item.place) && (
          <p className="line-clamp-2 text-[10px] text-muted">
            {item.people?.length ? `인물: ${item.people.join(", ")}` : ""}
            {item.objects?.length
              ? `${item.people?.length ? " · " : ""}객체: ${item.objects.join(", ")}`
              : ""}
            {item.place
              ? `${item.people?.length || item.objects?.length ? " · " : ""}${item.place}`
              : ""}
          </p>
        )}
      </div>
    </article>
  );
}
