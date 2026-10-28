import LookupManager from "../components/LookupManager";
import { getOrganizations, addOrganization, removeOrganization } from "../api";

const FIELDS = [
  { key: "organizationName", label: "Organization name", placeholder: "e.g. NSS, Student Council" },
  {
    key: "organizationType",
    label: "Type",
    placeholder: "e.g. CLUB",
    suggestions: ["CLUB", "COUNCIL", "COMMITTEE", "SOCIETY"],
  },
];

const buildPayload = (f) => ({
  organizationName: f.organizationName.trim(),
  organizationType: f.organizationType.trim().toUpperCase(),
});

function ManageOrganizations() {
  return (
    <LookupManager
      title="Manage Organizations"
      subtitle="Clubs, councils and committees that organizers can belong to."
      noun="organization"
      load={getOrganizations}
      add={addOrganization}
      remove={removeOrganization}
      idKey="organizationId"
      nameKey="organizationName"
      tagKey="organizationType"
      fields={FIELDS}
      buildPayload={buildPayload}
    />
  );
}

export default ManageOrganizations;