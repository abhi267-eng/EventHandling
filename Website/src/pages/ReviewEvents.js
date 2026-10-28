import { useEffect, useState } from "react";
import { getPendingEvents, reviewEvent, getCategories, getColleges } from "../api";
import { formatDateTime } from "../components/EventCard";
import PageState from "../components/PageState";
import { useToast } from "../components/Feedback";
// inside the component:
const toast = useToast();

function ReviewEvents() {
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState({});
  const [colleges, setColleges] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [comments, setComments] = useState({});

  useEffect(() => {
    async function load() {
      try {
        const [list, cats, cols] = await Promise.all([
          getPendingEvents(),
          getCategories().catch(() => []),
          getColleges().catch(() => []),
        ]);
        setEvents(list || []);
        setCategories(Object.fromEntries(cats.map((c) => [c.categoryId, c.categoryName])));
        setColleges(Object.fromEntries(cols.map((c) => [c.collegeId, c.collegeName])));
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  async function decide(event, status) {
    const text = (comments[event.eventId] || "").trim();
    if (status === "REJECTED" && !text) {
      toast.error("Please add a comment so the organizer knows what to fix.");
      return;
    }
    setBusyId(event.eventId);
    try {
      await reviewEvent(event.eventId, { status, comments: text || null });
      setEvents((prev) => prev.filter((e) => e.eventId !== event.eventId));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <>
      <div className="page-header">
        <h1>Event Review Queue</h1>
        <p>Approve or reject events submitted by organizers.</p>
      </div>

      <PageState
        loading={loading}
        error={error}
        empty={!loading && !error && events.length === 0}
        emptyText="No events waiting for review. 🎉"
      />

      <div className="review-queue">
        {events.map((ev) => (
          <div className="card queue-item" key={ev.eventId}>
            <div className="queue-head">
              <div>
                <h3>{ev.title}</h3>
                <div className="event-card-tags">
                  {categories[ev.categoryId] && <span className="tag">{categories[ev.categoryId]}</span>}
                  <span className="tag tag-venue">{ev.venueType}</span>
                  {ev.acceptVisitors && <span className="tag tag-open">Visitors welcome</span>}
                </div>
              </div>
            </div>

            <div className="profile-grid">
              <div className="profile-field"><span className="muted">Starts</span><strong>{formatDateTime(ev.startDatetime)}</strong></div>
              <div className="profile-field"><span className="muted">Ends</span><strong>{formatDateTime(ev.endDatetime)}</strong></div>
              <div className="profile-field"><span className="muted">Venue</span><strong>{ev.venueName || "—"}</strong></div>
              <div className="profile-field"><span className="muted">College</span><strong>{colleges[ev.collegeId] || "—"}</strong></div>
            </div>

            <textarea
              className="form-input queue-comment"
              rows="2"
              placeholder="Comments (required when rejecting)"
              value={comments[ev.eventId] || ""}
              onChange={(e) => setComments((p) => ({ ...p, [ev.eventId]: e.target.value }))}
            />

            <div className="queue-actions">
              <button
                type="button"
                className="btn btn-danger"
                disabled={busyId === ev.eventId}
                onClick={() => decide(ev, "REJECTED")}
              >
                Reject
              </button>
              <button
                type="button"
                className="btn btn-primary"
                disabled={busyId === ev.eventId}
                onClick={() => decide(ev, "APPROVED")}
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

export default ReviewEvents;