import { useEffect, useState } from "react";
import {
  createInviteCode,
  listInviteCodes,
  revokeInviteCode,
  type InviteCodeResponse,
} from "../api/adminInviteCodes";

export default function InviteCodePage() {
  const [codes, setCodes] = useState<InviteCodeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);

  // Create form
  const [showForm, setShowForm] = useState(false);
  const [newCode, setNewCode] = useState("");
  const [description, setDescription] = useState("");
  const [maxUses, setMaxUses] = useState("");
  const [creating, setCreating] = useState(false);

  const [revokingCodes, setRevokingCodes] = useState<Set<string>>(new Set());

  const fetchCodes = () => {
    setLoading(true);
    listInviteCodes()
      .then(setCodes)
      .catch((err) => setLoadError(err instanceof Error ? err.message : "목록을 불러올 수 없습니다."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchCodes();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setMutationError(null);
    try {
      const created = await createInviteCode({
        code: newCode.trim(),
        description: description.trim() || undefined,
        maxUses: maxUses ? parseInt(maxUses, 10) : undefined,
      });
      setCodes((prev) => [created, ...prev]);
      setNewCode("");
      setDescription("");
      setMaxUses("");
      setShowForm(false);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "초대코드 생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (code: string) => {
    setRevokingCodes((prev) => new Set(prev).add(code));
    setMutationError(null);
    try {
      await revokeInviteCode(code);
      setCodes((prev) => prev.map((c) => (c.code === code ? { ...c, enabled: false } : c)));
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "비활성화에 실패했습니다.");
    } finally {
      setRevokingCodes((prev) => {
        const next = new Set(prev);
        next.delete(code);
        return next;
      });
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">초대코드 관리</h1>
        <button
          type="button"
          onClick={() => setShowForm(!showForm)}
          className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
        >
          {showForm ? "취소" : "새 초대코드"}
        </button>
      </div>

      {mutationError && <p className="text-red-600 mb-4">{mutationError}</p>}

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-lg shadow p-4 mb-6 space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">코드</label>
            <input
              type="text"
              value={newCode}
              onChange={(e) => setNewCode(e.target.value)}
              required
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="예: EUNHYE-2026"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">설명 (선택)</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="예: 2026년 새가족용"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">최대 사용 횟수 (선택)</label>
            <input
              type="number"
              value={maxUses}
              onChange={(e) => setMaxUses(e.target.value)}
              min="1"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="비워두면 무제한"
            />
          </div>
          <button
            type="submit"
            disabled={creating || !newCode.trim()}
            className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700 disabled:opacity-50"
          >
            {creating ? "생성 중..." : "생성"}
          </button>
        </form>
      )}

      {loading && <p className="text-gray-500">로딩 중...</p>}
      {loadError && <p className="text-red-600 mb-4">{loadError}</p>}

      {!loading && !loadError && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">코드</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">설명</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">사용</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">생성일</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">작업</th>
              </tr>
            </thead>
            <tbody>
              {codes.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                    등록된 초대코드가 없습니다.
                  </td>
                </tr>
              )}
              {codes.map((c) => (
                <tr key={c.code} className="border-b last:border-b-0 hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-sm">{c.code}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">{c.description ?? "-"}</td>
                  <td className="px-4 py-3 text-sm">
                    {c.usedCount}{c.maxUses != null ? ` / ${c.maxUses}` : ""}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                        c.enabled
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {c.enabled ? "활성" : "비활성"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(c.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td className="px-4 py-3">
                    {c.enabled && (
                      <button
                        type="button"
                        onClick={() => handleRevoke(c.code)}
                        disabled={revokingCodes.has(c.code)}
                        className="text-red-600 hover:text-red-800 text-sm disabled:opacity-50"
                      >
                        {revokingCodes.has(c.code) ? "..." : "비활성화"}
                      </button>
                    )}
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
