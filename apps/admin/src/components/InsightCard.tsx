type InsightTone = "indigo" | "emerald" | "amber" | "sky" | "violet" | "rose" | "slate";

type InsightCardProps = {
  title: string;
  value: number | null;
  description?: string;
  badge?: string;
  tone?: InsightTone;
  ratio?: number | null;
  loading?: boolean;
};

type ToneStyle = {
  card: string;
  glow: string;
  value: string;
  badge: string;
  progress: string;
};

const TONE_STYLES: Record<InsightTone, ToneStyle> = {
  indigo: {
    card: "border-indigo-200 bg-gradient-to-br from-white via-indigo-50 to-indigo-100/70",
    glow: "bg-indigo-300/70",
    value: "text-indigo-700",
    badge: "bg-indigo-600/10 text-indigo-700",
    progress: "bg-indigo-500",
  },
  emerald: {
    card: "border-emerald-200 bg-gradient-to-br from-white via-emerald-50 to-emerald-100/70",
    glow: "bg-emerald-300/70",
    value: "text-emerald-700",
    badge: "bg-emerald-600/10 text-emerald-700",
    progress: "bg-emerald-500",
  },
  amber: {
    card: "border-amber-200 bg-gradient-to-br from-white via-amber-50 to-amber-100/70",
    glow: "bg-amber-300/70",
    value: "text-amber-700",
    badge: "bg-amber-600/10 text-amber-700",
    progress: "bg-amber-500",
  },
  sky: {
    card: "border-sky-200 bg-gradient-to-br from-white via-sky-50 to-sky-100/70",
    glow: "bg-sky-300/70",
    value: "text-sky-700",
    badge: "bg-sky-600/10 text-sky-700",
    progress: "bg-sky-500",
  },
  violet: {
    card: "border-violet-200 bg-gradient-to-br from-white via-violet-50 to-violet-100/70",
    glow: "bg-violet-300/70",
    value: "text-violet-700",
    badge: "bg-violet-600/10 text-violet-700",
    progress: "bg-violet-500",
  },
  rose: {
    card: "border-rose-200 bg-gradient-to-br from-white via-rose-50 to-rose-100/70",
    glow: "bg-rose-300/70",
    value: "text-rose-700",
    badge: "bg-rose-600/10 text-rose-700",
    progress: "bg-rose-500",
  },
  slate: {
    card: "border-slate-200 bg-gradient-to-br from-white via-slate-50 to-slate-100/70",
    glow: "bg-slate-300/70",
    value: "text-slate-900",
    badge: "bg-slate-900/10 text-slate-700",
    progress: "bg-slate-500",
  },
};

function clampRatio(value: number): number {
  if (!Number.isFinite(value)) return 0;
  if (value <= 0) return 0;
  if (value >= 1) return 1;
  return value;
}

export default function InsightCard({
  title,
  value,
  description,
  badge,
  tone = "indigo",
  ratio,
  loading = false,
}: InsightCardProps) {
  const toneStyle = TONE_STYLES[tone];
  const ratioValue = ratio == null ? null : clampRatio(ratio);
  const ratioPercent = ratioValue == null ? null : Math.round(ratioValue * 100);
  const displayValue = loading ? "..." : value == null ? "-" : value.toLocaleString("ko-KR");

  return (
    <div className={`relative overflow-hidden rounded-2xl border p-4 shadow-sm ${toneStyle.card}`}>
      <div className={`pointer-events-none absolute -right-6 -top-6 h-20 w-20 rounded-full blur-2xl ${toneStyle.glow}`} />
      <div className="relative flex items-start justify-between gap-3">
        <div>
          <div className="text-xs font-semibold tracking-wide text-slate-500">{title}</div>
          <div className={`mt-2 text-2xl font-bold ${toneStyle.value}`}>{displayValue}</div>
          {description && <div className="mt-1 text-xs text-slate-600">{description}</div>}
        </div>
        {badge && <span className={`rounded-lg px-2 py-1 text-[10px] font-semibold ${toneStyle.badge}`}>{badge}</span>}
      </div>
      {ratioPercent != null && !loading && (
        <div className="relative mt-3">
          <div className="mb-1 flex items-center justify-between text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            <span>Ratio</span>
            <span>{ratioPercent}%</span>
          </div>
          <div className="h-1.5 overflow-hidden rounded-full bg-white/80">
            <div
              className={`h-full rounded-full transition-[width] duration-300 ${toneStyle.progress}`}
              style={{ width: `${ratioPercent}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );
}
