"use client";

import Link from "next/link";

type PreviewChromeProps = {
  projectHref: string;
  dashboardHref?: string;
};

/** Owner preview chrome — only render when logged in. */
export function PreviewChrome({
  projectHref,
  dashboardHref = "/dashboard",
}: PreviewChromeProps) {
  return (
    <header className="sticky top-0 z-40 border-b border-[#C9A87C]/25 bg-[#FAF8F5]/95 backdrop-blur">
      <div className="mx-auto flex max-w-lg items-center justify-between gap-3 px-4 py-3">
        <p className="text-xs tracking-wide text-[#8A7F78]">청첩장 미리보기</p>
        <div className="flex items-center gap-2">
          <Link
            href={projectHref}
            className="rounded-full border border-[#C9A87C]/40 px-3 py-1.5 text-xs text-[#2C2420] transition hover:bg-white"
          >
            프로젝트
          </Link>
          <Link
            href={dashboardHref}
            className="rounded-full bg-[#2C2420] px-3 py-1.5 text-xs text-white transition hover:opacity-90"
          >
            대시보드
          </Link>
        </div>
      </div>
    </header>
  );
}
