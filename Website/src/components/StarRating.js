function StarRating({ value = 0, onChange, size = 20 }) {
  const rounded = Math.round(value);
  return (
    <span className="stars-rating" style={{ fontSize: size }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span
          key={n}
          className={`star ${n <= rounded ? "filled" : ""} ${onChange ? "clickable" : ""}`}
          onClick={onChange ? () => onChange(n) : undefined}
          role={onChange ? "button" : undefined}
          aria-label={onChange ? `${n} star${n > 1 ? "s" : ""}` : undefined}
        >
          ★
        </span>
      ))}
    </span>
  );
}

export default StarRating;