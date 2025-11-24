/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.transportpce.tapi.frequency.TeraHertzFactory;
import org.opendaylight.transportpce.tapi.frequency.grid.Available;
import org.opendaylight.transportpce.tapi.frequency.grid.AvailableGrid;
import org.opendaylight.transportpce.tapi.frequency.grid.FrequencyMath;
import org.opendaylight.transportpce.tapi.frequency.grid.Numeric;
import org.opendaylight.transportpce.tapi.frequency.grid.NumericFrequency;
import org.opendaylight.transportpce.tapi.frequency.range.FrequencyRangeFactory;
import org.opendaylight.transportpce.tapi.frequency.range.Range;
import org.opendaylight.transportpce.tapi.frequency.range.RangeFactory;
import org.opendaylight.transportpce.tapi.frequency.range.SortedRange;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev250110.degree.used.wavelengths.UsedWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev250110.degree.used.wavelengths.UsedWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.TxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev250110.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev250110.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev250110.xpdr.odu.switching.pools.OduSwitchingPoolsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev250110.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev250110.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev250110.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev250110.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev191129.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev250110.xpdr.mode.attributes.supported.operational.modes.OperationalModeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSize;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacity;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacity;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.payload.structure.CapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ParentNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ParentNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPEGigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ConnectionEndPoint2Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIERMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.PhotonicMediaNodeEdgePointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.PhotonicMediaNodeEdgePointSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.cep.list.connection.end.point.OtsMediaConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULECANNOTFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.AvailablePayloadStructure;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.AvailablePayloadStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedPayloadStructure;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedPayloadStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class centralizes common methods to handle the conversion of OpenROADM topology to a T-API Topology.
 * Provided methods are shared between Classes :
 *    - dedicated to the generation of Topology on RPC requests,
 *    - dedicated to the generation of Tapi Topology at initialization from existing Datastore,
 *    - dedicated to Tapi Topology update on Data-Tree-Changes.
 */
public class ORtoTapiTopoConversionTools {

    private static final Logger LOG = LoggerFactory.getLogger(ORtoTapiTopoConversionTools.class);
    private static final TreeMap<Integer, String> OPMODE_LOOPRATE_MAP;
    private static final int OPMODE_LOOPRATE_MAX;
    static final Map<String, Map<String, Map<LAYERPROTOCOLQUALIFIER, Uint64>>> LPN_MAP;
    private String ietfNodeId;
    private OpenroadmNodeType ietfNodeType;
    private AdminStates ietfNodeAdminState;
    private State ietfNodeOperState;
    private List<TerminationPoint> oorClientPortList;
    private List<TerminationPoint> oorNetworkPortList;
    private Map<OduSwitchingPoolsKey, OduSwitchingPools> oorOduSwitchingPool;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private Map<String, Uuid> uuidMap;
    private final Numeric numericFrequency;
    private Map<InterRuleGroupKey, InterRuleGroup> irgMap;
    private final RangeFactory rangeFactory;
    private final Factory frequencyFactory;

    static {
        OPMODE_LOOPRATE_MAP = new TreeMap<>(Comparator.reverseOrder());
        OPMODE_LOOPRATE_MAP.putAll(Map.of(8, "800G", 6, "600G" , 4, "400G" , 3, "300G" , 2, "200G" , 1, "100G"));
        OPMODE_LOOPRATE_MAX = Collections.max(OPMODE_LOOPRATE_MAP.keySet());
        LPN_MAP = new HashMap<>(Map.of(
            "ETH", new HashMap<>(Map.of(
                "If1GEODU0", Map.of(
                    ODUTYPEODU0.VALUE, Uint64.ONE, DIGITALSIGNALTYPEGigE.VALUE, Uint64.ONE),
                "If10GEODU2e", Map.of(
                    ODUTYPEODU2E.VALUE, Uint64.ONE, DIGITALSIGNALTYPE10GigELAN.VALUE, Uint64.ONE),
                "If10GEODU2", Map.of(
                    ODUTYPEODU2.VALUE, Uint64.ONE, DIGITALSIGNALTYPE10GigELAN.VALUE, Uint64.ONE),
                "If10GE", Map.of(DIGITALSIGNALTYPE10GigELAN.VALUE, Uint64.ONE),
                "If100GEODU4", Map.of(
                    ODUTYPEODU4.VALUE, Uint64.ONE, DIGITALSIGNALTYPE100GigE.VALUE, Uint64.ONE),
                "If100GE", Map.of(DIGITALSIGNALTYPE100GigE.VALUE, Uint64.ONE),
                //"IfOCH", Map.of(ODUTYPEODU4.VALUE, Uint64.ONE))),
                "IfOCH", Map.of(ODUTYPEODU4.VALUE, Uint64.ONE, OTUTYPEOTU4.VALUE, Uint64.ONE))),
            "OTU", new HashMap<>(Map.of(
                "IfOCHOTUCnODUCn",
                    Map.of(OTUTYPEOTUCN.VALUE, Uint64.ONE),
                "IfOCH",
                    Map.of(OTUTYPEOTU4.VALUE, Uint64.ONE),
                "IfOCHOTU4ODU4",
                    Map.of(OTUTYPEOTU4.VALUE, Uint64.ONE))),
            "ODU", new HashMap<>(Map.of(
                "If1GEODU0", Map.of(ODUTYPEODU0.VALUE, Uint64.ONE),
                "If10GEODU2e", Map.of(ODUTYPEODU2E.VALUE, Uint64.ONE),
                "If10GEODU2", Map.of(ODUTYPEODU2.VALUE, Uint64.ONE),
                "If100GEODU4", Map.of(ODUTYPEODU4.VALUE, Uint64.ONE),
                "IfOCHOTUCnODUCn", Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4), ODUTYPEODUCN.VALUE, Uint64.ONE),
                "IfOCH", Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4)),
                "IfOCHOTU4ODU4", Map.of(ODUTYPEODU4.VALUE, Uint64.ONE))),
            "DIGITAL_OTN", new HashMap<>(Map.of(
                "If1GEODU0", Map.of(ODUTYPEODU0.VALUE, Uint64.ONE),
                "If10GEODU2e", Map.of(ODUTYPEODU2E.VALUE, Uint64.ONE),
                "If10GEODU2", Map.of(ODUTYPEODU2.VALUE, Uint64.ONE),
                "If100GEODU4", Map.of(ODUTYPEODU4.VALUE, Uint64.ONE),
                "IfOCHOTUCnODUCn",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4), ODUTYPEODUCN.VALUE, Uint64.ONE,
                        OTUTYPEOTUCN.VALUE, Uint64.ONE),
                "IfOCH",
                    Map.of(OTUTYPEOTU4.VALUE, Uint64.ONE, ODUTYPEODU4.VALUE, Uint64.ONE),
                "IfOCHOTU4ODU4",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.ONE, OTUTYPEOTU4.VALUE, Uint64.ONE))),
            "PHOTONIC_MEDIA", new HashMap<>(Map.of(
                "IfOCHOTUCnODUCn",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4), ODUTYPEODUCN.VALUE, Uint64.ONE,
                        OTUTYPEOTUCN.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTSiMC.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTS.VALUE, Uint64.ONE),
                "IfOCH",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4),
                        PHOTONICLAYERQUALIFIEROTSiMC.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTS.VALUE, Uint64.ONE),
                "IfOCHOTU4ODU4",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.ONE, OTUTYPEOTU4.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTSiMC.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTS.VALUE, Uint64.ONE)
             ))
            ));
        LPN_MAP.put("DSR", LPN_MAP.get("ETH"));
        LPN_MAP.get("ODU").put("If10GE", LPN_MAP.get("ODU").get("If10GEODU2"));
        LPN_MAP.get("ODU").put("If100GE", LPN_MAP.get("ODU").get("If100GEODU4"));
        LPN_MAP.get("DIGITAL_OTN").put("If10GE", LPN_MAP.get("ODU").get("If10GEODU2"));
        LPN_MAP.get("DIGITAL_OTN").put("If100GE", LPN_MAP.get("ODU").get("If100GEODU4"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOtsiOtucnOducn", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTUCnODUCn"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOCHOTUCnODUCnRegen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTUCnODUCn"));
        LPN_MAP
            .get("PHOTONIC_MEDIA").put("IfOCHOTUCnODUCnUniregen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTUCnODUCn"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOCHOTU4ODU4Regen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTU4ODU4"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOCHOTU4ODU4Uniregen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTU4ODU4"));
    }

    /**
     * Instantiate an ORToTapiTopoConversionFactory Object.
     * @param tapiTopoUuid Uuid of the generated topology used in Builders.
     */
    public ORtoTapiTopoConversionTools(Uuid tapiTopoUuid) {
        this(
                tapiTopoUuid,
                new NumericFrequency(
                        GridConstant.START_EDGE_FREQUENCY_THZ,
                        GridConstant.EFFECTIVE_BITS,
                        new FrequencyMath()
                ),
                new FrequencyRangeFactory(),
                new TeraHertzFactory()
        );
    }

    /**
     * Instantiate an ORToTapiTopoConversionFactory Object.
     * @param tapiTopoUuid Uuid of the generated topology used in Builders.
     * @param numericFrequency NumericFrequency instance facilitating the management of the flex grid.
     * @param rangeFactory Simplifies creation of frequency ranges.
     * @param frequencyFactory Simplifies creation of Frequency objects.
     */
    public ORtoTapiTopoConversionTools(Uuid tapiTopoUuid, Numeric numericFrequency, RangeFactory rangeFactory,
            Factory frequencyFactory) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.uuidMap = new HashMap<>();
        this.tapiSips = new HashMap<>();
        this.oorOduSwitchingPool = new HashMap<>();
        this.irgMap = new HashMap<>();
        this.numericFrequency = numericFrequency;
        this.rangeFactory = rangeFactory;
        this.frequencyFactory = frequencyFactory;
    }

    /**
     * Convert Xponder Node from OpenROADM to Tapi.
     * @param ietfNode the OpenROADM node to be converted.
     * @param networkPorts The list of Node's network/line port.
     */
    public void convertNode(Node ietfNode, List<String> networkPorts) {
        this.ietfNodeId = ietfNode.getNodeId().getValue();
        var ietfAug =
            ietfNode.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Node1.class);
        if (ietfAug == null) {
            return;
        }
        this.ietfNodeType = ietfAug.getNodeType();
        this.ietfNodeAdminState = ietfAug.getAdministrativeState();
        this.ietfNodeOperState = ietfAug.getOperationalState();
        var ietfAugTopo =
            ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class);
        this.oorNetworkPortList = ietfAugTopo.getTerminationPoint().values().stream()
            .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                    == OpenroadmTpType.XPONDERNETWORK.getIntValue()
                && networkPorts.contains(tp.getTpId().getValue()))
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        OduSwitchingPools oorsp;
        if (this.ietfNodeType.equals(OpenroadmNodeType.TPDR)) {
           // this.oorOduSwitchingPool = createOduSwitchingPoolForTp100G();
            oorsp = createOduSwitchingPoolForTp100G();
            this.oorOduSwitchingPool.put(oorsp.key(), oorsp);
            List<TpId> tpList = new ArrayList<>();
            for (Map.Entry<NonBlockingListKey, NonBlockingList> nbl : oorsp.getNonBlockingList().entrySet()) {
                tpList.addAll(nbl.getValue().getTpList().stream().collect(Collectors.toList()));
            }
            this.oorClientPortList = ietfAugTopo.getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                        == OpenroadmTpType.XPONDERCLIENT.getIntValue()
                    && tpList.contains(tp.getTpId()))
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
            this.oorClientPortList.forEach(tp -> LOG.info("tp = {}", tp.getTpId()));
        } else {
            for (OduSwitchingPools osp : ietfNode.augmentation(Node1.class).getSwitchingPools()
                    .getOduSwitchingPools().values()) {
                this.oorOduSwitchingPool.put(osp.key(), osp);
            }
            this.oorClientPortList = ietfAugTopo.getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                    == OpenroadmTpType.XPONDERCLIENT.getIntValue())
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
        }

        // node creation [DSR/ODU] ([DSR/ODU] and OTSI merged in R 2.4.X)
        LOG.info("creation of a DSR/ODU node for {}", this.ietfNodeId);
        String nodeIdXpdr = String.join("+", this.ietfNodeId, TapiConstants.XPDR);
        this.uuidMap.put(nodeIdXpdr,
                //nodeUuid
                new Uuid(UUID.nameUUIDFromBytes(nodeIdXpdr.getBytes(StandardCharsets.UTF_8)).toString()));
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name").setValue(nodeIdXpdr).build();
        Name namePhot = new NameBuilder().setValueName("otsi node name").setValue(nodeIdXpdr).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type").setValue(this.ietfNodeType.getName()).build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node dsrNode =
            createTapiNode(
                Map.of(nameDsr.key(), nameDsr, namePhot.key(), namePhot, nameNodeType.key(), nameNodeType),
                //dsrLayerProtocols
                Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU,
                       LayerProtocolName.DIGITALOTN, LayerProtocolName.PHOTONICMEDIA));
        LOG.debug("XPDR Node {} should have {} NEPs and SIPs",
            this.ietfNodeId, this.oorClientPortList.size() + this.oorNetworkPortList.size());
        LOG.info("XPDR Node {} has {} NEPs and {} SIPs",
            this.ietfNodeId,
            dsrNode.getOwnedNodeEdgePoint().values().size(),
            dsrNode.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());
        tapiNodes.put(dsrNode.key(), dsrNode);
    }

    /**
     * Creates one elementary Node Rule Group including NEPs that can/can't forward among each others in a ROADM node.
     * @param topoType The level of abstraction of the topology.
     * @param nodeUuid The Uuid of the node.
     * @param subNodeName The extension in OpenROADM NodeId that identifies the  considered SRG/DEG. this subnode Name
     *                    is used to form the name of the NRG, as the information of the extension is removed from TAPI
     *                    nodeID (no disaggregation of ROADM in Tapi topology representation).
     * @param onepl List of owned node edge points to be considered in the NRGs.
     * @param forwardingRule Forwarding Rule set in the NRG (May or Cannot Forward).
     * @param index Index used to form the SRG name.
     */
    public Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForRdmNode(String topoType, Uuid nodeUuid,
            String subNodeName, List<OwnedNodeEdgePointKey> onepl, FORWARDINGRULE forwardingRule, int index) {
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
                nepMap = new HashMap<>();
        for (OwnedNodeEdgePointKey onepKey : onepl) {
            var nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                    .node.rule.group.NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeEdgePointUuid(onepKey.getUuid())
                .build();
            nepMap.put(nep.key(), nep);
        }
        String nrgNameValue = String.join("-", subNodeName, "node-rule-group-" + index);
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(forwardingRule)
            .setRuleType(new HashSet<RuleType>(Set.of(RuleType.FORWARDING)))
            .build();
        Name nrgName = new NameBuilder().setValueName("nrg name").setValue(nrgNameValue).build();
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiConstants.COST_HOP_VALUE)
                .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setName(Map.of(nrgName.key(), nrgName))
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((nrgNameValue).getBytes(StandardCharsets.UTF_8)).toString()))
            .setRule(new HashMap<RuleKey, Rule>(Map.of(rule.key(), rule)))
            .setNodeEdgePoint(nepMap)
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .build();
        return new HashMap<>(Map.of(nodeRuleGroup.key(), nodeRuleGroup));
    }

    /**
     * Creates Node Rule Group(s) that describe(s) the connectivity of the ROADM node.
     * @param topoType The level of abstraction of the topology which determines the way the NRG is computed :
     *                 (For abstracted topology, a single ROADM node summarizes the ROADM infrastructure with an
     *                 Any to Any connectivity),
     * @param nodeUuid Uuid of the node,
     * @param orNodeId id of the openROADM node,
     * @param onepl Collection of owned node edge points to be considered in the creation of the different NRGs.
     */
    public Map<NodeRuleGroupKey, NodeRuleGroup> createAllNodeRuleGroupForRdmNode(
            String topoType, Uuid nodeUuid, String orNodeId, Collection<OwnedNodeEdgePoint> onepl) {
        List<OwnedNodeEdgePoint> otsNepList = topoType.equals("T0ML") ? onepl.stream().collect(Collectors.toList())
                : onepl.stream()
                    .filter(onep -> onep.getName().keySet().contains(new NameKey("PHOTONIC_MEDIA_OTSNodeEdgePoint")))
                    .collect(Collectors.toList());
        LOG.debug("Creating NRG for {} {}", topoType, otsNepList);
        List<OwnedNodeEdgePointKey> degOnepKeyList = new ArrayList<>();
        List<String> srgNames = new ArrayList<>();
        Map<OwnedNodeEdgePointKey,String> srgMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep : otsNepList) {
            String onepName = onep.getName()
                .get(new NameKey(topoType.equals("T0ML") ? "NodeEdgePoint name" : "PHOTONIC_MEDIA_OTSNodeEdgePoint"))
                .getValue();
            String subNodeName = topoType.equals("T0ML") ? "ROADMINFRA-SRG-PP"
                : String.join("-", onepName.split("\\+")[0], onepName.split("\\+")[2]);
            if (subNodeName.contains("DEG")) {
                subNodeName = subNodeName.split("\\-TTP")[0];
                degOnepKeyList.add(onep.key());
            } else if (subNodeName.contains("SRG")) {
                subNodeName = subNodeName.split("\\-PP")[0];
                srgMap.put(onep.key(), subNodeName);
                if (!srgNames.contains(subNodeName)) {
                    srgNames.add(subNodeName);
                }
            }
        }
        int index = 0;
        Map<NodeRuleGroupKey, NodeRuleGroup> globalNrgMap = new  HashMap<>();
        if (topoType.equals("Full")) {
            globalNrgMap.putAll(createNodeRuleGroupForRdmNode(
                    topoType, nodeUuid, String.join("-", orNodeId, "DEG"),
                    degOnepKeyList, FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, index));
            index++;
        }
        for (String srgName : srgNames) {
            globalNrgMap.putAll(createNodeRuleGroupForRdmNode(
                topoType,
                nodeUuid,
                srgName,
                srgMap.entrySet().stream()
                    .filter(item -> item.getValue().equals(srgName))
                    .map(item -> item.getKey())
                    .collect(Collectors.toList()),
                // For T0ML we consider any port of ROADM INFRA can connect to potentially any other port
                //topoType.equals("T0ML") ? FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                topoType.equals("T0ML") ? FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                    : FORWARDINGRULECANNOTFORWARDACROSSGROUP.VALUE,
                // Whereas for Abstracted or Full Topology we consider any port of the same SRG can not forward to
                // another port of the same SRG. Connectivity between SRGS will be defined through inter-rule-group
                index));
            index++;
            LOG.debug("AllNodeRuleGroup : creating a NRG for {}", srgName);
        }
        return globalNrgMap;
    }

    public Map<InterRuleGroupKey, InterRuleGroup> createInterRuleGroupForXpdrNode(
            int index, Uuid nodeUuid, List<NodeRuleGroupKey> nrgKeyList,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.Rule rule) {
        Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> associatedNrgMap = new HashMap<>();
        for (NodeRuleGroupKey nrgKey : nrgKeyList) {
            AssociatedNodeRuleGroup associatedNrg = new AssociatedNodeRuleGroupBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeRuleGroupUuid(nrgKey.getUuid())
                .build();
            associatedNrgMap.put(associatedNrg.key(), associatedNrg);
        }

        Map<InterRuleGroupKey, InterRuleGroup> irgMapp = new HashMap<>();
        String irgNameValue = " irg-" + index;
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiConstants.COST_HOP_VALUE)
                .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();

        Name irgName = new NameBuilder().setValueName("irg name").setValue(irgNameValue).build();
        InterRuleGroup interRuleGroup = new InterRuleGroupBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((irgNameValue).getBytes(StandardCharsets.UTF_8)).toString()))
            .setName(Map.of(irgName.key(), irgName))
            .setRule(new HashMap<>(Map.of(rule.key(), rule)))
            .setAssociatedNodeRuleGroup(associatedNrgMap)
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .build();
        irgMapp.put(new InterRuleGroupKey(interRuleGroup.getUuid()), interRuleGroup);

        return irgMapp;
    }

