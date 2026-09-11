"use client";

import { InvitationImage } from "@/components/invitation/InvitationImage";
import { apiAssetUrl } from "@/lib/api";
import type {
  InvitationMedia,
  MainPhotoPlacement,
  MediaDisplaySize,
} from "@/types/invitation";

type Props = {
  photo: InvitationMedia;
  placement: MainPhotoPlacement;
  size: MediaDisplaySize;
  brightness: number;
  saturation: number;
  focalX: number;
  focalY: number;
  groomName: string;
  brideName: string;
  title?: string | null;
  auth?: boolean;
};

function heightClass(size: MediaDisplaySize, placement: MainPhotoPlacement) {
  if (placement === "TOP") {
    if (size === "SM") return "h-[42vh] min-h-[220px]";
    if (size === "MD") return "h-[52vh] min-h-[280px]";
    if (size === "LG") return "h-[68vh] min-h-[340px]";
    return "h-[88vh] min-h-[420px]";
  }
  if (size === "SM") return "h-48";
  if (size === "MD") return "h-64";
  if (size === "LG") return "h-80";
  return "h-96";
}

export function MainPhotoHero({
  photo,
  placement,
  size,
  brightness,
  saturation,
  focalX,
  focalY,
  groomName,
  brideName,
  title,
  auth = false,
}: Props) {
  const contentPath = auth ? photo.contentPath : apiAssetUrl(photo.contentPath);
  const filter = `brightness(${brightness}) saturate(${saturation})`;
  const objectPosition = `${focalX}% ${focalY}%`;

  if (placement === "TOP") {
    return (
      <section className={`relative w-full overflow-hidden ${heightClass(size, placement)}`}>
        <InvitationImage
          auth={auth}
          contentPath={contentPath}
          alt="메인 사진"
          className="absolute inset-0 h-full w-full object-cover"
          style={{ filter, objectPosition }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/45 via-black/15 to-transparent" />
        <div className="absolute inset-x-0 bottom-0 px-6 pb-10 text-center text-white">
          <p className="mb-3 text-[11px] tracking-[0.32em] uppercase opacity-90">
            Wedding Invitation
          </p>
          <h1
            className="text-[2.2rem] font-light leading-tight tracking-wide"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            {groomName}
            <span className="mx-3 inline-block text-lg text-[#E8D5B5]" aria-hidden>
              &
            </span>
            {brideName}
          </h1>
          {title && (
            <p className="mt-3 text-sm tracking-wide text-[#E8D5B5]">{title}</p>
          )}
        </div>
      </section>
    );
  }

  return (
    <div className="mx-auto w-full max-w-md px-6">
      <div
        className={`relative overflow-hidden rounded-[2rem] ${heightClass(size, placement)}`}
        style={{
          clipPath:
            "polygon(0% 8%, 100% 0%, 100% 92%, 0% 100%)",
        }}
      >
        <InvitationImage
          auth={auth}
          contentPath={contentPath}
          alt="메인 사진"
          className="h-full w-full object-cover"
          style={{ filter, objectPosition }}
        />
      </div>
    </div>
  );
}
