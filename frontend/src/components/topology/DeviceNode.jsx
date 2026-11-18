import { Handle, Position } from 'reactflow';
import { Monitor, Server, Router as RouterIcon, Share2, Radio, Zap, Layers, Wifi, Cpu, Globe } from 'lucide-react';

const icons = {
  PC: <Monitor size={22} className="text-primary" />,
  SERVER: <Server size={22} className="text-info" />,
  ROUTER: <RouterIcon size={22} className="text-warning" />,
  SWITCH: <Share2 size={22} className="text-success" />,
  HUB: <Radio size={22} className="text-danger" />,
  REPEATER: <Zap size={22} className="text-warning" />,
  BRIDGE: <Layers size={22} className="text-secondary" />,
  ACCESS_POINT: <Wifi size={22} className="text-primary" />,
  MODEM: <Cpu size={22} className="text-dark" />,
  GATEWAY: <Globe size={22} className="text-info" />,
};

const badgeColors = {
  PC: 'bg-primary',
  SERVER: 'bg-info text-dark',
  ROUTER: 'bg-warning text-dark',
  SWITCH: 'bg-success',
  HUB: 'bg-danger',
  REPEATER: 'bg-secondary',
  BRIDGE: 'bg-dark',
  ACCESS_POINT: 'bg-primary',
  MODEM: 'bg-secondary',
  GATEWAY: 'bg-dark',
};

export default function DeviceNode({ data, selected }) {
  return (
    <div className={`device-node p-2 rounded shadow-sm text-center bg-white border ${selected ? 'border-primary border-2 shadow' : 'border-secondary-subtle'}`} style={{ minWidth: '95px' }}>
      <Handle type="target" position={Position.Top} style={{ background: '#0d6efd', width: 8, height: 8 }} />
      <div className="device-icon mb-1">
        {icons[data.type] || <Monitor size={22} />}
      </div>
      <div className="fw-bold small text-truncate" style={{ maxWidth: '90px' }} title={data.name}>{data.name}</div>
      <span className={`badge ${badgeColors[data.type] || 'bg-secondary'} micro mt-1`}>
        {data.type}
      </span>
      <Handle type="source" position={Position.Bottom} style={{ background: '#0d6efd', width: 8, height: 8 }} />
      <Handle type="source" position={Position.Left} id="left" style={{ background: '#0d6efd', width: 8, height: 8, top: '50%' }} />
      <Handle type="source" position={Position.Right} id="right" style={{ background: '#0d6efd', width: 8, height: 8, top: '50%' }} />
    </div>
  );
}
