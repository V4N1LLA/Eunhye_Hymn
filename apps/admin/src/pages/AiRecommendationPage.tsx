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
          : "추천을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900">AI 찬송 추천</h2>
        <p className="mt-1 text-sm text-slate-600">
          예배/모임 상황을 입력하면 AI가 상황에 맞는 찬송을 추천합니다.
        </p>
        <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-700">
          <p className="font-semibold text-slate-900">사용 방법</p>
          <p className="mt-1">
            1) 상황 설명 입력 → 2) 추천 개수 선택 → 3) <span className="font-semibold">추천 받기</span> 실행
          </p>
          <p className="mt-1 text-xs text-slate-500">
            추천 결과는 참고용이며, 최종 선곡은 예배 목적/대상에 맞게 확인해 주세요.
          </p>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <form className="space-y-4" onSubmit={runRecommendation}>
          <label className="block text-sm">
            <div className="mb-1 font-semibold text-slate-700">상황 설명</div>
            <textarea
              value={situation}
              onChange={(event) => setSituation(event.target.value)}
              rows={4}
              maxLength={180}
              placeholder="예: 주일 새벽예배, 차분한 묵상 분위기"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <div className="mt-1 text-xs text-slate-500">
              {situation.trim().length}/180
            </div>
          </label>

          <label className="block text-sm">
            <div className="mb-1 font-semibold text-slate-700">추천 개수</div>
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
              {loading ? "추천 생성 중..." : "추천 받기"}
            </button>
            {requestedCount != null && candidateCount != null && (
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
                요청 {requestedCount}개 · 후보 {candidateCount}곡
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
          <h3 className="text-base font-semibold text-slate-900">추천 결과</h3>
          <span className="text-sm text-slate-500">{items.length}곡</span>
        </div>

        {items.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            아직 결과가 없습니다. 상황을 입력하고 추천 받기를 눌러 주세요.
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
                      {item.tags?.trim() || "태그 없음"}
                    </div>
                  </div>
                  <Link
                    to={`/hymns/${item.id}/edit`}
                    className="rounded-md border border-slate-300 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100"
                  >
                    찬송 열기
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
