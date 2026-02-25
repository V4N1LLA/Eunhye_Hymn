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
      setError(err instanceof Error ? err.message : "찬양 목록을 불러오지 못했습니다.");
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
      setActionSuccess(`"${updated.title}" 찬양 상태를 ${updated.enabled ? "활성" : "비활성"}으로 변경했습니다.`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "찬양 상태 변경에 실패했습니다.");
    } finally {
      setTogglingIds((prev) => {
        const next = new Set(prev);
        next.delete(hymn.id);
        return next;
      });
    }
  };

  const handleDelete = async (hymn: HymnResponse) => {
    if (!window.confirm(`"${hymn.title}" 찬양을 삭제하시겠습니까? 연결된 에셋/메타데이터도 함께 삭제됩니다.`)) return;
    clearActionFeedback();
    setDeletingIds((prev) => new Set(prev).add(hymn.id));
    try {
      await deleteHymn(hymn.id);
      setHymns((prev) => prev.filter((item) => item.id !== hymn.id));
      setActionSuccess(`"${hymn.title}" 찬양을 삭제했습니다.`);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "찬양 삭제에 실패했습니다.");
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
      <section className="soy-panel">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">카탈로그</p>
            <h2 className="soy-title">찬양 관리</h2>
            <p className="soy-description">제목/번호/태그 검색과 노출 상태 관리를 한 화면에서 처리합니다.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => void loadHymns()} disabled={loading} className="soy-btn soy-btn-secondary">
              {loading ? "로딩 중..." : "새로고침"}
            </button>
            <Link to="/hymns/new" className="soy-btn soy-btn-primary">
              찬양 추가
            </Link>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <InsightCard title="전체 찬양" value={hymns.length} tone="indigo" badge="HM" description="등록된 찬양 수" loading={loading} />
          <InsightCard
            title="활성 찬양"
            value={enabledCount}
            tone="emerald"
            badge="LIVE"
            description="사용자 노출 중"
            ratio={hymns.length > 0 ? enabledCount / hymns.length : 0}
            loading={loading}
          />
          <InsightCard
            title="비활성 찬양"
            value={disabledCount}
            tone="amber"
            badge="OFF"
            description="노출 중지 상태"
            ratio={hymns.length > 0 ? disabledCount / hymns.length : 0}
            loading={loading}
          />
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-[1fr_220px_auto]">
          <input
            type="text"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="제목, 번호, 태그 검색"
            className="soy-input"
          />
          <select value={enabledFilter} onChange={(event) => setEnabledFilter(event.target.value as EnabledFilter)} className="soy-select">
            <option value="all">전체</option>
            <option value="enabled">활성</option>
            <option value="disabled">비활성</option>
          </select>
          {hasActiveFilter && (
            <button type="button" onClick={clearFilters} className="soy-btn soy-btn-secondary">
              필터 초기화
            </button>
          )}
        </div>
      </section>

      {loading && <p className="text-sm text-slate-500">찬양 데이터를 불러오는 중입니다...</p>}
      {error && (
        <div className="soy-alert soy-alert-error flex flex-wrap items-center justify-between gap-2">
          <span>{error}</span>
          <button type="button" onClick={() => void loadHymns()} className="soy-btn soy-btn-secondary">
            다시 시도
          </button>
        </div>
      )}
      {actionSuccess && <div className="soy-alert soy-alert-success">{actionSuccess}</div>}
      {actionError && <div className="soy-alert soy-alert-error">{actionError}</div>}

      {!loading && !error && (
        <section className="soy-panel p-0">
          <div className="soy-table-wrap">
            <table className="soy-table min-w-[760px]">
              <thead>
                <tr>
                  <th>번호</th>
                  <th>제목</th>
                  <th>태그</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredHymns.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="soy-empty m-3">
                        {hasActiveFilter ? "검색/필터 조건에 맞는 찬양이 없습니다." : "등록된 찬양이 없습니다."}
                        {!hasActiveFilter && (
                          <div className="mt-3">
                            <Link to="/hymns/new" className="soy-btn soy-btn-secondary">
                              첫 찬양 추가하기
                            </Link>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
                {filteredHymns.map((hymn) => (
                  <tr key={hymn.id}>
                    <td>{hymn.number ?? "-"}</td>
                    <td>
                      <Link to={`/hymns/${hymn.id}/edit`} className="font-semibold text-indigo-700 hover:underline">
                        {hymn.title}
                      </Link>
                    </td>
                    <td>{hymn.tags ?? "-"}</td>
                    <td>
                      <button
                        type="button"
                        onClick={() => void handleToggleEnabled(hymn)}
                        disabled={togglingIds.has(hymn.id)}
                        className={`soy-pill ${hymn.enabled ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-600"}`}
                      >
                        {togglingIds.has(hymn.id) ? "변경 중..." : hymn.enabled ? "활성" : "비활성"}
                      </button>
                    </td>
                    <td>
                      <button
                        type="button"
                        onClick={() => void handleDelete(hymn)}
                        disabled={deletingIds.has(hymn.id)}
                        className="soy-btn soy-btn-ghost text-red-600 hover:text-red-700"
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
        <p className="text-sm text-slate-500">
          표시 {filteredHymns.length.toLocaleString("ko-KR")}건 / 전체 {hymns.length.toLocaleString("ko-KR")}건
        </p>
      )}
    </div>
  );
}
