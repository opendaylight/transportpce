/*
 * Copyright © 2021 AT&T and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.ParentOduAllocationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.parent.odu.allocation.trib.slots.choice.OpucnBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev200529.Off;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUTTPCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.OduAttributes.MonitoringMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.opu.OpuBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmOtnInterface710 {
    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private static final Logger LOG = LoggerFactory
        .getLogger(OpenRoadmOtnInterface710.class);

    public OpenRoadmOtnInterface710(PortMapping portMapping,
        OpenRoadmInterfaces openRoadmInterfaces) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public String createOpenRoadmEth100GInterface(String nodeId,
        String logicalConnPoint) throws OpenRoadmInterfaceException {

        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
            .setFec(Off.class)
            .setSpeed(Uint32.valueOf(100000));
        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(
            portMap, EthernetCsmacd.class,
            logicalConnPoint + "-ETHERNET100G");
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

    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint,
        String serviceName, String payLoad,
        boolean isNetworkPort, OpucnTribSlotDef minTribSlotNumber, OpucnTribSlotDef maxTribSlotNumber)
        throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }
        List<String> supportingInterfaceList = new ArrayList<>();
        String supportingInterface = null;
        if (isNetworkPort) {
            supportingInterface = portMap.getSupportingOduc4();
        } else {
            supportingInterface = logicalConnPoint + "-ETHERNET100G";
        }

        if (supportingInterface == null) {
            throw new OpenRoadmInterfaceException(
                "Interface Creation failed because of missing supported "
                    + "ODU4 on network end or Ethernet on client");
        }
        // Supporting interface is a list for B100G (7.1) device models
        supportingInterfaceList.add(supportingInterface);

        InterfaceBuilder oduIfBuilder = createGenericInterfaceBuilder(
            portMap, OtnOdu.class, logicalConnPoint + "-ODU4-" + serviceName)
            .setSupportingInterfaceList(supportingInterfaceList);
        // Agument ODU4 specific interface data
        OduBuilder oduBuilder = new OduBuilder().setRate(ODU4.class)
            .setOduFunction(ODUTTPCTP.class)
            .setMonitoringMode(MonitoringMode.Terminated);
        LOG.debug("Inside the ODU4 creation {} {} {}", isNetworkPort, minTribSlotNumber.getValue(),
            maxTribSlotNumber.getValue());
        // If it is a network port we have fill the required trib-slots and trib-ports
        if (isNetworkPort) {
            List<OpucnTribSlotDef> opucnTribSlotDefList = new ArrayList<>();
            // Escape characters are used to here to take the literal dot
            Uint16 tribPortNumber = Uint16.valueOf(minTribSlotNumber.getValue().split("\\.")[0]);
            Uint16 startTribSlot = Uint16.valueOf(minTribSlotNumber.getValue().split("\\.")[1]);
            Uint16 endTribSlot = Uint16.valueOf(maxTribSlotNumber.getValue().split("\\.")[1]);

            IntStream.range(startTribSlot.intValue(), endTribSlot.intValue() + 1)
                .forEach(
                    nbr -> opucnTribSlotDefList.add(OpucnTribSlotDef.getDefaultInstance(tribPortNumber + "." + nbr))
                );
            ParentOduAllocationBuilder parentOduAllocationBuilder = new ParentOduAllocationBuilder()
                .setTribPortNumber(tribPortNumber)
                .setTribSlotsChoice(new OpucnBuilder().setOpucnTribSlots(opucnTribSlotDefList).build());
            // reset the ODU function as ODUCTP and the monitoring moode
            oduBuilder.setOduFunction(ODUCTP.class)
                .setMonitoringMode(MonitoringMode.NotTerminated)
                .setParentOduAllocation(parentOduAllocationBuilder.build());
        }
        else {
           //  This is for the client side of the ODU (ODU-TTP-CTP)
            // Set Opu attributes
            OpuBuilder opuBuilder = new OpuBuilder()
                .setExpPayloadType(new PayloadTypeDef(payLoad))
                .setPayloadType(new PayloadTypeDef(payLoad));
            oduBuilder.setOpu(opuBuilder.build());
        }

        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder
            oduIf1Builder = new
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder();
        oduIfBuilder.addAugmentation(oduIf1Builder.setOdu(oduBuilder.build()).build());
        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, oduIfBuilder);
        LOG.info("Returning the ODU4 inteface {}", oduIfBuilder.getName());
        return oduIfBuilder.getName();
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
}