/**
 * Creates Inter Rule Group(s) that describe(s) forwarding rules between the different NRGs.
 * @param topoType The level of abstraction of the topology which conditions IRG name,
 * @param nodeUuid Uuid of the node,
 * @param orNodeId id of the openROADM node,
 * @param nrgMap Map of the different NRGs for which the IRG provides forwarding rules.
 */
    public Map<InterRuleGroupKey, InterRuleGroup> createInterRuleGroupForRdmNode(
            String topoType, Uuid nodeUuid, String orNodeId, Map<NodeRuleGroupKey, String> nrgMap) {

        Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> associatedNrgSrgMap = new HashMap<>();
        Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> associatedNrgDegMap = new HashMap<>();
        for (Map.Entry<NodeRuleGroupKey, String> nrgEntry : nrgMap.entrySet()) {
            AssociatedNodeRuleGroup associatedNrg = new AssociatedNodeRuleGroupBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeRuleGroupUuid(nrgEntry.getKey().getUuid())
                .build();
            if (nrgEntry.getValue().contains("SRG")) {
                associatedNrgSrgMap.put(associatedNrg.key(), associatedNrg);
            } else {
                associatedNrgDegMap.put(associatedNrg.key(), associatedNrg);
            }
        }
        var rule = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                .inter.rule.group.RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(new HashSet<RuleType>(Set.of(RuleType.FORWARDING)))
            .build();

        if (associatedNrgSrgMap == null || associatedNrgSrgMap.isEmpty()
                || associatedNrgDegMap == null || associatedNrgDegMap.isEmpty()) {
            return null;
        }
        Map<InterRuleGroupKey, InterRuleGroup> irgMapp = new HashMap<>();
        int srgCounter = 0;
        for (Map.Entry<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> anrg : associatedNrgSrgMap.entrySet()) {
            String irgNameValue = topoType.equals("Full")
                ? orNodeId + " inter rule group-" + srgCounter
                : "rdm infra inter rule group-" + srgCounter;
            Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> associatedNrgMap = new HashMap<>(Map.of(
                anrg.getKey(), anrg.getValue(),
                associatedNrgDegMap.entrySet().stream().findFirst().orElseThrow().getKey(),
                associatedNrgDegMap.entrySet().stream().findFirst().orElseThrow().getValue()));
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                    .setCostAlgorithm("Restricted Shortest Path - RSP")
                    .setCostName("HOP_COUNT")
                    .setCostValue(TapiConstants.COST_HOP_VALUE)
                    .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
                .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
                .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
                .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
                .build();

            Name irgName = new NameBuilder().setValueName("irg name").setValue(irgNameValue).build();
            InterRuleGroup interRuleGroup = new InterRuleGroupBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((irgNameValue).getBytes(StandardCharsets.UTF_8)).toString()))
                .setName(Map.of(irgName.key(), irgName))
                .setRule(new HashMap<>(Map.of(rule.key(), rule)))
                .setAssociatedNodeRuleGroup(associatedNrgMap)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            irgMapp.put(new InterRuleGroupKey(interRuleGroup.getUuid()), interRuleGroup);
            srgCounter++;
        }
        return irgMapp;
    }

    /**
     * Provides a list of Mapped Service Interface Points associated with a tp/NEP and add SIPs to tapiSip List.
     * so that they can be added later on to the SIP context.
     * Returns a List of Mapped Service Interface Points.
     * @param nb The number of SIPs to be created for the NEP,
     * @param layerProtocol Layer protocol the SIP is associated to,
     * @param tpId id of the tp converted to a string,
     * @param nodeid id of the node converted to a string,
     * @param supportedInterfaceCapability A collection of interfaces the tp supports.
     * @param operState the operational State of the SIP to be created.
     * @param adminState the administrative State of the SIP to be created.
     */
    public Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createMSIP(
            int nb, LayerProtocolName layerProtocol, String tpId, String nodeid,
            Collection<SupportedInterfaceCapability> supportedInterfaceCapability,
            OperationalState operState, AdministrativeState adminState) {
        // add them to SIP context
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            String sipName = nb == 1 ? String.join("+", "SIP", nodeid, tpId)
                    : String.join("+", "SIP", nodeid, tpId, "Nber", String.valueOf(i));
            LOG.info("SIP = {}", sipName);
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes(sipName.getBytes(StandardCharsets.UTF_8)).toString());
            MappedServiceInterfacePoint msip =
                new MappedServiceInterfacePointBuilder().setServiceInterfacePointUuid(sipUuid).build();
            ServiceInterfacePoint sip =
                createSIP(sipUuid, layerProtocol, tpId, nodeid, supportedInterfaceCapability, operState, adminState);
            this.tapiSips.put(sip.key(), sip);
            msipl.put(msip.key(), msip);
            LOG.debug("SIP created {}", sip.getUuid());
            LOG.debug("This SIP corresponds to SIP+nodeId {} + TpId {}", nodeid, tpId);
        }
        return msipl;
    }

    /**
     * Provides a list of Available Payload Structure supported by a Photonic Media Node Edge Point.
     * Returns an Available Payload Structure List.
     * @param rate remaining capacity supported by the Photonic NEP,
     * @param otsiProvisioned Boolean providing information on whether 1 OTSi service has been provisioned on the port,
     * @param sicList A collection of supported interface capabilities that the tp/NEP supports.
     * @param supportedOpModes List of operational Modes (as they appear in catalog) that the tp/NEP supports.
     */
    public List<AvailablePayloadStructure> createAvailablePayloadStructureForPhtncMedia(
            String rate, Boolean otsiProvisioned,
            Collection<SupportedInterfaceCapability> sicList,
            List<OperationalModeKey> supportedOpModes) {

        Integer nepRate = 0;
        Integer loopRate = 0;
        if (supportedOpModes != null && !supportedOpModes.isEmpty()) {
            for (OperationalModeKey operationalMode : supportedOpModes) {
                for (Map.Entry<Integer, String> entry: OPMODE_LOOPRATE_MAP.entrySet()) {
                    if (operationalMode.toString().contains(entry.getValue())) {
                        loopRate = entry.getKey();
                        break;
                    }
                }
                if (loopRate > nepRate) {
                    nepRate = loopRate;
                    if (nepRate >= OPMODE_LOOPRATE_MAX) {
                        break;
                    }
                }
            }
        } else {
            nepRate = rate == null ? 0 : Integer.parseInt(rate) / 100;
        }
        List<AvailablePayloadStructure> aps = new ArrayList<>();
        Integer cepInstanceNber = otsiProvisioned ? 0 : 1;
        for (SupportedInterfaceCapability sic : sicList) {
            switch (sic.getIfCapType().toString().split("\\{")[0]) {
                case "IfOCH":
                case "IfOCHOTU4ODU4":
                case "IfOCHOTU4ODU4Regen":
                case "IfOCHOTU4ODU4Uniregen":
                    aps.add(new AvailablePayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(PHOTONICLAYERQUALIFIEROTSi.VALUE, OTUTYPEOTU4.VALUE,
                            ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.valueOf(cepInstanceNber))
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(100.0 * cepInstanceNber, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                case "IfOCHOTUCnODUCn":
                case "IfOtsiOtucnOducn":
                case "IfOCHOTUCnODUCnRegen":
                case "IfOCHOTUCnODUCnUniregen":
                    aps.add(new AvailablePayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(PHOTONICLAYERQUALIFIEROTSi.VALUE, OTUTYPEOTUCN.VALUE,
                            ODUTYPEODUCN.VALUE, ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.valueOf(nepRate * cepInstanceNber))
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(nepRate * 100.0 * cepInstanceNber, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                default:
                    break;
            }
        }
        return aps.stream().distinct().toList();
    }

    /**
     * Provides a list of Available Payload Structure supported by a generic Node Edge Point.
     * Returns an Available Payload Structure List.
     * @param rate remaining capacity supported by the Photonic NEP,
     * @param isProvisioned Boolean providing information on whether a service has been provisioned on the port,
     * @param lpnList A list of layer protocol qualifiers that the tp/NEP supports.
     * @param nberOfInstances Number of instances the tp/NEP supports for the layer protocol qualifiers.
     */
    public List<AvailablePayloadStructure> createAvailablePayloadStructureForCommonNeps(
            Boolean isProvisioned, Double rate, int nberOfInstances, Set<LAYERPROTOCOLQUALIFIER> lpnList) {
        List<AvailablePayloadStructure> aps = new ArrayList<>();
        aps.add(new AvailablePayloadStructureBuilder()
            .setMultiplexingSequence(lpnList)
            .setNumberOfCepInstances(Uint64.valueOf(nberOfInstances))
            .setCapacity(
                new CapacityBuilder()
                    .setUnit(CAPACITYUNITGBPS.VALUE)
                    //.setValue(Decimal64.valueOf((rate * (isProvisioned ? 0 : 1)), RoundingMode.DOWN))
                    .setValue(Decimal64.valueOf(rate, RoundingMode.DOWN))
                    .build())
            .build());

        return aps;
    }

    /**
     * Provides a list of Supported Payload Structure supported by a Photonic Media Node Edge Point.
     * Returns a Supported Payload Structure List.
     * @param rate Rounded maximum rate supported by the Photonic NEP,
     * @param sicList A collection of supported interface capabilities that the tp/NEP supports.
     * @param supportedOpModes List of operational Modes (as they appear in catalog) that the tp/NEP supports.
     */
    public List<SupportedPayloadStructure> createSupportedPayloadStructureForPhtncMedia(String rate,
            Collection<SupportedInterfaceCapability> sicList, List<OperationalModeKey> supportedOpModes) {
        Integer nepRate = 0;
        Integer loopRate = 0;
        if (supportedOpModes != null && !supportedOpModes.isEmpty()) {
            for (OperationalModeKey operationalMode : supportedOpModes) {
                for (Map.Entry<Integer, String> entry: OPMODE_LOOPRATE_MAP.entrySet()) {
                    if (operationalMode.toString().contains(entry.getValue())) {
                        loopRate = entry.getKey();
                        break;
                    }
                }
                if (loopRate > nepRate) {
                    nepRate = loopRate;
                    if (nepRate >= OPMODE_LOOPRATE_MAX) {
                        break;
                    }
                }
            }
        } else {
            nepRate = rate == null ? 0 : Integer.parseInt(rate) / 100;
        }

        List<SupportedPayloadStructure> sps = new ArrayList<>();
        for (SupportedInterfaceCapability sic : sicList) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            switch (ifCapType) {
                case "IfOCHOTU4ODU4":
                case "IfOCHOTU4ODU4Regen":
                case "IfOCHOTU4ODU4Uniregen":
                    sps.add(new SupportedPayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(PHOTONICLAYERQUALIFIEROTSi.VALUE, ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.ONE)
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(100.0, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                case "IfOCHOTUCnODUCn":
                case "IfOtsiOtucnOducn":
                case "IfOCHOTUCnODUCnRegen":
                case "IfOCHOTUCnODUCnUniregen":
                    sps.add(new SupportedPayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(
                            PHOTONICLAYERQUALIFIEROTSi.VALUE, OTUTYPEOTUCN.VALUE,
                            ODUTYPEODUCN.VALUE, ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.valueOf(nepRate))
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(nepRate * 100.0, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                default:
                    break;
            }
        }
        return sps.stream().distinct().toList();
    }

    /**
     * Provides a list of Supported Payload Structure supported by a Generic Node Edge Point.
     * Returns a Supported Payload Structure List.
     * @param isProvisioned Boolean providing information on whether a service has been provisioned on the port,
     * @param rate Rounded maximum rate supported by the Photonic NEP,
     * @param nberOfInstances Number of instances the tp/NEP supports for the layer protocol qualifiers,
     * @param lpnList A list of layer protocol qualifiers that the tp/NEP supports.
     */
    public List<SupportedPayloadStructure> createSupportedPayloadStructureForCommonNeps(
            Boolean isProvisioned, Double rate, int nberOfInstances, Set<LAYERPROTOCOLQUALIFIER> lpnList) {
        List<SupportedPayloadStructure> sps = new ArrayList<>();
        sps.add(new SupportedPayloadStructureBuilder()
            .setMultiplexingSequence(lpnList)
            .setNumberOfCepInstances(Uint64.valueOf(nberOfInstances))
            .setCapacity(
                new CapacityBuilder()
                    .setUnit(CAPACITYUNITGBPS.VALUE)
                    .setValue(Decimal64.valueOf(rate, RoundingMode.DOWN))
                    .build())
            .build());

        return sps.stream().distinct().toList();
    }

    /**
     * Provides TotalSize for a Generic Node Edge Point whether it is for total-potential or available capacity.
     * Returns a Total Size.
     * @param rate The rate to be set in either total-potential or available capacity,
     */
    public TotalSize createTotalSizeForCommonNeps(Double rate) {
        return new TotalSizeBuilder()
            .setUnit(CAPACITYUNITGBPS.VALUE)
            .setValue(Decimal64.valueOf(rate, RoundingMode.DOWN))
            .build();
    }

    /**
     * Creates Connection End Point for ROADMs.
     * Returns a Connection End Point.
     * @param lowerFreqIndex Index of the slot corresponding to lower boundary of the spectrum occupied by the channel
     *                       in the flex-grid,
     * @param higherFreqIndex Index of the slot corresponding to higher boundary of the spectrum occupied by the channel
     *                       in the flex-grid,
     * @param id OpenROADM tpId,
     * @param qualifier Layer protocol qualifier used to process Nep and Cep Name,
     * @param omCepSpec Pre-processed Ots Media Connection End Point Spec to be included to the Cep (includes some
     *                  parameters required for impairment aware path computation),
     */
    public ConnectionEndPoint createCepRoadm(int lowerFreqIndex, int higherFreqIndex, String id, String qualifier,
        OtsMediaConnectionEndPointSpec omCepSpec, boolean srg) {
        String nepId = String.join("+", id.split("\\+")[0], qualifier, id.split("\\+")[1]);
        String nodeNepId = String.join("+",id.split("\\+")[0], TapiConstants.PHTNC_MEDIA);
        String extendedNepId = lowerFreqIndex == 0 && higherFreqIndex == 0
            ? nepId
            : String.join("-",nepId, ("[" + lowerFreqIndex + "-" + higherFreqIndex + "]"));
        LOG.info("NEP = {}", nepId);
        Name cepName = new NameBuilder()
            .setValueName("ConnectionEndPoint name")
            .setValue(String.join("+", "CEP", extendedNepId))
            .build();
        ParentNodeEdgePoint pnep = new ParentNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nepId.getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nodeNepId.getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setTopologyUuid(this.tapiTopoUuid)
            .build();
        String clientQualifier = "";

        switch (qualifier) {
            case TapiConstants.PHTNC_MEDIA_OTS:
                clientQualifier = TapiConstants.PHTNC_MEDIA_OMS;
                break;
            case TapiConstants.PHTNC_MEDIA_OMS:
                clientQualifier = TapiConstants.MC;
                break;
            case TapiConstants.MC:
                clientQualifier = TapiConstants.OTSI_MC;
                break;
            default:
                LOG.debug("not currently handling client NEP for OTSiMC CEP {}", nepId);
                break;
        }
        // If the CEP is OTS and created for an SRG, we force client qualifier to MC as there is no OMS on PPs
        clientQualifier = srg && qualifier.equals(TapiConstants.PHTNC_MEDIA_OTS)
            ? TapiConstants.MC
            : clientQualifier;
        ClientNodeEdgePoint cnep = new ClientNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", id.split("\\+")[0], clientQualifier, id.split("\\+")[1]))
                        .getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nodeNepId.getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setTopologyUuid(this.tapiTopoUuid)
            .build();
        // TODO: add augmentation with the corresponding cep-spec (i.e. MC, OTSiMC...)
        ConnectionEndPoint2Builder cep2builder = new ConnectionEndPoint2Builder();
        ConnectionEndPointBuilder cepBldr = new ConnectionEndPointBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "CEP", extendedNepId)).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setParentNodeEdgePoint(pnep)
            .setName(Map.of(cepName.key(), cepName))
            .setConnectionPortRole(PortRole.SYMMETRIC)
            .setDirection(Direction.BIDIRECTIONAL)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
        switch (qualifier) {
            case TapiConstants.PHTNC_MEDIA_OTS:
                cepBldr.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE);
                if (omCepSpec != null) {
                    cepBldr.addAugmentation(cep2builder.setOtsMediaConnectionEndPointSpec(omCepSpec).build());
                    LOG.info("In ConverTORToTapiTopology LINE599, add Augment to cep {}", cepBldr.build());
                }
                break;
            case TapiConstants.PHTNC_MEDIA_OMS:
                cepBldr.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROMS.VALUE);
                break;
            case TapiConstants.MC:
                cepBldr.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIERMC.VALUE);
                break;
            case TapiConstants.OTSI_MC:
                cepBldr.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE);
                break;
            default:
                break;
        }

        return TapiConstants.OTSI_MC.equals(qualifier)
            ? cepBldr.build()
            : cepBldr.setClientNodeEdgePoint(Map.of(cnep.key(), cnep)).build();
    }

    public ConnectionEndPoint createOTSCepXpdr(String nepId) {
//        String nepId = String.join("+", id.split("\\+")[0], TapiStringConstants.PHTNC_MEDIA_OTS, id.split("\\+")[1]);
//        String nepNodeId = String.join("+",id.split("\\+")[0], TapiStringConstants.XPDR);
        String nepNodeId = String.join("+",nepId.split("\\+")[0], TapiConstants.XPDR);
        LOG.debug("ConvertORToTapiTopology 722 CreateOTSCepXpdr for Nep {}", nepId);
        Name cepName = new NameBuilder()
            .setValueName("ConnectionEndPoint name")
            .setValue(String.join("+", "CEP", nepId))
            .build();
        ParentNodeEdgePoint pnep = new ParentNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nepId.getBytes(StandardCharsets.UTF_8)).toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nepNodeId.getBytes(StandardCharsets.UTF_8)).toString()))
            .setTopologyUuid(this.tapiTopoUuid)
            .build();
