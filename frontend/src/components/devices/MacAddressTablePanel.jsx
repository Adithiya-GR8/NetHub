import { useState, useEffect } from 'react';
import api from '../../services/api';
import { RefreshCw, Database } from 'lucide-react';

export default function MacAddressTablePanel({ deviceId }) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (deviceId) fetchMacTable();
  }, [deviceId]);

  const fetchMacTable = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/devices/${deviceId}/mac-table`);
      setEntries(res.data.data || []);
    } catch (err) {
      console.error("Failed to fetch MAC table", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card border-0 shadow-sm mb-3">
      <div className="card-header bg-white d-flex justify-content-between align-items-center py-2">
        <span className="fw-semibold small d-flex align-items-center gap-1 text-primary">
          <Database size={14} /> MAC Address Table
        </span>
        <button 
          className="btn btn-xs btn-outline-secondary d-flex align-items-center gap-1"
          onClick={fetchMacTable}
          disabled={loading}
          title="Refresh MAC Table"
        >
          <RefreshCw size={12} className={loading ? "spin" : ""} /> Refresh
        </button>
      </div>
      <div className="card-body p-0">
        {entries.length === 0 ? (
          <p className="text-muted small p-3 mb-0 text-center">No learned MAC addresses yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm table-striped mb-0 text-nowrap align-middle" style={{ fontSize: '0.8rem' }}>
              <thead className="table-light">
                <tr>
                  <th>MAC Address</th>
                  <th>Port</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry, idx) => (
                  <tr key={idx}>
                    <td className="font-monospace fw-medium">{entry.macAddress}</td>
                    <td>{entry.port}</td>
                    <td>
                      <span className={`badge bg-${entry.status === 'STATIC' ? 'info' : 'secondary'}`}>
                        {entry.status}
                      </span>
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
