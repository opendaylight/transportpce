/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.Lldp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.CpToDegreeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMappingImpl implements PortMapping {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingImpl.class);

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmInterfaces openRoadmInterfaces;

    public PortMappingImpl(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
        OpenRoadmInterfaces openRoadmInterfaces) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    @Override
    public boolean createMappingData(String nodeId) {

        LOG.info("Create Mapping Data for node {}", nodeId);
        List<Mapping> portMapList = new ArrayList<>();
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfoOptional = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, infoIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        Info deviceInfo;
        if (deviceInfoOptional.isPresent()) {
            deviceInfo = deviceInfoOptional.get();
        } else {
            LOG.warn("Device info subtree is absent for {}", nodeId);
            return false;
        }
        if (deviceInfo.getNodeType() == null) {
            LOG.error("Node type field is missing"); // TODO make mandatory in yang
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
        return postPortMapping(deviceInfo, portMapList, deviceInfo.getNodeType().getIntValue(), null);
    }

    /**
     * This private method gets the list of external ports on a degree. For each
     * port in the degree, it does a get on port subtree with
     * circuit-pack-name/port-name as key in order to get the logical connection
     * point name corresponding to it.
     *
     * @param deviceInfo
     *            Info subtree read from the device
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL
     *
     * @return true/false based on status of operation
     */
    private boolean createTtpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for degree TTP's

        List<Degree> degrees = getDegrees(nodeId, deviceInfo);
        List<ConnectionPorts> degreeConPorts = getDegreePorts(degrees);
        Map<String, String> interfaceList = getEthInterfaceList(nodeId);
        List<CpToDegree> cpToDegreeList = getCpToDegreeList(degrees, nodeId, interfaceList);
        LOG.info("Map looks like this {}",interfaceList);

        postPortMapping(deviceInfo, null, deviceInfo.getNodeType().getIntValue(), cpToDegreeList);

        // Getting circuit-pack-name/port-name corresponding to TTP's
        for (ConnectionPorts cp : degreeConPorts) {
            String circuitPackName = cp.getCircuitPackName();
            String portName = cp.getPortName().toString();
            InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                CircuitPacks.class, new CircuitPacksKey(circuitPackName)).child(Ports.class, new PortsKey(portName));

            LOG.info("Fetching logical Connection Point value for port {} at circuit pack {}", portName,
                circuitPackName);
            Optional<Ports> portObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                LogicalDatastoreType.OPERATIONAL, portIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (portObject.isPresent()) {
                Ports port = portObject.get();
                if (port.getLogicalConnectionPoint() != null) {
                    LOG.info("Logical Connection Point for {} {} is {}", circuitPackName, portName, port
                        .getLogicalConnectionPoint());
                    portMapList.add(createMappingObject(nodeId, port, circuitPackName, port
                        .getLogicalConnectionPoint()));
                } else {
                    LOG.warn("Logical Connection Point value is missing for {} {}", circuitPackName, port
                        .getPortName());
                }
            } else {
                LOG.warn("Port {} is not present in node {} in circuit pack {}!", portName, nodeId, circuitPackName);
                continue; // TODO continue or return true?
            }
        }
        return true;
    }

    private List<Degree> getDegrees(String deviceId, Info ordmInfo) {
        List<Degree> degrees = new ArrayList<>();
        Integer maxDegree;

        // Get value for max degree from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        if (ordmInfo.getMaxDegrees() != null) {
            maxDegree = ordmInfo.getMaxDegrees();
        } else {
            maxDegree = 20;
        }

        for (int degreeCounter = 1; degreeCounter <= maxDegree; degreeCounter++) {
            LOG.info("Getting Connection ports for Degree Number {}", degreeCounter);
            InstanceIdentifier<Degree> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                Degree.class, new DegreeKey(degreeCounter));
            Optional<Degree> ordmDegreeObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.CONFIGURATION, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmDegreeObject.isPresent()) {
                degrees.add(ordmDegreeObject.get());
            } else {
                LOG.info("Device has {} degree", degreeCounter - 1);
                break;
            }
        }
        return degrees;
    }

    /**
     * This private method gets the list of circuit packs on an Srg. For each
     * circuit pack on an Srg, it does a get on circuit-pack subtree with
     * circuit-pack-name as key in order to get the list of ports. It then
     * iterates over the list of ports to get ports with port-qual as
     * roadm-external. It appends a TX,RX,TXRX to the logical connection point
     * name based on the direction of the port.
     *
     * @param nodeId
     *            Id of device
     * @param deviceInfo
     *            Info subtree read from the device
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL
     *
     * @return true/false based on status of operation
     */

    private boolean createPpPortMapping(String nodeId, Info deviceInfo, List<Mapping> portMapList) {
        // Creating mapping data for SRG's PP
        HashMap<Integer, List<
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>> srgCps = getSrgCps(
                nodeId, deviceInfo);
        Set<Map.Entry<Integer, List<
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>>> circuitPacks = srgCps
                .entrySet();
        for (Entry<Integer, List<
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>> entry : circuitPacks) {
            Integer srgIndex = entry.getKey();
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks cp : entry
                .getValue()) {
                String circuitPackName = cp.getCircuitPackName();
                InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                    CircuitPacks.class, new CircuitPacksKey(circuitPackName));
                Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                    LogicalDatastoreType.OPERATIONAL, cpIID, Timeouts.DEVICE_READ_TIMEOUT,
                    Timeouts.DEVICE_READ_TIMEOUT_UNIT);

                if (!circuitPackObject.isPresent() || (circuitPackObject.get().getPorts() == null)) {
                    LOG.warn("{} : Circuit pack {} not found or without ports.", nodeId, circuitPackName);
                    continue; // TODO continue or return false?
                }
                CircuitPacks circuitPack = circuitPackObject.get();
                for (Ports port : circuitPack.getPorts()) {
                    if (port.getLogicalConnectionPoint() != null) {
                        String logicalConnectionPoint = getLogicalConnectionPort(port, srgIndex);
                        LOG.info("{} : Logical Connection Point for {} {} is {}", nodeId, circuitPackName, port
                            .getPortName(), logicalConnectionPoint);
                        portMapList.add(createMappingObject(nodeId, port, circuitPackName, logicalConnectionPoint));
                    } else if (Port.PortQual.RoadmInternal.equals(port.getPortQual())) {
                        LOG.info("Port is internal, skipping Logical Connection Point missing for {} {}",
                            circuitPackName, port.getPortName());
                    } else if (port.getLogicalConnectionPoint() == null) {
                        LOG.info("Value missing, Skipping Logical Connection Point missing for {} {}", circuitPackName,
                            port.getPortName());
                    }
                }
            }
        }
        return true;
    }

    /**
     * This method does a get operation on shared risk group subtree of the
     * netconf device's config datastore and returns a list of all circuit packs
     * objects that are part of srgs. It is required to do a selective get on
     * all the circuit packs that contain add/drop ports of interest.
     *
     * @param deviceId
     *            Device id
     * @param ordmInfo
     *            Info subtree from the device
     * @return List of circuit packs object belonging to- shared risk group
     *         subtree
     */
    private HashMap<Integer, List<
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>> getSrgCps(String deviceId,
            Info ordmInfo) {
        HashMap<Integer, List<
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>> cpPerSrg =
                new HashMap<>();
        Integer maxSrg;
        // Get value for max Srg from info subtree, required for iteration
        // if not present assume to be 20 (temporary)
        if (ordmInfo.getMaxSrgs() != null) {
            maxSrg = ordmInfo.getMaxSrgs();
        } else {
            maxSrg = 20;
        }
        for (int srgCounter = 1; srgCounter <= maxSrg; srgCounter++) {
            List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCps =
                new ArrayList<>();
            LOG.info("Getting Circuitpacks for Srg Number {}", srgCounter);
            InstanceIdentifier<SharedRiskGroup> srgIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                SharedRiskGroup.class, new SharedRiskGroupKey(srgCounter));
            Optional<SharedRiskGroup> ordmSrgObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.CONFIGURATION, srgIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (ordmSrgObject.isPresent()) {
                srgCps.addAll(ordmSrgObject.get().getCircuitPacks());
                cpPerSrg.put(ordmSrgObject.get().getSrgNumber(), srgCps);
            }
        }
        LOG.info("Device {} has {} Srg", deviceId, cpPerSrg.size());
        return cpPerSrg;
    }

    /**
     * This private method gets the list of circuit packs on a xponder. For each
     * circuit pack on a Xponder, it does a get on circuit-pack subtree with
     * circuit-pack-name as key in order to get the list of ports. It then
     * iterates over the list of ports to get ports with port-qual as
     * xpdr-network/xpdr-client. The line and client ports are saved as:
     *
     * <p>
     * 1. LINEn
     *
     * <p>
     * 2. CLNTn
     *
     * @param nodeId
     *            Id of device
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL
     *
     * @return true/false based on status of operation
     */
    private boolean createXpdrPortMapping(String nodeId, List<Mapping> portMapList) {
        // Creating for Xponder Line and Client Ports
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        // Variable to keep track of number of line ports
        int line = 1;
        // Variable to keep track of number of client ports
        int client = 1;
        if (!deviceObject.isPresent() || (deviceObject.get().getCircuitPacks() == null)) {
            LOG.warn("Circuit Packs are not present for {}", nodeId);
            return false; // TODO return false or continue?
        }

        List<CircuitPacks> circuitPackList = deviceObject.get().getCircuitPacks();
        circuitPackList.sort(Comparator.comparing(CircuitPack::getCircuitPackName));

        for (CircuitPacks cp : circuitPackList) {
            String circuitPackName = cp.getCircuitPackName();
            if (cp.getPorts() == null) {
                LOG.warn("Ports were not found for circuit pack: {}", circuitPackName);
                continue;
            }
            for (Ports port : cp.getPorts()) {
                if (Port.PortQual.XpdrNetwork.equals(port.getPortQual())) {
                    portMapList.add(createMappingObject(nodeId, port, circuitPackName,
                        "XPDR1-" + OpenRoadmInterfacesImpl.NETWORK_TOKEN + line));
                //String lcp = port.getLogicalConnectionPoint();
                //String regex = "XPDR[0-9]+-NETWORK[0-9]+";
                //checkLogicalConnectionPoint(regex, lcp, nodeId, portMapList, circuitPackName, port, "NETWORK", line);
                    line++;
                } else if (Port.PortQual.XpdrClient.equals(port.getPortQual())) {
                    portMapList.add(createMappingObject(nodeId, port, circuitPackName,
                        "XPDR1-" + OpenRoadmInterfacesImpl.CLIENT_TOKEN + client));
                //String lcp = port.getLogicalConnectionPoint();
                //String regex = "XPDR[0-9]+-CLIENT[0-9]+";
                //checkLogicalConnectionPoint(regex, lcp, nodeId, portMapList, circuitPackName, port, "CLIENT", client);
                    client++;
                } else {
                    LOG.warn("Port type {} not supported", port.getPortQual());
                }
            }
        }
        return true;
    }

    private void checkLogicalConnectionPoint(String regex, String logicalConnectionPoint, String nodeId, List<
        Mapping> portMapList, String circuitPackName, Ports port, String portQual, int index) {
        if (logicalConnectionPoint.matches(regex)) {
            Mapping map = createMappingObject(nodeId, port, circuitPackName, logicalConnectionPoint);
            if (portMapList.stream().filter(mapping -> mapping.getLogicalConnectionPoint().equals(
                logicalConnectionPoint)).count() > 0) {
                LOG.warn("{} : logical connection point {} on {}  {} already used. Port can not be used", nodeId,
                    logicalConnectionPoint, circuitPackName, port.getPortName());
            } else {
                portMapList.add(map);
                LOG.info("{} : logical connection point for {}  {} is {}", nodeId, circuitPackName, port.getPortName(),
                    logicalConnectionPoint);
            }
        } else if (logicalConnectionPoint.isEmpty()) {
            String lcp = "XPDR0-" + portQual + index;
            Mapping map = createMappingObject(nodeId, port, circuitPackName, lcp);
            if (portMapList.contains(map)) {
                LOG.warn("{} : can not assign logical connection point {} to port {} of circuit pack {}. "
                    + "Name is already used", nodeId, lcp, port.getPortName(), circuitPackName);
            } else {
                portMapList.add(createMappingObject(nodeId, port, circuitPackName, lcp));
                LOG.info("{} : no logical connection port on {}  {}. Assigning {}.", nodeId, circuitPackName, port
                    .getPortName(), lcp);
            }
        } else {
            LOG.warn("{} : logical connection point {} on {} {} does not respect OrgOpenRoadm recommendation. Port can "
                + "not be used.", nodeId, logicalConnectionPoint, circuitPackName, port.getPortName());
        }
    }

    /**
     * This private method builds the mapping object to be pushed in MD-SAL in
     * order to save port mapping. In case of TTP ports, it also saves the
     * OTS,OMS interfaces provisioned on the port.
     *
     * @param port
     *            Reference to device's port subtree object.
     * @param circuitPackName
     *            Name of cp where port exists.
     * @param logicalConnectionPoint
     *            logical name of the port.
     *
     * @return true/false based on status of operation
     */

    private Mapping createMappingObject(String nodeId, Ports port, String circuitPackName,
        String logicalConnectionPoint) {
        MappingBuilder mpBldr = new MappingBuilder();
        mpBldr.withKey(new MappingKey(logicalConnectionPoint)).setLogicalConnectionPoint(logicalConnectionPoint)
            .setSupportingCircuitPackName(circuitPackName).setSupportingPort(port.getPortName());

        // Get OMS and OTS interface provisioned on the TTP's
        if (logicalConnectionPoint.contains(OpenRoadmInterfacesImpl.TTP_TOKEN) && (port.getInterfaces() != null)) {
            for (Interfaces interfaces : port.getInterfaces()) {
                try {
                    Optional<Interface> openRoadmInterface = this.openRoadmInterfaces.getInterface(nodeId, interfaces
                        .getInterfaceName());
                    if (openRoadmInterface.isPresent()) {
                        Class<? extends InterfaceType> interfaceType = openRoadmInterface.get().getType();
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

    private CpToDegree createCpToDegreeObject(String circuitPackName, String degreeNumber, String nodeId,
                                                     Map<String, String> interfaceList) {
        String interfaceName = null;
        if (interfaceList.get(circuitPackName) !=  null) {
            interfaceName = interfaceList.get(circuitPackName);
        }
        return new CpToDegreeBuilder().withKey(new CpToDegreeKey(circuitPackName)).setCircuitPackName(circuitPackName)
            .setDegreeNumber(new Long(degreeNumber)).setInterfaceName(interfaceName).build();
    }

    private static List<ConnectionPorts> getDegreePorts(List<Degree> degrees) {
        return degrees.stream().filter(degree -> degree.getConnectionPorts() != null).flatMap(degree -> degree
            .getConnectionPorts().stream()).collect(Collectors.toList());
    }

    private List<CpToDegree> getCpToDegreeList(List<Degree> degrees, String nodeId,
                                               Map<String, String> interfaceList) {
        List<CpToDegree> cpToDegreeList = new ArrayList<>();
        for (Degree degree : degrees) {
            if (degree.getCircuitPacks() != null) {
                LOG.info("Inside CP to degree list");
                cpToDegreeList.addAll(degree.getCircuitPacks().stream()
                    .map(cp -> createCpToDegreeObject(cp.getCircuitPackName() ,
                            degree.getDegreeNumber().toString(), nodeId ,interfaceList))
                    .collect(Collectors.toList()));
            }
        }
        return cpToDegreeList;
    }

    /**
     * This method for ports the portMapping corresponding to the
     * portmapping.yang file to the MD-SAL datastore.
     *
     * <p>
     * 1. Supporting circuit pack 2. Supporting port 3. Supporting OMS interface
     * (if port on ROADM)
     *
     * @param deviceInfo
     *            Info subtree from the device.
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL.
     *
     * @return Result true/false based on status of operation.
     */
    private boolean postPortMapping(Info deviceInfo, List<Mapping> portMapList, Integer nodeType, List<
        CpToDegree> cp2DegreeList) {
        NodesBuilder nodesBldr = new NodesBuilder();
        nodesBldr.withKey(new NodesKey(deviceInfo.getNodeId())).setNodeId(deviceInfo.getNodeId());
        nodesBldr.setNodeType(NodeTypes.forValue(nodeType));

        if (portMapList != null) {
            nodesBldr.setMapping(portMapList);
        }
        if (cp2DegreeList != null) {
            nodesBldr.setCpToDegree(cp2DegreeList);
        }

        List<Nodes> nodesList = new ArrayList<>();
        nodesList.add(nodesBldr.build());

        NetworkBuilder nwBldr = new NetworkBuilder();
        nwBldr.setNodes(nodesList);

        final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Network> nodesIID = InstanceIdentifier.builder(Network.class).build();
        Network network = nwBldr.build();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodesIID, network);
        CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
        try {
            submit.checkedGet();
            return true;

        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to post {}", network, e);
            return false;
        }
    }

    @Override
    public Mapping getMapping(String nodeId, String logicalConnPoint) {

        /*
         * Getting physical mapping corresponding to logical connection point
         */
        InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
            new NodesKey(nodeId)).child(Mapping.class, new MappingKey(logicalConnPoint)).build();
        try (ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Mapping> mapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, portMappingIID).get()
                .toJavaUtil();
            if (mapObject.isPresent()) {
                Mapping mapping = mapObject.get();
                LOG.info("Found mapping for the logical port {}. Mapping: {}", logicalConnPoint, mapping.toString());
                return mapping;
            } else {
                LOG.warn("Could not find mapping for logical connection point {} for nodeId {}", logicalConnPoint,
                    nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for logical connection point : {} for nodeId {}", logicalConnPoint,
                nodeId, ex);
        }
        return null;
    }

    private static String getLogicalConnectionPort(Ports port, int srgCounter) {
        String logicalConnectionPoint = null;
        if (port.getLogicalConnectionPoint() != null) {
            switch (port.getPortDirection()) {
                case Tx:
                    // Port direction is transmit
                    if (!port.getLogicalConnectionPoint().contains("SRG")) {
                        logicalConnectionPoint = "SRG" + srgCounter + "-" + port.getLogicalConnectionPoint() + "-TX";
                    } else {
                        logicalConnectionPoint = port.getLogicalConnectionPoint() + "-TX";
                    }
                    break;
                case Rx:
                    // Port direction is receive
                    if (!port.getLogicalConnectionPoint().contains("SRG")) {
                        logicalConnectionPoint = "SRG" + srgCounter + "-" + port.getLogicalConnectionPoint() + "-RX";
                    } else {
                        logicalConnectionPoint = port.getLogicalConnectionPoint() + "-RX";
                    }
                    break;
                case Bidirectional:
                    // port is bidirectional
                    if (!port.getLogicalConnectionPoint().contains("SRG")) {
                        logicalConnectionPoint = "SRG" + srgCounter + "-" + port.getLogicalConnectionPoint();
                    } else {
                        logicalConnectionPoint = port.getLogicalConnectionPoint();
                    }
                    if (!port.getLogicalConnectionPoint().endsWith("-TXRX")) {
                        logicalConnectionPoint = logicalConnectionPoint.concat("-TXRX");
                    }
                    break;
                default:
                    // Unsupported Port direction
                    LOG.error("Unsupported port direction for port {}  {}", port, port.getPortDirection());
                    return null; // TODO return false or continue?
            }
            return logicalConnectionPoint;
        }
        LOG.warn("Unsupported port direction for port {} - {} - LogicalConnectionPoint is null", port, port
            .getPortDirection());
        return null; // TODO return false or continue?
    }

    @Override
    public void deleteMappingData(String nodeId) {
        LOG.info("Deleting Mapping Data corresponding at node '{}'", nodeId);
        WriteTransaction rw = this.dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class).child(Nodes.class, new NodesKey(
            nodeId));
        rw.delete(LogicalDatastoreType.CONFIGURATION, nodesIID);
        try {
            rw.submit().get(1, TimeUnit.SECONDS);
            LOG.info("Port mapping removal for node '{}'", nodeId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error for removing port mapping infos for node '{}'", nodeId);
        }

    }

    @Override
    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        LOG.info("Updating Mapping Data {} for node {}", oldMapping, nodeId);
        InstanceIdentifier<Ports> portIId = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
            CircuitPacks.class, new CircuitPacksKey(oldMapping.getSupportingCircuitPackName())).child(Ports.class,
                new PortsKey(oldMapping.getSupportingPort()));
        if (oldMapping != null && nodeId != null) {
            try {
                Optional<Ports> portObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                    LogicalDatastoreType.OPERATIONAL, portIId, Timeouts.DEVICE_READ_TIMEOUT,
                    Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (portObject.isPresent()) {
                    Ports port = portObject.get();
                    Mapping newMapping = createMappingObject(nodeId, port, oldMapping.getSupportingCircuitPackName(),
                        oldMapping.getLogicalConnectionPoint());

                    final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
                    InstanceIdentifier<Mapping> mapIID = InstanceIdentifier.create(Network.class).child(Nodes.class,
                        new NodesKey(nodeId)).child(Mapping.class, new MappingKey(oldMapping
                            .getLogicalConnectionPoint()));
                    writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, mapIID, newMapping);
                    CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
                    submit.checkedGet();
                    return true;
                }
                return false;
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction Commit Error updating Mapping {} for node {}", oldMapping
                    .getLogicalConnectionPoint(), nodeId, e);
                return false;
            }
        } else {
            LOG.error("Impossible to update mapping");
            return false;
        }

    }

    private Map<String, String> getEthInterfaceList(String nodeId) {
        LOG.info("It is calling get ethernet interface");
        Map<String, String> cpToInterfaceMap = new HashMap<>();
        InstanceIdentifier<Lldp> lldpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Protocols.class).augmentation(Protocols1.class).child(Lldp.class);
        Optional<Lldp> lldpObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                LogicalDatastoreType.OPERATIONAL, lldpIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (lldpObject.isPresent() && (lldpObject.get().getPortConfig() != null)) {
            for (PortConfig portConfig : lldpObject.get().getPortConfig()) {
                if (portConfig.getAdminStatus().equals(PortConfig.AdminStatus.Txandrx)) {
                    InstanceIdentifier<Interface> interfaceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                            .child(Interface.class, new InterfaceKey(portConfig.getIfName()));
                    Optional<Interface> interfaceObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                            LogicalDatastoreType.OPERATIONAL, interfaceIID, Timeouts.DEVICE_READ_TIMEOUT,
                            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    if (interfaceObject.isPresent() && (interfaceObject.get().getSupportingCircuitPackName() != null)) {
                        String supportingCircuitPackName = interfaceObject.get().getSupportingCircuitPackName();
                        cpToInterfaceMap.put(supportingCircuitPackName, portConfig.getIfName());
                        InstanceIdentifier<CircuitPacks> circuitPacksIID = InstanceIdentifier.create(OrgOpenroadmDevice
                                .class).child(CircuitPacks.class, new CircuitPacksKey(supportingCircuitPackName));
                        Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(
                                nodeId, LogicalDatastoreType.OPERATIONAL, circuitPacksIID, Timeouts
                                                .DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                        if (circuitPackObject.isPresent() && (circuitPackObject.get().getParentCircuitPack() != null)) {
                            cpToInterfaceMap.put(circuitPackObject.get().getParentCircuitPack().getCircuitPackName() ,
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

}
