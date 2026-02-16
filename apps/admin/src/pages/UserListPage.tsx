import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createUser,
  deleteUser,
  listUsers,
  updateUser,
  type UserIdentitySummary,
  type UserResponse,
} from "../api/adminUsers";
import { useAuth } from "../auth/AuthContext";
import InsightCard from "../components/InsightCard";

type RoleFilter = "all" | "ADMIN" | "USER";
type StatusFilter = "all" | "ACTIVE" | "DISABLED";
type EditableRole = "ADMIN" | "USER";
type EditableStatus = "ACTIVE" | "DISABLED";

const DISPLAY_NAME_MAX_LENGTH = 64;
const COPY_FEEDBACK_TIMEOUT_MS = 1500;

function roleLabel(role: string): string {
  return role === "ADMIN" ? "관리자" : "일반";
}

function statusLabel(status: string): string {
  return status === "ACTIVE" ? "활성" : "비활성";
}

function identityProviderLabel(provider: string | null): string {
  return provider ? provider.toUpperCase() : "UNKNOWN";
}

function identityPrimaryValue(identity: UserIdentitySummary): string {
  return identity.emailMasked ?? identity.providerSubjectMasked ?? "식별 정보 없음";
}

function shortUuid(value: string): string {
  if (value.length <= 14) return value;
  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("ko-KR");
}

