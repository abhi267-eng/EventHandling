import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  EVENT_CREATOR_ROLES,
  APPLICATION_REVIEWER_ROLES,
  EVENT_REVIEWER_ROLES,
  getUser,
  isLoggedIn,
  logout,
  formatRole,
  ADMIN_ROLES,
} from "../auth";

function Navbar() {
  const navigate = useNavigate();
  const loggedIn = isLoggedIn();
  const user = getUser();
  const role = user?.role;

  function handleLogout() {
    logout();
    navigate("/");
  }

  // Build the link list from the role
  const links = [{ to: "/events", label: "Events" }];

  if (loggedIn) {
    links.push({ to: "/home", label: "Dashboard" });
    links.push({ to: "/saved", label: "Saved" });

    if (EVENT_CREATOR_ROLES.includes(role)) {
      links.push({ to: "/my-events", label: "My Events" });
      links.push({ to: "/events/new", label: "Create Event" });
    }
    if (APPLICATION_REVIEWER_ROLES.includes(role)) {
      links.push({ to: "/applications", label: "Applications" });
    }
    if (EVENT_REVIEWER_ROLES.includes(role)) {
      links.push({ to: "/review-events", label: "Review Events" });
    }
    if (ADMIN_ROLES.includes(role)) {
      links.push({ to: "/admin", label: "Admin" });
    }
  }

  return (
    <header className="navbar">
      <Link to="/" className="navbar-logo">
        Event<span>Book</span>
      </Link>

      <nav className="navbar-links">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end
            className={({ isActive }) =>
              `navbar-link ${isActive ? "active" : ""}`
            }
          >
            {link.label}
          </NavLink>
        ))}
      </nav>

      <div className="navbar-actions">
        {loggedIn ? (
          <>
            <Link to="/profile" className="navbar-user">
              <strong>{user?.name || "User"}</strong>
              <small>{formatRole(role)}</small>
            </Link>
            <button className="btn btn-outline" onClick={handleLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn btn-outline">Login</Link>
            <Link to="/signup" className="btn btn-primary">Signup</Link>
          </>
        )}
      </div>
    </header>
  );
}

export default Navbar;