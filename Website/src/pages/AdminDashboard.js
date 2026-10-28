import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  getUsers,
  getEvents,
  getDepartments,
  getOrganizations,
  getColleges,
  getPendingApplications,
  getPendingEvents,
} from "../api";
import { getUser, formatRole, EVENT_REVIEWER_ROLES } from "../auth";
import useCollegeScope from "../hooks/useCollegeScope";
import StatCard from "../components/StatCard";
import PageState from "../components/PageState";

// number of items if the request worked, otherwise null (shown as "–")
const count = (result) =>
  result.status === "fulfilled" && Array.isArray(result.value) ? result.value.length : null;

function AdminDashboard() {
  const user = getUser();
  const role = user?.role;
  const scope = useCollegeScope({ allowAll: true });
  const [stats, setStats] = useState(null);

  const noCollege = !scope.loading && !scope.error && !scope.isPlatform && !scope.collegeId;

  useEffect(() => {
    if (scope.loading || scope.error || noCollege) return;

    const collegeId = scope.collegeId ? Number(scope.collegeId) : null;
    let cancelled = false;

    async function load() {
      const results = await Promise.allSettled([
        getUsers({ collegeId: collegeId || undefined, limit: 1000 }),
        getEvents().then((list) =>
          collegeId ? (list || []).filter((e) => e.collegeId === collegeId) : list
        ),
        getPendingApplications(),
        EVENT_REVIEWER_ROLES.includes(role) ? getPendingEvents() : Promise.resolve(null),
        scope.isPlatform ? getColleges() : getDepartments(collegeId),
        scope.isPlatform ? Promise.resolve(null) : getOrganizations(collegeId),
      ]);

      if (cancelled) return;

      setStats({
        users: count(results[0]),
        events: count(results[1]),
        applications: count(results[2]),
        pendingEvents: count(results[3]),
        colleges: scope.isPlatform ? count(results[4]) : null,
        departments: scope.isPlatform ? null : count(results[4]),
        organizations: count(results[5]),
      });
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [scope.loading, scope.error, scope.collegeId, scope.isPlatform, noCollege, role]);

  return (
    <>
      <div className="page-header">
        <h1>{scope.isPlatform ? "Platform Admin" : "College Dashboard"}</h1>
        <p>Signed in as {formatRole(role)}</p>
      </div>

      <PageState
        loading={scope.loading || (!stats && !scope.error && !noCollege)}
        error={scope.error || (noCollege && "Your account isn't linked to a college.")}
      />

      {stats && (
        <>
          <div className="stat-grid">
            {scope.isPlatform && (
              <StatCard label="Colleges" value={stats.colleges} to="/admin/colleges" />
            )}
            <StatCard label="Users" value={stats.users} to="/admin/users" />
            <StatCard label="Events" value={stats.events} to="/events" />
            <StatCard label="Applications to review" value={stats.applications} to="/applications" />
            {stats.pendingEvents !== null && (
              <StatCard label="Events to review" value={stats.pendingEvents} to="/review-events" />
            )}
            {!scope.isPlatform && (
              <>
                <StatCard label="Departments" value={stats.departments} to="/admin/departments" />
                <StatCard label="Organizations" value={stats.organizations} to="/admin/organizations" />
              </>
            )}
          </div>

          <div className="section-head">
            <h2>Manage</h2>
          </div>

          <div className="quick-actions">
            <Link to="/admin/users" className="btn btn-primary">Users</Link>
            <Link to="/admin/departments" className="btn btn-outline">Departments</Link>
            <Link to="/admin/organizations" className="btn btn-outline">Organizations</Link>
            <Link to="/applications" className="btn btn-outline">Applications</Link>
            {scope.isPlatform && (
              <Link to="/admin/colleges" className="btn btn-outline">All colleges</Link>
            )}
          </div>

          <p className="muted">Counts of users are capped at 1000.</p>
        </>
      )}
    </>
  );
}

export default AdminDashboard;