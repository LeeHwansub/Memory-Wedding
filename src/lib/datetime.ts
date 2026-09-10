/**
 * Backend stores weddingAt as LocalDateTime (wall clock, no timezone).
 * Never use Date#toISOString() — it shifts to UTC and breaks the displayed time.
 */

/** datetime-local input value: YYYY-MM-DDTHH:mm */
export function toDatetimeLocalValue(apiValue: string): string {
  const normalized = apiValue.trim().replace(" ", "T");

  if (/[zZ]$/.test(normalized) || /[+-]\d{2}:\d{2}$/.test(normalized)) {
    const date = new Date(normalized);
    if (Number.isNaN(date.getTime())) {
      return "";
    }
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  const match = normalized.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/);
  if (!match) {
    return "";
  }
  return `${match[1]}T${match[2]}`;
}

/** API LocalDateTime string: YYYY-MM-DDTHH:mm:ss (no Z) */
export function toApiLocalDateTime(datetimeLocal: string): string {
  const value = datetimeLocal.trim();
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    return `${value}:00`;
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(value)) {
    return value.slice(0, 19);
  }
  throw new Error("예식 일시 형식이 올바르지 않습니다.");
}

/** e.g. 2026년 9월 13일 (토) 오전 1:00 */
export function formatWeddingDateTime(apiValue: string): string {
  const local = toDatetimeLocalValue(apiValue);
  if (!local) {
    return apiValue;
  }

  const [datePart, timePart] = local.split("T");
  const [year, month, day] = datePart.split("-").map(Number);
  const [hour, minute] = timePart.split(":").map(Number);
  const date = new Date(year, month - 1, day, hour, minute);

  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
}
