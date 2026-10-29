import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { setupPlatformAdmin } from "../api";
import "../global.css";

function PlatformSetup() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    setupKey: "",
  });
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const setField = (name) => (e) => setForm((p) => ({ ...p, [name]: e.target.value }));

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");
    setSuccess(false);

    if (form.password.length < 8) {
      setMessage("Password must be at least 8 characters.");
      return;
    }
    if (form.password !== form.confirmPassword) {
      setMessage("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
      };
      if (form.setupKey.trim()) payload.setupKey = form.setupKey.trim();

      await setupPlatformAdmin(payload);

      setSuccess(true);
      setMessage("Platform admin created! Redirecting to login...");
      setTimeout(() => navigate("/login"), 1800);
    } catch (err) {
      setMessage(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>Platform Setup</h1>
          <p>Create the first Platform Admin account</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Full Name</label>
            <input id="name" className="form-input" type="text" value={form.name}
              onChange={setField("name")} placeholder="Enter your full name" required />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input id="email" className="form-input" type="email" value={form.email}
              onChange={setField("email")} placeholder="Enter your email" required />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input id="password" className="form-input" type="password" value={form.password}
              onChange={setField("password")} placeholder="Min 8 characters" required />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input id="confirmPassword" className="form-input" type="password"
              value={form.confirmPassword} onChange={setField("confirmPassword")}
              placeholder="Confirm your password" required />
          </div>

          <div className="form-group">
            <label htmlFor="setupKey">Setup key (only if your backend asks for one)</label>
            <input id="setupKey" className="form-input" type="password" value={form.setupKey}
              onChange={setField("setupKey")} placeholder="Optional" />
          </div>

          {message && (
            <div className={`auth-message ${success ? "success" : ""}`}>{message}</div>
          )}

          <button className="auth-button" type="submit" disabled={loading}>
            {loading ? "Creating..." : "Create Platform Admin"}
          </button>
        </form>

        <div className="auth-footer">
          <Link className="auth-link" to="/login">Back to Login</Link>
        </div>
      </main>
    </div>
  );
}

export default PlatformSetup;