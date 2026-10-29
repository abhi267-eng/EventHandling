import { useEffect, useMemo, useState } from "react";
import {
  getEvents,
  getCategories,
  getColleges,
  getSavedEvents,
  saveEvent,
  unsaveEvent,
} from "../api";
import { isLoggedIn } from "../auth";
import EventCard from "../components/EventCard";
import PageState from "../components/PageState";
import { useToast } from "../components/Feedback";

function EventList() {
  const toast = useToast();
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState([]);
  const [colleges, setColleges] = useState([]);
  const [savedIds, setSavedIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [search, setSearch] = useState("");
  const [collegeId, setCollegeId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [venueType, setVenueType] = useState("");
  const [upcomingOnly, setUpcomingOnly] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [ev, cat, col] = await Promise.all([
          getEvents(),
          getCategories(),
          getColleges(),
        ]);
        setEvents(Array.isArray(ev) ? ev : []);
        setCategories(Array.isArray(cat) ? cat : []);
        setColleges(Array.isArray(col) ? col : []);

        if (isLoggedIn()) {
          try {
            const saved = await getSavedEvents();
            setSavedIds(new Set((saved || []).map((e) => (e.event || e).eventId)));
          } catch {
            /* saved list is optional */
          }
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const categoryMap = useMemo(
    () => Object.fromEntries(categories.map((c) => [c.categoryId, c.categoryName])),
    [categories]
  );
  const collegeMap = useMemo(
    () => Object.fromEntries(colleges.map((c) => [c.collegeId, c.collegeName])),
    [colleges]
  );

  const venueTypes = useMemo(
    () => [...new Set(events.map((e) => e.venueType).filter(Boolean))],
    [events]
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    const now = new Date();
    return events
      .filter((e) => {
        if (q && !`${e.title} ${e.description || ""} ${e.venueName || ""}`.toLowerCase().includes(q))
          return false;
        if (collegeId && String(e.collegeId) !== collegeId) return false;
        if (categoryId && String(e.categoryId) !== categoryId) return false;
        if (venueType && e.venueType !== venueType) return false;
        if (upcomingOnly && new Date(e.endDatetime) < now) return false;
        return true;
      })
      .sort((a, b) => new Date(a.startDatetime) - new Date(b.startDatetime));
  }, [events, search, collegeId, categoryId, venueType, upcomingOnly]);

  async function toggleSave(eventId) {
    if (!isLoggedIn()) {
      window.location.href = "/login";
      return;
    }
    const wasSaved = savedIds.has(eventId);
    // optimistic update
    setSavedIds((prev) => {
      const next = new Set(prev);
      wasSaved ? next.delete(eventId) : next.add(eventId);
      return next;
    });
    try {
      wasSaved ? await unsaveEvent(eventId) : await saveEvent(eventId);
    } catch (err) {
      // roll back
      setSavedIds((prev) => {
        const next = new Set(prev);
        wasSaved ? next.add(eventId) : next.delete(eventId);
        return next;
      });
      toast.error(err.message);
    }
  }

  function clearFilters() {
    setSearch("");
    setCollegeId("");
    setCategoryId("");
    setVenueType("");
    setUpcomingOnly(true);
  }

  return (
    <>
      <div className="page-header">
        <h1>Browse Events</h1>
        <p>Discover what's happening across colleges.</p>
      </div>

      <div className="filter-bar card">
        <input
          className="form-input"
          type="text"
          placeholder="Search events..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />

        <select className="form-input" value={collegeId} onChange={(e) => setCollegeId(e.target.value)}>
          <option value="">All colleges</option>
          {colleges.map((c) => (
            <option key={c.collegeId} value={c.collegeId}>{c.collegeName}</option>
          ))}
        </select>

        <select className="form-input" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.categoryId} value={c.categoryId}>{c.categoryName}</option>
          ))}
        </select>

        <select className="form-input" value={venueType} onChange={(e) => setVenueType(e.target.value)}>
          <option value="">All venue types</option>
          {venueTypes.map((v) => (
            <option key={v} value={v}>{v}</option>
          ))}
        </select>

        <label className="filter-check">
          <input
            type="checkbox"
            checked={upcomingOnly}
            onChange={(e) => setUpcomingOnly(e.target.checked)}
          />
          Upcoming only
        </label>

        <button type="button" className="btn btn-outline" onClick={clearFilters}>
          Clear
        </button>
      </div>

      <PageState
        loading={loading}
        error={error}
        empty={!loading && !error && filtered.length === 0}
        emptyText="No events match your filters."
      />

      {!loading && !error && filtered.length > 0 && (
        <div className="grid">
          {filtered.map((event) => (
            <EventCard
              key={event.eventId}
              event={event}
              categoryName={categoryMap[event.categoryId]}
              collegeName={collegeMap[event.collegeId]}
              saved={savedIds.has(event.eventId)}
              onToggleSave={toggleSave}
            />
          ))}
        </div>
      )}
    </>
  );
}

export default EventList;