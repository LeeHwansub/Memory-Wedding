export type DaumPostcodeData = {
  zonecode: string;
  address: string;
  roadAddress: string;
  jibunAddress: string;
  buildingName: string;
  apartment: string;
  addressType: "R" | "J";
  userSelectedType: "R" | "J";
};

type DaumPostcodeOptions = {
  oncomplete: (data: DaumPostcodeData) => void;
  onclose?: (state: "FORCE_CLOSE" | "COMPLETE_CLOSE") => void;
  width?: string | number;
  height?: string | number;
};

type DaumPostcodeConstructor = new (options: DaumPostcodeOptions) => {
  open: () => void;
};

declare global {
  interface Window {
    daum?: {
      Postcode: DaumPostcodeConstructor;
    };
  }
}

const DAUM_POSTCODE_SCRIPT =
  "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";

let loadPromise: Promise<void> | null = null;

export function loadDaumPostcode(): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Window is not available"));
  }

  if (window.daum?.Postcode) {
    return Promise.resolve();
  }

  if (loadPromise) {
    return loadPromise;
  }

  loadPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(
      `script[src="${DAUM_POSTCODE_SCRIPT}"]`,
    );

    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () =>
        reject(new Error("Failed to load Daum Postcode script")),
      );
      return;
    }

    const script = document.createElement("script");
    script.src = DAUM_POSTCODE_SCRIPT;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Failed to load Daum Postcode script"));
    document.head.appendChild(script);
  });

  return loadPromise;
}

export function formatDaumAddress(data: DaumPostcodeData): string {
  const base =
    data.userSelectedType === "R"
      ? data.roadAddress || data.address
      : data.jibunAddress || data.address;

  if (data.buildingName) {
    return `${base} (${data.buildingName})`;
  }

  return base;
}

export async function openDaumPostcode(
  onComplete: (data: DaumPostcodeData) => void,
): Promise<void> {
  await loadDaumPostcode();

  if (!window.daum?.Postcode) {
    throw new Error("Daum Postcode is not available");
  }

  new window.daum.Postcode({
    oncomplete: onComplete,
    width: "100%",
    height: "100%",
  }).open();
}
