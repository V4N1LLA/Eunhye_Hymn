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
      setError("Please enter your situation.");
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
          : "Failed to get recommendations.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900">AI Hymn Recommendation</h2>
        <p className="mt-1 text-sm text-slate-600">
          Enter one situation and get low-cost recommendations from
          `gemini-2.5-flash-lite`.
        </p>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <form className="space-y-4" onSubmit={runRecommendation}>
          <label className="block text-sm">
            <div className="mb-1 font-semibold text-slate-700">Situation</div>
            <textarea
              value={situation}
              onChange={(event) => setSituation(event.target.value)}
              rows={4}
              maxLength={180}
              placeholder="Example: Sunday dawn prayer, calm and reflective mood"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <div className="mt-1 text-xs text-slate-500">
              {situation.trim().length}/180
            </div>
          </label>

          <label className="block text-sm">
            <div className="mb-1 font-semibold text-slate-700">Max Results</div>
            <select
              value={maxResults}
              onChange={(event) => setMaxResults(Number(event.target.value))}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {MAX_RESULTS_OPTIONS.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-60"
            >
              {loading ? "Recommending..." : "Run AI Recommendation"}
            </button>
            {requestedCount != null && candidateCount != null && (
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
                Requested {requestedCount}, candidates {candidateCount}
              </span>
            )}
          </div>
        </form>

        {error && (
          <div className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold text-slate-900">Recommendations</h3>
          <span className="text-sm text-slate-500">{items.length} items</span>
        </div>

        {items.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            No results yet. Run a recommendation first.
          </div>
        ) : (
          <div className="space-y-3">
            {items.map((item) => (
              <article
                key={item.id}
                className="rounded-xl border border-slate-200 bg-slate-50/60 p-4"
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <div className="text-xs font-semibold text-slate-500">
                      {item.number?.trim() || "-"}
                    </div>
                    <div className="mt-1 text-base font-semibold text-slate-900">
                      {item.title}
                    </div>
                    <div className="mt-1 text-xs text-slate-500">
                      {item.tags?.trim() || "No tags"}
                    </div>
                  </div>
                  <Link
                    to={`/hymns/${item.id}/edit`}
                    className="rounded-md border border-slate-300 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100"
                  >
                    Open Hymn
                  </Link>
                </div>
                <p className="mt-3 rounded-lg bg-white px-3 py-2 text-sm text-slate-700">
                  {item.reason}
                </p>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
