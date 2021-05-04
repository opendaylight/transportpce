package org.opendaylight.transportpce.renderer.openroadminterface;

import java.util.ArrayList;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OpucnTribSlotDef;
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
        List<String> supportingInterface = new ArrayList<String>();

        if (isNetworkPort) {
            supportingInterface = portMap.getSu;
        } else {
            supportingInterface = logicalConnPoint + "-ETHERNET100G";
        }

        if (supportingInterface == null) {
            throw new OpenRoadmInterfaceException(
                "Interface Creation failed because of missing supported "
                    + "ODU4 on network end or Ethernet on client");
        }

        InterfaceBuilder oduIfBuilder = createGenericInterfaceBuilder(
            portMap, OtnOdu.class, logicalConnPoint + "-ODU4-" + serviceName)
            .setSupportingInterfaceList()
        )
        return "";
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
