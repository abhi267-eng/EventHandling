function PageState({ loading, error, empty, emptyText = "Nothing here yet." }) {
  if (loading) return <div className="page-state">Loading...</div>;
  if (error) return <div className="page-state page-state-error">{error}</div>;
  if (empty) return <div className="page-state">{emptyText}</div>;
  return null;
}

export default PageState;