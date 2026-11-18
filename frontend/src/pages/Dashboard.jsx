import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Plus, Folder, AlertCircle, BookOpen, CheckCircle, 
  AlertTriangle, Search, Filter, ShieldCheck, ShieldAlert, Cpu, ArrowRight 
} from 'lucide-react';
import api from '../services/api';
import DemonstrationScenariosModal from '../components/simulation/DemonstrationScenariosModal';
import { PRELOADED_SCENARIOS } from '../data/demonstrationScenariosData';

export default function Dashboard() {
  const [projects, setProjects] = useState([]);
  const [scenarios, setScenarios] = useState(PRELOADED_SCENARIOS);
  const [mainTab, setMainTab] = useState('SCENARIOS'); // 'SCENARIOS' or 'PROJECTS'
  const [scenarioCategory, setScenarioCategory] = useState('VALID'); // 'VALID' or 'INVALID'
  const [scenarioSearch, setScenarioSearch] = useState('');
  const [scenarioDifficulty, setScenarioDifficulty] = useState('ALL');
  
  const [showModal, setShowModal] = useState(false);
  const [showScenarioModal, setShowScenarioModal] = useState(false);
  const [newProject, setNewProject] = useState({ name: '', description: '' });
  const [creating, setCreating] = useState(false);
  const [loadingScenarioCode, setLoadingScenarioCode] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchProjects();
    fetchScenarios();
  }, []);

  const fetchProjects = async () => {
    try {
      const response = await api.get('/projects');
      setProjects(response.data.data || []);
    } catch (error) {
      console.error("Error fetching projects", error);
    }
  };

  const fetchScenarios = async () => {
    try {
      const res = await api.get('/scenarios');
      if (res.data?.data && res.data.data.length > 0) {
        setScenarios(res.data.data);
      }
    } catch (error) {
      console.warn("Backend scenarios offline, using preloaded catalog", error);
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

  const handleLaunchScenario = async (scenario) => {
    setLoadingScenarioCode(scenario.code);
    try {
      const identifier = scenario.id || scenario.code;
      const res = await api.post(`/scenarios/${identifier}/load`);
      const proj = res.data.data;
      if (proj && proj.id) {
        navigate(`/project/${proj.id}`);
      } else {
        fetchProjects();
      }
    } catch (err) {
      console.error("Failed to load scenario", err);
      alert(err.response?.data?.message || "Failed to load demonstration scenario. Please ensure the backend is running.");
    } finally {
      setLoadingScenarioCode(null);
    }
  };

  const filteredScenarios = scenarios.filter(s => {
    if (s.category !== scenarioCategory) return false;
    if (scenarioDifficulty !== 'ALL' && s.difficulty !== scenarioDifficulty) return false;
    if (scenarioSearch.trim()) {
      const q = scenarioSearch.toLowerCase();
      const matchName = s.name.toLowerCase().includes(q);
      const matchCode = s.code.toLowerCase().includes(q);
      const matchDesc = s.description ? s.description.toLowerCase().includes(q) : false;
      const matchConcepts = s.concepts ? s.concepts.toLowerCase().includes(q) : false;
      return matchName || matchCode || matchDesc || matchConcepts;
    }
    return true;
  });

  const validCount = scenarios.filter(s => s.category === 'VALID').length;
  const invalidCount = scenarios.filter(s => s.category === 'INVALID').length;

  return (
    <div className="container mt-4 mb-5">
      {/* Top Hero Bar */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-3 border-bottom gap-3">
        <div>
          <h2 className="mb-1 text-dark fw-bold">NetHub Network Simulator</h2>
          <div className="text-muted small">
            Interactive virtual networking management, protocol emulation & demonstration topologies
          </div>
        </div>
        <div className="d-flex gap-2">
          <button 
            className="btn btn-outline-primary d-flex align-items-center gap-2"
            onClick={() => setShowScenarioModal(true)}
          >
            <BookOpen size={18} /> Quick Modal View
          </button>
          <button 
            className="btn btn-primary d-flex align-items-center gap-2" 
            onClick={() => {
              setNewProject({ name: '', description: '' });
              setErrorMessage('');
              setShowModal(true);
            }}
          >
            <Plus size={20} /> Create Blank Project
          </button>
        </div>
      </div>

      {/* Main Tab Navigation */}
      <div className="d-flex align-items-center justify-content-between mb-4">
        <ul className="nav nav-pills gap-2">
          <li className="nav-item">
            <button 
              className={`nav-link px-4 py-2 d-flex align-items-center gap-2 ${mainTab === 'SCENARIOS' ? 'active shadow-sm' : 'bg-light text-dark'}`}
              onClick={() => setMainTab('SCENARIOS')}
            >
              <BookOpen size={18} />
              <span>Demonstration Scenarios</span>
              <span className={`badge ${mainTab === 'SCENARIOS' ? 'bg-light text-primary' : 'bg-secondary'}`}>32</span>
            </button>
          </li>
          <li className="nav-item">
            <button 
              className={`nav-link px-4 py-2 d-flex align-items-center gap-2 ${mainTab === 'PROJECTS' ? 'active shadow-sm' : 'bg-light text-dark'}`}
              onClick={() => setMainTab('PROJECTS')}
            >
              <Folder size={18} />
              <span>My Projects</span>
              <span className={`badge ${mainTab === 'PROJECTS' ? 'bg-light text-primary' : 'bg-secondary'}`}>{projects.length}</span>
            </button>
          </li>
        </ul>
      </div>

      {/* VIEW 1: DEMONSTRATION SCENARIOS */}
      {mainTab === 'SCENARIOS' && (
        <div>
          {/* Category Selector & Filters */}
          <div className="card shadow-sm border-0 mb-4 bg-light">
            <div className="card-body py-3">
              <div className="row g-3 align-items-center">
                <div className="col-md-5">
                  <div className="btn-group w-100" role="group">
                    <button 
                      type="button" 
                      className={`btn d-flex align-items-center justify-content-center gap-2 ${scenarioCategory === 'VALID' ? 'btn-success fw-bold' : 'btn-outline-secondary'}`}
                      onClick={() => setScenarioCategory('VALID')}
                    >
                      <CheckCircle size={18} /> Valid Topologies ({validCount})
                    </button>
                    <button 
                      type="button" 
                      className={`btn d-flex align-items-center justify-content-center gap-2 ${scenarioCategory === 'INVALID' ? 'btn-warning fw-bold text-dark' : 'btn-outline-secondary'}`}
                      onClick={() => setScenarioCategory('INVALID')}
                    >
                      <AlertTriangle size={18} /> Invalid & Error Topologies ({invalidCount})
                    </button>
                  </div>
                </div>

                <div className="col-md-4">
                  <div className="input-group">
                    <span className="input-group-text bg-white border-end-0">
                      <Search size={16} className="text-muted" />
                    </span>
                    <input 
                      type="text" 
                      className="form-control border-start-0" 
                      placeholder="Search by code, title, concept (e.g. OSPF, VLAN, ARP)..."
                      value={scenarioSearch}
                      onChange={e => setScenarioSearch(e.target.value)}
                    />
                  </div>
                </div>

                <div className="col-md-3">
                  <div className="input-group">
                    <span className="input-group-text bg-white border-end-0">
                      <Filter size={16} className="text-muted" />
                    </span>
                    <select 
                      className="form-select border-start-0"
                      value={scenarioDifficulty}
                      onChange={e => setScenarioDifficulty(e.target.value)}
                    >
                      <option value="ALL">All Difficulties</option>
                      <option value="BEGINNER">Beginner</option>
                      <option value="INTERMEDIATE">Intermediate</option>
                      <option value="ADVANCED">Advanced</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Scenario Grid */}
          <div className="row g-4">
            {filteredScenarios.length === 0 ? (
              <div className="col-12 text-center py-5 text-muted">
                <AlertCircle size={32} className="mb-2 text-secondary" />
                <p>No demonstration scenarios matched your filters.</p>
              </div>
            ) : (
              filteredScenarios.map(sc => (
                <div key={sc.code} className="col-lg-6 col-xl-4">
                  <div className="card h-100 shadow-sm border-0 d-flex flex-column hover-shadow transition">
                    <div className="card-header bg-white border-0 pt-3 pb-2 d-flex justify-content-between align-items-center">
                      <div className="d-flex align-items-center gap-2">
                        <span className={`badge ${sc.category === 'VALID' ? 'bg-success' : 'bg-warning text-dark'} fs-6`}>
                          {sc.code}
                        </span>
                        <span className="badge bg-light text-dark border">
                          {sc.difficulty}
                        </span>
                      </div>
                      <span className="small text-muted d-flex align-items-center gap-1">
                        <Cpu size={14} /> {sc.deviceCount} Devices
                      </span>
                    </div>

                    <div className="card-body flex-grow-1">
                      <h5 className="card-title fw-bold text-primary mb-2">{sc.name}</h5>
                      <p className="card-text text-secondary small mb-3">{sc.description}</p>

                      {sc.concepts && (
                        <div className="mb-3">
                          <small className="text-muted fw-bold d-block mb-1">Concepts:</small>
                          <span className="badge bg-secondary-subtle text-dark border text-wrap text-start lh-base small">
                            {sc.concepts}
                          </span>
                        </div>
                      )}

                      {sc.category === 'VALID' ? (
                        <div className="alert alert-success py-2 px-3 small mb-0 border-0 bg-success-subtle text-success-emphasis">
                          <div className="fw-bold d-flex align-items-center gap-1 mb-1">
                            <ShieldCheck size={14} /> Expected Result:
                          </div>
                          <div>{sc.expectedResult}</div>
                        </div>
                      ) : (
                        <div className="alert alert-warning py-2 px-3 small mb-0 border-0 bg-warning-subtle text-warning-emphasis">
                          <div className="fw-bold d-flex align-items-center gap-1 mb-1">
                            <ShieldAlert size={14} /> Expected Diagnostic Error:
                          </div>
                          <div>{sc.expectedErrors}</div>
                        </div>
                      )}
                    </div>

                    <div className="card-footer bg-white border-top-0 pt-0 pb-3">
                      <button 
                        className={`btn w-100 d-flex align-items-center justify-content-center gap-2 ${sc.category === 'VALID' ? 'btn-outline-success' : 'btn-outline-warning text-dark'}`}
                        disabled={loadingScenarioCode === sc.code}
                        onClick={() => handleLaunchScenario(sc)}
                      >
                        {loadingScenarioCode === sc.code ? (
                          <>
                            <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                            Loading Topology...
                          </>
                        ) : (
                          <>
                            <span>Open Scenario Workspace</span>
                            <ArrowRight size={16} />
                          </>
                        )}
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* VIEW 2: CUSTOM PROJECTS */}
      {mainTab === 'PROJECTS' && (
        <div className="row g-4">
          {projects.length === 0 ? (
            <div className="col-12 text-center text-muted py-5 card border-0 bg-light">
              <Folder size={48} className="mx-auto mb-3 text-secondary opacity-50" />
              <h4>No custom projects yet</h4>
              <p className="text-secondary small">Create your own blank network topology or load a demonstration scenario to get started.</p>
              <div>
                <button 
                  className="btn btn-primary"
                  onClick={() => {
                    setNewProject({ name: '', description: '' });
                    setErrorMessage('');
                    setShowModal(true);
                  }}
                >
                  <Plus size={18} /> Create New Project
                </button>
              </div>
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
                    <Link to={`/project/${project.id}`} className="btn btn-outline-primary w-100">
                      Open Workspace
                    </Link>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Create Project Modal */}
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
                      placeholder="e.g. Campus Backbone Topology"
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
                  <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)} disabled={creating}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary d-flex align-items-center gap-1" disabled={creating}>
                    {creating ? 'Creating...' : 'Create Project'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Quick Modal View */}
      {showScenarioModal && (
        <DemonstrationScenariosModal
          onClose={() => setShowScenarioModal(false)}
          onScenarioLoaded={(project) => {
            setShowScenarioModal(false);
            if (project && project.id) {
              navigate(`/project/${project.id}`);
            } else {
              fetchProjects();
            }
          }}
        />
      )}
    </div>
  );
}
