import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  getEvent,
  getEventReviews,
  getCategories,
  getColleges,
  getSavedEvents,
  saveEvent,
  unsaveEvent,
  postReview,
} from "../api";
import { isLoggedIn } from "../auth";
import { formatDateTime } from "../components/EventCard";
import StarRating from "../components/StarRating";
import PageState from "../components/PageState";
import { useToast } from "../components/Feedback";
// inside the component:
const toast = useToast();

const DAY_MS = 24 * 60 * 60 * 1000;

function EventDetails() {
  const { id } = useParams();
  const loggedIn = isLoggedIn();

  const [event, setEvent] = useState(null);
  const [reviews, setReviews] = useState(null);
  const [categoryName, setCategoryName] = useState("");
  const [collegeName, setCollegeName] = useState("");
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [rating, setRating] = useState(0);
  const [reviewText, setReviewText] = useState("");
  const [reviewMsg, setReviewMsg] = useState({ text: "", ok: false });
  const [submitting, setSubmitting] = useState(false);

  async function loadReviews() {
    try {
      setReviews(await getEventReviews(id));
    } catch {
      setReviews({ averageRating: null, reviewCount: 0, reviews: [] });
    }
  }

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError("");
      try {
        const ev = await getEvent(id);
        setEvent(ev);

        const [cats, cols] = await Promise.all([
          getCategories().catch(() => []),
          getColleges().catch(() => []),
        ]);
        setCategoryName(cats.find((c) => c.categoryId === ev.categoryId)?.categoryName || "");
        setCollegeName(cols.find((c) => c.collegeId === ev.collegeId)?.collegeName || "");

        await loadReviews();

        if (loggedIn) {
          try {
            const savedList = await getSavedEvents();
            setSaved((savedList || []).some((s) => (s.event || s).eventId === ev.eventId));
          } catch {
            /* optional */
          }
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const reviewState = useMemo(() => {
    if (!event) return "NOT_ENDED";
    const end = new Date(event.endDatetime).getTime();
    const now = Date.now();
    if (now < end) return "NOT_ENDED";
    if (now > end + DAY_MS) return "CLOSED";
    return "OPEN";
  }, [event]);

  async function toggleSave() {
    if (!loggedIn) {
      window.location.href = "/login";
      return;
    }
    try {
      saved ? await unsaveEvent(id) : await saveEvent(id);
      setSaved(!saved);
    } catch (err) {
      alert(err.message);
    }
  }

  async function handleReview(e) {
    e.preventDefault();
    if (rating < 1) {
      setReviewMsg({ text: "Please select a rating.", ok: false });
      return;
    }
    setSubmitting(true);
    setReviewMsg({ text: "", ok: false });
    try {
      await postReview(id, rating, reviewText.trim() || null);
      setReviewMsg({ text: "Thanks! Your review was submitted.", ok: true });
      setRating(0);
      setReviewText("");
      await loadReviews();
    } catch (err) {
      setReviewMsg({ text: err.message, ok: false });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading || error || !event) {
    return <PageState loading={loading} error={error || (!event && "Event not found.")} />;
  }

  const hasMap = event.latitude != null && event.longitude != null;

  return (
    <>
      <Link to="/events" className="back-link">← Back to events</Link>

      <div className="details-hero card">
        {event.posterUrl && (
          <img className="details-poster" src={event.posterUrl} alt={event.title} />
        )}

        <div className="details-hero-text">
          <div className="event-card-tags">
            {categoryName && <span className="tag">{categoryName}</span>}
            <span className="tag tag-venue">{event.venueType}</span>
            {event.acceptVisitors && <span className="tag tag-open">Visitors welcome</span>}
          </div>

          <h1>{event.title}</h1>
          {collegeName && <p className="event-card-meta">🎓 {collegeName}</p>}

          {reviews?.averageRating != null && (
            <div className="details-rating">
              <StarRating value={reviews.averageRating} />
              <span>
                {reviews.averageRating} ({reviews.reviewCount} review
                {reviews.reviewCount === 1 ? "" : "s"})
              </span>
            </div>
          )}

          <button
            type="button"
            className={`btn ${saved ? "btn-primary" : "btn-outline"}`}
            onClick={toggleSave}
          >
            {saved ? "♥ Saved" : "♡ Save Event"}
          </button>
        </div>
      </div>

      <div className="details-layout">
        <div className="details-main">
          {event.description && (
            <section className="card details-section">
              <h2>About this event</h2>
              <p className="pre-wrap">{event.description}</p>
            </section>
          )}

          {event.rules && (
            <section className="card details-section">
              <h2>Rules</h2>
              <p className="pre-wrap">{event.rules}</p>
            </section>
          )}

          <section className="card details-section">
            <h2>Reviews</h2>

            {reviewState === "NOT_ENDED" && (
              <p className="muted">Reviews open once the event has ended.</p>
            )}
            {reviewState === "CLOSED" && (
              <p className="muted">The review window for this event has closed.</p>
            )}

            {reviewState === "OPEN" &&
              (loggedIn ? (
                <form className="review-form" onSubmit={handleReview}>
                  <label>Your rating</label>
                  <StarRating value={rating} onChange={setRating} size={28} />
                  <textarea
                    className="form-input"
                    rows="3"
                    placeholder="Share your experience (optional)"
                    value={reviewText}
                    onChange={(e) => setReviewText(e.target.value)}
                  />
                  {reviewMsg.text && (
                    <div className={`auth-message ${reviewMsg.ok ? "success" : ""}`}>
                      {reviewMsg.text}
                    </div>
                  )}
                  <button className="btn btn-primary" type="submit" disabled={submitting}>
                    {submitting ? "Submitting..." : "Submit Review"}
                  </button>
                </form>
              ) : (
                <p className="muted">
                  <Link to="/login" className="auth-link">Log in</Link> to review this event.
                </p>
              ))}

            <div className="review-list">
              {reviews?.reviews?.length ? (
                reviews.reviews.map((r) => (
                  <div className="review-item" key={r.reviewId}>
                    <div className="review-head">
                      <strong>{r.reviewerName}</strong>
                      <StarRating value={r.rating} size={16} />
                    </div>
                    {r.reviewText && <p>{r.reviewText}</p>}
                    <small className="muted">{formatDateTime(r.createdAt)}</small>
                  </div>
                ))
              ) : (
                <p className="muted">No reviews yet.</p>
              )}
            </div>
          </section>
        </div>

        <aside className="details-side">
          <section className="card details-section">
            <h2>When</h2>
            <p><strong>Starts:</strong><br />{formatDateTime(event.startDatetime)}</p>
            <p><strong>Ends:</strong><br />{formatDateTime(event.endDatetime)}</p>
          </section>

          <section className="card details-section">
            <h2>Where</h2>
            {event.venueName && <p><strong>{event.venueName}</strong></p>}
            {event.venueAddress && <p>{event.venueAddress}</p>}
            {!event.venueName && !event.venueAddress && (
              <p className="muted">{event.venueType}</p>
            )}

            {hasMap && (
              <>
                <iframe
                  className="details-map"
                  title="Event location"
                  loading="lazy"
                  src={`https://maps.google.com/maps?q=${event.latitude},${event.longitude}&z=15&output=embed`}
                />
                <a
                  className="auth-link"
                  target="_blank"
                  rel="noreferrer"
                  href={`https://www.google.com/maps?q=${event.latitude},${event.longitude}`}
                >
                  Open in Google Maps
                </a>
              </>
            )}
          </section>
        </aside>
      </div>
    </>
  );
}

export default EventDetails;