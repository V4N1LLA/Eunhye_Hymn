import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { listHymns, type HymnResponse } from "../api/hymns";

type StatusFilter = "all" | "enabled" | "disabled";

export default function HymnListPage() {
  const [hymns, setHymns] = useState<HymnResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");

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

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return hymns.filter((h) => {
      if (statusFilter === "enabled" && !h.enabled) return false;
      if (statusFilter === "disabled" && h.enabled) return false;
      if (query) {
        const titleMatch = h.title.toLowerCase().includes(query);
        const numberMatch = h.number !== null && String(h.number).includes(query);
        if (!titleMatch && !numberMatch) return false;
      }
      return true;
    });
  }, [hymns, search, statusFilter]);

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

      <div className="flex items-center gap-3 mb-4">
        <input
          type="text"
          placeholder="제목 또는 번호 검색..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
          className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          <option value="all">전체</option>
          <option value="enabled">활성</option>
          <option value="disabled">비활성</option>
        </select>
      </div>

      {loading && <p className="text-gray-500">로딩 중...</p>}
      {error && <p className="text-red-600">{error}</p>}

      {!loading && !error && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">번호</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">제목</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">태그</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-gray-400">
                    {hymns.length === 0 ? "등록된 찬양이 없습니다." : "검색 결과가 없습니다."}
                  </td>
                </tr>
              )}
              {filtered.map((h) => (
                <tr key={h.id} className="border-b last:border-b-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{h.number ?? "-"}</td>
                  <td className="px-4 py-3">
                    <Link to={`/hymns/${h.id}/edit`} className="text-indigo-600 hover:underline">
                      {h.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{h.tags ?? "-"}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                        h.enabled
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {h.enabled ? "활성" : "비활성"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
