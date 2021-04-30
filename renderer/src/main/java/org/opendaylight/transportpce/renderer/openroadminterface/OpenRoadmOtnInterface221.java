/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.openroadminterface;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev171215.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev181019.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev181019.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUTTPCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.OduAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.opu.OpuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.parent.odu.allocation.ParentOduAllocationBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenRoadmOtnInterface221 {

    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private static final Logger LOG = LoggerFactory
            .getLogger(OpenRoadmOtnInterface221.class);

    public OpenRoadmOtnInterface221(PortMapping portMapping,
            OpenRoadmInterfaces openRoadmInterfaces) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public String createOpenRoadmEth1GInterface(String nodeId,
            String logicalConnPoint) throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
                .setSpeed(Uint32.valueOf(1000));
        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(
                portMap, EthernetCsmacd.class,
                logicalConnPoint + "-ETHERNET1G");
        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        ethInterfaceBldr.addAugmentation(ethIf1Builder.setEthernet(ethIfBuilder.build()).build());
        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, ethInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack
        this.openRoadmInterfaces.postOTNEquipmentState(nodeId,
                portMap.getSupportingCircuitPackName(), true);
        this.portMapping.updateMapping(nodeId, portMap);
        String ethernetInterfaceName = ethInterfaceBldr.getName();

        return ethernetInterfaceName;
    }

    private void throwException(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        throw new OpenRoadmInterfaceException(String.format(
                "Unable to get mapping from PortMapping for node % and logical connection port %s",
                nodeId, logicalConnPoint));
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap,
            Class<? extends InterfaceType> type, String key) {
        return new InterfaceBuilder()
                // .setDescription(" TBD ")
                // .setCircuitId(" TBD ")
                .setSupportingCircuitPackName(
                        portMap.getSupportingCircuitPackName())
                .setSupportingPort(portMap.getSupportingPort())
                .setAdministrativeState(AdminStates.InService)
                // TODO get rid of unchecked cast warning
                .setType(type).setName(key).withKey(new InterfaceKey(key));
    }

    public String createOpenRoadmEth10GInterface(String nodeId,
            String logicalConnPoint) throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
                // .setAutoNegotiation(EthAttributes.AutoNegotiation.Disabled)
                .setSpeed(Uint32.valueOf(10000));
        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(portMap, EthernetCsmacd.class,
                logicalConnPoint + "-ETHERNET10G").addAugmentation(ethIf1Builder.setEthernet(ethIfBuilder.build())
                        .build());
        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, ethInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack
        this.openRoadmInterfaces.postOTNEquipmentState(nodeId,
                portMap.getSupportingCircuitPackName(), true);
        this.portMapping.updateMapping(nodeId, portMap);
        String ethernetInterfaceName = ethInterfaceBldr.getName();

        return ethernetInterfaceName;
    }

    public String createOpenRoadmOdu2eInterface(String nodeId,
            String logicalConnPoint, String serviceName, String payLoad,
            boolean isNetworkPort, int tribPortNumber, int tribSlotIndex)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }
        String supportingInterface = null;

        if (isNetworkPort) {
            supportingInterface = portMap.getSupportingOdu4();
        } else {
            supportingInterface = logicalConnPoint + "-ETHERNET10G";
        }

        if (supportingInterface == null) {
            throw new OpenRoadmInterfaceException(
                    "Interface Creation failed because of missing supported "
                    + "ODU4 on network end or Ethernet on client");
        }

        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(
                portMap, OtnOdu.class,
                logicalConnPoint + "-ODU2e-" + serviceName)
                        .setSupportingInterface(supportingInterface);

        // ODU interface specific data
        OduBuilder oduIfBuilder = new OduBuilder().setRate(ODU2e.class)
                .setOduFunction(ODUTTPCTP.class)
                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated);
        LOG.debug("Inside the ODU2e creation {} {} {}", isNetworkPort,
                tribPortNumber, tribSlotIndex);
        if (isNetworkPort) {
            List<Uint16> tribSlots = new ArrayList<>();
            Uint16 newIdx = Uint16.valueOf(tribSlotIndex);
            tribSlots.add(newIdx);
            IntStream.range(tribSlotIndex, tribSlotIndex + 8)
                    .forEach(nbr -> tribSlots.add(Uint16.valueOf(nbr)));
            ParentOduAllocationBuilder parentOduAllocationBuilder = new ParentOduAllocationBuilder()
                    .setTribPortNumber(Uint16.valueOf(tribPortNumber))
                    .setTribSlots(tribSlots);
            oduIfBuilder.setOduFunction(ODUCTP.class)
                    .setMonitoringMode(OduAttributes.MonitoringMode.Monitored)
                    .setParentOduAllocation(parentOduAllocationBuilder.build());
        } else {
            // Set Opu attributes
            OpuBuilder opuBldr = new OpuBuilder()
                    .setPayloadType(new PayloadTypeDef(payLoad))
                    .setExpPayloadType(new PayloadTypeDef(payLoad));
            oduIfBuilder.setOpu(opuBldr.build());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder
            oduIf1Builder = new
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, oduInterfaceBldr);
        LOG.info("returning the ODU2e inteface {}", oduInterfaceBldr.getName());
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOdu0Interface(String nodeId,
            String logicalConnPoint, String serviceName, String payLoad,
            boolean isNetworkPort, int tribPortNumber, int tribSlot)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        String supportingInterface = null;

        if (isNetworkPort) {
            supportingInterface = portMap.getSupportingOdu4();
        } else {
            supportingInterface = logicalConnPoint + "-ETHERNET1G";
        }
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(
                portMap, OtnOdu.class,
                logicalConnPoint + "-ODU0-" + serviceName);
        oduInterfaceBldr.setSupportingInterface(supportingInterface);

        // ODU interface specific data
        OduBuilder oduIfBuilder = new OduBuilder().setRate(ODU0.class)
                .setOduFunction(ODUTTPCTP.class)
                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated);
        if (isNetworkPort) {
            LOG.debug("Network port is true");
            List<Uint16> tribSlots = new ArrayList<>();
            // add trib slots
            tribSlots.add(Uint16.valueOf(tribSlot));
            ParentOduAllocationBuilder parentOduAllocationBuilder = new ParentOduAllocationBuilder()
                    // set trib port numbers
                    .setTribPortNumber(Uint16.valueOf(tribPortNumber))
                    .setTribSlots(tribSlots);
            oduIfBuilder.setOduFunction(ODUCTP.class)
                    .setMonitoringMode(OduAttributes.MonitoringMode.Monitored)
                    .setParentOduAllocation(parentOduAllocationBuilder.build());
            LOG.debug("Network port is true {} {} {}",
                    oduIfBuilder.getParentOduAllocation().getTribSlots(),
                    oduIfBuilder.getRate(),
                    oduIfBuilder.getParentOduAllocation().getTribPortNumber());
        } else {
            LOG.debug("Current port is a client port");
            OpuBuilder opuBldr = new OpuBuilder()
                    .setPayloadType(new PayloadTypeDef(payLoad))
                    .setExpPayloadType(new PayloadTypeDef(payLoad));
            oduIfBuilder.setOpu(opuBldr.build());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder
            oduIf1Builder = new
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, oduInterfaceBldr);
        LOG.info("returning the ODU0 inteface {}", oduInterfaceBldr.getName());
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOdu2Interface(String nodeId,
            String logicalConnPoint, String serviceName, String payLoad,
            boolean isNetworkPort, int tribPortNumber, int tribSlotIndex)
            throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        String supportingInterface = null;

        if (portMap != null) {
            if (isNetworkPort) {
                supportingInterface = portMap.getSupportingOdu4();
            } else {
                supportingInterface = portMap.getSupportingEthernet();
            }
        } else {
            throwException(nodeId, logicalConnPoint);
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(
                portMap, OtnOdu.class,
                logicalConnPoint + "-ODU2-" + serviceName)
                        .setSupportingInterface(supportingInterface);

        OduBuilder oduIfBuilder = new OduBuilder().setRate(ODU2.class)
                .setOduFunction(ODUTTPCTP.class)
                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated);
        if (isNetworkPort) {
            List<Uint16> tribSlots = new ArrayList<>();
            IntStream.range(tribSlotIndex, tribSlotIndex + 8)
                    .forEach(nbr -> tribSlots.add(Uint16.valueOf(nbr)));
            ParentOduAllocationBuilder parentOduAllocationBuilder = new ParentOduAllocationBuilder()
                    // set trib port numbers
                    .setTribPortNumber(Uint16.valueOf(tribPortNumber))
                    .setTribSlots(tribSlots);
            oduIfBuilder.setOduFunction(ODUCTP.class)
                    .setMonitoringMode(OduAttributes.MonitoringMode.Monitored)
                    .setParentOduAllocation(parentOduAllocationBuilder.build());
        } else {
            // Set Opu attributes
            OpuBuilder opuBldr = new OpuBuilder()
                    .setPayloadType(new PayloadTypeDef(payLoad))
                    .setExpPayloadType(new PayloadTypeDef(payLoad));
            oduIfBuilder.setOpu(opuBldr.build());
        }

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder
            oduIf1Builder = new
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, oduInterfaceBldr);
        LOG.info("returning the ODU2 inteface {}", oduInterfaceBldr.getName());
        return oduInterfaceBldr.getName();
    }
}
