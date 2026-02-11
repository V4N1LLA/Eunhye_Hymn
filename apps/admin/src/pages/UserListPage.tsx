import { useEffect, useMemo, useState } from "react";
import { listUsers, updateUser, type UserResponse } from "../api/adminUsers";

type RoleFilter = "all" | "ADMIN" | "USER";
type StatusFilter = "all" | "ACTIVE" | "DISABLED";

export default function UserListPage() {
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

  const handleRoleChange = async (user: UserResponse, newRole: string) => {
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
    const newStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
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
    <div>
      <h1 className="text-2xl font-bold mb-6">사용자 관리</h1>

      {/* Search & Filter Bar */}
      <div className="flex items-center gap-3 mb-4">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="이름 검색..."
          className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value as RoleFilter)}
          className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          <option value="all">전체 역할</option>
          <option value="ADMIN">관리자</option>
          <option value="USER">일반</option>
        </select>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
          className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          <option value="all">전체 상태</option>
          <option value="ACTIVE">활성</option>
          <option value="DISABLED">비활성</option>
        </select>
      </div>

      {loading && <p className="text-gray-500">로딩 중...</p>}
      {loadError && <p className="text-red-600 mb-4">{loadError}</p>}
      {mutationError && <p className="text-red-600 mb-4">{mutationError}</p>}

      {!loading && !loadError && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b">
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
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    {search || roleFilter !== "all" || statusFilter !== "all"
                      ? "검색 결과가 없습니다."
                      : "등록된 사용자가 없습니다."}
                  </td>
                </tr>
              )}
              {filteredUsers.map((u) => (
                <tr key={u.id} className="border-b last:border-b-0 hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">{u.displayName}</td>
                  <td className="px-4 py-3">
                    <select
                      value={u.role}
                      onChange={(e) => handleRoleChange(u, e.target.value)}
                      disabled={updatingIds.has(u.id)}
                      className="border border-gray-300 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50"
                    >
                      <option value="USER">일반</option>
                      <option value="ADMIN">관리자</option>
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => handleStatusToggle(u)}
                      disabled={updatingIds.has(u.id)}
                      className={`inline-block px-2 py-0.5 rounded text-xs font-medium cursor-pointer transition-colors disabled:opacity-50 ${
                        u.status === "ACTIVE"
                          ? "bg-green-100 text-green-700 hover:bg-green-200"
                          : "bg-red-100 text-red-700 hover:bg-red-200"
                      }`}
                    >
                      {updatingIds.has(u.id) ? "..." : u.status === "ACTIVE" ? "활성" : "비활성"}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(u.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString("ko-KR") : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && !loadError && (search || roleFilter !== "all" || statusFilter !== "all") && (
        <p className="mt-3 text-sm text-gray-500">
          {filteredUsers.length}명 / 전체 {users.length}명
        </p>
      )}
    </div>
  );
}
