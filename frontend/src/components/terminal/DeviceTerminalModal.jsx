import { useState, useEffect, useRef } from 'react';
import { Terminal as TerminalIcon, X, Send, CornerDownLeft, Trash2 } from 'lucide-react';
import api from '../../services/api';

export default function DeviceTerminalModal({ device, onClose }) {
  const [inputCmd, setInputCmd] = useState('');
  const [history, setHistory] = useState([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  const [cmdList, setCmdList] = useState([]);
  const [loading, setLoading] = useState(false);
  const terminalEndRef = useRef(null);

  const isCiscoStyle = device?.type === 'ROUTER' || device?.type === 'SWITCH';
  const promptSymbol = isCiscoStyle ? '#' : '>';

  useEffect(() => {
    // Initial welcome banner
    const banner = [
      `NetHub Device Terminal [Version 2.4.0]`,
      `Connected to ${device?.name} (${device?.type})`,
      `Type 'help' or '?' for a list of supported networking commands.\n`
    ].join('\n');
    setHistory([{ type: 'output', text: banner }]);
  }, [device]);

  useEffect(() => {
    terminalEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [history]);

  const handleSendCommand = async (cmdToSend) => {
    const cmd = (cmdToSend !== undefined ? cmdToSend : inputCmd).trim();
    if (!cmd) return;

    // Echo command
    setHistory(prev => [...prev, { type: 'command', text: `${device.name}${promptSymbol} ${cmd}` }]);
    setCmdList(prev => [...prev, cmd]);
    setHistoryIndex(-1);
    setInputCmd('');
    setLoading(true);

    try {
      const res = await api.post(`/devices/${device.id}/command`, { command: cmd });
      const output = res.data.data.output;
      setHistory(prev => [...prev, { type: 'output', text: output }]);
    } catch (err) {
      setHistory(prev => [...prev, { type: 'error', text: `Command execution error: ${err.response?.data?.message || err.message}` }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSendCommand();
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (cmdList.length > 0) {
        const nextIdx = historyIndex === -1 ? cmdList.length - 1 : Math.max(0, historyIndex - 1);
        setHistoryIndex(nextIdx);
        setInputCmd(cmdList[nextIdx]);
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (historyIndex !== -1) {
        const nextIdx = historyIndex + 1;
        if (nextIdx < cmdList.length) {
          setHistoryIndex(nextIdx);
          setInputCmd(cmdList[nextIdx]);
        } else {
          setHistoryIndex(-1);
          setInputCmd('');
        }
      }
    }
  };

  const quickCommands = device?.type === 'ROUTER' 
    ? ['show ip route', 'show ip interface brief', 'show arp', 'show ip ospf neighbor', 'show ip rip', 'show ip bgp', 'show ip nat translations', 'show access-lists']
    : device?.type === 'SWITCH'
    ? ['show mac address-table', 'show interfaces status', 'show vlan', 'show spanning-tree']
    : device?.type === 'ACCESS_POINT'
    ? ['show wireless', 'show clients', 'show ssid']
    : ['ipconfig /all', 'route print', 'arp -a', 'netstat', 'nbtstat', 'nslookup web.nethub.local', 'nmap 192.168.1.1'];

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.65)', zIndex: 1050 }}>
      <div className="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div className="modal-content bg-dark text-light border-secondary shadow-lg">
          {/* Header */}
          <div className="modal-header border-secondary py-2 px-3 d-flex justify-content-between align-items-center">
            <div className="d-flex align-items-center gap-2">
              <TerminalIcon size={18} className="text-info" />
              <span className="fw-bold text-white small">Terminal — {device?.name}</span>
              <span className="badge bg-secondary micro">{device?.type}</span>
            </div>
            <div className="d-flex align-items-center gap-2">
              <button 
                className="btn btn-sm btn-outline-secondary py-0 px-2 micro text-light" 
                onClick={() => setHistory([])}
                title="Clear screen"
              >
                <Trash2 size={12} className="me-1" /> Clear
              </button>
              <button className="btn-close btn-close-white" onClick={onClose}></button>
            </div>
          </div>

          {/* Quick Command Bar */}
          <div className="bg-black bg-opacity-50 px-3 py-1 border-bottom border-secondary d-flex flex-wrap gap-1 align-items-center">
            <span className="micro text-muted me-1">Quick:</span>
            {quickCommands.map((q, idx) => (
              <button 
                key={idx} 
                className="btn btn-xs btn-outline-info py-0 px-1 micro text-nowrap"
                style={{ fontSize: '0.72rem' }}
                onClick={() => handleSendCommand(q)}
                disabled={loading}
              >
                {q}
              </button>
            ))}
          </div>

          {/* Terminal Body */}
          <div 
            className="modal-body p-3 font-monospace bg-black" 
            style={{ minHeight: '340px', maxHeight: '450px', overflowY: 'auto', fontSize: '0.82rem' }}
          >
            {history.map((entry, idx) => (
              <div key={idx} className="mb-1" style={{ whiteSpace: 'pre-wrap', lineHeight: '1.4' }}>
                {entry.type === 'command' && (
                  <span className="text-info fw-bold">{entry.text}</span>
                )}
                {entry.type === 'output' && (
                  <span className="text-light">{entry.text}</span>
                )}
                {entry.type === 'error' && (
                  <span className="text-danger fw-bold">{entry.text}</span>
                )}
              </div>
            ))}
            {loading && <div className="text-warning micro">Executing command...</div>}
            <div ref={terminalEndRef} />
          </div>

          {/* Command Input Prompt */}
          <div className="modal-footer border-secondary p-2 bg-dark">
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-black text-info border-secondary font-monospace fw-bold">
                {device?.name}{promptSymbol}
              </span>
              <input 
                type="text" 
                className="form-control bg-black text-light border-secondary font-monospace"
                placeholder="Type command here (e.g. ping 192.168.1.1, help)..."
                value={inputCmd}
                onChange={e => setInputCmd(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={loading}
                autoFocus
              />
              <button 
                className="btn btn-primary d-flex align-items-center gap-1"
                onClick={() => handleSendCommand()}
                disabled={loading || !inputCmd.trim()}
              >
                <Send size={14} /> Send
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

