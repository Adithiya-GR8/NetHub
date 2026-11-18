import { Handle, Position } from 'reactflow';
import { Monitor, Server, Router as RouterIcon, Share2 } from 'lucide-react';

const icons = {
  PC: <Monitor size={24} />,
  SERVER: <Server size={24} />,
  ROUTER: <RouterIcon size={24} />,
  SWITCH: <Share2 size={24} />
};

export default function DeviceNode({ data, selected }) {
  return (
    <div className={`device-node ${selected ? 'selected' : ''}`}>
      <Handle type="target" position={Position.Top} style={{ background: '#555' }} />
      <div className="device-icon">
        {icons[data.type] || <Monitor size={24} />}
      </div>
      <div className="fw-bold">{data.name}</div>
      <div className="text-muted small">{data.type}</div>
      <Handle type="source" position={Position.Bottom} style={{ background: '#555' }} />
      <Handle type="source" position={Position.Left} style={{ background: '#555', top: '50%' }} />
      <Handle type="source" position={Position.Right} style={{ background: '#555', top: '50%' }} />
    </div>
  );
}
