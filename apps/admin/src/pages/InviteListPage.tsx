import { useEffect, useState } from "react";
import {
  createInvite,
  listInvites,
  revokeInvite,
  type InviteCode,
} from "../api/invites";

export default function InviteListPage() {
  const [invites, setInvites] = useState<InviteCode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newCode, setNewCode] = useState("");
  const [newMaxUses, setNewMaxUses] = useState("");
  const [creating, setCreating] = useState(false);

  const load = () => {
    setLoading(true);
    listInvites()
      .then(setInvites)
      .catch((err) =>
        setError(err instanceof Error ? err.message : "목록을 불러올 수 없습니다.")
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    if (!newCode.trim()) return;
    setCreating(true);
    setError(null);
    try {
      await createInvite({
        code: newCode.trim(),
        maxUses: newMaxUses ? parseInt(newMaxUses, 10) : undefined,
      });
      setNewCode("");
      setNewMaxUses("");
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (id: string) => {
    setError(null);
    try {
      await revokeInvite(id);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "폐기에 실패했습니다.");
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">초대코드 관리</h1>

      {/* Create form */}
      <div className="flex items-end gap-3 mb-6">
        <div className="flex-1">
          <label className="block text-sm font-medium text-gray-700 mb-1">코드</label>
          <input
            type="text"
            value={newCode}
            onChange={(e) => setNewCode(e.target.value)}
            placeholder="예: WELCOME2026"
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div className="w-32">
          <label className="block text-sm font-medium text-gray-700 mb-1">최대 사용</label>
          <input
            type="number"
            value={newMaxUses}
            onChange={(e) => setNewMaxUses(e.target.value)}
            placeholder="무제한"
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <button
          type="button"
          onClick={handleCreate}
          disabled={!newCode.trim() || creating}
          className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700 disabled:opacity-50 text-sm"
        >
          {creating ? "생성 중..." : "생성"}
        </button>
      </div>

      {error && <p className="text-red-600 mb-4">{error}</p>}
      {loading && <p className="text-gray-500">로딩 중...</p>}

      {!loading && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">코드</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">사용</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">생성일</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600"></th>
              </tr>
            </thead>
            <tbody>
              {invites.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    등록된 초대코드가 없습니다.
                  </td>
                </tr>
              )}
              {invites.map((inv) => {
                const isRevoked = inv.revokedAt !== null;
                const isExpired = inv.expiresAt !== null && new Date(inv.expiresAt) < new Date();
                const isMaxed = inv.maxUses !== null && inv.usedCount >= inv.maxUses;
                const isActive = !isRevoked && !isExpired && !isMaxed;

                return (
                  <tr key={inv.id} className="border-b last:border-b-0 hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-sm">{inv.code}</td>
                    <td className="px-4 py-3 text-sm">
                      {inv.usedCount}{inv.maxUses !== null ? ` / ${inv.maxUses}` : ""}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                          isActive
                            ? "bg-green-100 text-green-700"
                            : "bg-gray-100 text-gray-500"
                        }`}
                      >
                        {isRevoked ? "폐기됨" : isExpired ? "만료" : isMaxed ? "소진" : "활성"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {new Date(inv.createdAt).toLocaleDateString("ko-KR")}
                    </td>
                    <td className="px-4 py-3">
                      {isActive && (
                        <button
                          type="button"
                          onClick={() => handleRevoke(inv.id)}
                          className="text-red-600 hover:underline text-sm"
                        >
                          폐기
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
