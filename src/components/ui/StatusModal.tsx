"use client";

type StatusModalProps = {
  open: boolean;
  title: string;
  description?: string;
  steps?: string[];
  activeStep?: number;
  done?: boolean;
  confirmLabel?: string;
  onConfirm?: () => void;
};

export function StatusModal({
  open,
  title,
  description,
  steps,
  activeStep = 0,
  done = false,
  confirmLabel = "확인",
  onConfirm,
}: StatusModalProps) {
  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="status-modal-title"
    >
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-lg">
        <h2
          id="status-modal-title"
          className="text-lg font-medium"
          style={{ fontFamily: "var(--font-playfair), serif" }}
        >
          {title}
        </h2>
        {description && <p className="mt-2 text-sm text-muted">{description}</p>}

        {steps && steps.length > 0 && (
          <ol className="mt-5 space-y-2">
            {steps.map((step, index) => {
              const reached = done || index <= activeStep;
              const current = !done && index === activeStep;
              return (
                <li
                  key={step}
                  className={`flex items-start gap-2 text-sm ${
                    reached ? "text-foreground" : "text-muted"
                  }`}
                >
                  <span
                    className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] ${
                      done || index < activeStep
                        ? "bg-emerald-100 text-emerald-700"
                        : current
                          ? "bg-accent/15 text-accent"
                          : "bg-black/5 text-muted"
                    }`}
                  >
                    {done || index < activeStep ? "✓" : index + 1}
                  </span>
                  <span>
                    {step}
                    {current && !done ? "…" : ""}
                  </span>
                </li>
              );
            })}
          </ol>
        )}

        {!done && (
          <div className="mt-6 flex justify-center">
            <span
              className="h-5 w-5 animate-spin rounded-full border-2 border-accent/30 border-t-accent"
              aria-hidden
            />
          </div>
        )}

        {done && onConfirm && (
          <button
            type="button"
            onClick={onConfirm}
            className="mt-6 w-full rounded-full bg-accent px-5 py-2.5 text-sm text-white transition hover:opacity-90"
          >
            {confirmLabel}
          </button>
        )}
      </div>
    </div>
  );
}
