export default function Home() {
  return (
    <main className="min-h-screen">
      <section className="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center px-6 py-20 text-center">
        <p className="mb-4 text-sm tracking-[0.3em] text-muted uppercase">
          Memory Wedding
        </p>
        <h1
          className="mb-6 max-w-3xl text-5xl leading-tight font-light md:text-7xl"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          우리의 결혼식,
          <br />
          <span className="text-accent">모든 순간을 함께</span>
        </h1>
        <p className="mb-12 max-w-xl text-lg leading-relaxed text-muted">
          하객들이 찍은 사진과 메시지를 한곳에 모아
          <br className="hidden sm:block" />
          평생 간직할 수 있는 디지털 웨딩 앨범입니다.
        </p>

        <div className="flex flex-col gap-4 sm:flex-row">
          <button
            type="button"
            className="rounded-full bg-accent px-8 py-3 text-sm font-medium text-white transition hover:opacity-90"
          >
            앨범 만들기
          </button>
          <button
            type="button"
            className="rounded-full border border-accent/40 bg-accent-soft px-8 py-3 text-sm font-medium text-foreground transition hover:bg-accent/10"
          >
            초대 링크로 참여
          </button>
        </div>

        <div className="mt-24 grid w-full max-w-3xl gap-6 sm:grid-cols-3">
          {[
            { title: "사진 업로드", desc: "QR 코드로 누구나 쉽게 사진을 공유" },
            { title: "축하 메시지", desc: "따뜻한 마음을 글로 남기세요" },
            { title: "추억 보관", desc: "결혼식 후에도 언제든 다시 볼 수 있어요" },
          ].map((item) => (
            <article
              key={item.title}
              className="rounded-2xl border border-accent/20 bg-white/60 p-6 text-left backdrop-blur-sm"
            >
              <h2 className="mb-2 text-lg font-medium">{item.title}</h2>
              <p className="text-sm leading-relaxed text-muted">{item.desc}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
