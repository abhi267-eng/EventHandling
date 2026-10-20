import { useState } from "react";
import { Link } from "react-router-dom";

function Signup() {
  const [role, setRole] = useState("");
  const [collegeOption, setCollegeOption] = useState("");

  const [departments, setDepartments] = useState([""]);
  const [organizations, setOrganizations] = useState([""]);

  const addDepartment = () => {
    setDepartments([...departments, ""]);
  };

  const updateDepartment = (index, value) => {
    const updatedDepartments = [...departments];
    updatedDepartments[index] = value;
    setDepartments(updatedDepartments);
  };

  const removeDepartment = (index) => {
    if (departments.length === 1) return;

    setDepartments(
      departments.filter((_, departmentIndex) => departmentIndex !== index)
    );
  };

  const addOrganization = () => {
    setOrganizations([...organizations, ""]);
  };

  const updateOrganization = (index, value) => {
    const updatedOrganizations = [...organizations];
    updatedOrganizations[index] = value;
    setOrganizations(updatedOrganizations);
  };

  const removeOrganization = (index) => {
    if (organizations.length === 1) return;

    setOrganizations(
      organizations.filter(
        (_, organizationIndex) => organizationIndex !== index
      )
    );
  };

  return (
    <div className="app-background">
      <div className="stars"></div>

      <main className="auth-card">
        <div className="auth-brand">
          <h1>New Registration</h1>
          <p>Create your EventHandling account</p>
        </div>

        <form className="auth-form">
          {/* =========================
              ACCOUNT TYPE
          ========================= */}

          <div className="form-group">
            <label htmlFor="role">Account Type</label>

            <select
              id="role"
              className="form-select"
              value={role}
              onChange={(event) => {
                setRole(event.target.value);
                setCollegeOption("");
              }}
              required
            >
              <option value="">Select account type</option>

              <option value="NORMAL_USER">
                Normal User
              </option>

              <option value="STUDENT_ORGANIZER">
                Student Organizer
              </option>

              <option value="EVENT_COORDINATOR">
                Event Coordinator
              </option>

              <option value="FACULTY_COORDINATOR">
                Faculty Coordinator
              </option>

              <option value="COLLEGE_ADMIN">
                College Admin
              </option>
            </select>
          </div>

          {/* =========================
              USER INFORMATION
          ========================= */}

          {role && (
            <>
              {/* Profile Picture */}
              <div className="form-group">
                <label htmlFor="profilePicture">
                  Profile Picture
                </label>

                <input
                  id="profilePicture"
                  className="form-input"
                  type="file"
                  accept="image/*"
                />
              </div>

              {/* Full Name */}
              <div className="form-group">
                <label htmlFor="fullName">
                  Full Name
                </label>

                <input
                  id="fullName"
                  className="form-input"
                  type="text"
                  placeholder="Enter your full name"
                  required
                />
              </div>

              {/* Email */}
              <div className="form-group">
                <label htmlFor="email">
                  Email Address
                </label>

                <input
                  id="email"
                  className="form-input"
                  type="email"
                  placeholder="Enter your email"
                  required
                />
              </div>

              {/* Phone */}
              <div className="form-group">
                <label htmlFor="phone">
                  Phone Number
                </label>

                <input
                  id="phone"
                  className="form-input"
                  type="tel"
                  placeholder="Enter your phone number"
                  required
                />
              </div>

              {/* =========================
                  NORMAL USER
              ========================= */}

              {role === "NORMAL_USER" && (
                <div className="form-group">
                  <label htmlFor="college">
                    College (Optional)
                  </label>

                  <input
                    id="college"
                    className="form-input"
                    type="text"
                    placeholder="Enter your college"
                  />
                </div>
              )}

              {/* =========================
                  STUDENT ORGANIZER
                  EVENT COORDINATOR
                  FACULTY COORDINATOR
              ========================= */}

              {role !== "NORMAL_USER" &&
                role !== "COLLEGE_ADMIN" && (
                  <>
                    {/* College */}
                    <div className="form-group">
                      <label htmlFor="college">
                        College
                      </label>

                      <input
                        id="college"
                        className="form-input"
                        type="text"
                        placeholder="Enter your college"
                        required
                      />
                    </div>

                    {/* Department / Organization */}
                    <div className="form-group">
                      <label htmlFor="departmentOrganization">
                        Department / Organization
                      </label>

                      <input
                        id="departmentOrganization"
                        className="form-input"
                        type="text"
                        placeholder="e.g. AIML / NSS / Student Council"
                        required
                      />
                    </div>

                    {/* College ID */}
                    <div className="form-group">
                      <label htmlFor="collegeId">
                        College ID
                      </label>

                      <input
                        id="collegeId"
                        className="form-input"
                        type="text"
                        placeholder="Enter your college ID"
                        required
                      />
                    </div>

                    {/* ID Photo */}
                    <div className="form-group">
                      <label htmlFor="idPhoto">
                        ID Photo
                      </label>

                      <input
                        id="idPhoto"
                        className="form-input"
                        type="file"
                        accept="image/*"
                        required
                      />
                    </div>
                  </>
                )}

              {/* =========================
                  DESIGNATION
              ========================= */}

              {(role === "EVENT_COORDINATOR" ||
                role === "FACULTY_COORDINATOR" ||
                role === "COLLEGE_ADMIN") && (
                <div className="form-group">
                  <label htmlFor="designation">
                    Designation
                  </label>

                  <input
                    id="designation"
                    className="form-input"
                    type="text"
                    placeholder="Enter your designation"
                    required
                  />
                </div>
              )}

              {/* =========================
                  COLLEGE ADMIN
              ========================= */}

              {role === "COLLEGE_ADMIN" && (
                <>
                  {/* College Option */}
                  <div className="form-group">
                    <label htmlFor="collegeOption">
                      College
                    </label>

                    <select
                      id="collegeOption"
                      className="form-select"
                      value={collegeOption}
                      onChange={(event) =>
                        setCollegeOption(event.target.value)
                      }
                      required
                    >
                      <option value="">
                        Select college option
                      </option>

                      <option value="EXISTING_COLLEGE">
                        Select Existing College
                      </option>

                      <option value="NEW_COLLEGE">
                        Create New College Profile
                      </option>
                    </select>
                  </div>

                  {/* =========================
                      EXISTING COLLEGE
                  ========================= */}

                  {collegeOption === "EXISTING_COLLEGE" && (
                    <div className="form-group">
                      <label htmlFor="existingCollege">
                        Select Existing College
                      </label>

                      <select
                        id="existingCollege"
                        className="form-select"
                        required
                      >
                        <option value="">
                          Select your college
                        </option>

                        <option value="placeholder">
                          No colleges available yet
                        </option>
                      </select>
                    </div>
                  )}

                  {/* =========================
                      NEW COLLEGE PROFILE
                  ========================= */}

                  {collegeOption === "NEW_COLLEGE" && (
                    <>
                      <div className="form-group">
                        <label htmlFor="collegeName">
                          College Name
                        </label>

                        <input
                          id="collegeName"
                          className="form-input"
                          type="text"
                          placeholder="Enter official college name"
                          required
                        />
                      </div>

                      <div className="form-group">
                        <label htmlFor="collegeShortName">
                          College Short Name
                        </label>

                        <input
                          id="collegeShortName"
                          className="form-input"
                          type="text"
                          placeholder="e.g. VPPCE"
                        />
                      </div>

                      <div className="form-group">
                        <label htmlFor="collegeAddress">
                          College Address
                        </label>

                        <input
                          id="collegeAddress"
                          className="form-input"
                          type="text"
                          placeholder="Enter complete college address"
                          required
                        />
                      </div>

                      <div className="form-group">
                        <label htmlFor="city">
                          City
                        </label>

                        <input
                          id="city"
                          className="form-input"
                          type="text"
                          placeholder="Enter city"
                          required
                        />
                      </div>

                      <div className="form-group">
                        <label htmlFor="state">
                          State
                        </label>

                        <input
                          id="state"
                          className="form-input"
                          type="text"
                          placeholder="Enter state"
                          required
                        />
                      </div>

                      <div className="form-group">
                        <label htmlFor="pincode">
                          Pincode
                        </label>

                        <input
                          id="pincode"
                          className="form-input"
                          type="text"
                          placeholder="Enter pincode"
                          required
                        />
                      </div>

                      {/* College Location */}
                      <div className="form-group">
                        <label>
                          College Location
                        </label>

                        <div className="college-map-placeholder">
                          <p>
                            Map location selection will be
                            added later.
                          </p>

                          <p>
                            Select the exact college location
                            on the map.
                          </p>
                        </div>
                      </div>

                      {/* Latitude */}
                      <div className="form-group">
                        <label htmlFor="latitude">
                          Latitude
                        </label>

                        <input
                          id="latitude"
                          className="form-input"
                          type="number"
                          step="any"
                          placeholder="Will be selected from map"
                          readOnly
                        />
                      </div>

                      {/* Longitude */}
                      <div className="form-group">
                        <label htmlFor="longitude">
                          Longitude
                        </label>

                        <input
                          id="longitude"
                          className="form-input"
                          type="number"
                          step="any"
                          placeholder="Will be selected from map"
                          readOnly
                        />
                      </div>

                      {/* =========================
                          DEPARTMENTS
                      ========================= */}

                      <div className="form-group">
                        <label>
                          Departments
                        </label>

                        {departments.map(
                          (department, index) => (
                            <div
                              className="dynamic-field"
                              key={index}
                            >
                              <input
                                className="form-input"
                                type="text"
                                value={department}
                                onChange={(event) =>
                                  updateDepartment(
                                    index,
                                    event.target.value
                                  )
                                }
                                placeholder="Enter department name"
                                required
                              />

                              {departments.length > 1 && (
                                <button
                                  type="button"
                                  className="remove-field-button"
                                  onClick={() =>
                                    removeDepartment(index)
                                  }
                                >
                                  Remove
                                </button>
                              )}
                            </div>
                          )
                        )}

                        <button
                          type="button"
                          className="secondary-auth-button"
                          onClick={addDepartment}
                        >
                          + Add Department
                        </button>
                      </div>

                      {/* =========================
                          ORGANIZATIONS
                      ========================= */}

                      <div className="form-group">
                        <label>
                          Organizations / Councils
                        </label>

                        {organizations.map(
                          (organization, index) => (
                            <div
                              className="dynamic-field"
                              key={index}
                            >
                              <input
                                className="form-input"
                                type="text"
                                value={organization}
                                onChange={(event) =>
                                  updateOrganization(
                                    index,
                                    event.target.value
                                  )
                                }
                                placeholder="e.g. NSS, Student Council"
                                required
                              />

                              {organizations.length > 1 && (
                                <button
                                  type="button"
                                  className="remove-field-button"
                                  onClick={() =>
                                    removeOrganization(index)
                                  }
                                >
                                  Remove
                                </button>
                              )}
                            </div>
                          )
                        )}

                        <button
                          type="button"
                          className="secondary-auth-button"
                          onClick={addOrganization}
                        >
                          + Add Organization
                        </button>
                      </div>

                      {/* Website */}
                      <div className="form-group">
                        <label htmlFor="collegeWebsite">
                          College Website
                        </label>

                        <input
                          id="collegeWebsite"
                          className="form-input"
                          type="url"
                          placeholder="https://example.com"
                        />
                      </div>

                      {/* College Email */}
                      <div className="form-group">
                        <label htmlFor="collegeEmail">
                          College Email
                        </label>

                        <input
                          id="collegeEmail"
                          className="form-input"
                          type="email"
                          placeholder="Enter official college email"
                        />
                      </div>

                      {/* College Contact */}
                      <div className="form-group">
                        <label htmlFor="collegeContact">
                          College Contact Number
                        </label>

                        <input
                          id="collegeContact"
                          className="form-input"
                          type="tel"
                          placeholder="Enter college contact number"
                        />
                      </div>

                      {/* College Logo */}
                      <div className="form-group">
                        <label htmlFor="collegeLogo">
                          College Logo
                        </label>

                        <input
                          id="collegeLogo"
                          className="form-input"
                          type="file"
                          accept="image/*"
                        />
                      </div>

                      {/* Official Proof */}
                      <div className="form-group">
                        <label htmlFor="collegeProof">
                          College ID / Official Proof
                        </label>

                        <input
                          id="collegeProof"
                          className="form-input"
                          type="file"
                          accept="image/*,.pdf"
                          required
                        />
                      </div>
                    </>
                  )}
                </>
              )}

              {/* =========================
                  PASSWORD
              ========================= */}

              <div className="form-group">
                <label htmlFor="password">
                  Password
                </label>

                <input
                  id="password"
                  className="form-input"
                  type="password"
                  placeholder="Create a password"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="confirmPassword">
                  Confirm Password
                </label>

                <input
                  id="confirmPassword"
                  className="form-input"
                  type="password"
                  placeholder="Confirm your password"
                  required
                />
              </div>

              {/* =========================
                  SUBMIT
              ========================= */}

              <button
                className="auth-button"
                type="submit"
              >
                {role === "NORMAL_USER"
                  ? "Create Account"
                  : "Submit Application"}
              </button>
            </>
          )}
        </form>

        {/* Back to Login */}
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