/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.EthAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOtu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.R100G;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.OtsAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.OduAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.OpuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.OtuAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.otu.container.OtuBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenRoadmInterface121 {
    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterface121.class);

    public  OpenRoadmInterface121(PortMapping portMapping, OpenRoadmInterfaces openRoadmInterfaces) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public String createOpenRoadmEthInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder ethInterfaceBldr =
            createGenericInterfaceBuilder(portMap, EthernetCsmacd.VALUE, logicalConnPoint + "-ETHERNET")
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new Interface1Builder()
                        .setEthernet(
                            // Ethernet interface specific data
                            new EthernetBuilder()
                                .setAutoNegotiation(EthAttributes.AutoNegotiation.Enabled)
                                .setDuplex(EthAttributes.Duplex.Full)
                                .setFec(EthAttributes.Fec.Off)
                                .setSpeed(Uint32.valueOf(100000))
                                .setMtu(Uint32.valueOf(9000))
                                .build())
                        .build());
        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, ethInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack
        this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        return ethInterfaceBldr.getName();
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, InterfaceType type,
                                                           String key) {
        return new InterfaceBuilder()
                .setDescription("  TBD   ")
                .setCircuitId("   TBD    ")
                .setSupportingCircuitPackName(portMap.getSupportingCircuitPackName())
                .setSupportingPort(portMap.getSupportingPort())
                .setAdministrativeState(AdminStates.InService)
                .setType(type)
                .setName(key)
                .withKey(new InterfaceKey(key));
    }
    /**
     * This methods creates an OTU interface on the given termination point.
     *
     * @param nodeId node ID
     * @param logicalConnPoint logical Connection Point
     * @param supportOchInterface support OCH Interface
     *
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadmInterfaceException
     */

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder otuInterfaceBldr =
            createGenericInterfaceBuilder(portMap, OtnOtu.VALUE, logicalConnPoint + "-OTU")
                .setSupportingInterface(supportOchInterface)
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1Builder()
                        .setOtu(
                            // OTU interface specific data
                            new OtuBuilder()
                                .setFec(OtuAttributes.Fec.Scfec)
                                .setRate(OTU4.VALUE)
                                .build())
                        .build());
        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, otuInterfaceBldr);
        this.portMapping.updateMapping(nodeId, portMap);
        return otuInterfaceBldr.getName();
    }

    /**
     * This methods creates an ODU interface on the given termination point.
     *
     * @param nodeId node ID
     * @param logicalConnPoint logical Connection Point
     *
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadmInterfaceException
     */

    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr =
            createGenericInterfaceBuilder(portMap, OtnOdu.VALUE, logicalConnPoint + "-ODU");
        if (portMap.getSupportingOtu4() != null) {
            oduInterfaceBldr.setSupportingInterface(portMap.getSupportingOtu4());
        }
        oduInterfaceBldr.addAugmentation(
            // Create Interface1 type object required for adding as augmentation
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1Builder()
                .setOdu(
                    // ODU interface specific data
                    new OduBuilder()
                        .setRate(ODU4.VALUE)
                        .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
                        .setOpu(
                            // Set Opu attributes
                            new OpuBuilder()
                                .setPayloadType("07")
                                .setExpPayloadType("07")
                                .build())
                        .build())
                .build());
        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
        return oduInterfaceBldr.getName();
    }
    /**
     * This methods creates a list of OCH interface on the given termination point on
     * Roadm.
     *
     * @param nodeId node ID
     * @param logicalConnPoint logical connection point
     * @param spectrumInformation SpectrumInformation
     * @return List containing name of the interface if successful, otherwise return empty list.
     *
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public List<String> createOpenRoadmOchInterfaces(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr =
            createGenericInterfaceBuilder(
                portMap, OpticalChannel.VALUE,
                spectrumInformation.getIdentifierFromParams(logicalConnPoint));
        // Add supporting OMS interface
        if (portMap.getSupportingOms() != null) {
            ochInterfaceBldr.setSupportingInterface(portMap.getSupportingOms());
        }
        ochInterfaceBldr.addAugmentation(
            // Create Interface1 type object required for adding as augmentation
            new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder()
                .setOch(
                    // OCH interface specific data
                    new OchBuilder().setWavelengthNumber(spectrumInformation.getWaveLength()).build())
                .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);
        return new ArrayList<String>(List.of(ochInterfaceBldr.getName()));
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation) throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        ModulationFormat modulationFormat =
            OchAttributes.ModulationFormat.forName(spectrumInformation.getModulationFormat());
        if (modulationFormat == null) {
            modulationFormat = OchAttributes.ModulationFormat.DpQpsk;
        }
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr =
            createGenericInterfaceBuilder(
                    portMap, OpticalChannel.VALUE,
                    spectrumInformation.getIdentifierFromParams(logicalConnPoint))
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014
                            .Interface1Builder()
                        .setOch(
                            // OCH interface specific data
                            new OchBuilder()
                                .setWavelengthNumber(spectrumInformation.getWaveLength())
                                .setModulationFormat(modulationFormat)
                                .setRate(R100G.VALUE)
                                .setTransmitPower(new PowerDBm(Decimal64.valueOf("-5")))
                                .build())
                        .build());
        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        return ochInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterfaceName(String logicalConnectionPoint, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,logicalConnectionPoint, spectralSlotName);
    }

    public String createOpenRoadmOmsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        if (mapping.getSupportingOms() != null) {
            return mapping.getSupportingOms();
        }
        if (mapping.getSupportingOts() == null) {
            LOG.error("Unable to get ots interface from mapping {} - {}", nodeId, mapping.getLogicalConnectionPoint());
            return null;
        }
        // Create generic interface
        InterfaceBuilder omsInterfaceBldr =
            createGenericInterfaceBuilder(
                     mapping, OpenROADMOpticalMultiplex.VALUE,
                    "OMS-" + mapping.getLogicalConnectionPoint())
                .setSupportingInterface(mapping.getSupportingOts());
        this.openRoadmInterfaces.postInterface(nodeId, omsInterfaceBldr);
        this.portMapping.updateMapping(nodeId, mapping);
        return omsInterfaceBldr.build().getName();
    }

    public String createOpenRoadmOtsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        if (mapping.getSupportingOts() != null) {
            return mapping.getSupportingOts();
        }
        // Create generic interface
        InterfaceBuilder otsInterfaceBldr =
            createGenericInterfaceBuilder(mapping, OpticalTransport.VALUE, "OTS-" + mapping.getLogicalConnectionPoint())
                .addAugmentation(
                    // Create Interface1 type object required for adding as
                    // augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                            .Interface1Builder()
                        .setOts(
                            // OTS interface augmentation specific data
                            new OtsBuilder().setFiberType(OtsAttributes.FiberType.Smf).build())
                        .build());
        this.openRoadmInterfaces.postInterface(nodeId, otsInterfaceBldr);
        this.portMapping.updateMapping(nodeId, mapping);
        return otsInterfaceBldr.build().getName();
    }

    public boolean isUsedByXc(String nodeId, String interfaceName, String xc,
            DeviceTransactionManager deviceTransactionManager) {
        LOG.info("reading xc {} in node {}", xc, nodeId);
        Optional<RoadmConnections> crossconnection = deviceTransactionManager.getDataFromDevice(
            nodeId,
            LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier
                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(xc))
                .build(),
            Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (crossconnection.isEmpty()) {
            LOG.info("xd {} not found !", xc);
            return false;
        }
        RoadmConnections rc = crossconnection.orElseThrow();
        LOG.info("xd {} found", xc);
        if (rc.getSource().getSrcIf().equals(interfaceName)
                || rc.getDestination().getDstIf().equals(interfaceName)) {
            return true;
        }
        return false;
    }

}
