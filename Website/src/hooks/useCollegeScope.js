import { useEffect, useState } from "react";
import { getColleges, getMe } from "../api";
import { ROLES, getUser } from "../auth";

// allowAll: platform admins may leave the college empty ("all colleges")
function useCollegeScope({ allowAll = false } = {}) {
  const isPlatform = getUser()?.role === ROLES.PLATFORM_ADMIN;

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [colleges, setColleges] = useState([]);
  const [myCollegeId, setMyCollegeId] = useState(null);
  const [selected, setSelected] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        if (isPlatform) {
          const list = (await getColleges()) || [];
          if (cancelled) return;
          setColleges(list);
          if (!allowAll && list.length > 0) setSelected(String(list[0].collegeId));
        } else {
          const me = await getMe();
          if (cancelled) return;
          setMyCollegeId(me.profile.collegeId ?? null);
        }
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [isPlatform, allowAll]);

  const collegeId = isPlatform ? selected : myCollegeId ? String(myCollegeId) : "";

  return { isPlatform, loading, error, colleges, collegeId, setCollegeId: setSelected };
}

export default useCollegeScope;