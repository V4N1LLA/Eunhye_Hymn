import { useEffect, useMemo, useState } from "react";
import { createUser, deleteUser, listUsers, updateUser, type UserResponse } from "../api/adminUsers";
import { useAuth } from "../auth/AuthContext";

type RoleFilter = "all" | "ADMIN" | "USER";
type StatusFilter = "all" | "ACTIVE" | "DISABLED";
type EditableRole = "ADMIN" | "USER";
type EditableStatus = "ACTIVE" | "DISABLED";

export default function UserListPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);

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

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listUsers()
      .then((data) => {
        if (!cancelled) setUsers(data);
      })
      .catch((err) => {
        if (!cancelled) setLoadError(err instanceof Error ? err.message : "사용자 목록을 불러올 수 없습니다.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    return users.filter((u) => {
      if (roleFilter !== "all" && u.role !== roleFilter) return false;
      if (statusFilter !== "all" && u.status !== statusFilter) return false;
      if (query && !u.displayName.toLowerCase().includes(query)) return false;
      return true;
    });
  }, [users, search, roleFilter, statusFilter]);

  const activeAdminCount = useMemo(
    () => users.filter((u) => u.role === "ADMIN" && u.status === "ACTIVE").length,
    [users],
  );

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

  const handleRoleChange = async (user: UserResponse, newRoleValue: string) => {
    if (newRoleValue === user.role) {
      return;
    }

    const lockReason = getRoleLockReason(user);
    if (lockReason) {
      setMutationError(lockReason);
      return;
    }

    setMutationError(null);
    setUpdatingIds((prev) => new Set(prev).add(user.id));
    try {
      const updated = await updateUser(user.id, { role: newRoleValue });
      setUsers((prev) => prev.map((u) => (u.id === user.id ? updated : u)));
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "역할 변경에 실패했습니다.");
    } finally {
      setUpdatingIds((prev) => {
        const next = new Set(prev);
        next.delete(user.id);
        return next;
      });
    }
  };

  const handleStatusToggle = async (user: UserResponse) => {
    const lockReason = getStatusLockReason(user);
    if (lockReason) {
      setMutationError(lockReason);
      return;
    }

    const newStatusValue = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    setMutationError(null);
    setUpdatingIds((prev) => new Set(prev).add(user.id));
    try {
      const updated = await updateUser(user.id, { status: newStatusValue });
      setUsers((prev) => prev.map((u) => (u.id === user.id ? updated : u)));
    } catch (err) {
      setMutationError(err instanceof Error ? err.message : "상태 변경에 실패했습니다.");
    } finally {
      setUpdatingIds((prev) => {
        const next = new Set(prev);
        next.delete(user.id);
        return next;
      });
    }
  };

  const handleDeleteUser = async (targetUser: UserResponse) => {
    const lockReason = getDeleteLockReason(targetUser);
    if (lockReason) {
      setMutationError(lockReason);
      return;
    }

    const confirmed = window.confirm(`"${targetUser.displayName}" 사용자를 삭제(비활성) 처리하시겠습니까?`);
    if (!confirmed) return;

    setMutationError(null);
    setDeletingIds((prev) => new Set(prev).add(targetUser.id));
    try {
      const deleted = await deleteUser(targetUser.id);
      setUsers((prev) => prev.map((u) => (u.id === targetUser.id ? deleted : u)));
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
    if (!newDisplayName.trim()) {
      setMutationError("이름을 입력해 주세요.");
      return;
    }

    setMutationError(null);
    setCreating(true);
    try {
      const created = await createUser({
        displayName: newDisplayName.trim(),
        role: newRole,
        status: newStatus,
      });
      setUsers((prev) => [created, ...prev]);
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
            <p className="mt-1 text-sm text-slate-600">생성/조회/수정/삭제(비활성) 작업을 수행합니다.</p>
          </div>
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

        <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          현재 로그인한 운영자 계정과 마지막 활성 관리자 계정은 실수 방지를 위해 수정/삭제가 잠깁니다. 삭제는 soft-delete로
          `DISABLED` 처리됩니다.
        </div>

        <div className="mt-4 flex flex-col gap-3 md:flex-row">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="이름 검색..."
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
        </div>

        {showCreateForm && (
          <form onSubmit={handleCreateUser} className="mt-4 grid grid-cols-1 gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3 md:grid-cols-4">
            <input
              type="text"
              value={newDisplayName}
              onChange={(e) => setNewDisplayName(e.target.value)}
              placeholder="사용자 이름"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 md:col-span-2"
              required
            />
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
      {loadError && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{loadError}</div>}
      {mutationError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{mutationError}</div>
      )}

      {!loading && !loadError && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-left">
            <thead className="border-b border-slate-200 bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">이름</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">역할</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">가입일</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">최근 로그인</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">작업</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-10 text-center text-gray-400">
                    {search || roleFilter !== "all" || statusFilter !== "all"
                      ? "검색 결과가 없습니다."
                      : "등록된 사용자가 없습니다."}
                  </td>
                </tr>
              )}
              {filteredUsers.map((u) => {
                const roleLockReason = getRoleLockReason(u);
                const statusLockReason = getStatusLockReason(u);
                const deleteLockReason = getDeleteLockReason(u);
                const inProgress = updatingIds.has(u.id) || deletingIds.has(u.id);

                return (
                  <tr key={u.id} className="border-b border-slate-100 last:border-b-0 hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium">
                      <div className="flex flex-wrap items-center gap-2">
                        <span>{u.displayName}</span>
                        {isCurrentOperator(u) && (
                          <span className="rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-semibold text-indigo-700">
                            현재 운영자
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <select
                        value={u.role}
                        onChange={(e) => handleRoleChange(u, e.target.value)}
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
                        onClick={() => handleStatusToggle(u)}
                        disabled={inProgress || statusLockReason !== null}
                        title={statusLockReason ?? ""}
                        className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold transition-colors disabled:opacity-50 ${
                          u.status === "ACTIVE"
                            ? "bg-green-100 text-green-700 hover:bg-green-200"
                            : "bg-red-100 text-red-700 hover:bg-red-200"
                        }`}
                      >
                        {inProgress ? "..." : u.status === "ACTIVE" ? "활성" : "비활성"}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">{new Date(u.createdAt).toLocaleDateString("ko-KR")}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString("ko-KR") : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => handleDeleteUser(u)}
                        disabled={inProgress || deleteLockReason !== null}
                        title={deleteLockReason ?? ""}
                        className="text-sm font-semibold text-red-600 hover:text-red-800 disabled:opacity-40"
                      >
                        {deletingIds.has(u.id) ? "삭제 중..." : "삭제"}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </section>
      )}

      {!loading && !loadError && (search || roleFilter !== "all" || statusFilter !== "all") && (
        <p className="text-sm text-gray-500">{filteredUsers.length}명 / 전체 {users.length}명</p>
      )}
    </div>
  );
}
