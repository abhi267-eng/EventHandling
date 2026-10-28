import { Link } from "react-router-dom";

export function formatDateTime(value) {
  if (!value) return "";
  const d = new Date(value);
  if (isNaN(d)) return value;
  return d.toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function EventCard({ event, categoryName, collegeName, saved, onToggleSave }) {
  return (
    <div className="card event-card">
      <Link to={`/events/${event.eventId}`} className="event-card-poster">
        {event.posterUrl ? (
          <img src={event.posterUrl} alt={event.title} />
        ) : (
          <div className="event-card-poster-fallback">
            {event.title.charAt(0).toUpperCase()}
          </div>
        )}
      </Link>

      <div className="event-card-body">
        <div className="event-card-tags">
          {categoryName && <span className="tag">{categoryName}</span>}
          <span className="tag tag-venue">{event.venueType}</span>
        </div>

        <h3>
          <Link to={`/events/${event.eventId}`}>{event.title}</Link>
        </h3>

        <p className="event-card-meta">📅 {formatDateTime(event.startDatetime)}</p>
        {event.venueName && <p className="event-card-meta">📍 {event.venueName}</p>}
        {collegeName && <p className="event-card-meta">🎓 {collegeName}</p>}

        <div className="event-card-footer">
          <Link to={`/events/${event.eventId}`} className="btn btn-primary">
            View Details
          </Link>
          {onToggleSave && (
            <button
              type="button"
              className={`save-button ${saved ? "saved" : ""}`}
              onClick={() => onToggleSave(event.eventId)}
              title={saved ? "Remove from saved" : "Save event"}
            >
              {saved ? "♥" : "♡"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default EventCard;