import { useState, useEffect } from 'react';
import { Layers, Activity, Sliders, CheckCircle, Info } from 'lucide-react';
import api from '../../services/api';

export default function SwitchingComparisonModal({ onClose }) {
  const [messageSizeKb, setMessageSizeKb] = useState(1000); // 1 MB
  const [hops, setHops] = useState(4);
  const [bandwidthMbps, setBandwidthMbps] = useState(100);
  const [comparison, setComparison] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchComparison();
  }, [messageSizeKb, hops, bandwidthMbps]);

  const fetchComparison = async () => {
    setLoading(true);
    try {
      const bytes = messageSizeKb * 1024;
      const res = await api.get(`/switching/compare`, {
        params: {
          messageSizeBytes: bytes,
          hops: hops,
          bandwidthMbps: bandwidthMbps
        }
      });
      setComparison(res.data.data);
    } catch (err) {
      console.error("Failed to compare switching techniques", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.6)', zIndex: 1050 }}>
      <div className="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div className="modal-content shadow-lg border-0">
          {/* Header */}
          <div className="modal-header bg-primary text-white py-2 px-3">
            <div className="d-flex align-items-center gap-2">
              <Layers size={20} />
              <h6 className="modal-title mb-0 fw-bold">Switching Techniques: Circuit vs Message vs Packet Switching</h6>
            </div>
            <button className="btn-close btn-close-white" onClick={onClose}></button>
          </div>

          {/* Controls Bar */}
          <div className="modal-body p-3 bg-light border-bottom">
            <div className="row g-3 align-items-end">
              <div className="col-md-4">
                <label className="form-label small fw-bold text-muted mb-1">
                  Message Payload Size: <span className="text-primary">{messageSizeKb} KB</span> ({(messageSizeKb / 1024).toFixed(2)} MB)
                </label>
                <input 
                  type="range" 
                  className="form-range" 
                  min="64" 
                  max="10000" 
                  step="64"
                  value={messageSizeKb}
                  onChange={e => setMessageSizeKb(parseInt(e.target.value))}
                />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-bold text-muted mb-1">
                  Intermediate Hop Count: <span className="text-primary">{hops} Hops</span>
                </label>
                <input 
                  type="range" 
                  className="form-range" 
                  min="1" 
                  max="10" 
                  step="1"
                  value={hops}
                  onChange={e => setHops(parseInt(e.target.value))}
                />
              </div>
              <div className="col-md-4">
                <label className="form-label small fw-bold text-muted mb-1">
                  Link Bandwidth: <span className="text-primary">{bandwidthMbps} Mbps</span>
                </label>
                <input 
                  type="range" 
                  className="form-range" 
                  min="10" 
                  max="1000" 
                  step="10"
                  value={bandwidthMbps}
                  onChange={e => setBandwidthMbps(parseInt(e.target.value))}
                />
              </div>
            </div>
          </div>

          {/* Comparison Cards */}
          <div className="modal-body p-4">
            {loading && !comparison && <div className="text-center p-4">Computing switching parameters...</div>}

            {comparison && (
              <div className="row g-3">
                {/* 1. Circuit Switching */}
                <div className="col-md-4">
                  <div className="card h-100 border-primary border-opacity-50 shadow-sm">
                    <div className="card-header bg-primary bg-opacity-10 text-primary fw-bold d-flex justify-content-between align-items-center">
                      <span>Circuit Switching</span>
                      <span className="badge bg-primary">Dedicated</span>
                    </div>
                    <div className="card-body">
                      <div className="mb-3 text-center p-2 bg-light rounded">
                        <div className="text-muted micro text-uppercase">Total End-to-End Latency</div>
                        <div className="h3 mb-0 text-primary fw-bold">
                          {comparison.circuitSwitching?.totalLatencyMs?.toFixed(2)} <span className="small">ms</span>
                        </div>
                      </div>

                      <ul className="list-group list-group-flush small mb-3">
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Call Setup Delay:</span>
                          <span className="fw-semibold">{comparison.circuitSwitching?.setupDelayMs?.toFixed(2)} ms</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Tx & Prop Delay:</span>
                          <span className="fw-semibold">{(comparison.circuitSwitching?.transmissionDelayMs + comparison.circuitSwitching?.propagationDelayMs)?.toFixed(2)} ms</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Queuing Delay:</span>
                          <span className="fw-semibold text-success">0 ms (Dedicated)</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Line Efficiency:</span>
                          <span className="fw-bold">{comparison.circuitSwitching?.lineEfficiencyPercent?.toFixed(1)}%</span>
                        </li>
                      </ul>

                      <h6 className="micro text-uppercase text-muted fw-bold">Execution Workflow:</h6>
                      <ol className="small ps-3 text-muted" style={{ fontSize: '0.78rem' }}>
                        {comparison.circuitSwitching?.executionSteps?.map((s, i) => (
                          <li key={i} className="mb-1">{s}</li>
                        ))}
                      </ol>
                    </div>
                  </div>
                </div>

                {/* 2. Message Switching */}
                <div className="col-md-4">
                  <div className="card h-100 border-warning border-opacity-50 shadow-sm">
                    <div className="card-header bg-warning bg-opacity-10 text-warning-emphasis fw-bold d-flex justify-content-between align-items-center">
                      <span>Message Switching</span>
                      <span className="badge bg-warning text-dark">Store & Fwd</span>
                    </div>
                    <div className="card-body">
                      <div className="mb-3 text-center p-2 bg-light rounded">
                        <div className="text-muted micro text-uppercase">Total End-to-End Latency</div>
                        <div className="h3 mb-0 text-warning-emphasis fw-bold">
                          {comparison.messageSwitching?.totalLatencyMs?.toFixed(2)} <span className="small">ms</span>
                        </div>
                      </div>

                      <ul className="list-group list-group-flush small mb-3">
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Call Setup Delay:</span>
                          <span className="fw-semibold text-success">0 ms (No setup)</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Per-Hop Store & Fwd:</span>
                          <span className="fw-semibold text-danger">{(comparison.messageSwitching?.transmissionDelayMs / hops)?.toFixed(2)} ms/hop</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Queuing Latency:</span>
                          <span className="fw-semibold">{comparison.messageSwitching?.queuingDelayMs?.toFixed(2)} ms</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Line Efficiency:</span>
                          <span className="fw-bold">{comparison.messageSwitching?.lineEfficiencyPercent?.toFixed(1)}%</span>
                        </li>
                      </ul>

                      <h6 className="micro text-uppercase text-muted fw-bold">Execution Workflow:</h6>
                      <ol className="small ps-3 text-muted" style={{ fontSize: '0.78rem' }}>
                        {comparison.messageSwitching?.executionSteps?.map((s, i) => (
                          <li key={i} className="mb-1">{s}</li>
                        ))}
                      </ol>
                    </div>
                  </div>
                </div>

                {/* 3. Packet Switching */}
                <div className="col-md-4">
                  <div className="card h-100 border-success border-2 shadow">
                    <div className="card-header bg-success text-white fw-bold d-flex justify-content-between align-items-center">
                      <span>Packet Switching</span>
                      <span className="badge bg-white text-success">Pipelined (Best)</span>
                    </div>
                    <div className="card-body">
                      <div className="mb-3 text-center p-2 bg-success bg-opacity-10 rounded">
                        <div className="text-muted micro text-uppercase">Total End-to-End Latency</div>
                        <div className="h3 mb-0 text-success fw-bold">
                          {comparison.packetSwitching?.totalLatencyMs?.toFixed(2)} <span className="small">ms</span>
                        </div>
                      </div>

                      <ul className="list-group list-group-flush small mb-3">
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Total Segments/Packets:</span>
                          <span className="fw-bold text-primary">{comparison.packetSwitching?.totalPackets?.toLocaleString()}</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Pipelined Line Latency:</span>
                          <span className="fw-semibold">{comparison.packetSwitching?.transmissionDelayMs?.toFixed(2)} ms</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Header Overhead:</span>
                          <span className="fw-semibold text-muted">~2.7% (IP+Ethernet)</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0">
                          <span className="text-muted">Statistical Multiplexing:</span>
                          <span className="fw-bold text-success">Optimal</span>
                        </li>
                      </ul>

                      <h6 className="micro text-uppercase text-muted fw-bold">Execution Workflow:</h6>
                      <ol className="small ps-3 text-muted" style={{ fontSize: '0.78rem' }}>
                        {comparison.packetSwitching?.executionSteps?.map((s, i) => (
                          <li key={i} className="mb-1">{s}</li>
                        ))}
                      </ol>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {comparison?.recommendation && (
              <div className="alert alert-info mt-3 mb-0 d-flex gap-2 align-items-start small">
                <Info size={18} className="flex-shrink-0 mt-1" />
                <div>
                  <strong>Architectural Conclusion:</strong> {comparison.recommendation}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

