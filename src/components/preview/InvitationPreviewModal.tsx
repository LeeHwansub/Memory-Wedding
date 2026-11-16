"use client";

import { useEffect, useMemo, useState } from "react";
import { DEVICE_PRESETS } from "@/lib/project-nav";

type InvitationPreviewModalProps = {
  open: boolean;
  onClose: () => void;
  slug: string | null;
  title?: string;
};

export function InvitationPreviewModal({
  open,
  onClose,
  slug,
  title,
}: InvitationPreviewModalProps) {
  const [presetId, setPresetId] = useState<string>(DEVICE_PRESETS[0].id);
  const [customW, setCustomW] = useState(375);
  const [customH, setCustomH] = useState(667);
  const [scale, setScale] = useState(100);
  const [useCustom, setUseCustom] = useState(false);

  const size = useMemo(() => {
    if (useCustom) {
      return {
        width: Math.min(Math.max(customW, 280), 500),
        height: Math.min(Math.max(customH, 480), 1000),
      };
    }
    const preset = DEVICE_PRESETS.find((p) => p.id === presetId) ?? DEVICE_PRESETS[0];
    return { width: preset.width, height: preset.height };
  }, [customH, customW, presetId, useCustom]);

  // Tall devices (iPhone 14 / Pixel 등) are auto-fitted so the top isn't clipped.
  useEffect(() => {
    if (!open || typeof window === "undefined") return;
    const availableH = Math.max(window.innerHeight - 180, 320);
    const availableW = Math.max(Math.min(window.innerWidth - 80, 960), 280);
    const fit = Math.min(1, availableW / size.width, availableH / size.height);
    const next = Math.max(50, Math.min(100, Math.floor(fit * 100)));
    setScale(next);
  }, [open, size.height, size.width]);

  if (!open) return null;

  const scaleRatio = scale / 100;
  const layoutW = size.width * scaleRatio;
  const layoutH = size.height * scaleRatio;
  const src = slug ? `/w/${slug}?embed=1` : null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-stretch justify-center bg-black/45 p-3 sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="preview-modal-title"
    >
      <div className="flex max-h-full w-full max-w-5xl flex-col overflow-hidden rounded-2xl bg-[#F3F0EB] shadow-xl">
        <div className="flex flex-wrap items-center gap-2 border-b border-black/10 bg-white px-3 py-2.5 sm:gap-3 sm:px-4">
          <h2
            id="preview-modal-title"
            className="mr-auto text-sm font-medium"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            미리보기{title ? ` · ${title}` : ""}
          </h2>

          <label className="flex items-center gap-1.5 text-xs text-muted">
            <span className="hidden sm:inline">기기</span>
            <select
              value={useCustom ? "custom" : presetId}
              onChange={(e) => {
                if (e.target.value === "custom") {
                  setUseCustom(true);
                } else {
                  setUseCustom(false);
                  setPresetId(e.target.value);
                }
              }}
              className="rounded-lg border border-accent/30 bg-white px-2 py-1 text-xs text-foreground"
            >
              {DEVICE_PRESETS.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.label} ({p.width}×{p.height})
                </option>
              ))}
              <option value="custom">직접 입력</option>
            </select>
          </label>

          {useCustom && (
            <div className="flex items-center gap-1 text-xs">
              <input
                type="number"
                value={customW}
                onChange={(e) => setCustomW(Number(e.target.value) || 375)}
                className="w-16 rounded-lg border border-accent/30 px-2 py-1"
                aria-label="가로"
              />
              <span>×</span>
              <input
                type="number"
                value={customH}
                onChange={(e) => setCustomH(Number(e.target.value) || 667)}
                className="w-16 rounded-lg border border-accent/30 px-2 py-1"
                aria-label="세로"
              />
            </div>
          )}

          <label className="flex items-center gap-1.5 text-xs text-muted">
            <span className="hidden sm:inline">확대</span>
            <select
              value={scale}
              onChange={(e) => setScale(Number(e.target.value))}
              className="rounded-lg border border-accent/30 bg-white px-2 py-1 text-xs text-foreground"
            >
              {[50, 60, 75, 90, 100].map((v) => (
                <option key={v} value={v}>
                  {v}%
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-accent/30 px-3 py-1 text-xs transition hover:bg-accent-soft"
          >
            닫기
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-auto">
          <div className="flex justify-center p-4 sm:p-8">
            {!src ? (
              <p className="text-sm text-muted">프로젝트를 불러오는 중...</p>
            ) : (
              <div style={{ width: layoutW, height: layoutH }}>
                <div
                  className="overflow-hidden rounded-[2rem] border-[10px] border-[#1f1a17] bg-white shadow-2xl"
                  style={{
                    width: size.width,
                    height: size.height,
                    transform: `scale(${scaleRatio})`,
                    transformOrigin: "top left",
                  }}
                >
                  <iframe
                    title="청첩장 미리보기"
                    src={src}
                    className="h-full w-full border-0 bg-[#FAF8F5]"
                  />
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
