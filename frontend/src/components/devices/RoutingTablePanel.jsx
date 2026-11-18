import { useState, useEffect } from 'react';
import api from '../../services/api';
import { Route as RouteIcon, Plus, Trash2, RefreshCw } from 'lucide-react';

export default function RoutingTablePanel({ deviceId, availableInterfaces = [] }) {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAdd, setShowAdd] = useState(false);

  const [newRoute, setNewRoute] = useState({
    destinationNetwork: '',
    subnetMask: '255.255.255.0',
    nextHop: '',
    outgoingInterface: '',
    metric: 1
  });

  useEffect(() => {
    if (deviceId) fetchRoutingTable();
  }, [deviceId]);

  const fetchRoutingTable = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/devices/${deviceId}/routes`);
      setRoutes(res.data.data?.routes || []);
    } catch (err) {
      console.error("Failed to fetch routing table", err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddStaticRoute = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/devices/${deviceId}/routes`, newRoute);
      setNewRoute({
        destinationNetwork: '',
        subnetMask: '255.255.255.0',
        nextHop: '',
        outgoingInterface: '',
        metric: 1
      });
      setShowAdd(false);
      fetchRoutingTable();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to add static route");
    }
  };

  const handleDeleteRoute = async (routeId) => {
    if (!routeId) return;
    try {
      await api.delete(`/routes/${routeId}`);
      fetchRoutingTable();
    } catch (err) {
      alert("Failed to delete static route");
    }
  };

  return (
    <div className="card border-0 shadow-sm mb-3">
      <div className="card-header bg-white d-flex justify-content-between align-items-center py-2">
        <span className="fw-semibold small d-flex align-items-center gap-1 text-primary">
          <RouteIcon size={14} /> Routing Table
        </span>
        <div className="d-flex gap-1">
          <button 
            className="btn btn-xs btn-outline-primary d-flex align-items-center gap-1"
            onClick={() => setShowAdd(!showAdd)}
          >
            <Plus size={12} /> Add Static
          </button>
          <button 
            className="btn btn-xs btn-outline-secondary"
            onClick={fetchRoutingTable}
            disabled={loading}
          >
            <RefreshCw size={12} className={loading ? "spin" : ""} />
          </button>
        </div>
      </div>

      <div className="card-body p-0">
        {showAdd && (
          <form onSubmit={handleAddStaticRoute} className="p-3 bg-light border-bottom">
            <h6 className="small fw-bold text-dark mb-2">New Static Route</h6>
            <div className="mb-2">
              <label className="form-label micro mb-0 text-muted">Destination Network</label>
              <input 
                type="text" 
                className="form-control form-control-sm" 
                placeholder="e.g. 192.168.2.0"
                value={newRoute.destinationNetwork}
                onChange={e => setNewRoute({...newRoute, destinationNetwork: e.target.value})}
                required
              />
            </div>
            <div className="mb-2">
              <label className="form-label micro mb-0 text-muted">Subnet Mask</label>
              <input 
                type="text" 
                className="form-control form-control-sm" 
                placeholder="e.g. 255.255.255.0"
                value={newRoute.subnetMask}
                onChange={e => setNewRoute({...newRoute, subnetMask: e.target.value})}
                required
              />
            </div>
            <div className="mb-2">
              <label className="form-label micro mb-0 text-muted">Next Hop IP</label>
              <input 
                type="text" 
                className="form-control form-control-sm" 
                placeholder="e.g. 192.168.1.1"
                value={newRoute.nextHop}
                onChange={e => setNewRoute({...newRoute, nextHop: e.target.value})}
              />
            </div>
            <div className="mb-2">
              <label className="form-label micro mb-0 text-muted">Outgoing Interface</label>
              <select 
                className="form-select form-select-sm"
                value={newRoute.outgoingInterface}
                onChange={e => setNewRoute({...newRoute, outgoingInterface: e.target.value})}
              >
                <option value="">Select interface...</option>
                {availableInterfaces.map(i => (
                  <option key={i.id} value={i.name}>{i.name}</option>
                ))}
              </select>
            </div>
            <div className="d-flex justify-content-end gap-1 mt-2">
              <button type="button" className="btn btn-xs btn-secondary" onClick={() => setShowAdd(false)}>Cancel</button>
              <button type="submit" className="btn btn-xs btn-primary">Save Route</button>
            </div>
          </form>
        )}

        {routes.length === 0 ? (
          <p className="text-muted small p-3 mb-0 text-center">No routes defined.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm table-striped mb-0 text-nowrap align-middle" style={{ fontSize: '0.78rem' }}>
              <thead className="table-light">
                <tr>
                  <th>Destination</th>
                  <th>Mask</th>
                  <th>Next Hop</th>
                  <th>Intf</th>
                  <th>Type</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {routes.map((r, idx) => (
                  <tr key={idx}>
                    <td className="font-monospace fw-medium">{r.destinationNetwork}</td>
                    <td className="font-monospace">{r.subnetMask}</td>
                    <td className="font-monospace">{r.nextHop || '-'}</td>
                    <td>{r.outgoingInterface || '-'}</td>
                    <td>
                      <span className={`badge bg-${r.routeType === 'CONNECTED' ? 'success' : 'primary'}`}>
                        {r.routeType}
                      </span>
                    </td>
                    <td>
                      {r.routeType === 'STATIC' && r.id && (
                        <button 
                          className="btn btn-link text-danger p-0 border-0" 
                          onClick={() => handleDeleteRoute(r.id)}
                          title="Delete Route"
                        >
                          <Trash2 size={13} />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
