"use client";

import { useEffect, useState } from "react";
import { apiPublicFetch } from "@/lib/api";
import { getGuestbookClientKey } from "@/lib/guestbook-client";
import type { GuestbookEntry } from "@/types/guestbook";

type GuestbookLikeButtonProps = {
  slug: string;
  entry: GuestbookEntry;
  onUpdated?: (entry: GuestbookEntry) => void;
  className?: string;
};

export function GuestbookLikeButton({
  slug,
  entry,
  onUpdated,
  className = "",
}: GuestbookLikeButtonProps) {
  const [liked, setLiked] = useState(entry.likedByMe);
  const [count, setCount] = useState(entry.likeCount);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    setLiked(entry.likedByMe);
    setCount(entry.likeCount);
  }, [entry.id, entry.likedByMe, entry.likeCount]);

  async function handleLike() {
    if (pending || liked) return;
    setPending(true);
    try {
      const clientKey = getGuestbookClientKey();
      const res = await apiPublicFetch<GuestbookEntry>(
        `/api/public/w/${slug}/guestbook/${entry.id}/like`,
        {
          method: "POST",
          body: JSON.stringify({ clientKey }),
        },
      );
      if (res.data) {
        setLiked(true);
        setCount(res.data.likeCount);
        onUpdated?.(res.data);
      }
    } catch {
      // keep previous state on failure
    } finally {
      setPending(false);
    }
  }

  return (
    <button
      type="button"
      onClick={handleLike}
      disabled={pending || liked}
      aria-pressed={liked}
      title={liked ? "이미 좋아요를 눌렀습니다" : "좋아요"}
      className={`inline-flex items-center gap-1.5 text-xs tracking-wide transition disabled:opacity-70 ${
        liked ? "cursor-default text-[#C9A87C]" : "text-[#8A7F78] hover:text-[#C9A87C]"
      } ${className}`}
    >
      <span aria-hidden>{liked ? "♥" : "♡"}</span>
      <span>{count}</span>
    </button>
  );
}
