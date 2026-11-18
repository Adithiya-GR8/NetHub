import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactFlow, { 
  MiniMap, 
  Controls, 
  Background, 
  useNodesState, 
  useEdgesState, 
  addEdge,
  ReactFlowProvider
} from 'reactflow';
import { Save, CheckCircle, AlertTriangle, Activity, Layers, Terminal as TerminalIcon, Eye, BookOpen } from 'lucide-react';
import api from '../services/api';
import DevicePalette from '../components/topology/DevicePalette';
import DeviceNode from '../components/topology/DeviceNode';
import ConfigurationPanel from '../components/devices/ConfigurationPanel';
import NetworkSimulationPanel from '../components/simulation/NetworkSimulationPanel';
import SwitchingComparisonModal from '../components/simulation/SwitchingComparisonModal';
import OsiVisualizerModal from '../components/simulation/OsiVisualizerModal';
import DeviceTerminalModal from '../components/terminal/DeviceTerminalModal';
import DemonstrationScenariosModal from '../components/simulation/DemonstrationScenariosModal';

const nodeTypes = {
  customDevice: DeviceNode,
};

function NetworkWorkspaceInternal() {
  const { id } = useParams();
  const navigate = useNavigate();
  const reactFlowWrapper = useRef(null);
  
  const [project, setProject] = useState(null);
  const [rawDevices, setRawDevices] = useState([]);
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [reactFlowInstance, setReactFlowInstance] = useState(null);
  
  const [selectedDeviceId, setSelectedDeviceId] = useState(null);
  const [validationResult, setValidationResult] = useState(null);
  const [saving, setSaving] = useState(false);
  const [showSimulation, setShowSimulation] = useState(false);
  const [showSwitchingComparison, setShowSwitchingComparison] = useState(false);
  const [showOsiVisualizer, setShowOsiVisualizer] = useState(false);
  const [terminalDevice, setTerminalDevice] = useState(null);
  const [showScenariosModal, setShowScenariosModal] = useState(false);

  const handleScenarioLoaded = async (loadedProject, scenario) => {
    await fetchTopology();
    if (scenario && scenario.category === 'INVALID') {
      try {
        const valRes = await api.get(`/projects/${id}/validate`);
        setValidationResult(valRes.data.data);
      } catch (err) {
        console.error("Validation error after scenario load", err);
      }
    } else {
      setValidationResult(null);
    }
  };

  const handleOpenTerminal = () => {
    if (selectedDeviceId) {
      const dev = rawDevices.find(d => d.id === selectedDeviceId);
      if (dev) {
        setTerminalDevice(dev);
        return;
      }
    }
    if (rawDevices.length > 0) {
      setTerminalDevice(rawDevices[0]);
    }
  };

  useEffect(() => {
    fetchTopology();
  }, [id]);

  const fetchTopology = async () => {
    try {
      const projRes = await api.get(`/projects/${id}`);
      setProject(projRes.data.data);

      const [devRes, connRes] = await Promise.all([
        api.get(`/projects/${id}/devices`),
        api.get(`/projects/${id}/connections`)
      ]);

      const devices = devRes.data.data;
      setRawDevices(devices);
      const connections = connRes.data.data;

      const newNodes = devices.map(d => ({
        id: d.id.toString(),
        type: 'customDevice',
        position: { x: d.positionX, y: d.positionY },
        data: { name: d.name, type: d.type, status: d.status }
      }));
      
      const edgeList = [];
      if (connections.length > 0) {
          const allIntfPromises = devices.map(d => api.get(`/devices/${d.id}/interfaces`));
          const allIntfResponses = await Promise.all(allIntfPromises);
          const allInterfaces = allIntfResponses.flatMap(r => r.data.data);
          
          connections.forEach(c => {
             const srcIntf = allInterfaces.find(i => i.id === c.sourceInterfaceId);
             const dstIntf = allInterfaces.find(i => i.id === c.destinationInterfaceId);
             if (srcIntf && dstIntf) {
                 edgeList.push({
                     id: `e-${c.id}`,
                     source: srcIntf.deviceId.toString(),
                     target: dstIntf.deviceId.toString(),
                     type: 'smoothstep'
                 });
             }
          });
      }

      setNodes(newNodes);
      setEdges(edgeList);
    } catch (error) {
      console.error("Failed to fetch topology", error);
    }
  };

  const onConnect = useCallback(async (params) => {
    try {
        const srcId = parseInt(params.source);
        const tgtId = parseInt(params.target);
        
        let [srcIntfRes, tgtIntfRes] = await Promise.all([
            api.get(`/devices/${srcId}/interfaces`),
            api.get(`/devices/${tgtId}/interfaces`)
        ]);
        
        let srcIntf = srcIntfRes.data.data[0];
        let tgtIntf = tgtIntfRes.data.data[0];
        
        if (!srcIntf) {
            const res = await api.post(`/devices/${srcId}/interfaces`, { name: 'eth0', status: 'UP' });
            srcIntf = res.data.data;
        }
        if (!tgtIntf) {
            const res = await api.post(`/devices/${tgtId}/interfaces`, { name: 'eth0', status: 'UP' });
            tgtIntf = res.data.data;
        }
        
        const connRes = await api.post(`/projects/${id}/connections`, {
            sourceInterfaceId: srcIntf.id,
            destinationInterfaceId: tgtIntf.id
        });
        
        const newEdge = {
            ...params,
            id: `e-${connRes.data.data.id}`,
            type: 'smoothstep'
        };
        
        setEdges((eds) => addEdge(newEdge, eds));
    } catch (err) {
        alert(err.response?.data?.message || "Failed to create connection");
    }
  }, [setEdges, id]);

  const onDragOver = useCallback((event) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const createDevice = async (type, position) => {
      try {
          const deviceCount = nodes.filter(n => n.data.type === type).length + 1;
          const newDevice = {
              name: `${type}${deviceCount}`,
              hostname: `${type.toLowerCase()}${deviceCount}.local`,
              type: type,
              status: 'ONLINE',
              positionX: position.x,
              positionY: position.y
          };
          
          if (type === 'ACCESS_POINT') {
              newDevice.ssid = 'NetHub-WiFi';
              newDevice.wirelessChannel = 6;
              newDevice.wirelessSecurity = 'WPA2-PSK (AES)';
              newDevice.wirelessKey = 'nethub123';
          } else if (type === 'ROUTER' || type === 'GATEWAY') {
              newDevice.routingProtocol = 'OSPF';
              newDevice.asNumber = 65001;
          }

          const res = await api.post(`/projects/${id}/devices`, newDevice);
          const createdDevice = res.data.data;
          
          // Context-aware interface creation with default non-IP properties (IP left blank)
          if (type === 'SWITCH' || type === 'HUB') {
              await Promise.all([
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'port1', speed: '1Gbps', duplex: 'FULL', mtu: 1500, vlanId: 1, portMode: 'ACCESS', status: 'UP' }),
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'port2', speed: '1Gbps', duplex: 'FULL', mtu: 1500, vlanId: 1, portMode: 'ACCESS', status: 'UP' }),
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'port3', speed: '1Gbps', duplex: 'FULL', mtu: 1500, vlanId: 1, portMode: 'ACCESS', status: 'UP' }),
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'port4', speed: '1Gbps', duplex: 'FULL', mtu: 1500, vlanId: 1, portMode: 'ACCESS', status: 'UP' })
              ]);
          } else if (type === 'ROUTER') {
              await Promise.all([
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'eth0', speed: '1Gbps', duplex: 'FULL', mtu: 1500, subnetMask: '255.255.255.0', status: 'UP' }),
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'eth1', speed: '1Gbps', duplex: 'FULL', mtu: 1500, subnetMask: '255.255.255.0', status: 'UP' })
              ]);
          } else if (type === 'ACCESS_POINT') {
              await Promise.all([
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'wlan0', speed: '300Mbps', duplex: 'HALF', mtu: 1500, status: 'UP' }),
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'eth0', speed: '1Gbps', duplex: 'FULL', mtu: 1500, status: 'UP' })
              ]);
          } else if (type === 'BRIDGE' || type === 'REPEATER') {
              await Promise.all([
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'port1', speed: '100Mbps', duplex: 'FULL', mtu: 1500, status: 'UP' }),
                  api.post(`/devices/${createdDevice.id}/interfaces`, { name: 'port2', speed: '100Mbps', duplex: 'FULL', mtu: 1500, status: 'UP' })
              ]);
          } else {
              // PC, SERVER, MODEM, GATEWAY
              await api.post(`/devices/${createdDevice.id}/interfaces`, { 
                  name: 'eth0', 
                  speed: type === 'SERVER' ? '10Gbps' : '1Gbps',
                  duplex: 'FULL',
                  mtu: 1500,
                  subnetMask: '255.255.255.0',
                  dnsServer: '8.8.8.8',
                  status: 'UP' 
              });
          }
          
          const newNode = {
            id: createdDevice.id.toString(),
            type: 'customDevice',
            position,
            data: { name: createdDevice.name, type: createdDevice.type, status: createdDevice.status },
          };

          setNodes((nds) => nds.concat(newNode));
          setRawDevices(prev => [...prev, createdDevice]);
      } catch(err) {
          console.error("Error creating device", err);
      }
  };

  const onDrop = useCallback(
    (event) => {
      event.preventDefault();

      const type = event.dataTransfer.getData('application/reactflow');
      if (typeof type === 'undefined' || !type) return;

      const position = reactFlowInstance.screenToFlowPosition({
        x: event.clientX,
        y: event.clientY,
      });

      createDevice(type, position);
    },
    [reactFlowInstance, nodes, id, setNodes]
  );

  const handleAddDeviceClick = (type) => {
      const position = reactFlowInstance.screenToFlowPosition({
          x: window.innerWidth / 2,
          y: window.innerHeight / 2,
      });
      createDevice(type, position);
  };

  const onNodeClick = (event, node) => {
    setSelectedDeviceId(parseInt(node.id));
  };
  
  const onPaneClick = () => {
    setSelectedDeviceId(null);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const updatePromises = nodes.map(node => {
          return api.put(`/devices/${node.id}`, {
              name: node.data.name,
              type: node.data.type,
              status: node.data.status,
              positionX: node.position.x,
              positionY: node.position.y
          });
      });
      await Promise.all(updatePromises);
      alert("Topology saved successfully!");
    } catch (err) {
      alert("Error saving topology");
    } finally {
      setSaving(false);
    }
  };

  const handleValidate = async () => {
    try {
      const res = await api.post(`/projects/${id}/validate`);
      setValidationResult(res.data.data);
    } catch (err) {
      alert("Validation failed to execute");
    }
  };

  const onDeviceUpdated = (updatedDevice) => {
      setNodes(nds => nds.map(node => {
          if (node.id === updatedDevice.id.toString()) {
              return {
                  ...node,
                  data: {
                      ...node.data,
                      name: updatedDevice.name,
                      status: updatedDevice.status
                  }
              };
          }
          return node;
      }));
      setRawDevices(prev => prev.map(d => d.id === updatedDevice.id ? updatedDevice : d));
  };

  const onEdgesDelete = async (edgesToDelete) => {
      for (const edge of edgesToDelete) {
          if (edge.id.startsWith('e-')) {
              const connId = edge.id.substring(2);
              try {
                  await api.delete(`/connections/${connId}`);
              } catch(err) {
                  console.error("Failed to delete connection in backend", err);
              }
          }
      }
  };

  const onNodesDelete = async (nodesToDelete) => {
      for (const node of nodesToDelete) {
          try {
              await api.delete(`/devices/${node.id}`);
          } catch(err) {
              console.error("Failed to delete device in backend", err);
          }
      }
      setSelectedDeviceId(null);
  };

  if (!project) return <div className="p-5 text-center">Loading Workspace...</div>;

  return (
    <div className="workspace-container">
      <DevicePalette onAddDevice={handleAddDeviceClick} />
      
      <div className="canvas-area d-flex flex-column">
        <div className="bg-white border-bottom p-2 d-flex justify-content-between align-items-center shadow-sm">
          <div className="fw-bold px-2">{project.name} Workspace</div>
          <div className="d-flex gap-2 align-items-center">
            <button 
              className="btn btn-sm btn-warning d-flex align-items-center gap-1 fw-bold text-dark shadow-sm"
              onClick={() => setShowScenariosModal(true)}
              title="Browse and load preconfigured demonstration topologies"
            >
              <BookOpen size={16} /> Demo Scenarios
            </button>
            <button 
              className="btn btn-sm btn-outline-dark d-flex align-items-center gap-1"
              onClick={handleOpenTerminal}
              title="Open Device CLI Terminal"
              disabled={rawDevices.length === 0}
            >
              <TerminalIcon size={16} /> Terminal
            </button>
            <button 
              className="btn btn-sm btn-outline-info d-flex align-items-center gap-1"
              onClick={() => setShowOsiVisualizer(true)}
              title="7-Layer OSI & 4-Layer TCP/IP Encapsulation Visualizer"
            >
              <Eye size={16} /> OSI / TCP-IP
            </button>
            <button 
              className="btn btn-sm btn-outline-secondary d-flex align-items-center gap-1"
              onClick={() => setShowSwitchingComparison(true)}
              title="Circuit vs Message vs Packet Switching Comparison"
            >
              <Layers size={16} /> Switching Tech
            </button>
            <button 
              className={`btn btn-sm ${showSimulation ? 'btn-primary' : 'btn-outline-primary'} d-flex align-items-center gap-1`}
              onClick={() => setShowSimulation(!showSimulation)}
            >
              <Activity size={16} /> Simulate Network
            </button>
            <button className="btn btn-sm btn-outline-success d-flex align-items-center gap-1" onClick={handleValidate}>
              <CheckCircle size={16} /> Validate
            </button>
            <button className="btn btn-sm btn-primary d-flex align-items-center gap-1" onClick={handleSave} disabled={saving}>
              <Save size={16} /> {saving ? 'Saving...' : 'Save'}
            </button>
          </div>
        </div>
        
        {validationResult && (
          <div className={`alert rounded-0 mb-0 px-3 py-2 border-0 border-bottom alert-${validationResult.isValid ? 'success' : 'warning'} d-flex justify-content-between align-items-center`}>
            <div>
              {validationResult.isValid ? (
                <><CheckCircle size={16} className="me-2"/> Network configuration is valid.</>
              ) : (
                <><AlertTriangle size={16} className="me-2"/> Validation Errors Found!</>
              )}
            </div>
            <button className="btn-close btn-sm" onClick={() => setValidationResult(null)}></button>
          </div>
        )}
        
        {validationResult && !validationResult.isValid && validationResult.errors.length > 0 && (
          <div className="bg-light p-3 border-bottom overflow-auto" style={{maxHeight: '150px'}}>
             <ul className="mb-0 text-danger small">
               {validationResult.errors.map((err, i) => <li key={i}>{err}</li>)}
             </ul>
          </div>
        )}

        {showSimulation && (
          <div className="p-3 bg-light border-bottom" style={{ maxHeight: '380px', overflowY: 'auto' }}>
            <NetworkSimulationPanel 
              projectId={id} 
              devices={rawDevices} 
              onClose={() => setShowSimulation(false)} 
            />
          </div>
        )}

        <div className="flex-grow-1" ref={reactFlowWrapper}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onInit={setReactFlowInstance}
            onDrop={onDrop}
            onDragOver={onDragOver}
            nodeTypes={nodeTypes}
            onNodeClick={onNodeClick}
            onPaneClick={onPaneClick}
            onEdgesDelete={onEdgesDelete}
            onNodesDelete={onNodesDelete}
            fitView
          >
            <Controls />
            <MiniMap />
            <Background variant="dots" gap={12} size={1} />
          </ReactFlow>
        </div>
      </div>

      <ConfigurationPanel 
        deviceId={selectedDeviceId} 
        onClose={() => setSelectedDeviceId(null)} 
        onDeviceUpdated={onDeviceUpdated}
        onOpenTerminal={(dev) => setTerminalDevice(dev)}
      />

      {/* Switching Comparison Modal */}
      {showSwitchingComparison && (
        <SwitchingComparisonModal onClose={() => setShowSwitchingComparison(false)} />
      )}

      {/* 7-Layer OSI / 4-Layer TCP-IP Visualizer */}
      {showOsiVisualizer && (
        <OsiVisualizerModal onClose={() => setShowOsiVisualizer(false)} />
      )}

      {/* Interactive Device CLI Terminal */}
      {terminalDevice && (
        <DeviceTerminalModal device={terminalDevice} onClose={() => setTerminalDevice(null)} />
      )}

      {/* Preloaded Demonstration Scenarios Modal */}
      {showScenariosModal && (
        <DemonstrationScenariosModal 
          currentProjectId={id} 
          onScenarioLoaded={handleScenarioLoaded} 
          onClose={() => setShowScenariosModal(false)} 
        />
      )}
    </div>
  );
}

export default function NetworkWorkspace() {
  return (
    <ReactFlowProvider>
      <NetworkWorkspaceInternal />
    </ReactFlowProvider>
  );
}
