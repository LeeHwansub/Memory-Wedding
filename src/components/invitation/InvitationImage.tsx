"use client";

import { useEffect, useState } from "react";
import { apiFetchBlob } from "@/lib/api";

type Props = {
  contentPath: string;
  alt: string;
  className?: string;
  style?: React.CSSProperties;
  auth?: boolean;
};

export function InvitationImage({
  contentPath,
  alt,
  className,
  style,
  auth = false,
}: Props) {
  const [src, setSrc] = useState<string | null>(auth ? null : contentPath);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (!auth) {
      setSrc(contentPath);
      return;
    }

    let revoked = false;
    let objectUrl: string | null = null;

    apiFetchBlob(contentPath)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob);
        if (!revoked) setSrc(objectUrl);
      })
      .catch(() => {
        if (!revoked) setFailed(true);
      });

    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [auth, contentPath]);

  if (failed) {
    return (
      <div
        className={`flex items-center justify-center bg-[#F3EBE0] text-xs text-muted ${className ?? ""}`}
      >
        이미지를 불러올 수 없습니다
      </div>
    );
  }

  if (!src) {
    return (
      <div
        className={`animate-pulse bg-[#F3EBE0] ${className ?? ""}`}
        aria-hidden
      />
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={alt} className={className} style={style} />
  );
}
