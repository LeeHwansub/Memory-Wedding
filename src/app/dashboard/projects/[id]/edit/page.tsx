"use client";

import { useParams, useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { AddressSearchField } from "@/components/ui/AddressSearchField";
import { StatusModal } from "@/components/ui/StatusModal";
import { apiFetch } from "@/lib/api";
import { getToken } from "@/lib/auth";
import { toApiLocalDateTime, toDatetimeLocalValue } from "@/lib/datetime";
import type { ProjectStatus, WeddingProject } from "@/types";

type ProjectDriveStatus = {
  driveConnected: boolean;
  googleAccountEmail: string | null;
  pendingSyncCount: number;
  syncedCount: number;
};

type DriveModal =
  | null
  | { kind: "progress"; step: number; title: string; description?: string }
  | { kind: "done"; title: string; description: string };

const CONNECT_STEPS = [
  "연결 요청 준비",
  "Google 인증으로 이동",
  "권한 확인 및 저장",
];

export default function EditProjectPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [groomName, setGroomName] = useState("");
  const [brideName, setBrideName] = useState("");
  const [weddingAt, setWeddingAt] = useState("");
  const [venueName, setVenueName] = useState("");
  const [venueAddress, setVenueAddress] = useState("");
  const [status, setStatus] = useState<ProjectStatus>("DRAFT");
  const [drive, setDrive] = useState<ProjectDriveStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [driveBusy, setDriveBusy] = useState(false);
  const [modal, setModal] = useState<DriveModal>(null);

  const loadDrive = useCallback(async () => {
    const res = await apiFetch<ProjectDriveStatus>(`/api/projects/${params.id}/drive`);
    setDrive(res.data);
  }, [params.id]);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }

    const driveFlag = searchParams.get("drive");
    if (driveFlag === "connected") {
      setModal({
        kind: "done",
        title: "Drive 연동 완료",
        description: "Google Drive 연결이 완료되었습니다. 업로드 갤러리에서 파일을 동기화할 수 있습니다.",
      });
    } else if (driveFlag === "missing_refresh_token") {
      setError("Drive 권한 토큰을 받지 못했습니다. 다시 연결해 주세요.");
    } else if (driveFlag === "invalid_state") {
      setError("인증 상태가 만료되었습니다. 다시 연결해 주세요.");
    }

    Promise.all([
      apiFetch<WeddingProject>(`/api/projects/${params.id}`),
      apiFetch<ProjectDriveStatus>(`/api/projects/${params.id}/drive`),
    ])
      .then(([projectRes, driveRes]) => {
        const project = projectRes.data;
        if (!project) return;
        setGroomName(project.groomName);
        setBrideName(project.brideName);
        setWeddingAt(toDatetimeLocalValue(project.weddingAt));
        setVenueName(project.venueName ?? "");
        setVenueAddress(project.venueAddress ?? "");
        setStatus(project.status);
        setDrive(driveRes.data);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "조회 실패"))
      .finally(() => setLoading(false));
  }, [params.id, router, searchParams]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await apiFetch(`/api/projects/${params.id}`, {
        method: "PUT",
        body: JSON.stringify({
          groomName,
          brideName,
          weddingAt: toApiLocalDateTime(weddingAt),
          venueName: venueName || undefined,
          venueAddress: venueAddress || undefined,
          status,
        }),
      });
      router.replace(`/dashboard/projects/${params.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "수정 실패");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDriveConnect() {
    setDriveBusy(true);
    setError(null);
    setModal({
      kind: "progress",
      step: 0,
      title: "Google Drive 연동",
      description: "연결을 준비하고 있습니다.",
    });
    try {
      const res = await apiFetch<{ authorizationUrl: string }>(
        `/api/drive/connect-url?projectId=${params.id}`,
      );
      const url = res.data?.authorizationUrl;
      if (!url) throw new Error("연결 URL이 비어 있습니다.");
      setModal({
        kind: "progress",
        step: 1,
        title: "Google Drive 연동",
        description: "Google 인증 화면으로 이동합니다. 잠시만 기다려 주세요.",
      });
      window.setTimeout(() => {
        window.location.href = url;
      }, 450);
    } catch (err) {
      setModal(null);
      setError(err instanceof Error ? err.message : "Drive 연결 실패");
      setDriveBusy(false);
    }
  }

  async function handleDriveDisconnect() {
    if (!confirm("Google Drive 연결을 해제할까요? (Drive에 올린 파일은 그대로 둡니다)")) {
      return;
    }
    setDriveBusy(true);
    setError(null);
    try {
      await apiFetch("/api/drive/disconnect", { method: "DELETE" });
      setModal({
        kind: "done",
        title: "연결 해제 완료",
        description: "Google Drive 연결이 해제되었습니다.",
      });
      await loadDrive();
    } catch (err) {
      setError(err instanceof Error ? err.message : "연결 해제 실패");
    } finally {
      setDriveBusy(false);
    }
  }

  function closeDoneModal() {
    setModal(null);
    if (searchParams.get("drive")) {
      router.replace(`/dashboard/projects/${params.id}/edit`);
    }
  }

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  const driveLabel = !drive?.driveConnected
    ? "미연결"
    : drive.googleAccountEmail
      ? `연결됨 · ${drive.googleAccountEmail}`
      : "연결됨";

  return (
    <main className="mx-auto min-h-screen max-w-xl px-6 py-10 sm:py-14">
      <h1
        className="mb-8 text-3xl font-light"
        style={{ fontFamily: "var(--font-playfair), serif" }}
      >
        기본 정보 수정
      </h1>

      <form onSubmit={handleSubmit} className="space-y-4">
        <Field label="신랑 이름" value={groomName} onChange={setGroomName} required />
        <Field label="신부 이름" value={brideName} onChange={setBrideName} required />
        <label className="block text-sm">
          <span className="mb-1 block text-muted">예식 일시</span>
          <input
            type="datetime-local"
            required
            value={weddingAt}
            onChange={(e) => setWeddingAt(e.target.value)}
            className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
          />
        </label>
        <Field label="예식장명" value={venueName} onChange={setVenueName} />
        <AddressSearchField
          value={venueAddress}
          onChange={setVenueAddress}
          onBuildingName={(buildingName) => {
            if (!venueName.trim()) {
              setVenueName(buildingName);
            }
          }}
        />
        <label className="block text-sm">
          <span className="mb-1 block text-muted">상태</span>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as ProjectStatus)}
            className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
          >
            <option value="DRAFT">DRAFT</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="ARCHIVED">ARCHIVED</option>
          </select>
        </label>

        <section className="rounded-2xl border border-accent/20 bg-white/70 p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium">Google Drive</p>
              <p className="mt-1 text-xs text-muted">{driveLabel}</p>
              <p className="mt-1 text-xs text-muted">
                원본은 우리 저장소에 두고, Drive에는 사본만 전달합니다.
              </p>
            </div>
            {!drive?.driveConnected ? (
              <button
                type="button"
                disabled={driveBusy}
                onClick={handleDriveConnect}
                className="shrink-0 rounded-full border border-accent/30 px-4 py-1.5 text-xs disabled:opacity-50"
              >
                연결
              </button>
            ) : (
              <button
                type="button"
                disabled={driveBusy}
                onClick={handleDriveDisconnect}
                className="shrink-0 rounded-full border border-red-200 px-4 py-1.5 text-xs text-red-600 disabled:opacity-50"
              >
                해제
              </button>
            )}
          </div>
        </section>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-full bg-accent px-6 py-3 text-sm text-white transition hover:opacity-90 disabled:opacity-60"
        >
          {submitting ? "저장 중..." : "저장하기"}
        </button>
      </form>

      <StatusModal
        open={modal?.kind === "progress"}
        title={modal?.kind === "progress" ? modal.title : ""}
        description={modal?.kind === "progress" ? modal.description : undefined}
        steps={CONNECT_STEPS}
        activeStep={modal?.kind === "progress" ? modal.step : 0}
      />
      <StatusModal
        open={modal?.kind === "done"}
        title={modal?.kind === "done" ? modal.title : ""}
        description={modal?.kind === "done" ? modal.description : undefined}
        steps={CONNECT_STEPS}
        done
        onConfirm={closeDoneModal}
      />
    </main>
  );
}

function Field({
  label,
  value,
  onChange,
  required,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
}) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block text-muted">{label}</span>
      <input
        required={required}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-xl border border-accent/30 bg-white px-4 py-3"
      />
    </label>
  );
}
