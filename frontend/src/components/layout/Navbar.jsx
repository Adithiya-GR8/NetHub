import { Link } from 'react-router-dom';
import { Activity } from 'lucide-react';

export default function Navbar() {
  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
      <div className="container-fluid">
        <Link className="navbar-brand d-flex align-items-center gap-2" to="/">
          <Activity size={24} />
          <span>NetHub</span>
        </Link>
      </div>
    </nav>
  );
}
