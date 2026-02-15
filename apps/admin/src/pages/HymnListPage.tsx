import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { deleteHymn, listHymns, updateHymn, type HymnResponse } from "../api/hymns";

type EnabledFilter = "all" | "enabled" | "disabled";

export default function HymnListPage() {
  const [hymns, setHymns] = useState<HymnResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [enabledFilter, setEnabledFilter] = useState<EnabledFilter>("all");
  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listHymns()
      .then((data) => {
        if (!cancelled) setHymns(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "목록을 불러올 수 없습니다.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filteredHymns = useMemo(() => {
    const query = search.trim().toLowerCase();
    return hymns.filter((h) => {
      // enabled filter
      if (enabledFilter === "enabled" && !h.enabled) return false;
      if (enabledFilter === "disabled" && h.enabled) return false;

      // text search
      if (query) {
        const titleMatch = h.title.toLowerCase().includes(query);
        const numberMatch = h.number != null && String(h.number).includes(query);
        const tagsMatch = h.tags != null && h.tags.toLowerCase().includes(query);
        if (!titleMatch && !numberMatch && !tagsMatch) return false;
      }

      return true;
    });
  }, [hymns, search, enabledFilter]);

  const handleToggleEnabled = async (hymn: HymnResponse) => {
    setTogglingIds((prev) => new Set(prev).add(hymn.id));
    setActionError(null);
    try {
      const updated = await updateHymn(hymn.id, { enabled: !hymn.enabled });
      setHymns((prev) => prev.map((h) => (h.id === hymn.id ? { ...h, enabled: updated.enabled } : h)));
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
    setDeletingIds((prev) => new Set(prev).add(hymn.id));
    setActionError(null);
    try {
      await deleteHymn(hymn.id);
      setHymns((prev) => prev.filter((h) => h.id !== hymn.id));
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
          <Link
            to="/hymns/new"
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
          >
            새 찬양 추가
          </Link>
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
        </div>
      </section>

      {loading && <p className="text-sm text-gray-500">로딩 중...</p>}
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      {actionError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{actionError}</div>
      )}

      {!loading && !error && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-left">
            <thead className="bg-slate-50 border-b border-slate-200">
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
                    {search || enabledFilter !== "all" ? "검색 결과가 없습니다." : "등록된 찬양이 없습니다."}
                  </td>
                </tr>
              )}
              {filteredHymns.map((h) => (
                <tr key={h.id} className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50">
                  <td className="px-4 py-3 text-sm">{h.number ?? "-"}</td>
                  <td className="px-4 py-3">
                    <Link to={`/hymns/${h.id}/edit`} className="font-medium text-indigo-600 hover:underline">
                      {h.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{h.tags ?? "-"}</td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => handleToggleEnabled(h)}
                      disabled={togglingIds.has(h.id)}
                      className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold transition-colors disabled:opacity-50 ${
                        h.enabled
                          ? "bg-green-100 text-green-700 hover:bg-green-200"
                          : "bg-gray-100 text-gray-500 hover:bg-gray-200"
                      }`}
                    >
                      {togglingIds.has(h.id) ? "..." : h.enabled ? "활성" : "비활성"}
                    </button>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => handleDelete(h)}
                      disabled={deletingIds.has(h.id)}
                      className="text-sm font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                    >
                      {deletingIds.has(h.id) ? "삭제 중..." : "삭제"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {!loading && !error && (search || enabledFilter !== "all") && (
        <p className="text-sm text-gray-500">{filteredHymns.length}건 / 전체 {hymns.length}건</p>
      )}
    </div>
  );
}
