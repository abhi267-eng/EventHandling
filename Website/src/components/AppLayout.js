import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";
import "./components.css";

function AppLayout() {
  return (
    <div className="layout">
      <Navbar />
      <main className="page">
        <Outlet />
      </main>
    </div>
  );
}

export default AppLayout;