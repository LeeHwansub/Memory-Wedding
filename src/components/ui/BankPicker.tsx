"use client";

import { useEffect } from "react";
import { BANKS, type BankOption } from "@/lib/banks";

type BankPickerProps = {
  open: boolean;
  selectedName?: string;
  onSelect: (bank: BankOption) => void;
  onClose: () => void;
};

export function BankPicker({ open, selectedName, onSelect, onClose }: BankPickerProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
      <button
        type="button"
        aria-label="닫기"
        className="absolute inset-0 bg-black/40"
        onClick={onClose}
      />
      <div className="relative z-10 max-h-[80vh] w-full max-w-md overflow-hidden rounded-t-3xl bg-white sm:rounded-3xl">
        <div className="flex items-center justify-between border-b border-accent/10 px-5 py-4">
          <h2 className="text-base font-medium">은행 선택</h2>
          <button type="button" onClick={onClose} className="text-sm text-muted">
            닫기
          </button>
        </div>
        <div className="grid max-h-[65vh] grid-cols-3 gap-3 overflow-y-auto p-4 sm:grid-cols-4">
          {BANKS.map((bank) => {
            const selected = selectedName === bank.name;
            const textDark = bank.code === "kakao";
            return (
              <button
                key={bank.code}
                type="button"
                onClick={() => {
                  onSelect(bank);
                  onClose();
                }}
                className={`flex flex-col items-center gap-2 rounded-2xl p-3 text-center transition ${
                  selected ? "bg-accent-soft ring-1 ring-accent/40" : "hover:bg-[#FAF8F5]"
                }`}
              >
                <span
                  className="flex h-12 w-12 items-center justify-center rounded-2xl text-xs font-semibold"
                  style={{
                    backgroundColor: bank.color,
                    color: textDark ? "#191919" : "#FFFFFF",
                  }}
                >
                  {bank.short}
                </span>
                <span className="text-[11px] leading-tight text-[#2C2420]">{bank.name}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
