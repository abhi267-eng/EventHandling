import { Link } from "react-router-dom";

function StatCard({ label, value, to }) {
  const content = (
    <>
      <span className="stat-value">{value ?? "–"}</span>
      <span className="stat-label">{label}</span>
    </>
  );

  return to ? (
    <Link to={to} className="card stat-card">{content}</Link>
  ) : (
    <div className="card stat-card">{content}</div>
  );
}

export default StatCard;