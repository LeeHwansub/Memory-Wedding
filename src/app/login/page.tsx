import Link from "next/link";
import { getOAuthLoginUrl } from "@/lib/auth";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-6">
      <div className="w-full max-w-md rounded-2xl border border-accent/20 bg-white/80 p-8 text-center backdrop-blur-sm">
        <p className="mb-2 text-sm tracking-[0.3em] text-muted uppercase">
          Memory Wedding
        </p>
        <h1
          className="mb-3 text-3xl font-light"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          로그인
        </h1>
        <p className="mb-8 text-sm text-muted">
          신랑·신부 계정으로 로그인하세요
        </p>

        <div className="flex flex-col gap-3">
          <a
            href={getOAuthLoginUrl("google")}
            className="rounded-full border border-accent/30 bg-white px-6 py-3 text-sm font-medium transition hover:bg-accent-soft"
          >
            Google로 계속하기
          </a>
          <a
            href={getOAuthLoginUrl("naver")}
            className="rounded-full bg-[#03C75A] px-6 py-3 text-sm font-medium text-white transition hover:opacity-90"
          >
            Naver로 계속하기
          </a>
        </div>

        <Link
          href="/"
          className="mt-6 inline-block text-sm text-muted underline-offset-4 hover:underline"
        >
          ← 홈으로
        </Link>
      </div>
    </main>
  );
}
