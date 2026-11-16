const TOKEN_KEY = "memory_wedding_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function getOAuthLoginUrl(provider: "google" | "naver" | "kakao"): string {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
  return `${apiUrl}/oauth2/authorization/${provider}`;
}
