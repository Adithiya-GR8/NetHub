import { Monitor, Server, Router as RouterIcon, Share2, Radio, Zap, Layers, Wifi, Cpu, Globe } from 'lucide-react';

export default function DevicePalette({ onAddDevice }) {
  const devices = [
    { type: 'PC', label: 'PC', icon: <Monitor size={18} /> },
    { type: 'SERVER', label: 'Server', icon: <Server size={18} /> },
    { type: 'SWITCH', label: 'Switch (L2)', icon: <Share2 size={18} /> },
    { type: 'ROUTER', label: 'Router (L3)', icon: <RouterIcon size={18} /> },
    { type: 'HUB', label: 'Hub (L1)', icon: <Radio size={18} /> },
    { type: 'REPEATER', label: 'Repeater (L1)', icon: <Zap size={18} /> },
    { type: 'BRIDGE', label: 'Bridge (L2)', icon: <Layers size={18} /> },
    { type: 'ACCESS_POINT', label: 'Wireless AP', icon: <Wifi size={18} /> },
    { type: 'MODEM', label: 'Modem', icon: <Cpu size={18} /> },
    { type: 'GATEWAY', label: 'Gateway', icon: <Globe size={18} /> },
  ];

  const onDragStart = (event, nodeType) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div className="sidebar" style={{ width: '220px', minWidth: '220px', overflowY: 'auto' }}>
      <h6 className="text-uppercase text-muted fw-bold mb-3 small">Device Palette</h6>
      <div className="d-flex flex-column gap-2">
        {devices.map((device) => (
          <div 
            key={device.type}
            className="palette-item d-flex align-items-center gap-2 p-2 rounded border bg-light cursor-pointer shadow-sm"
            style={{ cursor: 'grab', transition: 'all 0.15s ease' }}
            onDragStart={(event) => onDragStart(event, device.type)}
            onClick={() => onAddDevice(device.type)}
            draggable
          >
            <div className="text-primary">{device.icon}</div>
            <span className="small fw-semibold">{device.label}</span>
          </div>
        ))}
      </div>
      <hr />
      <div className="micro text-muted">
        Drag and drop devices onto the canvas or click to add them.
      </div>
    </div>
  );
}
