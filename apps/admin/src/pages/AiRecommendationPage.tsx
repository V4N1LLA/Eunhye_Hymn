import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { recommendHymns, type RecommendHymnItem } from "../api/ai";

const MAX_RESULTS_OPTIONS = [1, 2, 3, 4, 5] as const;

export default function AiRecommendationPage() {
  const [situation, setSituation] = useState("");
  const [maxResults, setMaxResults] = useState<number>(3);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [items, setItems] = useState<RecommendHymnItem[]>([]);
  const [candidateCount, setCandidateCount] = useState<number | null>(null);
  const [requestedCount, setRequestedCount] = useState<number | null>(null);

  const runRecommendation = async (event: FormEvent) => {
    event.preventDefault();
    const normalized = situation.trim();
    if (!normalized) {
      setError("Situation description is required.");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await recommendHymns({
        situation: normalized,
        maxResults,
      });
      setItems(response.items);
      setCandidateCount(response.candidateCount);
      setRequestedCount(response.requestedMaxResults);
    } catch (requestError) {
      setItems([]);
      setCandidateCount(null);
      setRequestedCount(null);
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Failed to get recommendations. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel">
        <p className="soy-kicker">AI Assistant</p>
        <h2 className="soy-title">Hymn Recommendation</h2>
        <p className="soy-description">Describe your worship context and get situation-based hymn suggestions.</p>

        <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-700">
          <p className="font-semibold text-slate-900">How to use</p>
          <p className="mt-1">1. Describe the context 2. Choose result count 3. Run recommendation.</p>
          <p className="mt-1 text-xs text-slate-500">The final selection should still be reviewed by the operator.</p>
        </div>
      </section>

      <section className="soy-panel">
        <form className="space-y-4" onSubmit={runRecommendation}>
          <label className="block">
            <span className="soy-label">Situation Description</span>
            <textarea
              value={situation}
              onChange={(event) => setSituation(event.target.value)}
              rows={4}
              maxLength={180}
              placeholder="Sunday worship, calm prayer mood"
              className="soy-textarea"
            />
            <div className="mt-1 text-xs text-slate-500">{situation.trim().length}/180</div>
          </label>

          <label className="block">
            <span className="soy-label">Max Results</span>
            <select
              value={maxResults}
              onChange={(event) => setMaxResults(Number(event.target.value))}
              className="soy-select w-[120px]"
            >
              {MAX_RESULTS_OPTIONS.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>

          <div className="flex flex-wrap items-center gap-2">
            <button type="submit" disabled={loading} className="soy-btn soy-btn-primary">
              {loading ? "Generating..." : "Get Recommendations"}
            </button>
            {requestedCount != null && candidateCount != null && (
              <span className="soy-pill bg-slate-100 text-slate-600">
                Requested {requestedCount} | Candidates {candidateCount}
              </span>
            )}
          </div>
        </form>

        {error && <div className="mt-3 soy-alert soy-alert-error">{error}</div>}
      </section>

      <section className="soy-panel">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold text-slate-900">Recommendation Results</h3>
          <span className="text-sm text-slate-500">{items.length}</span>
        </div>

        {items.length === 0 ? (
          <div className="soy-empty">No results yet. Enter a situation and run recommendation.</div>
        ) : (
          <div className="space-y-3">
            {items.map((item) => (
              <article key={item.id} className="rounded-xl border border-slate-200 bg-slate-50/70 p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <div className="text-xs font-semibold text-slate-500">{item.number?.trim() || "-"}</div>
                    <div className="mt-1 text-base font-semibold text-slate-900">{item.title}</div>
                    <div className="mt-1 text-xs text-slate-500">{item.tags?.trim() || "No tags"}</div>
                  </div>
                  <Link to={`/hymns/${item.id}/edit`} className="soy-btn soy-btn-secondary">
                    Open Hymn
                  </Link>
                </div>
                <p className="mt-3 rounded-lg bg-white px-3 py-2 text-sm text-slate-700">{item.reason}</p>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
