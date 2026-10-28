import { useEffect, useState } from "react";
import { getColleges, getDepartments, getOrganizations, getUsers } from "../api";
import StatCard from "../components/StatCard";
import PageState from "../components/PageState";

function PlatformOverview() {
  const [rows, setRows] = useState([]);
  const [totalUsers, setTotalUsers] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const colleges = (await getColleges()) || [];

        const [users, ...perCollege] = await Promise.all([
          getUsers({ limit: 1000 }).catch(() => []),
          ...colleges.map((c) =>
            Promise.all([
              getDepartments(c.collegeId).catch(() => []),
              getOrganizations(c.collegeId).catch(() => []),
            ])
          ),
        ]);

        const userCounts = {};
        (users || []).forEach((u) => {
          if (u.collegeId) userCounts[u.collegeId] = (userCounts[u.collegeId] || 0) + 1;
        });

        setTotalUsers((users || []).length);
        setRows(
          colleges.map((c, i) => ({
            ...c,
            users: userCounts[c.collegeId] || 0,
            departments: perCollege[i][0].length,
            organizations: perCollege[i][1].length,
          }))
        );
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  return (
    <>
      <div className="page-header">
        <h1>All Colleges</h1>
        <p>Every college on the platform.</p>
      </div>

      <PageState
        loading={loading}
        error={error}
        empty={!loading && !error && rows.length === 0}
        emptyText="No colleges yet. They appear once a College Admin application is approved."
      />

      {!loading && !error && rows.length > 0 && (
        <>
          <div className="stat-grid">
            <StatCard label="Colleges" value={rows.length} />
            <StatCard label="Users" value={totalUsers} to="/admin/users" />
          </div>

          <div className="grid">
            {rows.map((c) => (
              <div className="card overview-card" key={c.collegeId}>
                {c.logoUrl && <img className="overview-logo" src={c.logoUrl} alt={c.collegeName} />}
                <h3>{c.collegeName}</h3>
                <p className="muted">
                  {c.collegeCode}
                  {c.city ? ` · ${c.city}` : ""}
                </p>

                <div className="overview-counts">
                  <span><strong>{c.users}</strong> users</span>
                  <span><strong>{c.departments}</strong> departments</span>
                  <span><strong>{c.organizations}</strong> organizations</span>
                </div>

                {c.website && (
                  <a className="auth-link" href={c.website} target="_blank" rel="noreferrer">
                    Website ↗
                  </a>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </>
  );
}

export default PlatformOverview;