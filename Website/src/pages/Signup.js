import { useState } from "react";
import { Link } from "react-router-dom";

function Signup() {
  const [role, setRole] = useState("");

  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>New Registration</h1>
          <p>Create your EventHandling account</p>
        </div>

        <form className="auth-form">
          <div className="form-group">
            <label htmlFor="role">Account Type</label>

            <select
              id="role"
              className="form-input"
              value={role}
              onChange={(event) => setRole(event.target.value)}
              required
            >
              <option value="">Select account type</option>

              <option value="NORMAL_USER">
                Normal User
              </option>

              <option value="STUDENT_ORGANIZER">
                Student Organizer
              </option>

              <option value="EVENT_COORDINATOR">
                Event Coordinator
              </option>

              <option value="FACULTY_COORDINATOR">
                Faculty Coordinator
              </option>

              <option value="COLLEGE_ADMIN">
                College Admin
              </option>
            </select>
          </div>

          <button className="auth-button" type="submit">
            Continue
          </button>
        </form>

        <div className="auth-footer">
          Already have an account?{" "}
          <Link className="auth-link" to="/">
            Back to Login
          </Link>
        </div>
      </main>
    </div>
  );
}

export default Signup;