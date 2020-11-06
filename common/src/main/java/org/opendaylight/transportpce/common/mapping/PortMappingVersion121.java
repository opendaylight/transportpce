/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import com.google.common.util.concurrent.FluentFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.NetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.CpToDegreeKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.NodeInfo.OpenroadmVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev201012.network.nodes.NodeInfoBuilder;
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

public class PortMappingVersion121 extends PortMappingCommon {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingVersion121.class);

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmInterfaces openRoadmInterfaces;

    public PortMappingVersion121(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
        OpenRoadmInterfaces openRoadmInterfaces) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public boolean createMappingData(String nodeId) {
        LOG.info("Create Mapping Data for node 1.2.1 {}", nodeId);
        List<Mapping> portMapList = new ArrayList<>();
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfoOptional = this.deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType
            .OPERATIONAL, infoIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        Info deviceInfo;
        NodeInfo nodeInfo;
        if (deviceInfoOptional.isPresent()) {
            deviceInfo = deviceInfoOptional.get();
            nodeInfo = createNodeInfo(deviceInfo);
            if (nodeInfo == null) {
                return false;
            } else {
                postPortMapping(nodeId, nodeInfo, null, null);
            }
        } else {
            LOG.warn("Device info subtree is absent for {}", nodeId);
            return false;
        }

        switch (deviceInfo.getNodeType()) {

            case Rdm:
                // Get TTP port mapping
                if (!createTtpPortMapping(nodeId, deviceInfo, portMapList)) {
                    // return false if mapping creation for TTP's failed
                    LOG.warn("Unable to create mapping for TTP's on node {}", nodeId);
                    return false;
                }

                // Get PP port mapping
                if (!createPpPortMapping(nodeId, deviceInfo, portMapList)) {
                    // return false if mapping creation for PP's failed
                    LOG.warn("Unable to create mapping for PP's on node {}", nodeId);
                    return false;
                }
                break;
            case Xpdr:
                if (!createXpdrPortMapping(nodeId, portMapList)) {
                    LOG.warn("Unable to create mapping for Xponder on node {}", nodeId);
                    return false;
                }
                break;
            default:
                LOG.error("Unable to create mapping for node {} : unknown nodetype ", nodeId);
                break;

        }
        return postPortMapping(nodeId, nodeInfo, portMapList, null);
    }

    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        InstanceIdentifier<Ports> portIId = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(oldMapping.getSupportingCircuitPackName()))
            .child(Ports.class, new PortsKey(oldMapping.getSupportingPort()));
        if ((oldMapping != null) && (nodeId != null)) {
            try {
                Optional<Ports> portObject = deviceTransactionManager.getDataFromDevice(nodeId,
                    LogicalDatastoreType.OPERATIONAL, portIId, Timeouts.DEVICE_READ_TIMEOUT,
                    Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (portObject.isPresent()) {
                    Ports port = portObject.get();
                    Mapping newMapping = createMappingObject(nodeId, port, oldMapping.getSupportingCircuitPackName(),
                        oldMapping.getLogicalConnectionPoint());
                    LOG.info("Updating old mapping Data {} for {} of {} by new mapping data {}", oldMapping,
                        oldMapping.getLogicalConnectionPoint(), nodeId, newMapping);
                    final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
                    InstanceIdentifier<Mapping> mapIID = InstanceIdentifier.create(Network.class)
                        .child(Nodes.class, new NodesKey(nodeId))
                        .child(Mapping.class, new MappingKey(oldMapping.getLogicalConnectionPoint()));
                    writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, mapIID, newMapping);
                    FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();
                    commit.get();
                    return true;
                }
                return false;
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error updating Mapping {} for node {}", oldMapping.getLogicalConnectionPoint(), nodeId, e);
                return false;
            }
        } else {
            LOG.error("Impossible to update mapping");
            return false;
        }
    }

    private boolean createXpdrPortMapping(String nodeId, List<Mapping> portMapList) {
        // Creating for Xponder Line and Client Ports
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, deviceIID,
            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        // Variable to keep track of number of line ports
        int line = 1;
        // Variable to keep track of number of client ports
        int client = 1;
        if (!deviceObject.isPresent() || deviceObject.get().getCircuitPacks() == null) {
            LOG.warn("Circuit Packs are not present for {}", nodeId);
            return false;
            // TODO return false or continue?
        }
        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();

        // com.google.common.collect.ImmutableList implementation of List
        List<CircuitPacks> circuitPackList = new ArrayList<>(deviceObject.get()
                .nonnullCircuitPacks().values());
        circuitPackList.sort(Comparator.comparing(CircuitPack::getCircuitPackName));

        for (CircuitPacks cp : circuitPackList) {
            String circuitPackName = cp.getCircuitPackName();
            if (cp.getPorts() == null) {
                LOG.warn("Ports were not found for circuit pack: {}", circuitPackName);
                continue;
            }

            // com.google.common.collect.ImmutableList implementation of List
            List<Ports> portList = new ArrayList<>(cp.nonnullPorts().values());
            portList.sort(Comparator.comparing(Ports::getPortName));
            for (Ports port : portList) {
                if (port.getPortQual() == null) {
                    LOG.warn("PortQual was not found for port {} on circuit pack: {}", port.getPortName(),
                        circuitPackName);
                    continue;
                }
                if (Port.PortQual.XpdrNetwork.getIntValue() == port.getPortQual().getIntValue()
                    && port.getPortDirection().getIntValue() == Direction.Bidirectional.getIntValue()) {
                    String lcp = "XPDR1-" + StringConstants.NETWORK_TOKEN + line;
                    lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp);
                    mappingMap.put(lcp, createXpdrMappingObject(nodeId, port, circuitPackName, lcp, null, null, null));
                    line++;
                } else if (Port.PortQual.XpdrNetwork.getIntValue() == port.getPortQual().getIntValue()
                    && port.getPortDirection().getIntValue() != Direction.Bidirectional.getIntValue()
                    && port.getPartnerPort() != null
                    && port.getPartnerPort().getCircuitPackName() != null
                    && port.getPartnerPort().getPortName() != null) {
                    if (lcpMap.containsKey(circuitPackName + '+' + port.getPortName())) {
                        continue;
                    }
                    String lcp1 = "XPDR1-" + StringConstants.NETWORK_TOKEN + line;
                    Optional<CircuitPacks> cpOpt = circuitPackList.stream().filter(cP -> cP.getCircuitPackName()
                        .equals(port.getPartnerPort().getCircuitPackName())).findFirst();
                    if (cpOpt.isPresent()) {
                        Optional<Ports> poOpt = cpOpt.get().nonnullPorts()
                                .values().stream().filter(p -> p.getPortName().equals(port
                            .getPartnerPort().getPortName().toString())).findFirst();
                        if (poOpt.isPresent()) {
                            Ports port2 = poOpt.get();
                            if ((Direction.Rx.getIntValue() == port.getPortDirection().getIntValue()
                                && Direction.Tx.getIntValue() == port2.getPortDirection().getIntValue()
                                && port2.getPartnerPort() != null && port2.getPartnerPort().getCircuitPackName() != null
                                && port2.getPartnerPort().getPortName() != null
                                && port2.getPartnerPort().getCircuitPackName().equals(circuitPackName)
                                && port2.getPartnerPort().getPortName().equals(port.getPortName()))
                                ||
                                (Direction.Tx.getIntValue() == port.getPortDirection().getIntValue()
                                && Direction.Rx.getIntValue() == port2.getPortDirection().getIntValue()
                                && port2.getPartnerPort() != null && port2.getPartnerPort().getCircuitPackName() != null
                                && port2.getPartnerPort().getPortName() != null
                                && port2.getPartnerPort().getCircuitPackName().equals(circuitPackName)
                                && port2.getPartnerPort().getPortName().equals(port.getPortName()))) {
                                String lcp2 = new StringBuilder("XPDR1-").append(StringConstants.NETWORK_TOKEN)
                                    .append(line + 1).toString();
                                if (!lcpMap.containsKey(lcp1) && !lcpMap.containsKey(lcp2)) {
                                    lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp1);
                                    lcpMap.put(cpOpt.get().getCircuitPackName() + '+' + port2.getPortName(), lcp2);
                                    mappingMap.put(lcp1, createXpdrMappingObject(nodeId, port, circuitPackName, lcp1,
                                        lcp2, null, null));
                                    mappingMap.put(lcp2, createXpdrMappingObject(nodeId, port2,
                                        cpOpt.get().getCircuitPackName(), lcp2, lcp1, null, null));
                                } else {
                                    LOG.warn("mapping already exists for {} or {}", lcp1, lcp2);
                                }
                                line += 2;
                            } else {
                                LOG.error("port {} on {} is not a correct partner port of {} on  {}",
                                    port2.getPortName(), cpOpt.get().getCircuitPackName(), port.getPortName(),
                                    circuitPackName);
                            }
                        } else {
                            LOG.error("Error fetching port {} on {} for {}", port.getPartnerPort().getPortName(),
                                port.getPartnerPort().getCircuitPackName(), nodeId);
                        }
                    } else {
                        LOG.error("Error fetching circuit-pack {} for {}", port.getPartnerPort().getCircuitPackName(),
                            nodeId);
                    }
                } else if (Port.PortQual.XpdrClient.getIntValue() == port.getPortQual().getIntValue()) {
                    String lcp = "XPDR1-" + StringConstants.CLIENT_TOKEN + client;
                    lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp);
                    mappingMap.put(lcp, createXpdrMappingObject(nodeId, port, circuitPackName, lcp, null, null, null));
                    client++;
                } else {
                    LOG.warn("Error in the configuration of port {} of {} for {}", port.getPortName(), circuitPackName,
                        nodeId);
                }
            }
        }
        Collection<ConnectionMap> connectionMap = deviceObject.get().nonnullConnectionMap().values();
        String slcp = null;
        String dlcp = null;
        for (ConnectionMap cm : connectionMap) {
            String skey = cm.getSource().getCircuitPackName() + "+" + cm.getSource().getPortName();
            if (lcpMap.containsKey(skey)) {
                slcp = lcpMap.get(skey);
            }
            Destination destination0 = cm.nonnullDestination().values().iterator().next();
            String dkey = destination0.getCircuitPackName() + "+" + destination0.getPortName();
            if (lcpMap.containsKey(dkey)) {
                dlcp = lcpMap.get(dkey);
            }
            if (slcp != null) {
                Mapping mapping = mappingMap.get(slcp);
                mappingMap.remove(slcp);
                portMapList.add(createXpdrMappingObject(nodeId, null, null, null, null, mapping, dlcp));
            } else {
                LOG.error("Error in connection-map analysis");
            }
        }
        if (!mappingMap.isEmpty()) {
            for (Mapping m : mappingMap.values()) {
                portMapList.add(m);
            }
        }
        return true;
    }

    private HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg
        .CircuitPacks>> getSrgCps(String deviceId, Info ordmInfo) {
        HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg
            .CircuitPacks>> cpPerSrg = new HashMap<>();
        Integer maxSrg;
        // Get value for max Srg from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        if (ordmInfo.getMaxSrgs() != null) {
            maxSrg = ordmInfo.getMaxSrgs().toJava();
        } else {
            maxSrg = 20;
        }
        for (int srgCounter = 1; srgCounter <= maxSrg; srgCounter++) {
            List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCps
                = new ArrayList<>();
            LOG.info("Getting Circuitpacks for Srg Number {}", srgCounter);
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
        LOG.info("Device {} has {} Srg", deviceId, cpPerSrg.size());
        return cpPerSrg;
    }

    //last LOG info message in this method is too long
    @SuppressWarnings("checkstyle:linelength")
    private boolean createPpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for SRG's PP
        HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>> srgCps
            = getSrgCps(nodeId, deviceInfo);

        for (Entry<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>> srgCpEntry : srgCps.entrySet()) {
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
                    } else if (Port.PortQual.RoadmExternal.getIntValue() == port.getPortQual().getIntValue()
                        && Direction.Bidirectional.getIntValue() == port.getPortDirection().getIntValue()
                        && !keys.contains(currentKey)) {
                        String logicalConnectionPoint = createLogicalConnectionPort(port, srgCpEntry.getKey(), portIndex);
                        LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId, circuitPackName,
                            port.getPortName(), logicalConnectionPoint);
                        portMapList.add(createMappingObject(nodeId, port, circuitPackName, logicalConnectionPoint));
                        portIndex++;
                        keys.add(currentKey);
                    } else if (Port.PortQual.RoadmExternal.getIntValue() == port.getPortQual().getIntValue()
                        && (Direction.Rx.getIntValue() == port.getPortDirection().getIntValue()
                        || Direction.Tx.getIntValue() == port.getPortDirection().getIntValue())
                        && !keys.contains(currentKey)
                        && port.getPartnerPort() != null) {
                        String logicalConnectionPoint1 = createLogicalConnectionPort(port, srgCpEntry.getKey(),
                            portIndex);
                        LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId, circuitPackName,
                            port.getPortName(), logicalConnectionPoint1);
                        InstanceIdentifier<Ports> port2ID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                            .child(CircuitPacks.class, new CircuitPacksKey(port.getPartnerPort().getCircuitPackName()))
                            .child(Ports.class, new PortsKey(port.getPartnerPort().getPortName().toString()));
                        Optional<Ports> port2Object = this.deviceTransactionManager.getDataFromDevice(nodeId,
                            LogicalDatastoreType.OPERATIONAL, port2ID,
                            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                        if (port2Object.isPresent()
                            && port2Object.get().getPortQual().getIntValue()
                                == Port.PortQual.RoadmExternal.getIntValue()) {
                            Ports port2 = port2Object.get();
                            if ((port.getPortDirection().getIntValue() == Direction.Rx.getIntValue()
                                && port2.getPortDirection().getIntValue() == Direction.Tx.getIntValue()
                                && port2.getPartnerPort() != null
                                && port2.getPartnerPort().getCircuitPackName().equals(circuitPackName)
                                && port2.getPartnerPort().getPortName().toString().equals(port.getPortName()))
                                ||
                                (port.getPortDirection().getIntValue() == Direction.Tx.getIntValue()
                                && port2.getPortDirection().getIntValue() == Direction.Rx.getIntValue()
                                && port2.getPartnerPort() != null
                                && port2.getPartnerPort().getCircuitPackName().equals(circuitPackName)
                                && port2.getPartnerPort().getPortName().toString().equals(port.getPortName()))) {
                                String logicalConnectionPoint2 = createLogicalConnectionPort(port2, srgCpEntry.getKey(),
                                    portIndex);
                                LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId, circuitPackName,
                                    port2.getPortName(), logicalConnectionPoint2);
                                portMapList.add(createMappingObject(nodeId, port, circuitPackName,
                                    logicalConnectionPoint1));
                                portMapList.add(createMappingObject(nodeId, port2, port.getPartnerPort()
                                    .getCircuitPackName(), logicalConnectionPoint2));
                                portIndex++;
                                keys.add(currentKey);
                                keys.add(port.getPartnerPort().getCircuitPackName() + "-" + port2.getPortName());
                            } else {
                                LOG.error("Error with partner port configuration for port {} of  {} - {}",
                                    port.getPortName(), circuitPackName, nodeId);
                                portIndex++;
                            }
                        } else {
                            LOG.error("error getting partner port {} of  {} - {}",
                                port.getPartnerPort().getPortName().toString(),
                                port.getPartnerPort().getCircuitPackName(), nodeId);
                            continue;
                        }
                    } else {
                        LOG.info("{} : port {} on {} is not roadm-external or has already been handled. No logicalConnectionPoint assignment for this port.",
                            nodeId, port.getPortName(), circuitPackName);
                    }
                }
            }
        }
        return true;
    }

    private String createLogicalConnectionPort(Ports port, int index, int portIndex) {
        String lcp = null;
        switch (port.getPortDirection()) {
            case Tx:
                lcp = "SRG" + index + "-PP" + portIndex + "-TX";
                break;
            case Rx:
                lcp = "SRG" + index + "-PP" + portIndex + "-RX";
                break;
            case Bidirectional:
                lcp = "SRG" + index + "-PP" + portIndex + "-TXRX";
                break;
            default:
                LOG.error("Unsupported port direction for port {} : {}", port, port.getPortDirection());
        }
        return lcp;
    }

    private List<Degree> getDegrees(String deviceId, Info ordmInfo) {
        List<Degree> degrees = new ArrayList<>();
        Integer maxDegree;

        // Get value for max degree from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        if (ordmInfo.getMaxDegrees() != null) {
            maxDegree = ordmInfo.getMaxDegrees().toJava();
        } else {
            maxDegree = 20;
        }

        for (int degreeCounter = 1; degreeCounter <= maxDegree; degreeCounter++) {
            LOG.info("Getting Connection ports for Degree Number {}", degreeCounter);
            InstanceIdentifier<Degree> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Degree.class, new DegreeKey(Uint16.valueOf(degreeCounter)));
            Optional<Degree> ordmDegreeObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.OPERATIONAL, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmDegreeObject.isPresent()) {
                degrees.add(ordmDegreeObject.get());
            }
        }
        LOG.info("Device {} has {} degree", deviceId, degrees.size());
        return degrees;
    }

    private Map<Integer, List<ConnectionPorts>> getPerDegreePorts(String deviceId, Info ordmInfo) {
        Map<Integer, List<ConnectionPorts>> conPortMap = new HashMap<>();
        Integer maxDegree;

        if (ordmInfo.getMaxDegrees() != null) {
            maxDegree = ordmInfo.getMaxDegrees().toJava();
        } else {
            maxDegree = 20;
        }
        for (int degreeCounter = 1; degreeCounter <= maxDegree; degreeCounter++) {
            LOG.info("Getting Connection ports for Degree Number {}", degreeCounter);
            InstanceIdentifier<Degree> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Degree.class, new DegreeKey(Uint16.valueOf(degreeCounter)));
            Optional<Degree> ordmDegreeObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.OPERATIONAL, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmDegreeObject.isPresent()) {
                conPortMap.put(degreeCounter, new ArrayList<>(ordmDegreeObject.get()
                        .nonnullConnectionPorts().values()));
            }
        }
        LOG.info("Device {} has {} degree", deviceId, conPortMap.size());
        return conPortMap;
    }

    private Map<String, String> getEthInterfaceList(String nodeId) {
        LOG.info("It is calling get ethernet interface");
        Map<String, String> cpToInterfaceMap = new HashMap<>();
        InstanceIdentifier<Protocols> protocoliid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(Protocols.class);
        Optional<Protocols> protocolObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, protocoliid, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (protocolObject.isPresent() && protocolObject.get().augmentation(Protocols1.class).getLldp() != null) {
            Lldp lldp = protocolObject.get().augmentation(Protocols1.class).getLldp();
            for (PortConfig portConfig : lldp.nonnullPortConfig().values()) {
                if (portConfig.getAdminStatus().equals(PortConfig.AdminStatus.Txandrx)) {
                    InstanceIdentifier<Interface> interfaceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(Interface.class, new InterfaceKey(portConfig.getIfName()));
                    Optional<Interface> interfaceObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, interfaceIID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    if (interfaceObject.isPresent() && (interfaceObject.get().getSupportingCircuitPackName() != null)) {
                        String supportingCircuitPackName = interfaceObject.get().getSupportingCircuitPackName();
                        cpToInterfaceMap.put(supportingCircuitPackName, portConfig.getIfName());
                        InstanceIdentifier<CircuitPacks> circuitPacksIID = InstanceIdentifier
                            .create(OrgOpenroadmDevice.class)
                            .child(CircuitPacks.class, new CircuitPacksKey(supportingCircuitPackName));
                        Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(
                            nodeId, LogicalDatastoreType.OPERATIONAL, circuitPacksIID, Timeouts.DEVICE_READ_TIMEOUT,
                            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                        if (circuitPackObject.isPresent() && (circuitPackObject.get().getParentCircuitPack() != null)) {
                            cpToInterfaceMap.put(circuitPackObject.get().getParentCircuitPack().getCircuitPackName(),
                                portConfig.getIfName());
                        }
                    }
                }
            }
        } else {
            LOG.warn("Couldnt find port config under LLDP for Node : {}", nodeId);
        }
        LOG.info("Processiong is done.. now returning..");
        return cpToInterfaceMap;
    }

    private List<CpToDegree> getCpToDegreeList(List<Degree> degrees, String nodeId,
        Map<String, String> interfaceList) {
        List<CpToDegree> cpToDegreeList = new ArrayList<>();
        for (Degree degree : degrees) {
            if (degree.getCircuitPacks() != null) {
                LOG.info("Inside CP to degree list");
                cpToDegreeList.addAll(degree.nonnullCircuitPacks().values().stream()
                    .map(cp -> createCpToDegreeObject(cp.getCircuitPackName(),
                        degree.getDegreeNumber().toString(), nodeId, interfaceList))
                    .collect(Collectors.toList()));
            }
        }
        return cpToDegreeList;
    }

    private boolean postPortMapping(String nodeId, NodeInfo nodeInfo, List<Mapping> portMapList,
        List<CpToDegree> cp2DegreeList) {
        NodesBuilder nodesBldr = new NodesBuilder();
        nodesBldr.withKey(new NodesKey(nodeId)).setNodeId(nodeId);
        if (nodeInfo != null) {
            nodesBldr.setNodeInfo(nodeInfo);
        }
        if (portMapList != null) {
            Map<MappingKey, Mapping> mappingMap = new HashMap<>();
            for (Mapping mapping: portMapList) {
                if (mapping != null) {
                    mappingMap.put(mapping.key(), mapping);
                }
            }
            nodesBldr.setMapping(mappingMap);
        }
        if (cp2DegreeList != null) {
            Map<CpToDegreeKey, CpToDegree> cpToDegreeMap = new HashMap<>();
            for (CpToDegree cp2Degree: cp2DegreeList) {
                if (cp2Degree != null) {
                    cpToDegreeMap.put(cp2Degree.key(), cp2Degree);
                }
            }
            nodesBldr.setCpToDegree(cpToDegreeMap);
        }

        Map<NodesKey,Nodes> nodesList = new HashMap<>();
        Nodes nodes = nodesBldr.build();
        nodesList.put(nodes.key(),nodes);

        NetworkBuilder nwBldr = new NetworkBuilder();
        nwBldr.setNodes(nodesList);

        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Network> nodesIID = InstanceIdentifier.builder(Network.class).build();
        Network network = nwBldr.build();
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

    private CpToDegree createCpToDegreeObject(String circuitPackName, String degreeNumber, String nodeId,
        Map<String, String> interfaceList) {
        String interfaceName = null;
        if (interfaceList.get(circuitPackName) != null) {
            interfaceName = interfaceList.get(circuitPackName);
        }
        return new CpToDegreeBuilder().withKey(new CpToDegreeKey(circuitPackName)).setCircuitPackName(circuitPackName)
            .setDegreeNumber(Uint32.valueOf(degreeNumber)).setInterfaceName(interfaceName).build();
    }

    private Mapping createMappingObject(String nodeId, Ports port, String circuitPackName,
        String logicalConnectionPoint) {
        MappingBuilder mpBldr = new MappingBuilder();
        mpBldr.withKey(new MappingKey(logicalConnectionPoint)).setLogicalConnectionPoint(logicalConnectionPoint)
            .setSupportingCircuitPackName(circuitPackName).setSupportingPort(port.getPortName())
            .setPortDirection(port.getPortDirection().getName());

        // Get OMS and OTS interface provisioned on the TTP's
        if (logicalConnectionPoint.contains(StringConstants.TTP_TOKEN) && (port.getInterfaces() != null)) {
            for (Interfaces interfaces : port.getInterfaces()) {
                try {
                    Optional<Interface> openRoadmInterface = this.openRoadmInterfaces.getInterface(nodeId,
                        interfaces.getInterfaceName());
                    if (openRoadmInterface.isPresent()) {
                        Class<? extends InterfaceType> interfaceType
                            = (Class<? extends InterfaceType>) openRoadmInterface.get().getType();
                        // Check if interface type is OMS or OTS
                        if (interfaceType.equals(OpenROADMOpticalMultiplex.class)) {
                            mpBldr.setSupportingOms(interfaces.getInterfaceName());
                        }
                        if (interfaceType.equals(OpticalTransport.class)) {
                            mpBldr.setSupportingOts(interfaces.getInterfaceName());
                        }
                    } else {
                        LOG.warn("Interface {} from node {} was null!", interfaces.getInterfaceName(), nodeId);
                    }
                } catch (OpenRoadmInterfaceException ex) {
                    LOG.warn("Error while getting interface {} from node {}!", interfaces.getInterfaceName(), nodeId,
                        ex);
                }
            }
        }
        return mpBldr.build();
    }

    private Mapping createXpdrMappingObject(String nodeId, Ports port, String circuitPackName,
        String logicalConnectionPoint, String partnerLcp, Mapping mapping, String assoLcp) {
        MappingBuilder mpBldr;

        if (mapping != null && assoLcp != null) {
            // update existing mapping
            mpBldr = new MappingBuilder(mapping);
            mpBldr.setConnectionMapLcp(assoLcp);
        } else {
            // create a new mapping
            mpBldr = new MappingBuilder();
            String nodeIdLcp = nodeId + "-" + logicalConnectionPoint;
            mpBldr.withKey(new MappingKey(logicalConnectionPoint))
                .setLogicalConnectionPoint(logicalConnectionPoint)
                .setSupportingCircuitPackName(circuitPackName)
                .setSupportingPort(port.getPortName())
                .setPortDirection(port.getPortDirection().getName())
                .setLcpHashVal(fnv(nodeIdLcp));
            if (port.getPortQual() != null) {
                mpBldr.setPortQual(port.getPortQual().getName());
            }
            if (partnerLcp != null) {
                mpBldr.setPartnerLcp(partnerLcp);
            }
        }
        return mpBldr.build();
    }

    // some LOG messages are too long
    @SuppressWarnings("checkstyle:linelength")
    @SuppressFBWarnings("DM_CONVERT_CASE")
    private boolean createTtpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for degree TTP's
        List<Degree> degrees = getDegrees(nodeId, deviceInfo);
        Map<String, String> interfaceList = getEthInterfaceList(nodeId);
        List<CpToDegree> cpToDegreeList = getCpToDegreeList(degrees, nodeId, interfaceList);
        LOG.info("Map looks like this {}", interfaceList);
        postPortMapping(nodeId, null, null, cpToDegreeList);

        Map<Integer, List<ConnectionPorts>> connectionPortMap = getPerDegreePorts(nodeId, deviceInfo);
        for (Entry<Integer, List<ConnectionPorts>> cpMapEntry : connectionPortMap.entrySet()) {
            switch (connectionPortMap.get(cpMapEntry.getKey()).size()) {
                case 1:
                    // port is bidirectional
                    InstanceIdentifier<Ports> portID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class, new CircuitPacksKey(connectionPortMap.get(cpMapEntry.getKey()).get(0)
                            .getCircuitPackName()))
                        .child(Ports.class, new PortsKey(connectionPortMap.get(cpMapEntry.getKey()).get(0)
                            .getPortName().toString()));
                    LOG.info("Fetching connection-port {} at circuit pack {}", connectionPortMap
                        .get(cpMapEntry.getKey()).get(0).getPortName().toString(), connectionPortMap
                        .get(cpMapEntry.getKey()).get(0).getCircuitPackName());
                    Optional<Ports> portObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, portID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    if (portObject.isPresent()) {
                        Ports port = portObject.get();
                        if (port.getPortQual() == null) {
                            continue;
                        } else if (Port.PortQual.RoadmExternal.getIntValue() == port.getPortQual().getIntValue()
                            && Direction.Bidirectional.getIntValue() == port.getPortDirection().getIntValue()) {
                            String logicalConnectionPoint = new StringBuilder("DEG").append(cpMapEntry.getKey())
                                .append("-TTP-TXRX").toString();
                            LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId,
                                connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(),
                                port.getPortName(), logicalConnectionPoint);
                            portMapList.add(createMappingObject(nodeId, port,
                                connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(),
                                logicalConnectionPoint));
                        } else {
                            LOG.error(
                                "Impossible to create logical connection point for port {} of {} on node {} - Error in configuration with port-qual or port-direction",
                                port.getPortName(), connectionPortMap
                                .get(cpMapEntry.getKey()).get(0).getCircuitPackName(), nodeId);
                        }
                    } else {
                        LOG.error("No port {} on circuit pack {} for node {}",
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName().toString(),
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(), nodeId);
                        return false;
                    }
                    break;
                case 2:
                    // ports are unidirectionals
                    String cp1Name = connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName();
                    String cp2Name = connectionPortMap.get(cpMapEntry.getKey()).get(1).getCircuitPackName();
                    InstanceIdentifier<Ports> port1ID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class, new CircuitPacksKey(cp1Name))
                        .child(Ports.class, new PortsKey(connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName()
                            .toString()));
                    LOG.info("Fetching connection-port {} at circuit pack {}", connectionPortMap
                        .get(cpMapEntry.getKey()).get(0).getPortName().toString(), cp1Name);
                    Optional<Ports> port1Object = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, port1ID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    InstanceIdentifier<Ports> port2ID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class, new CircuitPacksKey(cp2Name))
                        .child(Ports.class, new PortsKey(connectionPortMap.get(cpMapEntry.getKey()).get(1).getPortName()
                            .toString()));
                    LOG.info("Fetching connection-port {} at circuit pack {}",
                        connectionPortMap.get(cpMapEntry.getKey()).get(1).getPortName().toString(), cp2Name);
                    Optional<Ports> port2Object = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, port2ID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    if (port1Object.isPresent() && port2Object.isPresent()) {
                        Ports port1 = port1Object.get();
                        Ports port2 = port2Object.get();
                        if (port1.getPortQual() == null || port2.getPortQual() == null) {
                            continue;
                        } else if ((Port.PortQual.RoadmExternal.getIntValue() == port1.getPortQual().getIntValue()
                                && Port.PortQual.RoadmExternal.getIntValue() == port2.getPortQual().getIntValue()
                                && Direction.Rx.getIntValue() == port1.getPortDirection().getIntValue()
                                && Direction.Tx.getIntValue() == port2.getPortDirection().getIntValue()
                                && port1.getPartnerPort() != null && port2.getPartnerPort() != null
                                && port1.getPartnerPort().getCircuitPackName().equals(cp2Name)
                                && port1.getPartnerPort().getPortName().equals(port2.getPortName())
                                && port2.getPartnerPort().getCircuitPackName().equals(cp1Name)
                                && port2.getPartnerPort().getPortName().equals(port1.getPortName()))
                                ||
                                (Port.PortQual.RoadmExternal.getIntValue() == port1.getPortQual().getIntValue()
                                && Port.PortQual.RoadmExternal.getIntValue() == port2.getPortQual().getIntValue()
                                && Direction.Rx.getIntValue() == port2.getPortDirection().getIntValue()
                                && Direction.Tx.getIntValue() == port1.getPortDirection().getIntValue()
                                && port1.getPartnerPort() != null && port2.getPartnerPort() != null
                                && port1.getPartnerPort().getCircuitPackName().equals(cp2Name)
                                && port1.getPartnerPort().getPortName().equals(port2.getPortName())
                                && port2.getPartnerPort().getCircuitPackName().equals(cp1Name)
                                && port2.getPartnerPort().getPortName().equals(port1.getPortName()))) {
                            String logicalConnectionPoint1 = new StringBuilder("DEG").append(cpMapEntry.getKey())
                                .append("-TTP-").append(port1.getPortDirection().getName().toUpperCase()).toString();
                            LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId,
                                connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(),
                                port1.getPortName(), logicalConnectionPoint1);
                            portMapList.add(createMappingObject(nodeId, port1, connectionPortMap
                                .get(cpMapEntry.getKey()).get(0).getCircuitPackName(), logicalConnectionPoint1));
                            String logicalConnectionPoint2 = new StringBuilder("DEG").append(cpMapEntry.getKey())
                                .append("-TTP-").append(port2.getPortDirection().getName().toUpperCase()).toString();
                            LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId,
                                connectionPortMap.get(cpMapEntry.getKey()).get(1).getCircuitPackName(),
                                port2.getPortName(), logicalConnectionPoint2);
                            portMapList.add(createMappingObject(nodeId, port2, connectionPortMap
                                .get(cpMapEntry.getKey()).get(1).getCircuitPackName(), logicalConnectionPoint2));
                        } else {
                            LOG.error(
                                "Impossible to create logical connection point for port {} or port {} on node {} - Error in configuration with port-qual, port-direction or partner-port configuration",
                                port1.getPortName(), port2.getPortName(), nodeId);
                        }
                    } else {
                        LOG.error("No port {} on circuit pack {} for node {}",
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getPortName().toString(),
                            connectionPortMap.get(cpMapEntry.getKey()).get(0).getCircuitPackName(), nodeId);
                        return false;
                    }

                    break;
                default:
                    LOG.error("Number of connection port for DEG{} on {} is incorrect", cpMapEntry.getKey(), nodeId);
                    continue;
            }
        }
        return true;
    }

    private NodeInfo createNodeInfo(Info deviceInfo) {
        NodeInfoBuilder nodeInfoBldr = new NodeInfoBuilder();
        if (deviceInfo.getNodeType() != null) {
            nodeInfoBldr.setOpenroadmVersion(OpenroadmVersion._121);
            if (deviceInfo.getNodeType().getIntValue() == 1) {
                nodeInfoBldr.setNodeType(NodeTypes.Rdm);
            } else if (deviceInfo.getNodeType().getIntValue() == 2) {
                nodeInfoBldr.setNodeType(NodeTypes.Xpdr);
            } else {
                LOG.error("Error with node-type of {}", deviceInfo.getNodeId());
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
        } else {
         // TODO make mandatory in yang
            LOG.error("Node type field is missing");
            return null;
        }
        return nodeInfoBldr.build();
    }
}
