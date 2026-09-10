"use client";

import Link from "next/link";
import { useParams } from "next/navigation";

export default function GuestGuestbookPlaceholderPage() {
  const params = useParams<{ slug: string }>();

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#FAF8F5] px-6">
      <div className="max-w-md text-center">
        <h1
          className="mb-3 text-2xl font-light"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          방명록
        </h1>
        <p className="mb-6 text-sm text-muted">다음 단계에서 구현 예정입니다.</p>
        <Link href={`/w/${params.slug}`} className="text-sm underline">
          청첩장으로 돌아가기
        </Link>
      </div>
    </main>
  );
}
