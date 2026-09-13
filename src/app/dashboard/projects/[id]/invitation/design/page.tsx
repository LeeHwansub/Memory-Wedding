"use client";

import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { InvitationDesignPreview } from "@/components/invitation/InvitationDesignPreview";
import { InvitationImage } from "@/components/invitation/InvitationImage";
import { apiFetch, apiUpload } from "@/lib/api";
import { getToken } from "@/lib/auth";
import { toDatetimeLocalValue } from "@/lib/datetime";
import { InvitationTemplatePicker } from "@/components/invitation/InvitationTemplatePicker";
import { getInvitationTemplate } from "@/lib/invitation-templates";
import type {
  GalleryLayout,
  Invitation,
  InvitationMedia,
  InvitationTemplate,
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

type MobilePane = "edit" | "preview";

export default function InvitationDesignPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [invitation, setInvitation] = useState<Invitation | null>(null);
  const [template, setTemplate] = useState<InvitationTemplate>("CLASSIC");
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
  const [savedSnapshot, setSavedSnapshot] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState<"MAIN" | "GALLERY" | null>(null);
  const [uploadProgress, setUploadProgress] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [mobilePane, setMobilePane] = useState<MobilePane>("edit");
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [applyTemplatePresets, setApplyTemplatePresets] = useState(true);
  const skipAutoSave = useRef(true);
  const savingRef = useRef(false);

  function settingsSnapshot(data: {
    template: InvitationTemplate;
    galleryLayout: GalleryLayout;
    galleryColumns: number;
    galleryImageSize: MediaDisplaySize;
    mainPhotoSize: MediaDisplaySize;
    mainPhotoPlacement: MainPhotoPlacement;
    mainBrightness: number;
    mainSaturation: number;
    mainFocalX: number;
    mainFocalY: number;
  }) {
    return JSON.stringify(data);
  }

  function applyInvitation(data: Invitation) {
    const next = {
      template: data.template ?? "CLASSIC",
      galleryLayout: data.galleryLayout ?? "SLIDER",
      galleryColumns: data.galleryColumns ?? 2,
      galleryImageSize: data.galleryImageSize ?? "MD",
      mainPhotoSize: data.mainPhotoSize ?? "LG",
      mainPhotoPlacement: data.mainPhotoPlacement ?? "TOP",
      mainBrightness: data.mainBrightness ?? 1,
      mainSaturation: data.mainSaturation ?? 1,
      mainFocalX: data.mainFocalX ?? 50,
      mainFocalY: data.mainFocalY ?? 50,
    };
    setInvitation(data);
    setTemplate(next.template);
    setGalleryLayout(next.galleryLayout);
    setGalleryColumns(next.galleryColumns);
    setGalleryImageSize(next.galleryImageSize);
    setMainPhotoSize(next.mainPhotoSize);
    setMainPhotoPlacement(next.mainPhotoPlacement);
    setMainBrightness(next.mainBrightness);
    setMainSaturation(next.mainSaturation);
    setMainFocalX(next.mainFocalX);
    setMainFocalY(next.mainFocalY);
    setMainPhoto(data.mainPhoto ?? null);
    setGallery(data.gallery ?? []);
    setSavedSnapshot(settingsSnapshot(next));
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

  const dirty = useMemo(() => {
    if (!savedSnapshot) return false;
    return (
      settingsSnapshot({
        template,
        galleryLayout,
        galleryColumns,
        galleryImageSize,
        mainPhotoSize,
        mainPhotoPlacement,
        mainBrightness,
        mainSaturation,
        mainFocalX,
        mainFocalY,
      }) !== savedSnapshot
    );
  }, [
    savedSnapshot,
    template,
    galleryLayout,
    galleryColumns,
    galleryImageSize,
    mainPhotoSize,
    mainPhotoPlacement,
    mainBrightness,
    mainSaturation,
    mainFocalX,
    mainFocalY,
  ]);

  async function handleSave(event?: FormEvent, options?: { silent?: boolean }) {
    event?.preventDefault();
    if (!invitation || savingRef.current) return;
    savingRef.current = true;
    setSaving(true);
    setError(null);
    if (!options?.silent) setMessage(null);
    try {
      const res = await apiFetch<Invitation>(`/api/projects/${params.id}/invitation`, {
        method: "PUT",
        body: JSON.stringify({
          title: invitation.title || undefined,
          greetingMessage: invitation.greetingMessage || undefined,
          accounts: invitation.accounts ?? [],
          template,
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
      setMessage(options?.silent ? "자동 저장됨" : "디자인 설정이 저장되었습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장 실패");
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  }

  useEffect(() => {
    if (loading || !invitation) return;
    if (skipAutoSave.current) {
      skipAutoSave.current = false;
      return;
    }
    if (!dirty) return;
    const timer = window.setTimeout(() => {
      void handleSave(undefined, { silent: true });
    }, 1100);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- debounce on settings dirty only
  }, [
    dirty,
    template,
    galleryLayout,
    galleryColumns,
    galleryImageSize,
    mainPhotoSize,
    mainPhotoPlacement,
    mainBrightness,
    mainSaturation,
    mainFocalX,
    mainFocalY,
  ]);

  async function uploadOne(type: "MAIN" | "GALLERY", file: File) {
    const formData = new FormData();
    formData.append("file", file);
    const res = await apiUpload<InvitationMedia>(
      `/api/projects/${params.id}/invitation/media?type=${type}`,
      formData,
    );
    return res.data;
  }

  async function uploadMedia(type: "MAIN" | "GALLERY", files: FileList | File[] | null) {
    if (!files || files.length === 0) return;
    const list = Array.from(files);
    setUploading(type);
    setError(null);
    setMessage(null);
    try {
      if (type === "MAIN") {
        const data = await uploadOne("MAIN", list[0]);
        if (data) setMainPhoto(data);
        setMessage("메인 사진이 업로드되었습니다.");
        return;
      }

      const remaining = Math.max(0, 15 - gallery.length);
      const batch = list.slice(0, remaining);
      if (batch.length === 0) {
        setError("갤러리는 최대 15장까지 등록할 수 있습니다.");
        return;
      }

      const uploaded: InvitationMedia[] = [];
      for (let i = 0; i < batch.length; i += 1) {
        setUploadProgress(`${i + 1}/${batch.length}`);
        const data = await uploadOne("GALLERY", batch[i]);
        if (data) uploaded.push(data);
      }
      setGallery((prev) => [...prev, ...uploaded]);
      setMessage(
        batch.length === 1
          ? "갤러리 사진이 추가되었습니다."
          : `갤러리 사진 ${uploaded.length}장이 추가되었습니다.`,
      );
      if (list.length > remaining) {
        setError(`최대 15장 제한으로 ${remaining}장만 업로드했습니다.`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "업로드 실패");
    } finally {
      setUploading(null);
      setUploadProgress(null);
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

  async function persistGalleryOrder(next: InvitationMedia[]) {
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

  async function moveGallery(index: number, direction: -1 | 1) {
    const nextIndex = index + direction;
    if (nextIndex < 0 || nextIndex >= gallery.length) return;
    const next = [...gallery];
    const [item] = next.splice(index, 1);
    next.splice(nextIndex, 0, item);
    await persistGalleryOrder(next);
  }

  async function dropGallery(toIndex: number) {
    if (dragIndex === null || dragIndex === toIndex) {
      setDragIndex(null);
      return;
    }
    const next = [...gallery];
    const [item] = next.splice(dragIndex, 1);
    next.splice(toIndex, 0, item);
    setDragIndex(null);
    await persistGalleryOrder(next);
  }

  function applyTemplate(nextId: InvitationTemplate) {
    const def = getInvitationTemplate(nextId);
    setTemplate(def.id);
    if (!applyTemplatePresets) return;
    setGalleryLayout(def.presets.galleryLayout);
    setGalleryColumns(def.presets.galleryColumns);
    setGalleryImageSize(def.presets.galleryImageSize);
    setMainPhotoSize(def.presets.mainPhotoSize);
    setMainPhotoPlacement(def.presets.mainPhotoPlacement);
    setMainBrightness(def.presets.mainBrightness);
    setMainSaturation(def.presets.mainSaturation);
  }

  function setFocalFromClick(
    event: React.MouseEvent<HTMLButtonElement>,
  ) {
    const rect = event.currentTarget.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) return;
    const x = Math.round(((event.clientX - rect.left) / rect.width) * 100);
    const y = Math.round(((event.clientY - rect.top) / rect.height) * 100);
    setMainFocalX(Math.min(100, Math.max(0, x)));
    setMainFocalY(Math.min(100, Math.max(0, y)));
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

  const preview = (
    <InvitationDesignPreview
      auth
      template={template}
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
  );

  return (
    <main className="mx-auto min-h-[calc(100vh-3.5rem)] max-w-6xl px-4 py-6 sm:px-6 sm:py-8">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1
            className="text-2xl font-light sm:text-3xl"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            청첩장 디자인
          </h1>
          <p className="mt-1 text-sm text-muted">
            좌측에서 설정하고 우측(또는 미리보기 탭)에서 바로 확인합니다.
          </p>
        </div>
        {dirty && (
          <span className="rounded-full bg-accent-soft px-3 py-1 text-xs text-[#8B7355]">
            저장되지 않은 변경
          </span>
        )}
      </div>

      <div className="mb-5 grid grid-cols-2 gap-1 rounded-full border border-accent/25 bg-white/70 p-1 lg:hidden">
        <button
          type="button"
          onClick={() => setMobilePane("edit")}
          className={`rounded-full py-2 text-sm transition ${
            mobilePane === "edit"
              ? "bg-foreground text-background"
              : "text-muted"
          }`}
        >
          설정
        </button>
        <button
          type="button"
          onClick={() => setMobilePane("preview")}
          className={`rounded-full py-2 text-sm transition ${
            mobilePane === "preview"
              ? "bg-foreground text-background"
              : "text-muted"
          }`}
        >
          미리보기
        </button>
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_380px] xl:grid-cols-[minmax(0,1fr)_400px]">
        <form
          onSubmit={(e) => void handleSave(e)}
          className={`min-w-0 space-y-8 pb-28 lg:block lg:pb-8 ${
            mobilePane === "edit" ? "block" : "hidden"
          }`}
        >
          <InvitationTemplatePicker
            value={template}
            applyPresets={applyTemplatePresets}
            onApplyPresetsChange={setApplyTemplatePresets}
            onSelect={applyTemplate}
          />

          <section className="space-y-4">
            <div>
              <p className="text-sm font-medium">메인 사진</p>
              <p className="mt-1 text-xs text-muted">
                사진을 클릭해 초점을 지정할 수 있습니다. 밝기·채도·위치·크기도 조절됩니다.
              </p>
            </div>

            {mainPhoto ? (
              <div className="space-y-3">
                <button
                  type="button"
                  onClick={setFocalFromClick}
                  className="relative block w-full overflow-hidden rounded-xl border border-accent/20"
                  title="클릭하여 초점 지정"
                >
                  <InvitationImage
                    auth
                    contentPath={mainPhoto.contentPath}
                    alt="메인 사진"
                    className="pointer-events-none h-44 w-full object-cover"
                    style={{
                      filter: `brightness(${mainBrightness}) saturate(${mainSaturation})`,
                      objectPosition: `${mainFocalX}% ${mainFocalY}%`,
                    }}
                  />
                  <span
                    className="pointer-events-none absolute h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white bg-accent shadow"
                    style={{ left: `${mainFocalX}%`, top: `${mainFocalY}%` }}
                    aria-hidden
                  />
                </button>
                <div className="flex items-center justify-between gap-3">
                  <p className="text-[11px] text-muted">
                    초점 {mainFocalX}% · {mainFocalY}%
                  </p>
                  <button
                    type="button"
                    onClick={() => void deleteMedia(mainPhoto.id, "MAIN")}
                    className="text-xs text-red-600"
                  >
                    삭제
                  </button>
                </div>
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
                  void uploadMedia("MAIN", e.target.files);
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
          </section>

          <section className="space-y-4">
            <div>
              <p className="text-sm font-medium">웨딩 갤러리</p>
              <p className="mt-1 text-xs text-muted">
                여러 장을 한 번에 선택할 수 있습니다. 공개 페이지에서는 카드 → 모달로 열립니다.
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
                  ? `업로드 중... ${uploadProgress ?? ""}`
                  : `갤러리 추가 (${gallery.length}/15, 다중 선택 가능)`}
              </span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                disabled={uploading !== null || gallery.length >= 15}
                onChange={(e) => {
                  void uploadMedia("GALLERY", e.target.files);
                  e.target.value = "";
                }}
                className="block w-full text-sm"
              />
            </label>

            {gallery.length > 0 && (
              <ul className="grid grid-cols-3 gap-2 sm:grid-cols-4">
                {gallery.map((item, index) => (
                  <li
                    key={item.id}
                    draggable
                    onDragStart={() => setDragIndex(index)}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={() => void dropGallery(index)}
                    onDragEnd={() => setDragIndex(null)}
                    className={`overflow-hidden rounded-xl border bg-white ${
                      dragIndex === index
                        ? "border-accent opacity-70"
                        : "border-accent/20"
                    }`}
                  >
                    <InvitationImage
                      auth
                      contentPath={item.contentPath}
                      alt=""
                      className="aspect-square w-full cursor-grab object-cover active:cursor-grabbing"
                    />
                    <div className="flex items-center justify-between gap-1 px-1.5 py-1">
                      <button
                        type="button"
                        onClick={() => void moveGallery(index, -1)}
                        disabled={index === 0}
                        className="px-1 text-[11px] text-muted disabled:opacity-30"
                        aria-label="앞으로"
                      >
                        ←
                      </button>
                      <button
                        type="button"
                        onClick={() => void deleteMedia(item.id, "GALLERY")}
                        className="text-[11px] text-red-600"
                      >
                        삭제
                      </button>
                      <button
                        type="button"
                        onClick={() => void moveGallery(index, 1)}
                        disabled={index === gallery.length - 1}
                        className="px-1 text-[11px] text-muted disabled:opacity-30"
                        aria-label="뒤로"
                      >
                        →
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
            {gallery.length > 1 && (
              <p className="text-[11px] text-muted">썸네일을 드래그해 순서를 바꿀 수 있습니다.</p>
            )}
          </section>

          {error && <p className="text-sm text-red-600">{error}</p>}
          {message && <p className="text-sm text-green-700">{message}</p>}

          <div className="fixed inset-x-0 bottom-0 z-30 border-t border-accent/20 bg-[#FAF8F5]/95 p-3 backdrop-blur lg:static lg:border-0 lg:bg-transparent lg:p-0 lg:backdrop-blur-none">
            <div className="mx-auto flex max-w-6xl gap-2 lg:block">
              <button
                type="button"
                onClick={() => setMobilePane("preview")}
                className="flex-1 rounded-full border border-accent/40 px-4 py-3 text-sm lg:hidden"
              >
                미리보기
              </button>
              <button
                type="submit"
                disabled={saving || !dirty}
                className="flex-[1.4] rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-50 lg:w-full lg:sticky lg:bottom-4"
              >
                {saving ? "저장 중..." : dirty ? "디자인 저장" : "저장됨"}
              </button>
            </div>
          </div>
        </form>

        <aside
          className={`lg:sticky lg:top-20 lg:block lg:self-start ${
            mobilePane === "preview" ? "block" : "hidden"
          }`}
        >
          <div className="mb-3 flex items-center justify-between lg:justify-center lg:gap-3">
            <p className="text-xs tracking-wide text-muted">
              미리보기 · {getInvitationTemplate(template).name}
            </p>
            <button
              type="button"
              onClick={() => setMobilePane("edit")}
              className="text-xs text-accent underline lg:hidden"
            >
              설정으로
            </button>
          </div>
          <div className="mx-auto w-full max-w-[360px] overflow-hidden rounded-[2rem] border border-black/10 bg-black shadow-[0_20px_50px_rgba(0,0,0,0.12)]">
            <div className="flex items-center justify-center gap-1 border-b border-white/10 bg-[#1c1c1e] px-4 py-2">
              <span className="h-1.5 w-1.5 rounded-full bg-white/30" />
              <span className="h-1 w-12 rounded-full bg-white/20" />
            </div>
            <div className="h-[min(72vh,700px)] overflow-y-auto overscroll-contain bg-[#FAF8F5]">
              {preview}
            </div>
          </div>
          {dirty && (
            <button
              type="button"
              onClick={() => void handleSave()}
              disabled={saving}
              className="mt-4 w-full rounded-full bg-accent px-6 py-3 text-sm text-white lg:hidden"
            >
              {saving ? "저장 중..." : "디자인 저장"}
            </button>
          )}
        </aside>
      </div>
    </main>
  );
}
