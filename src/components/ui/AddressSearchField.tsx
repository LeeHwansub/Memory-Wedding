"use client";

import { useState } from "react";
import { formatDaumAddress, openDaumPostcode } from "@/lib/daum-postcode";

type AddressSearchFieldProps = {
  label?: string;
  value: string;
  onChange: (address: string) => void;
  onBuildingName?: (buildingName: string) => void;
};

export function AddressSearchField({
  label = "예식장 주소",
  value,
  onChange,
  onBuildingName,
}: AddressSearchFieldProps) {
  const [error, setError] = useState<string | null>(null);
  const [opening, setOpening] = useState(false);

  async function handleSearch() {
    setError(null);
    setOpening(true);

    try {
      await openDaumPostcode((data) => {
        onChange(formatDaumAddress(data));
        if (onBuildingName && data.buildingName) {
          onBuildingName(data.buildingName);
        }
      });
    } catch {
      setError("주소 검색을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setOpening(false);
    }
  }

  return (
    <div className="block text-sm">
      <span className="mb-1 block text-muted">{label}</span>
      <div className="flex gap-2">
        <input
          readOnly
          value={value}
          placeholder="주소 검색 버튼으로 입력"
          className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
        />
        <button
          type="button"
          onClick={handleSearch}
          disabled={opening}
          className="rounded-xl border border-accent/40 px-4 text-sm whitespace-nowrap transition hover:bg-accent-soft disabled:opacity-60"
        >
          {opening ? "여는 중..." : "주소 검색"}
        </button>
      </div>
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}
