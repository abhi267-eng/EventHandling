import { useEffect, useMemo, useState } from "react";
import { getUsers, userAction, deleteUser } from "../api";
import { ROLES, getUser, formatRole } from "../auth";
import useCollegeScope from "../hooks/useCollegeScope";
import { useToast, useConfirm } from "../components/Feedback";
import StatusBadge from "../components/StatusBadge";
import PageState from "../components/PageState";

const PAGE_SIZE = 15;
const STATUSES = ["ACTIVE", "SUSPENDED", "DELETED"];

function ManageUsers() {
  const toast = useToast();
  const confirm = useConfirm();
  const me = getUser();
  const scope = useCollegeScope({ allowAll: true });

  const [users, setUsers] = useState([]);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);

  const notLinked = !scope.loading && !scope.isPlatform && !scope.collegeId;

  const collegeMap = useMemo(
    () => Object.fromEntries(scope.colleges.map((c) => [c.collegeId, c.collegeName])),
    [scope.colleges]
  );

  // Wait a moment after typing before searching
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    if (scope.loading || scope.error) return;
    if (notLinked) {
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError("");

    getUsers({
      collegeId: scope.collegeId ? Number(scope.collegeId) : undefined,
      role: role || undefined,
      status: status || undefined,
      search: search || undefined,
      limit: PAGE_SIZE + 1, // one extra row tells us if there is a next page
      offset: page * PAGE_SIZE,
    })
      .then((list) => {
        if (cancelled) return;
        const rows = list || [];
        setHasNext(rows.length > PAGE_SIZE);
        setUsers(rows.slice(0, PAGE_SIZE));
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [scope.loading, scope.error, scope.collegeId, notLinked, role, status, search, page, reloadKey]);

  function canManage(u) {
    return (
      u.userId !== me?.userId &&
      u.role !== ROLES.PLATFORM_ADMIN &&
      u.accountStatus !== "DELETED"
    );
  }

  async function toggleStatus(u) {
    const suspend = u.accountStatus !== "SUSPENDED";
    const ok = await confirm({
      title: suspend ? "Suspend user?" : "Activate user?",
      message: suspend
        ? `${u.name}'s account will be suspended.`
        : `${u.name}'s account will be activated again.`,
      confirmText: suspend ? "Suspend" : "Activate",
      danger: suspend,
    });
    if (!ok) return;

    setBusyId(u.userId);
    try {
      await userAction(u.userId, suspend ? "suspend" : "activate");
      setUsers((prev) =>
        prev.map((x) =>
          x.userId === u.userId
            ? { ...x, accountStatus: suspend ? "SUSPENDED" : "ACTIVE" }
            : x
        )
      );
      toast.success(suspend ? "User suspended." : "User activated.");
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(u) {
    const ok = await confirm({
      title: "Delete user?",
      message: `${u.name}'s account will be removed and their personal details cleared. This cannot be undone.`,
      confirmText: "Delete",
      danger: true,
    });
    if (!ok) return;

    setBusyId(u.userId);
    try {
      await deleteUser(u.userId);
      toast.success("User deleted.");
      setReloadKey((k) => k + 1);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  }

  function clearFilters() {
    setSearchInput("");
    setSearch("");
    setRole("");
    setStatus("");
    setPage(0);
    if (scope.isPlatform) scope.setCollegeId("");
  }

  return (
    <>
      <div className="page-header">
        <h1>Manage Users</h1>
        <p>{scope.isPlatform ? "Users across all colleges." : "Users in your college."}</p>
      </div>

      <div className="toolbar card">
        <input
          className="form-input"
          type="text"
          placeholder="Search by name or email..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />

        {scope.isPlatform && (
          <select
            className="form-input"
            value={scope.collegeId}
            onChange={(e) => {
              scope.setCollegeId(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All colleges</option>
            {scope.colleges.map((c) => (
              <option key={c.collegeId} value={c.collegeId}>{c.collegeName}</option>
            ))}
          </select>
        )}

        <select
          className="form-input"
          value={role}
          onChange={(e) => {
            setRole(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All roles</option>
          {Object.values(ROLES).map((r) => (
            <option key={r} value={r}>{formatRole(r)}</option>
          ))}
        </select>

        <select
          className="form-input"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{formatRole(s)}</option>
          ))}
        </select>

        <button type="button" className="btn btn-outline" onClick={clearFilters}>
          Clear
        </button>
      </div>

      <PageState
        loading={loading || scope.loading}
        error={scope.error || error || (notLinked && "Your account isn't linked to a college.")}
        empty={!loading && !scope.loading && !error && !notLinked && users.length === 0}
        emptyText="No users found."
      />

      {!loading && users.length > 0 && (
        <>
          <div className="card table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Role</th>
                  {scope.isPlatform && <th>College</th>}
                  <th>Status</th>
                  <th>Verified</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.userId}>
                    <td>
                      <strong>{u.name}</strong>
                      <span className="cell-sub">{u.email}</span>
                    </td>
                    <td>
                      {formatRole(u.role)}
                      {u.designation && <span className="cell-sub">{u.designation}</span>}
                    </td>
                    {scope.isPlatform && <td>{collegeMap[u.collegeId] || "—"}</td>}
                    <td><StatusBadge status={u.accountStatus} /></td>
                    <td>{u.emailVerified ? "Yes" : "No"}</td>
                    <td>
                      {canManage(u) && (
                        <div className="row-actions">
                          <button
                            type="button"
                            className="btn btn-outline btn-sm"
                            disabled={busyId === u.userId}
                            onClick={() => toggleStatus(u)}
                          >
                            {u.accountStatus === "SUSPENDED" ? "Activate" : "Suspend"}
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            disabled={busyId === u.userId}
                            onClick={() => handleDelete(u)}
                          >
                            Delete
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pagination">
            <button
              type="button"
              className="btn btn-outline btn-sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              ← Previous
            </button>
            <span className="muted">Page {page + 1}</span>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              disabled={!hasNext}
              onClick={() => setPage((p) => p + 1)}
            >
              Next →
            </button>
          </div>
        </>
      )}
    </>
  );
}

export default ManageUsers;