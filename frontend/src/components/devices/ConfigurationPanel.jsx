import { useState, useEffect } from 'react';
import api from '../../services/api';
import MacAddressTablePanel from './MacAddressTablePanel';
import RoutingTablePanel from './RoutingTablePanel';
import ArpTablePanel from './ArpTablePanel';

export default function ConfigurationPanel({ deviceId, onClose, onDeviceUpdated }) {
  const [device, setDevice] = useState(null);
  const [interfaces, setInterfaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('general');

  useEffect(() => {
    if (deviceId) {
      fetchDeviceDetails();
      setActiveTab('general');
    }
  }, [deviceId]);

  const fetchDeviceDetails = async () => {
    setLoading(true);
    try {
      const [devRes, intRes] = await Promise.all([
        api.get(`/devices/${deviceId}`),
        api.get(`/devices/${deviceId}/interfaces`)
      ]);
      setDevice(devRes.data.data);
      setInterfaces(intRes.data.data);
    } catch (error) {
      console.error("Error fetching device config", error);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveDevice = async (e) => {
    e.preventDefault();
    try {
      const res = await api.put(`/devices/${device.id}`, device);
      onDeviceUpdated(res.data.data);
      alert("Device updated successfully!");
    } catch (error) {
      alert("Failed to update device");
    }
  };

  const handleSaveInterface = async (intf) => {
    try {
      await api.put(`/interfaces/${intf.id}`, intf);
      alert("Interface saved successfully!");
    } catch (error) {
      alert(error.response?.data?.message || "Failed to update interface");
    }
  };

  if (!deviceId) return null;
  if (loading) return <div className="config-panel p-4 text-center">Loading Configuration...</div>;

  return (
    <div className="config-panel d-flex flex-column h-100 shadow-lg bg-white border-start" style={{ width: '380px', zIndex: 10 }}>
      <div className="d-flex justify-content-between align-items-center p-3 border-bottom bg-light">
        <div>
          <h6 className="mb-0 fw-bold">{device?.name}</h6>
          <span className="badge bg-secondary micro">{device?.type}</span>
        </div>
        <button className="btn-close" onClick={onClose}></button>
      </div>

      {/* Tabs */}
      <ul className="nav nav-tabs px-2 pt-2 border-bottom bg-light" style={{ fontSize: '0.8rem' }}>
        <li className="nav-item">
          <button 
            className={`nav-link py-1 px-2 ${activeTab === 'general' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('general')}
          >
            Config
          </button>
        </li>
        {device?.type === 'SWITCH' && (
          <li className="nav-item">
            <button 
              className={`nav-link py-1 px-2 ${activeTab === 'mac' ? 'active fw-bold' : ''}`}
              onClick={() => setActiveTab('mac')}
            >
              MAC Table
            </button>
          </li>
        )}
        {device?.type === 'ROUTER' && (
          <li className="nav-item">
            <button 
              className={`nav-link py-1 px-2 ${activeTab === 'routing' ? 'active fw-bold' : ''}`}
              onClick={() => setActiveTab('routing')}
            >
              Routes
            </button>
          </li>
        )}
        <li className="nav-item">
          <button 
            className={`nav-link py-1 px-2 ${activeTab === 'arp' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('arp')}
          >
            ARP Cache
          </button>
        </li>
      </ul>

      {/* Tab Content */}
      <div className="flex-grow-1 overflow-auto p-3">
        {activeTab === 'general' && (
          <>
            {device && (
              <form onSubmit={handleSaveDevice} className="mb-4 bg-light p-3 rounded border">
                <h6 className="text-primary border-bottom pb-1 small fw-bold">Device Settings</h6>
                <div className="mb-2">
                  <label className="form-label small mb-1">Device Name</label>
                  <input 
                    type="text" 
                    className="form-control form-control-sm" 
                    value={device.name}
                    onChange={e => setDevice({...device, name: e.target.value})}
                  />
                </div>
                <div className="mb-2">
                  <label className="form-label small mb-1">Status</label>
                  <select 
                    className="form-select form-select-sm"
                    value={device.status}
                    onChange={e => setDevice({...device, status: e.target.value})}
                  >
                    <option value="ONLINE">ONLINE</option>
                    <option value="OFFLINE">OFFLINE</option>
                  </select>
                </div>
                <button type="submit" className="btn btn-sm btn-outline-primary w-100 mt-2">Save Device</button>
              </form>
            )}

            <h6 className="text-primary border-bottom pb-1 small fw-bold">Interfaces</h6>
            {interfaces.length === 0 ? (
              <p className="text-muted small">No interfaces found.</p>
            ) : (
              interfaces.map(intf => (
                <div key={intf.id} className="card mb-3 border-0 shadow-sm">
                  <div className="card-body p-2 bg-light rounded border">
                    <div className="fw-bold mb-2 border-bottom pb-1 d-flex justify-content-between align-items-center">
                      <span className="small">{intf.name}</span>
                      <span className={`badge bg-${intf.status === 'UP' ? 'success' : 'danger'}`}>{intf.status}</span>
                    </div>

                    <div className="mb-1 d-flex justify-content-between align-items-center">
                      <span className="micro text-muted">MAC:</span>
                      <span className="micro font-monospace">{intf.macAddress}</span>
                    </div>

                    <div className="mb-2">
                      <label className="form-label micro mb-0 text-muted">IP Address</label>
                      <input 
                        type="text" 
                        className="form-control form-control-sm font-monospace" 
                        value={intf.ipAddress || ''}
                        onChange={e => {
                          const updated = interfaces.map(i => i.id === intf.id ? {...i, ipAddress: e.target.value} : i);
                          setInterfaces(updated);
                        }}
                        placeholder="e.g. 192.168.1.10"
                      />
                    </div>
                    <div className="mb-2">
                      <label className="form-label micro mb-0 text-muted">Subnet Mask</label>
                      <input 
                        type="text" 
                        className="form-control form-control-sm font-monospace" 
                        value={intf.subnetMask || ''}
                        onChange={e => {
                          const updated = interfaces.map(i => i.id === intf.id ? {...i, subnetMask: e.target.value} : i);
                          setInterfaces(updated);
                        }}
                        placeholder="e.g. 255.255.255.0"
                      />
                    </div>
                    <div className="mb-2">
                      <label className="form-label micro mb-0 text-muted">Default Gateway</label>
                      <input 
                        type="text" 
                        className="form-control form-control-sm font-monospace" 
                        value={intf.gateway || ''}
                        onChange={e => {
                          const updated = interfaces.map(i => i.id === intf.id ? {...i, gateway: e.target.value} : i);
                          setInterfaces(updated);
                        }}
                        placeholder="e.g. 192.168.1.1"
                      />
                    </div>
                    <button 
                      className="btn btn-sm btn-primary w-100" 
                      onClick={() => handleSaveInterface(interfaces.find(i => i.id === intf.id))}
                    >
                      Save {intf.name}
                    </button>
                  </div>
                </div>
              ))
            )}
          </>
        )}

        {activeTab === 'mac' && device?.type === 'SWITCH' && (
          <MacAddressTablePanel deviceId={deviceId} />
        )}

        {activeTab === 'routing' && device?.type === 'ROUTER' && (
          <RoutingTablePanel deviceId={deviceId} availableInterfaces={interfaces} />
        )}

        {activeTab === 'arp' && (
          <ArpTablePanel deviceId={deviceId} />
        )}
      </div>
    </div>
  );
}
