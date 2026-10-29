const API_BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";

async function apiFetch(path, { method = "GET", body, auth = true } = {}) {
  const headers = {};
  const token = localStorage.getItem("token");

  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth && token) headers["Authorization"] = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error("Cannot reach the server. Is the backend running?");
  }

  // Expired / invalid token on a protected call -> force re-login
  if (response.status === 401 && auth && token) {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.location.href = "/login";
    throw new Error("Session expired. Please log in again.");
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      (data && typeof data === "object" && (data.message || data.error)) ||
      (typeof data === "string" && data) ||
      `Request failed (${response.status})`;
    throw new Error(message);
  }

  return data;
}

/* ---------- Auth ---------- */
export const loginUser = (email, password) =>
  apiFetch("/auth/login", { method: "POST", body: { email, password }, auth: false });
export const signupUser = (payload) =>
  apiFetch("/auth/signup", { method: "POST", body: payload, auth: false });
export const applyForRole = (payload) =>
  apiFetch("/auth/apply", { method: "POST", body: payload, auth: false });
export const verifyEmail = (payload) =>
  apiFetch("/auth/verify-email", { method: "POST", body: payload, auth: false });
export const getMe = () => apiFetch("/auth/me");
export const reviewApplication = (payload) =>
  apiFetch("/auth/applications/review", { method: "POST", body: payload });
export const setupPlatformAdmin = (payload) =>
  apiFetch("/setup/platform-admin", { method: "POST", body: payload, auth: false });

/* ---------- Lookups (public) ---------- */
export const getColleges = () => apiFetch("/colleges", { auth: false });
export const getDepartments = (collegeId) =>
  apiFetch(`/colleges/${collegeId}/departments`, { auth: false });
export const getOrganizations = (collegeId) =>
  apiFetch(`/colleges/${collegeId}/organizations`, { auth: false });
export const getCategories = () => apiFetch("/categories", { auth: false });

/* ---------- Events ---------- */
export const getEvents = () => apiFetch("/events", { auth: false });
export const getEvent = (id) => apiFetch(`/events/${id}`, { auth: false });
export const getEventReviews = (id) =>
  apiFetch(`/events/${id}/reviews`, { auth: false });
export const createEvent = (payload) =>
  apiFetch("/events", { method: "POST", body: payload });
export const updateEvent = (id, payload) =>
  apiFetch(`/events/${id}`, { method: "PUT", body: payload });
export const deleteEvent = (id) =>
  apiFetch(`/events/${id}`, { method: "DELETE" });
export const submitEvent = (id) =>
  apiFetch(`/events/${id}/submit`, { method: "POST" });
export const verifyEvent = (id, payload) =>
  apiFetch(`/events/${id}/verify`, { method: "POST", body: payload });
export const reviewEvent = (id, payload) =>
  apiFetch(`/events/${id}/review`, { method: "POST", body: payload });

/* ---------- User ---------- */
export const getMyEvents = () => apiFetch("/users/me/events");
export const getSavedEvents = () => apiFetch("/users/me/saved");
export const saveEvent = (id) =>
  apiFetch(`/events/${id}/save`, { method: "POST" });
export const unsaveEvent = (id) =>
  apiFetch(`/events/${id}/save`, { method: "DELETE" });
export const postReview = (id, rating, reviewText) =>
  apiFetch(`/events/${id}/reviews`, { method: "POST", body: { rating, reviewText } });

/* ---------- Admin ---------- */
export const getPendingApplications = () => apiFetch("/admin/applications/pending");
export const getPendingEvents = () => apiFetch("/admin/events/pending");
export const getUsers = ({ collegeId, role, status, search, limit = 20, offset = 0 } = {}) => {
  const params = new URLSearchParams();
  if (collegeId) params.set("collegeId", collegeId);
  if (role) params.set("role", role);
  if (status) params.set("status", status);
  if (search) params.set("search", search);
  params.set("limit", limit);
  params.set("offset", offset);
  return apiFetch(`/admin/users?${params.toString()}`);
};
export const deleteUser = (id) =>
  apiFetch(`/admin/users/${id}`, { method: "DELETE" });
export const userAction = (id, action) =>
  apiFetch(`/admin/users/${id}/${action}`, { method: "PATCH" });
export const addDepartment = (collegeId, payload) =>
  apiFetch(`/colleges/${collegeId}/departments`, { method: "POST", body: payload });
export const removeDepartment = (id) =>
  apiFetch(`/departments/${id}`, { method: "DELETE" });
export const addOrganization = (collegeId, payload) =>
  apiFetch(`/colleges/${collegeId}/organizations`, { method: "POST", body: payload });
export const removeOrganization = (id) =>
  apiFetch(`/organizations/${id}`, { method: "DELETE" });

/* ---------- File upload ---------- */
// Set to true once your friend's upload endpoint is live
export const UPLOADS_READY = false;

const UPLOAD_PATH = "/upload"; // change to your friend's real path

export async function uploadFile(file) {
  if (!file) return null;

  if (file.size > 5 * 1024 * 1024) {
    throw new Error(`${file.name} is larger than 5 MB`);
  }

  if (!UPLOADS_READY) return null; // skip until backend is ready

  const formData = new FormData();
  formData.append("file", file); // field name must match the backend

  const token = localStorage.getItem("token");

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${UPLOAD_PATH}`, {
      method: "POST",
      // No Content-Type here: the browser sets it with the multipart boundary
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    });
  } catch {
    throw new Error("Cannot reach the server. Is the backend running?");
  }

  if (!response.ok) throw new Error("File upload failed");

  const data = await response.json();
  return data.url; // assumes the backend returns { "url": "..." }
}