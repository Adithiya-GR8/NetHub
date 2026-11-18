import { Link } from 'react-router-dom';
import { Activity, BookOpen, Layers } from 'lucide-react';

export default function Navbar() {
  return (
    <nav className="navbar navbar-expand navbar-dark bg-primary shadow-sm py-2">
      <div className="container">
        <Link className="navbar-brand d-flex align-items-center gap-2 fw-bold" to="/">
          <Activity size={24} />
          <span>NetHub</span>
        </Link>
        <div className="navbar-nav ms-auto d-flex align-items-center gap-3">
          <Link className="nav-link d-flex align-items-center gap-1 active" to="/">
            <Layers size={16} />
            <span>Dashboard</span>
          </Link>
          <span className="badge bg-white text-primary px-2 py-1 rounded-pill small fw-normal d-none d-sm-inline-block">
            32 Demo Scenarios (14 Valid • 18 Error Diagnoses)
          </span>
        </div>
      </div>
    </nav>
  );
}
