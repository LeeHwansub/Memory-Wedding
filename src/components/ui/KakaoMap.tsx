"use client";

import { useEffect, useRef, useState } from "react";
import { getKakaoMapAppKey, renderKakaoMapByAddress } from "@/lib/kakao-map";

type KakaoMapProps = {
  address: string;
  className?: string;
};

export function KakaoMap({ address, className = "" }: KakaoMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const container = containerRef.current;
    if (!container || !address.trim()) return;

    let cancelled = false;
    let cleanup: (() => void) | undefined;

    setLoading(true);
    setError(null);

    if (!getKakaoMapAppKey()) {
      setLoading(false);
      setError("카카오맵 키가 설정되지 않았습니다.");
      return;
    }

    renderKakaoMapByAddress(container, address.trim())
      .then((dispose) => {
        if (cancelled) {
          dispose();
          return;
        }
        cleanup = dispose;
        setLoading(false);
      })
      .catch((err) => {
        if (cancelled) return;
        setLoading(false);
        setError(err instanceof Error ? err.message : "지도를 불러오지 못했습니다.");
      });

    return () => {
      cancelled = true;
      cleanup?.();
    };
  }, [address]);

  return (
    <div className={`relative overflow-hidden rounded-2xl border border-accent/20 ${className}`}>
      <div ref={containerRef} className="h-56 w-full" />
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-[#FAF8F5]/90 text-sm text-muted">
          지도를 불러오는 중...
        </div>
      )}
      {error && (
        <div className="absolute inset-0 flex items-center justify-center bg-[#FAF8F5] px-4 text-center text-sm text-muted">
          {error}
        </div>
      )}
    </div>
  );
}