function formatDateTime(value: string | null): string {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR", { hour12: false });
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

export default function UserListPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [mutationSuccess, setMutationSuccess] = useState<string | null>(null);

  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");

  const [updatingIds, setUpdatingIds] = useState<Set<string>>(new Set());
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newDisplayName, setNewDisplayName] = useState("");
  const [newRole, setNewRole] = useState<EditableRole>("USER");
  const [newStatus, setNewStatus] = useState<EditableStatus>("ACTIVE");
  const [creating, setCreating] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [copiedUserId, setCopiedUserId] = useState<string | null>(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await listUsers();
      setUsers(data);
      setSelectedUserId((prev) => {
        if (prev && data.some((item) => item.id === prev)) {
          return prev;
        }
        return data.length > 0 ? data[0].id : null;
      });
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "사용자 목록을 불러올 수 없습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  const clearMutationFeedback = () => {
    setMutationError(null);
    setMutationSuccess(null);
  };

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    return users.filter((u) => {
      if (roleFilter !== "all" && u.role !== roleFilter) return false;
      if (statusFilter !== "all" && u.status !== statusFilter) return false;

      if (!query) return true;
      if (u.displayName.toLowerCase().includes(query)) return true;
      if (u.id.toLowerCase().includes(query)) return true;

      const identities = u.identities ?? [];
      return identities.some((identity) =>
        `${identity.provider ?? ""} ${identity.emailMasked ?? ""} ${identity.providerSubjectMasked ?? ""}`
          .toLowerCase()
          .includes(query),
      );
    });
  }, [users, search, roleFilter, statusFilter]);

  const selectedUser = useMemo(() => {
    if (!selectedUserId) return null;
    return users.find((u) => u.id === selectedUserId) ?? null;
  }, [users, selectedUserId]);

  const totalUsers = users.length;
  const activeUsers = useMemo(() => users.filter((u) => u.status === "ACTIVE").length, [users]);
  const disabledUsers = totalUsers - activeUsers;
  const activeAdminCount = useMemo(
    () => users.filter((u) => u.role === "ADMIN" && u.status === "ACTIVE").length,
    [users],
  );
  const linkedIdentityUsers = useMemo(
    () => users.filter((u) => (u.identities ?? []).length > 0).length,
    [users],
  );

  const hasActiveFilter = search.trim().length > 0 || roleFilter !== "all" || statusFilter !== "all";

  const isCurrentOperator = (targetUser: UserResponse): boolean => currentUser?.userId === targetUser.id;

  const isLastActiveAdmin = (targetUser: UserResponse): boolean =>
    targetUser.role === "ADMIN" && targetUser.status === "ACTIVE" && activeAdminCount <= 1;

  const getRoleLockReason = (targetUser: UserResponse): string | null => {
    if (isCurrentOperator(targetUser)) {
      return "현재 로그인한 운영자 계정은 역할 변경이 잠겨 있습니다.";
    }
    if (isLastActiveAdmin(targetUser)) {
      return "마지막 활성 관리자 계정은 일반 사용자로 변경할 수 없습니다.";
    }
    return null;
  };

  const getStatusLockReason = (targetUser: UserResponse): string | null => {
    if (isCurrentOperator(targetUser)) {
      return "현재 로그인한 운영자 계정은 상태 변경이 잠겨 있습니다.";
    }
    if (isLastActiveAdmin(targetUser)) {
      return "마지막 활성 관리자 계정은 비활성화할 수 없습니다.";
    }
    return null;
  };

  const getDeleteLockReason = (targetUser: UserResponse): string | null => {
    if (isCurrentOperator(targetUser)) {
      return "현재 로그인한 운영자 계정은 삭제할 수 없습니다.";
    }
    if (isLastActiveAdmin(targetUser)) {
      return "마지막 활성 관리자 계정은 삭제할 수 없습니다.";
    }
    if (targetUser.status === "DISABLED") {
      return "이미 삭제(비활성) 처리된 계정입니다.";
    }
    return null;
  };

  const clearFilters = () => {
    setSearch("");
    setRoleFilter("all");
    setStatusFilter("all");
  };

  const handleCopyUserId = async (userId: string) => {
    clearMutationFeedback();
    try {
      await copyTextToClipboard(userId);
      setCopiedUserId(userId);
      setMutationSuccess("사용자 UUID를 클립보드에 복사했습니다.");
      window.setTimeout(() => {
        setCopiedUserId((prev) => (prev === userId ? null : prev));
      }, COPY_FEEDBACK_TIMEOUT_MS);
    } catch {
      setMutationError("UUID 복사에 실패했습니다. 수동으로 복사해 주세요.");
    }
  };

  const handleRoleChange = async (targetUser: UserResponse, newRoleValue: string) => {
    if (newRoleValue === targetUser.role) {
      return;
    }

    const lockReason = getRoleLockReason(targetUser);
    if (lockReason) {
      setMutationError(lockReason);
      setMutationSuccess(null);
      return;
    }

    clearMutationFeedback();
    setUpdatingIds((prev) => new Set(prev).add(targetUser.id));
    try {
      const updated = await updateUser(targetUser.id, { role: newRoleValue });
      setUsers((prev) => prev.map((u) => (u.id === targetUser.id ? updated : u)));
      setMutationSuccess(`"${updated.displayName}" 사용자의 역할을 ${roleLabel(updated.role)}로 변경했습니다.`);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "역할 변경에 실패했습니다.");
    } finally {
      setUpdatingIds((prev) => {
        const next = new Set(prev);
        next.delete(targetUser.id);
        return next;
      });
    }
  };

  const handleStatusToggle = async (targetUser: UserResponse) => {
    const lockReason = getStatusLockReason(targetUser);
    if (lockReason) {
      setMutationError(lockReason);
      setMutationSuccess(null);
      return;
    }

    const newStatusValue = targetUser.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    clearMutationFeedback();
    setUpdatingIds((prev) => new Set(prev).add(targetUser.id));
    try {
      const updated = await updateUser(targetUser.id, { status: newStatusValue });
      setUsers((prev) => prev.map((u) => (u.id === targetUser.id ? updated : u)));
      setMutationSuccess(`"${updated.displayName}" 사용자를 ${statusLabel(updated.status)} 상태로 변경했습니다.`);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setUpdatingIds((prev) => {
        const next = new Set(prev);
        next.delete(targetUser.id);
        return next;
      });
    }
  };

  const handleDeleteUser = async (targetUser: UserResponse) => {
    const lockReason = getDeleteLockReason(targetUser);
    if (lockReason) {
      setMutationError(lockReason);
      setMutationSuccess(null);
      return;
    }

    const confirmed = window.confirm(`"${targetUser.displayName}" 사용자를 삭제(비활성) 처리하시겠습니까?`);
    if (!confirmed) return;

    clearMutationFeedback();
    setDeletingIds((prev) => new Set(prev).add(targetUser.id));
    try {
      const deleted = await deleteUser(targetUser.id);
      setUsers((prev) => prev.map((u) => (u.id === targetUser.id ? deleted : u)));
      setMutationSuccess(`"${deleted.displayName}" 사용자를 비활성 처리했습니다.`);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "삭제에 실패했습니다.");
    } finally {
      setDeletingIds((prev) => {
        const next = new Set(prev);
        next.delete(targetUser.id);
        return next;
      });
    }
  };

  const resetCreateForm = () => {
    setNewDisplayName("");
    setNewRole("USER");
    setNewStatus("ACTIVE");
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    const normalizedDisplayName = newDisplayName.trim();
    if (!normalizedDisplayName) {
      setMutationError("이름을 입력해 주세요.");
      setMutationSuccess(null);
      return;
    }
    if (normalizedDisplayName.length > DISPLAY_NAME_MAX_LENGTH) {
      setMutationError(`이름은 최대 ${DISPLAY_NAME_MAX_LENGTH}자까지 입력할 수 있습니다.`);
      setMutationSuccess(null);
      return;
    }

    clearMutationFeedback();
    setCreating(true);
    try {
      const created = await createUser({
        displayName: normalizedDisplayName,
        role: newRole,
        status: newStatus,
      });
      setUsers((prev) => [created, ...prev]);
      setSelectedUserId(created.id);
      setMutationSuccess(`"${created.displayName}" 사용자를 생성했습니다.`);
      resetCreateForm();
      setShowCreateForm(false);
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "사용자 생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-slate-900">사용자 관리</h2>
            <p className="mt-1 text-sm text-slate-600">이름/UUID/연동 식별정보로 사용자를 찾고 권한/상태를 관리합니다.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadUsers()}
              disabled={loading}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50"
            >
              {loading ? "새로고침 중..." : "목록 새로고침"}
            </button>
            <button
              type="button"
              onClick={() => {
                if (showCreateForm) {
                  resetCreateForm();
                }
                setShowCreateForm((prev) => !prev);
              }}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
            >
              {showCreateForm ? "생성 취소" : "새 사용자 생성"}
            </button>
          </div>
        </div>

        <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          현재 로그인한 운영자 계정과 마지막 활성 관리자 계정은 실수 방지를 위해 수정/삭제가 잠깁니다. 삭제는 soft-delete로
          `DISABLED` 처리됩니다.
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <InsightCard title="전체 사용자" value={totalUsers} tone="slate" badge="USR" description="가입된 계정" loading={loading} />
          <InsightCard
            title="활성 사용자"
            value={activeUsers}
            tone="emerald"
            badge="ON"
            description="접근 가능한 상태"
            ratio={totalUsers > 0 ? activeUsers / totalUsers : 0}
            loading={loading}
          />
          <InsightCard
            title="비활성 사용자"
            value={disabledUsers}
            tone="amber"
            badge="OFF"
            description="차단 또는 soft-delete"
            ratio={totalUsers > 0 ? disabledUsers / totalUsers : 0}
            loading={loading}
          />
          <InsightCard
            title="활성 관리자"
            value={activeAdminCount}
            tone="indigo"
            badge="ADM"
            description="ADMIN + ACTIVE"
            ratio={totalUsers > 0 ? activeAdminCount / totalUsers : 0}
            loading={loading}
          />
          <InsightCard
            title="연동 계정"
            value={linkedIdentityUsers}
            tone="sky"
            badge="ID"
            description="소셜 로그인 연결"
            ratio={totalUsers > 0 ? linkedIdentityUsers / totalUsers : 0}
            loading={loading}
          />
        </div>

        <div className="mt-4 flex flex-col gap-3 md:flex-row">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="이름/UUID/연동정보 검색..."
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value as RoleFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">전체 역할</option>
            <option value="ADMIN">관리자</option>
            <option value="USER">일반</option>
          </select>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">전체 상태</option>
            <option value="ACTIVE">활성</option>
            <option value="DISABLED">비활성</option>
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

        {showCreateForm && (
          <form onSubmit={handleCreateUser} className="mt-4 grid grid-cols-1 gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3 md:grid-cols-4">
            <div className="md:col-span-2">
              <input
                type="text"
                value={newDisplayName}
                onChange={(e) => setNewDisplayName(e.target.value)}
                placeholder="사용자 이름"
                maxLength={DISPLAY_NAME_MAX_LENGTH}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
              <div className="mt-1 text-right text-xs text-slate-500">
                {newDisplayName.trim().length}/{DISPLAY_NAME_MAX_LENGTH}
              </div>
            </div>
            <select
              value={newRole}
              onChange={(e) => setNewRole(e.target.value as EditableRole)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="USER">일반</option>
              <option value="ADMIN">관리자</option>
            </select>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value as EditableStatus)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ACTIVE">활성</option>
              <option value="DISABLED">비활성</option>
            </select>
            <div className="flex items-center gap-2 md:col-span-4">
              <button
                type="submit"
                disabled={creating || !newDisplayName.trim()}
                className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
              >
                {creating ? "생성 중..." : "사용자 생성"}
              </button>
              <button
                type="button"
                onClick={() => {
                  resetCreateForm();
                  setShowCreateForm(false);
                }}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100"
              >
                취소
              </button>
            </div>
          </form>
        )}
      </section>

      {loading && <p className="text-sm text-gray-500">로딩 중...</p>}
      {loadError && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <span>{loadError}</span>
          <button
            type="button"
            onClick={() => void loadUsers()}
            className="rounded border border-red-300 px-2.5 py-1 text-xs font-semibold text-red-700 hover:bg-red-100"
          >
            다시 시도
          </button>
        </div>
      )}
      {mutationSuccess && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{mutationSuccess}</div>
      )}
      {mutationError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{mutationError}</div>
      )}

      {!loading && !loadError && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-[1080px] w-full text-left">
              <thead className="border-b border-slate-200 bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">사용자</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">연동 식별정보</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">역할</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">최근 로그인</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">작업</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-10 text-center text-gray-400">
                      {hasActiveFilter ? (
                        "검색 결과가 없습니다."
                      ) : (
                        <div className="space-y-2">
                          <div>등록된 사용자가 없습니다.</div>
                          <button
                            type="button"
                            onClick={() => setShowCreateForm(true)}
                            className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-100"
                          >
                            첫 사용자 생성하기
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                )}
                {filteredUsers.map((user) => {
                  const roleLockReason = getRoleLockReason(user);
                  const statusLockReason = getStatusLockReason(user);
                  const deleteLockReason = getDeleteLockReason(user);
                  const lockHints = [...new Set([roleLockReason, statusLockReason, deleteLockReason].filter(Boolean))] as string[];
                  const inProgress = updatingIds.has(user.id) || deletingIds.has(user.id);
                  const identities = user.identities ?? [];
                  const isSelected = selectedUserId === user.id;

                  return (
                    <tr key={user.id} className={`border-b border-slate-100 last:border-b-0 ${isSelected ? "bg-indigo-50/40" : "hover:bg-slate-50"}`}>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap items-center gap-2 font-medium">
                          <span>{user.displayName}</span>
                          {isCurrentOperator(user) && (
                            <span className="rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-semibold text-indigo-700">
                              현재 운영자
                            </span>
                          )}
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                          <span className="font-mono" title={user.id}>
                            {shortUuid(user.id)}
                          </span>
                          <button
                            type="button"
                            onClick={() => void handleCopyUserId(user.id)}
                            className="rounded border border-slate-300 px-1.5 py-0.5 text-[11px] font-semibold text-slate-600 hover:bg-slate-100"
                          >
                            {copiedUserId === user.id ? "복사됨" : "UUID 복사"}
                          </button>
                          <span>가입: {formatDate(user.createdAt)}</span>
                        </div>
                        {lockHints.length > 0 && (
                          <div className="mt-1 text-xs text-amber-700">{lockHints.join(" · ")}</div>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {identities.length === 0 ? (
                          <span className="text-xs text-slate-400">연동 정보 없음</span>
                        ) : (
                          <div className="space-y-1">
                            {identities.slice(0, 2).map((identity, index) => (
                              <div
                                key={`${user.id}-${identity.createdAt}-${index}`}
                                className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-1"
                              >
                                <div className="text-[11px] font-semibold text-slate-600">{identityProviderLabel(identity.provider)}</div>
                                <div className="mt-0.5 font-mono text-[11px] text-slate-700">{identityPrimaryValue(identity)}</div>
                              </div>
                            ))}
                            {identities.length > 2 && (
                              <div className="text-[11px] text-slate-500">+{identities.length - 2}개 더 있음 (상세에서 확인)</div>
                            )}
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <select
                          value={user.role}
                          onChange={(e) => void handleRoleChange(user, e.target.value)}
                          disabled={inProgress || roleLockReason !== null}
                          title={roleLockReason ?? ""}
                          className="rounded border border-gray-300 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50"
                        >
                          <option value="USER">일반</option>
                          <option value="ADMIN">관리자</option>
                        </select>
                      </td>
                      <td className="px-4 py-3">
                        <button
                          type="button"
                          onClick={() => void handleStatusToggle(user)}
                          disabled={inProgress || statusLockReason !== null}
                          title={statusLockReason ?? ""}
                          className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold transition-colors disabled:opacity-50 ${
                            user.status === "ACTIVE"
                              ? "bg-green-100 text-green-700 hover:bg-green-200"
                              : "bg-red-100 text-red-700 hover:bg-red-200"
                          }`}
                        >
                          {inProgress ? "..." : statusLabel(user.status)}
                        </button>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">{formatDateTime(user.lastLoginAt)}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => setSelectedUserId(user.id)}
                            className={`text-sm font-semibold ${isSelected ? "text-indigo-700" : "text-indigo-600 hover:text-indigo-800"}`}
                          >
                            상세
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleDeleteUser(user)}
                            disabled={inProgress || deleteLockReason !== null}
                            title={deleteLockReason ?? ""}
                            className="text-sm font-semibold text-red-600 hover:text-red-800 disabled:opacity-40"
                          >
                            {deletingIds.has(user.id) ? "삭제 중..." : "삭제"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && !loadError && selectedUser && (
        <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">사용자 상세</h3>
              <p className="mt-1 text-sm text-slate-600">선택한 사용자의 식별 정보와 로그인 연동 상태를 확인합니다.</p>
            </div>
            <button
              type="button"
              onClick={() => void handleCopyUserId(selectedUser.id)}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-100"
            >
              {copiedUserId === selectedUser.id ? "UUID 복사됨" : "UUID 복사"}
            </button>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">이름</div>
              <div className="mt-1 text-sm font-semibold text-slate-900">{selectedUser.displayName}</div>
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">UUID</div>
              <div className="mt-1 break-all font-mono text-xs text-slate-700">{selectedUser.id}</div>
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">권한/상태</div>
              <div className="mt-1 text-sm font-semibold text-slate-900">
                {roleLabel(selectedUser.role)} / {statusLabel(selectedUser.status)}
              </div>
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">가입일 / 최근 로그인</div>
              <div className="mt-1 text-sm text-slate-700">
                {formatDateTime(selectedUser.createdAt)} / {formatDateTime(selectedUser.lastLoginAt)}
              </div>
            </div>
          </div>

          <div className="mt-4">
            <div className="mb-2 flex items-center justify-between">
              <h4 className="text-sm font-semibold text-slate-900">연동 식별 정보</h4>
              <span className="text-xs text-slate-500">{(selectedUser.identities ?? []).length}개 연결됨</span>
            </div>

            {(selectedUser.identities ?? []).length === 0 ? (
              <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-3 py-4 text-xs text-slate-500">
                연결된 소셜 로그인 정보가 없습니다.
              </div>
            ) : (
              <div className="space-y-2">
                {(selectedUser.identities ?? []).map((identity, index) => (
                  <div key={`${selectedUser.id}-${identity.createdAt}-${index}`} className="rounded-lg border border-slate-200 px-3 py-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-700">
                        {identityProviderLabel(identity.provider)}
                      </span>
                      <span className="text-xs text-slate-500">연동일: {formatDateTime(identity.createdAt)}</span>
                    </div>
                    <div className="mt-2 grid grid-cols-1 gap-2 md:grid-cols-2">
                      <div className="rounded border border-slate-200 bg-slate-50 px-2 py-1">
                        <div className="text-[11px] text-slate-500">마스킹 이메일</div>
                        <div className="mt-0.5 font-mono text-xs text-slate-700">{identity.emailMasked ?? "-"}</div>
                      </div>
                      <div className="rounded border border-slate-200 bg-slate-50 px-2 py-1">
                        <div className="text-[11px] text-slate-500">마스킹 providerSubject</div>
                        <div className="mt-0.5 font-mono text-xs text-slate-700">{identity.providerSubjectMasked ?? "-"}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {!loading && !loadError && (
        <p className="text-sm text-gray-500">
          {filteredUsers.length}명 표시 / 전체 {users.length}명
        </p>
      )}
    </div>
  );
}
