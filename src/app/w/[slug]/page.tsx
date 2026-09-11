"use client";

import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { GuestbookCarousel } from "@/components/guestbook/GuestbookCarousel";
import { GalleryCard } from "@/components/invitation/GalleryCard";
import { MainPhotoHero } from "@/components/invitation/MainPhotoHero";
import { KakaoMap } from "@/components/ui/KakaoMap";
import { PreviewChrome } from "@/components/ui/PreviewChrome";
import { apiPublicFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import { toDatetimeLocalValue } from "@/lib/datetime";
import type { PublicInvitation } from "@/types/invitation";

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div className="mb-6 flex flex-col items-center gap-3">
      <span
        className="text-[11px] tracking-[0.28em] text-[#C9A87C] uppercase"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        {children}
      </span>
      <span className="h-px w-10 bg-[#C9A87C]/50" aria-hidden />
    </div>
  );
}

export default function PublicInvitationPage() {
  const params = useParams<{ slug: string }>();
  const searchParams = useSearchParams();
  const [data, setData] = useState<PublicInvitation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [showPreviewChrome, setShowPreviewChrome] = useState(false);
  const fromProjectId = searchParams.get("from");
  const embed = searchParams.get("embed") === "1";

  useEffect(() => {
    setShowPreviewChrome(Boolean(getToken()) && !embed);
    apiPublicFetch<PublicInvitation>(`/api/public/w/${params.slug}`)
      .then((res) => setData(res.data))
      .catch((err) => setError(err instanceof Error ? err.message : "불러오기 실패"))
      .finally(() => setLoading(false));
  }, [embed, params.slug]);

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#FAF8F5]">
        <p className="text-muted">청첩장을 불러오는 중...</p>
      </main>
    );
  }

  if (error || !data) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#FAF8F5] px-6">
        <div className="max-w-md text-center">
          <h1
            className="mb-3 text-2xl font-light"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            청첩장을 볼 수 없습니다
          </h1>
          <p className="text-sm text-muted">{error ?? "유효하지 않은 링크입니다."}</p>
        </div>
      </main>
    );
  }

  const weddingLocal = toDatetimeLocalValue(data.weddingAt);
  const [datePart, timePart] = weddingLocal.split("T");
  const weddingDateOnly = (() => {
    if (!datePart || !timePart) return data.weddingAt;
    const [year, month, day] = datePart.split("-").map(Number);
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "long",
      day: "numeric",
      weekday: "long",
    });
  })();
  const weddingTimeOnly = (() => {
    if (!timePart) return "";
    const [hour, minute] = timePart.split(":").map(Number);
    const date = new Date(2000, 0, 1, hour, minute);
    return date.toLocaleTimeString("ko-KR", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  })();

  const hasTopMain =
    Boolean(data.mainPhoto) && data.mainPhotoPlacement === "TOP";
  const hasMiddleMain =
    Boolean(data.mainPhoto) && data.mainPhotoPlacement === "MIDDLE";
  const gallery = data.gallery ?? [];

  return (
    <main className="min-h-screen bg-[#FAF8F5] text-[#2C2420]">
      {showPreviewChrome && (
        <PreviewChrome
          projectHref={
            fromProjectId
              ? `/dashboard/projects/${fromProjectId}`
              : "/dashboard"
          }
        />
      )}

      {hasTopMain && data.mainPhoto && (
        <MainPhotoHero
          photo={data.mainPhoto}
          placement="TOP"
          size={data.mainPhotoSize ?? "LG"}
          brightness={data.mainBrightness ?? 1}
          saturation={data.mainSaturation ?? 1}
          focalX={data.mainFocalX ?? 50}
          focalY={data.mainFocalY ?? 50}
          groomName={data.groomName}
          brideName={data.brideName}
          title={data.title}
        />
      )}

      <section className="mx-auto flex min-h-screen max-w-lg flex-col px-6 py-16 text-center">
        {!hasTopMain && (
          <>
            <p className="mb-4 text-[11px] tracking-[0.32em] text-[#8A7F78] uppercase">
              Wedding Invitation
            </p>
            <h1
              className="mb-3 text-[2.35rem] font-light leading-tight tracking-wide"
              style={{ fontFamily: "var(--font-playfair), serif" }}
            >
              {data.groomName}
              <span className="mx-3 inline-block text-lg text-[#C9A87C]" aria-hidden>
                &
              </span>
              {data.brideName}
            </h1>
            {data.title && (
              <p className="mb-12 text-sm tracking-wide text-[#C9A87C]">{data.title}</p>
            )}
            {!data.title && <div className="mb-12" />}
          </>
        )}

        {hasTopMain && <div className="mb-8" />}

        <div className="mb-14 space-y-1.5">
          <p className="text-[13px] tracking-[0.12em] text-[#8A7F78]">
            {weddingDateOnly}
          </p>
          <p className="text-sm font-medium tracking-wide">{weddingTimeOnly}</p>
          {(data.venueName || data.venueAddress) && (
            <div className="pt-4">
              {data.venueName && (
                <p className="text-sm font-medium tracking-wide">{data.venueName}</p>
              )}
              {data.venueAddress && (
                <p className="mt-1 text-xs leading-relaxed text-[#8A7F78]">
                  {data.venueAddress}
                </p>
              )}
            </div>
          )}
        </div>

        {hasMiddleMain && data.mainPhoto && (
          <div className="mb-16 -mx-6">
            <MainPhotoHero
              photo={data.mainPhoto}
              placement="MIDDLE"
              size={data.mainPhotoSize ?? "LG"}
              brightness={data.mainBrightness ?? 1}
              saturation={data.mainSaturation ?? 1}
              focalX={data.mainFocalX ?? 50}
              focalY={data.mainFocalY ?? 50}
              groomName={data.groomName}
              brideName={data.brideName}
              title={data.title}
            />
          </div>
        )}

        {data.greetingMessage && (
          <div className="mb-16">
            <SectionLabel>Greeting</SectionLabel>
            <p className="mx-auto max-w-sm whitespace-pre-wrap text-center text-[15px] leading-[1.9] tracking-wide text-[#2C2420]/90">
              {data.greetingMessage}
            </p>
          </div>
        )}

        {gallery.length > 0 && (
          <div className="mb-16">
            <SectionLabel>Gallery</SectionLabel>
            <GalleryCard
              layout={data.galleryLayout ?? "SLIDER"}
              columns={data.galleryColumns ?? 2}
              imageSize={data.galleryImageSize ?? "MD"}
              items={gallery}
            />
          </div>
        )}

        {(data.venueAddress || data.mapUrl) && (
          <div className="mb-16">
            <SectionLabel>Location</SectionLabel>
            {data.venueName && (
              <p className="mb-1 text-sm font-medium tracking-wide">{data.venueName}</p>
            )}
            {data.venueAddress && (
              <p className="mb-5 text-xs leading-relaxed text-[#8A7F78]">
                {data.venueAddress}
              </p>
            )}
            {data.venueAddress && <KakaoMap address={data.venueAddress} />}
            {data.mapUrl && (
              <a
                href={data.mapUrl}
                target="_blank"
                rel="noreferrer"
                className="mt-4 inline-flex text-xs tracking-[0.14em] text-[#C9A87C] underline-offset-4 transition hover:underline"
              >
                카카오맵에서 열기
              </a>
            )}
          </div>
        )}

        {data.accounts?.length > 0 && (
          <div className="mb-16">
            <SectionLabel>Account</SectionLabel>
            <ul className="mx-auto max-w-sm divide-y divide-[#C9A87C]/20 text-left">
              {data.accounts.map((account, index) => (
                <li
                  key={`${account.relation}-${index}`}
                  className="flex items-baseline justify-between gap-4 py-3.5 first:pt-0 last:pb-0"
                >
                  <span className="shrink-0 text-[13px] tracking-wide text-[#8A7F78]">
                    {account.relation}
                  </span>
                  <span className="min-w-0 text-right">
                    <span className="block text-[13px] text-[#2C2420]">
                      {account.bankName}
                    </span>
                    <span className="mt-0.5 block font-medium tracking-[0.06em] text-[#2C2420]">
                      {account.accountNumber}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}

        <GuestbookCarousel slug={data.slug} />

        <div className="mt-auto flex flex-col gap-3 pt-6">
          <Link
            href={`/w/${data.slug}/guestbook`}
            className="rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90"
          >
            축하 메시지 남기기
          </Link>
          <Link
            href={`/w/${data.slug}/upload`}
            className="rounded-full border border-accent/40 px-6 py-3 text-sm transition hover:bg-accent-soft"
          >
            사진·영상 올리기
          </Link>
        </div>
      </section>
    </main>
  );
}
