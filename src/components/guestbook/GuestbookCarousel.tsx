"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { GuestbookLikeButton } from "@/components/guestbook/GuestbookLikeButton";
import { apiPublicFetch } from "@/lib/api";
import { getGuestbookClientKey } from "@/lib/guestbook-client";
import type { GuestbookEntry, GuestbookPage } from "@/types/guestbook";

type GuestbookCarouselProps = {
  slug: string;
};

const PREVIEW_SIZE = 5;

export function GuestbookCarousel({ slug }: GuestbookCarouselProps) {
  const [entries, setEntries] = useState<GuestbookEntry[]>([]);
  const [index, setIndex] = useState(0);
  const [visible, setVisible] = useState(true);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    apiPublicFetch<GuestbookPage>(
      `/api/public/w/${slug}/guestbook?page=0&size=${PREVIEW_SIZE}&clientKey=${encodeURIComponent(getGuestbookClientKey())}`,
    )
      .then((res) => {
        if (!cancelled) setEntries(res.data?.content ?? []);
      })
      .catch(() => {
        if (!cancelled) setEntries([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [slug]);

  useEffect(() => {
    if (entries.length <= 1) return;
    const timer = window.setInterval(() => {
      setVisible(false);
      window.setTimeout(() => {
        setIndex((prev) => (prev + 1) % entries.length);
        setVisible(true);
      }, 300);
    }, 4500);
    return () => window.clearInterval(timer);
  }, [entries.length]);

  if (loading || entries.length === 0) {
    return null;
  }

  const current = entries[index] ?? entries[0];

  return (
    <div className="mb-16">
      <div className="mb-7 flex flex-col items-center gap-3">
        <span
          className="text-[11px] tracking-[0.28em] text-[#C9A87C] uppercase"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          Guestbook
        </span>
        <span className="h-px w-10 bg-[#C9A87C]/50" aria-hidden />
      </div>

      <div className="relative mx-auto max-w-sm">
        <div
          className={`relative overflow-hidden rounded-[1.75rem] transition-all duration-300 ${
            visible ? "translate-y-0 opacity-100" : "translate-y-2 opacity-0"
          }`}
          style={{
            background:
              "linear-gradient(165deg, rgba(255,255,255,0.92) 0%, rgba(243,235,224,0.75) 100%)",
            boxShadow: "0 12px 40px -24px rgba(44, 36, 32, 0.35)",
          }}
        >
          <div
            className="pointer-events-none absolute inset-x-6 top-0 h-px bg-gradient-to-r from-transparent via-[#C9A87C]/60 to-transparent"
            aria-hidden
          />
          <div className="px-6 pb-6 pt-7 text-left">
            <p
              className="mb-3 text-4xl leading-none text-[#C9A87C]/55"
              style={{ fontFamily: "var(--font-playfair), serif" }}
              aria-hidden
            >
              “
            </p>
            <p className="min-h-[6.5rem] whitespace-pre-wrap text-[15px] leading-[1.85] tracking-wide text-[#2C2420]/90">
              {current.message.length > 120
                ? `${current.message.slice(0, 120).trimEnd()}…`
                : current.message}
            </p>
            <div className="mt-5 flex items-center justify-between gap-3 border-t border-[#C9A87C]/20 pt-4">
              <div>
                <p
                  className="text-sm tracking-wide text-[#2C2420]"
                  style={{ fontFamily: "var(--font-playfair), serif" }}
                >
                  {current.guestName}
                </p>
                <p className="mt-0.5 text-[11px] text-[#8A7F78]">
                  {formatEntryTime(current.createdAt)}
                </p>
              </div>
              <GuestbookLikeButton
                slug={slug}
                entry={current}
                onUpdated={(updated) => {
                  setEntries((prev) =>
                    prev.map((item) => (item.id === updated.id ? updated : item)),
                  );
                }}
              />
            </div>
          </div>
        </div>

        {entries.length > 1 && (
          <div className="mt-5 flex items-center justify-center gap-2">
            {entries.map((entry, i) => (
              <button
                key={entry.id}
                type="button"
                aria-label={`${i + 1}번째 메시지`}
                onClick={() => {
                  setVisible(false);
                  window.setTimeout(() => {
                    setIndex(i);
                    setVisible(true);
                  }, 200);
                }}
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  i === index ? "w-5 bg-[#C9A87C]" : "w-1.5 bg-[#C9A87C]/30"
                }`}
              />
            ))}
          </div>
        )}
      </div>

      <div className="mt-6 text-center">
        <Link
          href={`/w/${slug}/guestbook`}
          className="text-xs tracking-[0.16em] text-[#C9A87C] underline-offset-4 transition hover:underline"
        >
          방명록 전체 보기
        </Link>
      </div>
    </div>
  );
}

function formatEntryTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
  });
}
