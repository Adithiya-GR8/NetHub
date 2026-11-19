export const PRELOADED_SCENARIOS = [
  // VALID SCENARIOS (14)
  {
    code: 'V1',
    name: 'Basic Hub Network',
    category: 'VALID',
    difficulty: 'BEGINNER',
    deviceCount: 4,
    description: 'Demonstrates a Layer 1 multiport repeater (Hub) connecting 3 PCs on a shared medium. Hub floods bits unconditionally out all active ports.',
    concepts: 'Layer 1 repeating, Hub broadcast flooding, shared collision domain, half-duplex medium',
    expectedResult: 'Frames transmitted by PC1 are repeated to both PC2 and PC3, with PC2 accepting the unicast frame matching its MAC.',
    expectedErrors: null
  },
  {
    code: 'V2',
    name: 'Repeater Network',
    category: 'VALID',
    difficulty: 'BEGINNER',
    deviceCount: 5,
    description: 'Layer 1 signal regenerator extending transmission distance between two Ethernet bus segments.',
    concepts: 'Signal regeneration, attenuation mitigation, extended collision domain, Layer 1 transparent forwarding',
    expectedResult: 'Signal from Segment A is cleaned and amplified onto Segment B, enabling transparent PC1-to-PC3 frame delivery.',
    expectedErrors: null
  },
  {
    code: 'V3',
    name: 'Bridge Network',
    category: 'VALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 5,
    description: 'Two-port Layer 2 bridge partitioning collision domains while maintaining a unified broadcast domain.',
    concepts: 'MAC address learning, collision domain segmentation, same-segment frame filtering, unicast forwarding',
    expectedResult: 'Intra-segment traffic (PC1->PC2) is filtered at Bridge port 1. Inter-segment traffic (PC1->PC3) is forwarded across port 2.',
    expectedErrors: null
  },
  {
    code: 'V4',
    name: 'Switched LAN',
    category: 'VALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 7,
    description: 'Dual-switch enterprise LAN with 4 client PCs and 1 server demonstrating MAC learning, ARP resolution, and unicast forwarding.',
    concepts: 'Dynamic MAC address table learning, ARP cache resolution, full-duplex microsegmentation, inter-switch trunk forwarding',
    expectedResult: 'Initial ARP broadcast flood followed by direct unicast frame delivery with zero unnecessary flooding.',
    expectedErrors: null
  },
  {
    code: 'V5',
    name: 'Basic Router Network',
    category: 'VALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 7,
    description: 'Dual-subnet network connected via a multi-interface Layer 3 router performing inter-subnet packet forwarding.',
    concepts: 'Default gateway resolution, Layer 2 frame rewrite (MAC substitution), TTL decrement, routing table lookup, subnet isolation',
    expectedResult: 'PC1 sends packet to Router default gateway, router rewrites L2 Ethernet header, and forwards packet to PC3 on Subnet B.',
    expectedErrors: null
  },
  {
    code: 'V6',
    name: 'Multi-Router Network',
    category: 'VALID',
    difficulty: 'ADVANCED',
    deviceCount: 6,
    description: '3-router transit topology demonstrating multi-hop static route lookup, longest-prefix matching, and hop-by-hop forwarding.',
    concepts: 'Multi-hop static routing, longest prefix matching, next-hop resolution, hop-by-hop TTL decrement, point-to-point /30 transit subnets',
    expectedResult: 'Packets from PC1 traverse R1 -> R2 -> R3 -> Server with TTL decremented at each router hop and successful response.',
    expectedErrors: null
  },
  {
    code: 'V7',
    name: 'VLAN Network',
    category: 'VALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 6,
    description: 'Campus network partitioned into VLAN 10 (Sales) and VLAN 20 (Engineering) with 802.1Q tagged inter-switch trunk link.',
    concepts: '802.1Q VLAN tagging, access port untagging, trunk encapsulation, broadcast domain isolation without physical rewiring',
    expectedResult: 'PC1 communicates with PC3 on VLAN 10 across trunk link. Broadcast frames on VLAN 10 never leak to PC2 on VLAN 20.',
    expectedErrors: null
  },
  {
    code: 'V8',
    name: 'Wireless Infrastructure Network',
    category: 'VALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 5,
    description: 'Wi-Fi access point bridging wireless laptops to a wired switch and server infrastructure.',
    concepts: '802.11 wireless frame conversion, CSMA/CA channel access, BSSID association, bridge conversion to 802.3 Ethernet',
    expectedResult: 'Wireless laptop associates with AP, receives frame bridging onto Ethernet, and ping to wired server succeeds.',
    expectedErrors: null
  },
  {
    code: 'V9',
    name: 'Wireless Ad-hoc Network',
    category: 'VALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 3,
    description: 'Peer-to-peer independent basic service set (IBSS) with direct station-to-station communication without an AP.',
    concepts: '802.11 IBSS mode, peer-to-peer direct transmission, distributed coordination function (DCF), carrier sensing',
    expectedResult: 'Client1 communicates directly with Client2 on same wireless channel without requiring an intermediate access point.',
    expectedErrors: null
  },
  {
    code: 'V10',
    name: 'DHCP and DNS Network',
    category: 'VALID',
    difficulty: 'ADVANCED',
    deviceCount: 5,
    description: 'Comprehensive network with DHCP server, DNS server, Web server, and dynamic clients.',
    concepts: 'DHCP DORA sequence (Discover, Offer, Request, Acknowledge), dynamic IP lease, DNS A-record query and resolution, HTTP request',
    expectedResult: 'ClientPC acquires IP via DHCP, issues DNS query for web.corp.local, receives IP, and successfully completes HTTP GET.',
    expectedErrors: null
  },
  {
    code: 'V11',
    name: 'TCP and UDP Client-Server Network',
    category: 'VALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 4,
    description: 'Dual-protocol demonstration comparing connection-oriented TCP handshake against connectionless UDP datagram transmission.',
    concepts: 'TCP 3-way handshake (SYN, SYN-ACK, ACK), sequence/acknowledgment numbers, UDP datagram streaming, transport port multiplexing',
    expectedResult: 'TCP client completes 3-way handshake before data transfer. UDP client transmits datagrams directly without connection overhead.',
    expectedErrors: null
  },
  {
    code: 'V12',
    name: 'HTTP and HTTPS Network',
    category: 'VALID',
    difficulty: 'ADVANCED',
    deviceCount: 4,
    description: 'Secure application layer communications with cleartext HTTP port 80 vs TLS 1.3 encrypted HTTPS port 443.',
    concepts: 'HTTP GET/200 OK, TLS 1.3 cryptographic handshake, cipher negotiation, digital certificate verification, payload encryption',
    expectedResult: 'HTTP session exhibits plaintext headers in packet inspector. HTTPS session negotiates TLS parameters and protects payload.',
    expectedErrors: null
  },
  {
    code: 'V13',
    name: 'Routing Protocol Network',
    category: 'VALID',
    difficulty: 'ADVANCED',
    deviceCount: 5,
    description: 'Mesh network with 3 interconnected routers demonstrating link-state advertisements and shortest-path calculation.',
    concepts: 'OSPF Link-State Database (LSDB), Dijkstra Shortest Path First (SPF), link metric cost accumulation, dynamic convergence',
    expectedResult: 'Routers calculate optimal lowest-cost path. Traffic routes across lowest metric link rather than highest hop count.',
    expectedErrors: null
  },
  {
    code: 'V14',
    name: 'Comparative Topology: Hub vs Switch vs Router',
    category: 'VALID',
    difficulty: 'ADVANCED',
    deviceCount: 9,
    description: 'Side-by-side comparative architecture demonstrating collision domains, broadcast domains, and forwarding logic across Hub, Switch, Bridge, and Router.',
    concepts: 'Collision domain isolation, broadcast domain boundaries, Layer 1 bit flooding vs Layer 2 switching vs Layer 3 packet routing',
    expectedResult: 'Interactive inspection shows: Hub creates 1 collision domain; Switch creates per-port collision domains; Router stops broadcasts.',
    expectedErrors: null
  },

  // INVALID SCENARIOS (18)
  {
    code: 'I1',
    name: 'Duplicate IP Address',
    category: 'INVALID',
    difficulty: 'BEGINNER',
    deviceCount: 3,
    description: 'Two separate PCs configured with identical IPv4 address (192.168.1.10) on the same switched LAN segment.',
    concepts: 'IP uniqueness constraint, gratuitous ARP conflict, ARP table poisoning, address collision',
    expectedResult: 'Communication becomes erratic; ARP replies from both PCs cause MAC table thrashing and packet delivery failures.',
    expectedErrors: 'Network validation error: Duplicate IP address found in project: 192.168.1.10'
  },
  {
    code: 'I2',
    name: 'Incorrect Subnet Mask',
    category: 'INVALID',
    difficulty: 'BEGINNER',
    deviceCount: 3,
    description: 'PC1 is configured with /24 (255.255.255.0) while PC2 is configured with /16 (255.255.0.0) on what was intended to be the same subnet.',
    concepts: 'Subnet locality evaluation, asymmetric routing, default gateway requirement for non-local destinations',
    expectedResult: 'PC1 believes PC2 is local and sends ARP, but PC2 considers PC1 non-local and attempts to route via nonexistent gateway.',
    expectedErrors: 'Asymmetric subnet mask: PC2 treats 192.168.1.10 as non-local and drops packet with NO_DEFAULT_GATEWAY.'
  },
  {
    code: 'I3',
    name: 'Invalid Default Gateway',
    category: 'INVALID',
    difficulty: 'BEGINNER',
    deviceCount: 3,
    description: 'Host IP is in 192.168.1.0/24 but its default gateway is set to 10.0.0.1, which is outside the host local subnet.',
    concepts: 'Gateway locality rule, direct L2 ARP resolution requirement for default gateways',
    expectedResult: 'Host cannot resolve gateway MAC address via ARP because gateway IP is not in its local subnet. Remote packets are discarded.',
    expectedErrors: 'Gateway IP 10.0.0.1 does not belong to interface subnet 192.168.1.0/24.'
  },
  {
    code: 'I4',
    name: 'Missing Default Gateway',
    category: 'INVALID',
    difficulty: 'BEGINNER',
    deviceCount: 4,
    description: 'PC1 attempts to communicate with a remote server in another subnet (192.168.2.10) but has no default gateway configured.',
    concepts: 'Subnet boundaries, non-local destination detection, default gateway forwarding requirement',
    expectedResult: 'PC1 determines target IP is non-local, discovers no default gateway is configured, and drops packet with NO_DEFAULT_GATEWAY.',
    expectedErrors: 'Packet dropped: Destination 192.168.2.10 is remote and no default gateway is configured on interface eth0.'
  },
  {
    code: 'I5',
    name: 'Missing Routing Table Entry',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 4,
    description: 'Router receives packet destined for remote network 192.168.3.0/24, but its routing table has no matching static or dynamic route.',
    concepts: 'Longest prefix match failure, blackhole drop, ICMP Destination Network Unreachable (Type 3 Code 0)',
    expectedResult: 'Router performs route lookup, finds no matching destination or default route, and generates ICMP Destination Unreachable.',
    expectedErrors: 'ICMP Destination Unreachable: No route to host 192.168.3.10 in router routing table.'
  },
  {
    code: 'I6',
    name: 'Disconnected Network Link',
    category: 'INVALID',
    difficulty: 'BEGINNER',
    deviceCount: 3,
    description: 'Cable connecting PC1 to Switch1 is unplugged (Connection status INACTIVE). Frame cannot propagate across Layer 1 medium.',
    concepts: 'Layer 1 physical link check, cable fault detection, carrier detect loss',
    expectedResult: 'Simulation drops frame at physical layer with LINK_DOWN / DISCONNECTED_CABLE error.',
    expectedErrors: 'Interface eth0 is operationally DOWN (disconnected cable).'
  },
  {
    code: 'I7',
    name: 'Interface Disabled (Admin Down)',
    category: 'INVALID',
    difficulty: 'BEGINNER',
    deviceCount: 3,
    description: 'Interface eth0 on PC1 is administratively shut down (AdminStatus = DOWN). Interface cannot transmit or receive frames.',
    concepts: 'Administrative state vs operational state, interface shutdown, Layer 2 interface gating',
    expectedResult: 'Frame transmission attempt is blocked by device kernel with INTERFACE_ADMIN_DOWN error.',
    expectedErrors: 'Interface eth0 is administratively disabled. Packet cannot be sent.'
  },
  {
    code: 'I8',
    name: 'Incorrect ARP Resolution (Unreachable IP)',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 3,
    description: 'PC1 transmits ARP Request for 192.168.1.99, an IP address not assigned to any device on the switched LAN.',
    concepts: 'ARP request broadcast, ARP reply timeout, MAC address resolution failure, host unreachable drop',
    expectedResult: 'ARP request broadcasts out all switch ports, no host responds, ARP times out after retries, and ping drops with ARP_TIMEOUT.',
    expectedErrors: 'ARP resolution timeout: No response received for IP 192.168.1.99.'
  },
  {
    code: 'I9',
    name: 'VLAN Misconfiguration',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 3,
    description: 'PC1 is assigned to VLAN 10 and PC2 is assigned to VLAN 20 on the same switch without inter-VLAN routing.',
    concepts: 'VLAN broadcast domain isolation, Layer 2 segmentation, 802.1Q boundary enforcement',
    expectedResult: 'Switch isolates frames by VLAN tag; broadcast and unicast frames on VLAN 10 are never forwarded to VLAN 20.',
    expectedErrors: 'Frame dropped: Port 2 is on VLAN 20; frame from VLAN 10 is isolated.'
  },
  {
    code: 'I10',
    name: 'Routing Loop / TTL Expiration',
    category: 'INVALID',
    difficulty: 'ADVANCED',
    deviceCount: 4,
    description: 'Router1 forwards 10.0.0.0/8 to Router2, while Router2 routes 10.0.0.0/8 back to Router1, creating a circular routing loop.',
    concepts: 'Routing loop formation, IP Time-To-Live (TTL) field decrement, ICMP Time Exceeded (Type 11 Code 0)',
    expectedResult: 'Packet bounces continuously between Router1 and Router2 until TTL reaches 0, where router drops packet and returns ICMP Time Exceeded.',
    expectedErrors: 'ICMP Time Exceeded: TTL expired in transit (hop limit reached).'
  },
  {
    code: 'I11',
    name: 'DHCP Failure (No Active Server)',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 2,
    description: 'ClientPC broadcasts DHCP Discover message to 255.255.255.255, but no DHCP server is available on the LAN.',
    concepts: 'DHCP Discover broadcast, lease timeout, unconfigured interface status, APIPA fallback',
    expectedResult: 'Discover broadcasts out port1, times out after repeated attempts, leaving interface without an IPv4 address.',
    expectedErrors: 'DHCP Lease Timeout: No DHCP Offer received from any server on the subnet.'
  },
  {
    code: 'I12',
    name: 'DNS Failure (Unreachable Server)',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 3,
    description: 'Client is configured with an invalid DNS server IP (192.168.1.250). Application queries cannot resolve domain names to IP addresses.',
    concepts: 'DNS resolution path, UDP port 53 query timeout, difference between IP reachability and domain name resolution',
    expectedResult: 'DNS query to 192.168.1.250 times out. Direct IP ping to web server succeeds, proving network reachability while DNS is broken.',
    expectedErrors: 'DNS lookup failed: No response from DNS server 192.168.1.250. Direct IP ping works.'
  },
  {
    code: 'I13',
    name: 'ACL / Firewall Blocking',
    category: 'INVALID',
    difficulty: 'ADVANCED',
    deviceCount: 4,
    description: 'Router has an Access Control List (ACL) that denies TCP destination port 80 while permitting ICMP echo requests.',
    concepts: 'Stateful firewall / ACL packet filtering, transport port inspection, layer-specific denial rules',
    expectedResult: 'Ping (ICMP) from Client to WebServer succeeds, but HTTP GET on TCP port 80 is dropped by router ACL.',
    expectedErrors: 'ACL DENIED: TCP port 80 dropped by inbound access-list rule on Router1.'
  },
  {
    code: 'I14',
    name: 'Wireless Authentication Failure',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 2,
    description: 'Laptop attempts to associate with WPA2-secured AP using an incorrect pre-shared key (wrongpass vs nethub123).',
    concepts: '802.11 4-way handshake, pairwise master key (PMK) derivation, mic failure, authentication rejection',
    expectedResult: 'AP rejects 4-way handshake due to cryptographic MIC mismatch, disassociating the client station.',
    expectedErrors: 'Association Rejected: WPA2 pre-shared key authentication failure (Invalid MIC).'
  },
  {
    code: 'I15',
    name: 'MTU Mismatch (DF Bit Set)',
    category: 'INVALID',
    difficulty: 'ADVANCED',
    deviceCount: 4,
    description: 'Client transmits a 1500-byte frame with Don\'t Fragment (DF=1) across an intermediate link with MTU configured to 576 bytes.',
    concepts: 'Maximum Transmission Unit (MTU), IPv4 DF bit, Path MTU Discovery, ICMP Fragmentation Needed (Type 3 Code 4)',
    expectedResult: 'Router cannot forward packet without fragmentation, but DF bit forbids it; packet dropped with Fragmentation Needed error.',
    expectedErrors: 'Packet dropped: MTU exceeded (Packet size 1500 > Interface MTU 576) and DF bit set.'
  },
  {
    code: 'I16',
    name: 'Duplicate MAC Address',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 3,
    description: 'Conflicting MAC addresses assigned to two different devices on the same switched LAN.',
    concepts: 'MAC address table port flapping, frame misdirection, Layer 2 unicast collision',
    expectedResult: 'Switch continually overwrites its MAC address table port mappings (MAC flapping), causing erratic frame delivery.',
    expectedErrors: 'Network validation error: Duplicate MAC address found: 00:AA:BB:CC:DD:EE'
  },
  {
    code: 'I17',
    name: 'No External / Default Route',
    category: 'INVALID',
    difficulty: 'INTERMEDIATE',
    deviceCount: 3,
    description: 'PC sends packet to public Internet IP (8.8.8.8). Gateway router has routes for local subnets but no default route (0.0.0.0/0).',
    concepts: 'Default route of last resort (0.0.0.0/0), edge routing, exterior packet drop',
    expectedResult: 'Router matches no route for 8.8.8.8 in its routing table and drops the packet with NO_ROUTE_TO_HOST.',
    expectedErrors: 'Router has no default route (0.0.0.0/0) to forward public destination 8.8.8.8.'
  },
  {
    code: 'I18',
    name: 'Routing Protocol Misconfiguration',
    category: 'INVALID',
    difficulty: 'ADVANCED',
    deviceCount: 4,
    description: 'Router1 is configured in OSPF Area 0 while Router2 is configured in OSPF Area 1 across the same transit link.',
    concepts: 'OSPF Hello packet validation, Area ID field matching, neighbor adjacency state machine failure',
    expectedResult: 'Routers exchange Hello packets but reject adjacency establishment due to Area ID mismatch; no dynamic routes are exchanged.',
    expectedErrors: 'OSPF Adjacency Failure: Area ID mismatch (Area 0 vs Area 1).'
  }
];

