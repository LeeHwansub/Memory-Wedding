"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { clearToken } from "@/lib/auth";

type AppHeaderProps = {
  title?: string;
};

export function AppHeader({ title = "Memory Wedding" }: AppHeaderProps) {
  const pathname = usePathname();
  const router = useRouter();
  const onDashboard = pathname === "/dashboard";

  function handleLogout() {
    clearToken();
    router.replace("/login");
  }

  return (
    <header className="sticky top-0 z-40 border-b border-accent/20 bg-[#FAF8F5]/95 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between gap-4 px-4 sm:px-6">
        <div className="flex min-w-0 items-center gap-3">
          <Link
            href="/dashboard"
            className="truncate text-sm tracking-[0.14em] text-foreground uppercase"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            {title}
          </Link>
          {!onDashboard && (
            <Link href="/dashboard" className="hidden text-xs text-muted hover:underline sm:inline">
              대시보드
            </Link>
          )}
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="rounded-full border border-accent/30 px-3 py-1.5 text-xs transition hover:bg-accent-soft"
        >
          로그아웃
        </button>
      </div>
    </header>
  );
}
