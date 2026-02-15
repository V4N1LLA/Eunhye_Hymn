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
  const [expiresAt, setExpiresAt] = useState("");
  const [creating, setCreating] = useState(false);

  const [revokingCodes, setRevokingCodes] = useState<Set<string>>(new Set());

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    listInviteCodes()
      .then((data) => {
        if (!cancelled) setCodes(data);
      })
      .catch((err) => {
        if (!cancelled) setLoadError(err instanceof Error ? err.message : "목록을 불러올 수 없습니다.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const resetForm = () => {
    setNewCode("");
    setDescription("");
    setMaxUses("");
    setExpiresAt("");
  };

  const handleToggleForm = () => {
    if (showForm) {
      resetForm();
    }
    setShowForm(!showForm);
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setMutationError(null);
    const parsed = maxUses ? parseInt(maxUses, 10) : undefined;
    try {
      const created = await createInviteCode({
        code: newCode.trim(),
        description: description.trim() || undefined,
        maxUses: parsed !== undefined && !Number.isNaN(parsed) ? parsed : undefined,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      });
      setCodes((prev) => [created, ...prev]);
      resetForm();
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
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-slate-900">초대코드 관리</h2>
            <p className="mt-1 text-sm text-slate-600">앱 사용자 등록용 코드 생성/비활성화/사용량 조회를 수행합니다.</p>
          </div>
          <button
            type="button"
            onClick={handleToggleForm}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
          >
            {showForm ? "취소" : "새 초대코드"}
          </button>
        </div>
      </section>

      {mutationError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{mutationError}</div>
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">코드</label>
            <input
              type="text"
              value={newCode}
              onChange={(e) => setNewCode(e.target.value)}
              required
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="예: EUNHYE-2026"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">설명 (선택)</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="예: 2026년 새가족용"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">최대 사용 횟수 (선택)</label>
            <input
              type="number"
              value={maxUses}
              onChange={(e) => setMaxUses(e.target.value)}
              min="1"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="비워두면 무제한"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">만료일 (선택)</label>
            <input
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <button
            type="submit"
            disabled={creating || !newCode.trim()}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {creating ? "생성 중..." : "생성"}
          </button>
        </form>
      )}

      {loading && <p className="text-sm text-gray-500">로딩 중...</p>}
      {loadError && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{loadError}</div>}

      {!loading && !loadError && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-left">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">코드</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">설명</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">사용</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">만료일</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">생성일</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">작업</th>
              </tr>
            </thead>
            <tbody>
              {codes.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-10 text-center text-gray-400">
                    등록된 초대코드가 없습니다.
                  </td>
                </tr>
              )}
              {codes.map((c) => (
                <tr key={c.code} className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50">
                  <td className="px-4 py-3 font-mono text-sm">{c.code}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">{c.description ?? "-"}</td>
                  <td className="px-4 py-3 text-sm">
                    {c.usedCount}
                    {c.maxUses != null ? ` / ${c.maxUses}` : ""}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${
                        c.enabled ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {c.enabled ? "활성" : "비활성"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {c.expiresAt ? (
                      <span className={new Date(c.expiresAt) < new Date() ? "text-red-500" : "text-gray-500"}>
                        {new Date(c.expiresAt).toLocaleDateString("ko-KR")}
                        {new Date(c.expiresAt) < new Date() && " (만료)"}
                      </span>
                    ) : (
                      <span className="text-gray-400">-</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{new Date(c.createdAt).toLocaleDateString("ko-KR")}</td>
                  <td className="px-4 py-3">
                    {c.enabled && (
                      <button
                        type="button"
                        onClick={() => handleRevoke(c.code)}
                        disabled={revokingCodes.has(c.code)}
                        className="text-sm font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                      >
                        {revokingCodes.has(c.code) ? "..." : "비활성화"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  );
}
