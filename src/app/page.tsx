"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=2000&q=80";

const FEATURES = [
  {
    title: "모바일 청첩장",
    desc: "사진·인사말·지도까지 한 장에",
    icon: (
      <path
        d="M8 3.5h8A1.5 1.5 0 0 1 17.5 5v14a1.5 1.5 0 0 1-1.5 1.5H8A1.5 1.5 0 0 1 6.5 19V5A1.5 1.5 0 0 1 8 3.5Zm3.5 13h1"
        stroke="currentColor"
        strokeWidth="1.4"
        fill="none"
        strokeLinecap="round"
      />
    ),
  },
  {
    title: "하객 업로드",
    desc: "QR로 사진·영상을 바로 모아요",
    icon: (
      <path
        d="M4.5 15.5 9 11l3 3 4.5-5.5 2.5 3M7 7.5h.01M5 19.5h14a1.5 1.5 0 0 0 1.5-1.5V6A1.5 1.5 0 0 0 19 4.5H5A1.5 1.5 0 0 0 3.5 6v12A1.5 1.5 0 0 0 5 19.5Z"
        stroke="currentColor"
        strokeWidth="1.4"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    ),
  },
  {
    title: "방명록",
    desc: "따뜻한 축하 메시지를 남기세요",
    icon: (
      <path
        d="M6.5 5.5h11A1.5 1.5 0 0 1 19 7v9.5L15.5 14H6.5A1.5 1.5 0 0 1 5 12.5v-6A1.5 1.5 0 0 1 6.5 5.5Zm2 4h7m-7 3h4"
        stroke="currentColor"
        strokeWidth="1.4"
        fill="none"
        strokeLinecap="round"
      />
    ),
  },
  {
    title: "추억 보관",
    desc: "예식 후에도 갤러리로 다시 보기",
    icon: (
      <path
        d="M6 19.5h12M8 19.5V8.2A1.7 1.7 0 0 1 9.7 6.5h4.6A1.7 1.7 0 0 1 16 8.2v11.3M10 10h4"
        stroke="currentColor"
        strokeWidth="1.4"
        fill="none"
        strokeLinecap="round"
      />
    ),
  },
];

const STEPS = [
  { step: "01", title: "프로젝트 만들기", desc: "예식 정보와 청첩장을 준비합니다." },
  { step: "02", title: "초대 링크 공유", desc: "QR·링크로 하객을 초대합니다." },
  { step: "03", title: "순간을 모으기", desc: "사진·영상·메시지가 한곳에 쌓입니다." },
];

