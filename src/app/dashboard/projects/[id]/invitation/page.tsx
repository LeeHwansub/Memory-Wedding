"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { BankPicker } from "@/components/ui/BankPicker";
import { useProjectPreview } from "@/components/shell/ProjectShell";
import { apiFetch } from "@/lib/api";
import { findBankByName } from "@/lib/banks";
import { getToken } from "@/lib/auth";
import type { AccountEntry, Invitation } from "@/types/invitation";

const emptyAccount = (): AccountEntry => ({
  relation: "",
  bankName: "",
  accountNumber: "",
});

export default function InvitationEditPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { openPreview } = useProjectPreview();
  const [invitation, setInvitation] = useState<Invitation | null>(null);
  const [title, setTitle] = useState("");
  const [greetingMessage, setGreetingMessage] = useState("");
  const [accounts, setAccounts] = useState<AccountEntry[]>([emptyAccount()]);
  const [bankPickerIndex, setBankPickerIndex] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    apiFetch<Invitation>(`/api/projects/${params.id}/invitation`)
      .then((res) => {
        const data = res.data;
        if (!data) return;
        setInvitation(data);
        setTitle(data.title ?? "");
        setGreetingMessage(data.greetingMessage ?? "");
        setAccounts(data.accounts?.length ? data.accounts : [emptyAccount()]);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [params.id, router]);

  function updateAccount(index: number, key: keyof AccountEntry, value: string) {
    setAccounts((prev) =>
      prev.map((item, i) => (i === index ? { ...item, [key]: value } : item)),
    );
  }

  function addAccount() {
    setAccounts((prev) => [...prev, emptyAccount()]);
  }

  function removeAccount(index: number) {
    setAccounts((prev) => (prev.length === 1 ? prev : prev.filter((_, i) => i !== index)));
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);

    const cleaned = accounts
      .map((item) => ({
        relation: item.relation.trim(),
        bankName: item.bankName.trim(),
        accountNumber: item.accountNumber.trim(),
      }))
      .filter((item) => item.relation || item.bankName || item.accountNumber);

    for (const item of cleaned) {
      if (!item.relation || !item.bankName || !item.accountNumber) {
        setError("축의금 항목은 관계, 은행, 계좌번호를 모두 입력해 주세요.");
        setSaving(false);
        return;
      }
    }

    try {
      const res = await apiFetch<Invitation>(`/api/projects/${params.id}/invitation`, {
        method: "PUT",
        body: JSON.stringify({
          title: title || undefined,
          greetingMessage: greetingMessage || undefined,
          accounts: cleaned,
        }),
      });
      setInvitation(res.data);
      setAccounts(res.data?.accounts?.length ? res.data.accounts : [emptyAccount()]);
      setMessage("저장되었습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "저장 실패");
    } finally {
      setSaving(false);
    }
  }

  async function togglePublish(published: boolean) {
    setError(null);
    setMessage(null);
    try {
      const res = await apiFetch<Invitation>(
        `/api/projects/${params.id}/invitation/publish`,
        {
          method: "PATCH",
          body: JSON.stringify({ published }),
        },
      );
      setInvitation(res.data);
      setMessage(published ? "청첩장이 공개되었습니다." : "청첩장이 비공개로 전환되었습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "공개 설정 실패");
    }
  }

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  if (!invitation) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16">
        <p className="text-red-600">{error ?? "청첩장을 찾을 수 없습니다."}</p>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen max-w-xl px-6 py-10 sm:py-14">
      <h1
        className="mb-2 text-3xl font-light"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        청첩장 편집
      </h1>
      <p className="mb-8 text-sm text-muted">
        상태: {invitation.published ? "공개" : "비공개"} ·{" "}
        <button type="button" onClick={openPreview} className="underline">
          미리보기
        </button>
      </p>

      <Link
        href={`/dashboard/projects/${params.id}/invitation/design`}
        className="mb-6 flex items-center justify-between rounded-2xl border border-accent/25 bg-white/70 px-4 py-4 transition hover:border-accent/45 hover:bg-white"
      >
        <div>
          <p className="text-sm font-medium">사진 · 디자인</p>
          <p className="mt-0.5 text-xs text-muted">
            메인 사진, 웨딩 갤러리, 레이아웃은 디자인 페이지에서 편집합니다.
          </p>
        </div>
        <span className="text-sm text-accent">열기 →</span>
      </Link>

      <div className="mb-6 rounded-2xl border border-accent/20 bg-white/60 p-4 text-sm">
        <p className="mb-1 text-muted">오시는 길 (Project 등록 주소)</p>
        <p className="font-medium">{invitation.venueName || "-"}</p>
        <p className="text-muted">{invitation.venueAddress || "주소 미등록"}</p>
        {invitation.mapUrl && (
          <a
            href={invitation.mapUrl}
            target="_blank"
            rel="noreferrer"
            className="mt-2 inline-block text-sm underline"
          >
            카카오맵에서 보기
          </a>
        )}
      </div>

      <form onSubmit={handleSave} className="space-y-4">
        <label className="block text-sm">
          <span className="mb-1 block text-muted">제목</span>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-muted">인사말</span>
          <textarea
            value={greetingMessage}
            onChange={(e) => setGreetingMessage(e.target.value)}
            rows={5}
            className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
          />
        </label>

        <section className="space-y-4">
          <p className="text-sm font-medium">축의금 계좌</p>

          {accounts.map((account, index) => {
            const bank = findBankByName(account.bankName);
            return (
              <div
                key={index}
                className="rounded-2xl border border-accent/20 bg-white/80 p-4"
              >
                <div className="mb-3 flex items-center justify-between">
                  <p className="text-xs tracking-wide text-muted">계좌 {index + 1}</p>
                  <button
                    type="button"
                    onClick={() => removeAccount(index)}
                    disabled={accounts.length === 1}
                    className="text-xs text-red-600 disabled:opacity-40"
                  >
                    삭제
                  </button>
                </div>

                <div className="space-y-3">
                  <label className="block text-sm">
                    <span className="mb-1 block text-muted">관계</span>
                    <input
                      value={account.relation}
                      onChange={(e) => updateAccount(index, "relation", e.target.value)}
                      placeholder="예: 신랑, 신부 아버지"
                      className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
                    />
                  </label>

                  <div className="block text-sm">
                    <span className="mb-1 block text-muted">은행</span>
                    <button
                      type="button"
                      onClick={() => setBankPickerIndex(index)}
                      className="flex w-full items-center gap-3 rounded-xl border border-accent/30 bg-white px-4 py-3 text-left transition hover:bg-[#FAF8F5]"
                    >
                      {bank ? (
                        <>
                          <span
                            className="flex h-9 w-9 items-center justify-center rounded-xl text-[10px] font-semibold text-white"
                            style={{
                              backgroundColor: bank.color,
                              color: bank.code === "kakao" ? "#191919" : "#FFFFFF",
                            }}
                          >
                            {bank.short}
                          </span>
                          <span className="text-sm">{bank.name}</span>
                        </>
                      ) : (
                        <span className="text-sm text-muted">은행을 선택하세요</span>
                      )}
                    </button>
                  </div>

                  <label className="block text-sm">
                    <span className="mb-1 block text-muted">계좌번호</span>
                    <input
                      value={account.accountNumber}
                      onChange={(e) => updateAccount(index, "accountNumber", e.target.value)}
                      placeholder="숫자와 - 만 입력"
                      inputMode="numeric"
                      className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
                    />
                  </label>
                </div>
              </div>
            );
          })}

          <button
            type="button"
            onClick={addAccount}
            className="w-full rounded-2xl border border-dashed border-accent/40 px-4 py-3 text-sm text-muted transition hover:bg-accent-soft"
          >
            + 계좌 추가
          </button>
        </section>

        {error && <p className="text-sm text-red-600">{error}</p>}
        {message && <p className="text-sm text-green-700">{message}</p>}

        <button
          type="submit"
          disabled={saving}
          className="w-full rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
        >
          {saving ? "저장 중..." : "내용 저장"}
        </button>
      </form>

      <div className="mt-6 flex gap-3">
        <button
          type="button"
          onClick={() => togglePublish(true)}
          disabled={invitation.published}
          className="rounded-full bg-accent px-5 py-2 text-sm text-white disabled:opacity-50"
        >
          공개하기
        </button>
        <button
          type="button"
          onClick={() => togglePublish(false)}
          disabled={!invitation.published}
          className="rounded-full border border-accent/40 px-5 py-2 text-sm disabled:opacity-50"
        >
          비공개
        </button>
      </div>

      <BankPicker
        open={bankPickerIndex !== null}
        selectedName={
          bankPickerIndex !== null ? accounts[bankPickerIndex]?.bankName : undefined
        }
        onClose={() => setBankPickerIndex(null)}
        onSelect={(bank) => {
          if (bankPickerIndex === null) return;
          updateAccount(bankPickerIndex, "bankName", bank.name);
        }}
      />
    </main>
  );
}
