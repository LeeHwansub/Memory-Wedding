type KakaoLatLng = {
  getLat: () => number;
  getLng: () => number;
};

type KakaoMapInstance = {
  setCenter: (latlng: KakaoLatLng) => void;
  relayout: () => void;
};

type KakaoMaps = {
  load: (callback: () => void) => void;
  LatLng: new (lat: number, lng: number) => KakaoLatLng;
  Map: new (
    container: HTMLElement,
    options: { center: KakaoLatLng; level: number },
  ) => KakaoMapInstance;
  Marker: new (options: { map: KakaoMapInstance; position: KakaoLatLng }) => unknown;
  services: {
    Status: { OK: string };
    Geocoder: new () => {
      addressSearch: (
        address: string,
        callback: (
          result: Array<{ x: string; y: string; address_name: string }>,
          status: string,
        ) => void,
      ) => void;
    };
  };
};

declare global {
  interface Window {
    kakao?: {
      maps: KakaoMaps;
    };
  }
}

const loadPromises = new Map<string, Promise<void>>();

export function getKakaoMapAppKey(): string | null {
  const key = process.env.NEXT_PUBLIC_KAKAO_MAP_APP_KEY?.trim();
  return key || null;
}

export function loadKakaoMapSdk(appKey: string): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Window is not available"));
  }

  if (window.kakao?.maps) {
    return Promise.resolve();
  }

  const cached = loadPromises.get(appKey);
  if (cached) return cached;

  const src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false&libraries=services`;

  const promise = new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${src}"]`);

    const onReady = () => {
      if (!window.kakao?.maps) {
        reject(new Error("Kakao Maps SDK failed to initialize"));
        return;
      }
      window.kakao.maps.load(() => resolve());
    };

    if (existing) {
      if (window.kakao?.maps) {
        onReady();
        return;
      }
      existing.addEventListener("load", onReady);
      existing.addEventListener("error", () =>
        reject(new Error("Failed to load Kakao Maps SDK")),
      );
      return;
    }

    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.onload = onReady;
    script.onerror = () => reject(new Error("Failed to load Kakao Maps SDK"));
    document.head.appendChild(script);
  });

  loadPromises.set(appKey, promise);
  return promise;
}

export function normalizeAddressForMap(address: string): string {
  return address
    .replace(/\s*\([^)]*\)\s*$/u, "")
    .replace(/\s+/g, " ")
    .trim();
}

export async function renderKakaoMapByAddress(
  container: HTMLElement,
  address: string,
): Promise<() => void> {
  const appKey = getKakaoMapAppKey();
  if (!appKey) {
    throw new Error("NEXT_PUBLIC_KAKAO_MAP_APP_KEY is not set");
  }

  await loadKakaoMapSdk(appKey);

  const kakao = window.kakao;
  if (!kakao?.maps) {
    throw new Error("Kakao Maps is not available");
  }

  const query = normalizeAddressForMap(address);

  return new Promise((resolve, reject) => {
    const geocoder = new kakao.maps.services.Geocoder();
    geocoder.addressSearch(query, (result, status) => {
      if (status !== kakao.maps.services.Status.OK || !result[0]) {
        reject(new Error("주소를 지도에서 찾을 수 없습니다."));
        return;
      }

      const coords = new kakao.maps.LatLng(
        Number(result[0].y),
        Number(result[0].x),
      );
      const map = new kakao.maps.Map(container, {
        center: coords,
        level: 3,
      });
      new kakao.maps.Marker({ map, position: coords });

      // 레이아웃이 늦게 잡히는 경우 대비
      requestAnimationFrame(() => map.relayout());

      resolve(() => {
        container.replaceChildren();
      });
    });
  });
}
