/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter.util;

import java.util.HashMap;
import java.util.HashSet;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.NwTopologyService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.NwTopologyServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
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
            .setIndex(Uint8.ONE)
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

    public static Context buildContext() {
        Set<LayerProtocolName> layerProtname = new HashSet<LayerProtocolName>();
        layerProtname.add(LayerProtocolName.DSR);
        layerProtname.add(LayerProtocolName.DIGITALOTN);
        layerProtname.add(LayerProtocolName.ODU);
        layerProtname.add(LayerProtocolName.PHOTONICMEDIA);
        Name nameService = new NameBuilder()
            .setValue("T0 - Full Multi-layer topology")
            .setValueName("TAPI Topology Name")
            .build();
        Map<NameKey, Name> mapNameservice = new HashMap<NameKey, Name>();
        mapNameservice.put(nameService.key(), nameService);
        Map<NameKey, Name> mapName = new HashMap<NameKey, Name>();
        Name name = new NameBuilder()
            .setValue("T-API Context")
            .setValueName("TAPI Context Name")
            .build();
        mapName.put(name.key(), name);
        mapName.put(name.key(), name);
        new TopologyBuilder().setLayerProtocolName(layerProtname)
            .setUuid(new Uuid("4aedacb6-f830-3b3d-983a-a2de06bc373b")).build();
        Set<RuleType> ruletypeSet = new HashSet<RuleType>();
        ruletypeSet.add(RuleType.FORWARDING);
        FORWARDINGRULE forwardingRule = null;
        Rule rule = new RuleBuilder()
            .setLocalId("forward2.2")
            .setRuleType(ruletypeSet)
            .setForwardingRule(forwardingRule)
            .build();
        Map<RuleKey, Rule> ruleMap = new HashMap<RuleKey, Rule>();
        ruleMap.put(rule.key(), rule);
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setUuid(new Uuid("3dbea9a6-867a-3233-a479-6af3d9912f94"))
            .setRule(ruleMap)
            .build();
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<NodeRuleGroupKey, NodeRuleGroup>();
        nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
        Node node = new NodeBuilder()
            .setUuid(new Uuid("4582e51f-2b2d-3b70-b374-86c463062710"))
            .setNodeRuleGroup(nodeRuleGroupMap)
            .build();
        Map<NodeKey, Node> nodeMap = new HashMap<NodeKey, Node>();
        nodeMap.put(node.key(), node);
        Map<TopologyKey, Topology> topologyMap = new HashMap<TopologyKey, Topology>();
        Map<NameKey, Name> mapNameTopo = new HashMap<NameKey, Name>();
        Name nameTopo = new NameBuilder()
            .setValue("Value topo")
            .setValueName("Value name topo")
            .build();
        mapNameTopo.put(nameTopo.key(), nameTopo);
        Topology topology = new TopologyBuilder()
            .setUuid(new Uuid("4aedacb6-f830-3b3d-983a-a2de06bc373b"))
            .setLayerProtocolName(layerProtname)
            .setName(mapNameTopo)
            .setNode(nodeMap)
            .build();
        topologyMap.put(topology.key(), topology);
        NwTopologyService nktopologyService = new NwTopologyServiceBuilder()
            .setUuid(new Uuid("1be1ab9d-a0cb-3538-a4ce-1ae71bfa302f"))
            .setName(mapNameservice)
            .build();
        TopologyContext topologyContext = new TopologyContextBuilder()
            .setTopology(topologyMap)
            .setNwTopologyService(nktopologyService).build();
        return new ContextBuilder()
            .setName(mapName)
            .setUuid(new Uuid("30b80400-8589-3e24-b586-c9ceceacb9c1"))
            .addAugmentation(new Context1Builder()
                .setTopologyContext(topologyContext).build())
            .build();
    }
}
