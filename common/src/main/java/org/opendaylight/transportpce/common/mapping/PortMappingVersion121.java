/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.NetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.cp.to.degree.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.cp.to.degree.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.cp.to.degree.CpToDegreeKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.Lldp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfig;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: many common pieces of code between PortMapping Versions 121 and 221 and 710
// some mutualization would be helpful
@SuppressWarnings("CPD-START")
public class PortMappingVersion121 {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingVersion121.class);
    private static final Map<Direction, String> SUFFIX;
    private static final String NOT_CORRECT_PARTNERPORT_LOGMSG =
        "{} : port {} on {} is not a correct partner port of {} on  {}";
    private static final String FETCH_CONNECTIONPORT_LOGMSG =
        "{} : Fetching connection-port {} at circuit pack {}";

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmInterfaces openRoadmInterfaces;

    static {
        SUFFIX =  Map.of(
            Direction.Tx, "TX",
            Direction.Rx, "RX",
            Direction.Bidirectional, "TXRX");
    }

    public PortMappingVersion121(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
        OpenRoadmInterfaces openRoadmInterfaces) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public boolean createMappingData(String nodeId) {
        LOG.info("{} : OpenROADM version 1.2.1 node - Creating Mapping Data", nodeId);
        List<Mapping> portMapList = new ArrayList<>();
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfoOptional = this.deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType
            .OPERATIONAL, infoIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceInfoOptional.isPresent()) {
            LOG.warn("{} : Device info subtree is absent", nodeId);
            return false;
        }
        Info deviceInfo = deviceInfoOptional.get();
        NodeInfo nodeInfo = createNodeInfo(deviceInfo);
        if (nodeInfo == null) {
            return false;
        }
        postPortMapping(nodeId, nodeInfo, null, null);

        switch (deviceInfo.getNodeType()) {

            case Rdm:
                // Get TTP port mapping
                if (!createTtpPortMapping(nodeId, deviceInfo, portMapList)) {
                    // return false if mapping creation for TTP's failed
                    LOG.warn("{} : Unable to create mapping for TTP's", nodeId);
                    return false;
                }

                // Get PP port mapping
                if (!createPpPortMapping(nodeId, deviceInfo, portMapList)) {
                    // return false if mapping creation for PP's failed
                    LOG.warn("{} : Unable to create mapping for PP's", nodeId);
                    return false;
                }
                break;
            case Xpdr:
                if (!createXpdrPortMapping(nodeId, portMapList)) {
                    LOG.warn("{} : Unable to create mapping for Xponder", nodeId);
                    return false;
                }
                break;
            default:
                LOG.error("{} : unknown nodetype - Unable to create mapping", nodeId);
                break;

        }
        return postPortMapping(nodeId, nodeInfo, portMapList, null);
    }

    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        InstanceIdentifier<Ports> portId = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(oldMapping.getSupportingCircuitPackName()))
            .child(Ports.class, new PortsKey(oldMapping.getSupportingPort()));
        if ((oldMapping == null) || (nodeId == null)) {
            LOG.error("Impossible to update mapping");
            return false;
        }
        try {
            Ports port = deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL,
                portId, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT).get();
            Mapping newMapping = updateMappingObject(nodeId, port, oldMapping);
            LOG.debug("{} : Updating old mapping Data {} for {} by new mapping data {}",
                    nodeId, oldMapping, oldMapping.getLogicalConnectionPoint(), newMapping);
            final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<Mapping> mapIID = InstanceIdentifier.create(Network.class)
                .child(Nodes.class, new NodesKey(nodeId))
                .child(Mapping.class, new MappingKey(oldMapping.getLogicalConnectionPoint()));
            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, mapIID, newMapping);
            FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();
            commit.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("{} : exception when updating Mapping {} - ",
                    nodeId, oldMapping.getLogicalConnectionPoint(), e);
            return false;
        }
    }

    private boolean createXpdrPortMapping(String nodeId, List<Mapping> portMapList) {
        // Creating for Xponder Line and Client Ports
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, deviceIID,
            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.error("{} : Impossible to get device configuration", nodeId);
            return false;
        }
        OrgOpenroadmDevice device = deviceObject.get();
        if (device.getCircuitPacks() == null) {
            LOG.warn("{} : Circuit Packs not present", nodeId);
            return false;
        }
        // Variable to keep track of number of line ports
        int line = 1;
        // Variable to keep track of number of client ports
        int client = 1;
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        List<CircuitPacks> circuitPackList = new ArrayList<>(device.nonnullCircuitPacks().values());
        circuitPackList.sort(Comparator.comparing(CircuitPack::getCircuitPackName));

        for (CircuitPacks cp : circuitPackList) {
            String circuitPackName = cp.getCircuitPackName();
            if (cp.getPorts() == null) {
                LOG.warn("{} : Ports were not found for circuit pack {}", nodeId, circuitPackName);
                continue;
            }

            // com.google.common.collect.ImmutableList implementation of List
            List<Ports> portList = new ArrayList<>(cp.nonnullPorts().values());
            portList.sort(Comparator.comparing(Ports::getPortName));
            for (Ports port : portList) {
                int[] counters = fillXpdrLcpsMaps(line, client, nodeId,
                    1, circuitPackName, port,
                    circuitPackList, lcpMap, mappingMap);
                line = counters[0];
                client = counters[1];
            }
        }

        Collection<ConnectionMap> connectionMap = deviceObject.get().nonnullConnectionMap().values();
        for (ConnectionMap cm : connectionMap) {
            String skey = cm.getSource().getCircuitPackName() + "+" + cm.getSource().getPortName();
            String slcp = lcpMap.containsKey(skey) ? lcpMap.get(skey) : null;
            Destination destination0 = cm.nonnullDestination().values().iterator().next();
            String dkey = destination0.getCircuitPackName() + "+" + destination0.getPortName();
            if (slcp == null) {
                LOG.error("{} : Error in connection-map analysis for source {} and destination (CP+port) {}",
                    nodeId, skey, dkey);
                continue;
            }
            String dlcp = lcpMap.containsKey(dkey) ? lcpMap.get(dkey) : null;
            Mapping mapping = mappingMap.get(slcp);
            mappingMap.remove(slcp);
            portMapList.add(createXpdrMappingObject(nodeId, null, null, null, null, mapping, dlcp));
        }

        if (!mappingMap.isEmpty()) {
            for (Mapping m : mappingMap.values()) {
                portMapList.add(m);
            }
        }
        return true;
    }

    private boolean checkPartnerPortNotNull(Ports port) {
        return (port.getPartnerPort() != null
            && port.getPartnerPort().getCircuitPackName() != null
            && port.getPartnerPort().getPortName() != null);
    }

    private boolean checkPartnerPortNoDir(String circuitPackName, Ports port1, Ports port2) {
        return (checkPartnerPortNotNull(port2)
            && port2.getPartnerPort().getCircuitPackName().equals(circuitPackName)
            && port2.getPartnerPort().getPortName().equals(port1.getPortName()));
    }

    private boolean checkPartnerPort(String circuitPackName, Ports port1, Ports port2) {
        return (checkPartnerPortNoDir(circuitPackName, port1, port2)
            && ((Direction.Rx.getIntValue() == port1.getPortDirection().getIntValue()
                    && Direction.Tx.getIntValue() == port2.getPortDirection().getIntValue())
                ||
                (Direction.Tx.getIntValue() == port1.getPortDirection().getIntValue()
                    && Direction.Rx.getIntValue() == port2.getPortDirection().getIntValue())));
    }

    private HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg
            .CircuitPacks>> getSrgCps(String deviceId, Info ordmInfo) {
        HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg
            .CircuitPacks>> cpPerSrg = new HashMap<>();
        // Get value for max Srg from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        Integer maxSrg = ordmInfo.getMaxSrgs() == null ? 20 : ordmInfo.getMaxSrgs().toJava();
        for (int srgCounter = 1; srgCounter <= maxSrg; srgCounter++) {
            List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCps
                = new ArrayList<>();
            LOG.debug("{} : Getting Circuitpacks for Srg Number {}", deviceId, srgCounter);
            InstanceIdentifier<SharedRiskGroup> srgIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(SharedRiskGroup.class, new SharedRiskGroupKey(Uint16.valueOf(srgCounter)));
            Optional<SharedRiskGroup> ordmSrgObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.OPERATIONAL, srgIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmSrgObject.isPresent()) {
                srgCps.addAll(ordmSrgObject.get().nonnullCircuitPacks().values());
                cpPerSrg.put(ordmSrgObject.get().getSrgNumber().toJava(), srgCps);
            }
        }
        LOG.info("{} : Device has {} Srg", deviceId, cpPerSrg.size());
        return cpPerSrg;
    }

    private boolean createPpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for SRG's PP
        HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>> srgCps
            = getSrgCps(nodeId, deviceInfo);

        for (Entry<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>>
                srgCpEntry : srgCps.entrySet()) {
            List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> cpList =
                srgCps.get(srgCpEntry.getKey());
            List<String> keys = new ArrayList<>();
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks cp : cpList) {
                String circuitPackName = cp.getCircuitPackName();
                InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName));
                Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                    LogicalDatastoreType.OPERATIONAL, cpIID,
                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

                if (!circuitPackObject.isPresent() || (circuitPackObject.get().getPorts() == null)) {
                    LOG.warn("{} : Circuit pack {} not found or without ports.", nodeId, circuitPackName);
                    continue;
                }
                // com.google.common.collect.ImmutableList implementation of List
                @Nullable
                List<Ports> portList = new ArrayList<>(circuitPackObject.get().nonnullPorts().values());
                Collections.sort(portList, new SortPort121ByName());
                int portIndex = 1;
                for (Ports port : portList) {
                    String currentKey = circuitPackName + "-" + port.getPortName();
                    if (port.getPortQual() == null) {
                        continue;
                    }

                    if (Port.PortQual.RoadmExternal.getIntValue() != port.getPortQual().getIntValue()) {
                        LOG.debug("{} : port {} on {} is not roadm-external - cannot assign logicalConnectionPoint.",
                                nodeId, port.getPortName(), circuitPackName);
                        continue;
                    }

                    if (keys.contains(currentKey)) {
                        LOG.debug("{} : port {} on {} has already been handled - cannot assign logicalConnectionPoint.",
                                nodeId, port.getPortName(), circuitPackName);
                        continue;
                    }

                    switch (port.getPortDirection()) {

                        case Bidirectional:
                            String lcp = createLogicalConnectionPort(port, srgCpEntry.getKey(), portIndex);
                            LOG.info("{} : port {} on {} - associated Logical Connection Point is {}",
                                    nodeId, port.getPortName(), circuitPackName, lcp);
                            portMapList.add(createMappingObject(nodeId, port, circuitPackName, lcp));
                            portIndex++;
                            keys.add(currentKey);
                            break;

                        case Rx:
                        case Tx:
                            if (!checkPartnerPortNotNull(port)) {
                                LOG.info("{} : port {} on {} is unidirectional but has no valid partnerPort"
                                        + " - cannot assign  logicalConnectionPoint.",
                                        nodeId, port.getPortName(), circuitPackName);
                                continue;
                            }

                            String lcp1 = createLogicalConnectionPort(port, srgCpEntry.getKey(), portIndex);
                            LOG.info("{} :  port {} on {} - associated Logical Connection Point is {}",
                                    nodeId, port.getPortName(), circuitPackName, lcp1);
                            InstanceIdentifier<Ports> port2ID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                                .child(CircuitPacks.class,
                                    new CircuitPacksKey(port.getPartnerPort().getCircuitPackName()))
                                .child(Ports.class, new PortsKey(port.getPartnerPort().getPortName()));
                            Optional<Ports> port2Object = this.deviceTransactionManager
                                .getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, port2ID,
                                    Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                            if (!port2Object.isPresent()
                                || port2Object.get().getPortQual().getIntValue()
                                    != Port.PortQual.RoadmExternal.getIntValue()) {
                                LOG.error("{} : port {} on {} - error getting partner",
                                        nodeId, port.getPartnerPort().getPortName(),
                                        port.getPartnerPort().getCircuitPackName());
                                continue;
                            }

                            Ports port2 = port2Object.get();
                            if (!checkPartnerPort(circuitPackName, port, port2)) {
                                LOG.error("{} : port {} on {} - Error with partner port configuration",
                                        nodeId, port.getPortName(), circuitPackName);
                                portIndex++;
                                continue;
                            }
                            String lcp2 = createLogicalConnectionPort(port2, srgCpEntry.getKey(),portIndex);
                            LOG.info("{} : port {} on {} - associated Logical Connection Point is {}",
                                    nodeId, port2.getPortName(), circuitPackName, lcp2);
                            portMapList.add(createMappingObject(nodeId, port, circuitPackName, lcp1));
                            portMapList.add(
                                createMappingObject(nodeId ,port2, port.getPartnerPort().getCircuitPackName(), lcp2));
                            portIndex++;
                            keys.add(currentKey);
                            keys.add(port.getPartnerPort().getCircuitPackName() + "-" + port2.getPortName());
                            break;

                        default:
                            LOG.error("{} : port {} on {} - unsupported Direction {}"
                                    + " - cannot assign  logicalConnectionPoint.",
                                    nodeId, port.getPortName(), circuitPackName, port.getPortDirection());

                    }
                }
            }
        }
        return true;
    }

    private String createLogicalConnectionPort(Ports port, int index, int portIndex) {
        if (SUFFIX.containsKey(port.getPortDirection())) {
            return String.join("-", "SRG" + index, "PP" + portIndex, SUFFIX.get(port.getPortDirection()));
        }
        LOG.error("port {} : Unsupported port direction {}", port, port.getPortDirection());
        return null;
    }

    private String createXpdrLogicalConnectionPort(int xponderNb, int lcpNb, String token) {
        return new StringBuilder("XPDR").append(xponderNb)
                .append("-")
                .append(token).append(lcpNb)
                .toString();
    }

    private Map<Integer, Degree> getDegreesMap(String deviceId, Info ordmInfo) {
        Map<Integer, Degree> degrees = new HashMap<>();

        // Get value for max degree from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        Integer maxDegree = ordmInfo.getMaxDegrees() == null ? 20 : ordmInfo.getMaxDegrees().toJava();

        for (int degreeCounter = 1; degreeCounter <= maxDegree; degreeCounter++) {
            LOG.debug("{} : Getting Connection ports for Degree Number {}", deviceId, degreeCounter);
            InstanceIdentifier<Degree> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Degree.class, new DegreeKey(Uint16.valueOf(degreeCounter)));
            Optional<Degree> ordmDegreeObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.OPERATIONAL, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmDegreeObject.isPresent()) {
                degrees.put(degreeCounter, ordmDegreeObject.get());
            }
        }
        LOG.info("{} : Device has {} degree(s)", deviceId, degrees.size());
        return degrees;
    }

    private Map<Integer, List<ConnectionPorts>> getPerDegreePorts(String deviceId, Info ordmInfo) {
        Map<Integer, List<ConnectionPorts>> conPortMap = new HashMap<>();
        getDegreesMap(deviceId, ordmInfo).forEach(
            (index, degree) -> conPortMap.put(index, new ArrayList<>(degree.nonnullConnectionPorts().values())));
        return conPortMap;
    }

    private Map<String, String> getEthInterfaceList(String nodeId) {
        LOG.info("{} : It is calling get ethernet interface", nodeId);
        InstanceIdentifier<Protocols> protocoliid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(Protocols.class);
        Optional<Protocols> protocolObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, protocoliid, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!protocolObject.isPresent() || protocolObject.get().augmentation(Protocols1.class).getLldp() == null) {
            LOG.warn("{} : Couldnt find port config under LLDP - Processiong is done.. now returning..", nodeId);
            return new HashMap<>();
        }
        Map<String, String> cpToInterfaceMap = new HashMap<>();
        Lldp lldp = protocolObject.get().augmentation(Protocols1.class).getLldp();
        for (PortConfig portConfig : lldp.nonnullPortConfig().values()) {
            if (!portConfig.getAdminStatus().equals(PortConfig.AdminStatus.Txandrx)) {
                continue;
            }
            InstanceIdentifier<Interface> interfaceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(portConfig.getIfName()));
            Optional<Interface> interfaceObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                LogicalDatastoreType.OPERATIONAL, interfaceIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!interfaceObject.isPresent() || (interfaceObject.get().getSupportingCircuitPackName() == null)) {
                continue;
            }
            String supportingCircuitPackName = interfaceObject.get().getSupportingCircuitPackName();
            cpToInterfaceMap.put(supportingCircuitPackName, portConfig.getIfName());
            InstanceIdentifier<CircuitPacks> circuitPacksIID = InstanceIdentifier
                .create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(supportingCircuitPackName));
            Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(
                nodeId, LogicalDatastoreType.OPERATIONAL, circuitPacksIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!circuitPackObject.isPresent() || (circuitPackObject.get().getParentCircuitPack() == null)) {
                continue;
            }
            cpToInterfaceMap.put(circuitPackObject.get().getParentCircuitPack().getCircuitPackName(),
                portConfig.getIfName());
        }
        LOG.info("{} : Processiong is done.. now returning..", nodeId);
        return cpToInterfaceMap;
    }

    private List<CpToDegree> getCpToDegreeList(Map<Integer, Degree> degrees, Map<String, String> interfaceList) {
        List<CpToDegree> cpToDegreeList = new ArrayList<>();
        for (Degree degree : degrees.values()) {
            LOG.debug("Inside CP to degree list");
            cpToDegreeList.addAll(degree.nonnullCircuitPacks().values().stream()
                .map(cp -> createCpToDegreeObject(cp.getCircuitPackName(),
                    degree.getDegreeNumber().toString(), interfaceList))
                .collect(Collectors.toList()));
        }
        return cpToDegreeList;
    }

    private boolean postPortMapping(String nodeId, NodeInfo nodeInfo, List<Mapping> portMapList,
            List<CpToDegree> cp2DegreeList) {
        NodesBuilder nodesBldr = new NodesBuilder().withKey(new NodesKey(nodeId)).setNodeId(nodeId);
        if (nodeInfo != null) {
            nodesBldr.setNodeInfo(nodeInfo);
        }
        if (portMapList != null) {
            Map<MappingKey, Mapping> mappingMap = new HashMap<>();
            // No element in the list below should be null at this stage
            for (Mapping mapping: portMapList) {
                mappingMap.put(mapping.key(), mapping);
            }
            nodesBldr.setMapping(mappingMap);
        }
        if (cp2DegreeList != null) {
            Map<CpToDegreeKey, CpToDegree> cpToDegreeMap = new HashMap<>();
            // No element in the list below should be null at this stage
            for (CpToDegree cp2Degree: cp2DegreeList) {
                cpToDegreeMap.put(cp2Degree.key(), cp2Degree);
            }
            nodesBldr.setCpToDegree(cpToDegreeMap);
        }

        Map<NodesKey,Nodes> nodesList = new HashMap<>();
        Nodes nodes = nodesBldr.build();
        nodesList.put(nodes.key(),nodes);

        Network network = new NetworkBuilder().setNodes(nodesList).build();

        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Network> nodesIID = InstanceIdentifier.builder(Network.class).build();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodesIID, network);
        FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();
        try {
            commit.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to post {}", network, e);
            return false;
        }
    }

    private CpToDegree createCpToDegreeObject(String circuitPackName, String degreeNumber,
            Map<String, String> interfaceList) {
        return new CpToDegreeBuilder()
            .withKey(new CpToDegreeKey(circuitPackName))
            .setCircuitPackName(circuitPackName)
            .setDegreeNumber(Uint32.valueOf(degreeNumber))
            .setInterfaceName(interfaceList.get(circuitPackName)).build();
    }

    private Mapping createMappingObject(String nodeId, Ports port, String circuitPackName,
            String logicalConnectionPoint) {
        MappingBuilder mpBldr = new MappingBuilder()
                .withKey(new MappingKey(logicalConnectionPoint))
                .setLogicalConnectionPoint(logicalConnectionPoint)
                .setSupportingCircuitPackName(circuitPackName)
                .setSupportingPort(port.getPortName())
                .setPortDirection(port.getPortDirection().getName());
        if (port.getAdministrativeState() != null) {
            mpBldr.setPortAdminState(port.getAdministrativeState().name());
        }
        if (port.getOperationalState() != null) {
            mpBldr.setPortOperState(port.getOperationalState().name());
        }
        if (!logicalConnectionPoint.contains(StringConstants.TTP_TOKEN) || (port.getInterfaces() == null)) {
            return mpBldr.build();
        }
        mpBldr = updateMappingInterfaces(nodeId, mpBldr, port);
        return mpBldr.build();
    }

    private Mapping updateMappingObject(String nodeId, Ports port, Mapping oldmapping) {
        MappingBuilder mpBldr = new MappingBuilder(oldmapping);
        updateMappingStates(mpBldr, port, oldmapping);
        if ((port.getInterfaces() == null)
            || (!oldmapping.getLogicalConnectionPoint().contains(StringConstants.TTP_TOKEN)
                && !oldmapping.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN))) {
            return mpBldr.build();
        }
        // Get interfaces provisioned on the port
        mpBldr = updateMappingInterfaces(nodeId, mpBldr, port);
        return mpBldr.build();
    }

    private MappingBuilder updateMappingStates(MappingBuilder mpBldr, Ports port, Mapping oldmapping) {
        if (port.getAdministrativeState() != null
            && !port.getAdministrativeState().getName().equals(oldmapping.getPortAdminState())) {
            mpBldr.setPortAdminState(port.getAdministrativeState().name());
        }
        if (port.getOperationalState() != null
            && !port.getOperationalState().getName().equals(oldmapping.getPortOperState())) {
            mpBldr.setPortOperState(port.getOperationalState().name());
        }
        return mpBldr;
    }

    private MappingBuilder updateMappingInterfaces(String nodeId, MappingBuilder mpBldr, Ports port) {
        for (Interfaces interfaces : port.getInterfaces()) {
            try {
                Optional<Interface> openRoadmInterface = this.openRoadmInterfaces.getInterface(nodeId,
                    interfaces.getInterfaceName());
                if (!openRoadmInterface.isPresent()) {
                    LOG.warn("{} : Interface {} was null!", nodeId, interfaces.getInterfaceName());
                    continue;
                }
                LOG.debug("{} : interface get from device is {} and of type {}",
                    nodeId, openRoadmInterface.get().getName(), openRoadmInterface.get().getType());
                Class<? extends InterfaceType> interfaceType
                    = (Class<? extends InterfaceType>) openRoadmInterface.get().getType();
                // Check if interface type is OMS or OTS
                if (interfaceType.equals(OpenROADMOpticalMultiplex.class)) {
                    mpBldr.setSupportingOms(interfaces.getInterfaceName());
                }
                if (interfaceType.equals(OpticalTransport.class)) {
                    mpBldr.setSupportingOts(interfaces.getInterfaceName());
                }
            } catch (OpenRoadmInterfaceException ex) {
                LOG.warn("{} : Error while getting interface {} - ",
                    nodeId, interfaces.getInterfaceName(), ex);
            }
        }
        return mpBldr;
    }

    private Mapping createXpdrMappingObject(String nodeId, Ports port, String circuitPackName,
            String logicalConnectionPoint, String partnerLcp, Mapping mapping, String assoLcp) {

        if (mapping != null && assoLcp != null) {
            // update existing mapping
            return new MappingBuilder(mapping).setConnectionMapLcp(assoLcp).build();
        }

        // create a new mapping
        String nodeIdLcp = nodeId + "-" + logicalConnectionPoint;
        MappingBuilder mpBldr = new MappingBuilder()
                .withKey(new MappingKey(logicalConnectionPoint))
                .setLogicalConnectionPoint(logicalConnectionPoint)
                .setSupportingCircuitPackName(circuitPackName)
                .setSupportingPort(port.getPortName())
                .setPortDirection(port.getPortDirection().getName())
                .setLcpHashVal(FnvUtils.fnv1size64(nodeIdLcp));
        if (port.getPortQual() != null) {
            mpBldr.setPortQual(port.getPortQual().getName());
        }
        if (partnerLcp != null) {
            mpBldr.setPartnerLcp(partnerLcp);
        }
        if (port.getAdministrativeState() != null) {
            mpBldr.setPortAdminState(port.getAdministrativeState().name());
        }
        if (port.getOperationalState() != null) {
            mpBldr.setPortOperState(port.getOperationalState().name());
        }
        return mpBldr.build();
    }

    private Ports getPort2(Ports port, String nodeId, String circuitPackName, StringBuilder circuitPackName2,
            //circuitPackName2 will be updated by reference contrary to circuitPackName
            List<CircuitPacks> circuitPackList, Map<String, String> lcpMap) {
        if (!checkPartnerPortNotNull(port)) {
            LOG.warn("{} : port {} on {} - Error in the configuration ",
                    nodeId, port.getPortName(), circuitPackName);
            return null;
        }
        if (lcpMap.containsKey(circuitPackName + '+' + port.getPortName())) {
            return null;
        }
        Optional<CircuitPacks> cpOpt = circuitPackList.stream()
            .filter(cP -> cP.getCircuitPackName().equals(port.getPartnerPort().getCircuitPackName()))
            .findFirst();
        if (!cpOpt.isPresent()) {
            LOG.error("{} : Error fetching circuit-pack {}",
                    nodeId, port.getPartnerPort().getCircuitPackName());
            return null;
        }
        Optional<Ports> poOpt = cpOpt.get().nonnullPorts().values().stream()
            .filter(p -> p.getPortName().equals(port.getPartnerPort().getPortName()))
            .findFirst();
        if (!poOpt.isPresent()) {
            LOG.error("{} : Error fetching port {} on {}",
                    nodeId, port.getPartnerPort().getPortName(), port.getPartnerPort().getCircuitPackName());
            return null;
        }
        Ports port2 = poOpt.get();
        circuitPackName2.append(cpOpt.get().getCircuitPackName());
        if (!checkPartnerPort(circuitPackName, port, port2)) {
            LOG.error(NOT_CORRECT_PARTNERPORT_LOGMSG,
                    nodeId, port2.getPortName(), circuitPackName2, port.getPortName(), circuitPackName);
            return null;
        }
        return port2;
    }

    private void putXpdrLcpsInMaps(int line, String nodeId,
            Integer xponderNb,
            String circuitPackName, String circuitPackName2, Ports port, Ports port2,
            Map<String, String> lcpMap, Map<String, Mapping> mappingMap) {
        String lcp1 = createXpdrLogicalConnectionPort(xponderNb, line, StringConstants.NETWORK_TOKEN);
        String lcp2 = createXpdrLogicalConnectionPort(xponderNb, line + 1, StringConstants.NETWORK_TOKEN);
        if (lcpMap.containsKey(lcp1) || lcpMap.containsKey(lcp2)) {
            LOG.warn("{} : mapping already exists for {} or {}", nodeId, lcp1, lcp2);
            return;
        }
        lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp1);
        lcpMap.put(circuitPackName2 + '+' + port2.getPortName(), lcp2);
        mappingMap.put(lcp1,
                createXpdrMappingObject(nodeId, port, circuitPackName, lcp1, lcp2, null, null));
        mappingMap.put(lcp2,
                createXpdrMappingObject(nodeId, port2, circuitPackName2, lcp2, lcp1, null, null));
        return;
    }

    private int[] fillXpdrLcpsMaps(int line, int client, String nodeId,
            Integer xponderNb,
            String circuitPackName,  Ports port,
            List<CircuitPacks> circuitPackList, Map<String, String> lcpMap, Map<String, Mapping> mappingMap) {

        if (port.getPortQual() == null) {
            LOG.warn("{} : port {} on {} - PortQual was not found",
                    nodeId, port.getPortName(), circuitPackName);
            return new int[] {line, client};
        }

        switch (port.getPortQual()) {

            case XpdrClient:
                String lcp0 = createXpdrLogicalConnectionPort(xponderNb, client, StringConstants.CLIENT_TOKEN);
                lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp0);
                mappingMap.put(lcp0,
                    createXpdrMappingObject(nodeId, port, circuitPackName, lcp0, null, null, null));
                client++;
                break;

            case XpdrNetwork:
                line = fillXpdrNetworkLcpsMaps(line, nodeId,
                        xponderNb,
                        circuitPackName,  port,
                        circuitPackList,  lcpMap, mappingMap);
                break;

            default:
                LOG.error("{} : port {} on {} - unsupported PortQual {}",
                        nodeId, port.getPortName(), circuitPackName, port.getPortQual());
        }
        return new int[] {line, client};
    }

    private int fillXpdrNetworkLcpsMaps(int line, String nodeId,
            Integer xponderNb,
            String circuitPackName,  Ports port,
            List<CircuitPacks> circuitPackList, Map<String, String> lcpMap, Map<String, Mapping> mappingMap) {

        switch (port.getPortDirection()) {

            case Bidirectional:
                String lcp = createXpdrLogicalConnectionPort(xponderNb, line, StringConstants.NETWORK_TOKEN);
                lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp);
                mappingMap.put(lcp,
                    createXpdrMappingObject(nodeId, port, circuitPackName, lcp, null, null, null));
                line++;
                break;

            case Rx:
            case Tx:
                StringBuilder circuitPackName2 = new StringBuilder();
                Ports port2 = getPort2(port, nodeId, circuitPackName, circuitPackName2,
                        circuitPackList, lcpMap);

                if (port2 == null) {
                     //key already present or an error occured and was logged
                    return line;
                }

                putXpdrLcpsInMaps(line, nodeId, xponderNb,
                        circuitPackName, circuitPackName2.toString(), port, port2,
                        lcpMap, mappingMap);

                line += 2;
                break;

            default:
                LOG.error("{} : port {} on {} - unsupported Direction {}",
                     nodeId, port.getPortName(), circuitPackName, port.getPortDirection());
        }

        return line;
    }

    private boolean createTtpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for degree TTP's
        Map<Integer, Degree> degrees = getDegreesMap(nodeId, deviceInfo);
        Map<String, String> interfaceList = getEthInterfaceList(nodeId);
        List<CpToDegree> cpToDegreeList = getCpToDegreeList(degrees, interfaceList);
        LOG.info("{} : Map looks like this {}", nodeId, interfaceList);
        postPortMapping(nodeId, null, null, cpToDegreeList);

        Map<Integer, List<ConnectionPorts>> connectionPortMap = getPerDegreePorts(nodeId, deviceInfo);
        for (Entry<Integer, List<ConnectionPorts>> cpMapEntry : connectionPortMap.entrySet()) {
            switch (connectionPortMap.get(cpMapEntry.getKey()).size()) {
                case 1:
                    // port is bidirectional
                    InstanceIdentifier<Ports> portID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class,
                            new CircuitPacksKey(connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName()))
                        .child(Ports.class,
                            new PortsKey(connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName()));
                    LOG.debug(FETCH_CONNECTIONPORT_LOGMSG,
                            nodeId,
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName(),
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName());
                    Optional<Ports> portObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, portID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    if (!portObject.isPresent()) {
                        LOG.error("{} : No port {} on circuit pack {}",
                                nodeId,
                                connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName(),
                                connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName());
                        return false;
                    }
                    Ports port = portObject.get();
                    if (port.getPortQual() == null) {
                        continue;
                    }
                    if (Port.PortQual.RoadmExternal.getIntValue() != port.getPortQual().getIntValue()
                        || Direction.Bidirectional.getIntValue() != port.getPortDirection().getIntValue()) {
                        LOG.error("{} : port {} on {} - Impossible to create logical connection point"
                                + " - Error in configuration with port-qual or port-direction",
                                nodeId, port.getPortName(),
                                connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName());
                        continue;
                    }
                    String logicalConnectionPoint = new StringBuilder("DEG")
                        .append(cpMapEntry.getKey())
                        .append("-TTP-TXRX")
                        .toString();
                    LOG.info("{} : Logical Connection Point for {} on {} is {}",
                            nodeId,
                            port.getPortName(), connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(),
                            logicalConnectionPoint);
                    portMapList.add(createMappingObject(nodeId, port,
                        connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(),
                        logicalConnectionPoint));
                    break;
                case 2:
                    // ports are unidirectionals
                    String cp1Name = connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName();
                    String cp2Name = connectionPortMap.get(cpMapEntry.getKey()).get(1).getCircuitPackName();
                    InstanceIdentifier<Ports> port1ID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class, new CircuitPacksKey(cp1Name))
                        .child(Ports.class,
                            new PortsKey(connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName()));
                    LOG.debug(FETCH_CONNECTIONPORT_LOGMSG,
                            nodeId, connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName(), cp1Name);
                    Optional<Ports> port1Object = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, port1ID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    InstanceIdentifier<Ports> port2ID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class, new CircuitPacksKey(cp2Name))
                        .child(Ports.class,
                            new PortsKey(connectionPortMap.get(cpMapEntry.getKey()).get(1).getPortName()));
                    LOG.debug(FETCH_CONNECTIONPORT_LOGMSG,
                            nodeId, connectionPortMap.get(cpMapEntry.getKey()).get(1).getPortName(), cp2Name);
                    Optional<Ports> port2Object = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, port2ID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    if (!port1Object.isPresent() || !port2Object.isPresent()) {
                        LOG.error("No port {} on circuit pack {} for node {}",
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName(),
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(), nodeId);
                        return false;
                    }

                    Ports port1 = port1Object.get();
                    Ports port2 = port2Object.get();
                    if (port1.getPortQual() == null || port2.getPortQual() == null) {
                        continue;
                    }
                    if (Port.PortQual.RoadmExternal.getIntValue() != port1.getPortQual().getIntValue()
                        || Port.PortQual.RoadmExternal.getIntValue() != port2.getPortQual().getIntValue()) {
                        LOG.error("{} : Impossible to create logical connection point for port {} or port {}"
                                + " - Error in configuration with port-qual",
                                nodeId, port1.getPortName(), port2.getPortName());
                        continue;
                    }
                    if (!checkPartnerPort(cp1Name, port1, port2)) {
                        LOG.error(NOT_CORRECT_PARTNERPORT_LOGMSG,
                                nodeId, port2.getPortName(), cp2Name, port1.getPortName(), cp1Name);
                        continue;
                    }
                    // Directions checks are the same for cp1 and cp2, no need to check them twice.
                    if (!checkPartnerPortNoDir(cp2Name, port2, port1)) {
                        LOG.error(NOT_CORRECT_PARTNERPORT_LOGMSG,
                                nodeId, port1.getPortName(), cp1Name, port2.getPortName(), cp2Name);
                        continue;
                    }

                    String logicalConnectionPoint1 = new StringBuilder("DEG")
                        .append(cpMapEntry.getKey())
                        .append("-TTP-")
                        .append(port1.getPortDirection().getName().toUpperCase(Locale.getDefault()))
                        .toString();
                    LOG.info("{} : Logical Connection Point for {} {} is {}",
                            nodeId,
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(),
                            port1.getPortName(), logicalConnectionPoint1);
                    portMapList.add(createMappingObject(nodeId, port1,
                        connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(),
                        logicalConnectionPoint1));
                    String logicalConnectionPoint2 = new StringBuilder("DEG")
                        .append(cpMapEntry.getKey())
                        .append("-TTP-")
                        .append(port2.getPortDirection().getName().toUpperCase(Locale.getDefault()))
                        .toString();
                    LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId,
                        connectionPortMap.get(cpMapEntry.getKey()).get(1).getCircuitPackName(),
                        port2.getPortName(), logicalConnectionPoint2);
                    portMapList.add(createMappingObject(nodeId, port2,
                        connectionPortMap.get(cpMapEntry.getKey()).get(1).getCircuitPackName(),
                        logicalConnectionPoint2));
                    break;
                default:
                    LOG.error("{} : Number of connection port for DEG{} is incorrect", nodeId, cpMapEntry.getKey());
                    continue;
            }
        }
        return true;
    }

    private NodeInfo createNodeInfo(Info deviceInfo) {

        if (deviceInfo.getNodeType() == null) {
            // TODO make mandatory in yang
            LOG.error("Node type field is missing");
            return null;
        }

        NodeInfoBuilder nodeInfoBldr = new NodeInfoBuilder()
                .setOpenroadmVersion(OpenroadmNodeVersion._121);
        // TODO check if we can use here .setNodeType(NodeTypes.forValue(..) such as with 221
        switch (deviceInfo.getNodeType().getIntValue()) {
            case 1:
            case 2:
                nodeInfoBldr.setNodeType(NodeTypes.forValue(deviceInfo.getNodeType().getIntValue()));
                break;
            default:
                LOG.error("Error with node-type of {}", deviceInfo.getNodeId());
                // TODO: is this protection useful ? it is not present in Portmapping 221
        }
        if (deviceInfo.getClli() != null && !deviceInfo.getClli().isEmpty()) {
            nodeInfoBldr.setNodeClli(deviceInfo.getClli());
        } else {
            nodeInfoBldr.setNodeClli("defaultCLLI");
        }
        if (deviceInfo.getModel() != null) {
            nodeInfoBldr.setNodeModel(deviceInfo.getModel());
        }
        if (deviceInfo.getVendor() != null) {
            nodeInfoBldr.setNodeVendor(deviceInfo.getVendor());
        }
        if (deviceInfo.getIpAddress() != null) {
            nodeInfoBldr.setNodeIpAddress(deviceInfo.getIpAddress());
        }

        return nodeInfoBldr.build();
    }

}
