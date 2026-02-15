import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createInviteCode,
  listInviteCodes,
  revokeInviteCode,
  type InviteCodeResponse,
} from "../api/adminInviteCodes";

type StatusFilter = "all" | "active" | "inactive" | "expired" | "exhausted";

const CODE_MAX_LENGTH = 40;

function isExpired(code: InviteCodeResponse): boolean {
  return code.expiresAt != null && new Date(code.expiresAt).getTime() < Date.now();
}

function isExhausted(code: InviteCodeResponse): boolean {
  return code.maxUses != null && code.usedCount >= code.maxUses;
}

function statusLabel(code: InviteCodeResponse): string {
  if (!code.enabled) return "비활성";
  if (isExpired(code)) return "만료";
  if (isExhausted(code)) return "소진";
  return "활성";
}

function statusBadgeClass(code: InviteCodeResponse): string {
  if (!code.enabled) return "bg-slate-100 text-slate-600";
  if (isExpired(code)) return "bg-amber-100 text-amber-700";
  if (isExhausted(code)) return "bg-orange-100 text-orange-700";
  return "bg-green-100 text-green-700";
}

function StatCard({ title, value }: { title: string; value: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
      <div className="text-xs text-slate-500">{title}</div>
      <div className="mt-1 text-xl font-semibold text-slate-900">{value.toLocaleString("ko-KR")}</div>
    </div>
  );
}

async function copyTextToClipboard(text: string): Promise<void> {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "");
  textarea.style.position = "absolute";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  document.body.removeChild(textarea);

  if (!copied) {
    throw new Error("copy_failed");
  }
}

