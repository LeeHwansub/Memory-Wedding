"use client";

import { useState } from "react";
import { InvitationImage } from "@/components/invitation/InvitationImage";
import { apiAssetUrl } from "@/lib/api";
import type {
  GalleryLayout,
  InvitationMedia,
  MediaDisplaySize,
} from "@/types/invitation";

type Props = {
  layout: GalleryLayout;
  columns: number;
  imageSize: MediaDisplaySize;
  items: InvitationMedia[];
  auth?: boolean;
};

function sizeClass(size: MediaDisplaySize, layout: GalleryLayout): string {
  if (layout === "VERTICAL") {
    if (size === "SM") return "max-h-56";
    if (size === "MD") return "max-h-80";
    if (size === "LG") return "max-h-[28rem]";
    return "max-h-none";
  }
  if (size === "SM") return "h-28";
  if (size === "MD") return "h-40";
  if (size === "LG") return "h-56";
  return "h-72";
}

function resolvePath(path: string, auth: boolean) {
  return auth ? path : apiAssetUrl(path);
}

export function WeddingGallery({
  layout,
  columns,
  imageSize,
  items,
  auth = false,
}: Props) {
  const [active, setActive] = useState(0);
  const [lightbox, setLightbox] = useState<number | null>(null);

  if (!items.length) return null;

  const path = (item: InvitationMedia) => resolvePath(item.contentPath, auth);

  if (layout === "SLIDER") {
    const current = items[Math.min(active, items.length - 1)];
    return (
      <div className="w-full">
        <button
          type="button"
          className="relative w-full overflow-hidden rounded-sm"
          onClick={() => setLightbox(active)}
        >
          <InvitationImage
            auth={auth}
            contentPath={path(current)}
            alt={current.originalFilename}
            className={`w-full object-cover ${sizeClass(imageSize, layout)}`}
          />
        </button>
        {items.length > 1 && (
          <div className="mt-3 flex gap-2 overflow-x-auto pb-1">
            {items.map((item, index) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setActive(index)}
                className={`relative h-14 w-14 shrink-0 overflow-hidden rounded-sm border ${
                  index === active ? "border-[#C9A87C]" : "border-transparent opacity-70"
                }`}
              >
                <InvitationImage
                  auth={auth}
                  contentPath={path(item)}
                  alt=""
                  className="h-full w-full object-cover"
                />
              </button>
            ))}
          </div>
        )}
        <Lightbox
          auth={auth}
          items={items}
          index={lightbox}
          onClose={() => setLightbox(null)}
          onChange={setLightbox}
        />
      </div>
    );
  }

  if (layout === "VERTICAL") {
    return (
      <div className="w-full space-y-3">
        {items.map((item, index) => (
          <button
            key={item.id}
            type="button"
            className="block w-full overflow-hidden"
            onClick={() => setLightbox(index)}
          >
            <InvitationImage
              auth={auth}
              contentPath={path(item)}
              alt={item.originalFilename}
              className={`w-full object-cover ${sizeClass(imageSize, layout)}`}
            />
          </button>
        ))}
        <Lightbox
          auth={auth}
          items={items}
          index={lightbox}
          onClose={() => setLightbox(null)}
          onChange={setLightbox}
        />
      </div>
    );
  }

  const colClass =
    columns >= 4 ? "columns-4" : columns === 3 ? "columns-3" : "columns-2";

  return (
    <div className={`w-full gap-2 ${colClass} space-y-2`}>
      {items.map((item, index) => (
        <button
          key={item.id}
          type="button"
          className="mb-2 block w-full break-inside-avoid overflow-hidden"
          onClick={() => setLightbox(index)}
        >
          <InvitationImage
            auth={auth}
            contentPath={path(item)}
            alt={item.originalFilename}
            className="w-full object-cover"
            style={{
              aspectRatio: index % 5 === 0 ? "3/4" : index % 3 === 0 ? "1/1" : "4/3",
            }}
          />
        </button>
      ))}
      <Lightbox
        auth={auth}
        items={items}
        index={lightbox}
        onClose={() => setLightbox(null)}
        onChange={setLightbox}
      />
    </div>
  );
}

function Lightbox({
  auth,
  items,
  index,
  onClose,
  onChange,
}: {
  auth: boolean;
  items: InvitationMedia[];
  index: number | null;
  onClose: () => void;
  onChange: (next: number | null) => void;
}) {
  if (index === null) return null;
  const item = items[index];
  if (!item) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
      role="dialog"
      aria-modal
      onClick={onClose}
    >
      <button
        type="button"
        className="absolute right-4 top-4 text-sm text-white"
        onClick={onClose}
      >
        닫기
      </button>
      {items.length > 1 && (
        <>
          <button
            type="button"
            className="absolute left-3 top-1/2 -translate-y-1/2 px-3 py-2 text-white"
            onClick={(e) => {
              e.stopPropagation();
              onChange((index - 1 + items.length) % items.length);
            }}
          >
            ‹
          </button>
          <button
            type="button"
            className="absolute right-3 top-1/2 -translate-y-1/2 px-3 py-2 text-white"
            onClick={(e) => {
              e.stopPropagation();
              onChange((index + 1) % items.length);
            }}
          >
            ›
          </button>
        </>
      )}
      <div className="max-h-[85vh] max-w-3xl" onClick={(e) => e.stopPropagation()}>
        <InvitationImage
          auth={auth}
          contentPath={resolvePath(item.contentPath, auth)}
          alt={item.originalFilename}
          className="max-h-[85vh] w-auto max-w-full object-contain"
        />
      </div>
    </div>
  );
}
