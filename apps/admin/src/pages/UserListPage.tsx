import { useEffect, useState } from "react";
import { listUsers, updateUser, type UserInfo } from "../api/users";

export default function UserListPage() {
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    listUsers()
      .then(setUsers)
      .catch((err) =>
        setError(err instanceof Error ? err.message : "목록을 불러올 수 없습니다.")
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleRoleChange = async (id: string, role: string) => {
    setError(null);
    try {
      await updateUser(id, { role });
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "역할 변경에 실패했습니다.");
    }
  };

  const handleStatusToggle = async (id: string, currentStatus: string) => {
    const newStatus = currentStatus === "ACTIVE" ? "DISABLED" : "ACTIVE";
    setError(null);
    try {
      await updateUser(id, { status: newStatus });
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "상태 변경에 실패했습니다.");
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">사용자 관리</h1>

      {error && <p className="text-red-600 mb-4">{error}</p>}
      {loading && <p className="text-gray-500">로딩 중...</p>}

      {!loading && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full text-left">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">이름</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">역할</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">가입일</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600">최근 로그인</th>
                <th className="px-4 py-3 text-sm font-semibold text-gray-600"></th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                    등록된 사용자가 없습니다.
                  </td>
                </tr>
              )}
              {users.map((u) => (
                <tr key={u.id} className="border-b last:border-b-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{u.displayName}</td>
                  <td className="px-4 py-3">
                    <select
                      value={u.role}
                      onChange={(e) => handleRoleChange(u.id, e.target.value)}
                      className="border border-gray-300 rounded px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                        u.status === "ACTIVE"
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {u.status === "ACTIVE" ? "활성" : "비활성"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(u.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString("ko-KR") : "-"}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => handleStatusToggle(u.id, u.status)}
                      className={`text-sm hover:underline ${
                        u.status === "ACTIVE" ? "text-red-600" : "text-green-600"
                      }`}
                    >
                      {u.status === "ACTIVE" ? "비활성화" : "활성화"}
                    </button>
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
