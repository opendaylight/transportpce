/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.ServiceRateConstant;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.transportpce.pce.gnpy.utils.AToZComparator;
import org.opendaylight.transportpce.pce.gnpy.utils.ZToAComparator;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220221.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220221.topo.ElementsKey;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.RouteIncludeEro;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.TeHopType;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.TePathDisjointness;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.common.constraints_config.TeBandwidth;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.common.constraints_config.TeBandwidthBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.explicit.route.hop.Type;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.explicit.route.hop.type.NumUnnumHopBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.explicit.route.hop.type.num.unnum.hop.NumUnnumHop;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.generic.path.constraints.PathConstraints;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.generic.path.constraints.PathConstraintsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.gnpy.specific.parameters.EffectiveFreqSlot;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.gnpy.specific.parameters.EffectiveFreqSlotBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.path.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.path.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.path.route.objects.explicit.route.objects.RouteObjectIncludeExclude;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.path.route.objects.explicit.route.objects.RouteObjectIncludeExcludeBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.path.route.objects.explicit.route.objects.RouteObjectIncludeExcludeKey;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.service.PathRequest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.service.PathRequestBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.service.PathRequestKey;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.synchronization.info.Synchronization;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.synchronization.info.SynchronizationBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.synchronization.info.synchronization.Svec;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.synchronization.info.synchronization.SvecBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev211210.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.Resource;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to create the service corresponding to GNPy requirements.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class GnpyServiceImpl {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyServiceImpl.class);

    private Map<PathRequestKey, PathRequest> pathRequest = new HashMap<>();
    private List<Synchronization> synchronization = new ArrayList<>();
    private Map<String, String> mapDisgNodeRefNode = new HashMap<>();
    private Map<String, List<String>> mapLinkFiber = new HashMap<>();
    private List<String> trxList = new ArrayList<>();
    private Map<ElementsKey, Elements> elements = new HashMap<>();
    private Map<RouteObjectIncludeExcludeKey, RouteObjectIncludeExclude> routeObjectIncludeExcludes = new HashMap<>();
    private String currentNodeId = null;
    private AToZComparator atoZComparator =  new AToZComparator();
    private ZToAComparator ztoAComparator =  new ZToAComparator();
    private static final Table<Uint32, BigDecimal, String> TRX_MODE_TABLE = initTrxModeTable();

    private static Table<Uint32, BigDecimal, String> initTrxModeTable() {
        Table<Uint32, BigDecimal, String> trxModeTable = HashBasedTable.create();
        trxModeTable.put(ServiceRateConstant.RATE_100, GridConstant.SLOT_WIDTH_50, "100 Gbit/s, 27.95 Gbaud, DP-QPSK");
        trxModeTable.put(ServiceRateConstant.RATE_200, GridConstant.SLOT_WIDTH_50, "200 Gbit/s, 31.57 Gbaud, DP-16QAM");
        trxModeTable.put(ServiceRateConstant.RATE_200, GridConstant.SLOT_WIDTH_87_5, "200 Gbit/s, DP-QPSK");
        trxModeTable.put(ServiceRateConstant.RATE_300, GridConstant.SLOT_WIDTH_87_5, "300 Gbit/s, DP-8QAM");
        trxModeTable.put(ServiceRateConstant.RATE_400, GridConstant.SLOT_WIDTH_87_5, "400 Gbit/s, DP-16QAM");
        return trxModeTable;
    }

    public static final Map<Uint32, BigDecimal> RATE_OUTPUTPOWER = Map.of(
            ServiceRateConstant.RATE_100, GridConstant.OUTPUT_POWER_100GB_W,
            ServiceRateConstant.RATE_400, GridConstant.OUTPUT_POWER_400GB_W);

    /*
     * Construct the GnpyServiceImpl
     */
    public GnpyServiceImpl(PathComputationRequestInput input, AToZDirection atoz, Uint32 requestId,
                GnpyTopoImpl gnpyTopo, PceConstraints pceHardConstraints) throws GnpyException {
        this.elements = gnpyTopo.getElements();
        this.mapDisgNodeRefNode = gnpyTopo.getMapDisgNodeRefNode();
        this.mapLinkFiber = gnpyTopo.getMapLinkFiber();
        this.trxList = gnpyTopo.getTrxList();
        this.pathRequest = extractPathRequest(input, atoz, requestId.toJava(), pceHardConstraints);
        this.synchronization = extractSynchronization(requestId);
    }

    public GnpyServiceImpl(PathComputationRequestInput input, ZToADirection ztoa, Uint32 requestId,
                GnpyTopoImpl gnpyTopo, PceConstraints pceHardConstraints) throws GnpyException {
        this.elements = gnpyTopo.getElements();
        this.mapDisgNodeRefNode = gnpyTopo.getMapDisgNodeRefNode();
        this.mapLinkFiber = gnpyTopo.getMapLinkFiber();
        this.trxList = gnpyTopo.getTrxList();
        pathRequest = extractPathRequest(input, ztoa, requestId.toJava(), pceHardConstraints);
        synchronization = extractSynchronization(requestId);
    }

    private Map<PathRequestKey, PathRequest> extractPathRequest(
            PathComputationRequestInput input, AToZDirection atoz, Long requestId,
            PceConstraints pceHardConstraints) throws GnpyException {

        // Create the source and destination nodes
        String sourceNode = input.getServiceAEnd().getNodeId();
        String destNode = input.getServiceZEnd().getNodeId();
        if (!trxList.contains(sourceNode) || !trxList.contains(destNode)) {
            throw new GnpyException("In GnpyServiceImpl: source and destination should be transmitter nodes");
        }

        // Create explicitRouteObjects
        List<AToZ> listAtoZ = new ArrayList<>(atoz.nonnullAToZ().values());
        if (listAtoZ.isEmpty()) {
            extractHardConstraints(pceHardConstraints);
        } else {
            Collections.sort(listAtoZ, atoZComparator);
            extractRouteObjectIcludeAtoZ(listAtoZ);
        }

        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
            .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();
        //Create Path Constraint
        PathConstraints pathConstraints = createPathConstraints(atoz.getRate().toJava(),
                atoz.getModulationFormat(),
                atoz.getAToZMinFrequency(),
                atoz.getAToZMaxFrequency());

        // Create the path request
        Map<PathRequestKey, PathRequest> pathRequestMap = new HashMap<>();
        PathRequest pathRequestEl = new PathRequestBuilder().setRequestId(requestId.toString())
            .setSource(sourceNode)
            .setDestination(destNode)
            .setSrcTpId(sourceNode)
            .setDstTpId(destNode)
            .setBidirectional(false).setPathConstraints(pathConstraints).setPathConstraints(pathConstraints)
            .setExplicitRouteObjects(explicitRouteObjects).build();
        pathRequestMap.put(pathRequestEl.key(),pathRequestEl);
        LOG.debug("In GnpyServiceImpl: path request AToZ is extracted");
        return pathRequestMap;
    }

    private Map<PathRequestKey, PathRequest> extractPathRequest(
            PathComputationRequestInput input, ZToADirection ztoa, Long requestId,
            PceConstraints pceHardConstraints) throws GnpyException {
        // Create the source and destination nodes
        String sourceNode = input.getServiceZEnd().getNodeId();
        String destNode = input.getServiceAEnd().getNodeId();
        if (!trxList.contains(sourceNode) || !trxList.contains(destNode)) {
            throw new GnpyException("In GnpyServiceImpl: source and destination should be transmitter nodes");
        }
        // Create explicitRouteObjects
        @NonNull List<ZToA> listZtoA = new ArrayList<>(ztoa.nonnullZToA().values());
        if (listZtoA.isEmpty()) {
            extractHardConstraints(pceHardConstraints);
        } else {
            Collections.sort(listZtoA, ztoAComparator);
            extractRouteObjectIcludeZtoA(listZtoA);
        }

        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
            .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();
        //Create Path Constraint
        PathConstraints pathConstraints = createPathConstraints(ztoa.getRate().toJava(),
                ztoa.getModulationFormat(),
                ztoa.getZToAMinFrequency(),
                ztoa.getZToAMaxFrequency());

        //Create the path request
        Map<PathRequestKey, PathRequest> pathRequestMap = new HashMap<>();
        PathRequest pathRequestEl = new PathRequestBuilder().setRequestId(requestId.toString())
            .setSource(sourceNode)
            .setDestination(destNode)
            .setSrcTpId(sourceNode)
            .setDstTpId(destNode)
            .setBidirectional(false).setPathConstraints(pathConstraints)
            .setExplicitRouteObjects(explicitRouteObjects).build();
        pathRequestMap.put(pathRequestEl.key(),pathRequestEl);
        LOG.debug("In GnpyServiceImpl: path request ZToA is extracted is extracted");
        return pathRequestMap;
    }

    //Extract RouteObjectIncludeExclude list in the case of pre-computed path A-to-Z
    private void extractRouteObjectIcludeAtoZ(Collection<AToZ> listAtoZ) throws GnpyException {
        Long index = 0L;
        for (AToZ entry : listAtoZ) {
            index = createResource(entry.getResource().getResource(),index);
        }
    }

    //Extract RouteObjectIncludeExclude list in the case of pre-computed path Z-to-A
    private void extractRouteObjectIcludeZtoA(@NonNull List<ZToA> listZtoA) throws GnpyException {
        Long index = 0L;
        for (ZToA entry : listZtoA) {
            index = createResource(entry.getResource().getResource(),index);
        }
    }

    //Create a new resource node or link
    private Long createResource(@Nullable Resource resource, Long index) throws GnpyException {
        Long idx = index;
        if (resource
            instanceof
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                    .pce.resource.resource.resource.Node) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                .pce.resource.resource.resource.Node node =
                (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                    .pce.resource.resource.resource.Node) resource;
            if (node.getNodeId() == null) {
                throw new GnpyException("In gnpyServiceImpl: nodeId is null");
            }
            idx = addNodeToRouteObject(this.mapDisgNodeRefNode.get(node.getNodeId()),idx);
        }

        if (resource
            instanceof
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                    .pce.resource.resource.resource.Link) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                .pce.resource.resource.resource.Link link =
                (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                    .pce.resource.resource.resource.Link) resource;
            idx = addLinkToRouteObject(link.getLinkId(),idx);
        }
        return idx;
    }

    //Create RouteObjectIncludeExclude list in the case of hard constraint
    private void extractHardConstraints(PceConstraints pceHardConstraints) throws GnpyException {
        List<String> listNodeToInclude = getListToInclude(pceHardConstraints);
        if (!listNodeToInclude.isEmpty()) {
            Long index = 0L;
            for (int i = 0; i < listNodeToInclude.size(); i++) {
                String nodeId = listNodeToInclude.get(i);
                index = addNodeToRouteObject(nodeId, index);
            }
        }
    }

    //Create the list of nodes to include
    private List<String> getListToInclude(PceConstraints pceHardConstraints) {
        List<String> listNodeToInclude = new ArrayList<>();
        if (pceHardConstraints != null) {
            List<ResourcePair> listToInclude = pceHardConstraints.getListToInclude();
            Iterator<ResourcePair> it = listToInclude.iterator();
            while (it.hasNext()) {
                ResourcePair rs = it.next();
                if (rs.getType().name().equals("NODE")) {
                    listNodeToInclude.add(rs.getName());
                }
            }
        }
        return listNodeToInclude;
    }

    //Add a node to the route object
    private Long addNodeToRouteObject(String nodeRef, Long index) throws GnpyException {
        Long idx = index;
        for (Elements element : this.elements.values()) {
            if (element.getUid().equals(nodeRef)) {
                if ((this.currentNodeId == null) || (!this.currentNodeId.equals(nodeRef))) {
                    this.currentNodeId = nodeRef;
                    RouteObjectIncludeExclude routeObjectIncludeExclude = addRouteObjectIncludeExclude(nodeRef,
                            Uint32.valueOf(1), idx);
                    RouteObjectIncludeExcludeKey key = new RouteObjectIncludeExcludeKey(Uint32.valueOf(idx));
                    routeObjectIncludeExcludes.put(key, routeObjectIncludeExclude);
                    idx += 1;
                }
                return idx;
            }
        }
        throw new GnpyException(String.format("In gnpyServiceImpl : NodeRef %s does not exist",nodeRef));
    }

    //Add a link to the route object
    private Long addLinkToRouteObject(String linkId, Long index) throws GnpyException {
        Long idx = index;
        if (linkId == null) {
            throw new GnpyException("In GnpyServiceImpl: the linkId is null");
        }
        //Only the ROADM-to-ROADM link are included in the route object
        if (!mapLinkFiber.containsKey(linkId)) {
            return idx;
        }
        List<String> listSubLink = this.mapLinkFiber.get(linkId);
        if (listSubLink == null) {
            throw new GnpyException(String.format("In gnpyServiceImpl addNodeRouteObject : no sublink in %s",linkId));
        }
        for (String subLink : listSubLink) {
            RouteObjectIncludeExclude routeObjectIncludeExclude =
                addRouteObjectIncludeExclude(subLink, Uint32.valueOf(1),idx);
            RouteObjectIncludeExcludeKey key = new RouteObjectIncludeExcludeKey(Uint32.valueOf(idx));
            routeObjectIncludeExcludes.put(key, routeObjectIncludeExclude);
            idx += 1;
        }
        return idx;
    }

    // Add routeObjectIncludeExclude
    private RouteObjectIncludeExclude addRouteObjectIncludeExclude(String nodeId, Uint32 teTpValue, Long index) {
        NumUnnumHop numUnnumHop = new org.opendaylight.yang.gen.v1.gnpy.path.rev220221.explicit.route.hop.type.num
            .unnum.hop.NumUnnumHopBuilder()
                .setNodeId(nodeId)
                .setLinkTpId(teTpValue.toString())
                .setHopType(TeHopType.STRICT).build();
        Type type1 = new NumUnnumHopBuilder().setNumUnnumHop(numUnnumHop).build();
        // Create routeObjectIncludeExclude element
        return new RouteObjectIncludeExcludeBuilder()
            .setIndex(Uint32.valueOf(index)).setExplicitRouteUsage(RouteIncludeEro.class).setType(type1).build();
    }

    //Create the path constraints
    private PathConstraints createPathConstraints(Long rate, String modulationFormat, FrequencyTHz minFrequency,
            FrequencyTHz maxFrequency) {
        BigDecimal spacing = GridConstant.SLOT_WIDTH_50;
        int mvalue = GridConstant.NB_SLOTS_100G;
        int nvalue = 0;
        if (minFrequency != null && maxFrequency != null && modulationFormat != null) {
            LOG.info("Creating path constraints for rate {}, modulationFormat {}, min freq {}, max freq {}", rate,
                    modulationFormat, minFrequency, maxFrequency);
            ModulationFormat mformat = ModulationFormat.DpQpsk;
            Optional<ModulationFormat> optionalModulationFormat = ModulationFormat.forName(modulationFormat);
            if (optionalModulationFormat.isPresent()) {
                mformat = optionalModulationFormat.get();
            }
            spacing = GridConstant.FREQUENCY_SLOT_WIDTH_TABLE.get(Uint32.valueOf(rate), mformat);
            FrequencyTHz centralFrequency = GridUtils
                    .getCentralFrequency(minFrequency.getValue(), maxFrequency.getValue());
            int centralFrequencyBitSetIndex = GridUtils.getIndexFromFrequency(centralFrequency.getValue());
            mvalue = (int) Math.ceil(spacing.doubleValue() / (GridConstant.GRANULARITY));
            nvalue = GridUtils.getNFromFrequencyIndex(centralFrequencyBitSetIndex);
        }
        LOG.info("Creating path constraints for rate {}, mvalue {}, nvalue {}, spacing {}", rate,
                mvalue, nvalue, spacing);
        EffectiveFreqSlot effectiveFreqSlot = new EffectiveFreqSlotBuilder()
                .setM(Uint32.valueOf(mvalue / 2)).setN(nvalue).build();

        TeBandwidth teBandwidth = new TeBandwidthBuilder()
                .setPathBandwidth(BigDecimal.valueOf(rate * 1e9))
                .setTechnology("flexi-grid").setTrxType("OpenROADM MSA ver. 5.0")
                .setTrxMode(TRX_MODE_TABLE.get(Uint32.valueOf(rate), spacing))
                .setOutputPower(GridUtils.convertDbmW(GridConstant.OUTPUT_POWER_100GB_DBM
                        + 10 * Math.log10(mvalue / (double)GridConstant.NB_SLOTS_100G)))
                .setEffectiveFreqSlot(Map.of(effectiveFreqSlot.key(), effectiveFreqSlot))
                .setSpacing(spacing.multiply(BigDecimal.valueOf(1e9))).build();
        return new PathConstraintsBuilder().setTeBandwidth(teBandwidth).build();
    }

    //Create the synchronization
    private List<Synchronization> extractSynchronization(Uint32 requestId) {
        // Create RequestIdNumber
        List<String> requestIdNumber = new ArrayList<>();
        requestIdNumber.add(requestId.toString());
        // Create a synchronization
        Svec svec = new SvecBuilder().setRelaxable(true)
            .setDisjointness(new TePathDisjointness(true, true, false))
            .setRequestIdNumber(requestIdNumber).build();
        List<Synchronization> synchro = new ArrayList<>();
        Synchronization synchronization1 = new SynchronizationBuilder()
                .setSynchronizationId(Uint32.valueOf(0).toString())
                .setSvec(svec).build();
        synchro.add(synchronization1);
        return (synchro);
    }

    public Map<PathRequestKey, PathRequest> getPathRequest() {
        return pathRequest;
    }

    public void setPathRequest(Map<PathRequestKey, PathRequest> pathRequest) {
        this.pathRequest = pathRequest;
    }

    public List<Synchronization> getSynchronization() {
        return synchronization;
    }

    public void setSynchronization(List<Synchronization> synchronization) {
        this.synchronization = synchronization;
    }

}
