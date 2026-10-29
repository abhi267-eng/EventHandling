import { Link } from "react-router-dom";
import "../global.css";

function Pending() {
  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>Application Received</h1>
          <p>Your application is waiting for approval</p>
        </div>

        <div className="auth-form">
          <div className="auth-message success">
            Thanks for applying! You'll be able to log in once a reviewer approves it.
          </div>

          <p className="pending-text">
            Student Organizers are reviewed by a Faculty or Event Coordinator.
            Coordinators are reviewed by their College Admin, and College Admins by
            the Platform Admin.
          </p>

          <Link className="auth-button pending-button" to="/login">Go to Login</Link>
        </div>

        <div className="auth-footer">
          <Link className="auth-link" to="/events">Browse events meanwhile</Link>
        </div>
      </main>
    </div>
  );
}

export default Pending;