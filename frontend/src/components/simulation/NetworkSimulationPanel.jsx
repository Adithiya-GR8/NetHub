import { useState, useEffect } from 'react';
import api from '../../services/api';
import { Play, CheckCircle2, XCircle, ArrowRight, Activity, ShieldAlert, Cpu } from 'lucide-react';

export default function NetworkSimulationPanel({ projectId, devices = [], onClose }) {
  const [sourceDeviceId, setSourceDeviceId] = useState('');
  const [destinationIp, setDestinationIp] = useState('');
  const [availableIps, setAvailableIps] = useState([]);
  const [simulating, setSimulating] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (devices.length > 0 && !sourceDeviceId) {
      setSourceDeviceId(devices[0].id.toString());
    }
    fetchProjectInterfaces();
  }, [projectId, devices]);

  const fetchProjectInterfaces = async () => {
    try {
      const res = await api.get(`/projects/${projectId}/network/interfaces`);
      const intfs = res.data.data || [];
      const ips = intfs
        .filter(i => i.ipAddress && i.ipAddress.trim() !== '')
        .map(i => ({ ip: i.ipAddress, deviceId: i.deviceId, name: i.name }));
      setAvailableIps(ips);
      if (ips.length > 0 && !destinationIp) {
        setDestinationIp(ips[0].ip);
      }
    } catch (err) {
      console.error("Failed to fetch project interfaces", err);
    }
  };

  const handleSimulate = async (e) => {
    e.preventDefault();
    if (!sourceDeviceId || !destinationIp) {
      alert("Please select a source device and destination IP.");
      return;
    }

    setSimulating(true);
    setResult(null);

    try {
      const res = await api.post(`/projects/${projectId}/simulate`, {
        sourceDeviceId: parseInt(sourceDeviceId),
        destinationIp: destinationIp.trim()
      });
      setResult(res.data.data);
    } catch (err) {
      setResult({
        success: false,
        errorCode: 'SIMULATION_ERROR',
        errorMessage: err.response?.data?.message || 'Failed to execute transmission simulation',
        events: []
      });
    } finally {
      setSimulating(false);
    }
  };

  const getEventBadge = (type) => {
    switch (type) {
      case 'SOURCE_VALIDATED': return 'bg-info text-dark';
      case 'SUBNET_CHECK': return 'bg-secondary';
      case 'ARP_REQUEST': return 'bg-warning text-dark';
      case 'ARP_REPLY': return 'bg-success';
      case 'FRAME_CREATED': return 'bg-primary';
      case 'FRAME_FORWARD': return 'bg-indigo text-white';
      case 'ROUTE_LOOKUP': return 'bg-purple text-white';
      case 'FRAME_REENCAPSULATION': return 'bg-dark';
      case 'DELIVERED': return 'bg-success';
      case 'ERROR': return 'bg-danger';
      default: return 'bg-secondary';
    }
  };

  return (
    <div className="card shadow border-0 h-100 d-flex flex-column">
      <div className="card-header bg-white d-flex justify-content-between align-items-center py-2 border-bottom">
        <span className="fw-bold d-flex align-items-center gap-2 text-primary">
          <Activity size={18} /> Network Simulation Panel
        </span>
        {onClose && <button className="btn-close btn-sm" onClick={onClose}></button>}
      </div>

      <div className="card-body overflow-auto p-3 flex-grow-1">
        <form onSubmit={handleSimulate} className="mb-4 bg-light p-3 rounded border">
          <div className="row g-2 align-items-end">
            <div className="col-md-5">
              <label className="form-label small mb-1 fw-medium text-secondary">Source Device</label>
              <select 
                className="form-select form-select-sm"
                value={sourceDeviceId}
                onChange={e => setSourceDeviceId(e.target.value)}
                required
              >
                {devices.map(d => (
                  <option key={d.id} value={d.id}>{d.name} ({d.type})</option>
                ))}
              </select>
            </div>

            <div className="col-md-5">
              <label className="form-label small mb-1 fw-medium text-secondary">Destination IP</label>
              <div className="input-group input-group-sm">
                <input 
                  type="text" 
                  className="form-control form-control-sm"
                  placeholder="e.g. 192.168.2.10"
                  value={destinationIp}
                  onChange={e => setDestinationIp(e.target.value)}
                  required
                />
                {availableIps.length > 0 && (
                  <button 
                    type="button" 
                    className="btn btn-outline-secondary dropdown-toggle dropdown-toggle-split"
                    data-bs-toggle="dropdown"
                  >
                    <span className="visually-hidden">Quick Select</span>
                  </button>
                )}
                <ul className="dropdown-menu dropdown-menu-end shadow-sm" style={{ maxHeight: '200px', overflowY: 'auto' }}>
                  {availableIps.map((ipObj, idx) => (
                    <li key={idx}>
                      <button 
                        type="button" 
                        className="dropdown-menu-item dropdown-item small py-1"
                        onClick={() => setDestinationIp(ipObj.ip)}
                      >
                        <span className="fw-mono">{ipObj.ip}</span> <span className="text-muted">({ipObj.name})</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="col-md-2">
              <button 
                type="submit" 
                className="btn btn-sm btn-primary w-100 d-flex align-items-center justify-content-center gap-1"
                disabled={simulating}
              >
                <Play size={14} /> {simulating ? 'Simulating...' : 'Simulate'}
              </button>
            </div>
          </div>
        </form>

        {result && (
          <div className="simulation-results">
            {/* Status Header */}
            <div className={`alert p-3 mb-3 d-flex align-items-center justify-content-between alert-${result.success ? 'success' : 'danger'}`}>
              <div className="d-flex align-items-center gap-2">
                {result.success ? <CheckCircle2 size={22} /> : <XCircle size={22} />}
                <div>
                  <h6 className="mb-0 fw-bold">{result.success ? 'Transmission Successful!' : 'Transmission Failed'}</h6>
                  <span className="small">
                    {result.success 
                      ? `${result.source} successfully delivered packet to ${result.destination}`
                      : `${result.errorCode || 'ERROR'}: ${result.errorMessage}`}
                  </span>
                </div>
              </div>
            </div>

            {/* Path Visualization */}
            {result.path && result.path.length > 0 && (
              <div className="card mb-3 border shadow-sm">
                <div className="card-header bg-white py-1 px-3 fw-bold small text-secondary">
                  Calculated Network Path
                </div>
                <div className="card-body py-2 px-3">
                  <div className="d-flex align-items-center flex-wrap gap-2">
                    {result.path.map((hop, idx) => (
                      <span key={idx} className="d-flex align-items-center gap-2">
                        <span className="badge bg-light text-dark border px-2 py-1 font-monospace fw-semibold">
                          <Cpu size={12} className="me-1" /> {hop}
                        </span>
                        {idx < result.path.length - 1 && <ArrowRight size={14} className="text-muted" />}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Event Timeline */}
            <div className="card border shadow-sm">
              <div className="card-header bg-white py-2 px-3 fw-bold small text-secondary">
                Simulation Event Log
              </div>
              <div className="card-body p-0">
                <div className="list-group list-group-flush">
                  {result.events && result.events.map((evt, idx) => (
                    <div key={idx} className="list-group-item p-2 px-3 border-bottom-0">
                      <div className="d-flex align-items-start justify-content-between">
                        <div>
                          <span className={`badge ${getEventBadge(evt.type)} me-2 micro`}>{evt.type}</span>
                          <span className="fw-semibold small text-dark me-2">[{evt.device}]</span>
                          <span className="small text-secondary">{evt.description}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
