import { useEffect, useState } from "react";
import {
  getPendingApplications,
  reviewApplication,
  getColleges,
  getDepartments,
  getOrganizations,
} from "../api";
import { formatRole } from "../auth";
import PageState from "../components/PageState";
import { useToast } from "../components/Feedback";


function Applications() {
  const toast = useToast();
  const [apps, setApps] = useState([]);
  const [colleges, setColleges] = useState({});
  const [departments, setDepartments] = useState({});
  const [organizations, setOrganizations] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [comments, setComments] = useState({});

  useEffect(() => {
    async function load() {
      try {
        const list = (await getPendingApplications()) || [];
        setApps(list);

        // Resolve ids to readable names
        const cols = await getColleges().catch(() => []);
        setColleges(Object.fromEntries(cols.map((c) => [c.collegeId, c.collegeName])));

        const collegeIds = [...new Set(list.map((a) => a.collegeId).filter(Boolean))];
        const deptMap = {};
        const orgMap = {};
        await Promise.all(
          collegeIds.map(async (cid) => {
            const [d, o] = await Promise.all([
              getDepartments(cid).catch(() => []),
              getOrganizations(cid).catch(() => []),
            ]);
            d.forEach((x) => (deptMap[x.departmentId] = x.departmentName));
            o.forEach((x) => (orgMap[x.organizationId] = x.organizationName));
          })
        );
        setDepartments(deptMap);
        setOrganizations(orgMap);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  async function decide(app, status) {
    const text = (comments[app.applicationId] || "").trim();
    if (status === "REJECTED" && !text) {
      toast.error("Please add a comment explaining the rejection.");
      return;
    }
    setBusyId(app.applicationId);
    try {
      await reviewApplication({
        applicationId: app.applicationId,
        status,
        comments: text || null,
      });
      setApps((prev) => prev.filter((a) => a.applicationId !== app.applicationId));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <>
      <div className="page-header">
        <h1>Role Applications</h1>
        <p>Review people applying for higher roles.</p>
      </div>

      <PageState
        loading={loading}
        error={error}
        empty={!loading && !error && apps.length === 0}
        emptyText="No pending applications. 🎉"
      />

      <div className="review-queue">
        {apps.map((a) => (
          <div className="card queue-item" key={a.applicationId}>
            <div className="queue-head">
              {a.profilePictureUrl ? (
                <img className="profile-avatar queue-avatar" src={a.profilePictureUrl} alt={a.name} />
              ) : (
                <div className="profile-avatar profile-avatar-fallback queue-avatar">
                  {a.name.charAt(0).toUpperCase()}
                </div>
              )}
              <div>
                <h3>{a.name}</h3>
                <span className="tag">{formatRole(a.role)}</span>
              </div>
            </div>

            <div className="profile-grid">
              <div className="profile-field"><span className="muted">Email</span><strong>{a.email}</strong></div>
              <div className="profile-field"><span className="muted">Phone</span><strong>{a.phone || "—"}</strong></div>
              <div className="profile-field"><span className="muted">College</span><strong>{colleges[a.collegeId] || "—"}</strong></div>
              <div className="profile-field"><span className="muted">College ID number</span><strong>{a.collegeIdNumber || "—"}</strong></div>
              <div className="profile-field"><span className="muted">Designation</span><strong>{a.designation || "—"}</strong></div>
              {a.requestedDepartmentId && (
                <div className="profile-field">
                  <span className="muted">Department</span>
                  <strong>{departments[a.requestedDepartmentId] || `#${a.requestedDepartmentId}`}</strong>
                </div>
              )}
              {a.requestedOrganizationId && (
                <div className="profile-field">
                  <span className="muted">Organization</span>
                  <strong>{organizations[a.requestedOrganizationId] || `#${a.requestedOrganizationId}`}</strong>
                </div>
              )}
            </div>

            {a.collegeIdPhotoUrl && (
              <a className="auth-link" href={a.collegeIdPhotoUrl} target="_blank" rel="noreferrer">
                View ID proof ↗
              </a>
            )}

            <textarea
              className="form-input queue-comment"
              rows="2"
              placeholder="Comments (required when rejecting)"
              value={comments[a.applicationId] || ""}
              onChange={(e) => setComments((p) => ({ ...p, [a.applicationId]: e.target.value }))}
            />

            <div className="queue-actions">
              <button
                type="button"
                className="btn btn-danger"
                disabled={busyId === a.applicationId}
                onClick={() => decide(a, "REJECTED")}
              >
                Reject
              </button>
              <button
                type="button"
                className="btn btn-primary"
                disabled={busyId === a.applicationId}
                onClick={() => decide(a, "APPROVED")}
              >
                Approve
              </button>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}

export default Applications;