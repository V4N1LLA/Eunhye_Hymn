import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listHymns, type HymnResponse } from "../api/hymns";

export default function HymnListPage() {
  const [hymns, setHymns] = useState<HymnResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
              {hymns.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-gray-400">
                    등록된 찬양이 없습니다.
                  </td>
                </tr>
              )}
              {hymns.map((h) => (
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
