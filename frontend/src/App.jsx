import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import NetworkWorkspace from './pages/NetworkWorkspace';
import Navbar from './components/layout/Navbar';

function App() {
  return (
    <Router>
      <Navbar />
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/project/:id" element={<NetworkWorkspace />} />
      </Routes>
    </Router>
  );
}

export default App;
