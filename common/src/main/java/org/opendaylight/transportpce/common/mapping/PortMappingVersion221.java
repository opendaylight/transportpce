/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.switching.pool.lcp.SwitchingPoolLcp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.switching.pool.lcp.SwitchingPoolLcpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.switching.pool.lcp.SwitchingPoolLcpKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.switching.pool.lcp.switching.pool.lcp.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.switching.pool.lcp.switching.pool.lcp.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.switching.pool.lcp.switching.pool.lcp.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PortQual;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Xponder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.xponder.XpdrPort;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.Lldp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.PortConfig;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181019.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev191129.SwitchingPoolTypes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: many common pieces of code between PortMapping Versions 121 and 221 and 710
// some mutualization would be helpful
public class PortMappingVersion221 {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingVersion221.class);
    private static final Map<Direction, String> SUFFIX;
    private static final Set<Integer> TXRX_SET = Set.of(Direction.Tx.getIntValue(), Direction.Rx.getIntValue());

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmInterfaces openRoadmInterfaces;

    static {
        SUFFIX =  Map.of(
            Direction.Tx, "TX",
            Direction.Rx, "RX",
            Direction.Bidirectional, "TXRX");
    }

    public PortMappingVersion221(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
            OpenRoadmInterfaces openRoadmInterfaces) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public boolean createMappingData(String nodeId) {
        LOG.info(PortMappingUtils.CREATE_MAPPING_DATA_LOGMSG, nodeId, "2.2.1");
        List<Mapping> portMapList = new ArrayList<>();
        Map<McCapabilitiesKey, McCapabilities> mcCapabilities = new HashMap<>();
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfoOptional = this.deviceTransactionManager.getDataFromDevice(
                nodeId, LogicalDatastoreType.OPERATIONAL, infoIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (deviceInfoOptional.isEmpty()) {
            LOG.warn(PortMappingUtils.DEVICE_HAS_LOGMSG, nodeId, "no info", "subtree");
            return false;
        }
        Info deviceInfo = deviceInfoOptional.get();
        NodeInfo nodeInfo = createNodeInfo(deviceInfo);
        if (nodeInfo == null) {
            return false;
        }
        postPortMapping(nodeId, nodeInfo, null, null, null, null);

        switch (deviceInfo.getNodeType()) {

            case Rdm:
                // Get TTP port mapping
                if (!createTtpPortMapping(nodeId, deviceInfo, portMapList)) {
                    // return false if mapping creation for TTP's failed
                    LOG.warn(PortMappingUtils.UNABLE_MAPPING_LOGMSG, nodeId, PortMappingUtils.CREATE, "TTP's");
                    return false;
                }

                // Get PP port mapping
                if (!createPpPortMapping(nodeId, deviceInfo, portMapList)) {
                    // return false if mapping creation for PP's failed
                    LOG.warn(PortMappingUtils.UNABLE_MAPPING_LOGMSG, nodeId, PortMappingUtils.CREATE, "PP's");
                    return false;
                }
                // Get MC capabilities
                if (!createMcCapabilitiesList(nodeId, deviceInfo, mcCapabilities)) {
                    // return false if MC capabilites failed
                    LOG.warn(PortMappingUtils.UNABLE_MC_CAPA_LOGMSG, nodeId);
                    return false;
                }
                break;
            case Xpdr:
                if (!createXpdrPortMapping(nodeId, portMapList)) {
                    LOG.warn(PortMappingUtils.UNABLE_MAPPING_LOGMSG, nodeId, PortMappingUtils.CREATE, "Xponder");
                    return false;
                }
                break;
            default:
                LOG.error(PortMappingUtils.UNABLE_MAPPING_LOGMSG,
                    nodeId, PortMappingUtils.CREATE, deviceInfo.getNodeType() + " - unknown nodetype");
                break;

        }
        return postPortMapping(nodeId, nodeInfo, portMapList, null, null, mcCapabilities);
    }

    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        if (nodeId == null) {
            LOG.error(PortMappingUtils.UNABLE_MAPPING_LOGMSG, "node id null" , PortMappingUtils.UPDATE, "a null value");
            return false;
        }
        if (oldMapping == null) {
            LOG.error(PortMappingUtils.UNABLE_MAPPING_LOGMSG, nodeId, PortMappingUtils.UPDATE, "a null value");
            return false;
        }
        InstanceIdentifier<Ports> portId = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(oldMapping.getSupportingCircuitPackName()))
                .child(Ports.class, new PortsKey(oldMapping.getSupportingPort()));
        try {
            Ports port = deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL,
                portId, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT).get();
            Mapping newMapping = updateMappingObject(nodeId, port, oldMapping);
            LOG.debug(PortMappingUtils.UPDATE_MAPPING_LOGMSG,
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
            LOG.error(PortMappingUtils.UNABLE_MAPPING_LOGMSG,
                nodeId, PortMappingUtils.UPDATE, oldMapping.getLogicalConnectionPoint(), e);
            return false;
        }
    }

    private boolean createXpdrPortMapping(String nodeId, List<Mapping> portMapList) {
        // Creating for Xponder Line and Client Ports
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, deviceIID,
            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (deviceObject.isEmpty()) {
            LOG.error(PortMappingUtils.CANNOT_GET_DEV_CONF_LOGMSG, nodeId);
            return false;
        }
        OrgOpenroadmDevice device = deviceObject.get();
        if (device.getCircuitPacks() == null) {
            LOG.warn(PortMappingUtils.MISSING_CP_LOGMSG, nodeId, PortMappingUtils.FOUND);
            return false;
        }

        Map<String, String> lcpMap = new HashMap<>();
        Map<String, Mapping> mappingMap = new HashMap<>();
        List<CircuitPacks> circuitPackList = new ArrayList<>(device.nonnullCircuitPacks().values());
        circuitPackList.sort(Comparator.comparing(CircuitPack::getCircuitPackName));
        if (device.getXponder() == null) {
            LOG.warn(PortMappingUtils.XPDR_LIST_IN_CONF_LOGMSG, nodeId, "not found");
            // Variables to keep track of number of line ports and client ports
            int line = 1;
            int client = 1;
            // TODO the treatment here inside the 2 nested for-loop is very similar to the one
            //     when device.getXponder() != null. Some code mutualization must be considered.
            for (CircuitPacks cp : circuitPackList) {
                String circuitPackName = cp.getCircuitPackName();
                if (cp.getPorts() == null) {
                    LOG.warn(PortMappingUtils.NO_PORT_ON_CP_LOGMSG, nodeId, PortMappingUtils.FOUND, circuitPackName);
                    continue;
                }
                List<Ports> portList = new ArrayList<>(cp.nonnullPorts().values());
                portList.sort(Comparator.comparing(Ports::getPortName));
                for (Ports port : portList) {
                    int[] counters = fillXpdrLcpsMaps(line, client, nodeId,
                        1, null, circuitPackName, port,
                        circuitPackList, lcpMap, mappingMap);
                    line = counters[0];
                    client = counters[1];
                }
            }
        } else {
            LOG.info(PortMappingUtils.XPDR_LIST_IN_CONF_LOGMSG, nodeId, PortMappingUtils.FOUND);
            for (Xponder xponder : deviceObject.get().nonnullXponder().values()) {
                // Variables to keep track of number of line ports and client ports
                int line = 1;
                int client = 1;
                Integer xponderNb = xponder.getXpdrNumber().toJava();
                XpdrNodeTypes xponderType = xponder.getXpdrType();
                for (XpdrPort xpdrPort : xponder.nonnullXpdrPort().values().stream()
                        .sorted((xp1, xp2) -> xp1.getIndex().compareTo(xp2.getIndex())).collect(Collectors.toList())) {
                    String circuitPackName = xpdrPort.getCircuitPackName();
                    String portName = xpdrPort.getPortName();
                    // If there xponder-subtree has missing circuit-packs or ports,
                    // This gives a null-pointer expection,
                    Optional<CircuitPacks> cpList = device.nonnullCircuitPacks().values().stream()
                            .filter(cp -> cp.getCircuitPackName().equals(circuitPackName)).findFirst();
                    if (cpList.isEmpty()) {
                        LOG.warn(PortMappingUtils.MISSING_CP_LOGMSG + PortMappingUtils.PORTMAPPING_IGNORE_LOGMSG,
                            nodeId, circuitPackName);
                        continue;
                    }
                    Optional<Ports> portsList = cpList.get().nonnullPorts().values().stream()
                            .filter(p -> p.getPortName().equals(portName)).findFirst();
                    if (portsList.isEmpty()) {
                        LOG.warn(PortMappingUtils.NO_ASSOC_FOUND_LOGMSG + PortMappingUtils.PORTMAPPING_IGNORE_LOGMSG,
                            nodeId, portName, circuitPackName, "in the device");
                        continue;
                    }
                    int[] counters = fillXpdrLcpsMaps(line, client, nodeId,
                        xponderNb, xponderType, circuitPackName, portsList.get(),
                        circuitPackList, lcpMap, mappingMap);
                    line = counters[0];
                    client = counters[1];
                }
            }
        }

        if (device.getConnectionMap() == null) {
            LOG.warn(PortMappingUtils.NO_CONMAP_LOGMSG, nodeId);
        } else {
            for (ConnectionMap cm : deviceObject.get().nonnullConnectionMap().values()) {
                String skey = cm.getSource().getCircuitPackName() + "+" + cm.getSource().getPortName();
                Destination destination0 = cm.nonnullDestination().values().iterator().next();
                String dkey = destination0.getCircuitPackName() + "+" + destination0.getPortName();
                if (!lcpMap.containsKey(skey)) {
                    LOG.error(PortMappingUtils.CONMAP_ISSUE_LOGMSG, nodeId, skey, dkey);
                    continue;
                }
                String slcp = lcpMap.get(skey);
                Mapping mapping = mappingMap.get(slcp);
                mappingMap.remove(slcp);
                portMapList.add(createXpdrMappingObject(nodeId, null, null, null, null, mapping,
                        //dlcp
                        lcpMap.containsKey(dkey) ? lcpMap.get(dkey) : null,
                        null));
            }
        }

        if (device.getOduSwitchingPools() != null) {
            List<SwitchingPoolLcp> switchingPoolList = new ArrayList<>();
            for (OduSwitchingPools odp : device.nonnullOduSwitchingPools().values()) {
                Map<NonBlockingListKey,NonBlockingList> nbMap = new HashMap<>();
                for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org
                    .openroadm.device.odu.switching.pools.NonBlockingList nbl : odp.nonnullNonBlockingList().values()) {
                    if (nbl.getPortList() == null) {
                        continue;
                    }
                    List<String> lcpList = new ArrayList<>();
                    for (PortList item : nbl.nonnullPortList().values()) {
                        String key = item.getCircuitPackName() + "+" + item.getPortName();
                        if (!lcpMap.containsKey(key)) {
                            LOG.error(PortMappingUtils.NO_ASSOC_FOUND_LOGMSG,
                                nodeId, item.getPortName(), item.getCircuitPackName(), "to a logical connection point");
                            continue;
                        }
                        lcpList.add(lcpMap.get(key));
                    }
                    NonBlockingList nonBlockingList = new NonBlockingListBuilder()
                        .setNblNumber(nbl.getNblNumber())
                        .setInterconnectBandwidth(nbl.getInterconnectBandwidth())
                        .setInterconnectBandwidthUnit(nbl.getInterconnectBandwidthUnit())
                        .setLcpList(lcpList)
                        .build();
                    nbMap.put(nonBlockingList.key(), nonBlockingList);
                }
                SwitchingPoolLcp splBldr = new SwitchingPoolLcpBuilder()
                    .setSwitchingPoolNumber(odp.getSwitchingPoolNumber())
                    .setSwitchingPoolType(SwitchingPoolTypes.forValue(odp.getSwitchingPoolType().getIntValue()))
                    .setNonBlockingList(nbMap)
                    .build();
                switchingPoolList.add(splBldr);
            }
            postPortMapping(nodeId, null, null, null, switchingPoolList, null);
        }

        mappingMap.forEach((k,v) -> portMapList.add(v));
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
        return checkPartnerPortNoDir(circuitPackName, port1, port2)
            && Set.of(port1.getPortDirection().getIntValue(), port2.getPortDirection().getIntValue())
                .equals(TXRX_SET);
    }


    private HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg
            .CircuitPacks>> getSrgCps(String deviceId, Info ordmInfo) {
        HashMap<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg
            .CircuitPacks>> cpPerSrg = new HashMap<>();
        // Get value for max Srg from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        Integer maxSrg = ordmInfo.getMaxSrgs() == null ? 20 : ordmInfo.getMaxSrgs().toJava();
        for (int srgCounter = 1; srgCounter <= maxSrg; srgCounter++) {
            List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacks> srgCps
                = new ArrayList<>();
            LOG.debug(PortMappingUtils.GETTING_CP_LOGMSG, deviceId, srgCounter);
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
        LOG.info(PortMappingUtils.DEVICE_HAS_LOGMSG, deviceId, cpPerSrg.size(), "SRG");
        return cpPerSrg;
    }

    private boolean createPpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for SRG's PP
        for (Entry<Integer, List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacks>>
                srgCpEntry : getSrgCps(nodeId, deviceInfo).entrySet()) {
            List<String> keys = new ArrayList<>();
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacks
                    cp : srgCpEntry.getValue()) {
                String circuitPackName = cp.getCircuitPackName();
                List<Ports> portList = getPortList(circuitPackName, nodeId);
                Collections.sort(portList, new SortPort221ByName());
                int portIndex = 1;
                for (Ports port : portList) {
                    if (!checkPortQual(port, circuitPackName, nodeId)) {
                        continue;
                    }
                    String currentKey = circuitPackName + "-" + port.getPortName();
                    if (keys.contains(currentKey)) {
                        LOG.debug(PortMappingUtils.PORT_ALREADY_HANDLED_LOGMSG + PortMappingUtils.CANNOT_AS_LCP_LOGMSG,
                            nodeId, port.getPortName(), circuitPackName);
                        continue;
                    }
                    switch (port.getPortDirection()) {
                        case Bidirectional:
                            String lcp = createLogicalConnectionPort(port, srgCpEntry.getKey(), portIndex);
                            LOG.info(PortMappingUtils.ASSOCIATED_LCP_LOGMSG,
                                nodeId, port.getPortName(), circuitPackName, lcp);
                            portMapList.add(createMappingObject(nodeId, port, circuitPackName, lcp));
                            portIndex++;
                            keys.add(currentKey);
                            break;
                        case Rx:
                        case Tx:
                            Ports port2 = getPartnerPort(port, circuitPackName, nodeId);
                            if (port2 == null) {
                                continue;
                            }
                            String lcp1 = createLogicalConnectionPort(port, srgCpEntry.getKey(), portIndex);
                            LOG.info(PortMappingUtils.ASSOCIATED_LCP_LOGMSG,
                                nodeId, port.getPortName(), circuitPackName, lcp1);
                            String lcp2 = createLogicalConnectionPort(port2, srgCpEntry.getKey(),portIndex);
                            LOG.info(PortMappingUtils.ASSOCIATED_LCP_LOGMSG,
                                    nodeId, port2.getPortName(), circuitPackName, lcp2);
                            portMapList.add(createMappingObject(nodeId, port, circuitPackName, lcp1));
                            portMapList.add(
                                createMappingObject(nodeId ,port2, port.getPartnerPort().getCircuitPackName(), lcp2));
                            portIndex++;
                            keys.add(currentKey);
                            keys.add(port.getPartnerPort().getCircuitPackName() + "-" + port2.getPortName());
                            break;
                        default:
                            LOG.error(PortMappingUtils.UNSUPPORTED_DIR_LOGMSG + PortMappingUtils.CANNOT_AS_LCP_LOGMSG,
                                nodeId, port.getPortName(), circuitPackName, port.getPortDirection());
                    }
                }
            }
        }
        return true;
    }

    private Ports getPartnerPort(Ports port, String circuitPackName, String nodeId) {
        if (!checkPartnerPortNotNull(port)) {
            LOG.info(PortMappingUtils.NO_VALID_PARTNERPORT_LOGMSG + PortMappingUtils.CANNOT_AS_LCP_LOGMSG,
                nodeId, port.getPortName(), circuitPackName);
            return null;
        }
        InstanceIdentifier<Ports> port2ID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(port.getPartnerPort().getCircuitPackName()))
            .child(Ports.class, new PortsKey(port.getPartnerPort().getPortName()));
        Optional<Ports> port2Object = this.deviceTransactionManager
            .getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, port2ID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (port2Object.isEmpty()
                || port2Object.get().getPortQual().getIntValue() != PortQual.RoadmExternal.getIntValue()) {
            LOG.error(PortMappingUtils.NOT_CORRECT_PARTNERPORT_LOGMSG + PortMappingUtils.PARTNERPORT_GET_ERROR_LOGMSG,
                nodeId, port.getPartnerPort().getPortName(), port.getPartnerPort().getCircuitPackName(),
                port.getPortName(), circuitPackName);
            return null;
        }
        Ports port2 = port2Object.get();
        if (!checkPartnerPort(circuitPackName, port, port2)) {
            LOG.error(PortMappingUtils.NOT_CORRECT_PARTNERPORT_LOGMSG + PortMappingUtils.PARTNERPORT_CONF_ERROR_LOGMSG,
                nodeId, port2.getPortName(), port.getPartnerPort().getCircuitPackName(),
                port.getPortName(), circuitPackName);
            //portIndex++;
            //TODO check if we really needed to increase portIndex
            //     if yes this block should not be in getPartnerPort
            return null;
        }
        return port2;
    }

    private List<Ports> getPortList(String circuitPackName, String nodeId) {
        InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName));
        Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
             LogicalDatastoreType.OPERATIONAL, cpIID,
             Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (circuitPackObject.isEmpty()) {
            LOG.warn(PortMappingUtils.MISSING_CP_LOGMSG + PortMappingUtils.PORTMAPPING_IGNORE_LOGMSG,
                nodeId, circuitPackName);
            return new ArrayList<>();
        }
        if (circuitPackObject.get().getPorts() == null) {
            LOG.warn(PortMappingUtils.NO_PORT_ON_CP_LOGMSG, nodeId, PortMappingUtils.FOUND, circuitPackName);
            return new ArrayList<>();
        }
        return new ArrayList<>(circuitPackObject.get().nonnullPorts().values());
    }

    private String createLogicalConnectionPort(Ports port, int index, int portIndex) {
        if (SUFFIX.containsKey(port.getPortDirection())) {
            return String.join("-", "SRG" + index, "PP" + portIndex, SUFFIX.get(port.getPortDirection()));
        }
        LOG.error(PortMappingUtils.UNSUPPORTED_DIR_LOGMSG,
            "createLogicalConnectionPort", port, "SRG" + index + "-PP" + portIndex, port.getPortDirection());
        return null;
    }

    private Map<Integer, Degree> getDegreesMap(String deviceId, Info ordmInfo) {
        Map<Integer, Degree> degrees = new HashMap<>();

        // Get value for max degree from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        Integer maxDegree = ordmInfo.getMaxDegrees() == null ? 20 : ordmInfo.getMaxDegrees().toJava();

        for (int degreeCounter = 1; degreeCounter <= maxDegree; degreeCounter++) {
            LOG.debug(PortMappingUtils.GETTING_CONPORT_LOGMSG, deviceId, degreeCounter);
            InstanceIdentifier<Degree> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Degree.class, new DegreeKey(Uint16.valueOf(degreeCounter)));
            Optional<Degree> ordmDegreeObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.OPERATIONAL, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmDegreeObject.isPresent()) {
                degrees.put(degreeCounter, ordmDegreeObject.get());
            }
        }
        LOG.info(PortMappingUtils.DEVICE_HAS_LOGMSG,
            deviceId, degrees.size(), degrees.size() <= 1 ? "degree" : "degrees");
        return degrees;
    }

    private Map<Integer, List<ConnectionPorts>> getPerDegreePorts(String deviceId, Info ordmInfo) {
        Map<Integer, List<ConnectionPorts>> conPortMap = new HashMap<>();
        getDegreesMap(deviceId, ordmInfo).forEach(
            (index, degree) -> conPortMap.put(index, new ArrayList<>(degree.nonnullConnectionPorts().values())));
        return conPortMap;
    }

    private List<SharedRiskGroup> getSrgs(String deviceId, Info ordmInfo) {
        List<SharedRiskGroup> srgs = new ArrayList<>();

        // Get value for max Srg from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        Integer maxSrg = ordmInfo.getMaxSrgs() == null ? 20 : ordmInfo.getMaxSrgs().toJava();
        for (int srgCounter = 1; srgCounter <= maxSrg; srgCounter++) {
            InstanceIdentifier<SharedRiskGroup> srgIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(SharedRiskGroup.class, new SharedRiskGroupKey(Uint16.valueOf(srgCounter)));
            Optional<SharedRiskGroup> ordmSrgObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.OPERATIONAL, srgIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmSrgObject.isPresent()) {
                srgs.add(ordmSrgObject.get());

            }
        }
        return srgs;
    }

    private Map<String, String> getEthInterfaceList(String nodeId) {
        LOG.info(PortMappingUtils.GETTING_ETH_LIST_LOGMSG, nodeId);
        InstanceIdentifier<Protocols> protocoliid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(Protocols.class);
        Optional<Protocols> protocolObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, protocoliid, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (protocolObject.isEmpty() || protocolObject.get().augmentation(Protocols1.class).getLldp() == null) {
            LOG.warn(PortMappingUtils.PROCESSING_DONE_LOGMSG, nodeId, PortMappingUtils.CANNOT_GET_LLDP_CONF_LOGMSG);
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
            if (interfaceObject.isEmpty() || interfaceObject.get().getSupportingCircuitPackName() == null) {
                continue;
            }
            String supportingCircuitPackName = interfaceObject.get().getSupportingCircuitPackName();
            cpToInterfaceMap.put(supportingCircuitPackName, portConfig.getIfName());
            InstanceIdentifier<CircuitPacks> circuitPacksIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(supportingCircuitPackName));
            Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(
                nodeId, LogicalDatastoreType.OPERATIONAL, circuitPacksIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (circuitPackObject.isEmpty() || circuitPackObject.get().getParentCircuitPack() == null) {
                continue;
            }
            cpToInterfaceMap.put(circuitPackObject.get().getParentCircuitPack().getCircuitPackName(),
                portConfig.getIfName());
        }
        LOG.info(PortMappingUtils.PROCESSING_DONE_LOGMSG, nodeId, " - success");
        return cpToInterfaceMap;
    }

    private List<CpToDegree> getCpToDegreeList(Map<Integer, Degree> degrees, Map<String, String> interfaceList) {
        List<CpToDegree> cpToDegreeList = new ArrayList<>();
        for (Degree degree : degrees.values()) {
            cpToDegreeList.addAll(degree.nonnullCircuitPacks().values().stream()
                .map(cp -> createCpToDegreeObject(cp.getCircuitPackName(),
                    degree.getDegreeNumber().toString(), interfaceList))
                .collect(Collectors.toList()));
        }
        return cpToDegreeList;
    }

    private Map<McCapabilitiesKey, McCapabilities> getMcCapabilities(Map<Integer, Degree> degrees,
            List<SharedRiskGroup> srgs, String nodeId) {
        LOG.info(PortMappingUtils.GETTING_MC_CAPA_LOGMSG , nodeId, "degrees");
        Map<McCapabilitiesKey, McCapabilities> mcCapabilities = degrees.values().stream()
            .map(degree -> createMcCapDegreeObject(degree, nodeId))
            .collect(Collectors.toMap(McCapabilities::key, mcc -> mcc));
        // Add the SRG mc-capabilities
        LOG.info(PortMappingUtils.GETTING_MC_CAPA_LOGMSG , nodeId, "SRGs");
        mcCapabilities.putAll(srgs.stream().map(srg -> createMcCapSrgObject(srg, nodeId))
            .collect(Collectors.toMap(McCapabilities::key, mcCapabilities2 -> mcCapabilities2)));
        return mcCapabilities;
    }

    private boolean postPortMapping(String nodeId, NodeInfo nodeInfo, List<Mapping> portMapList,
            List<CpToDegree> cp2DegreeList, List<SwitchingPoolLcp> splList,
            Map<McCapabilitiesKey, McCapabilities> mcCapabilities) {
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

        if (splList != null) {
            Map<SwitchingPoolLcpKey,SwitchingPoolLcp> splMap = new HashMap<>();
            // No element in the list below should be null at this stage
            for (SwitchingPoolLcp spl: splList) {
                splMap.put(spl.key(), spl);
            }
            nodesBldr.setSwitchingPoolLcp(splMap);
        }
        if (mcCapabilities != null) {
            nodesBldr.setMcCapabilities(mcCapabilities);
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
            LOG.warn(PortMappingUtils.PORTMAPPING_POST_FAIL_LOGMSG, nodeId, network, e);
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

    private McCapabilities createMcCapDegreeObject(Degree degree, String nodeId) {
        String mcNodeName =  PortMappingUtils.degreeTtpNodeName(degree.getDegreeNumber().toString(), null);
        McCapabilitiesBuilder mcCapabilitiesBuilder = new McCapabilitiesBuilder()
            .withKey(new McCapabilitiesKey(mcNodeName))
            .setMcNodeName(mcNodeName);
        if (degree.getMcCapabilities() == null) {
            LOG.warn(PortMappingUtils.NO_MC_LOGMSG, nodeId, "degree", degree.getDegreeNumber());
            mcCapabilitiesBuilder
                .setCenterFreqGranularity(FrequencyGHz.getDefaultInstance("50"))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("50"));
        } else {
            mcCapabilitiesBuilder
                .setCenterFreqGranularity(FrequencyGHz.getDefaultInstance(degree.getMcCapabilities()
                    .getCenterFreqGranularity().getValue().toString()))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance(degree.getMcCapabilities()
                    .getSlotWidthGranularity().getValue().toString()));
        }
        return mcCapabilitiesBuilder.build();
    }

    private McCapabilities createMcCapSrgObject(SharedRiskGroup srg, String nodeId) {
        String mcNodeName = "SRG" + srg.getSrgNumber().toString() + "-PP";
        McCapabilitiesBuilder mcCapabilitiesBuilder = new McCapabilitiesBuilder()
            .withKey(new McCapabilitiesKey(mcNodeName))
            .setMcNodeName(mcNodeName);
        if (srg.getMcCapabilities() == null) {
            LOG.warn(PortMappingUtils.NO_MC_LOGMSG, nodeId, "SRG", srg.getSrgNumber());
            mcCapabilitiesBuilder
                .setCenterFreqGranularity(FrequencyGHz.getDefaultInstance("50"))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("50"));
        } else {
            mcCapabilitiesBuilder
                .setCenterFreqGranularity(FrequencyGHz.getDefaultInstance(srg.getMcCapabilities()
                    .getCenterFreqGranularity().getValue().toString()))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance(srg.getMcCapabilities()
                    .getSlotWidthGranularity().getValue().toString()));
        }
        return mcCapabilitiesBuilder.build();
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

        if ((port.getInterfaces() == null)
            || (!logicalConnectionPoint.contains(StringConstants.TTP_TOKEN)
                && !logicalConnectionPoint.contains(StringConstants.NETWORK_TOKEN))) {
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
                if (openRoadmInterface.isEmpty()) {
                    LOG.warn(PortMappingUtils.INTF_ISSUE_LOGMSG,
                        nodeId, interfaces.getInterfaceName() + "- empty interface");
                    continue;
                }
                LOG.debug(PortMappingUtils.GOT_INTF_LOGMSG,
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
                if (interfaceType.equals(OtnOdu.class)) {
                    mpBldr.setSupportingOdu4(interfaces.getInterfaceName());
                }
            } catch (OpenRoadmInterfaceException ex) {
                LOG.warn(PortMappingUtils.INTF_ISSUE_LOGMSG, nodeId, interfaces.getInterfaceName(), ex);
            }
        }
        return mpBldr;
    }

    private Mapping createXpdrMappingObject(String nodeId, Ports port, String circuitPackName,
            String logicalConnectionPoint, String partnerLcp, Mapping mapping, String connectionMapLcp,
            XpdrNodeTypes xpdrNodeType) {
        if (mapping != null && connectionMapLcp != null) {
            // update existing mapping
            return new MappingBuilder(mapping).setConnectionMapLcp(connectionMapLcp).build();
        }
        return createNewXpdrMapping(nodeId, port, circuitPackName, logicalConnectionPoint, partnerLcp, xpdrNodeType);
    }

    private Mapping createNewXpdrMapping(String nodeId, Ports port, String circuitPackName,
            String logicalConnectionPoint, String partnerLcp, XpdrNodeTypes xpdrNodeType) {
        MappingBuilder mpBldr = new MappingBuilder()
                .withKey(new MappingKey(logicalConnectionPoint))
                .setLogicalConnectionPoint(logicalConnectionPoint)
                .setSupportingCircuitPackName(circuitPackName)
                .setSupportingPort(port.getPortName())
                .setPortDirection(port.getPortDirection().getName())
                .setLcpHashVal(PortMappingUtils.fnv1size64(nodeId + "-" + logicalConnectionPoint));
        if (port.getPortQual() != null) {
            mpBldr.setPortQual(port.getPortQual().getName());
        }
        if (xpdrNodeType != null) {
            mpBldr.setXponderType(
                org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes.forValue(
                    xpdrNodeType.getIntValue()));
        }
        if (partnerLcp != null) {
            mpBldr.setPartnerLcp(partnerLcp);
        }
        if (port.getSupportedInterfaceCapability() != null) {
            List<Class<? extends org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327
                .SupportedIfCapability>> supportedIntf = new ArrayList<>();
            for (Class<? extends SupportedIfCapability> sup: port.getSupportedInterfaceCapability()) {
                supportedIntf.add(MappingUtilsImpl.convertSupIfCapa(sup.getSimpleName()));
            }
            mpBldr.setSupportedInterfaceCapability(supportedIntf);
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
            LOG.warn(PortMappingUtils.NO_VALID_PARTNERPORT_LOGMSG, nodeId, port.getPortName(), circuitPackName);
            return null;
        }
        if (lcpMap.containsKey(circuitPackName + '+' + port.getPortName())) {
            return null;
        }
        Optional<CircuitPacks> cpOpt = circuitPackList.stream()
            .filter(
                cP -> cP.getCircuitPackName().equals(port.getPartnerPort().getCircuitPackName()))
            .findFirst();
        if (cpOpt.isEmpty()) {
            LOG.error(PortMappingUtils.MISSING_CP_LOGMSG, nodeId, port.getPartnerPort().getCircuitPackName());
            return null;
        }
        Optional<Ports> poOpt = cpOpt.get().nonnullPorts().values().stream()
            .filter(p -> p.getPortName().equals(port.getPartnerPort().getPortName()))
            .findFirst();
        if (poOpt.isEmpty()) {
            LOG.error(PortMappingUtils.NO_PORT_ON_CP_LOGMSG,
                nodeId, port.getPartnerPort().getPortName(), port.getPartnerPort().getCircuitPackName());
            return null;
        }
        Ports port2 = poOpt.get();
        circuitPackName2.append(cpOpt.get().getCircuitPackName());
        if (!checkPartnerPort(circuitPackName, port, port2)) {
            LOG.error(PortMappingUtils.NOT_CORRECT_PARTNERPORT_LOGMSG,
                nodeId, port2.getPortName(), circuitPackName2, port.getPortName(), circuitPackName);
            return null;
        }
        return port2;
    }


    private void putXpdrLcpsInMaps(int line, String nodeId,
            Integer xponderNb, XpdrNodeTypes xponderType,
            String circuitPackName, String circuitPackName2, Ports port, Ports port2,
            Map<String, String> lcpMap, Map<String, Mapping> mappingMap) {
        String lcp1 =
            PortMappingUtils.createXpdrLogicalConnectionPort(xponderNb, line, StringConstants.NETWORK_TOKEN);
        if (lcpMap.containsKey(lcp1)) {
            LOG.warn(PortMappingUtils.UNABLE_MAPPING_LOGMSG, nodeId, "add", lcp1 + " - already exist");
            return;
        }
        String lcp2 =
            PortMappingUtils.createXpdrLogicalConnectionPort(xponderNb, line + 1, StringConstants.NETWORK_TOKEN);
        if (lcpMap.containsKey(lcp2)) {
            LOG.warn(PortMappingUtils.UNABLE_MAPPING_LOGMSG, nodeId, "add", lcp2 + " - already exist");
            return;
        }
        lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp1);
        lcpMap.put(circuitPackName2 + '+' + port2.getPortName(), lcp2);
        mappingMap.put(lcp1,
                createXpdrMappingObject(nodeId, port, circuitPackName, lcp1, lcp2, null, null, xponderType));
        mappingMap.put(lcp2,
                createXpdrMappingObject(nodeId, port2, circuitPackName2, lcp2, lcp1, null, null, xponderType));
        return;
    }

    private int[] fillXpdrLcpsMaps(int line, int client, String nodeId,
            Integer xponderNb, XpdrNodeTypes xponderType,
            String circuitPackName,  Ports port,
            List<CircuitPacks> circuitPackList, Map<String, String> lcpMap, Map<String, Mapping> mappingMap) {
        if (port.getPortQual() == null) {
            LOG.warn(PortMappingUtils.PORTQUAL_LOGMSG, nodeId, port.getPortName(), circuitPackName, "not found");
            return new int[] {line, client};
        }

        switch (port.getPortQual()) {

            case XpdrClient:
            case SwitchClient:
                String lcp0 =
                    PortMappingUtils.createXpdrLogicalConnectionPort(xponderNb, client, StringConstants.CLIENT_TOKEN);
                lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp0);
                mappingMap.put(lcp0,
                    createXpdrMappingObject(nodeId, port, circuitPackName, lcp0, null, null, null, null));
                client++;
                break;

            case XpdrNetwork:
            case SwitchNetwork:
                line = fillXpdrNetworkLcpsMaps(line, nodeId,
                        xponderNb, xponderType,
                        circuitPackName,  port,
                        circuitPackList,  lcpMap, mappingMap);
                break;

            default:
                LOG.error(PortMappingUtils.PORTQUAL_LOGMSG,
                    nodeId, port.getPortName(), circuitPackName, port.getPortQual() + " not supported");
        }
        return new int[] {line, client};
    }

    private int fillXpdrNetworkLcpsMaps(int line, String nodeId,
            Integer xponderNb, XpdrNodeTypes xponderType,
            String circuitPackName,  Ports port,
            List<CircuitPacks> circuitPackList, Map<String, String> lcpMap, Map<String, Mapping> mappingMap) {

        switch (port.getPortDirection()) {

            case Bidirectional:
                String lcp =
                    PortMappingUtils.createXpdrLogicalConnectionPort(xponderNb, line, StringConstants.NETWORK_TOKEN);
                lcpMap.put(circuitPackName + '+' + port.getPortName(), lcp);
                mappingMap.put(lcp,
                    createXpdrMappingObject(nodeId, port, circuitPackName, lcp, null, null, null, xponderType));
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

                putXpdrLcpsInMaps(line, nodeId, xponderNb, xponderType,
                        circuitPackName, circuitPackName2.toString(), port, port2,
                        lcpMap, mappingMap);
                line += 2;
                break;

            default:
                LOG.error(PortMappingUtils.UNSUPPORTED_DIR_LOGMSG,
                    nodeId, port.getPortName(), circuitPackName, port.getPortDirection());
        }

        return line;
    }

    private boolean createMcCapabilitiesList(String nodeId, Info deviceInfo,
            Map<McCapabilitiesKey, McCapabilities> mcCapabilitiesMap) {
        Map<Integer, Degree> degrees = getDegreesMap(nodeId, deviceInfo);
        List<SharedRiskGroup> srgs = getSrgs(nodeId, deviceInfo);
        mcCapabilitiesMap.putAll(getMcCapabilities(degrees, srgs, nodeId));
        return true;
    }

    private boolean createTtpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for degree TTP's
        Map<Integer, Degree> degrees = getDegreesMap(nodeId, deviceInfo);
        Map<String, String> interfaceList = getEthInterfaceList(nodeId);
        List<CpToDegree> cpToDegreeList = getCpToDegreeList(degrees, interfaceList);
        LOG.info(PortMappingUtils.MAP_LOOKS_LOGMSG, nodeId, interfaceList);
        postPortMapping(nodeId, null, null, cpToDegreeList, null, null);

        for (Entry<Integer, List<ConnectionPorts>> cpMapEntry : getPerDegreePorts(nodeId, deviceInfo).entrySet()) {
            List<ConnectionPorts> cpMapValue = cpMapEntry.getValue();
            ConnectionPorts cp1 = cpMapValue.get(0);
            String cp1Name = cp1.getCircuitPackName();
            switch (cpMapValue.size()) {
                case 1:
                    // port is bidirectional
                    Ports port = getTtpPort(cp1, cp1Name, nodeId);
                    if (port == null) {
                        return false;
                    }
                    if (!checkTtpPort(port, cp1Name, nodeId, true)) {
                        continue;
                    }
                    String logicalConnectionPoint =
                            PortMappingUtils.degreeTtpNodeName(cpMapEntry.getKey().toString(), "TXRX");
                    LOG.info(PortMappingUtils.ASSOCIATED_LCP_LOGMSG,
                        nodeId, port.getPortName(), cp1Name, logicalConnectionPoint);
                    portMapList.add(createMappingObject(nodeId, port, cp1Name, logicalConnectionPoint));
                    break;
                case 2:
                    // ports are unidirectionals
                    Ports port1 = getTtpPort(cp1, cp1Name, nodeId);
                    if (port1 == null) {
                        return false;
                    }
                    ConnectionPorts cp2 = cpMapValue.get(1);
                    String cp2Name = cp2.getCircuitPackName();
                    Ports port2 = getTtpPort(cp2, cp2Name, nodeId);
                    if (port2 == null) {
                        return false;
                    }
                    if (!checkTtpPortsUnidir(port1, port2, cp1Name, cp2Name, nodeId)) {
                        continue;
                    }
                    String logicalConnectionPoint1 = PortMappingUtils.degreeTtpNodeName(cpMapEntry.getKey().toString(),
                            port1.getPortDirection().getName().toUpperCase(Locale.getDefault()));
                    LOG.info(PortMappingUtils.ASSOCIATED_LCP_LOGMSG,
                        nodeId, port1.getPortName(), cp1Name, logicalConnectionPoint1);
                    portMapList.add(createMappingObject(nodeId, port1, cp1Name, logicalConnectionPoint1));
                    String logicalConnectionPoint2 = PortMappingUtils.degreeTtpNodeName(cpMapEntry.getKey().toString(),
                            port2.getPortDirection().getName().toUpperCase(Locale.getDefault()));
                    LOG.info(PortMappingUtils.ASSOCIATED_LCP_LOGMSG,
                        nodeId, port2.getPortName(), cp2Name, logicalConnectionPoint2);
                    portMapList.add(createMappingObject(nodeId, port2, cp2Name, logicalConnectionPoint2));
                    break;
                default:
                    LOG.error(PortMappingUtils.NOT_CORRECT_CONPORT_LOGMSG, nodeId, cpMapEntry.getKey());
                    continue;
                    //TODO should it be continue or return false ?
            }
        }
        return true;
    }

    private Ports getTtpPort(ConnectionPorts cp, String cpName, String nodeId) {
        InstanceIdentifier<Ports> portID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey(cpName))
            .child(Ports.class, new PortsKey(cp.getPortName()));
        LOG.debug(PortMappingUtils.FETCH_CONNECTIONPORT_LOGMSG, nodeId, cp.getPortName(), cpName);
        Optional<Ports> portObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, portID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (portObject.isEmpty()) {
            LOG.error(PortMappingUtils.NO_PORT_ON_CP_LOGMSG, nodeId, cp.getPortName(), cpName);
            return null;
        }
        return portObject.get();
    }

    private boolean checkPortQual(Ports port, String cpName, String nodeId) {
        if (port.getPortQual() == null) {
            return false;
        }
        if (PortQual.RoadmExternal.getIntValue() != port.getPortQual().getIntValue()) {
            //used to be LOG.error when called from createTtpPortMapping
            LOG.debug(PortMappingUtils.PORT_NOT_RDMEXT_LOGMSG + PortMappingUtils.CANNOT_AS_LCP_LOGMSG,
                nodeId, port.getPortName(), cpName);
            return false;
        }
        return true;
    }

    private boolean checkTtpPort(Ports port, String cpName, String nodeId, boolean bidirectional) {
        if (!checkPortQual(port, cpName, nodeId)) {
            return false;
        }
        if (Direction.Bidirectional.getIntValue() == port.getPortDirection().getIntValue() ^ bidirectional) {
        // (a ^ b) makes more sense than (!a && b) here since it can also work for unidirectional links
            LOG.error(PortMappingUtils.PORTDIR_ERROR_LOGMSG + PortMappingUtils.CANNOT_AS_LCP_LOGMSG,
                nodeId, port.getPortName(), cpName);
            return false;
        }
        return true;
    }

    private boolean checkTtpPortsUnidir(Ports port1, Ports port2, String cp1Name, String cp2Name, String nodeId) {
        if (!checkTtpPort(port1, cp1Name, nodeId, false)) {
            return false;
        }
        if (!checkTtpPort(port2, cp2Name, nodeId, false)) {
            return false;
        }
        if (!checkPartnerPort(cp1Name, port1, port2)) {
            LOG.error(PortMappingUtils.NOT_CORRECT_PARTNERPORT_LOGMSG,
                nodeId, port2.getPortName(), cp2Name, port1.getPortName(), cp1Name);
            return false;
        }
        // Directions checks are the same for cp1 and cp2, no need to check them twice.
        if (!checkPartnerPortNoDir(cp2Name, port2, port1)) {
            LOG.error(PortMappingUtils.NOT_CORRECT_PARTNERPORT_LOGMSG,
                nodeId, port1.getPortName(), cp1Name, port2.getPortName(), cp2Name);
            return false;
        }
        return true;
    }

    private NodeInfo createNodeInfo(Info deviceInfo) {
        if (deviceInfo.getNodeType() == null) {
            // TODO make mandatory in yang
            LOG.error(PortMappingUtils.NODE_TYPE_LOGMSG, deviceInfo.getNodeId(), "field missing");
            return null;
        }
        NodeInfoBuilder nodeInfoBldr = new NodeInfoBuilder()
                .setOpenroadmVersion(OpenroadmNodeVersion._221)
                .setNodeClli(
                    deviceInfo.getClli() == null || deviceInfo.getClli().isEmpty()
                        ? "defaultCLLI"
                        : deviceInfo.getClli())
                .setNodeType(NodeTypes.forValue(deviceInfo.getNodeType().getIntValue()));
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
