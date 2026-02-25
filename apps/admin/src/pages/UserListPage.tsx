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
  if (!displayName) return { payload: null, error: "Display name is required." };
  if (displayName.length > 64) return { payload: null, error: "Display name is too long." };
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
      return { payload: null, error: "Church, name, and group are required when updating profile." };
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
      return { payload: null, error: "Phone number must be 10-11 digits." };
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
      setError(err instanceof Error ? err.message : "Failed to load users.");
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
      setError("You cannot change your own role.");
      return;
    }
    if (currentUser?.userId === selectedUser.id && draft.status !== selectedUser.status) {
      setError("You cannot change your own status.");
      return;
    }
    const wouldLoseAdmin =
      selectedUser.role === "ADMIN" &&
      selectedUser.status === "ACTIVE" &&
      (draft.role !== "ADMIN" || draft.status !== "ACTIVE");
    if (wouldLoseAdmin && activeAdminCount <= 1) {
      setError("You cannot demote/disable the last active admin.");
      return;
    }

    const { payload, error: payloadError } = buildPayload(selectedUser, draft);
    if (payloadError) {
      setError(payloadError);
      return;
    }
    if (!payload) {
      setMessage("No changes to save.");
      return;
    }

    setSaving(true);
    try {
      const updated = await updateUser(selectedUser.id, payload);
      setUsers((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
      setDraft(toDraft(updated));
      setMessage("User updated.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update user.");
    } finally {
      setSaving(false);
    }
  };

  const handleDisable = async (targetUser: UserResponse) => {
    if (currentUser?.userId === targetUser.id) {
      setError("You cannot disable your own account.");
      return;
    }
    if (targetUser.role === "ADMIN" && targetUser.status === "ACTIVE" && activeAdminCount <= 1) {
      setError("You cannot disable the last active admin.");
      return;
    }
    if (targetUser.status === "DISABLED") {
      setError("This user is already disabled.");
      return;
    }

    setDeletingId(targetUser.id);
    setError(null);
    setMessage(null);
    try {
      const deleted = await deleteUser(targetUser.id);
      setUsers((prev) => prev.map((item) => (item.id === deleted.id ? deleted : item)));
      setMessage(`Disabled ${deleted.displayName}.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to disable user.");
    } finally {
      setDeletingId(null);
    }
  };

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const displayName = normalizeText(newDisplayName);
    if (!displayName) {
      setError("Display name is required.");
      return;
    }
    if (displayName.length > 64) {
      setError("Display name is too long.");
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
      setMessage(`Created ${created.displayName}.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create user.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-card p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-600">User Console</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-900">User Management</h2>
            <p className="mt-1 text-sm text-slate-500">Filter by church and identify users with phone/email quickly.</p>
          </div>
          <button
            type="button"
            onClick={() => void loadUsers()}
            className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            Reload
          </button>
        </div>

        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">Total Users</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">{users.length.toLocaleString("ko-KR")}</div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">Active Users</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">
              {users.filter((item) => item.status === "ACTIVE").length.toLocaleString("ko-KR")}
            </div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">Active Admins</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">{activeAdminCount.toLocaleString("ko-KR")}</div>
          </div>
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div className="text-xs text-slate-500">Phone Verified</div>
            <div className="mt-1 text-lg font-semibold text-slate-900">
              {users.filter((item) => normalizePhone(item.verification?.phoneNumber).length > 0).length.toLocaleString("ko-KR")}
            </div>
          </div>
        </div>

        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search name/email/phone/UUID"
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 md:col-span-2"
          />
          <select
            value={churchFilter}
            onChange={(event) => setChurchFilter(event.target.value)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">All churches</option>
            {churchOptions.map((churchName) => (
              <option key={churchName} value={churchName}>
                {churchName}
              </option>
            ))}
          </select>
          <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500">
            Filtered: {filteredUsers.length.toLocaleString("ko-KR")}
          </div>
        </div>
        <div className="mt-2 grid grid-cols-1 gap-3 md:grid-cols-2">
          <select
            value={roleFilter}
            onChange={(event) => setRoleFilter(event.target.value as RoleFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">All roles</option>
            <option value="ADMIN">ADMIN</option>
            <option value="USER">USER</option>
          </select>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="all">All status</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="DISABLED">DISABLED</option>
          </select>
        </div>

        <form onSubmit={handleCreate} className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
          <input
            value={newDisplayName}
            onChange={(event) => setNewDisplayName(event.target.value)}
            placeholder="New user display name"
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 md:col-span-2"
          />
          <select
            value={newRole}
            onChange={(event) => setNewRole(event.target.value as EditableRole)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <div className="flex gap-2">
            <select
              value={newStatus}
              onChange={(event) => setNewStatus(event.target.value as EditableStatus)}
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="DISABLED">DISABLED</option>
            </select>
            <button
              type="submit"
              disabled={creating}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {creating ? "Creating..." : "Create"}
            </button>
          </div>
        </form>
      </section>

      {loading && <p className="text-sm text-gray-500">Loading...</p>}
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      {message && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</div>}

      {!loading && (
        <section className="soy-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-[980px] w-full text-left">
              <thead className="border-b border-slate-200 bg-slate-50">
                <tr>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">User</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">Church</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">Contact</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">Role</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">Status</th>
                  <th className="px-4 py-3 text-sm font-semibold text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id} className={`border-b border-slate-100 last:border-b-0 ${selectedUserId === user.id ? "bg-indigo-50/40" : ""}`}>
                    <td className="px-4 py-3">
                      <div className="font-medium">{user.displayName}</div>
                      <div className="font-mono text-xs text-slate-500">{user.id}</div>
                    </td>
                    <td className="px-4 py-3 text-sm">{user.profile?.churchName ?? "-"}</td>
                    <td className="px-4 py-3 text-sm">
                      <div>{primaryContact(user)}</div>
                      <div className="text-xs text-slate-500">{user.primaryEmail ?? "-"}</div>
                    </td>
                    <td className="px-4 py-3 text-sm">{user.role}</td>
                    <td className="px-4 py-3 text-sm">{user.status}</td>
                    <td className="px-4 py-3 text-sm">
                      <button
                        type="button"
                        onClick={() => setSelectedUserId(user.id)}
                        className="mr-3 font-semibold text-indigo-700 hover:text-indigo-900"
                      >
                        Details
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleDisable(user)}
                        disabled={deletingId === user.id}
                        className="font-semibold text-red-600 hover:text-red-800 disabled:opacity-40"
                      >
                        {deletingId === user.id ? "Disabling..." : "Disable"}
                      </button>
                    </td>
                  </tr>
                ))}
                {!filteredUsers.length && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-400">
                      No users found.
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
          <h3 className="text-lg font-semibold text-slate-900">User Details</h3>
          <p className="mt-1 text-sm text-slate-600">Update profile, account status, and phone info.</p>
          <form onSubmit={handleSave} className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            <input
              value={draft.displayName}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, displayName: event.target.value } : prev))}
              placeholder="Display name"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.phoneNumber}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, phoneNumber: event.target.value } : prev))}
              placeholder="Phone number"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.churchName}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, churchName: event.target.value } : prev))}
              placeholder="Church"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.name}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, name: event.target.value } : prev))}
              placeholder="Name"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              value={draft.group}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, group: event.target.value } : prev))}
              placeholder="Group"
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <select
              value={draft.gender}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, gender: event.target.value as EditableGender } : prev))}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="UNKNOWN">UNKNOWN</option>
              <option value="MALE">MALE</option>
              <option value="FEMALE">FEMALE</option>
            </select>
            <select
              value={draft.role}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, role: event.target.value as EditableRole } : prev))}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <select
              value={draft.status}
              onChange={(event) => setDraft((prev) => (prev ? { ...prev, status: event.target.value as EditableStatus } : prev))}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="DISABLED">DISABLED</option>
            </select>
            <div className="md:col-span-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
              Joined: {formatDateTime(selectedUser.createdAt)} | Last login: {formatDateTime(selectedUser.lastLoginAt)}
            </div>
            <button
              type="submit"
              disabled={saving}
              className="md:col-span-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {saving ? "Saving..." : "Save Changes"}
            </button>
          </form>
        </section>
      )}
    </div>
  );
}