export default function Home() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [joinOpen, setJoinOpen] = useState(false);
  const [slug, setSlug] = useState("");

  useEffect(() => {
    const id = window.requestAnimationFrame(() => setReady(true));
    return () => window.cancelAnimationFrame(id);
  }, []);

  function handleJoin(event: FormEvent) {
    event.preventDefault();
    const cleaned = slug.trim().replace(/^\/?w\//, "");
    if (!cleaned) return;
    router.push(`/w/${cleaned}`);
  }

  return (
    <main className="min-h-screen overflow-x-hidden bg-[#F7F3EE] text-[#2A221C]">
      <header className="absolute inset-x-0 top-0 z-30">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5 sm:px-8">
          <Link
            href="/"
            className="text-[13px] tracking-[0.28em] text-white uppercase"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            Memory Wedding
          </Link>
          <nav className="flex items-center gap-2 sm:gap-3">
            <button
              type="button"
              onClick={() => setJoinOpen(true)}
              className="hidden rounded-full px-3 py-2 text-xs text-white/85 transition hover:text-white sm:inline"
            >
              초대 링크로 참여
            </button>
            <Link
              href="/login"
              className="rounded-full border border-white/35 bg-white/10 px-4 py-2 text-xs text-white backdrop-blur transition hover:bg-white/20"
            >
              시작하기
            </Link>
          </nav>
        </div>
      </header>

      <section className="relative isolate flex min-h-[100svh] items-end overflow-hidden">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={HERO_IMAGE}
          alt=""
          className={`absolute inset-0 h-full w-full object-cover transition-transform duration-[2.4s] ease-out ${
            ready ? "scale-100" : "scale-105"
          }`}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#1A1512]/88 via-[#1A1512]/35 to-[#1A1512]/25" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,transparent_20%,rgba(26,21,18,0.35)_100%)]" />

        <div
          className={`relative z-10 mx-auto w-full max-w-6xl px-5 pb-16 pt-28 sm:px-8 sm:pb-20 transition-all duration-1000 ${
            ready ? "translate-y-0 opacity-100" : "translate-y-6 opacity-0"
          }`}
        >
          <p
            className="mb-5 text-[11px] tracking-[0.42em] text-[#E8D7BE] uppercase"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            Memory Wedding
          </p>
          <h1
            className="max-w-3xl text-[2.6rem] font-light leading-[1.12] tracking-wide text-white sm:text-6xl md:text-7xl"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            우리의 결혼식,
            <br />
            모든 순간을 함께
          </h1>
          <p className="mt-5 max-w-lg text-[15px] leading-relaxed text-white/80 sm:text-base">
            모바일 청첩장과 하객 사진·메시지를 한곳에서 모으고,
            예식이 끝난 뒤에도 오래 간직하세요.
          </p>
          <div className="mt-9 flex flex-col gap-3 sm:flex-row sm:items-center">
            <Link
              href="/login"
              className="inline-flex items-center justify-center rounded-full bg-[#C9A87C] px-8 py-3.5 text-sm text-white transition hover:opacity-90"
            >
              청첩장 만들기
            </Link>
            <button
              type="button"
              onClick={() => setJoinOpen(true)}
              className="inline-flex items-center justify-center rounded-full border border-white/40 px-8 py-3.5 text-sm text-white transition hover:bg-white/10"
            >
              초대 링크로 참여
            </button>
          </div>
        </div>
      </section>

      <section className="border-y border-[#2A221C]/10 bg-[#1A1512] text-white">
        <div className="mx-auto grid max-w-6xl grid-cols-2 gap-6 px-5 py-10 sm:grid-cols-4 sm:gap-4 sm:px-8 sm:py-12">
          {FEATURES.map((item, index) => (
            <div
              key={item.title}
              className={`flex flex-col items-center text-center transition-all duration-700 ${
                ready ? "translate-y-0 opacity-100" : "translate-y-4 opacity-0"
              }`}
              style={{ transitionDelay: `${180 + index * 90}ms` }}
            >
              <span className="mb-3 flex h-14 w-14 items-center justify-center rounded-full border border-white/20 bg-white/5">
                <svg viewBox="0 0 24 24" className="h-6 w-6 text-[#E8D7BE]">
                  {item.icon}
                </svg>
              </span>
              <p className="text-sm font-medium tracking-wide">{item.title}</p>
              <p className="mt-1 max-w-[11rem] text-[11px] leading-relaxed text-white/55">
                {item.desc}
              </p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto grid max-w-6xl items-center gap-12 px-5 py-20 sm:px-8 lg:grid-cols-[1.05fr_0.95fr] lg:gap-16 lg:py-28">
        <div>
          <p
            className="mb-3 text-[11px] tracking-[0.28em] text-[#C9A87C] uppercase"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            Invitation
          </p>
          <h2
            className="text-3xl font-light leading-snug sm:text-4xl"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            하객이 보는 청첩장부터
            <br />
            사진 갤러리까지
          </h2>
          <p className="mt-5 max-w-md text-sm leading-relaxed text-[#7A7068]">
            메인 사진과 웨딩 갤러리를 직접 꾸레이션하고, 예식 정보·축의금·방명록을
            모바일 청첩장에 담습니다. 하객은 링크 하나로 참여합니다.
          </p>
          <ul className="mt-8 space-y-3 text-sm text-[#2A221C]/85">
            <li className="flex gap-3">
              <span className="mt-2 h-1 w-1 rounded-full bg-[#C9A87C]" />
              메인 히어로 · 갤러리 레이아웃 선택
            </li>
            <li className="flex gap-3">
              <span className="mt-2 h-1 w-1 rounded-full bg-[#C9A87C]" />
              QR·링크로 하객 업로드
            </li>
            <li className="flex gap-3">
              <span className="mt-2 h-1 w-1 rounded-full bg-[#C9A87C]" />
              방명록과 축하 메시지
            </li>
          </ul>
        </div>

        <div className="relative mx-auto w-full max-w-[320px]">
          <div className="absolute -inset-6 rounded-[2.5rem] bg-[#E8D7BE]/35 blur-2xl" aria-hidden />
          <div className="landing-phone relative overflow-hidden rounded-[2rem] border border-[#2A221C]/15 bg-[#FAF8F5] shadow-[0_30px_80px_rgba(42,34,28,0.18)]">
            <div className="flex items-center justify-center gap-1 border-b border-black/5 bg-[#1c1c1e] px-4 py-2">
              <span className="h-1.5 w-1.5 rounded-full bg-white/30" />
              <span className="h-1 w-12 rounded-full bg-white/20" />
            </div>
            <div className="relative h-[420px] overflow-hidden">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src="https://images.unsplash.com/photo-1465495976277-4387d4b0b4c6?auto=format&fit=crop&w=900&q=80"
                alt=""
                className="h-[58%] w-full object-cover"
              />
              <div className="absolute inset-x-0 top-[42%] bg-gradient-to-t from-[#FAF8F5] via-[#FAF8F5] to-transparent px-5 pb-6 pt-16 text-center">
                <p className="text-[10px] tracking-[0.28em] text-[#8A7F78] uppercase">
                  Wedding Invitation
                </p>
                <p
                  className="mt-2 text-2xl font-light"
                  style={{ fontFamily: "var(--font-playfair), serif" }}
                >
                  Minho <span className="text-[#C9A87C]">&</span> Soyeon
                </p>
                <p className="mt-3 text-xs text-[#8A7F78]">2026. 10. 17 · Saturday</p>
                <div className="mt-5 grid grid-cols-2 gap-1">
                  <div className="aspect-square overflow-hidden bg-[#EDE6DC]" />
                  <div className="aspect-square overflow-hidden bg-[#E4D8C8]" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="border-t border-[#2A221C]/8 bg-[#EFE8DF]">
        <div className="mx-auto max-w-6xl px-5 py-20 sm:px-8 sm:py-24">
          <p
            className="mb-3 text-center text-[11px] tracking-[0.28em] text-[#C9A87C] uppercase"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            How it works
          </p>
          <h2
            className="mb-12 text-center text-3xl font-light sm:text-4xl"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            세 단계로 시작해요
          </h2>
          <div className="grid gap-10 sm:grid-cols-3 sm:gap-8">
            {STEPS.map((item) => (
              <div key={item.step} className="text-center sm:text-left">
                <p
                  className="text-sm tracking-[0.2em] text-[#C9A87C]"
                  style={{ fontFamily: "var(--font-playfair), serif" }}
                >
                  {item.step}
                </p>
                <h3 className="mt-3 text-lg font-medium">{item.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-[#7A7068]">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="relative overflow-hidden bg-[#1A1512] px-5 py-20 text-center text-white sm:px-8 sm:py-24">
        <div
          className="pointer-events-none absolute inset-0 opacity-40"
          style={{
            background:
              "radial-gradient(circle at 30% 20%, rgba(201,168,124,0.28), transparent 45%), radial-gradient(circle at 80% 80%, rgba(232,215,190,0.12), transparent 40%)",
          }}
        />
        <div className="relative mx-auto max-w-2xl">
          <h2
            className="text-3xl font-light sm:text-4xl"
            style={{ fontFamily: "var(--font-playfair), serif" }}
          >
            지금, 우리의 순간을 모아보세요
          </h2>
          <p className="mt-4 text-sm leading-relaxed text-white/65">
            Memory Wedding에서 청첩장을 만들고 하객과 함께 추억을 쌓아보세요.
          </p>
          <Link
            href="/login"
            className="mt-8 inline-flex rounded-full bg-[#C9A87C] px-8 py-3.5 text-sm text-white transition hover:opacity-90"
          >
            시작하기
          </Link>
        </div>
      </section>

      <footer className="border-t border-[#2A221C]/10 bg-[#F7F3EE] px-5 py-8 text-center sm:px-8">
        <p
          className="text-[11px] tracking-[0.28em] text-[#8A7F78] uppercase"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          Memory Wedding
        </p>
        <p className="mt-2 text-xs text-[#8A7F78]">
          모바일 청첩장 · 하객 참여형 웨딩 아카이브
        </p>
      </footer>

      {joinOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="join-title"
          onClick={() => setJoinOpen(false)}
        >
          <form
            onSubmit={handleJoin}
            className="w-full max-w-md rounded-2xl bg-[#F7F3EE] p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2
              id="join-title"
              className="text-xl font-light"
              style={{ fontFamily: "var(--font-playfair), serif" }}
            >
              초대 링크로 참여
            </h2>
            <p className="mt-2 text-sm text-[#7A7068]">
              청첩장 주소의 slug를 입력하세요. 예:{" "}
              <span className="text-[#2A221C]">/w/minho-soyeon</span>
            </p>
            <input
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              placeholder="minho-soyeon"
              className="mt-5 w-full rounded-xl border border-[#C9A87C]/35 bg-white px-4 py-3 text-sm"
              autoFocus
            />
            <div className="mt-5 flex gap-2">
              <button
                type="button"
                onClick={() => setJoinOpen(false)}
                className="flex-1 rounded-full border border-[#C9A87C]/40 px-4 py-3 text-sm"
              >
                취소
              </button>
              <button
                type="submit"
                className="flex-1 rounded-full bg-[#C9A87C] px-4 py-3 text-sm text-white"
              >
                이동
              </button>
            </div>
          </form>
        </div>
      )}

    </main>
  );
}
