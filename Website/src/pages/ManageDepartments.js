import LookupManager from "../components/LookupManager";
import { getDepartments, addDepartment, removeDepartment } from "../api";

const FIELDS = [
  { key: "departmentName", label: "Department name", placeholder: "e.g. Artificial Intelligence & Machine Learning" },
  { key: "departmentCode", label: "Code", placeholder: "e.g. AIML" },
];

const buildPayload = (f) => ({
  departmentName: f.departmentName.trim(),
  departmentCode: f.departmentCode.trim().toUpperCase(),
});

function ManageDepartments() {
  return (
    <LookupManager
      title="Manage Departments"
      subtitle="Departments that students and faculty can choose when they apply."
      noun="department"
      load={getDepartments}
      add={addDepartment}
      remove={removeDepartment}
      idKey="departmentId"
      nameKey="departmentName"
      tagKey="departmentCode"
      fields={FIELDS}
      buildPayload={buildPayload}
    />
  );
}

export default ManageDepartments;