export default function InviteCodePage() {
  const [codes, setCodes] = useState<InviteCodeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [mutationSuccess, setMutationSuccess] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");

  const [showForm, setShowForm] = useState(false);
  const [newCode, setNewCode] = useState("");
  const [description, setDescription] = useState("");
  const [maxUses, setMaxUses] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [creating, setCreating] = useState(false);

  const [revokingCodes, setRevokingCodes] = useState<Set<string>>(new Set());

  const loadCodes = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await listInviteCodes();
      setCodes(
        [...data].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        ),
      );
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "목록을 불러올 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCodes();
  }, [loadCodes]);

  const clearFeedback = () => {
    setMutationError(null);
    setMutationSuccess(null);
  };

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
    setShowForm((prev) => !prev);
  };

  const hasActiveFilter = search.trim().length > 0 || statusFilter !== "all";

  const filteredCodes = useMemo(() => {
    const query = search.trim().toLowerCase();
    return codes.filter((code) => {
      if (statusFilter === "active" && statusLabel(code) !== "활성") return false;
      if (statusFilter === "inactive" && statusLabel(code) !== "비활성") return false;
      if (statusFilter === "expired" && statusLabel(code) !== "만료") return false;
      if (statusFilter === "exhausted" && statusLabel(code) !== "소진") return false;

      if (!query) return true;
      return code.code.toLowerCase().includes(query) || (code.description ?? "").toLowerCase().includes(query);
    });
  }, [codes, search, statusFilter]);

  const stats = useMemo(() => {
    let active = 0;
    let inactive = 0;
    let expired = 0;
    let exhausted = 0;

    for (const code of codes) {
      const label = statusLabel(code);
      if (label === "활성") active += 1;
      else if (label === "비활성") inactive += 1;
      else if (label === "만료") expired += 1;
      else exhausted += 1;
    }

    return {
      total: codes.length,
      active,
      inactive,
      expired,
      exhausted,
    };
  }, [codes]);

  const clearFilters = () => {
    setSearch("");
    setStatusFilter("all");
  };

  const handleCopyCode = async (code: string) => {
    clearFeedback();
    try {
      await copyTextToClipboard(code);
      setCopiedCode(code);
      setMutationSuccess(`"${code}" 코드를 클립보드에 복사했습니다.`);
      window.setTimeout(() => setCopiedCode((prev) => (prev === code ? null : prev)), 1500);
    } catch {
      setMutationError("클립보드 복사에 실패했습니다. 수동으로 복사해 주세요.");
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    const normalizedCode = newCode.trim().toUpperCase();

    if (!normalizedCode) {
      setMutationError("코드를 입력해 주세요.");
      setMutationSuccess(null);
      return;
    }
    if (normalizedCode.length > CODE_MAX_LENGTH) {
      setMutationError(`코드는 최대 ${CODE_MAX_LENGTH}자까지 입력할 수 있습니다.`);
      setMutationSuccess(null);
      return;
    }
    if (/\s/.test(normalizedCode)) {
      setMutationError("코드에는 공백을 포함할 수 없습니다.");
      setMutationSuccess(null);
      return;
    }

    const parsedMaxUses = maxUses ? parseInt(maxUses, 10) : undefined;
    if (parsedMaxUses !== undefined && (Number.isNaN(parsedMaxUses) || parsedMaxUses < 1)) {
      setMutationError("최대 사용 횟수는 1 이상의 숫자여야 합니다.");
      setMutationSuccess(null);
      return;
    }

    if (expiresAt) {
      const expiresAtMillis = new Date(expiresAt).getTime();
      if (Number.isNaN(expiresAtMillis) || expiresAtMillis <= Date.now()) {
        setMutationError("만료일은 현재 시각 이후로 입력해 주세요.");
        setMutationSuccess(null);
        return;
      }
    }

    setCreating(true);
    clearFeedback();
    try {
      const created = await createInviteCode({
        code: normalizedCode,
        description: description.trim() || undefined,
        maxUses: parsedMaxUses,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      });
      setCodes((prev) => [created, ...prev]);
      setMutationSuccess(`"${created.code}" 초대코드를 생성했습니다.`);
      resetForm();
      setShowForm(false);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "초대코드 생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (code: string) => {
    if (!window.confirm(`"${code}" 코드를 비활성화하시겠습니까?`)) return;

    setRevokingCodes((prev) => new Set(prev).add(code));
    clearFeedback();
    try {
      await revokeInviteCode(code);
      setCodes((prev) => prev.map((item) => (item.code === code ? { ...item, enabled: false } : item)));
      setMutationSuccess(`"${code}" 코드를 비활성화했습니다.`);
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
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadCodes()}
              disabled={loading}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50"
            >
              {loading ? "새로고침 중..." : "목록 새로고침"}
            </button>
            <button
              type="button"
              onClick={handleToggleForm}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
            >
              {showForm ? "생성 취소" : "새 초대코드"}
            </button>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-5">
          <StatCard title="전체 코드" value={stats.total} />
          <StatCard title="활성 코드" value={stats.active} />
          <StatCard title="비활성 코드" value={stats.inactive} />
          <StatCard title="만료 코드" value={stats.expired} />
          <StatCard title="소진 코드" value={stats.exhausted} />
        </div>

        <div className="mt-4 flex flex-col gap-3 md:flex-row">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="코드/설명 검색..."
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">전체 상태</option>
            <option value="active">활성</option>
            <option value="inactive">비활성</option>
            <option value="expired">만료</option>
            <option value="exhausted">소진</option>
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

      {mutationSuccess && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{mutationSuccess}</div>
      )}
      {mutationError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{mutationError}</div>
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">코드 *</label>
            <input
              type="text"
              value={newCode}
              onChange={(e) => setNewCode(e.target.value.toUpperCase())}
              required
              maxLength={CODE_MAX_LENGTH}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="예: EUNHYE-2026"
            />
            <div className="mt-1 text-xs text-slate-500">
              공백 없이 입력해 주세요. ({newCode.trim().length}/{CODE_MAX_LENGTH})
            </div>
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
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
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
          </div>
          <div className="flex items-center gap-2">
            <button
              type="submit"
              disabled={creating || !newCode.trim()}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {creating ? "생성 중..." : "생성"}
            </button>
            <button
              type="button"
              onClick={() => {
                resetForm();
                setShowForm(false);
              }}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100"
            >
              취소
            </button>
          </div>
        </form>
      )}

      {loading && <p className="text-sm text-gray-500">로딩 중...</p>}
      {loadError && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <span>{loadError}</span>
          <button
            type="button"
            onClick={() => void loadCodes()}
            className="rounded border border-red-300 px-2.5 py-1 text-xs font-semibold text-red-700 hover:bg-red-100"
          >
            다시 시도
          </button>
        </div>
      )}

      {!loading && !loadError && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-[900px] w-full text-left">
              <thead className="border-b border-slate-200 bg-slate-50">
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
                {filteredCodes.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-10 text-center text-gray-400">
                      {hasActiveFilter ? "검색 결과가 없습니다." : "등록된 초대코드가 없습니다."}
                    </td>
                  </tr>
                )}
                {filteredCodes.map((code) => (
                  <tr key={code.code} className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="font-mono text-sm">{code.code}</div>
                      {copiedCode === code.code && <div className="mt-1 text-xs text-emerald-600">복사됨</div>}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">{code.description ?? "-"}</td>
                    <td className="px-4 py-3 text-sm">
                      {code.usedCount}
                      {code.maxUses != null ? ` / ${code.maxUses}` : " / 무제한"}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${statusBadgeClass(code)}`}>
                        {statusLabel(code)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      {code.expiresAt ? (
                        <span className={isExpired(code) ? "text-amber-700" : "text-gray-500"}>
                          {new Date(code.expiresAt).toLocaleString("ko-KR")}
                        </span>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">{new Date(code.createdAt).toLocaleDateString("ko-KR")}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <button
                          type="button"
                          onClick={() => void handleCopyCode(code.code)}
                          className="text-sm font-semibold text-indigo-600 hover:text-indigo-800"
                        >
                          복사
                        </button>
                        {code.enabled && (
                          <button
                            type="button"
                            onClick={() => void handleRevoke(code.code)}
                            disabled={revokingCodes.has(code.code)}
                            className="text-sm font-semibold text-red-600 hover:text-red-800 disabled:opacity-50"
                          >
                            {revokingCodes.has(code.code) ? "..." : "비활성화"}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && !loadError && (
        <p className="text-sm text-gray-500">
          {filteredCodes.length}개 표시 / 전체 {codes.length}개
        </p>
      )}
    </div>
  );
}
