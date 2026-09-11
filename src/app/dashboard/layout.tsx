"use client";

import { usePathname } from "next/navigation";
import { AppHeader } from "@/components/shell/AppHeader";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const inProjectDetail = /^\/dashboard\/projects\/\d+/.test(pathname);

  if (inProjectDetail) {
    return <>{children}</>;
  }

  return (
    <div className="min-h-screen">
      <AppHeader />
      {children}
    </div>
  );
}
