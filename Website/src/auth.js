export const ROLES = {
  NORMAL_USER: "NORMAL_USER",
  STUDENT_ORGANIZER: "STUDENT_ORGANIZER",
  EVENT_COORDINATOR: "EVENT_COORDINATOR",
  FACULTY_COORDINATOR: "FACULTY_COORDINATOR",
  COLLEGE_ADMIN: "COLLEGE_ADMIN",
  PLATFORM_ADMIN: "PLATFORM_ADMIN",
};

// Roles that can create events
export const EVENT_CREATOR_ROLES = [
  ROLES.STUDENT_ORGANIZER,
  ROLES.EVENT_COORDINATOR,
  ROLES.FACULTY_COORDINATOR,
];

// Roles that review lower-role applications
export const APPLICATION_REVIEWER_ROLES = [
  ROLES.PLATFORM_ADMIN,
  ROLES.COLLEGE_ADMIN,
  ROLES.FACULTY_COORDINATOR,
  ROLES.EVENT_COORDINATOR,
];

// Roles that review submitted events
export const EVENT_REVIEWER_ROLES = [
  ROLES.COLLEGE_ADMIN,
  ROLES.FACULTY_COORDINATOR,
  ROLES.EVENT_COORDINATOR,
];

export function getToken() {
  return localStorage.getItem("token");
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem("user"));
  } catch {
    return null;
  }
}

export function isLoggedIn() {
  return Boolean(getToken());
}

export function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
}

export function formatRole(role) {
  if (!role) return "";
  return role
    .split("_")
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(" ");
}
// Roles that manage a college (departments, organizations, users)
export const ADMIN_ROLES = [ROLES.COLLEGE_ADMIN, ROLES.PLATFORM_ADMIN];

// Where each role lands after login
export function defaultRouteFor(role) {
  return ADMIN_ROLES.includes(role) ? "/admin" : "/home";
}