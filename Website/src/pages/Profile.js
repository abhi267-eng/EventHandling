import { useEffect, useState } from "react";
import { getMe } from "../api";
import { formatRole } from "../auth";
import StatusBadge from "../components/StatusBadge";
import PageState from "../components/PageState";

function Field({ label, value }) {
  return (
    <div className="profile-field">
      <span className="muted">{label}</span>
      <strong>{value || "—"}</strong>
    </div>
  );
}

function Profile() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getMe()
      .then(setData)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading || error || !data) {
    return <PageState loading={loading} error={error || (!data && "Profile not found.")} />;
  }

  const { profile, departments, organizations } = data;

  return (
    <>
      <div className="page-header">
        <h1>My Profile</h1>
      </div>

      <div className="card profile-card">
        <div className="profile-top">
          {profile.profilePictureUrl ? (
            <img className="profile-avatar" src={profile.profilePictureUrl} alt={profile.name} />
          ) : (
            <div className="profile-avatar profile-avatar-fallback">
              {profile.name.charAt(0).toUpperCase()}
            </div>
          )}
          <div>
            <h2>{profile.name}</h2>
            <p className="muted">{formatRole(profile.role)}</p>
            <StatusBadge status={profile.accountStatus} />
          </div>
        </div>

        <div className="profile-grid">
          <Field label="Email" value={profile.email} />
          <Field label="Phone" value={profile.phone} />
          <Field label="College" value={profile.collegeName} />
          <Field label="Designation" value={profile.designation} />
          <Field label="Email verified" value={profile.emailVerified ? "Yes" : "No"} />
        </div>

        <h3 className="profile-subhead">Departments</h3>
        {departments.length ? (
          <div className="event-card-tags">
            {departments.map((d) => (
              <span className="tag" key={d.departmentId}>
                {d.departmentName} ({d.departmentCode})
              </span>
            ))}
          </div>
        ) : (
          <p className="muted">None</p>
        )}

        <h3 className="profile-subhead">Organizations</h3>
        {organizations.length ? (
          <div className="event-card-tags">
            {organizations.map((o) => (
              <span className="tag tag-venue" key={o.organizationId}>
                {o.organizationName} · {o.organizationType}
              </span>
            ))}
          </div>
        ) : (
          <p className="muted">None</p>
        )}
      </div>
    </>
  );
}

export default Profile;