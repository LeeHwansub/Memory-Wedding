const STORAGE_KEY = "memory_wedding_guestbook_client_key";

let memoryKey: string | null = null;

function createKey(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `guest-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function readStoredKey(): string | null {
  try {
    const fromLocal = window.localStorage.getItem(STORAGE_KEY);
    if (fromLocal && fromLocal.trim() && fromLocal.length <= 64) {
      return fromLocal.trim();
    }
  } catch {
    // localStorage blocked
  }

  try {
    const fromSession = window.sessionStorage.getItem(STORAGE_KEY);
    if (fromSession && fromSession.trim() && fromSession.length <= 64) {
      return fromSession.trim();
    }
  } catch {
    // sessionStorage blocked
  }

  return memoryKey;
}

function writeStoredKey(key: string): void {
  memoryKey = key;
  try {
    window.localStorage.setItem(STORAGE_KEY, key);
  } catch {
    // ignore
  }
  try {
    window.sessionStorage.setItem(STORAGE_KEY, key);
  } catch {
    // ignore
  }
}

/**
 * Browser-local guest identity for guestbook write/edit/like.
 * Not related to OAuth login. Persists in localStorage when available.
 */
export function getGuestbookClientKey(): string {
  if (typeof window === "undefined") {
    throw new Error("방명록 식별자는 브라우저에서만 생성할 수 있습니다. 페이지를 새로고침해 주세요.");
  }

  const existing = readStoredKey();
  if (existing) {
    memoryKey = existing;
    return existing;
  }

  const key = createKey();
  writeStoredKey(key);
  return key;
}
