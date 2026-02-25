import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createInviteCode,
  listInviteCodes,
  revokeInviteCode,
  type InviteCodeResponse,
} from "../api/adminInviteCodes";
import InsightCard from "../components/InsightCard";

type StatusFilter = "all" | "active" | "inactive" | "expired" | "exhausted";

const CODE_MAX_LENGTH = 40;

function isExpired(code: InviteCodeResponse): boolean {
  return code.expiresAt != null && new Date(code.expiresAt).getTime() < Date.now();
}

function isExhausted(code: InviteCodeResponse): boolean {
  return code.maxUses != null && code.usedCount >= code.maxUses;
}

function statusLabel(code: InviteCodeResponse): "활성" | "비활성" | "만료" | "소진" {
  if (!code.enabled) return "비활성";
  if (isExpired(code)) return "만료";
  if (isExhausted(code)) return "소진";
  return "활성";
}

function statusBadgeClass(code: InviteCodeResponse): string {
  if (!code.enabled) return "bg-slate-100 text-slate-600";
  if (isExpired(code)) return "bg-amber-100 text-amber-700";
  if (isExhausted(code)) return "bg-orange-100 text-orange-700";
  return "bg-emerald-100 text-emerald-700";
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
        [...data].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
      );
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "초대 코드 목록을 불러오지 못했습니다.");
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
      const label = statusLabel(code);
      if (statusFilter === "active" && label !== "활성") return false;
      if (statusFilter === "inactive" && label !== "비활성") return false;
      if (statusFilter === "expired" && label !== "만료") return false;
      if (statusFilter === "exhausted" && label !== "소진") return false;

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

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    const normalizedCode = newCode.trim().toUpperCase();

    if (!normalizedCode) {
      setMutationError("코드를 입력해 주세요.");
      setMutationSuccess(null);
      return;
    }
    if (normalizedCode.length > CODE_MAX_LENGTH) {
      setMutationError(`코드는 ${CODE_MAX_LENGTH}자 이하로 입력해 주세요.`);
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
      setMutationError("최대 사용 횟수는 1 이상이어야 합니다.");
      setMutationSuccess(null);
      return;
    }

    if (expiresAt) {
      const expiresAtMillis = new Date(expiresAt).getTime();
      if (Number.isNaN(expiresAtMillis) || expiresAtMillis <= Date.now()) {
        setMutationError("만료일은 현재 시각 이후여야 합니다.");
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
      setMutationSuccess(`"${created.code}" 초대 코드를 생성했습니다.`);
      resetForm();
      setShowForm(false);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "초대 코드 생성에 실패했습니다.");
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
      setMutationError(err instanceof Error ? err.message : "코드 비활성화에 실패했습니다.");
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
      <section className="soy-panel">
        <div className="soy-panel-header">
          <div>
            <p className="soy-kicker">온보딩</p>
            <h2 className="soy-title">초대 코드 관리</h2>
            <p className="soy-description">회원가입용 초대 코드를 생성/조회/비활성화합니다.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => void loadCodes()} disabled={loading} className="soy-btn soy-btn-secondary">
              {loading ? "로딩 중..." : "새로고침"}
            </button>
            <button type="button" onClick={handleToggleForm} className="soy-btn soy-btn-primary">
              {showForm ? "생성 폼 닫기" : "코드 생성"}
            </button>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <InsightCard title="전체 코드" value={stats.total} tone="slate" badge="ALL" description="등록된 코드 수" loading={loading} />
          <InsightCard
            title="활성 코드"
            value={stats.active}
            tone="emerald"
            badge="ON"
            description="즉시 사용 가능"
            ratio={stats.total > 0 ? stats.active / stats.total : 0}
            loading={loading}
          />
          <InsightCard
            title="비활성 코드"
            value={stats.inactive}
            tone="indigo"
            badge="OFF"
            description="관리자 비활성화"
            ratio={stats.total > 0 ? stats.inactive / stats.total : 0}
            loading={loading}
          />
          <InsightCard
            title="만료 코드"
            value={stats.expired}
            tone="amber"
            badge="EXP"
            description="유효기간 경과"
            ratio={stats.total > 0 ? stats.expired / stats.total : 0}
            loading={loading}
          />
          <InsightCard
            title="소진 코드"
            value={stats.exhausted}
            tone="rose"
            badge="MAX"
            description="사용 횟수 초과"
            ratio={stats.total > 0 ? stats.exhausted / stats.total : 0}
            loading={loading}
          />
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-[1fr_220px_auto]">
          <input
            type="text"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="코드/설명 검색"
            className="soy-input"
          />
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
            className="soy-select"
          >
            <option value="all">전체 상태</option>
            <option value="active">활성</option>
            <option value="inactive">비활성</option>
            <option value="expired">만료</option>
            <option value="exhausted">소진</option>
          </select>
          {hasActiveFilter && (
            <button type="button" onClick={clearFilters} className="soy-btn soy-btn-secondary">
              필터 초기화
            </button>
          )}
        </div>
      </section>

      {mutationSuccess && <div className="soy-alert soy-alert-success">{mutationSuccess}</div>}
      {mutationError && <div className="soy-alert soy-alert-error">{mutationError}</div>}

      {showForm && (
        <form onSubmit={handleCreate} className="soy-panel space-y-3">
          <div>
            <label className="soy-label">코드 *</label>
            <input
              type="text"
              value={newCode}
              onChange={(event) => setNewCode(event.target.value.toUpperCase())}
              required
              maxLength={CODE_MAX_LENGTH}
              className="soy-input"
              placeholder="EUNHYE-2026"
            />
            <div className="mt-1 text-xs text-slate-500">
              공백 없이 입력 ({newCode.trim().length}/{CODE_MAX_LENGTH})
            </div>
          </div>

          <div>
            <label className="soy-label">설명</label>
            <input
              type="text"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className="soy-input"
              placeholder="2026년 상반기 등록용"
            />
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="soy-label">최대 사용 횟수</label>
              <input
                type="number"
                value={maxUses}
                onChange={(event) => setMaxUses(event.target.value)}
                min="1"
                className="soy-input"
                placeholder="비워두면 무제한"
              />
            </div>
            <div>
              <label className="soy-label">만료 일시</label>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(event) => setExpiresAt(event.target.value)}
                className="soy-input"
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button type="submit" disabled={creating || !newCode.trim()} className="soy-btn soy-btn-primary">
              {creating ? "생성 중..." : "생성"}
            </button>
            <button
              type="button"
              onClick={() => {
                resetForm();
                setShowForm(false);
              }}
              className="soy-btn soy-btn-secondary"
            >
              취소
            </button>
          </div>
        </form>
      )}

      {loading && <p className="text-sm text-slate-500">초대 코드 데이터를 불러오는 중입니다...</p>}
      {loadError && (
        <div className="soy-alert soy-alert-error flex flex-wrap items-center justify-between gap-2">
          <span>{loadError}</span>
          <button type="button" onClick={() => void loadCodes()} className="soy-btn soy-btn-secondary">
            다시 시도
          </button>
        </div>
      )}

      {!loading && !loadError && (
        <section className="soy-panel p-0">
          <div className="soy-table-wrap">
            <table className="soy-table min-w-[920px]">
              <thead>
                <tr>
                  <th>코드</th>
                  <th>설명</th>
                  <th>사용량</th>
                  <th>상태</th>
                  <th>만료일</th>
                  <th>생성일</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredCodes.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <div className="soy-empty m-3">
                        {hasActiveFilter ? "검색/필터 조건에 맞는 코드가 없습니다." : "등록된 초대 코드가 없습니다."}
                      </div>
                    </td>
                  </tr>
                )}
                {filteredCodes.map((code) => (
                  <tr key={code.code}>
                    <td>
                      <div className="font-mono text-sm">{code.code}</div>
                      {copiedCode === code.code && <div className="mt-1 text-xs text-emerald-600">복사됨</div>}
                    </td>
                    <td>{code.description ?? "-"}</td>
                    <td>
                      {code.usedCount}
                      {code.maxUses != null ? ` / ${code.maxUses}` : " / 무제한"}
                    </td>
                    <td>
                      <span className={`soy-pill ${statusBadgeClass(code)}`}>{statusLabel(code)}</span>
                    </td>
                    <td>
                      {code.expiresAt ? (
                        <span className={isExpired(code) ? "text-amber-700" : "text-slate-500"}>
                          {new Date(code.expiresAt).toLocaleString("ko-KR")}
                        </span>
                      ) : (
                        <span className="text-slate-400">-</span>
                      )}
                    </td>
                    <td>{new Date(code.createdAt).toLocaleString("ko-KR")}</td>
                    <td>
                      <div className="flex items-center gap-1">
                        <button type="button" onClick={() => void handleCopyCode(code.code)} className="soy-btn soy-btn-ghost text-indigo-700">
                          복사
                        </button>
                        {code.enabled && (
                          <button
                            type="button"
                            onClick={() => void handleRevoke(code.code)}
                            disabled={revokingCodes.has(code.code)}
                            className="soy-btn soy-btn-ghost text-red-600"
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
        <p className="text-sm text-slate-500">
          표시 {filteredCodes.length.toLocaleString("ko-KR")}건 / 전체 {codes.length.toLocaleString("ko-KR")}건
        </p>
      )}
    </div>
  );
}
