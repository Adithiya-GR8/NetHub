import { useState, useEffect } from 'react';
import { Layers, ArrowRight, ArrowLeft, Shield, Check, Info } from 'lucide-react';
import api from '../../services/api';

export default function OsiVisualizerModal({ onClose }) {
  const [trace, setTrace] = useState(null);
  const [activeStep, setActiveStep] = useState(0);
  const [mode, setMode] = useState('encapsulation'); // 'encapsulation' or 'decapsulation'
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTrace();
  }, []);

  const fetchTrace = async () => {
    setLoading(true);
    try {
      const res = await api.get('/simulation/osi-trace', {
        params: {
          sourceHost: 'Client-PC',
          sourceMac: '00:AA:01:11:22:33',
          sourceIp: '192.168.1.10',
          sourcePort: 52140,
          destHost: 'WebServer',
          destMac: '00:AA:02:88:99:AA',
          destIp: '192.168.2.80',
          destPort: 80,
          protocol: 'HTTP',
          payload: 'GET /index.html HTTP/1.1\r\nHost: webserver.local'
        }
      });
      setTrace(res.data.data);
    } catch (err) {
      console.error("Failed to load OSI trace", err);
    } finally {
      setLoading(false);
    }
  };

  const layers = trace?.layers || [];
  const currentLayer = layers[activeStep];

  const handleNext = () => {
    if (activeStep < layers.length - 1) setActiveStep(activeStep + 1);
  };

  const handlePrev = () => {
    if (activeStep > 0) setActiveStep(activeStep - 1);
  };

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.65)', zIndex: 1050 }}>
      <div className="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div className="modal-content shadow-lg border-0">
          {/* Header */}
          <div className="modal-header bg-dark text-white py-2 px-3">
            <div className="d-flex align-items-center gap-2">
              <Layers size={20} className="text-info" />
              <h6 className="modal-title mb-0 fw-bold">7-Layer OSI & 4-Layer TCP/IP Protocol Visualizer</h6>
            </div>
            <button className="btn-close btn-close-white" onClick={onClose}></button>
          </div>

          <div className="modal-body p-4 bg-light">
            {loading && <div className="text-center p-5">Generating Layer Encapsulation Model...</div>}

            {!loading && trace && (
              <div className="row g-4">
                {/* Left Column: Vertical Layer Stack */}
                <div className="col-md-5">
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="small fw-bold text-muted text-uppercase">OSI 7-Layer Stack</span>
                    <span className="micro badge bg-info text-dark">Data Flow: Top &rarr; Down</span>
                  </div>

                  <div className="d-flex flex-column gap-2">
                    {layers.map((layer, idx) => {
                      const isCurrent = idx === activeStep;
                      const isProcessed = idx <= activeStep;
                      return (
                        <div 
                          key={layer.layerNumber}
                          onClick={() => setActiveStep(idx)}
                          className={`p-2 rounded border cursor-pointer d-flex justify-content-between align-items-center ${
                            isCurrent 
                              ? 'bg-primary text-white border-primary shadow' 
                              : isProcessed 
                              ? 'bg-white text-dark border-primary-subtle' 
                              : 'bg-white text-muted border-secondary-subtle opacity-75'
                          }`}
                          style={{ cursor: 'pointer', transition: 'all 0.15s' }}
                        >
                          <div className="d-flex align-items-center gap-2">
                            <span className={`badge rounded-pill ${isCurrent ? 'bg-white text-primary' : 'bg-secondary'}`} style={{ width: '24px' }}>
                              L{layer.layerNumber}
                            </span>
                            <div>
                              <div className="fw-bold small">{layer.layerName}</div>
                              <div className="micro" style={{ opacity: 0.85 }}>{layer.pduName}</div>
                            </div>
                          </div>

                          <div className="text-end">
                            {layer.headerAdded && (
                              <span className={`badge ${isCurrent ? 'bg-warning text-dark' : 'bg-light text-dark border'} micro`}>
                                + {layer.headerAdded}
                              </span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Step Navigation Controls */}
                  <div className="d-flex justify-content-between align-items-center mt-3 pt-2 border-top">
                    <button 
                      className="btn btn-sm btn-outline-secondary d-flex align-items-center gap-1"
                      onClick={handlePrev}
                      disabled={activeStep === 0}
                    >
                      <ArrowLeft size={14} /> Previous Layer
                    </button>
                    <span className="small text-muted fw-bold">Layer {activeStep + 1} of {layers.length}</span>
                    <button 
                      className="btn btn-sm btn-primary d-flex align-items-center gap-1"
                      onClick={handleNext}
                      disabled={activeStep === layers.length - 1}
                    >
                      Next Layer <ArrowRight size={14} />
                    </button>
                  </div>
                </div>

                {/* Right Column: Layer Inspection & Header Breakdown */}
                <div className="col-md-7">
                  {currentLayer && (
                    <div className="card h-100 border-0 shadow-sm bg-white">
                      <div className="card-header bg-white border-bottom py-2 d-flex justify-content-between align-items-center">
                        <div className="d-flex align-items-center gap-2">
                          <span className="badge bg-primary">Layer {currentLayer.layerNumber}</span>
                          <span className="fw-bold">{currentLayer.layerName} Layer</span>
                        </div>
                        <span className="badge bg-light text-dark border">PDU: {currentLayer.pduName}</span>
                      </div>

                      <div className="card-body">
                        {/* Header Banner */}
                        <div className="p-3 mb-3 rounded bg-light border border-info border-opacity-25">
                          <div className="micro text-uppercase text-muted fw-bold">Header Injected at this Layer:</div>
                          <div className="fw-bold text-primary font-monospace">{currentLayer.headerAdded || 'Payload (Raw Application Data)'}</div>
                        </div>

                        {/* Header Fields Table */}
                        <h6 className="small fw-bold text-muted mb-2 text-uppercase">Encapsulated Protocol Headers:</h6>
                        <div className="table-responsive mb-3 border rounded">
                          <table className="table table-sm table-striped mb-0 font-monospace" style={{ fontSize: '0.8rem' }}>
                            <thead className="table-light">
                              <tr>
                                <th>Field Name</th>
                                <th>Value</th>
                              </tr>
                            </thead>
                            <tbody>
                              {Object.entries(currentLayer.headerFields || {}).map(([key, val]) => (
                                <tr key={key}>
                                  <td className="text-muted">{key}</td>
                                  <td className="fw-bold text-dark">{String(val)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>

                        {/* Encapsulation PDU visualization */}
                        <h6 className="small fw-bold text-muted mb-1 text-uppercase">Frame Structure at L{currentLayer.layerNumber}:</h6>
                        <div className="p-2 bg-dark rounded text-light font-monospace d-flex flex-wrap gap-1 align-items-center" style={{ fontSize: '0.75rem' }}>
                          {currentLayer.layerNumber <= 2 && <span className="badge bg-secondary p-2">ETH PREAMBLE [7B]</span>}
                          {currentLayer.layerNumber <= 2 && <span className="badge bg-danger p-2">ETH HEADER [14B]</span>}
                          {currentLayer.layerNumber <= 3 && <span className="badge bg-warning text-dark p-2">IP HEADER [20B]</span>}
                          {currentLayer.layerNumber <= 4 && <span className="badge bg-success p-2">TCP HEADER [20B]</span>}
                          <span className="badge bg-primary p-2 flex-grow-1 text-truncate">PAYLOAD DATA [{currentLayer.dataField?.length || 48}B]</span>
                          {currentLayer.layerNumber <= 2 && <span className="badge bg-danger p-2">FCS CRC-32 [4B]</span>}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

