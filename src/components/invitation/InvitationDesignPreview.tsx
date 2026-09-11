"use client";

import { MainPhotoHero } from "@/components/invitation/MainPhotoHero";
import { GalleryCard } from "@/components/invitation/GalleryCard";
import type {
  GalleryLayout,
  InvitationMedia,
  MainPhotoPlacement,
  MediaDisplaySize,
} from "@/types/invitation";

type Props = {
  auth?: boolean;
  groomName: string;
  brideName: string;
  title?: string | null;
  greetingMessage?: string | null;
  weddingDateLabel?: string;
  weddingTimeLabel?: string;
  venueName?: string | null;
  mainPhoto?: InvitationMedia | null;
  mainPhotoPlacement: MainPhotoPlacement;
  mainPhotoSize: MediaDisplaySize;
  mainBrightness: number;
  mainSaturation: number;
  mainFocalX: number;
  mainFocalY: number;
  gallery: InvitationMedia[];
  galleryLayout: GalleryLayout;
  galleryColumns: number;
  galleryImageSize: MediaDisplaySize;
};

/** Live phone-frame preview for the design editor (matches public card gallery UX). */
export function InvitationDesignPreview(props: Props) {
  const {
    auth = false,
    groomName,
    brideName,
    title,
    greetingMessage,
    weddingDateLabel,
    weddingTimeLabel,
    venueName,
    mainPhoto,
    mainPhotoPlacement,
    mainPhotoSize,
    mainBrightness,
    mainSaturation,
    mainFocalX,
    mainFocalY,
    gallery,
    galleryLayout,
    galleryColumns,
    galleryImageSize,
  } = props;

  const hasTopMain = Boolean(mainPhoto) && mainPhotoPlacement === "TOP";
  const hasMiddleMain = Boolean(mainPhoto) && mainPhotoPlacement === "MIDDLE";

  return (
    <div className="bg-[#FAF8F5] text-[#2C2420]">
      {hasTopMain && mainPhoto && (
        <MainPhotoHero
          auth={auth}
          photo={mainPhoto}
          placement="TOP"
          size={mainPhotoSize}
          brightness={mainBrightness}
          saturation={mainSaturation}
          focalX={mainFocalX}
          focalY={mainFocalY}
          groomName={groomName}
          brideName={brideName}
          title={title}
        />
      )}

      <div className="px-5 py-8 text-center">
        {!hasTopMain && (
          <>
            <p className="mb-3 text-[10px] tracking-[0.28em] text-[#8A7F78] uppercase">
              Wedding Invitation
            </p>
            <h1
              className="mb-2 text-[1.65rem] font-light leading-tight"
              style={{ fontFamily: "var(--font-playfair), serif" }}
            >
              {groomName}
              <span className="mx-2 text-base text-[#C9A87C]">&</span>
              {brideName}
            </h1>
            {title && (
              <p className="mb-8 text-xs tracking-wide text-[#C9A87C]">{title}</p>
            )}
            {!title && <div className="mb-8" />}
          </>
        )}

        {(weddingDateLabel || weddingTimeLabel || venueName) && (
          <div className="mb-8 space-y-1 text-[12px] text-[#8A7F78]">
            {weddingDateLabel && <p>{weddingDateLabel}</p>}
            {weddingTimeLabel && (
              <p className="font-medium text-[#2C2420]">{weddingTimeLabel}</p>
            )}
            {venueName && <p className="pt-2">{venueName}</p>}
          </div>
        )}

        {hasMiddleMain && mainPhoto && (
          <div className="mb-8 -mx-5">
            <MainPhotoHero
              auth={auth}
              photo={mainPhoto}
              placement="MIDDLE"
              size={mainPhotoSize}
              brightness={mainBrightness}
              saturation={mainSaturation}
              focalX={mainFocalX}
              focalY={mainFocalY}
              groomName={groomName}
              brideName={brideName}
              title={title}
            />
          </div>
        )}

        {greetingMessage && (
          <div className="mb-8">
            <p className="mb-3 text-[10px] tracking-[0.22em] text-[#C9A87C] uppercase">
              Greeting
            </p>
            <p className="whitespace-pre-wrap text-[13px] leading-relaxed text-[#2C2420]/90">
              {greetingMessage}
            </p>
          </div>
        )}

        {gallery.length > 0 && (
          <div className="mb-4 text-left">
            <p className="mb-3 text-center text-[10px] tracking-[0.22em] text-[#C9A87C] uppercase">
              Gallery
            </p>
            <GalleryCard
              auth={auth}
              layout={galleryLayout}
              columns={galleryColumns}
              imageSize={galleryImageSize}
              items={gallery}
            />
          </div>
        )}

        {!mainPhoto && gallery.length === 0 && (
          <p className="py-16 text-xs text-muted">
            메인 사진이나 갤러리를 추가하면
            <br />
            여기에 미리보기가 표시됩니다.
          </p>
        )}
      </div>
    </div>
  );
}
