import { clearToken, getToken } from "./auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export async function apiFetch<T>(
  path: string,
  options?: RequestInit,
): Promise<ApiResponse<T>> {
  const token = getToken();

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  });

  if (response.status === 401) {
    clearToken();
    throw new Error("Unauthorized");
  }

  if (!response.ok) {
    let message = `API error: ${response.status}`;
    try {
      const body = (await response.json()) as ApiResponse<unknown>;
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }

  return response.json();
}

export async function apiPublicFetch<T>(
  path: string,
  options?: RequestInit,
): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });

  if (!response.ok) {
    let message = `API error: ${response.status}`;
    try {
      const body = (await response.json()) as ApiResponse<unknown>;
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }

  return response.json();
}

export async function apiPublicUpload<T>(
  path: string,
  formData: FormData,
): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    let message = `API error: ${response.status}`;
    try {
      const body = (await response.json()) as ApiResponse<unknown>;
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }

  return response.json();
}

export async function apiUpload<T>(
  path: string,
  formData: FormData,
): Promise<ApiResponse<T>> {
  const token = getToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  });

  if (response.status === 401) {
    clearToken();
    throw new Error("Unauthorized");
  }

  if (!response.ok) {
    let message = `API error: ${response.status}`;
    try {
      const body = (await response.json()) as ApiResponse<unknown>;
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }

  return response.json();
}

export function apiAssetUrl(path: string): string {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  return `${API_BASE_URL}${path}`;
}

export async function apiFetchBlob(path: string): Promise<Blob> {
  const token = getToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (response.status === 401) {
    clearToken();
    throw new Error("Unauthorized");
  }
  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }
  return response.blob();
}
