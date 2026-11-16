"use client";

import { useEffect, useRef, useState } from "react";
import { apiAssetUrl } from "@/lib/api";

type InvitationBgmPlayerProps = {
  contentPath: string;
};

export function InvitationBgmPlayer({ contentPath }: InvitationBgmPlayerProps) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [playing, setPlaying] = useState(false);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    const onPlay = () => setPlaying(true);
    const onPause = () => setPlaying(false);
    audio.addEventListener("play", onPlay);
    audio.addEventListener("pause", onPause);
    return () => {
      audio.removeEventListener("play", onPlay);
      audio.removeEventListener("pause", onPause);
    };
  }, []);

  async function toggle() {
    const audio = audioRef.current;
    if (!audio) return;
    try {
      if (audio.paused) {
        audio.volume = 0.55;
        await audio.play();
      } else {
        audio.pause();
      }
    } catch {
      setPlaying(false);
    }
  }

  return (
    <>
      <audio ref={audioRef} src={apiAssetUrl(contentPath)} loop preload="none" />
      <button
        type="button"
        onClick={() => void toggle()}
        className="fixed bottom-5 right-5 z-40 rounded-full border border-black/10 bg-white/90 px-4 py-2 text-xs shadow-md backdrop-blur"
        aria-label={playing ? "음악 일시정지" : "음악 재생"}
      >
        {playing ? "음악 끄기" : "음악 켜기"}
      </button>
    </>
  );
}
