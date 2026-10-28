import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  getEvents,
  getSavedEvents,
  getMyEvents,
  getPendingEvents,
  getPendingApplications,
} from "../api";
import {
  getUser,
  formatRole,
  EVENT_CREATOR_ROLES,
  EVENT_REVIEWER_ROLES,
  APPLICATION_REVIEWER_ROLES,
} from "../auth";
import EventCard from "../components/EventCard";

function StatCard({ label, value, to }) {
  const content = (
    <>
      <span className="stat-value">{value ?? "–"}</span>
      <span className="stat-label">{label}</span>
    </>
  );
  return to ? (
    <Link to={to} className="card stat-card">{content}</Link>
  ) : (
    <div className="card stat-card">{content}</div>
  );
}

function Home() {
  const user = getUser();
  const role = user?.role;

  const [upcoming, setUpcoming] = useState([]);
  const [savedCount, setSavedCount] = useState(null);
  const [myCounts, setMyCounts] = useState(null);
  const [pendingEvents, setPendingEvents] = useState(null);
  const [pendingApps, setPendingApps] = useState(null);

  useEffect(() => {
    // Each block is independent so one failure doesn't break the page
    getEvents()
      .then((list) => {
        const now = new Date();
        setUpcoming(
          (list || [])
            .filter((e) => new Date(e.endDatetime) >= now)
            .sort((a, b) => new Date(a.startDatetime) - new Date(b.startDatetime))
            .slice(0, 3)
        );
      })
      .catch(() => {});

    getSavedEvents()
      .then((list) => setSavedCount((list || []).length))
      .catch(() => {});

    if (EVENT_CREATOR_ROLES.includes(role)) {
      getMyEvents()
        .then((list) => {
          const counts = { total: 0, draft: 0, submitted: 0, approved: 0, rejected: 0 };
          (list || []).forEach((item) => {
            counts.total += 1;
            const s = (item.event.status || "").toLowerCase();
            const v = (item.event.verificationStatus || "").toLowerCase();
            if (v === "approved" || v === "verified" || s === "approved") counts.approved += 1;
            else if (v === "rejected" || s === "rejected") counts.rejected += 1;
            else if (s === "submitted" || v === "pending") counts.submitted += 1;
            else if (s === "pending" || s === "submitted" || v === "pending") counts.submitted += 1;
            else counts.draft += 1;
          });
          setMyCounts(counts);
        })
        .catch(() => {});
    }

    if (EVENT_REVIEWER_ROLES.includes(role)) {
      getPendingEvents()
        .then((list) => setPendingEvents((list || []).length))
        .catch(() => {});
    }

    if (APPLICATION_REVIEWER_ROLES.includes(role)) {
      getPendingApplications()
        .then((list) => setPendingApps((list || []).length))
        .catch(() => {});
    }
  }, [role]);

  return (
    <>
      <div className="page-header">
        <h1>Welcome, {user?.name || "User"}!</h1>
        <p>Signed in as {formatRole(role)}</p>
      </div>

      <div className="stat-grid">
        <StatCard label="Saved events" value={savedCount} to="/saved" />

        {myCounts && (
          <>
            <StatCard label="My events" value={myCounts.total} to="/my-events" />
            <StatCard label="Awaiting review" value={myCounts.submitted} to="/my-events" />
            <StatCard label="Approved" value={myCounts.approved} to="/my-events" />
            <StatCard label="Rejected" value={myCounts.rejected} to="/my-events" />
          </>
        )}

        {pendingEvents !== null && (
          <StatCard label="Events to review" value={pendingEvents} to="/review-events" />
        )}
        {pendingApps !== null && (
          <StatCard label="Applications to review" value={pendingApps} to="/applications" />
        )}
      </div>

      {EVENT_CREATOR_ROLES.includes(role) && (
        <div className="quick-actions">
          <Link to="/events/new" className="btn btn-primary">+ Create Event</Link>
          <Link to="/my-events" className="btn btn-outline">Manage My Events</Link>
        </div>
      )}

      <div className="section-head">
        <h2>Upcoming events</h2>
        <Link to="/events" className="auth-link">View all →</Link>
      </div>

      {upcoming.length === 0 ? (
        <div className="card muted">No upcoming events right now.</div>
      ) : (
        <div className="grid">
          {upcoming.map((e) => (
            <EventCard key={e.eventId} event={e} />
          ))}
        </div>
      )}
    </>
  );
}

export default Home;