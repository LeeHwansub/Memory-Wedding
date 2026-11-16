"use client";

import {
  INVITATION_TEMPLATES,
  type InvitationTemplateDef,
} from "@/lib/invitation-templates";
import type { InvitationTemplate } from "@/types/invitation";

type Props = {
  value: InvitationTemplate;
  applyPresets: boolean;
  onApplyPresetsChange: (next: boolean) => void;
  onSelect: (id: InvitationTemplate) => void;
};

function MiniPreview({ item }: { item: InvitationTemplateDef }) {
  const { theme, presets } = item;
  const isTop = presets.mainPhotoPlacement === "TOP";

  return (
    <div
      className="relative mx-auto h-[88px] w-[56px] overflow-hidden rounded-[10px] border shadow-sm"
      style={{
        backgroundColor: theme.bg,
        borderColor: theme.line,
        color: theme.fg,
      }}
    >
      {isTop ? (
        <div
          className="relative h-[42px] w-full"
          style={{
            background: `linear-gradient(160deg, ${theme.soft}, ${theme.accent}55)`,
          }}
        >
          <div
            className="absolute inset-x-0 bottom-0 px-1 pb-1 text-center"
            style={{ background: theme.heroOverlay }}
          >
            <p className="text-[5px] leading-none text-white/90">A & B</p>
          </div>
        </div>
      ) : (
        <div className="px-1.5 pt-2 text-center">
          <p
            className="text-[5px] tracking-wide uppercase"
            style={{ color: theme.muted }}
          >
            Invite
          </p>
          <p
            className="mt-0.5 text-[7px] font-light leading-none"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            A <span style={{ color: theme.accent }}>&</span> B
          </p>
          <div
            className="mx-auto mt-1.5 h-7 w-full rounded-sm"
            style={{
              background: `linear-gradient(160deg, ${theme.soft}, ${theme.accent}66)`,
              clipPath: "polygon(0% 10%, 100% 0%, 100% 90%, 0% 100%)",
            }}
          />
        </div>
      )}
      <div className="absolute inset-x-1 bottom-1.5 space-y-0.5">
        <div className="mx-auto h-px w-4" style={{ backgroundColor: theme.line }} />
        <div className="grid grid-cols-2 gap-0.5">
          <div className="aspect-square rounded-[1px]" style={{ backgroundColor: theme.soft }} />
          <div
            className="aspect-square rounded-[1px]"
            style={{ backgroundColor: `${theme.accent}55` }}
          />
        </div>
      </div>
    </div>
  );
}

export function InvitationTemplatePicker({
  value,
  applyPresets,
  onApplyPresetsChange,
  onSelect,
}: Props) {
  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-sm font-medium">템플릿</p>
          <p className="mt-1 text-xs text-muted">
            미니 미리보기로 색감·구성을 비교한 뒤 선택하세요.
          </p>
        </div>
        <label className="flex items-center gap-2 text-xs text-muted">
          <input
            type="checkbox"
            checked={applyPresets}
            onChange={(e) => onApplyPresetsChange(e.target.checked)}
            className="rounded border-accent/40"
          />
          레이아웃 프리셋도 적용
        </label>
      </div>

      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 sm:grid sm:grid-cols-3 sm:overflow-visible lg:grid-cols-4 xl:grid-cols-7">
        {INVITATION_TEMPLATES.map((item) => {
          const selected = value === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onSelect(item.id)}
              className={`min-w-[108px] shrink-0 rounded-2xl border p-2.5 text-left transition sm:min-w-0 ${
                selected
                  ? "border-foreground bg-white shadow-sm ring-1 ring-foreground"
                  : "border-accent/20 bg-white/70 hover:border-accent/40"
              }`}
            >
              <MiniPreview item={item} />
              <span className="mt-2 block text-center text-xs font-medium">
                {item.name}
              </span>
              <span className="mt-0.5 block text-center text-[10px] leading-snug text-muted">
                {item.description}
              </span>
              <span className="mt-1.5 block text-center text-[10px] text-muted">
                {item.presets.mainPhotoPlacement === "TOP" ? "상단" : "중간"} ·{" "}
                {item.presets.galleryLayout === "SLIDER"
                  ? "슬라이더"
                  : item.presets.galleryLayout === "COLLAGE"
                    ? "콜라주"
                    : "세로"}
              </span>
            </button>
          );
        })}
      </div>
    </section>
  );
}
