import { Link } from "react-router-dom";
import "../global.css";

function NotFound() {
  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card not-found">
        <div className="auth-brand">
          <h1>404</h1>
          <p>We couldn't find that page.</p>
        </div>

        <div className="auth-form">
          <Link className="auth-button pending-button" to="/">Back to Home</Link>
        </div>

        <div className="auth-footer">
          <Link className="auth-link" to="/events">Browse events</Link>
        </div>
      </main>
    </div>
  );
}

export default NotFound;