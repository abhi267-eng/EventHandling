import { Link } from "react-router-dom";

function VerifyEmail() {
  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>Verify Email</h1>
          <p>Verify your EventHandling account</p>
        </div>

        <div className="auth-form">
          <div className="form-group">
            <label htmlFor="otp">Verification Code</label>

            <input
              id="otp"
              className="form-input"
              type="text"
              placeholder="Enter 6-digit code"
              maxLength="6"
            />
          </div>

          <button className="auth-button" type="button">
            Verify Email
          </button>
        </div>

        <div className="auth-footer">
          <Link className="auth-link" to="/">
            Back to Login
          </Link>
        </div>
      </main>
    </div>
  );
}

export default VerifyEmail;