import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyEvents, submitEvent, deleteEvent } from "../api";
import { formatDateTime } from "../components/EventCard";
import StatusBadge from "../components/StatusBadge";
import PageState from "../components/PageState";
import { useToast, useConfirm } from "../components/Feedback";
// inside the component:

const TABS = [
  { key: "all", label: "All" },
  { key: "draft", label: "Draft" },
  { key: "submitted", label: "Under review" },
  { key: "approved", label: "Approved" },
  { key: "rejected", label: "Rejected" },
];

// Collapse the backend's status + verificationStatus into one bucket
function bucketOf(event) {
  
  const s = (event.status || "").toLowerCase();
  const v = (event.verificationStatus || "").toLowerCase();
  if (v === "rejected" || s === "rejected") return "rejected";
  if (v === "approved" || v === "verified" || s === "approved") return "approved";
  if (s === "submitted" || v === "pending") return "submitted";
  if (s === "pending" || s === "submitted" || v === "pending") return "submitted";
  return "draft";
}

function MyEvents() {
  const toast = useToast();
  const confirm = useConfirm();
  const [items, setItems] = useState([]);
  const [tab, setTab] = useState("all");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  async function load() {
    try {
      setItems((await getMyEvents()) || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleSubmit(id) {
    setBusyId(id);
    try {
      await submitEvent(id);
      await load();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(id, title) {
    const ok = await confirm({
        title: "Delete event?",
        message: `"${title}" will be permanently deleted. This cannot be undone.`,
        confirmText: "Delete",
        danger: true,
        });
        if (!ok) return;
        setBusyId(id);
    try {
      await deleteEvent(id);
      setItems((prev) => prev.filter((i) => i.event.eventId !== id));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  }

  const visible = items.filter((i) => tab === "all" || bucketOf(i.event) === tab);

  return (
    <>
      <div className="page-header page-header-row">
        <div>
          <h1>My Events</h1>
          <p>Create, submit and track your events.</p>
        </div>
        <Link to="/events/new" className="btn btn-primary">+ Create Event</Link>
      </div>

      <div className="tab-bar">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            className={`tab ${tab === t.key ? "active" : ""}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
            <span className="tab-count">
              {t.key === "all"
                ? items.length
                : items.filter((i) => bucketOf(i.event) === t.key).length}
            </span>
          </button>
        ))}
      </div>

      <PageState
        loading={loading}
        error={error}
        empty={!loading && !error && visible.length === 0}
        emptyText="No events here yet."
      />

      <div className="my-events-list">
        {visible.map(({ event, reviewComments }) => {
          const bucket = bucketOf(event);
          const busy = busyId === event.eventId;
          return (
            <div className="card my-event" key={event.eventId}>
              <div className="my-event-main">
                <div className="my-event-title">
                  <Link to={`/events/${event.eventId}`}>{event.title}</Link>
                  <StatusBadge status={bucket === "submitted" ? "Under review" : bucket} />
                </div>
                <p className="event-card-meta">📅 {formatDateTime(event.startDatetime)}</p>
                {event.venueName && <p className="event-card-meta">📍 {event.venueName}</p>}

                {reviewComments && (
                  <div className="review-comment">
                    <strong>Reviewer comments:</strong> {reviewComments}
                  </div>
                )}
              </div>

              <div className="my-event-actions">
                {(bucket === "draft" || bucket === "rejected") && (
                  <>
                    <Link to={`/events/${event.eventId}/edit`} className="btn btn-outline">
                      Edit
                    </Link>
                    <button
                      type="button"
                      className="btn btn-primary"
                      disabled={busy}
                      onClick={() => handleSubmit(event.eventId)}
                    >
                      {bucket === "rejected" ? "Resubmit" : "Submit for review"}
                    </button>
                  </>
                )}
                <button
                  type="button"
                  className="btn btn-danger"
                  disabled={busy}
                  onClick={() => handleDelete(event.eventId, event.title)}
                >
                  Delete
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
}

export default MyEvents;