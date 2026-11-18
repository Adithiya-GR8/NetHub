import { useState, useEffect } from 'react';
import api from '../../services/api';
import { RefreshCw, List } from 'lucide-react';

export default function ArpTablePanel({ deviceId }) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (deviceId) fetchArpTable();
  }, [deviceId]);

  const fetchArpTable = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/devices/${deviceId}/arp-table`);
      setEntries(res.data.data || []);
    } catch (err) {
      console.error("Failed to fetch ARP table", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card border-0 shadow-sm mb-3">
      <div className="card-header bg-white d-flex justify-content-between align-items-center py-2">
        <span className="fw-semibold small d-flex align-items-center gap-1 text-primary">
          <List size={14} /> ARP Cache Table
        </span>
        <button 
          className="btn btn-xs btn-outline-secondary d-flex align-items-center gap-1"
          onClick={fetchArpTable}
          disabled={loading}
        >
          <RefreshCw size={12} className={loading ? "spin" : ""} /> Refresh
        </button>
      </div>
      <div className="card-body p-0">
        {entries.length === 0 ? (
          <p className="text-muted small p-3 mb-0 text-center">ARP cache is empty.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm table-striped mb-0 text-nowrap align-middle" style={{ fontSize: '0.8rem' }}>
              <thead className="table-light">
                <tr>
                  <th>IP Address</th>
                  <th>MAC Address</th>
                  <th>State</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry, idx) => (
                  <tr key={idx}>
                    <td className="font-monospace fw-medium">{entry.ipAddress}</td>
                    <td className="font-monospace">{entry.macAddress}</td>
                    <td>
                      <span className="badge bg-success">{entry.state}</span>
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
