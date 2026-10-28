import { useCallback, useEffect, useState } from "react";
import useCollegeScope from "../hooks/useCollegeScope";
import { useToast, useConfirm } from "./Feedback";
import PageState from "./PageState";

function LookupManager({
  title,
  subtitle,
  noun,        // "department" | "organization"
  load,        // (collegeId) => Promise<list>
  add,         // (collegeId, payload) => Promise
  remove,      // (id) => Promise
  idKey,
  nameKey,
  tagKey,
  fields,      // [{ key, label, placeholder, suggestions? }]
  buildPayload // (formValues) => payload
}) {
  const toast = useToast();
  const confirm = useConfirm();
  const scope = useCollegeScope();

  const emptyForm = Object.fromEntries(fields.map((f) => [f.key, ""]));

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState(null);

  const collegeId = scope.collegeId ? Number(scope.collegeId) : null;

  const reload = useCallback(async () => {
    if (!collegeId) {
      setItems([]);
      return;
    }
    setLoading(true);
    setError("");
    try {
      setItems((await load(collegeId)) || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [collegeId, load]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function handleAdd(e) {
    e.preventDefault();
    if (!collegeId) return;
    setSaving(true);
    try {
      await add(collegeId, buildPayload(form));
      toast.success(`Added ${noun}.`);
      setForm(emptyForm);
      await reload();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleRemove(item) {
    const ok = await confirm({
      title: `Delete ${noun}?`,
      message: `"${item[nameKey]}" will be removed. People already linked to it may be affected.`,
      confirmText: "Delete",
      danger: true,
    });
    if (!ok) return;

    setBusyId(item[idKey]);
    try {
      await remove(item[idKey]);
      setItems((prev) => prev.filter((i) => i[idKey] !== item[idKey]));
      toast.success(`Deleted ${noun}.`);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusyId(null);
    }
  }

  const notLinked = !scope.loading && !scope.isPlatform && !scope.collegeId;

  return (
    <>
      <div className="page-header">
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>

      {scope.isPlatform && (
        <div className="scope-bar">
          <label htmlFor="scopeCollege">College</label>
          <select
            id="scopeCollege"
            className="form-input"
            value={scope.collegeId}
            onChange={(e) => scope.setCollegeId(e.target.value)}
          >
            <option value="">Select a college</option>
            {scope.colleges.map((c) => (
              <option key={c.collegeId} value={c.collegeId}>
                {c.collegeName}
              </option>
            ))}
          </select>
        </div>
      )}

      <PageState
        loading={scope.loading || loading}
        error={scope.error || error || (notLinked && "Your account isn't linked to a college.")}
      />

      {collegeId && (
        <>
          <form className="card add-form" onSubmit={handleAdd}>
            {fields.map((f) => (
              <div className="form-group" key={f.key}>
                <label htmlFor={`add-${f.key}`}>{f.label}</label>
                <input
                  id={`add-${f.key}`}
                  className="form-input"
                  type="text"
                  list={f.suggestions ? `${f.key}-options` : undefined}
                  placeholder={f.placeholder}
                  value={form[f.key]}
                  onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
                  required
                />
                {f.suggestions && (
                  <datalist id={`${f.key}-options`}>
                    {f.suggestions.map((s) => (
                      <option key={s} value={s} />
                    ))}
                  </datalist>
                )}
              </div>
            ))}
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? "Adding..." : `+ Add ${noun}`}
            </button>
          </form>

          {!loading && !error && items.length === 0 && (
            <div className="page-state">No {noun}s yet. Add the first one above.</div>
          )}

          <div className="lookup-list">
            {items.map((item) => (
              <div className="card lookup-row" key={item[idKey]}>
                <div>
                  <strong>{item[nameKey]}</strong>{" "}
                  <span className="tag">{item[tagKey]}</span>
                </div>
                <button
                  type="button"
                  className="btn btn-danger btn-sm"
                  disabled={busyId === item[idKey]}
                  onClick={() => handleRemove(item)}
                >
                  Delete
                </button>
              </div>
            ))}
          </div>
        </>
      )}
    </>
  );
}

export default LookupManager;