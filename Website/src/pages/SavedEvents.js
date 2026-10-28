import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getSavedEvents, unsaveEvent } from "../api";
import EventCard from "../components/EventCard";
import PageState from "../components/PageState";
import { useToast } from "../components/Feedback";
// inside the component:
const toast = useToast();

function SavedEvents() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getSavedEvents()
      .then((list) => setEvents((list || []).map((item) => item.event || item)))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleUnsave(eventId) {
    try {
      await unsaveEvent(eventId);
      setEvents((prev) => prev.filter((e) => e.eventId !== eventId));
    } catch (err) {
      alert(err.message);
    }
  }

  return (
    <>
      <div className="page-header">
        <h1>Saved Events</h1>
        <p>Events you've bookmarked.</p>
      </div>

      <PageState loading={loading} error={error} />

      {!loading && !error && events.length === 0 && (
        <div className="page-state">
          You haven't saved any events yet.
          <div style={{ marginTop: 16 }}>
            <Link to="/events" className="btn btn-primary">Browse events</Link>
          </div>
        </div>
      )}

      {events.length > 0 && (
        <div className="grid">
          {events.map((e) => (
            <EventCard key={e.eventId} event={e} saved onToggleSave={handleUnsave} />
          ))}
        </div>
      )}
    </>
  );
}

export default SavedEvents;