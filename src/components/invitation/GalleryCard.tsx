"use client";

import { useState } from "react";
import { InvitationImage } from "@/components/invitation/InvitationImage";
import { WeddingGallery } from "@/components/invitation/WeddingGallery";
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

function resolvePath(path: string, auth: boolean) {
  return auth ? path : apiAssetUrl(path);
}

/** Compact gallery entry on the invitation — opens full viewer in a modal. */
export function GalleryCard({
  layout,
  columns,
  imageSize,
  items,
  auth = false,
}: Props) {
  const [open, setOpen] = useState(false);

  if (!items.length) return null;

  const cover = items.slice(0, 4);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="갤러리 보기"
        className="group w-full overflow-hidden rounded-2xl border bg-[var(--inv-bg,#fff)] transition"
        style={{ borderColor: "var(--inv-line, rgba(201,168,124,0.25))" }}
      >
        <div
          className={`grid gap-0.5 ${
            cover.length === 1
              ? "grid-cols-1"
              : cover.length === 2
                ? "grid-cols-2"
                : "grid-cols-2"
          }`}
        >
          {cover.map((item, index) => (
            <div
              key={item.id}
              className={`relative overflow-hidden ${
                cover.length === 3 && index === 0 ? "col-span-2 aspect-[2/1]" : "aspect-square"
              } ${cover.length === 1 ? "aspect-[4/3]" : ""}`}
              style={{ backgroundColor: "var(--inv-soft, #F3EBE0)" }}
            >
              <InvitationImage
                auth={auth}
                contentPath={resolvePath(item.contentPath, auth)}
                alt=""
                className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.02]"
              />
            </div>
          ))}
        </div>
      </button>

      {open && (
        <div
          className="fixed inset-0 z-50 flex items-stretch justify-center bg-black/55 p-3 sm:p-6"
          role="dialog"
          aria-modal="true"
          aria-label="웨딩 갤러리"
          onClick={() => setOpen(false)}
        >
          <div
            className="flex max-h-full w-full max-w-lg flex-col overflow-hidden rounded-2xl shadow-xl"
            style={{ backgroundColor: "var(--inv-bg, #FAF8F5)", color: "var(--inv-fg, #2C2420)" }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              className="flex items-center justify-between px-4 py-3"
              style={{ borderBottom: "1px solid var(--inv-line, rgba(201,168,124,0.2))" }}
            >
              <p
                className="text-sm tracking-wide"
                style={{ fontFamily: "var(--font-playfair), serif" }}
              >
                Gallery
              </p>
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="rounded-full px-3 py-1.5 text-xs text-muted hover:bg-accent-soft"
              >
                닫기
              </button>
            </div>
            <div className="min-h-0 flex-1 overflow-y-auto px-4 py-5">
              <WeddingGallery
                layout={layout}
                columns={columns}
                imageSize={imageSize}
                items={items}
                auth={auth}
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
