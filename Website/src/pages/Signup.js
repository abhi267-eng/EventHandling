import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import {
  getColleges,
  getDepartments,
  getOrganizations,
  signupUser,
  applyForRole,
  uploadFile,
  UPLOADS_READY,
} from "../api";
import "../global.css";

/* ---------- small helpers ---------- */

function TextField({ id, label, ...props }) {
  return (
    <div className="form-group">
      <label htmlFor={id}>{label}</label>
      <input id={id} className="form-input" {...props} />
    </div>
  );
}

function SelectField({ id, label, options, placeholder, ...props }) {
  return (
    <div className="form-group">
      <label htmlFor={id}>{label}</label>
      <select id={id} className="form-select" {...props}>
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}

const emptyToNull = (value) => (value === "" || value === undefined ? null : value);
const toNumber = (value) => (value ? Number(value) : null);

const INITIAL_FORM = {
  name: "",
  email: "",
  phone: "",
  password: "",
  confirmPassword: "",
  collegeId: "",
  departmentId: "",
  organizationId: "",
  collegeIdNumber: "",
  designation: "",
  // college admin: new college
  collegeName: "",
  collegeCode: "",
  emailDomain: "",
  address: "",
  city: "",
  state: "",
  pincode: "",
  website: "",
};

function Signup() {
  const navigate = useNavigate();

  const [role, setRole] = useState("");
  const [collegeOption, setCollegeOption] = useState("");
  const [form, setForm] = useState(INITIAL_FORM);
  const [files, setFiles] = useState({
    profilePicture: null,
    idPhoto: null,
    collegeLogo: null,
  });

  const [colleges, setColleges] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [organizations, setOrganizations] = useState([]);

  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  /* which fields each role needs */
  const isNormal = role === "NORMAL_USER";
  const isAdmin = role === "COLLEGE_ADMIN";
  const isNewCollege = isAdmin && collegeOption === "NEW_COLLEGE";
  const needsDepartment = role === "STUDENT_ORGANIZER" || role === "FACULTY_COORDINATOR";
  const needsOrganization = role === "STUDENT_ORGANIZER" || role === "EVENT_COORDINATOR";
  const needsDesignation = ["EVENT_COORDINATOR", "FACULTY_COORDINATOR", "COLLEGE_ADMIN"].includes(role);
  const needsStudentId = role === "STUDENT_ORGANIZER" || role === "EVENT_COORDINATOR" || role === "FACULTY_COORDINATOR";
  const showCollegeSelect = role && (!isAdmin || collegeOption === "EXISTING_COLLEGE");

  const setField = (name) => (event) =>
    setForm((previous) => ({ ...previous, [name]: event.target.value }));

  const setFile = (name) => (event) =>
    setFiles((previous) => ({ ...previous, [name]: event.target.files[0] || null }));

  /* load colleges once */
  useEffect(() => {
    getColleges()
      .then(setColleges)
      .catch(() => setMessage("Could not load colleges. Is the backend running?"));
  }, []);

  /* load departments + organizations when the college changes */
  useEffect(() => {
    if (!form.collegeId) {
      setDepartments([]);
      setOrganizations([]);
      return;
    }

    let cancelled = false;

    Promise.all([getDepartments(form.collegeId), getOrganizations(form.collegeId)])
      .then(([departmentList, organizationList]) => {
        if (cancelled) return;
        setDepartments(departmentList);
        setOrganizations(organizationList);
      })
      .catch(() => {
        if (!cancelled) setMessage("Could not load departments and organizations.");
      });

    return () => {
      cancelled = true;
    };
  }, [form.collegeId]);

  function handleRoleChange(event) {
    setRole(event.target.value);
    setCollegeOption("");
    setMessage("");
    setForm((previous) => ({
      ...previous,
      collegeId: "",
      departmentId: "",
      organizationId: "",
      designation: "",
    }));
  }

  function handleCollegeChange(event) {
    const collegeId = event.target.value;
    setForm((previous) => ({
      ...previous,
      collegeId,
      departmentId: "",
      organizationId: "",
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
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
      // 1. Upload files first (returns null for now while UPLOADS_READY is false)
      const profilePictureUrl = await uploadFile(files.profilePicture);
      const collegeIdPhotoUrl = await uploadFile(files.idPhoto);
      const proposedLogoUrl = isNewCollege ? await uploadFile(files.collegeLogo) : null;

      // 2. Fields shared by both requests
      const base = {
        name: form.name.trim(),
        email: form.email.trim(),
        phone: emptyToNull(form.phone.trim()),
        password: form.password,
        profilePictureUrl,
        collegeId: toNumber(form.collegeId),
        collegeIdNumber: emptyToNull(form.collegeIdNumber.trim()),
        collegeIdPhotoUrl,
      };

      let data;

      if (isNormal) {
        data = await signupUser(base); // POST /auth/signup
      } else {
        data = await applyForRole({
          // POST /auth/apply
          ...base,
          role,
          designation: emptyToNull(form.designation.trim()),
          departmentId: needsDepartment ? toNumber(form.departmentId) : null,
          organizationId: needsOrganization ? toNumber(form.organizationId) : null,

          proposedCollegeName: isNewCollege ? form.collegeName.trim() : null,
          proposedCollegeCode: isNewCollege ? form.collegeCode.trim() : null,
          proposedEmailDomain: isNewCollege
            ? form.emailDomain.trim().replace(/^@/, "")
            : null,
          proposedAddress: isNewCollege
            ? [form.address, form.city, form.state, form.pincode]
                .map((part) => part.trim())
                .filter(Boolean)
                .join(", ")
            : null,
          proposedCity: isNewCollege ? form.city.trim() : null,
          proposedWebsite: isNewCollege ? emptyToNull(form.website.trim()) : null,
          proposedLogoUrl,
        });
      }

      // 3. Go to email verification (it needs the userId from the response)
      const userId = data?.userId ?? data?.user?.userId;

      if (userId) {
        navigate("/verify-email", { state: { userId, email: base.email, isApplication: !isNormal } });
      } else {
        setSuccess(true);
        setMessage(
          isNormal
            ? "Account created! Please log in."
            : "Application submitted! You can log in once it is approved."
        );
        setTimeout(() => navigate(isNormal ? "/login" : "/pending"), 2500);
      }
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>New Registration</h1>
          <p>Create your EventBook account</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          {/* ACCOUNT TYPE */}
          <SelectField
            id="role"
            label="Account Type"
            placeholder="Select account type"
            value={role}
            onChange={handleRoleChange}
            required
            options={[
              { value: "NORMAL_USER", label: "Normal User" },
              { value: "STUDENT_ORGANIZER", label: "Student Organizer" },
              { value: "EVENT_COORDINATOR", label: "Event Coordinator" },
              { value: "FACULTY_COORDINATOR", label: "Faculty Coordinator" },
              { value: "COLLEGE_ADMIN", label: "College Admin" },
            ]}
          />

          {role && (
            <>
              {/* BASIC DETAILS */}
              <TextField
                id="profilePicture"
                label="Profile Picture (optional)"
                type="file"
                accept="image/*"
                onChange={setFile("profilePicture")}
              />

              <TextField
                id="fullName"
                label="Full Name"
                type="text"
                placeholder="Enter your full name"
                value={form.name}
                onChange={setField("name")}
                required
              />

              <TextField
                id="email"
                label="Email Address"
                type="email"
                placeholder="Enter your email"
                value={form.email}
                onChange={setField("email")}
                required
              />

              <TextField
                id="phone"
                label="Phone Number"
                type="tel"
                placeholder="Enter your phone number"
                value={form.phone}
                onChange={setField("phone")}
                required
              />

              {/* COLLEGE ADMIN: existing or new */}
              {isAdmin && (
                <SelectField
                  id="collegeOption"
                  label="College"
                  placeholder="Select college option"
                  value={collegeOption}
                  onChange={(event) => {
                    setCollegeOption(event.target.value);
                    setForm((previous) => ({ ...previous, collegeId: "" }));
                  }}
                  required
                  options={[
                    { value: "EXISTING_COLLEGE", label: "Select Existing College" },
                    { value: "NEW_COLLEGE", label: "Create New College Profile" },
                  ]}
                />
              )}

              {/* COLLEGE DROPDOWN */}
              {showCollegeSelect && (
                <SelectField
                  id="college"
                  label={isNormal ? "College (Optional)" : "College"}
                  placeholder={
                    colleges.length === 0
                      ? "No colleges available yet"
                      : "Select your college"
                  }
                  value={form.collegeId}
                  onChange={handleCollegeChange}
                  required={!isNormal}
                  options={colleges.map((college) => ({
                    value: college.collegeId,
                    label: college.collegeName,
                  }))}
                />
              )}

              {/* DEPARTMENT / ORGANIZATION */}
              {needsDepartment && (
                <SelectField
                  id="department"
                  label="Department"
                  placeholder={
                    form.collegeId ? "Select department" : "Select a college first"
                  }
                  value={form.departmentId}
                  onChange={setField("departmentId")}
                  disabled={!form.collegeId}
                  required
                  options={departments.map((department) => ({
                    value: department.departmentId,
                    label: department.departmentName,
                  }))}
                />
              )}

              {needsOrganization && (
                <SelectField
                  id="organization"
                  label="Organization / Council"
                  placeholder={
                    form.collegeId ? "Select organization" : "Select a college first"
                  }
                  value={form.organizationId}
                  onChange={setField("organizationId")}
                  disabled={!form.collegeId}
                  required
                  options={organizations.map((organization) => ({
                    value: organization.organizationId,
                    label: organization.organizationName,
                  }))}
                />
              )}

              {/* COLLEGE ID + PHOTO */}
              {needsStudentId && (
                <>
                  <TextField
                    id="collegeIdNumber"
                    label={role === "STUDENT_ORGANIZER" ? "College ID Number" : "College / Employee ID"}
                    type="text"
                    placeholder="Enter your ID number"
                    value={form.collegeIdNumber}
                    onChange={setField("collegeIdNumber")}
                    required
                  />

                  <TextField
                    id="idPhoto"
                    label="ID Card Photo"
                    type="file"
                    accept="image/*"
                    onChange={setFile("idPhoto")}
                    required
                  />
                </>
              )}

              {/* DESIGNATION */}
              {needsDesignation && (
                <TextField
                  id="designation"
                  label="Designation"
                  type="text"
                  placeholder="e.g. Professor, HOD, Principal"
                  value={form.designation}
                  onChange={setField("designation")}
                  required
                />
              )}

              {/* NEW COLLEGE PROFILE */}
              {isNewCollege && (
                <>
                  <TextField
                    id="collegeName"
                    label="College Name"
                    type="text"
                    placeholder="Enter official college name"
                    value={form.collegeName}
                    onChange={setField("collegeName")}
                    required
                  />
                  <TextField
                    id="collegeCode"
                    label="College Short Name / Code"
                    type="text"
                    placeholder="e.g. VPPCE"
                    value={form.collegeCode}
                    onChange={setField("collegeCode")}
                    required
                  />
                  <TextField
                    id="emailDomain"
                    label="College Email Domain"
                    type="text"
                    placeholder="e.g. vppcoe.ac.in"
                    value={form.emailDomain}
                    onChange={setField("emailDomain")}
                    required
                  />
                  <TextField
                    id="address"
                    label="College Address"
                    type="text"
                    placeholder="Enter complete college address"
                    value={form.address}
                    onChange={setField("address")}
                    required
                  />
                  <TextField
                    id="city"
                    label="City"
                    type="text"
                    placeholder="Enter city"
                    value={form.city}
                    onChange={setField("city")}
                    required
                  />
                  <TextField
                    id="state"
                    label="State"
                    type="text"
                    placeholder="Enter state"
                    value={form.state}
                    onChange={setField("state")}
                    required
                  />
                  <TextField
                    id="pincode"
                    label="Pincode"
                    type="text"
                    placeholder="Enter pincode"
                    value={form.pincode}
                    onChange={setField("pincode")}
                    required
                  />
                  <TextField
                    id="website"
                    label="College Website (optional)"
                    type="url"
                    placeholder="https://example.com"
                    value={form.website}
                    onChange={setField("website")}
                  />
                  <TextField
                    id="collegeLogo"
                    label="College Logo (optional)"
                    type="file"
                    accept="image/*"
                    onChange={setFile("collegeLogo")}
                  />
                  <TextField
                    id="collegeProof"
                    label="College ID / Official Proof"
                    type="file"
                    accept="image/*,.pdf"
                    onChange={setFile("idPhoto")}
                    required
                  />
                </>
              )}

              {/* PASSWORD */}
              <TextField
                id="password"
                label="Password"
                type="password"
                placeholder="Create a password (min 8 characters)"
                value={form.password}
                onChange={setField("password")}
                required
              />

              <TextField
                id="confirmPassword"
                label="Confirm Password"
                type="password"
                placeholder="Confirm your password"
                value={form.confirmPassword}
                onChange={setField("confirmPassword")}
                required
              />

              {!UPLOADS_READY && (
                <p style={{ fontSize: "0.8rem", color: "#facc15", textAlign: "center" }}>
                  Dev note: file uploads are not enabled yet, selected files are ignored.
                </p>
              )}
            </>
          )}

          {message && (
            <div className={`auth-message ${success ? "success" : ""}`}>
              {message}
            </div>
          )}

          {role && (
            <button className="auth-button" type="submit" disabled={loading}>
              {loading
                ? "Please wait..."
                : isNormal
                ? "Create Account"
                : "Submit Application"}
            </button>
          )}
        </form>

        <div className="auth-footer">
          Already have an account?{" "}
          <Link className="auth-link" to="/login">
            Back to Login
          </Link>
        </div>
      </main>
    </div>
  );
}

export default Signup;