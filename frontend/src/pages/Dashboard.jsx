import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Folder, AlertCircle } from 'lucide-react';
import api from '../services/api';

export default function Dashboard() {
  const [projects, setProjects] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [newProject, setNewProject] = useState({ name: '', description: '' });
  const [creating, setCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchProjects();
  }, []);

  const fetchProjects = async () => {
    try {
      const response = await api.get('/projects');
      setProjects(response.data.data || []);
    } catch (error) {
      console.error("Error fetching projects", error);
    }
  };

  const handleCreateProject = async (e) => {
    e.preventDefault();
    if (!newProject.name || !newProject.name.trim()) {
      setErrorMessage("Project name is required.");
      return;
    }

    setCreating(true);
    setErrorMessage('');

    try {
      const response = await api.post('/projects', {
        name: newProject.name.trim(),
        description: newProject.description ? newProject.description.trim() : ''
      });

      const created = response.data.data;
      setShowModal(false);
      setNewProject({ name: '', description: '' });

      if (created && created.id) {
        navigate(`/project/${created.id}`);
      } else {
        fetchProjects();
      }
    } catch (error) {
      console.error("Error creating project", error);
      setErrorMessage(error.response?.data?.message || "Failed to create project. Please ensure backend server is running.");
    } finally {
      setCreating(false);
    }
  };

  const handleOpenModal = () => {
    setNewProject({ name: '', description: '' });
    setErrorMessage('');
    setShowModal(true);
  };

  return (
    <div className="container mt-5">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Projects</h2>
        <button className="btn btn-primary d-flex align-items-center gap-2" onClick={handleOpenModal}>
          <Plus size={20} /> Create Project
        </button>
      </div>

      <div className="row g-4">
        {projects.length === 0 ? (
          <div className="col-12 text-center text-muted py-5">
            <p>No projects found. Create your first network project!</p>
          </div>
        ) : (
          projects.map(project => (
            <div key={project.id} className="col-md-4">
              <div className="card project-card h-100 shadow-sm border-0">
                <div className="card-body">
                  <div className="d-flex align-items-center gap-2 mb-3 text-primary">
                    <Folder size={24} />
                    <h5 className="card-title mb-0">{project.name}</h5>
                  </div>
                  <p className="card-text text-muted small">{project.description || 'No description'}</p>
                  <p className="card-text small mb-4 text-secondary">
                    Last modified: {project.updatedAt ? new Date(project.updatedAt).toLocaleDateString() : 'N/A'}
                  </p>
                  <Link to={`/project/${project.id}`} className="btn btn-outline-primary w-100">Open Workspace</Link>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {showModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content shadow">
              <div className="modal-header">
                <h5 className="modal-title">Create New Project</h5>
                <button type="button" className="btn-close" onClick={() => setShowModal(false)}></button>
              </div>
              <form onSubmit={handleCreateProject}>
                <div className="modal-body">
                  {errorMessage && (
                    <div className="alert alert-danger d-flex align-items-center gap-2 py-2 small mb-3">
                      <AlertCircle size={16} />
                      <div>{errorMessage}</div>
                    </div>
                  )}
                  <div className="mb-3">
                    <label className="form-label">Project Name</label>
                    <input 
                      type="text" 
                      className="form-control" 
                      value={newProject.name}
                      onChange={e => setNewProject({...newProject, name: e.target.value})}
                      placeholder="e.g. My Network Topology"
                      required
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Description</label>
                    <textarea 
                      className="form-control" 
                      value={newProject.description}
                      onChange={e => setNewProject({...newProject, description: e.target.value})}
                      placeholder="e.g. 2 Routers, 2 Switches, Server and Client"
                      rows="3"
                    ></textarea>
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)} disabled={creating}>Cancel</button>
                  <button type="submit" className="btn btn-primary d-flex align-items-center gap-1" disabled={creating}>
                    {creating ? 'Creating...' : 'Create Project'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
