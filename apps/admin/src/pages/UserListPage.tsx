import { useEffect, useMemo, useState } from "react";
import { listUsers, updateUser, type UserResponse } from "../api/adminUsers";
import { useAuth } from "../auth/AuthContext";

type RoleFilter = "all" | "ADMIN" | "USER";
type StatusFilter = "all" | "ACTIVE" | "DISABLED";

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

  const handleRoleChange = async (user: UserResponse, newRole: string) => {
    if (newRole === user.role) {
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
      const updated = await updateUser(user.id, { role: newRole });
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

    const newStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    setMutationError(null);
    setUpdatingIds((prev) => new Set(prev).add(user.id));
    try {
      const updated = await updateUser(user.id, { status: newStatus });
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

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900">사용자 관리</h2>
        <p className="mt-1 text-sm text-slate-600">
          단일 운영자 환경에서는 관리자 계정을 최소 1개 유지해야 합니다.
        </p>
        <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          현재 로그인한 운영자 계정과 마지막 활성 관리자 계정은 실수 방지를 위해 역할/상태 변경이 잠깁니다.
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
      </section>

      {loading && <p className="text-sm text-gray-500">로딩 중...</p>}
      {loadError && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{loadError}</div>}
      {mutationError && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{mutationError}</div>
      )}

      {!loading && !loadError && (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-left">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">이름</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">역할</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">가입일</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">최근 로그인</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-10 text-center text-gray-400">
                    {search || roleFilter !== "all" || statusFilter !== "all"
                      ? "검색 결과가 없습니다."
                      : "등록된 사용자가 없습니다."}
                  </td>
                </tr>
              )}
              {filteredUsers.map((u) => {
                const roleLockReason = getRoleLockReason(u);
                const statusLockReason = getStatusLockReason(u);

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
                        disabled={updatingIds.has(u.id) || roleLockReason !== null}
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
                        disabled={updatingIds.has(u.id) || statusLockReason !== null}
                        title={statusLockReason ?? ""}
                        className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold transition-colors disabled:opacity-50 ${
                          u.status === "ACTIVE"
                            ? "bg-green-100 text-green-700 hover:bg-green-200"
                            : "bg-red-100 text-red-700 hover:bg-red-200"
                        }`}
                      >
                        {updatingIds.has(u.id) ? "..." : u.status === "ACTIVE" ? "활성" : "비활성"}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">{new Date(u.createdAt).toLocaleDateString("ko-KR")}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString("ko-KR") : "-"}
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
