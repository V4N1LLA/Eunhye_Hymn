import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { createUser, deleteUser, listUsers, updateUser, type UpdateUserRequest, type UserResponse } from "../api/adminUsers";
import { useAuth } from "../auth/AuthContext";

type RoleFilter = "all" | "ADMIN" | "USER";
type StatusFilter = "all" | "ACTIVE" | "DISABLED";
type EditableRole = "ADMIN" | "USER";
type EditableStatus = "ACTIVE" | "DISABLED";
type EditableGender = "MALE" | "FEMALE" | "UNKNOWN";

type UserDraft = {
  displayName: string;
  role: EditableRole;
  status: EditableStatus;
  churchName: string;
  name: string;
  group: string;
  gender: EditableGender;
  phoneNumber: string;
};

function normalizeText(value: string): string {
  return value.trim();
}

function normalizePhone(value: string | null | undefined): string {
  return (value ?? "").replace(/\D/g, "");
}

function formatDateTime(value: string | null): string {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR", { hour12: false });
}

function formatPhone(value: string | null | undefined): string {
  const digits = normalizePhone(value);
  if (!digits) return "-";
  if (digits.length === 10) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`;
  if (digits.length === 11) return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  return digits;
}

function roleLabel(role: string | null | undefined): string {
  switch ((role ?? "").trim().toUpperCase()) {
    case "ADMIN":
      return "관리자";
    case "USER":
      return "일반 사용자";
    default:
      return "미확인";
  }
}

function statusLabel(status: string | null | undefined): string {
  switch ((status ?? "").trim().toUpperCase()) {
    case "ACTIVE":
      return "활성";
    case "DISABLED":
      return "비활성";
    default:
      return "미확인";
  }
}

function genderLabel(gender: string | null | undefined): string {
  switch ((gender ?? "").trim().toUpperCase()) {
    case "MALE":
      return "남성";
    case "FEMALE":
      return "여성";
    default:
      return "미상";
  }
}

function toDraft(user: UserResponse): UserDraft {
  return {
    displayName: user.displayName,
    role: user.role === "ADMIN" ? "ADMIN" : "USER",
    status: user.status === "DISABLED" ? "DISABLED" : "ACTIVE",
    churchName: user.profile?.churchName ?? "",
    name: user.profile?.name ?? "",
    group: user.profile?.group ?? "",
    gender: user.profile?.gender === "MALE" || user.profile?.gender === "FEMALE" ? user.profile.gender : "UNKNOWN",
    phoneNumber: normalizePhone(user.verification?.phoneNumber),
  };
}

function buildPayload(user: UserResponse, draft: UserDraft): { payload: UpdateUserRequest | null; error: string | null } {
  const payload: UpdateUserRequest = {};

  const displayName = normalizeText(draft.displayName);
  if (!displayName) return { payload: null, error: "표시 이름을 입력해 주세요." };
  if (displayName.length > 64) return { payload: null, error: "표시 이름은 64자 이하로 입력해 주세요." };
  if (displayName !== user.displayName) {
    payload.displayName = displayName;
  }

  if (draft.role !== user.role) payload.role = draft.role;
  if (draft.status !== user.status) payload.status = draft.status;

  const nextChurch = normalizeText(draft.churchName);
  const nextName = normalizeText(draft.name);
  const nextGroup = normalizeText(draft.group);
  const nextGender = draft.gender;
  const currentChurch = user.profile?.churchName ?? "";
  const currentName = user.profile?.name ?? "";
  const currentGroup = user.profile?.group ?? "";
  const currentGender = user.profile?.gender ?? "UNKNOWN";
  const profileChanged =
    nextChurch !== currentChurch ||
    nextName !== currentName ||
    nextGroup !== currentGroup ||
    nextGender !== currentGender;
  if (profileChanged) {
    if (!nextChurch || !nextName || !nextGroup) {
      return { payload: null, error: "프로필 변경 시 교회/이름/구역은 필수입니다." };
    }
    payload.churchName = nextChurch;
    payload.name = nextName;
    payload.group = nextGroup;
    payload.gender = nextGender;
  }

  const nextPhone = normalizePhone(draft.phoneNumber);
  const currentPhone = normalizePhone(user.verification?.phoneNumber);
  if (nextPhone !== currentPhone) {
    if (nextPhone && (nextPhone.length < 10 || nextPhone.length > 11)) {
      return { payload: null, error: "전화번호는 숫자 10~11자리여야 합니다." };
    }
    payload.phoneNumber = nextPhone;
  }

  if (Object.keys(payload).length === 0) return { payload: null, error: null };
  return { payload, error: null };
}

function primaryContact(user: UserResponse): string {
  const phone = normalizePhone(user.verification?.phoneNumber);
  if (phone) return formatPhone(phone);
  if (user.primaryEmail) return user.primaryEmail;
  return "-";
}

export default function UserListPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [churchFilter, setChurchFilter] = useState("all");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [draft, setDraft] = useState<UserDraft | null>(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [newDisplayName, setNewDisplayName] = useState("");
  const [newRole, setNewRole] = useState<EditableRole>("USER");
  const [newStatus, setNewStatus] = useState<EditableStatus>("ACTIVE");

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listUsers();
      setUsers(data);
      setSelectedUserId((prev) => (prev && data.some((item) => item.id === prev) ? prev : data[0]?.id ?? null));
    } catch (err) {
      setError(err instanceof Error ? err.message : "사용자 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  const churchOptions = useMemo(() => {
    const items = users
      .map((user) => (user.profile?.churchName ?? "").trim())
      .filter((value) => value.length > 0);
    return Array.from(new Set(items)).sort((a, b) => a.localeCompare(b, "ko-KR"));
  }, [users]);

  const filteredUsers = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return users.filter((user) => {
      if (churchFilter !== "all" && (user.profile?.churchName ?? "") !== churchFilter) return false;
      if (roleFilter !== "all" && user.role !== roleFilter) return false;
      if (statusFilter !== "all" && user.status !== statusFilter) return false;
      if (!keyword) return true;
      return [
        user.displayName,
        user.id,
        user.primaryEmail ?? "",
        user.verification?.phoneNumber ?? "",
        user.profile?.churchName ?? "",
        user.profile?.name ?? "",
        user.profile?.group ?? "",
      ]
        .join(" ")
        .toLowerCase()
        .includes(keyword);
    });
  }, [users, search, churchFilter, roleFilter, statusFilter]);

  const selectedUser = useMemo(
    () => (selectedUserId ? users.find((user) => user.id === selectedUserId) ?? null : null),
    [users, selectedUserId],
  );

  useEffect(() => {
    if (!selectedUser) {
      setDraft(null);
      return;
    }
    setDraft(toDraft(selectedUser));
  }, [selectedUser]);

  const activeAdminCount = useMemo(
    () => users.filter((item) => item.role === "ADMIN" && item.status === "ACTIVE").length,
    [users],
  );

  const handleSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedUser || !draft) return;
    setError(null);
    setMessage(null);

    if (currentUser?.userId === selectedUser.id && draft.role !== selectedUser.role) {
      setError("본인 계정의 역할은 변경할 수 없습니다.");
      return;
    }
    if (currentUser?.userId === selectedUser.id && draft.status !== selectedUser.status) {
      setError("본인 계정의 상태는 변경할 수 없습니다.");
      return;
    }
    const wouldLoseAdmin =
      selectedUser.role === "ADMIN" &&
      selectedUser.status === "ACTIVE" &&
      (draft.role !== "ADMIN" || draft.status !== "ACTIVE");
    if (wouldLoseAdmin && activeAdminCount <= 1) {
      setError("마지막 활성 관리자 계정은 강등/비활성화할 수 없습니다.");
      return;
    }

    const { payload, error: payloadError } = buildPayload(selectedUser, draft);
    if (payloadError) {
      setError(payloadError);
      return;
    }
    if (!payload) {
      setMessage("변경된 항목이 없습니다.");
      return;
    }

    setSaving(true);
    try {
      const updated = await updateUser(selectedUser.id, payload);
      setUsers((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
      setDraft(toDraft(updated));
      setMessage("사용자 정보를 저장했습니다.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "사용자 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleDisable = async (targetUser: UserResponse) => {
    if (currentUser?.userId === targetUser.id) {
      setError("본인 계정은 비활성화할 수 없습니다.");
      return;
    }
    if (targetUser.role === "ADMIN" && targetUser.status === "ACTIVE" && activeAdminCount <= 1) {
      setError("마지막 활성 관리자 계정은 비활성화할 수 없습니다.");
      return;
    }
    if (targetUser.status === "DISABLED") {
      setError("이미 비활성화된 사용자입니다.");
      return;
    }

    setDeletingId(targetUser.id);
    setError(null);
    setMessage(null);
    try {
      const deleted = await deleteUser(targetUser.id);
      setUsers((prev) => prev.map((item) => (item.id === deleted.id ? deleted : item)));
      setMessage(`${deleted.displayName} 계정을 비활성화했습니다.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "비활성화 처리에 실패했습니다.");
    } finally {
      setDeletingId(null);
    }
  };

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const displayName = normalizeText(newDisplayName);
    if (!displayName) {
      setError("새 사용자 표시 이름을 입력해 주세요.");
      return;
    }
    if (displayName.length > 64) {
      setError("표시 이름은 64자 이하로 입력해 주세요.");
      return;
    }

    setCreating(true);
    setError(null);
    setMessage(null);
    try {
      const created = await createUser({
        displayName,
        role: newRole,
        status: newStatus,
      });
      setUsers((prev) => [created, ...prev]);
      setSelectedUserId(created.id);
      setNewDisplayName("");
      setNewRole("USER");
      setNewStatus("ACTIVE");
      setMessage(`${created.displayName} 계정을 생성했습니다.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "사용자 생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-card p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-600">사용자 콘솔</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-900">사용자 관리</h2>
            <p className="mt-1 text-sm text-slate-500">교회별 조회와 전화번호 중심 식별로 사용자 정보를 관리합니다.</p>
          </div>
          <button
            type="button"
            onClick={() => void loadUsers()}
            className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            새로고침
          </button>
        </div>

        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">전체 사용자</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">{users.length.toLocaleString("ko-KR")}</div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">활성 사용자</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">
              {users.filter((item) => item.status === "ACTIVE").length.toLocaleString("ko-KR")}
            </div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">활성 관리자</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">{activeAdminCount.toLocaleString("ko-KR")}</div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">전화번호 인증</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">
              {users.filter((item) => normalizePhone(item.verification?.phoneNumber).length > 0).length.toLocaleString("ko-KR")}
            </div>
          </div>
        </div>

        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="이름/이메일/전화번호/UUID 검색"
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 md:col-span-2"
          />
          <select
            value={churchFilter}
            onChange={(event) => setChurchFilter(event.target.value)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">전체 교회</option>
            {churchOptions.map((churchName) => (
              <option key={churchName} value={churchName}>
                {churchName}
              </option>
            ))}
          </select>
          <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500">
            필터 결과: {filteredUsers.length.toLocaleString("ko-KR")}명
          </div>
        </div>
        <div className="mt-2 grid grid-cols-1 gap-3 md:grid-cols-2">
          <select
            value={roleFilter}
            onChange={(event) => setRoleFilter(event.target.value as RoleFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">전체 역할</option>
            <option value="ADMIN">관리자</option>
            <option value="USER">일반 사용자</option>
          </select>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">전체 상태</option>
            <option value="ACTIVE">활성</option>
            <option value="DISABLED">비활성</option>
          </select>
        </div>

        <form onSubmit={handleCreate} className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
          <input
            value={newDisplayName}
            onChange={(event) => setNewDisplayName(event.target.value)}
            placeholder="새 사용자 표시 이름"
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 md:col-span-2"
          />
          <select
            value={newRole}
            onChange={(event) => setNewRole(event.target.value as EditableRole)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="USER">일반 사용자</option>
            <option value="ADMIN">관리자</option>
          </select>
          <div className="flex gap-2">
            <select
              value={newStatus}
              onChange={(event) => setNewStatus(event.target.value as EditableStatus)}
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ACTIVE">활성</option>
              <option value="DISABLED">비활성</option>
            </select>
            <button
              type="submit"
              disabled={creating}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {creating ? "생성 중..." : "생성"}
            </button>
          </div>
        </form>
      </section>

      {loading && <p className="text-sm text-gray-500">로딩 중...</p>}
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      {message && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</div>}

      {!loading && (
        <section className="soy-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-[980px] w-full text-left">
              <thead className="border-b border-slate-200 bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">사용자</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">교회</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">식별 정보</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">역할</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">상태</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr
                    key={user.id}
                    onClick={() => setSelectedUserId(user.id)}
                    className={`border-b border-slate-100 last:border-b-0 cursor-pointer ${
                      selectedUserId === user.id ? "bg-indigo-50/40" : "hover:bg-slate-50"
                    }`}
                    title="클릭하면 상세 정보가 열립니다."
                  >
                    <td className="px-4 py-3">
                      <div className="font-medium">{user.displayName}</div>
                      <div className="font-mono text-xs text-slate-500">{user.id}</div>
                    </td>
                    <td className="px-4 py-3 text-sm">{user.profile?.churchName ?? "-"}</td>
                    <td className="px-4 py-3 text-sm">
                      <div className="font-medium">{primaryContact(user)}</div>
                      <div className="text-xs text-slate-500">{user.primaryEmail ?? "-"}</div>
                    </td>
                    <td className="px-4 py-3 text-sm">{roleLabel(user.role)}</td>
                    <td className="px-4 py-3 text-sm">{statusLabel(user.status)}</td>
                    <td className="px-4 py-3 text-sm">
                      <button
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation();
                          void handleDisable(user);
                        }}
                        disabled={deletingId === user.id}
                        className="font-semibold text-red-600 hover:text-red-800 disabled:opacity-40"
                      >
                        {deletingId === user.id ? "비활성화 중..." : "비활성화"}
                      </button>
                    </td>
                  </tr>
                ))}
                {!filteredUsers.length && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-400">
                      조회된 사용자가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {selectedUser && draft && (
        <section className="soy-card p-4">
          <h3 className="text-lg font-semibold text-slate-900">사용자 상세</h3>
          <p className="mt-1 text-sm text-slate-600">프로필, 계정 상태, 전화번호를 수정할 수 있습니다.</p>
          <form onSubmit={handleSave} className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            <input
              value={draft.displayName}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, displayName: event.target.value } : prev))}
              placeholder="표시 이름"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.phoneNumber}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, phoneNumber: event.target.value } : prev))}
              placeholder="전화번호"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.churchName}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, churchName: event.target.value } : prev))}
              placeholder="교회"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.name}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, name: event.target.value } : prev))}
              placeholder="이름"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.group}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, group: event.target.value } : prev))}
              placeholder="구역"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <select
              value={draft.gender}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, gender: event.target.value as EditableGender } : prev))}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="UNKNOWN">{genderLabel("UNKNOWN")}</option>
              <option value="MALE">{genderLabel("MALE")}</option>
              <option value="FEMALE">{genderLabel("FEMALE")}</option>
            </select>
            <select
              value={draft.role}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, role: event.target.value as EditableRole } : prev))}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="USER">{roleLabel("USER")}</option>
              <option value="ADMIN">{roleLabel("ADMIN")}</option>
            </select>
            <select
              value={draft.status}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, status: event.target.value as EditableStatus } : prev))}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ACTIVE">{statusLabel("ACTIVE")}</option>
              <option value="DISABLED">{statusLabel("DISABLED")}</option>
            </select>
            <div className="md:col-span-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
              가입일: {formatDateTime(selectedUser.createdAt)} | 최근 로그인: {formatDateTime(selectedUser.lastLoginAt)}
            </div>
            <button
              type="submit"
              disabled={saving}
              className="md:col-span-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {saving ? "저장 중..." : "변경 저장"}
            </button>
          </form>
        </section>
      )}
    </div>
  );
}
