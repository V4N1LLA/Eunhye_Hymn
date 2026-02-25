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
      setError("상황 설명을 입력해 주세요.");
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
          : "AI 추천 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel">
        <p className="soy-kicker">AI 도우미</p>
        <h2 className="soy-title">찬양 추천</h2>
        <p className="soy-description">말씀/모임 상황을 입력하면 상황 기반 추천 목록을 제공합니다.</p>

        <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-700">
          <p className="font-semibold text-slate-900">사용 방법</p>
          <p className="mt-1">1) 상황 입력 2) 추천 개수 선택 3) 추천 실행</p>
          <p className="mt-1 text-xs text-slate-500">최종 선택은 말씀 목적/대상에 맞게 운영자가 확인해 주세요.</p>
        </div>
      </section>

      <section className="soy-panel">
        <form className="space-y-4" onSubmit={runRecommendation}>
          <label className="block">
            <span className="soy-label">상황 설명</span>
            <textarea
              value={situation}
              onChange={(event) => setSituation(event.target.value)}
              rows={4}
              maxLength={180}
              placeholder="예: 주일 말씀, 차분한 묵상 분위기"
              className="soy-textarea"
            />
            <div className="mt-1 text-xs text-slate-500">{situation.trim().length}/180</div>
          </label>

          <label className="block">
            <span className="soy-label">추천 개수</span>
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
              {loading ? "추천 생성 중..." : "추천 받기"}
            </button>
            {requestedCount != null && candidateCount != null && (
              <span className="soy-pill bg-slate-100 text-slate-600">
                요청 {requestedCount}건 | 후보 {candidateCount}건
              </span>
            )}
          </div>
        </form>

        {error && <div className="mt-3 soy-alert soy-alert-error">{error}</div>}
      </section>

      <section className="soy-panel">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold text-slate-900">추천 결과</h3>
          <span className="text-sm text-slate-500">{items.length}건</span>
        </div>

        {items.length === 0 ? (
          <div className="soy-empty">아직 결과가 없습니다. 상황을 입력하고 추천을 실행해 주세요.</div>
        ) : (
          <div className="space-y-3">
            {items.map((item) => (
              <article key={item.id} className="rounded-xl border border-slate-200 bg-slate-50/70 p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <div className="text-xs font-semibold text-slate-500">{item.number?.trim() || "-"}</div>
                    <div className="mt-1 text-base font-semibold text-slate-900">{item.title}</div>
                    <div className="mt-1 text-xs text-slate-500">{item.tags?.trim() || "태그 없음"}</div>
                  </div>
                  <Link to={`/hymns/${item.id}/edit`} className="soy-btn soy-btn-secondary">
                    찬양 열기
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
