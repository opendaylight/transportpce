/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.alarm.pm.types.rev191129.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev191129.EquipmentTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.CircuitPackCategoryBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.PortsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDeviceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.shelves.Shelves;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.shelves.ShelvesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.shelves.ShelvesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.supporting.circuit.pack.list.grp.SupportingPortList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.supporting.circuit.pack.list.grp.SupportingPortListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.supporting.circuit.pack.list.grp.SupportingPortListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.EthernetCsmacd;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class ConverterTestUtil {

    private ConverterTestUtil() {
        throw new UnsupportedOperationException("Do not create an instance of utility class");
    }

    public static OrgOpenroadmDevice buildDevice() {
        Interface interfaceList = new InterfaceBuilder()
                .setName("interface 1GE")
                .setType(EthernetCsmacd.VALUE)
                .setOperationalState(State.InService)
                .build();
        Map<InterfaceKey, Interface> interfaceMap = new HashMap<>();
        interfaceMap.put(interfaceList.key(), interfaceList);
        SupportingPortList supPortList = new SupportingPortListBuilder()
            .setIndex(Uint8.valueOf(1))
            .setCircuitPackName("circuit-pack-name")
            .setPortList(Set.of("port-name"))
            .build();
        Map<SupportingPortListKey, SupportingPortList> supPortListMap = new HashMap<>();
        supPortListMap.put(supPortList.key(), supPortList);
        Ports ports = new PortsBuilder()
            .setPortName("port-name")
            .setPortDirection(Direction.Bidirectional)
            .setIsPhysical(true)
            .setFaceplateLabel("faceplate-label")
            .setOperationalState(State.InService)
            .setAdministrativeState(AdminStates.OutOfService)
            .setSupportingPortList(supPortListMap)
            .build();
        Map<PortsKey, Ports> portsMap = new HashMap<>();
        portsMap.put(ports.key(), ports);
        CircuitPacks cp = new CircuitPacksBuilder()
            .setCircuitPackName("circuit-pack-name")
            .setCircuitPackType("circuit-pack-type")
            .setAdministrativeState(AdminStates.InService)
            .setVendor("vendor")
            .setCircuitPackMode("NORMAL")
            .setModel("model")
            .setSerialId("serial-id")
            .setCircuitPackCategory(new CircuitPackCategoryBuilder().setType(EquipmentTypeEnum.CircuitPack).build())
            .setShelf("1")
            .setSlot("slot")
            .setIsPluggableOptics(false)
            .setIsPhysical(true)
            .setIsPassive(false)
            .setFaceplateLabel("faceplate-label")
            .setPorts(portsMap)
            .build();
        Map<CircuitPacksKey, CircuitPacks> cpMap = new HashMap<>();
        cpMap.put(cp.key(), cp);
        Shelves shelf = new ShelvesBuilder()
            .setShelfName("1")
            .setShelfType("shelf-type")
            .setVendor("vendor")
            .setModel("model")
            .setSerialId("serial-id")
            .setIsPhysical(true)
            .setIsPassive(false)
            .setFaceplateLabel("faceplate-label")
            .build();
        Map<ShelvesKey, Shelves> shelfMap = new HashMap<>();
        shelfMap.put(shelf.key(), shelf);
        return new OrgOpenroadmDeviceBuilder()
            .setShelves(shelfMap)
            .setCircuitPacks(cpMap)
            .setInterface(interfaceMap)
            .build();
    }
}