//        String clientQualifier = TapiStringConstants.OTSI_MC;
        ClientNodeEdgePoint cnep = new ClientNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
//                (String.join("+", id.split("\\+")[0], clientQualifier, id.split("\\+")[1]))
                String.join("+", nepId.split("\\+" + TapiConstants.PHTNC_MEDIA_OTS)[0], TapiConstants.OTSI_MC,
                    nepId.split((TapiConstants.PHTNC_MEDIA_OTS + "\\+"))[1])
                    .getBytes(StandardCharsets.UTF_8)).toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nepNodeId.getBytes(StandardCharsets.UTF_8)).toString()))
            .setTopologyUuid(this.tapiTopoUuid)
            .build();

        ConnectionEndPointBuilder cepBldr = new ConnectionEndPointBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", nepId))
                    .getBytes(StandardCharsets.UTF_8)).toString()))
            .setParentNodeEdgePoint(pnep)
            .setName(Map.of(cepName.key(), cepName))
            .setConnectionPortRole(PortRole.SYMMETRIC)
            .setDirection(Direction.BIDIRECTIONAL)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
            .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE);
        return cepBldr.setClientNodeEdgePoint(Map.of(cnep.key(), cnep)).build();
    }

    /**
     * Provides a list of the Cep Layer Protocol Qualifier Instances that are Supported by Node Edge Point.
     * Returns a List of Supported Cep Layer Protocol Qualifier Instances.
     * @param sicList A collection of supported interface capabilities that the tp/NEP supports.
     * @param lpn Layer protocol qualifier that the tp/NEP supports.
     * @param key String key used to refine the case of Digital-OTN that does not separate OTU from ODU case.
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SF_SWITCH_FALLTHROUGH",
        justification = "Voluntarily No break in switchcase where comment is inserted in following method")
    public List<SupportedCepLayerProtocolQualifierInstances> createSupportedCepLayerProtocolQualifier(
            Collection<SupportedInterfaceCapability> sicList, LayerProtocolName lpn, String key) {
        if (sicList == null) {
            return new ArrayList<>(List.of(
                new SupportedCepLayerProtocolQualifierInstancesBuilder()
                    .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                    .setNumberOfCepInstances(Uint64.ONE)
                    .build()));
        }
        LOG.debug("SIC list = {}", sicList);
        List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        String lpnName = lpn.getName();
        // Try to manage transition from OTU and ODU to DIGITAL_OTN which is still not fully achieved in TAPI
        if (lpn.equals(LayerProtocolName.DIGITALOTN) && key.equals("OTU")) {
            lpnName = "OTU";
        } else if (lpn.equals(LayerProtocolName.DIGITALOTN) && key.equals("ODU")) {
            lpnName = "ODU";
        }
        for (SupportedInterfaceCapability sic : sicList) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            if (!LPN_MAP.containsKey(lpnName)) {
                LOG.error("Layer Protocol Name is unknown {}", lpnName);
                break;
            }
            var ifCapTypeMap = LPN_MAP.get(lpnName);
            if (!ifCapTypeMap.containsKey(ifCapType)) {
                LOG.error("IfCapability type {} not managed", ifCapType);
                break;
            }
            for (LAYERPROTOCOLQUALIFIER qualifier: ifCapTypeMap.get(ifCapType).keySet()) {
                sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                    .setLayerProtocolQualifier(qualifier)
                    .setNumberOfCepInstances(Uint64.ONE)
                    .build());
            }
        }
        if (key.contains(TapiConstants.OTSI_MC)) {
            sclpqiList = sclpqiList.stream()
                .filter(sclpqi -> !sclpqi.getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIEROTS.VALUE))
                .collect(Collectors.toList());
        }
        return sclpqiList.stream().distinct().toList();
    }

    /**
     * Retrieves from OpenROADM tp the information on the wavelength used (when a service is provisioned).
     * Returns a Map of Min and Max Frequency corresponding to occupied-slot low and high boundaries.
     * @param tp OpenROADM Termination Point (ietf/openROADM topology Object).
     */
    public Map<Frequency, Frequency> getXpdrUsedWavelength(TerminationPoint tp) {
        var tpAug = tp.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1.class);
        if (tpAug == null) {
            return new HashMap<>();
        }
        XpdrNetworkAttributes xnatt = tpAug.getXpdrNetworkAttributes();
        if (xnatt == null) {
            return new HashMap<>();
        }
        var xnattWvlgth = xnatt.getWavelength();
        if (xnattWvlgth == null) {
            return new HashMap<>();
        }
        var freq = xnattWvlgth.getFrequency();
        if (freq == null) {
            return new HashMap<>();
        }
        var width = xnattWvlgth.getWidth();
        if (width == null) {
            return new HashMap<>();
        }
        Double centerFrequencyTHz = freq.getValue().doubleValue();
        Double widthGHz = width.getValue().doubleValue();
        return rangeFactory.range(centerFrequencyTHz, widthGHz).ranges();
    }

    /**
     * Retrieves from OpenROADM tp (ROADM SRG-PP)the information on the wavelength used on the tp.
     * Returns a Map of Min and Max Frequency corresponding to the different occupied-slots low and high boundaries.
     * @param terminationPoint OpenROADM Termination Point (ietf/openROADM topology Object),
     */
    public Map<Frequency, Frequency> getPPUsedFrequencies(TerminationPoint terminationPoint) {
        return getPP11UsedFrequencies(
                terminationPoint.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology
                                .rev250110.TerminationPoint1.class
                )
        );
    }


    /**
     * Retrieves from OpenROADM tp (ROADM DEG-TTP)the information on the wavelength provisioned in the MW interface.
     * Returns a Map of Min and Max Frequency corresponding to the different occupied-slots low and high boundaries.
     * @param tp OpenROADM Termination Point (ietf/openROADM topology Object),
     */
    public Range getTTPUsedFreqMap(TerminationPoint tp) {
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        var termPoint1 = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
            .TerminationPoint1.class);
        if (termPoint1 == null) {
            return new SortedRange();
        }
        TxTtpAttributes txttpAtt = termPoint1.getTxTtpAttributes();
        if (txttpAtt  == null) {
            return new SortedRange();
        }
        var txttpAttUsedWvl = txttpAtt.getUsedWavelengths();
        if (txttpAttUsedWvl == null || txttpAttUsedWvl.isEmpty()) {
            var txttpAttAvlFreqMaps = txttpAtt.getAvailFreqMaps();
            if (txttpAttAvlFreqMaps == null || !txttpAttAvlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
                return new SortedRange();
            }
            byte[] freqByteSet = new byte[GridConstant.NB_OCTECTS];
            LOG.debug("Creation of Bitset {}", freqByteSet);
            AvailFreqMapsKey availFreqMapsKey = new AvailFreqMapsKey(GridConstant.C_BAND);
            freqByteSet = txttpAttAvlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().equals(availFreqMapsKey))
                .findFirst().orElseThrow().getValue().getFreqMap();

            LOG.debug("TTP used frequency byte set ({} bytes, 0 represents 8 used frequencies): {} ",
                    freqByteSet.length,
                    freqByteSet
            );
            Available bitMap = new AvailableGrid(freqByteSet);

            LOG.debug("TTP used frequency bit set (min=0, max={}, each number represents a used frequency): {} ",
                    GridConstant.EFFECTIVE_BITS,
                    bitMap.assignedFrequencies()
            );
            Map<Double, Double> assignedFrequencyRanges = numericFrequency.assignedFrequency(bitMap);
            LOG.info("TTP used frequency map {}", assignedFrequencyRanges);

            return new SortedRange(assignedFrequencyRanges);

        }
        Range range = new SortedRange();
        for (Map.Entry<UsedWavelengthsKey, UsedWavelengths> usedLambdas : txttpAttUsedWvl.entrySet()) {
            Double centFreq = usedLambdas.getValue().getFrequency().getValue().doubleValue();
            Double width = usedLambdas.getValue().getWidth().getValue().doubleValue();
            range.add(centFreq, width, frequencyFactory);
        }
        return range;
    }

    /**
     * Retrieves the BitMap containing the information on spectrum used on the ROADM-TTP MW interface from tp.
     * Returns a Map of Min and Max Frequency corresponding to the different occupied-slots low and high boundaries.
     * @param tp OpenROADM Termination Point (ietf/openROADM topology Object),
     */
    public Range getTTPAvailableFreqMap(TerminationPoint tp) {
        var termPoint1 = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
            .TerminationPoint1.class);
        if (termPoint1 == null) {
            return new SortedRange();
        }
        TxTtpAttributes txttpAtt = termPoint1.getTxTtpAttributes();
        if (txttpAtt == null) {
            return new SortedRange();
        }
        var avlFreqMaps = txttpAtt.getAvailFreqMaps();
        if (avlFreqMaps == null || !avlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
            return new SortedRange();
        }
        byte[] freqByteSet = new byte[GridConstant.NB_OCTECTS];
        LOG.debug("Creation of Bitset {}", freqByteSet);
        AvailFreqMapsKey availFreqMapsKey = new AvailFreqMapsKey(GridConstant.C_BAND);
        freqByteSet = avlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().equals(availFreqMapsKey))
                .findFirst().orElseThrow().getValue().getFreqMap();

        LOG.debug("TTP available frequency byte set ({} bytes, 0 represents 8 available frequencies): {} ",
                freqByteSet.length,
                freqByteSet
        );
        Available bitMap = new AvailableGrid(freqByteSet);

        LOG.debug("TTP available frequency bit set (min=0, max={}, each number represents an available frequency): {}",
                GridConstant.EFFECTIVE_BITS,
                bitMap.availableFrequencies()
        );
        Map<Double, Double> availableFrequencyRanges = numericFrequency.availableFrequency(bitMap);

        LOG.info("TTP available frequency map {}", availableFrequencyRanges);
        return new SortedRange(availableFrequencyRanges);

    }


    /**
     * Retrieves the BitMap containing the information on spectrum used on the ROADM-TTP MW interface.
     * Done from TerminationPoint1 Augmentation.
     * Returns a Map of Min and Max Frequency corresponding to the different occupied-slots low and high boundaries.
     * @param tp OpenROADM Termination Point (ietf/openROADM topology Object)
     */
    public Range getTTP11AvailableFreqMap(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1 tp) {
        if (tp == null) {
            return new SortedRange();
        }
        TxTtpAttributes txttpAtt = tp.getTxTtpAttributes();
        if (txttpAtt == null) {
            return new SortedRange();
        }
        var avlFreqMaps = txttpAtt.getAvailFreqMaps();
        if (avlFreqMaps == null || !avlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
            return new SortedRange();
        }
        byte[] freqByteSet = new byte[GridConstant.NB_OCTECTS];
        LOG.debug("Creation of Bitset {}", freqByteSet);
        AvailFreqMapsKey availFreqMapsKey = new AvailFreqMapsKey(GridConstant.C_BAND);
        freqByteSet = avlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().equals(availFreqMapsKey))
                .findFirst().orElseThrow().getValue().getFreqMap();

        LOG.debug("TTP11 available frequency byte set ({} bytes, 0 represents 8 available frequencies): {} ",
                freqByteSet.length,
                freqByteSet
        );
        Available bitMap = new AvailableGrid(freqByteSet);

        LOG.debug("TTP11 available frequency bit set (min=0, max={}, "
                        + "each number represents an available frequency): {}",
                GridConstant.EFFECTIVE_BITS,
                bitMap.availableFrequencies()
        );
        Map<Double, Double> availableFrequencyRanges = numericFrequency.availableFrequency(bitMap);

        LOG.info("TTP11 available frequency map {}", availableFrequencyRanges);
        return new SortedRange(availableFrequencyRanges);
    }

    /**
     * Retrieves from OpenROADM tp (ROADM SRG-PP) the information on the wavelength available on the tp.
     * Done directly from TerminationPoint1 Augmentation.
     * Returns a map of Min and Max Frequency corresponding to the different occupied-slots low and high boundaries.
     * @param tp OpenROADM Termination Point (ietf/openROADM topology Object),
     */
    public Map<Frequency, Frequency> getPP11AvailableFrequencies(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1 tp) {
        if (tp == null) {
            return Map.of();
        }

        PpAttributes ppAtt = tp.getPpAttributes();
        if (ppAtt == null) {
            return Map.of();
        }

        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMaps = ppAtt.getAvailFreqMaps();
        AvailFreqMapsKey cband = new AvailFreqMapsKey(GridConstant.C_BAND);

        return new SortedRange(availableRanges(availFreqMaps, cband)).ranges();
    }

    /**
     * Computes the available frequency ranges for a given band.
     *
     * <p>The method expects a map of available frequency maps (typically keyed by band),
     * retrieves the {@link AvailFreqMaps} entry for {@code bandKey} and converts
     * it into numeric ranges.
     *
     * <p>If the input map is {@code null}, the key is missing, or the entry/frequency map
     * is {@code null}, this method returns an empty map.
     *
     * @param avlFreqMaps map containing available frequency maps per band; may be {@code null}
     * @param bandKey the key representing the band to extract
     * @return a map of numeric frequency ranges (e.g., start → end), or an empty map if unavailable
     */
    private Map<Double, Double> availableRanges(
            Map<AvailFreqMapsKey, AvailFreqMaps> avlFreqMaps,
            AvailFreqMapsKey bandKey) {

        if (!hasBand(avlFreqMaps, bandKey)) {
            return Map.of();
        }

        AvailFreqMaps afm = avlFreqMaps.get(bandKey);
        byte[] freqByteSet = Arrays.copyOf(afm.getFreqMap(), GridConstant.NB_OCTECTS);
        return numericFrequency.availableFrequency(new AvailableGrid(freqByteSet));
    }

    /**
     * Checks whether the provided map contains a non-null entry for {@code bandKey}
     * and that the entry has a non-null frequency bitmap.
     *
     * @param avlFreqMaps map containing available frequency maps per band; may be {@code null}
     * @param bandKey the key representing the band to validate
     * @return {@code true} if {@code avlFreqMaps} contains a non-null {@link AvailFreqMaps}
     */
    private static boolean hasBand(Map<AvailFreqMapsKey, AvailFreqMaps> avlFreqMaps, AvailFreqMapsKey bandKey) {
        if (avlFreqMaps == null) {
            return false;
        }

        AvailFreqMaps afm = avlFreqMaps.get(bandKey);
        return afm != null && afm.getFreqMap() != null;
    }

    /**
     * Retrieves from OpenROADM tp (ROADM SRG-PP)the information on the wavelength used on the tp.
     * Done directly from TerminationPoint1 Augmentation.
     * Returns a map of Min and Max Frequency corresponding to the different occupied-slots low and high boundaries.
     * @param tp OpenROADM Termination Point (ietf/openROADM topology Object),
     */
    public Map<Frequency, Frequency> getPP11UsedFrequencies(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1 tp) {
        SortedRange sortedRange = new SortedRange();
        if (tp == null) {
            return new HashMap<>();
        }

        Map<AvailFreqMapsKey, AvailFreqMaps> avlFreqMaps = new HashMap<>();
        PpAttributes ppAtt = tp.getPpAttributes();
        if (ppAtt != null) {
            avlFreqMaps = ppAtt.getAvailFreqMaps();
        }

        if (avlFreqMaps != null && avlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
            byte[] freqByteSet = new byte[GridConstant.NB_OCTECTS];

            AvailFreqMapsKey availFreqMapsKey = new AvailFreqMapsKey(GridConstant.C_BAND);
            freqByteSet = Arrays.copyOf(avlFreqMaps.entrySet().stream()
                    .filter(afm -> afm.getKey().equals(availFreqMapsKey))
                    .findFirst()
                    .orElseThrow()
                    .getValue()
                    .getFreqMap(), freqByteSet.length);
            LOG.debug("Available frequency byte set: {}", freqByteSet);

            Map<Double, Double> ranges = numericFrequency.assignedFrequency(new AvailableGrid(freqByteSet));
            LOG.debug("Used frequency ranges: {}", ranges);

            sortedRange.add(new SortedRange(ranges));
        }

        return sortedRange.ranges();
    }

    /**
     * Retrieves from OpenROADM tp (ROADM SRG-PP)the information on the available spectrum (BitMap) on the tp.
     * Done directly from TerminationPoint1 Augmentation.
     * Returns a Map of Min and Max Frequency corresponding to the different occupied-slots low and high boundaries.
     * @param tp OpenROADM Termination Point (ietf/openROADM topology Object),
     */
    public Range getTTP11UsedFreqMap(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1 tp) {
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        if (tp == null) {
            return new SortedRange();
        }
        TxTtpAttributes txttpAtt = tp.getTxTtpAttributes();
        if (txttpAtt == null) {
            return new SortedRange();
        }
        var txttpAttUsedWvl = txttpAtt.getUsedWavelengths();
        if (txttpAttUsedWvl == null || txttpAttUsedWvl.isEmpty()) {
            var txttpAttAvlFreqMaps = txttpAtt.getAvailFreqMaps();
            if (txttpAttAvlFreqMaps == null || !txttpAttAvlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
                return new SortedRange();
            }
            byte[] freqByteSet = new byte[GridConstant.NB_OCTECTS];
            LOG.debug("Creation of Bitset {}", freqByteSet);
            AvailFreqMapsKey availFreqMapsKey = new AvailFreqMapsKey(GridConstant.C_BAND);
            freqByteSet = txttpAttAvlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().equals(availFreqMapsKey))
                .findFirst().orElseThrow().getValue().getFreqMap();

            LOG.debug("TTP11 used frequency byte set ({} bytes, 0 represents 8 used frequencies): {} ",
                    freqByteSet.length,
                    freqByteSet
            );
            Available bitMap = new AvailableGrid(freqByteSet);

            LOG.debug("TTP11 used frequency bit set (min=0, max={}, each number represents a used frequency): {}",
                    GridConstant.EFFECTIVE_BITS,
                    bitMap.assignedFrequencies()
            );
            Map<Double, Double> assignedFrequencyRanges = numericFrequency.assignedFrequency(bitMap);

            LOG.info("TTP11 used frequency map {}", assignedFrequencyRanges);
            return new SortedRange(assignedFrequencyRanges);
        }
        return getRange(txttpAttUsedWvl);
    }

    private Range getRange(Map<UsedWavelengthsKey, UsedWavelengths> txttpAttUsedWvl) {
        Range range = new SortedRange();
        for (Map.Entry<UsedWavelengthsKey, UsedWavelengths> usedLambdas : txttpAttUsedWvl.entrySet()) {
            var usedLambdasValue = usedLambdas.getValue();
            Double centFreq = usedLambdasValue.getFrequency().getValue().doubleValue();
            Double width = usedLambdasValue.getWidth().getValue().doubleValue();
            range.add(centFreq, width, frequencyFactory);
        }
        return range;
    }

    /**
     * Adds the Payload Structure and the Photonic Node Edge Point Spec to an OwnedNodeEdgePointBuilder.
     * Returns  Augmented OnepBuilder provided as an input.
     * @param nodeId OpenROADM node Id.
     * @param freqMap Map of Min/Max Frequency corresponding to the different occupied-slots low/high boundaries.
     * @param operModeList List of Keys of the operational modes supported by the NEP.
     * @param sicColl A collection of supported interface capabilities that the tp/NEP supports.
     * @param onepBldr The onepBuilder of the NEP to augment.
     * @param keyword Concatenation of the nodeId and the layer protocol qualifier.
     */
    public OwnedNodeEdgePointBuilder addPayloadStructureAndPhotSpecToOnep(String nodeId, String rate,
            Map<Frequency, Frequency> freqMap, List<OperationalModeKey> operModeList,
            Collection<SupportedInterfaceCapability> sicColl, OwnedNodeEdgePointBuilder onepBldr, String keyword) {
        if (!String.join("+", nodeId, TapiConstants.OTSI_MC).equals(keyword)
                && !String.join("+", nodeId, TapiConstants.PHTNC_MEDIA_OTS).equals(keyword)) {
            return onepBldr;
        }
        LOG.debug("Entering LOOP Step1");
        // Creating OTS & OTSI_MC NEP specific attributes
        onepBldr.setSupportedPayloadStructure(
            createSupportedPayloadStructureForPhtncMedia(rate, sicColl,operModeList));
        onepBldr.setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                createTotalSizeForCommonNeps(Double.valueOf(rate))).build());
        SpectrumCapabilityPacBuilder spectrumPac = new SpectrumCapabilityPacBuilder();
        OccupiedSpectrumBuilder ospecBd = new OccupiedSpectrumBuilder();
        Frequency lowSupFreq = new TeraHertz(GridConstant.START_EDGE_FREQUENCY_THZ);
        Frequency upSupFreq =  frequencyFactory.frequency(
                GridConstant.START_EDGE_FREQUENCY_THZ,
                GridConstant.GRANULARITY,
                GridConstant.EFFECTIVE_BITS
        );
        if (freqMap == null || freqMap.isEmpty()) {
//                TODO: verify if we need to fill OcupiedSpectrum as follows when no lambda provisioned
//                ospecBd
//                    .setUpperFrequency(Uint64.ZERO)
//                    .setLowerFrequency(Uint64.ZERO);
            onepBldr.setAvailablePayloadStructure(
                createAvailablePayloadStructureForPhtncMedia(rate, false, sicColl,operModeList));
            onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                createTotalSizeForCommonNeps(Double.valueOf(rate))).build());
            AvailableSpectrum  aspec = new AvailableSpectrumBuilder()
                .setLowerFrequency(lowSupFreq.hertz())
                .setUpperFrequency(upSupFreq.hertz())
                .build();
            spectrumPac.setAvailableSpectrum(
                new HashMap<AvailableSpectrumKey, AvailableSpectrum>(Map.of(
                    new AvailableSpectrumKey(aspec.getLowerFrequency(), aspec.getUpperFrequency()), aspec)));
        } else {
            LOG.debug("Entering LOOP Step2");
            onepBldr.setAvailablePayloadStructure(
                createAvailablePayloadStructureForPhtncMedia(rate, true, sicColl,operModeList));
            onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                createTotalSizeForCommonNeps(Double.valueOf(0.0))).build());
            for (Map.Entry<Frequency, Frequency> frequency : freqMap.entrySet()) {
                ospecBd
                    .setLowerFrequency(frequency.getKey().hertz())
                    .setUpperFrequency(frequency.getValue().hertz());
            }
            OccupiedSpectrum ospec = ospecBd.build();
            spectrumPac.setOccupiedSpectrum(
                new HashMap<OccupiedSpectrumKey, OccupiedSpectrum>(Map.of(
                    new OccupiedSpectrumKey(ospec.getLowerFrequency(), ospec.getUpperFrequency()), ospec)));
        }
        LOG.debug("Entering LOOP Step3");
        SupportableSpectrum  sspec = new SupportableSpectrumBuilder()
            .setLowerFrequency(lowSupFreq.hertz())
            .setUpperFrequency(upSupFreq.hertz())
            .build();
        spectrumPac.setSupportableSpectrum(
            new HashMap<SupportableSpectrumKey, SupportableSpectrum>(Map.of(
                new SupportableSpectrumKey(sspec.getLowerFrequency(), sspec.getUpperFrequency()), sspec)));
        LOG.debug("Entering LOOP Step4");
        var onep1 = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                .OwnedNodeEdgePoint1Builder()
            .setPhotonicMediaNodeEdgePointSpec(
                new PhotonicMediaNodeEdgePointSpecBuilder().setSpectrumCapabilityPac(spectrumPac.build()).build())
            .build();
        LOG.debug("creating Photonic NEP SPEC for node {} and nep {}", nodeId, onep1);
        onepBldr.addAugmentation(onep1);
        LOG.debug("Entering LOOP Step5");
        return onepBldr;
    }

    /**
     * Adds the Photonic Node Edge Point Spec to a ROADM OwnedNodeEdgePointBuilder.
     * Returns Augmented OnepBuilder provided as an input.
     * @param nodeId OpenROADM node Id,
     * @param usedFreqMap Map of Min/Max Frequency corresponding to the different occupied-slots low/high boundaries.
     * @param onepBldr The onepBuilder of the NEP to augment.
     * @param keyword Concatenation of the nodeId and the layer protocol qualifier.
     */
    public OwnedNodeEdgePointBuilder addPhotSpecToRoadmOnep(String nodeId,
            Map<Frequency, Frequency> usedFreqMap, Map<Frequency, Frequency> availableFreqMap,
            OwnedNodeEdgePointBuilder onepBldr, String keyword) {
        LOG.debug("Entering Add PhotSpec to Roadm, ConvertToTopology LINE 1050 , availfreqmap is {} Used FreqMap {}",
            availableFreqMap, usedFreqMap);
        if (String.join("+", nodeId, TapiConstants.PHTNC_MEDIA_OTS).equals(keyword)
                || String.join("+", nodeId, TapiConstants.PHTNC_MEDIA_OMS).equals(keyword)) {
            // Creating OTS/OMS NEP specific attributes
            SpectrumCapabilityPacBuilder spectrumPac = new SpectrumCapabilityPacBuilder();
            if ((usedFreqMap == null || usedFreqMap.isEmpty())
                    && (availableFreqMap == null || availableFreqMap.isEmpty())) {
                AvailableSpectrum  aspec = new AvailableSpectrumBuilder()
                    .setLowerFrequency(new TeraHertz(GridConstant.START_EDGE_FREQUENCY_THZ).hertz())
                    .setUpperFrequency(
                            frequencyFactory.frequency(
                                    GridConstant.START_EDGE_FREQUENCY_THZ,
                                    GridConstant.GRANULARITY,
                                    GridConstant.EFFECTIVE_BITS).hertz()
                    ).build();
                Map<AvailableSpectrumKey, AvailableSpectrum> aspecMap = new HashMap<>();
                aspecMap.put(new AvailableSpectrumKey(aspec.getLowerFrequency(),
                    aspec.getUpperFrequency()), aspec);
                spectrumPac.setAvailableSpectrum(aspecMap);
            } else {
                if (availableFreqMap != null && !availableFreqMap.isEmpty()) {
                    Map<AvailableSpectrumKey, AvailableSpectrum> aspecMap = new HashMap<>();
                    AvailableSpectrumBuilder  aspecBd = new AvailableSpectrumBuilder();
                    for (Map.Entry<Frequency, Frequency> frequency : availableFreqMap.entrySet()) {
                        aspecBd
                            .setLowerFrequency(frequency.getKey().hertz())
                            .setUpperFrequency(frequency.getValue().hertz());
                        AvailableSpectrum aspec = aspecBd.build();
                        aspecMap.put(new AvailableSpectrumKey(aspec.getLowerFrequency(),
                            aspec.getUpperFrequency()), aspec);
                    }
                    spectrumPac.setAvailableSpectrum(aspecMap);
                }
                if (usedFreqMap != null && !usedFreqMap.isEmpty()) {
                    Map<OccupiedSpectrumKey, OccupiedSpectrum> ospecMap = new HashMap<>();
                    OccupiedSpectrumBuilder ospecBd = new OccupiedSpectrumBuilder();
                    for (Map.Entry<Frequency, Frequency> frequency : usedFreqMap.entrySet()) {
                        ospecBd
                            .setLowerFrequency(frequency.getKey().hertz())
                            .setUpperFrequency(frequency.getValue().hertz());
                        OccupiedSpectrum ospec = ospecBd.build();
                        ospecMap.put(new OccupiedSpectrumKey(ospec.getLowerFrequency(),
                            ospec.getUpperFrequency()), ospec);
                    }
                    spectrumPac.setOccupiedSpectrum(ospecMap);
                }
            }
            SupportableSpectrum  sspec = new SupportableSpectrumBuilder()
                .setLowerFrequency(new TeraHertz(GridConstant.START_EDGE_FREQUENCY_THZ).hertz())
                .setUpperFrequency(frequencyFactory.frequency(
                        GridConstant.START_EDGE_FREQUENCY_THZ,
                        GridConstant.GRANULARITY,
                        GridConstant.EFFECTIVE_BITS).hertz()
                ).build();
            Map<SupportableSpectrumKey, SupportableSpectrum> sspecMap = new HashMap<>();
            sspecMap.put(new SupportableSpectrumKey(sspec.getLowerFrequency(),
                sspec.getUpperFrequency()), sspec);
            spectrumPac.setSupportableSpectrum(sspecMap);
            PhotonicMediaNodeEdgePointSpec pnepSpec = new PhotonicMediaNodeEdgePointSpecBuilder()
                .setSpectrumCapabilityPac(spectrumPac.build())
                .build();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1 onep1 =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                        .OwnedNodeEdgePoint1Builder()
                    .setPhotonicMediaNodeEdgePointSpec(pnepSpec)
                    .build();
            onepBldr.addAugmentation(onep1);
            LOG.debug("Add Photonic Node Edge point Spec to {} including available Spectrum {} = ",
                onepBldr.getName(),
                onep1.getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac().getAvailableSpectrum());
        }
        return onepBldr;
    }

    /**
     * Create an OpenROADM Odu Switching pool for 100G transponder that rely on a connection map.
     * @param OduSwitchingPools OduSwitchingPool returned by the method.
     */
    private OduSwitchingPools createOduSwitchingPoolForTp100G() {
        Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
        int count = 1;
        for (TerminationPoint tp : this.oorNetworkPortList) {
            Set<TpId> nblTpId = new HashSet<>();
            nblTpId.addAll(tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110
                .TerminationPoint1.class).getAssociatedConnectionMapTp());
            nblTpId.add(tp.getTpId());
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(count))
                .setTpList(new HashSet<>(nblTpId))
                .build();
            nblMap.put(nbl.key(), nbl);
            count++;
        }
        return new OduSwitchingPoolsBuilder()
            .setNonBlockingList(nblMap)
            .setSwitchingPoolNumber(Uint16.ONE)
            .setSwitchingPoolType(SwitchingPoolTypes.Blocking)
            .build();
    }

    /**
     * Create Tapi Node from class parameters setting automatically some mandatory default parameters.
     * @param nodeName Map of NameKey and Name provided as an input of the method.
     * @param layerProtocols Set of layer protocol names supported by the Node.
     */
    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node createTapiNode(
            Map<NameKey, Name> nodeNames, Set<LayerProtocolName> layerProtocols) {
        Uuid nodeUuid = null;
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        if (layerProtocols.contains(LayerProtocolName.DSR)
                || layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA)) {
//            Rule rule = new RuleBuilder()
//                .setLocalId("forward")
//                .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
//                .setRuleType(new HashSet<>(Set.of(RuleType.FORWARDING)))
//                .build();
            nodeUuid = getNodeUuid4Dsr(onepl, nodeRuleGroupMap);
        } else {
            var nodeName = nodeNames.get(nodeNames.keySet().iterator().next());
            LOG.error("Undefined LayerProtocolName for {} node {}", nodeName.getValueName(), nodeName.getValue());
        }
     // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(nodeNames)
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(transformAsToTapiAdminState(this.ietfNodeAdminState.getName()))
            .setOperationalState(transformOsToTapiOperationalState(this.ietfNodeOperState.getName()))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepl)
            .setInterRuleGroup(irgMap)
            .setNodeRuleGroup(nodeRuleGroupMap)
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskParameterPac(
                new RiskParameterPacBuilder()
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .build())
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    /**
     * Main method used to populate and create the Node Neps, node rule groups of a Xponder.
     * Done scanning all OpenROADM termination points and switching pool.
     * Returns the Uuid of the Node returned by the method in case of successfull Nep and NRG creation.
     * @param onepList A map of owned node edge point filled scanning the Node OpenROADM tps.
     * @param nodeRuleGroupList A map of Node Rule Group filled scanning the Node Odu Switching Pool.
     * @param ruleList Map of Rules to be used for the creation of the NRGs.
     */
    private Uuid getNodeUuid4Dsr(
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
            Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList) {
        // client NEP DSR creation on DSR/ODU node
        List<OwnedNodeEdgePoint> onepList = new ArrayList<>();
        for (int i = 0; i < oorClientPortList.size(); i++) {
            String nodeIdDsr = String.join("+",
                this.ietfNodeId, TapiConstants.DSR, oorClientPortList.get(i).getTpId().getValue());
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(nodeIdDsr.getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdDsr, nepUuid);
            this.uuidMap.put(nodeIdDsr, nepUuid);
            Name name = new NameBuilder()
                .setValue(nodeIdDsr)
                .setValueName(OpenroadmNodeType.TPDR.equals(this.ietfNodeType) ? "100G-tpdr" : "NodeEdgePoint_C")
                .build();
            onepList.addAll(createNep(
                oorClientPortList.get(i),
                Map.of(name.key(), name),
                LayerProtocolName.DSR, LayerProtocolName.DSR, true,
                String.join("+", this.ietfNodeId, TapiConstants.DSR)));
        }
        // CLIENT NEP E_ODU creation on DSR/ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            String nodeIdEodu = String.join("+",
                this.ietfNodeId, TapiConstants.E_ODU, oorClientPortList.get(i).getTpId().getValue());
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(nodeIdEodu.getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdEodu, nepUuid1);
            this.uuidMap.put(nodeIdEodu, nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint_N")
                .setValue(nodeIdEodu)
                .build();
            onepList.addAll(createNep(
                oorClientPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, false,
                String.join("+", this.ietfNodeId, TapiConstants.E_ODU)));
        }
        // NETWORK NEPs I_ODU creation on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            String nodeIdIodu = String.join("+",
                this.ietfNodeId, TapiConstants.I_ODU, oorNetworkPortList.get(i).getTpId().getValue());
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(nodeIdIodu.getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdIodu, nepUuid1);
            this.uuidMap.put(nodeIdIodu, nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint_N")
                .setValue(nodeIdIodu)
                .build();
            onepList.addAll(createNep(
                oorNetworkPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true,
                String.join("+", this.ietfNodeId, TapiConstants.I_ODU)));
        }
        // NETWORK NEP OTS network on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            String nodeIdPmOts = String.join("+",
                this.ietfNodeId, TapiConstants.PHTNC_MEDIA_OTS, oorNetworkPortList.get(i).getTpId().getValue());
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(nodeIdPmOts.getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdPmOts, nepUuid2);
            this.uuidMap.put(nodeIdPmOts, nepUuid2);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint")
                .setValue(nodeIdPmOts)
                .build();
            onepList.addAll(createNep(
                oorNetworkPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiConstants.PHTNC_MEDIA_OTS)));
        }
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            String nodeIdOtMc = String.join("+",
                this.ietfNodeId, TapiConstants.OTSI_MC, oorNetworkPortList.get(i).getTpId().getValue());
            Uuid nepUuid3 = new Uuid(UUID.nameUUIDFromBytes(nodeIdOtMc.getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdOtMc, nepUuid3);
            this.uuidMap.put(nodeIdOtMc, nepUuid3);
            Name onedName = new NameBuilder()
                .setValueName("PhotMedNodeEdgePoint")
                .setValue(nodeIdOtMc)
                .build();
            onepList.addAll(createNep(
                oorNetworkPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiConstants.OTSI_MC)));
        }
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            String nodeIdOtu = String.join("+",
                this.ietfNodeId, TapiConstants.I_OTU, oorNetworkPortList.get(i).getTpId().getValue());
            Uuid nepUuid4 = new Uuid(UUID.nameUUIDFromBytes(nodeIdOtu.getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdOtu, nepUuid4);
            this.uuidMap.put(nodeIdOtu, nepUuid4);
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint_OTU")
                .setValue(nodeIdOtu)
                .build();
            onepList.addAll(createNep(
                oorNetworkPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.DIGITALOTN, LayerProtocolName.DIGITALOTN, false,
                String.join("+", this.ietfNodeId, TapiConstants.I_OTU)));
        }
        for (OwnedNodeEdgePoint onep : onepList) {
            onepl.put(onep.key(), onep);
        }

        // create NodeRuleGroup
        String ietfXpdr = String.join("+", this.ietfNodeId, TapiConstants.XPDR);
        nodeRuleGroupList.putAll(createNodeRuleGroupForXpdrNode(this.ietfNodeId, this.oorOduSwitchingPool));

        return this.uuidMap.get(ietfXpdr);
    }

    /**
     * Creates Node Rule Group for Xponders.
     * Returns a Map of Node Rule Groups returned by the method in case of successful creation.
     * @param nodeId of OpenROADM Node.
     * @param oorOduSwPool OpenROADM Node Odu Switching Pool.
     */
    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForXpdrNode(String nodeId,
        Map<OduSwitchingPoolsKey, OduSwitchingPools> oorOduSwPool) {

        if (oorOduSwPool == null) {
            LOG.info("No switching pool created for node = {}", nodeId);
            return new HashMap<>();
        }
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(
            String.join("+", nodeId, TapiConstants.XPDR).getBytes(StandardCharsets.UTF_8)).toString());
        int count = 0;
        RuleBuilder nblRuleBd = new RuleBuilder()
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(new HashSet<>(Set.of(RuleType.FORWARDING)));

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.Rule rule;

        for (Map.Entry<OduSwitchingPoolsKey, OduSwitchingPools> oduSwPool : oorOduSwPool.entrySet()) {
            List<NodeRuleGroupKey> nrgKeyList = new ArrayList<>();
            if (oduSwPool.getValue().getSwitchingPoolType().equals(SwitchingPoolTypes.NonBlocking)) {
                rule = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                        .inter.rule.group.RuleBuilder()
                    .setLocalId("forward" + count)
                    .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                    .setRuleType(new HashSet<>(Set.of(RuleType.FORWARDING)))
                    .build();
            } else {
                rule = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                        .inter.rule.group.RuleBuilder()
                    .setLocalId("non-forward" + count)
                    .setForwardingRule(FORWARDINGRULECANNOTFORWARDACROSSGROUP.VALUE)
                    .setRuleType(new HashSet<>(Set.of(RuleType.FORWARDING)))
                    .build();
            }
            int nblCount = 0;

            String ietfXpdr = String.join("+", nodeId, TapiConstants.XPDR);
            for (Map.Entry<NonBlockingListKey, NonBlockingList> nblEntry :
                oduSwPool.getValue().getNonBlockingList().entrySet()) {
                nblCount++;
                LOG.debug("CORTTTline1574 CreateNodeRuleGroupForXpdrNode, Non blocking list = {}", nblEntry.getValue());

                Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                        .node.rule.group.NodeEdgePointKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                        .node.rule.group.NodeEdgePoint>
                    nepList = new HashMap<>();
                Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                        .node.rule.group.NodeEdgePointKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                        .node.rule.group.NodeEdgePoint>
                    oduNepList = new HashMap<>();

                for (TpId tp : nblEntry.getValue().getTpList()) {
                    String tpValue = tp.getValue();
                    String ietfEoduTp = String.join("+", nodeId, TapiConstants.E_ODU, tpValue);
                    LOG.debug("UuidKey={}", ietfEoduTp);
                    String ietfIoduTp = String.join("+", nodeId, TapiConstants.I_ODU, tpValue);
                    //Following Nep MAp is currently not used
                    // TODO: consolidate or remove after PCE for TAPI is consolidated
                    if (this.uuidMap.containsKey(String.join("+", nodeId, TapiConstants.DSR, tpValue))
                            || this.uuidMap.containsKey(ietfIoduTp)) {
                        var nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                    .node.rule.group.NodeEdgePointBuilder()
                                .setTopologyUuid(tapiTopoUuid)
                                .setNodeUuid(this.uuidMap.get(ietfXpdr))
                                .setNodeEdgePointUuid(this.uuidMap.get(
                                    String.join("+", nodeId, tpValue.contains("CLIENT") ? TapiConstants.DSR
                                            : TapiConstants.I_ODU,
                                        tpValue)))
                                .build();
                        nepList.put(nep.key(), nep);
                    }
                    if (this.uuidMap.containsKey(ietfEoduTp)) {
                        var nep1 = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                    .node.rule.group.NodeEdgePointBuilder()
                                .setTopologyUuid(tapiTopoUuid)
                                .setNodeUuid(this.uuidMap.get(ietfXpdr))
                                .setNodeEdgePointUuid(this.uuidMap.get(ietfEoduTp))
                                .build();
                        oduNepList.put(nep1.key(), nep1);
                    }
                    if (this.uuidMap.containsKey(ietfIoduTp)) {
                    // TODO already checked with DSR above -> potential factorization ?
                        var nep2 = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                    .node.rule.group.NodeEdgePointBuilder()
                                .setTopologyUuid(tapiTopoUuid)
                                .setNodeUuid(this.uuidMap.get(ietfXpdr))
                                .setNodeEdgePointUuid(this.uuidMap.get(ietfIoduTp))
                                .build();
                        oduNepList.put(nep2.key(), nep2);
                    }
                }
                //EndLoop on TP
                LOG.info("COORTTLine1647 NEPLIST (DSR/I_ODU) of [dsr node rule group] is {}", nepList);
                LOG.info("COORTTLine1648 ODUNEPLIST (E_ODU/I_ODU) of [odu node rule group] is {}", oduNepList);
                AvailableCapacity avc;
                TotalPotentialCapacity tpc;
                if (nblEntry.getValue().getCapableInterconnectBandwidth() != null
                        && nblEntry.getValue().getInterconnectBandwidthUnit() != null) {
                    TotalSize potentialTs = new TotalSizeBuilder()
                        .setValue(Decimal64.valueOf((nblEntry.getValue().getCapableInterconnectBandwidth().doubleValue()
                            * nblEntry.getValue().getInterconnectBandwidthUnit().doubleValue() / 1000000000),
                            RoundingMode.DOWN))
                        .setUnit(CAPACITYUNITGBPS.VALUE)
                        .build();
                    tpc = new TotalPotentialCapacityBuilder()
                        .setTotalSize(potentialTs).build();
                    Uint32 availBW = nblEntry.getValue().getAvailableInterconnectBandwidth();
                    // TODO: Right now available BW is not in Device OR model. Correct next line when it will be
                    availBW = availBW == null ? nblEntry.getValue().getCapableInterconnectBandwidth() : availBW;
                    TotalSize availableTs = new TotalSizeBuilder()
                        .setValue(Decimal64.valueOf((availBW.doubleValue()
                            * nblEntry.getValue().getInterconnectBandwidthUnit().doubleValue() / 1000000000),
                            RoundingMode.DOWN))
                        .setUnit(CAPACITYUNITGBPS.VALUE)
                        .build();
                    avc = new AvailableCapacityBuilder()
                        .setTotalSize(availableTs).build();
                } else {
                    avc = new AvailableCapacityBuilder().build();
                    tpc = new TotalPotentialCapacityBuilder().build();
                }

                CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                    .setCostAlgorithm("Restricted Shortest Path - RSP")
                    .setCostName("HOP_COUNT")
                    .setCostValue(TapiConstants.COST_HOP_VALUE)
                    .build();
                LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                    .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
                    .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
                    .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
                    .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
                    .setTrafficPropertyName("FIXED_LATENCY")
                    .build();
                RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                    .setRiskCharacteristicName("risk characteristic")
                    .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
                    .build();

                if (nepList != null && !nepList.isEmpty()) {
                    Name nrgName1 = new NameBuilder().setValueName("nrg name")
                        .setValue("dsr node rule group-" + count + "." + nblCount).build();
                    NodeRuleGroup nodeRuleGroup1 = new NodeRuleGroupBuilder()
                        .setName(Map.of(nrgName1.key(), nrgName1))
                        .setUuid(new Uuid(
                            UUID.nameUUIDFromBytes(("dsr node rule group-" + count + "." + nblCount)
                                .getBytes(StandardCharsets.UTF_8)).toString()))
                        .setRule(new HashMap<>(Map.of(new RuleKey("forward" + nblCount),
                            nblRuleBd.setLocalId("forward" + count + "." + nblCount).build())))
                        .setNodeEdgePoint(nepList)
                        .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                        .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                        .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                        .setAvailableCapacity(avc)
                        .setTotalPotentialCapacity(tpc)
                        .build();
                    nodeRuleGroupMap.put(nodeRuleGroup1.key(), nodeRuleGroup1);
                    nrgKeyList.add(nodeRuleGroup1.key());
                }

                if (oduNepList != null && !oduNepList.isEmpty()) {
                    Name nrgName2 = new NameBuilder().setValueName("nrg name")
                        .setValue("odu node rule group-" + count + "." + nblCount).build();
                    NodeRuleGroup nodeRuleGroup2 = new NodeRuleGroupBuilder()
                        .setName(Map.of(nrgName2.key(), nrgName2))
                        .setUuid(new Uuid(
                            UUID.nameUUIDFromBytes(("odu node rule group-" + count + "." + nblCount)
                                .getBytes(StandardCharsets.UTF_8)).toString()))
                        .setRule(new HashMap<>(Map.of(new RuleKey("forward" + nblCount),
                            nblRuleBd.setLocalId("forward" + count + "." + nblCount).build())))
                        .setNodeEdgePoint(oduNepList)
                        .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                        .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                        .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                        .setAvailableCapacity(avc)
                        .setTotalPotentialCapacity(tpc)
                        .build();
                    nodeRuleGroupMap.put(nodeRuleGroup2.key(), nodeRuleGroup2);
                    nrgKeyList.add(nodeRuleGroup2.key());
                }
            }
            // End LOOP on NBL
            count++;
            if (nrgKeyList.size() > 1) {
                this.irgMap = createInterRuleGroupForXpdrNode(count, nodeUuid, nrgKeyList, rule);
            }
        }
        //endLoop MAP<ospkey, osp>

        return nodeRuleGroupMap;
    }

    /**
     * Creates a Node Edge Point from an OpenROADM termination point.
     * Returns a List of owned node edge points returned by the method in case of successful Nep creation.
     * @param oorTp OpenROADM tp the NEP is mapped to.
     * @param nepNames A map of Names associated to the created NEP.
     * @param nepProtocol Layer protocol the NEP is associated to,
     * @param nodeProtocol Layer protocol the Node is associated to,
     * @param withSip Boolean used to trigger the creation of SIP associated to the Nep,
     * @param keyword String key used to refine the case of Digital-OTN that does not separate OTU from ODU case.
     */
    private List<OwnedNodeEdgePoint> createNep(TerminationPoint oorTp, Map<NameKey, Name> nepNames,
            LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip, String keyword) {
        var tp1 = oorTp.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev250110.TerminationPoint1.class);
        var oorTpId = oorTp.getTpId();
        var oorTpIdValue = oorTpId.getValue();
        if (tp1.getTpSupportedInterfaces() == null) {
            LOG.warn("Tp supported interface doesnt exist on TP {}", oorTpIdValue);
            return null;
        }
        Collection<SupportedInterfaceCapability> sicColl =
            tp1.getTpSupportedInterfaces().getSupportedInterfaceCapability().values();
        TerminationPoint1 oorTpAug = oorTp.augmentation(TerminationPoint1.class);
        String rate = "100";
        List<OperationalModeKey> opModeList = new ArrayList<>();
        if (oorTpAug.getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
            var tp11 = oorTp.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1.class);
            if (tp11 == null || tp11.getXpdrNetworkAttributes() == null) {
                for (SupportedInterfaceCapability sic : sicColl) {
                    String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
                    switch (ifCapType) {
                        case "IfOCHOTUCnODUCn":
                        case "IfOCHOTUCnODUCnUniregen":
                        case "IfOCHOTUCnODUCnRegen":
                            opModeList.add(new OperationalModeKey("400G"));
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + " Assumes that by default, OTUCN is 400G capable",
                                oorTpId);
                            rate = "400";
                            break;
                        default:
                            break;
                    }
                }
                opModeList.add(new OperationalModeKey("100G"));
                LOG.warn(TopologyUtils.NOOPMODEDECLARED + "Assumes that by default, 100G rate available", oorTpId);
            } else {
                opModeList = tp11.getXpdrNetworkAttributes().getSupportedOperationalModes().getOperationalMode()
                    .keySet().stream().toList();
                if (tp11.getXpdrNetworkAttributes().getRate() != null) {
                    String rateIdentity = tp11.getXpdrNetworkAttributes().getRate().toString();
                    if (rateIdentity.contains("200")) {
                        rate = "200";
                    } else if (rateIdentity.contains("300")) {
                        rate = "300";
                    } else if (rateIdentity.contains("400")) {
                        rate = "400";
                    } else if (rateIdentity.contains("600")) {
                        rate = "600";
                    } else if (rateIdentity.contains("800")) {
                        rate = "800";
                    } else {
                        rate = "100";
                    }
                }
            }
        }

        double clientRate = 100.0;
        List<Double> rateList = new ArrayList<>();
        if (oorTpAug.getTpType().equals(OpenroadmTpType.XPONDERCLIENT)) {
            for (SupportedInterfaceCapability sic : sicColl) {
                String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
                if (ifCapType.contains("ODU0") || ifCapType.contains("TYPEGigE")) {
                    rateList.add(1.0);
                } else if (ifCapType.contains("ODU2") || ifCapType.contains("TYPE10GigE")) {
                    rateList.add(10.0);
                } else if (ifCapType.contains("ODU4") || ifCapType.contains("TYPE100GigE")) {
                    rateList.add(100.0);
                }
            }
            for (Double rateOfList : rateList) {
                clientRate = (rateOfList < clientRate) ? rateOfList : clientRate;
            }
        }
        String key = keyword;
        if (keyword.contains(("ODU"))) {
            key = "ODU";
        } else if (keyword.contains(("OTU"))) {
            key = "OTU";
        }
        var oorTpAugAdmState = oorTpAug.getAdministrativeState();
        AdministrativeState adminState =
            oorTpAugAdmState == null ? null : transformAsToTapiAdminState(oorTpAugAdmState.getName());
        var oorTpAugOprState = oorTpAug.getOperationalState();
        OperationalState operState =
            oorTpAugOprState == null ? null : transformOsToTapiOperationalState(oorTpAugOprState.getName());
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
            .setUuid(this.uuidMap.get(String.join("+", keyword, oorTpIdValue)))
            .setLayerProtocolName(nepProtocol)
            .setName(nepNames)
            .setSupportedCepLayerProtocolQualifierInstances(
                createSupportedCepLayerProtocolQualifier(sicColl, nepProtocol, key))
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(
                createMSIP(1, nepProtocol, oorTpIdValue, keyword, sicColl, operState, adminState));
        }
        List<OwnedNodeEdgePoint> onepList = new ArrayList<>();
        if (!keyword.contains(TapiConstants.OTSI_MC) && !keyword.contains(TapiConstants.PHTNC_MEDIA_OTS)) {
            if (sicColl == null || sicColl.isEmpty()) {
                onepList.add(onepBldr.build());
                return onepList;
            }
            if (nepProtocol.equals(LayerProtocolName.DSR)) {
                if (!sicColl.stream()
                        .filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName().contains("GE"))
                        .findFirst().orElseThrow().toString().isEmpty()) {
                    LOG.info("COTTtopology 1451 Interface searched in LPN Map is {}",
                        sicColl.stream().filter(lp -> lp.getIfCapType().toString().contains("GE"))
                        .findFirst().orElseThrow().getIfCapType().implementedInterface().getSimpleName());
                    Map<LAYERPROTOCOLQUALIFIER, Uint64> supInt = new HashMap<>();
                    supInt.putAll(LPN_MAP.get("ETH").get(sicColl.stream()
                        .filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName().contains("GE"))
                        .findFirst().orElseThrow().getIfCapType().implementedInterface().getSimpleName()));
                    onepBldr.setSupportedPayloadStructure(createSupportedPayloadStructureForCommonNeps(
                        false, clientRate, Integer.valueOf(1), supInt.keySet()));
                    onepBldr.setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                        createTotalSizeForCommonNeps(Double.valueOf(rate))).build());
                    if (oorTpAug.getOperationalState() == null
                            || oorTpAug.getOperationalState().getName().equals("inService")) {
                        onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                            true, clientRate, Integer.valueOf(1), supInt.keySet()));
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(Double.valueOf(rate))).build());
                    } else if (oorTpAug.getOperationalState().getName().equals("outOfService")) {
                        onepBldr.setAvailablePayloadStructure(
                            createAvailablePayloadStructureForCommonNeps(false, clientRate,
                                Integer.valueOf(0), supInt.keySet()));
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(0.0)).build());
                    }
                } else if (!sicColl.stream().filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName()
                        .contains("OTU4")).findFirst().orElseThrow().toString().isEmpty()) {
                    Map<LAYERPROTOCOLQUALIFIER, Uint64> supInt = new HashMap<>();
                    supInt.putAll(LPN_MAP.get("ETH").get("IfOCH"));
                    onepBldr.setSupportedPayloadStructure(createSupportedPayloadStructureForCommonNeps(
                        false, Double.valueOf(rate), Integer.valueOf(1), supInt.keySet()));
                    onepBldr.setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                        createTotalSizeForCommonNeps(Double.valueOf(rate))).build());
                    if (oorTpAug.getOperationalState().getName().equals("inService")) {
                        onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                            true, Double.valueOf(rate), Integer.valueOf(0), supInt.keySet()));
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(Double.valueOf(rate))).build());
                    } else if (oorTpAug.getOperationalState().getName().equals("outOfService")) {
                        onepBldr.setAvailablePayloadStructure(
                            createAvailablePayloadStructureForCommonNeps(false, Double.valueOf(rate),
                                Integer.valueOf(0), supInt.keySet()));
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(0.0)).build());
                    }
                } else {
                    onepList.add(onepBldr.build());
                    return onepList;
                }
            } else if ((nepProtocol.equals(LayerProtocolName.ODU) || nepProtocol.equals(LayerProtocolName.DIGITALOTN))
                    && oorTpAug.getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                Integer numberOfInstance = Integer.parseInt(rate) / 100;
                if (!sicColl.stream()
                        .filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName().contains("ODU4"))
                        .findFirst().toString().isEmpty()) {
                    Map<LAYERPROTOCOLQUALIFIER, Uint64> supInt = new HashMap<>();
                    supInt.putAll(Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO));
                    onepBldr.setSupportedPayloadStructure(createSupportedPayloadStructureForCommonNeps(
                        false, Double.valueOf(100), numberOfInstance, supInt.keySet()));
                    onepBldr.setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                        createTotalSizeForCommonNeps(100.0)).build());
                    if (tp1.getXpdrTpPortConnectionAttributes() == null
                            || tp1.getXpdrTpPortConnectionAttributes().getTsPool() == null
                            || tp1.getXpdrTpPortConnectionAttributes().getTsPool().isEmpty()) {
                        onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                            false, Double.valueOf(100), numberOfInstance, supInt.keySet()));
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(100.0 * numberOfInstance)).build());
                    } else {
                        if (Integer.parseInt(rate)
                                - tp1.getXpdrTpPortConnectionAttributes().getTsPool().size() * 5 < 0) {
                            numberOfInstance = (int) Math.round(
                                (tp1.getXpdrTpPortConnectionAttributes().getTsPool().size() * 1.25) / 100);
                        } else {
                            numberOfInstance = (tp1.getXpdrTpPortConnectionAttributes().getTsPool().size() * 5) / 100;
                        }
                        onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                            false, Double.valueOf(100), numberOfInstance, supInt.keySet()));
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(100.0 * numberOfInstance)).build());
                    }
                } else {
                    // this is the case where SicColl does not contain ODU4 and nep Protocol is digital OTN
                    // meaning we create an OTU (OTU4 or OTUCn) Nep
                    Set<LAYERPROTOCOLQUALIFIER> lpqSet = new HashSet<>();

                    if (Integer.parseInt(rate) == 100) {
                        lpqSet.add(OTUTYPEOTU4.VALUE);
                        onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                            false, Double.valueOf(100), 0, lpqSet));
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(100.0)).build());
                    } else {
                        if (key.equals("OTU")) {
                            lpqSet.add(OTUTYPEOTUCN.VALUE);
                            onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                                false, Double.valueOf(rate), 0, lpqSet));
                            //Recursive call to create ODUCN NEP just after OTUCN one
                            String onedNameVal = String.join("+", this.ietfNodeId, TapiConstants.E_ODUCN,
                                oorTp.getTpId().getValue());
                            LOG.info("Creating eODUCN NEP = {} recursivly after processing OTUCN NEP", onedNameVal);
                            Name onedName = new NameBuilder().setValueName("eNodeEdgePoint_N").setValue(onedNameVal)
                                .build();
                            onepList.addAll(createNep(oorTp, Map.of(onedName.key(), onedName),
                                LayerProtocolName.DIGITALOTN, LayerProtocolName.DIGITALOTN, true,
                                String.join("+", this.ietfNodeId, TapiConstants.E_ODUCN)));
                        } else {
                            lpqSet.add(ODUTYPEODUCN.VALUE);
                            onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                                false, Double.valueOf(rate), 0, lpqSet));
                        }
                        onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                            createTotalSizeForCommonNeps(0.0)).build());
                    }
                    onepBldr.setSupportedPayloadStructure(createSupportedPayloadStructureForCommonNeps(
                        false, Double.valueOf(rate), 1, lpqSet));
                    onepBldr.setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                        createTotalSizeForCommonNeps(Double.valueOf(rate))).build());
                }
            } else if ((nepProtocol.equals(LayerProtocolName.ODU) || nepProtocol.equals(LayerProtocolName.DIGITALOTN))
                    && oorTpAug.getTpType().equals(OpenroadmTpType.XPONDERCLIENT)) {
                Map<LAYERPROTOCOLQUALIFIER, Uint64> supInt = new HashMap<>();
                if (!sicColl.stream().filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName()
                        .contains("ODU4")).collect(Collectors.toList()).isEmpty()) {
                    supInt.putAll(Map.of(ODUTYPEODU4.VALUE, Uint64.ONE));
                } else if (!sicColl.stream().filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName()
                        .contains("ODU2e")).collect(Collectors.toList()).isEmpty()) {
                    supInt.putAll(Map.of(ODUTYPEODU2E.VALUE, Uint64.ONE));
                } else if (!sicColl.stream().filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName()
                        .contains("ODU2")).collect(Collectors.toList()).isEmpty()) {
                    supInt.putAll(Map.of(ODUTYPEODU2.VALUE, Uint64.ONE));
                } else if (!sicColl.stream().filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName()
                    .contains("ODU0")).collect(Collectors.toList()).isEmpty()) {
                    supInt.putAll(Map.of(ODUTYPEODU0.VALUE, Uint64.ONE));
                }
                onepBldr.setSupportedPayloadStructure(createSupportedPayloadStructureForCommonNeps(
                    false, clientRate, Integer.valueOf(1), supInt.keySet()));
                onepBldr.setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                    createTotalSizeForCommonNeps(clientRate)).build());
                if (oorTpAug.getOperationalState() == null
                        || oorTpAug.getOperationalState().getName().equals("inService")) {
                    onepBldr.setAvailablePayloadStructure(createAvailablePayloadStructureForCommonNeps(
                        true, clientRate, Integer.valueOf(1), supInt.keySet()));
                    onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                        createTotalSizeForCommonNeps(clientRate)).build());
                } else if (oorTpAug.getOperationalState().getName().equals("outOfService")) {
                    onepBldr.setAvailablePayloadStructure(
                        createAvailablePayloadStructureForCommonNeps(false, clientRate,
                            Integer.valueOf(0), supInt.keySet()));
                    onepBldr.setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                        createTotalSizeForCommonNeps(0.0)).build());
                }
            }
            onepList.add(onepBldr.build());
            return onepList;
        }
        if (oorTpAug.getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
            onepBldr = addPayloadStructureAndPhotSpecToOnep(
                this.ietfNodeId, rate, getXpdrUsedWavelength(oorTp), opModeList, sicColl, onepBldr, keyword);
        }
        if (keyword.contains(TapiConstants.PHTNC_MEDIA_OTS)) {
            String nepId = nepNames.entrySet().stream()
                .filter(n -> n.getKey().equals(new NameKey("eNodeEdgePoint")))
                .findAny().orElseThrow().getValue().getValue();
            if (nepId != null) {
                ConnectionEndPoint otsCep = createOTSCepXpdr(nepId);
                Map<ConnectionEndPointKey, ConnectionEndPoint> cepMap = new HashMap<>(Map.of(otsCep.key(), otsCep));
                onepBldr.addAugmentation(
                    new OwnedNodeEdgePoint1Builder().setCepList(
                            new CepListBuilder().setConnectionEndPoint(cepMap).build())
                        .build());
            }
        }
        OwnedNodeEdgePoint onep = onepBldr.build();
        LOG.debug("ConvertORToTapiTopology 1485, onep = {}", onep);
        onepList.add(onepBldr.build());
        return onepList;
    }

    /**
     * Creates a ROADM Node Edge Point.
     * Returns an owned node edge point in case of successful Nep creation.
     * @param orNodeId Id of the OpenROADM Node the NEP is mapped to.
     * @param tpId Id of the OpenROADM tp the NEP is mapped to.
     * @param withSip Boolean used to trigger the creation of SIP associated to the Nep,
     * @param operState Operational state of the Nep.
     * @param adminState Administrative state of the Nep
     * @param nepPhotonicSublayer The layer protocol qualifier of the Nep.
     */
    public OwnedNodeEdgePoint createRoadmNep(String orNodeId, String tpId, boolean withSip,
            OperationalState operState, AdministrativeState adminState, String nepPhotonicSublayer) {
        //TODO : complete implementation with SIP
        Name nepName = new NameBuilder()
                .setValueName(TapiConstants.PHTNC_MEDIA + "NodeEdgePoint")
                .setValue(String.join("+", orNodeId, nepPhotonicSublayer, tpId))
                .build();
        return new OwnedNodeEdgePointBuilder()
            .setUuid(
                new Uuid(UUID.nameUUIDFromBytes(
                        (String.join("+", orNodeId, nepPhotonicSublayer,tpId)).getBytes(StandardCharsets.UTF_8))
                    .toString()))
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
            .setName(Map.of(nepName.key(), nepName))
            .setSupportedCepLayerProtocolQualifierInstances(
                new ArrayList<>(List.of(
                    new SupportedCepLayerProtocolQualifierInstancesBuilder()
                        .setLayerProtocolQualifier(
                            TapiConstants.MC.equals(nepPhotonicSublayer)
                                ? PHOTONICLAYERQUALIFIERMC.VALUE
                                : PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                        .setNumberOfCepInstances(Uint64.ONE)
                        .build())))
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState).setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED)
            .build();
    }

    /**
     * Creates a Service Interface Point.
     * @param sipUuid The SIP Uuid,
     * @param layerProtocol Layer protocol the SIP is associated to,
     * @param tpId OpenROADM termination Point Id,
     * @param nodeId OpenROADM Node Id,
     * @param supportedInterfaceCapability Collection of supported interface capabilities,
     * @param operState Operational state of the SIP,
     * @param adminState Administrative state of the SIP,
     */
    private ServiceInterfacePoint createSIP(Uuid sipUuid, LayerProtocolName layerProtocol, String tpId,
        String nodeid, Collection<SupportedInterfaceCapability> supportedInterfaceCapability,
        OperationalState operState, AdministrativeState adminState) {
    // TODO: what value should be set in total capacity and available capacity??
        LOG.debug("SIP name = {}", String.join("+", nodeid, tpId));
        Name sipName = new NameBuilder()
            .setValueName("SIP name")
            .setValue(String.join("+", nodeid, tpId))
            .build();
        return new ServiceInterfacePointBuilder()
            .setUuid(sipUuid)
            .setName(Map.of(sipName.key(), sipName))
            .setLayerProtocolName(layerProtocol)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setAvailableCapacity(new AvailableCapacityBuilder().build())
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().build())
            .setSupportedCepLayerProtocolQualifierInstances(createSipSupportedLayerProtocolQualifier(
                supportedInterfaceCapability, layerProtocol))
            .build();
    }

    /**
     * Generates a list of Supported Cep Layer Protocol Qualifier Instances supported by a Service Interface Point.
     * @param supportedInterfaceCapability Collection of supported interface capabilities,
     * @param layerProtocolName Layer protocol the SIP is associated to,
     * @param operState Operational state of the SIP,
     * @param adminState Administrative state of the SIP,
     */
    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                .service._interface.point.SupportedCepLayerProtocolQualifierInstances>
             createSipSupportedLayerProtocolQualifier(
                Collection<SupportedInterfaceCapability> supportedInterfaceCapability, LayerProtocolName lpn) {
        if (supportedInterfaceCapability == null) {
            return new ArrayList<>(List.of(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                .setNumberOfCepInstances(Uint64.ONE)
                .build()));
        }
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
            .service._interface.point.SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        String lpnName = lpn.getName();
        for (SupportedInterfaceCapability sic : supportedInterfaceCapability) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            if (!LPN_MAP.containsKey(lpnName)) {
                LOG.error("Layer Protocol Name is unknown {}", lpnName);
                break;
            }
            var ifCapTypeMap = LPN_MAP.get(lpnName);
            if (!ifCapTypeMap.containsKey(ifCapType)) {
                LOG.error("IfCapability type {} not managed", ifCapType);
                break;
            }
            for (Map.Entry<LAYERPROTOCOLQUALIFIER, Uint64> entry: ifCapTypeMap.get(ifCapType).entrySet()) {
                sclpqiList.add(
                    new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                            .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                        .setLayerProtocolQualifier(entry.getKey())
                        .setNumberOfCepInstances(entry.getValue())
                        .build());
            }
        }
        return sclpqiList.stream().distinct().toList();
    }

    public Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            getTapiNodes() {
        return tapiNodes;
    }

    public Map<LinkKey, Link> getTapiLinks() {
        return tapiLinks;
    }

    /**
     * Converts an OpenROADM compliant administrative state (provided as a string) to a Tapi administrative state.
     * @param adminState Administrative state in OpenROADM format converted to a string,
     */
    public AdministrativeState transformAsToTapiAdminState(String adminState) {
        return adminState == null ? null
            : adminState.equals(AdminStates.InService.getName())
                    || adminState.equals(AdministrativeState.UNLOCKED.getName())
                ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    /**
     * Converts an OpenROADM compliant operational state (provided as a string) to a Tapi operational state.
     * @param operState Operational state in OpenROADM format converted to a string,
     */
    public OperationalState transformOsToTapiOperationalState(String operState) {
        return operState == null ? null
            : operState.equals("inService") || operState.equals(OperationalState.ENABLED.getName())
                ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getTapiSips() {
        return tapiSips;
    }

    public void setTapiSips(Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSip) {
        this.tapiSips.putAll(tapiSip);
    }

}
