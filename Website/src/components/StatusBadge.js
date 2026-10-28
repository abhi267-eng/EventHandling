function StatusBadge({ status }) {
  if (!status) return null;
  const cls = status.toLowerCase().replace(/_/g, "-");
  return (
    <span className={`status-badge status-${cls}`}>
      {status.replace(/_/g, " ")}
    </span>
  );
}

export default StatusBadge;