import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { deleteHymn, listHymns, updateHymn, type HymnResponse } from "../api/hymns";
import InsightCard from "../components/InsightCard";

type EnabledFilter = "all" | "enabled" | "disabled";

export default function HymnListPage() {
  const [hymns, setHymns] = useState<HymnResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const [search, setSearch] = useState("");
  const [enabledFilter, setEnabledFilter] = useState<EnabledFilter>("all");
  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());

  const loadHymns = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listHymns();
      setHymns(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "목록을 불러올 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadHymns();
  }, [loadHymns]);

  const clearActionFeedback = () => {
    setActionError(null);
    setActionSuccess(null);
  };

  const filteredHymns = useMemo(() => {
    const query = search.trim().toLowerCase();
    return hymns.filter((hymn) => {
      if (enabledFilter === "enabled" && !hymn.enabled) return false;
      if (enabledFilter === "disabled" && hymn.enabled) return false;

      if (query) {
        const titleMatch = hymn.title.toLowerCase().includes(query);
        const numberMatch = hymn.number != null && hymn.number.toLowerCase().includes(query);
        const tagsMatch = hymn.tags != null && hymn.tags.toLowerCase().includes(query);
        if (!titleMatch && !numberMatch && !tagsMatch) return false;
      }

      return true;
    });
  }, [hymns, search, enabledFilter]);

  const enabledCount = useMemo(() => hymns.filter((item) => item.enabled).length, [hymns]);
  const disabledCount = hymns.length - enabledCount;
  const hasActiveFilter = search.trim().length > 0 || enabledFilter !== "all";

  const clearFilters = () => {
    setSearch("");
    setEnabledFilter("all");
  };

  const handleToggleEnabled = async (hymn: HymnResponse) => {
    clearActionFeedback();
    setTogglingIds((prev) => new Set(prev).add(hymn.id));
    try {
      const updated = await updateHymn(hymn.id, { enabled: !hymn.enabled });
      setHymns((prev) => prev.map((item) => (item.id === hymn.id ? { ...item, enabled: updated.enabled } : item)));
      setActionSuccess(`"${updated.title}" 찬양을 ${updated.enabled ? "활성" : "비활성"} 상태로 변경했습니다.`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setTogglingIds((prev) => {
        const next = new Set(prev);
        next.delete(hymn.id);
        return next;
      });
    }
  };

  const handleDelete = async (hymn: HymnResponse) => {
    if (!window.confirm(`"${hymn.title}" 찬양을 삭제하시겠습니까? 관련된 에셋, 메모, 상태, 이벤트가 모두 삭제됩니다.`)) return;
    clearActionFeedback();
    setDeletingIds((prev) => new Set(prev).add(hymn.id));
    try {
      await deleteHymn(hymn.id);
      setHymns((prev) => prev.filter((item) => item.id !== hymn.id));
      setActionSuccess(`"${hymn.title}" 찬양을 삭제했습니다.`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "삭제에 실패했습니다.");
    } finally {
      setDeletingIds((prev) => {
        const next = new Set(prev);
        next.delete(hymn.id);
        return next;
      });
    }
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-slate-900">찬양 관리</h2>
            <p className="mt-1 text-sm text-slate-600">제목/태그 검색, 활성화 토글, 수정/삭제를 수행합니다.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadHymns()}
              disabled={loading}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50"
            >
              {loading ? "새로고침 중..." : "목록 새로고침"}
            </button>
            <Link
              to="/hymns/new"
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
            >
              새 찬양 추가
            </Link>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <InsightCard title="전체 찬양" value={hymns.length} tone="indigo" badge="HM" description="등록된 찬양" loading={loading} />
          <InsightCard
            title="활성 찬양"
            value={enabledCount}
            tone="emerald"
            badge="LIVE"
            description="서비스 노출 중"
            ratio={hymns.length > 0 ? enabledCount / hymns.length : 0}
            loading={loading}
          />
          <InsightCard
            title="비활성 찬양"
            value={disabledCount}
            tone="amber"
            badge="OFF"
            description="노출 중단 상태"
            ratio={hymns.length > 0 ? disabledCount / hymns.length : 0}
            loading={loading}
          />
        </div>

        <div className="mt-4 flex flex-col gap-3 md:flex-row">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="제목, 번호, 태그 검색..."
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <select
            value={enabledFilter}
            onChange={(e) => setEnabledFilter(e.target.value as EnabledFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">전체</option>
            <option value="enabled">활성만</option>
            <option value="disabled">비활성만</option>
          </select>
          {hasActiveFilter && (
            <button
              type="button"
              onClick={clearFilters}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100"
            >
              필터 초기화
            </button>
          )}
        </div>
      </section>

      {loading && <p className="text-sm text-gray-500">로딩 중...</p>}
      {error && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <span>{error}</span>
          <button
            type="button"
            onClick={() => void loadHymns()}
            className="rounded border border-red-300 px-2.5 py-1 text-xs font-semibold text-red-700 hover:bg-red-100"
          >
            다시 시도
          </button>
        </div>
      )}
      {actionSuccess && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{actionSuccess}</div>
      )}
      {actionError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{actionError}</div>
      )}

      {!loading && !error && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-[700px] w-full text-left">
              <thead className="border-b border-slate-200 bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">번호</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">제목</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">태그</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">작업</th>
                </tr>
              </thead>
              <tbody>
                {filteredHymns.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-10 text-center text-gray-400">
                      {hasActiveFilter ? (
                        "검색 결과가 없습니다."
                      ) : (
                        <div className="space-y-2">
                          <div>등록된 찬양이 없습니다.</div>
                          <Link
                            to="/hymns/new"
                            className="inline-block rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-100"
                          >
                            첫 찬양 추가하기
                          </Link>
                        </div>
                      )}
                    </td>
                  </tr>
                )}
                {filteredHymns.map((hymn) => (
                  <tr key={hymn.id} className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50">
                    <td className="px-4 py-3 text-sm">{hymn.number ?? "-"}</td>
                    <td className="px-4 py-3">
                      <Link to={`/hymns/${hymn.id}/edit`} className="font-medium text-indigo-600 hover:underline">
                        {hymn.title}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">{hymn.tags ?? "-"}</td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => void handleToggleEnabled(hymn)}
                        disabled={togglingIds.has(hymn.id)}
                        className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold transition-colors disabled:opacity-50 ${
                          hymn.enabled
                            ? "bg-green-100 text-green-700 hover:bg-green-200"
                            : "bg-gray-100 text-gray-500 hover:bg-gray-200"
                        }`}
                      >
                        {togglingIds.has(hymn.id) ? "..." : hymn.enabled ? "활성" : "비활성"}
                      </button>
                    </td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => void handleDelete(hymn)}
                        disabled={deletingIds.has(hymn.id)}
                        className="text-sm font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                      >
                        {deletingIds.has(hymn.id) ? "삭제 중..." : "삭제"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && !error && (
        <p className="text-sm text-gray-500">
          {filteredHymns.length}건 표시 / 전체 {hymns.length}건
        </p>
      )}
    </div>
  );
}
