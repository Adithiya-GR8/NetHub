import { useState, useEffect } from 'react';
import { 
  BookOpen, CheckCircle, AlertTriangle, Play, RefreshCw, X, 
  Search, Filter, ShieldCheck, ShieldAlert, Cpu, ArrowRight 
} from 'lucide-react';
import api from '../../services/api';
import { PRELOADED_SCENARIOS } from '../../data/demonstrationScenariosData';

export default function DemonstrationScenariosModal({ currentProjectId, onScenarioLoaded, onClose }) {
  const [scenarios, setScenarios] = useState(PRELOADED_SCENARIOS);
  const [activeTab, setActiveTab] = useState('VALID'); // 'VALID' or 'INVALID'
  const [searchQuery, setSearchQuery] = useState('');
  const [difficultyFilter, setDifficultyFilter] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    fetchScenarios();
  }, []);

  const fetchScenarios = async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      const res = await api.get('/scenarios');
      if (res.data?.data && res.data.data.length > 0) {
        setScenarios(res.data.data);
      } else {
        setScenarios(PRELOADED_SCENARIOS);
      }
    } catch (err) {
      console.warn("Using preloaded demonstration scenarios fallback", err);
      setScenarios(PRELOADED_SCENARIOS);
    } finally {
      setLoading(false);
    }
  };

  const handleLoadScenario = async (scenario) => {
    const hasConfirmed = window.confirm(
      `Load "${scenario.name}" (${scenario.code})?\n\nThis will load the preconfigured ${scenario.category.toLowerCase()} topology into your workspace.`
    );
    if (!hasConfirmed) return;

    const identifier = scenario.id || scenario.code;
    setActionLoadingId(identifier);
    setErrorMessage('');
    try {
      const res = await api.post(`/scenarios/${identifier}/load`, null, {
        params: currentProjectId ? { targetProjectId: currentProjectId } : {}
      });
      const project = res.data.data;
      if (onScenarioLoaded) {
        onScenarioLoaded(project, scenario);
      }
      onClose();
    } catch (err) {
      console.error("Failed to load scenario", err);
      setErrorMessage(err.response?.data?.message || "Failed to load scenario into workspace.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleResetScenario = async (scenario) => {
    if (!window.confirm(`Reset scenario "${scenario.name}" back to original seeded default state?`)) return;

    setActionLoadingId(scenario.id);
    try {
      await api.post(`/scenarios/${scenario.id}/reset`);
      await fetchScenarios();
      alert(`Scenario "${scenario.name}" has been reset to its pristine seeded configuration.`);
    } catch (err) {
      console.error("Failed to reset scenario", err);
      alert("Failed to reset scenario.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const filteredScenarios = scenarios.filter(s => {
    if (s.category !== activeTab) return false;
    if (difficultyFilter !== 'ALL' && s.difficulty !== difficultyFilter) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
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
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.65)', zIndex: 1050 }}>
      <div className="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div className="modal-content shadow-lg border-0">
          
          {/* Header */}
          <div className="modal-header bg-dark text-white py-3 px-4">
            <div className="d-flex align-items-center gap-2">
              <BookOpen size={22} className="text-warning" />
              <div>
                <h5 className="modal-title mb-0 fw-bold">Demonstration Scenarios</h5>
                <p className="text-muted small mb-0">Preloaded valid & invalid networking topologies for direct simulation & academic demonstration</p>
              </div>
            </div>
            <button className="btn-close btn-close-white" onClick={onClose}></button>
          </div>

          {/* Subheader / Tabs & Filters */}
          <div className="p-3 bg-light border-bottom">
            <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
              {/* Category Tabs */}
              <div className="btn-group" role="group">
                <button 
                  type="button" 
                  className={`btn btn-sm d-flex align-items-center gap-2 ${activeTab === 'VALID' ? 'btn-success fw-bold text-white' : 'btn-outline-success'}`}
                  onClick={() => setActiveTab('VALID')}
                >
                  <ShieldCheck size={16} /> Valid Configurations ({validCount})
                </button>
                <button 
                  type="button" 
                  className={`btn btn-sm d-flex align-items-center gap-2 ${activeTab === 'INVALID' ? 'btn-danger fw-bold text-white' : 'btn-outline-danger'}`}
                  onClick={() => setActiveTab('INVALID')}
                >
                  <ShieldAlert size={16} /> Invalid Configurations ({invalidCount})
                </button>
              </div>

              {/* Search & Difficulty Filter */}
              <div className="d-flex gap-2 align-items-center">
                <div className="input-group input-group-sm" style={{ width: '220px' }}>
                  <span className="input-group-text bg-white"><Search size={14} /></span>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="Search scenarios..." 
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                  />
                  {searchQuery && (
                    <button className="btn btn-outline-secondary" onClick={() => setSearchQuery('')}>✕</button>
                  )}
                </div>

                <div className="d-flex align-items-center gap-1">
                  <Filter size={14} className="text-muted" />
                  <select 
                    className="form-select form-select-sm"
                    value={difficultyFilter}
                    onChange={e => setDifficultyFilter(e.target.value)}
                    style={{ width: '135px' }}
                  >
                    <option value="ALL">All Levels</option>
                    <option value="BEGINNER">Beginner</option>
                    <option value="INTERMEDIATE">Intermediate</option>
                    <option value="ADVANCED">Advanced</option>
                  </select>
                </div>
              </div>
            </div>

            {activeTab === 'INVALID' && (
              <div className="alert alert-warning py-1 px-3 mt-2 mb-0 small d-flex align-items-center gap-2">
                <AlertTriangle size={16} className="flex-shrink-0 text-warning" />
                <span>
                  <strong>Invalid configurations</strong> contain intentional network misconfigurations. Loading them allows you to inspect faulty devices, trigger real validation and simulation errors, and correct the issues.
                </span>
              </div>
            )}
          </div>

          {/* Modal Body: Scenario Cards Grid */}
          <div className="modal-body p-4 bg-light" style={{ minHeight: '380px' }}>
            {errorMessage && (
              <div className="alert alert-danger py-2 px-3 mb-3 small d-flex align-items-center gap-2">
                <AlertTriangle size={16} />
                <span>{errorMessage}</span>
              </div>
            )}

            {loading ? (
              <div className="text-center py-5">
                <div className="spinner-border text-primary" role="status"></div>
                <p className="text-muted small mt-2">Loading demonstration scenarios...</p>
              </div>
            ) : filteredScenarios.length === 0 ? (
              <div className="text-center py-5 text-muted">
                <p className="mb-0">No scenarios found matching your criteria.</p>
              </div>
            ) : (
              <div className="row g-3">
                {filteredScenarios.map((scenario) => {
                  const isInvalid = scenario.category === 'INVALID';
                  const diffColor = scenario.difficulty === 'BEGINNER' ? 'success' : scenario.difficulty === 'INTERMEDIATE' ? 'primary' : 'danger';
                  const conceptsList = scenario.concepts ? scenario.concepts.split(',').map(c => c.trim()) : [];

                  return (
                    <div key={scenario.id} className="col-lg-6 col-12">
                      <div className={`card h-100 shadow-sm border ${isInvalid ? 'border-danger-subtle' : 'border-success-subtle'}`}>
                        
                        {/* Card Header */}
                        <div className="card-header bg-white py-2 px-3 d-flex justify-content-between align-items-center border-bottom">
                          <div className="d-flex align-items-center gap-2">
                            <span className={`badge ${isInvalid ? 'bg-danger' : 'bg-success'} font-monospace px-2 py-1`}>
                              {scenario.code}
                            </span>
                            <h6 className="mb-0 fw-bold text-dark">{scenario.name}</h6>
                          </div>
                          <div className="d-flex align-items-center gap-1">
                            <span className={`badge bg-${diffColor}-subtle text-${diffColor} border border-${diffColor}-subtle micro`}>
                              {scenario.difficulty}
                            </span>
                            <span className="badge bg-secondary-subtle text-secondary micro">
                              {scenario.deviceCount} Devices
                            </span>
                          </div>
                        </div>

                        {/* Card Body */}
                        <div className="card-body p-3 d-flex flex-column justify-content-between">
                          <div>
                            <p className="text-secondary small mb-2">{scenario.description}</p>

                            {/* Concepts Pills */}
                            {conceptsList.length > 0 && (
                              <div className="d-flex flex-wrap gap-1 mb-2">
                                {conceptsList.map((concept, idx) => (
                                  <span key={idx} className="badge bg-light text-muted border micro">
                                    {concept}
                                  </span>
                                ))}
                              </div>
                            )}

                            {/* Expected Result / Expected Error Box */}
                            {scenario.expectedResult && !isInvalid && (
                              <div className="p-2 rounded bg-success-subtle border border-success-subtle mb-3 small text-success-emphasis">
                                <div className="fw-bold micro text-uppercase text-success mb-1 d-flex align-items-center gap-1">
                                  <CheckCircle size={12} /> Expected Behavior
                                </div>
                                <div className="micro">{scenario.expectedResult}</div>
                              </div>
                            )}

                            {isInvalid && (
                              <div className="p-2 rounded bg-danger-subtle border border-danger-subtle mb-3 small text-danger-emphasis">
                                <div className="fw-bold micro text-uppercase text-danger mb-1 d-flex align-items-center gap-1">
                                  <AlertTriangle size={12} /> Expected Error / Diagnosis
                                </div>
                                <div className="micro mb-1">{scenario.expectedErrors || scenario.expectedResult}</div>
                              </div>
                            )}
                          </div>

                          {/* Action Buttons */}
                          <div className="d-flex justify-content-between align-items-center pt-2 border-top">
                            <button 
                              type="button" 
                              className="btn btn-xs btn-outline-secondary d-flex align-items-center gap-1 text-muted"
                              style={{ fontSize: '0.75rem' }}
                              onClick={() => handleResetScenario(scenario)}
                              disabled={actionLoadingId === scenario.id}
                              title="Reset this scenario's template back to pristine default"
                            >
                              <RefreshCw size={12} /> Reset Seed
                            </button>

                            <button 
                              type="button" 
                              className={`btn btn-sm ${isInvalid ? 'btn-danger' : 'btn-success'} d-flex align-items-center gap-1 px-3`}
                              onClick={() => handleLoadScenario(scenario)}
                              disabled={actionLoadingId === scenario.id}
                            >
                              {actionLoadingId === scenario.id ? (
                                <>
                                  <span className="spinner-border spinner-border-sm me-1" role="status"></span> Loading...
                                </>
                              ) : (
                                <>
                                  <Play size={14} /> Load Scenario <ArrowRight size={14} />
                                </>
                              )}
                            </button>
                          </div>

                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="modal-footer bg-white py-2 px-4 d-flex justify-content-between">
            <div className="micro text-muted">
              Showing {filteredScenarios.length} scenarios ({activeTab.toLowerCase()} category)
            </div>
            <button type="button" className="btn btn-sm btn-secondary" onClick={onClose}>
              Close
            </button>
          </div>

        </div>
      </div>
    </div>
  );
}
