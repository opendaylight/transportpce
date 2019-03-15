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

import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PowerDBm;

import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.EthAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.Interface1;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.RateIdentity;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenRoadmInterface121 {
    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfaceFactory.class);

    public  OpenRoadmInterface121(PortMapping portMapping, OpenRoadmInterfaces openRoadmInterfaces) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public String createOpenRoadmEthInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(String.format("Unable to get mapping from PortMapping for node % and"
                + " logical connection port %s", nodeId, logicalConnPoint));
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder();
        ethIfBuilder.setAutoNegotiation(EthAttributes.AutoNegotiation.Enabled);
        ethIfBuilder.setDuplex(EthAttributes.Duplex.Full);
        ethIfBuilder.setFec(EthAttributes.Fec.Off);
        ethIfBuilder.setSpeed(100000L);
        ethIfBuilder.setMtu(9000L);

        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(portMap, EthernetCsmacd.class,
                                                                          logicalConnPoint + "-ETHERNET");

        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        ethInterfaceBldr.addAugmentation(Interface1.class, ethIf1Builder.setEthernet(ethIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, ethInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack
        this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);

        return ethInterfaceBldr.getName();
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, Class<? extends InterfaceType> type,
                                                           String key) {
        InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        interfaceBuilder.setDescription("  TBD   ");
        interfaceBuilder.setCircuitId("   TBD    ");
        interfaceBuilder.setSupportingCircuitPackName(portMap.getSupportingCircuitPackName());
        interfaceBuilder.setSupportingPort(portMap.getSupportingPort());
        interfaceBuilder.setAdministrativeState(AdminStates.InService);
        interfaceBuilder.setType(type);
        interfaceBuilder.setName(key);
        interfaceBuilder.withKey(new InterfaceKey(key));
        return interfaceBuilder;
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
            throw new OpenRoadmInterfaceException(String.format("Unable to get mapping from PortMapping for node % and"
                + " logical connection port %s", nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder otuInterfaceBldr = createGenericInterfaceBuilder(portMap, OtnOtu.class, logicalConnPoint
                + "-OTU");
        otuInterfaceBldr.setSupportingInterface(supportOchInterface);

        // OTU interface specific data
        OtuBuilder otuIfBuilder = new OtuBuilder();
        otuIfBuilder.setFec(OtuAttributes.Fec.Scfec);
        otuIfBuilder.setRate(OTU4.class);

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1Builder otuIf1Builder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1Builder();
        otuInterfaceBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1.class,
                        otuIf1Builder.setOtu(otuIfBuilder.build()).build());

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
            throw new OpenRoadmInterfaceException(String.format("Unable to get mapping from PortMapping for node % and"
                + " logical connection port %s", nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(portMap, OtnOdu.class, logicalConnPoint
                + "-ODU");
        oduInterfaceBldr.setSupportingInterface(supportingOtuInterface);

        // ODU interface specific data
        OduBuilder oduIfBuilder = new OduBuilder();
        oduIfBuilder.setRate(ODU4.class);
        oduIfBuilder.setMonitoringMode(OduAttributes.MonitoringMode.Terminated);

        // Set Opu attributes
        OpuBuilder opuBldr = new OpuBuilder();
        opuBldr.setPayloadType("07");
        opuBldr.setExpPayloadType("07");
        oduIfBuilder.setOpu(opuBldr.build());

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1Builder oduIf1Builder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1Builder();
        oduInterfaceBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1.class,
                oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
        return oduInterfaceBldr.getName();
    }
    /**
     * This methods creates an OCH interface on the given termination point on
     * Roadm.
     *
     * @param waveNumber wavelength number of the OCH interface.
     * @return Name of the interface if successful, otherwise return null.
     */

    public List<String> createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber)
        throws OpenRoadmInterfaceException {

        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(String.format("Unable to get mapping from PortMapping for node %s and"
                + " logical connection port %s", nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = createGenericInterfaceBuilder(portMap, OpticalChannel.class,
            createOpenRoadmOchInterfaceName(logicalConnPoint, waveNumber));

        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder();
        ocIfBuilder.setWavelengthNumber(waveNumber);

        // Add supporting OMS interface
        if (portMap.getSupportingOms() != null) {
            ochInterfaceBldr.setSupportingInterface(portMap.getSupportingOms());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder
                ochIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces
                .rev161014.Interface1Builder();
        ochInterfaceBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1.class,
                ochIf1Builder.setOch(ocIfBuilder.build()).build());

        List<String> interfacesCreated = new ArrayList<>();
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);
        interfacesCreated.add(ochInterfaceBldr.getName());
        return interfacesCreated;
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber, Class<
            ? extends RateIdentity> rate, OchAttributes.ModulationFormat format) throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(String.format("Unable to get mapping from PortMapping for node %s and"
                + " logical connection port %s", nodeId, logicalConnPoint));
        }

        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder();
        ocIfBuilder.setWavelengthNumber(waveNumber);
        ocIfBuilder.setModulationFormat(format);
        ocIfBuilder.setRate(rate);
        ocIfBuilder.setTransmitPower(new PowerDBm(new BigDecimal("-5")));

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014
                .Interface1Builder ochIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel
                .interfaces.rev161014.Interface1Builder();
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = createGenericInterfaceBuilder(portMap, OpticalChannel.class,
            createOpenRoadmOchInterfaceName(logicalConnPoint, waveNumber));
        ochInterfaceBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1.class,
                ochIf1Builder.setOch(ocIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        return ochInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterfaceName(String logicalConnectionPoint, Long waveNumber) {
        return logicalConnectionPoint + "-" + waveNumber;
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
            otsInterfaceBldr.addAugmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                    .Interface1.class,
                    otsIf1Builder.setOts(otsIfBuilder.build()).build());
            this.openRoadmInterfaces.postInterface(nodeId, otsInterfaceBldr);
            this.portMapping.updateMapping(nodeId, mapping);
            return otsInterfaceBldr.build().getName();
        } else {
            return mapping.getSupportingOts();
        }
    }
}
