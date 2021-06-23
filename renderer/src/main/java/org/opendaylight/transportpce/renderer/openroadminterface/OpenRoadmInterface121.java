/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.math.BigDecimal;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PowerDBm;
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
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenRoadmInterface121 {
    private static final String MAPPING_MSG_ERROR =
            "Unable to get mapping from PortMapping for node % and logical connection port %s";
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
            throw new OpenRoadmInterfaceException(String.format(MAPPING_MSG_ERROR, nodeId, logicalConnPoint));
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
                .setAutoNegotiation(EthAttributes.AutoNegotiation.Enabled)
                .setDuplex(EthAttributes.Duplex.Full)
                .setFec(EthAttributes.Fec.Off)
                .setSpeed(Uint32.valueOf(100000))
                .setMtu(Uint32.valueOf(9000));

        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(portMap, EthernetCsmacd.class,
                                                                          logicalConnPoint + "-ETHERNET");

        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        ethInterfaceBldr.addAugmentation(ethIf1Builder.setEthernet(ethIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, ethInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack
        this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);

        return ethInterfaceBldr.getName();
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, Class<? extends InterfaceType> type,
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
            throw new OpenRoadmInterfaceException(String.format(MAPPING_MSG_ERROR, nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder otuInterfaceBldr = createGenericInterfaceBuilder(portMap, OtnOtu.class, logicalConnPoint
                + "-OTU");
        otuInterfaceBldr.setSupportingInterface(supportOchInterface);

        // OTU interface specific data
        OtuBuilder otuIfBuilder = new OtuBuilder()
                .setFec(OtuAttributes.Fec.Scfec)
                .setRate(OTU4.class);

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1Builder otuIf1Builder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1Builder();
        otuInterfaceBldr.addAugmentation(otuIf1Builder.setOtu(otuIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, otuInterfaceBldr);
        return otuInterfaceBldr.getName();
    }

    /**
     * This methods creates an ODU interface on the given termination point.
     *
     * @param nodeId node ID
     * @param logicalConnPoint logical Connection Point
     * @param supportingOtuInterface supporting OTU Interface
     *
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadmInterfaceException
     */

    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(String.format(MAPPING_MSG_ERROR, nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(portMap, OtnOdu.class, logicalConnPoint
                + "-ODU");
        oduInterfaceBldr.setSupportingInterface(supportingOtuInterface);

        // ODU interface specific data
        // Set Opu attributes
        OpuBuilder opuBldr = new OpuBuilder()
                .setPayloadType("07")
                .setExpPayloadType("07");
        OduBuilder oduIfBuilder = new OduBuilder()
                .setRate(ODU4.class)
                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
                .setOpu(opuBldr.build());

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1Builder oduIf1Builder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

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
            throw new OpenRoadmInterfaceException(String.format(
                "Unable to get mapping from PortMapping for node %s and logical connection port %s",
                    nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = createGenericInterfaceBuilder(portMap, OpticalChannel.class,
                spectrumInformation.getIdentifierFromParams(logicalConnPoint));

        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder().setWavelengthNumber(spectrumInformation.getWaveLength());

        // Add supporting OMS interface
        if (portMap.getSupportingOms() != null) {
            ochInterfaceBldr.setSupportingInterface(portMap.getSupportingOms());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder
                ochIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces
                .rev161014.Interface1Builder();
        ochInterfaceBldr.addAugmentation(ochIf1Builder.setOch(ocIfBuilder.build()).build());

        List<String> interfacesCreated = new ArrayList<>();
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);
        interfacesCreated.add(ochInterfaceBldr.getName());
        return interfacesCreated;
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation) throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(String.format(
                "Unable to get mapping from PortMapping for node %s and logical connection port %s",
                    nodeId, logicalConnPoint));
        }
        OchAttributes.ModulationFormat modulationFormat = OchAttributes.ModulationFormat.DpQpsk;
        Optional<OchAttributes.ModulationFormat> optionalModulationFormat = OchAttributes.ModulationFormat
                .forName(spectrumInformation.getModulationFormat());
        if (optionalModulationFormat.isPresent()) {
            modulationFormat =  optionalModulationFormat.get();
        }
        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder()
                .setWavelengthNumber(spectrumInformation.getWaveLength())
                .setModulationFormat(modulationFormat)
                .setRate(R100G.class)
                .setTransmitPower(new PowerDBm(new BigDecimal("-5")));

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014
                .Interface1Builder ochIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel
                .interfaces.rev161014.Interface1Builder();
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = createGenericInterfaceBuilder(portMap, OpticalChannel.class,
                spectrumInformation.getIdentifierFromParams(logicalConnPoint));
        ochInterfaceBldr.addAugmentation(ochIf1Builder.setOch(ocIfBuilder.build()).build());

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
        if (mapping.getSupportingOms() == null) {
            // Create generic interface
            InterfaceBuilder omsInterfaceBldr = createGenericInterfaceBuilder(mapping, OpenROADMOpticalMultiplex.class,
                "OMS-" + mapping.getLogicalConnectionPoint());
            if (mapping.getSupportingOts() != null) {
                omsInterfaceBldr.setSupportingInterface(mapping.getSupportingOts());
            } else {
                LOG.error("Unable to get ots interface from mapping {} - {}", nodeId,
                          mapping.getLogicalConnectionPoint());
                return null;
            }
            this.openRoadmInterfaces.postInterface(nodeId, omsInterfaceBldr);
            this.portMapping.updateMapping(nodeId, mapping);
            return omsInterfaceBldr.build().getName();
        } else {
            return mapping.getSupportingOms();
        }
    }

    public String createOpenRoadmOtsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        if (mapping.getSupportingOts() == null) {
            // Create generic interface
            InterfaceBuilder otsInterfaceBldr = createGenericInterfaceBuilder(mapping, OpticalTransport.class, "OTS-"
                    + mapping.getLogicalConnectionPoint());
            // OTS interface augmentation specific data
            OtsBuilder otsIfBuilder = new OtsBuilder();
            otsIfBuilder.setFiberType(OtsAttributes.FiberType.Smf);

            // Create Interface1 type object required for adding as
            // augmentation
            org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                    .Interface1Builder otsIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm
                    .optical.transport.interfaces.rev161014.Interface1Builder();
            otsInterfaceBldr.addAugmentation(otsIf1Builder.setOts(otsIfBuilder.build()).build());
            this.openRoadmInterfaces.postInterface(nodeId, otsInterfaceBldr);
            this.portMapping.updateMapping(nodeId, mapping);
            return otsInterfaceBldr.build().getName();
        } else {
            return mapping.getSupportingOts();
        }
    }

    public boolean isUsedByXc(String nodeId, String interfaceName, String xc,
        DeviceTransactionManager deviceTransactionManager) {
        InstanceIdentifier<RoadmConnections> xciid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey(xc));
        LOG.info("reading xc {} in node {}", xc, nodeId);
        Optional<RoadmConnections> crossconnection = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, xciid, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (crossconnection.isPresent()) {
            RoadmConnections rc = crossconnection.get();
            LOG.info("xd {} found", xc);
            if (rc.getSource().getSrcIf().equals(interfaceName)
                || rc.getDestination().getDstIf().equals(interfaceName)) {
                return true;
            }
        } else {
            LOG.info("xd {} not found !", xc);
        }
        return false;
    }

}
