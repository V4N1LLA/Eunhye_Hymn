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
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">찬양 관리</h1>
        <Link
          to="/hymns/new"
          className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
        >
          새 찬양 추가
        </Link>
      </div>

      {/* Search & Filter Bar */}
      <div className="flex items-center gap-3 mb-4">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="제목, 번호, 태그 검색..."
          className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <select
          value={enabledFilter}
          onChange={(e) => setEnabledFilter(e.target.value as EnabledFilter)}
          className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          <option value="all">전체</option>
          <option value="enabled">활성만</option>
          <option value="disabled">비활성만</option>
        </select>
      </div>

      {loading && <p className="text-gray-500">로딩 중...</p>}
      {error && <p className="text-red-600 mb-4">{error}</p>}
      {actionError && <p className="text-red-600 mb-4">{actionError}</p>}

      {!loading && !error && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b">
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
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    {search || enabledFilter !== "all"
                      ? "검색 결과가 없습니다."
                      : "등록된 찬양이 없습니다."}
                  </td>
                </tr>
              )}
              {filteredHymns.map((h) => (
                <tr key={h.id} className="border-b last:border-b-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{h.number ?? "-"}</td>
                  <td className="px-4 py-3">
                    <Link to={`/hymns/${h.id}/edit`} className="text-indigo-600 hover:underline">
                      {h.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{h.tags ?? "-"}</td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => handleToggleEnabled(h)}
                      disabled={togglingIds.has(h.id)}
                      className={`inline-block px-2 py-0.5 rounded text-xs font-medium cursor-pointer transition-colors disabled:opacity-50 ${
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
                      className="text-red-600 hover:text-red-800 text-sm font-medium disabled:opacity-50"
                    >
                      {deletingIds.has(h.id) ? "삭제 중..." : "삭제"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Result count */}
      {!loading && !error && (search || enabledFilter !== "all") && (
        <p className="mt-3 text-sm text-gray-500">
          {filteredHymns.length}건 / 전체 {hymns.length}건
        </p>
      )}
    </div>
  );
}
