import { Monitor, Server, Router as RouterIcon, Share2 } from 'lucide-react';

export default function DevicePalette({ onAddDevice }) {
  const devices = [
    { type: 'PC', label: 'PC', icon: <Monitor size={20} /> },
    { type: 'SERVER', label: 'Server', icon: <Server size={20} /> },
    { type: 'SWITCH', label: 'Switch', icon: <Share2 size={20} /> },
    { type: 'ROUTER', label: 'Router', icon: <RouterIcon size={20} /> },
  ];

  const onDragStart = (event, nodeType) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div className="sidebar">
      <h6 className="text-uppercase text-muted fw-bold mb-3">Devices</h6>
      <div className="d-flex flex-column gap-2">
        {devices.map((device) => (
          <div 
            key={device.type}
            className="palette-item d-flex align-items-center gap-2"
            onDragStart={(event) => onDragStart(event, device.type)}
            onClick={() => onAddDevice(device.type)}
            draggable
          >
            {device.icon}
            <span>{device.label}</span>
          </div>
        ))}
      </div>
      <hr />
      <div className="small text-muted">
        Drag and drop devices onto the canvas or click to add them.
      </div>
    </div>
  );
}
