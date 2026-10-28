import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createEvent, updateEvent, getEvent, getCategories, getMe } from "../api";
import { getUser } from "../auth";
import PageState from "../components/PageState";

const VENUE_TYPES = ["OFFLINE", "ONLINE", "HYBRID"];

const EMPTY = {
  title: "",
  description: "",
  categoryId: "",
  rules: "",
  startDatetime: "",
  endDatetime: "",
  venueType: "OFFLINE",
  venueName: "",
  venueAddress: "",
  latitude: "",
  longitude: "",
  acceptVisitors: false,
  posterUrl: "",
};

// "2026-11-02 10:00:00" or "2026-11-02T10:00:00" -> "2026-11-02T10:00" (datetime-local)
const toInputValue = (v) => (v ? v.replace(" ", "T").slice(0, 16) : "");
// datetime-local gives "2026-11-02T10:00" -> add seconds for the backend
const toApiValue = (v) => (v && v.length === 16 ? `${v}:00` : v);

function EventForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY);
  const [categories, setCategories] = useState([]);
  const [collegeId, setCollegeId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const [cats, me] = await Promise.all([getCategories(), getMe()]);
        setCategories(cats || []);
        setCollegeId(me.profile.collegeId);

        if (isEdit) {
          const ev = await getEvent(id);
          setCollegeId(ev.collegeId);
          setForm({
            title: ev.title || "",
            description: ev.description || "",
            categoryId: ev.categoryId ?? "",
            rules: ev.rules || "",
            startDatetime: toInputValue(ev.startDatetime),
            endDatetime: toInputValue(ev.endDatetime),
            venueType: ev.venueType || "OFFLINE",
            venueName: ev.venueName || "",
            venueAddress: ev.venueAddress || "",
            latitude: ev.latitude ?? "",
            longitude: ev.longitude ?? "",
            acceptVisitors: Boolean(ev.acceptVisitors),
            posterUrl: ev.posterUrl || "",
          });
        }
      } catch (err) {
        setLoadError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id, isEdit]);

  function set(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function useMyLocation() {
    if (!navigator.geolocation) {
      setMessage("Your browser doesn't support location.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        set("latitude", pos.coords.latitude.toFixed(6));
        set("longitude", pos.coords.longitude.toFixed(6));
      },
      () => setMessage("Couldn't get your location. Enter coordinates manually."),
    );
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");

    if (!collegeId) {
      setMessage("Your account isn't linked to a college, so you can't create events.");
      return;
    }
    if (new Date(form.endDatetime) <= new Date(form.startDatetime)) {
      setMessage("End date/time must be after the start date/time.");
      return;
    }

    const lat = form.latitude === "" ? null : Number(form.latitude);
    const lng = form.longitude === "" ? null : Number(form.longitude);
    if ((lat !== null && isNaN(lat)) || (lng !== null && isNaN(lng))) {
      setMessage("Latitude and longitude must be numbers.");
      return;
    }

    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      collegeId: Number(collegeId),
      createdBy: getUser()?.userId ?? null,
      categoryId: form.categoryId === "" ? null : Number(form.categoryId),
      rules: form.rules.trim() || null,
      startDatetime: toApiValue(form.startDatetime),
      endDatetime: toApiValue(form.endDatetime),
      venueType: form.venueType,
      venueName: form.venueName.trim() || null,
      venueAddress: form.venueAddress.trim() || null,
      latitude: form.venueType === "ONLINE" ? null : lat,
      longitude: form.venueType === "ONLINE" ? null : lng,
      acceptVisitors: form.acceptVisitors,
      posterUrl: form.posterUrl.trim() || null,
    };

    setSaving(true);
    try {
      if (isEdit) {
        await updateEvent(id, payload);
      } else {
        await createEvent(payload);
      }
      navigate("/my-events");
    } catch (err) {
      setMessage(err.message);
    } finally {
      setSaving(false);
    }
  }

  if (loading || loadError) {
    return <PageState loading={loading} error={loadError} />;
  }

  const isOnline = form.venueType === "ONLINE";

  return (
    <>
      <Link to="/my-events" className="back-link">← Back to my events</Link>

      <div className="page-header">
        <h1>{isEdit ? "Edit Event" : "Create Event"}</h1>
        <p>
          {isEdit
            ? "Update your event details."
            : "Fill in the details. It is saved as a draft; submit it for review from My Events."}
        </p>
      </div>

      <form className="card event-form" onSubmit={handleSubmit}>
        <h2 className="form-section-title">Basics</h2>

        <div className="form-group">
          <label htmlFor="title">Title *</label>
          <input
            id="title"
            className="form-input"
            type="text"
            value={form.title}
            onChange={(e) => set("title", e.target.value)}
            placeholder="e.g. Annual Tech Fest 2026"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            className="form-input"
            rows="4"
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            placeholder="What is this event about?"
          />
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="category">Category</label>
            <select
              id="category"
              className="form-input"
              value={form.categoryId}
              onChange={(e) => set("categoryId", e.target.value)}
            >
              <option value="">Select a category</option>
              {categories.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>
                  {c.categoryName}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="poster">Poster image URL</label>
            <input
              id="poster"
              className="form-input"
              type="url"
              value={form.posterUrl}
              onChange={(e) => set("posterUrl", e.target.value)}
              placeholder="https://..."
            />
          </div>
        </div>

        {form.posterUrl && (
          <img className="poster-preview" src={form.posterUrl} alt="Poster preview" />
        )}

        <div className="form-group">
          <label htmlFor="rules">Rules</label>
          <textarea
            id="rules"
            className="form-input"
            rows="3"
            value={form.rules}
            onChange={(e) => set("rules", e.target.value)}
            placeholder="Eligibility, team size, code of conduct..."
          />
        </div>

        <h2 className="form-section-title">Date &amp; time</h2>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="start">Starts *</label>
            <input
              id="start"
              className="form-input"
              type="datetime-local"
              value={form.startDatetime}
              onChange={(e) => set("startDatetime", e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="end">Ends *</label>
            <input
              id="end"
              className="form-input"
              type="datetime-local"
              value={form.endDatetime}
              min={form.startDatetime || undefined}
              onChange={(e) => set("endDatetime", e.target.value)}
              required
            />
          </div>
        </div>

        <h2 className="form-section-title">Venue</h2>

        <div className="form-group">
          <label htmlFor="venueType">Venue type *</label>
          <select
            id="venueType"
            className="form-input"
            value={form.venueType}
            onChange={(e) => set("venueType", e.target.value)}
          >
            {VENUE_TYPES.map((v) => (
              <option key={v} value={v}>{v}</option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="venueName">
              {isOnline ? "Platform (e.g. Google Meet)" : "Venue name"}
            </label>
            <input
              id="venueName"
              className="form-input"
              type="text"
              value={form.venueName}
              onChange={(e) => set("venueName", e.target.value)}
            />
          </div>
          <div className="form-group">
            <label htmlFor="venueAddress">
              {isOnline ? "Meeting link" : "Address"}
            </label>
            <input
              id="venueAddress"
              className="form-input"
              type="text"
              value={form.venueAddress}
              onChange={(e) => set("venueAddress", e.target.value)}
            />
          </div>
        </div>

        {!isOnline && (
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="lat">Latitude</label>
              <input
                id="lat"
                className="form-input"
                type="text"
                inputMode="decimal"
                value={form.latitude}
                onChange={(e) => set("latitude", e.target.value)}
                placeholder="e.g. 19.0760"
              />
            </div>
            <div className="form-group">
              <label htmlFor="lng">Longitude</label>
              <input
                id="lng"
                className="form-input"
                type="text"
                inputMode="decimal"
                value={form.longitude}
                onChange={(e) => set("longitude", e.target.value)}
                placeholder="e.g. 72.8777"
              />
            </div>
            <button
              type="button"
              className="btn btn-outline location-button"
              onClick={useMyLocation}
            >
              Use my location
            </button>
          </div>
        )}

        <label className="filter-check visitors-check">
          <input
            type="checkbox"
            checked={form.acceptVisitors}
            onChange={(e) => set("acceptVisitors", e.target.checked)}
          />
          Accept visitors from other colleges
        </label>

        {message && <div className="auth-message">{message}</div>}

        <div className="form-actions">
          <Link to="/my-events" className="btn btn-outline">Cancel</Link>
          <button className="btn btn-primary" type="submit" disabled={saving}>
            {saving ? "Saving..." : isEdit ? "Save Changes" : "Create Event"}
          </button>
        </div>
      </form>
    </>
  );
}

export default EventForm;