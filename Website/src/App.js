import { BrowserRouter, Routes, Route } from "react-router-dom";

import Landing from "./pages/Landing";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import VerifyEmail from "./pages/VerifyEmail";
import Pending from "./pages/Pending";
import PlatformSetup from "./pages/PlatformSetup";
import NotFound from "./pages/NotFound";

import Home from "./pages/Home";
import EventList from "./pages/EventList";
import EventDetails from "./pages/EventDetails";
import SavedEvents from "./pages/SavedEvents";
import Profile from "./pages/Profile";
import MyEvents from "./pages/MyEvents";
import EventForm from "./pages/EventForm";
import Applications from "./pages/Applications";
import ReviewEvents from "./pages/ReviewEvents";

import AdminDashboard from "./pages/AdminDashboard";
import ManageDepartments from "./pages/ManageDepartments";
import ManageOrganizations from "./pages/ManageOrganizations";
import ManageUsers from "./pages/ManageUsers";
import PlatformOverview from "./pages/PlatformOverview";

import AppLayout from "./components/AppLayout";
import ProtectedRoute from "./components/ProtectedRoute";
import { FeedbackProvider } from "./components/Feedback";
import {
  ROLES,
  ADMIN_ROLES,
  EVENT_CREATOR_ROLES,
  APPLICATION_REVIEWER_ROLES,
  EVENT_REVIEWER_ROLES,
} from "./auth";

function App() {
  return (
    <FeedbackProvider>
      <BrowserRouter>
        <Routes>
          {/* 1. Full-screen pages (no navbar) */}
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/pending" element={<Pending />} />
          <Route path="/setup" element={<PlatformSetup />} />

          {/* 2. Everything below has the navbar */}
          <Route element={<AppLayout />}>
            {/* Public */}
            <Route path="/events" element={<EventList />} />
            <Route path="/events/:id" element={<EventDetails />} />

            {/* Any logged-in user */}
            <Route element={<ProtectedRoute />}>
              <Route path="/home" element={<Home />} />
              <Route path="/saved" element={<SavedEvents />} />
              <Route path="/profile" element={<Profile />} />
            </Route>

            {/* Organizers / coordinators */}
            <Route element={<ProtectedRoute roles={EVENT_CREATOR_ROLES} />}>
              <Route path="/my-events" element={<MyEvents />} />
              <Route path="/events/new" element={<EventForm />} />
              <Route path="/events/:id/edit" element={<EventForm />} />
            </Route>

            {/* Reviewers of applications */}
            <Route element={<ProtectedRoute roles={APPLICATION_REVIEWER_ROLES} />}>
              <Route path="/applications" element={<Applications />} />
            </Route>

            {/* Reviewers of events */}
            <Route element={<ProtectedRoute roles={EVENT_REVIEWER_ROLES} />}>
              <Route path="/review-events" element={<ReviewEvents />} />
            </Route>

            {/* College Admin + Platform Admin */}
            <Route element={<ProtectedRoute roles={ADMIN_ROLES} />}>
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/users" element={<ManageUsers />} />
              <Route path="/admin/departments" element={<ManageDepartments />} />
              <Route path="/admin/organizations" element={<ManageOrganizations />} />
            </Route>

            {/* Platform Admin only */}
            <Route element={<ProtectedRoute roles={[ROLES.PLATFORM_ADMIN]} />}>
              <Route path="/admin/colleges" element={<PlatformOverview />} />
            </Route>
          </Route>

          {/* 3. Anything else */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </FeedbackProvider>
  );
}

export default App;