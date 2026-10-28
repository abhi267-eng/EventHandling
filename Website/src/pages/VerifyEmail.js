import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { verifyEmail } from "../api";
import "../global.css";

function VerifyEmail() {
  const navigate = useNavigate();
  const location = useLocation();

  const userId = location.state?.userId;
  const email = location.state?.email;

  const [code, setCode] = useState("");
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleVerify(event) {
    event.preventDefault();
    setMessage("");
    setLoading(true);

    try {
      await verifyEmail({ userId, code: code.trim() });

      setSuccess(true);
      setMessage("Email verified! Redirecting to login...");
      setTimeout(
        () => navigate(location.state?.isApplication ? "/pending" : "/login"),
        1500);
    } catch (error) {
      setSuccess(false);
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>Verify Email</h1>
          <p>
            {email
              ? `Enter the 6-digit code sent to ${email}`
              : "Verify your EventBook account"}
          </p>
        </div>

        {!userId ? (
          <div className="auth-message">
            We couldn't find your registration. Please sign up again.
          </div>
        ) : (
          <form className="auth-form" onSubmit={handleVerify}>
            <div className="form-group">
              <label htmlFor="otp">Verification Code</label>
              <input
                id="otp"
                className="form-input"
                type="text"
                inputMode="numeric"
                placeholder="Enter 6-digit code"
                maxLength="6"
                value={code}
                onChange={(event) => setCode(event.target.value)}
                required
              />
            </div>

            {message && (
              <div className={`auth-message ${success ? "success" : ""}`}>
                {message}
              </div>
            )}

            <button className="auth-button" type="submit" disabled={loading}>
              {loading ? "Verifying..." : "Verify Email"}
            </button>
          </form>
        )}

        <div className="auth-footer">
          <Link className="auth-link" to={userId ? "/login" : "/signup"}>
            {userId ? "Back to Login" : "Go to Signup"}
          </Link>
        </div>
      </main>
    </div>
  );
}

export default VerifyEmail;