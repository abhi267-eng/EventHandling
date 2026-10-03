import { useNavigate } from "react-router-dom";

function Home() {
  const navigate = useNavigate();

  const user = JSON.parse(localStorage.getItem("user"));

  function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    navigate("/");
  }

  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>Welcome to EventHandling</h1>

          <p>
            Hello, {user?.name || "User"}!
          </p>
        </div>

        <div className="auth-footer">
          Role: {user?.role || "NORMAL_USER"}
        </div>

        <button
          className="auth-button"
          type="button"
          onClick={handleLogout}
        >
          Logout
        </button>
      </main>
    </div>
  );
}

export default Home;