"use client";

import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { InvitationDesignPreview } from "@/components/invitation/InvitationDesignPreview";
import { InvitationImage } from "@/components/invitation/InvitationImage";
import { apiFetch, apiUpload } from "@/lib/api";
import { getToken } from "@/lib/auth";
import { toDatetimeLocalValue } from "@/lib/datetime";
import type {
  GalleryLayout,
  Invitation,
  InvitationMedia,
  MainPhotoPlacement,
  MediaDisplaySize,
} from "@/types/invitation";

const LAYOUTS: { value: GalleryLayout; label: string; hint: string }[] = [
  { value: "SLIDER", label: "슬라이더", hint: "큰 사진 + 썸네일" },
  { value: "COLLAGE", label: "콜라주", hint: "모자이크 그리드" },
  { value: "VERTICAL", label: "세로 스택", hint: "풀블리드 나열" },
];

const SIZES: { value: MediaDisplaySize; label: string }[] = [
  { value: "SM", label: "작게" },
  { value: "MD", label: "보통" },
  { value: "LG", label: "크게" },
  { value: "FULL", label: "최대" },
];

export default function InvitationDesignPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [invitation, setInvitation] = useState<Invitation | null>(null);
  const [galleryLayout, setGalleryLayout] = useState<GalleryLayout>("SLIDER");
  const [galleryColumns, setGalleryColumns] = useState(2);
  const [galleryImageSize, setGalleryImageSize] = useState<MediaDisplaySize>("MD");
  const [mainPhotoSize, setMainPhotoSize] = useState<MediaDisplaySize>("LG");
  const [mainPhotoPlacement, setMainPhotoPlacement] =
    useState<MainPhotoPlacement>("TOP");
  const [mainBrightness, setMainBrightness] = useState(1);
  const [mainSaturation, setMainSaturation] = useState(1);
  const [mainFocalX, setMainFocalX] = useState(50);
  const [mainFocalY, setMainFocalY] = useState(50);
  const [mainPhoto, setMainPhoto] = useState<InvitationMedia | null>(null);
  const [gallery, setGallery] = useState<InvitationMedia[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState<"MAIN" | "GALLERY" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  function applyInvitation(data: Invitation) {
    setInvitation(data);
    setGalleryLayout(data.galleryLayout ?? "SLIDER");
    setGalleryColumns(data.galleryColumns ?? 2);
    setGalleryImageSize(data.galleryImageSize ?? "MD");
    setMainPhotoSize(data.mainPhotoSize ?? "LG");
    setMainPhotoPlacement(data.mainPhotoPlacement ?? "TOP");
    setMainBrightness(data.mainBrightness ?? 1);
    setMainSaturation(data.mainSaturation ?? 1);
    setMainFocalX(data.mainFocalX ?? 50);
    setMainFocalY(data.mainFocalY ?? 50);
    setMainPhoto(data.mainPhoto ?? null);
    setGallery(data.gallery ?? []);
  }

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    apiFetch<Invitation>(`/api/projects/${params.id}/invitation`)
      .then((res) => {
        if (res.data) applyInvitation(res.data);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [params.id, router]);

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    if (!invitation) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const res = await apiFetch<Invitation>(`/api/projects/${params.id}/invitation`, {
        method: "PUT",
        body: JSON.stringify({
          title: invitation.title || undefined,
          greetingMessage: invitation.greetingMessage || undefined,
          accounts: invitation.accounts ?? [],
          galleryLayout,
          galleryColumns,
          galleryImageSize,
          mainPhotoSize,
          mainPhotoPlacement,
          mainBrightness,
          mainSaturation,
          mainFocalX,
          mainFocalY,
        }),
      });
      if (res.data) applyInvitation(res.data);
      setMessage("디자인 설정이 저장되었습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장 실패");
    } finally {
      setSaving(false);
    }
  }

  async function uploadMedia(type: "MAIN" | "GALLERY", file: File | null) {
    if (!file) return;
    setUploading(type);
    setError(null);
    setMessage(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const res = await apiUpload<InvitationMedia>(
        `/api/projects/${params.id}/invitation/media?type=${type}`,
        formData,
      );
      if (!res.data) return;
      if (type === "MAIN") setMainPhoto(res.data);
      else setGallery((prev) => [...prev, res.data!]);
      setMessage(type === "MAIN" ? "메인 사진이 업로드되었습니다." : "갤러리 사진이 추가되었습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "업로드 실패");
    } finally {
      setUploading(null);
    }
  }

  async function deleteMedia(mediaId: number, type: "MAIN" | "GALLERY") {
    setError(null);
    try {
      await apiFetch(`/api/projects/${params.id}/invitation/media/${mediaId}`, {
        method: "DELETE",
      });
      if (type === "MAIN") setMainPhoto(null);
      else setGallery((prev) => prev.filter((item) => item.id !== mediaId));
      setMessage("사진이 삭제되었습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "삭제 실패");
    }
  }

  async function moveGallery(index: number, direction: -1 | 1) {
    const nextIndex = index + direction;
    if (nextIndex < 0 || nextIndex >= gallery.length) return;
    const next = [...gallery];
    const [item] = next.splice(index, 1);
    next.splice(nextIndex, 0, item);
    setGallery(next);
    try {
      const res = await apiFetch<Invitation>(
        `/api/projects/${params.id}/invitation/gallery/order`,
        {
          method: "PUT",
          body: JSON.stringify({ galleryIds: next.map((g) => g.id) }),
        },
      );
      if (res.data) applyInvitation(res.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "순서 변경 실패");
    }
  }

  if (loading) {
    return (
      <main className="flex min-h-[50vh] items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  if (!invitation) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16">
        <p className="text-red-600">{error ?? "청첩장을 찾을 수 없습니다."}</p>
      </main>
    );
  }

  const weddingLocal = toDatetimeLocalValue(invitation.weddingAt);
  const [datePart, timePart] = weddingLocal.split("T");
  const weddingDateLabel = (() => {
    if (!datePart) return "";
    const [year, month, day] = datePart.split("-").map(Number);
    return new Date(year, month - 1, day).toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "long",
      day: "numeric",
      weekday: "long",
    });
  })();
  const weddingTimeLabel = (() => {
    if (!timePart) return "";
    const [hour, minute] = timePart.split(":").map(Number);
    return new Date(2000, 0, 1, hour, minute).toLocaleTimeString("ko-KR", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  })();

  return (
    <main className="mx-auto min-h-[calc(100vh-3.5rem)] max-w-6xl px-4 py-6 sm:px-6 sm:py-8">
      <div className="mb-6">
        <h1
          className="text-2xl font-light sm:text-3xl"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          청첩장 디자인
        </h1>
        <p className="mt-1 text-sm text-muted">
          메인 사진·웨딩 갤러리·레이아웃을 설정합니다. 우측에서 바로 확인할 수 있습니다.
        </p>
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_380px] xl:grid-cols-[minmax(0,1fr)_400px]">
        <form onSubmit={handleSave} className="min-w-0 space-y-8 pb-24 lg:pb-8">
          <section className="space-y-4">
            <div>
              <p className="text-sm font-medium">메인 사진</p>
              <p className="mt-1 text-xs text-muted">
                히어로 사진 1장. 밝기·채도·초점·위치·크기를 조절할 수 있습니다.
              </p>
            </div>

            {mainPhoto ? (
              <div className="space-y-3">
                <div className="overflow-hidden rounded-xl border border-accent/20">
                  <InvitationImage
                    auth
                    contentPath={mainPhoto.contentPath}
                    alt="메인 사진"
                    className="h-40 w-full object-cover"
                    style={{
                      filter: `brightness(${mainBrightness}) saturate(${mainSaturation})`,
                      objectPosition: `${mainFocalX}% ${mainFocalY}%`,
                    }}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => void deleteMedia(mainPhoto.id, "MAIN")}
                  className="text-xs text-red-600"
                >
                  메인 사진 삭제
                </button>
              </div>
            ) : (
              <p className="text-xs text-muted">아직 메인 사진이 없습니다.</p>
            )}

            <label className="block">
              <span className="mb-2 block text-xs text-muted">
                {uploading === "MAIN"
                  ? "업로드 중..."
                  : "메인 사진 업로드 (JPG/PNG/WEBP, 10MB↓)"}
              </span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                disabled={uploading !== null}
                onChange={(e) => {
                  void uploadMedia("MAIN", e.target.files?.[0] ?? null);
                  e.target.value = "";
                }}
                className="block w-full text-sm"
              />
            </label>

            <div className="grid grid-cols-2 gap-3 text-sm">
              <label>
                <span className="mb-1 block text-muted">위치</span>
                <select
                  value={mainPhotoPlacement}
                  onChange={(e) =>
                    setMainPhotoPlacement(e.target.value as MainPhotoPlacement)
                  }
                  className="w-full rounded-xl border border-accent/30 bg-white px-3 py-2"
                >
                  <option value="TOP">상단 풀블리드</option>
                  <option value="MIDDLE">중간 프레임</option>
                </select>
              </label>
              <label>
                <span className="mb-1 block text-muted">크기</span>
                <select
                  value={mainPhotoSize}
                  onChange={(e) => setMainPhotoSize(e.target.value as MediaDisplaySize)}
                  className="w-full rounded-xl border border-accent/30 bg-white px-3 py-2"
                >
                  {SIZES.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label className="block text-sm">
              <span className="mb-1 flex justify-between text-muted">
                밝기 <span>{mainBrightness.toFixed(2)}</span>
              </span>
              <input
                type="range"
                min={0.5}
                max={1.5}
                step={0.05}
                value={mainBrightness}
                onChange={(e) => setMainBrightness(Number(e.target.value))}
                className="w-full"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 flex justify-between text-muted">
                채도 <span>{mainSaturation.toFixed(2)}</span>
              </span>
              <input
                type="range"
                min={0.5}
                max={1.5}
                step={0.05}
                value={mainSaturation}
                onChange={(e) => setMainSaturation(Number(e.target.value))}
                className="w-full"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 flex justify-between text-muted">
                초점 X <span>{mainFocalX}%</span>
              </span>
              <input
                type="range"
                min={0}
                max={100}
                value={mainFocalX}
                onChange={(e) => setMainFocalX(Number(e.target.value))}
                className="w-full"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 flex justify-between text-muted">
                초점 Y <span>{mainFocalY}%</span>
              </span>
              <input
                type="range"
                min={0}
                max={100}
                value={mainFocalY}
                onChange={(e) => setMainFocalY(Number(e.target.value))}
                className="w-full"
              />
            </label>
          </section>

          <section className="space-y-4">
            <div>
              <p className="text-sm font-medium">웨딩 갤러리</p>
              <p className="mt-1 text-xs text-muted">
                최대 15장. 공개 청첩장에서는 카드로 보이고, 클릭 시 모달에서 레이아웃대로 열립니다.
              </p>
            </div>

            <div className="grid grid-cols-3 gap-2">
              {LAYOUTS.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  onClick={() => setGalleryLayout(item.value)}
                  className={`rounded-xl border px-2 py-3 text-left ${
                    galleryLayout === item.value
                      ? "border-accent bg-accent-soft"
                      : "border-accent/20 bg-white"
                  }`}
                >
                  <span className="block text-xs font-medium">{item.label}</span>
                  <span className="mt-1 block text-[10px] text-muted">{item.hint}</span>
                </button>
              ))}
            </div>

            <div className="grid grid-cols-2 gap-3 text-sm">
              <label>
                <span className="mb-1 block text-muted">콜라주 열 수</span>
                <select
                  value={galleryColumns}
                  onChange={(e) => setGalleryColumns(Number(e.target.value))}
                  disabled={galleryLayout !== "COLLAGE"}
                  className="w-full rounded-xl border border-accent/30 bg-white px-3 py-2 disabled:opacity-50"
                >
                  <option value={2}>2열</option>
                  <option value={3}>3열</option>
                  <option value={4}>4열</option>
                </select>
              </label>
              <label>
                <span className="mb-1 block text-muted">이미지 크기</span>
                <select
                  value={galleryImageSize}
                  onChange={(e) =>
                    setGalleryImageSize(e.target.value as MediaDisplaySize)
                  }
                  className="w-full rounded-xl border border-accent/30 bg-white px-3 py-2"
                >
                  {SIZES.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label className="block">
              <span className="mb-2 block text-xs text-muted">
                {uploading === "GALLERY"
                  ? "업로드 중..."
                  : `갤러리 추가 (${gallery.length}/15)`}
              </span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                disabled={uploading !== null || gallery.length >= 15}
                onChange={(e) => {
                  void uploadMedia("GALLERY", e.target.files?.[0] ?? null);
                  e.target.value = "";
                }}
                className="block w-full text-sm"
              />
            </label>

            {gallery.length > 0 && (
              <ul className="space-y-2">
                {gallery.map((item, index) => (
                  <li
                    key={item.id}
                    className="flex items-center gap-3 rounded-xl border border-accent/20 bg-white/80 p-2"
                  >
                    <InvitationImage
                      auth
                      contentPath={item.contentPath}
                      alt=""
                      className="h-14 w-14 shrink-0 rounded-lg object-cover"
                    />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-xs">{item.originalFilename}</p>
                      <div className="mt-1 flex gap-2">
                        <button
                          type="button"
                          onClick={() => void moveGallery(index, -1)}
                          disabled={index === 0}
                          className="text-[11px] text-muted disabled:opacity-40"
                        >
                          위로
                        </button>
                        <button
                          type="button"
                          onClick={() => void moveGallery(index, 1)}
                          disabled={index === gallery.length - 1}
                          className="text-[11px] text-muted disabled:opacity-40"
                        >
                          아래로
                        </button>
                        <button
                          type="button"
                          onClick={() => void deleteMedia(item.id, "GALLERY")}
                          className="text-[11px] text-red-600"
                        >
                          삭제
                        </button>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {error && <p className="text-sm text-red-600">{error}</p>}
          {message && <p className="text-sm text-green-700">{message}</p>}

          <button
            type="submit"
            disabled={saving}
            className="w-full rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60 lg:sticky lg:bottom-4"
          >
            {saving ? "저장 중..." : "디자인 저장"}
          </button>
        </form>

        <aside className="lg:sticky lg:top-20 lg:self-start">
          <p className="mb-3 text-center text-xs tracking-wide text-muted">미리보기</p>
          <div className="mx-auto w-full max-w-[360px] overflow-hidden rounded-[2rem] border border-black/10 bg-black shadow-[0_20px_50px_rgba(0,0,0,0.12)]">
            <div className="flex items-center justify-center gap-1 border-b border-white/10 bg-[#1c1c1e] px-4 py-2">
              <span className="h-1.5 w-1.5 rounded-full bg-white/30" />
              <span className="h-1 w-12 rounded-full bg-white/20" />
            </div>
            <div className="h-[min(70vh,680px)] overflow-y-auto overscroll-contain bg-[#FAF8F5]">
              <InvitationDesignPreview
                auth
                groomName={invitation.groomName}
                brideName={invitation.brideName}
                title={invitation.title}
                greetingMessage={invitation.greetingMessage}
                weddingDateLabel={weddingDateLabel}
                weddingTimeLabel={weddingTimeLabel}
                venueName={invitation.venueName}
                mainPhoto={mainPhoto}
                mainPhotoPlacement={mainPhotoPlacement}
                mainPhotoSize={mainPhotoSize}
                mainBrightness={mainBrightness}
                mainSaturation={mainSaturation}
                mainFocalX={mainFocalX}
                mainFocalY={mainFocalY}
                gallery={gallery}
                galleryLayout={galleryLayout}
                galleryColumns={galleryColumns}
                galleryImageSize={galleryImageSize}
              />
            </div>
          </div>
        </aside>
      </div>
    </main>
  );
}
