/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.RouteIncludeEro;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.TeHopType;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.TeNodeId;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.TePathDisjointness;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.TeTpId;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.common.constraints_config.TeBandwidth;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.common.constraints_config.TeBandwidthBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.explicit.route.hop.Type;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.explicit.route.hop.type.NumUnnumHopBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.explicit.route.hop.type.num.unnum.hop.NumUnnumHop;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.generic.path.constraints.PathConstraints;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.generic.path.constraints.PathConstraintsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.gnpy.specific.parameters.EffectiveFreqSlot;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.gnpy.specific.parameters.EffectiveFreqSlotBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.path.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.path.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.path.route.objects.explicit.route.objects.RouteObjectIncludeExclude;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.path.route.objects.explicit.route.objects.RouteObjectIncludeExcludeBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.service.PathRequest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.service.PathRequestBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.synchronization.info.Synchronization;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.synchronization.info.SynchronizationBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.synchronization.info.synchronization.Svec;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.synchronization.info.synchronization.SvecBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.Resource;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
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
  //Fix-grid channel width (THz)
    private static final double FIX_CH = 0.05;
  //Number of slot in 50GHz channel (THz)
    private static final int NB_SLOT_BW = 4;
  //Nominal central frequency granularity (THz)
    private static final double SLOT_BW = 0.00625;
  //Minimum channel center frequency (openRoadm spec) (THz)
    private static final double MAX_CENTRAL_FREQ = 196.1;
  //Flex-grid reference channel frequency (THz)
    private static final double FLEX_CENTRAL_FREQ = 193.1;
  //Convert THz to Hz
    private static final double CONVERT_TH_HZ = 1e12;

    private List<PathRequest> pathRequest = new ArrayList<>();
    private List<Synchronization> synchronization = new ArrayList<>();
    private Map<String, String> mapDisgNodeRefNode = new HashMap<>();
    private Map<String, IpAddress> mapNodeRefIp = new HashMap<>();
    private Map<String, List<String>> mapLinkFiber = new HashMap<>();
    private Map<String, IpAddress> mapFiberIp = new HashMap<>();
    private List<String> trxList = new ArrayList<>();
    private List<Elements> elements = new ArrayList<>();
    private List<RouteObjectIncludeExclude> routeObjectIncludeExcludes = new ArrayList<>();
    private IpAddress currentNodeIpAddress = null;

    /*
     * Construct the GnpyServiceImpl
     */
    public GnpyServiceImpl(PathComputationRequestInput input, AToZDirection atoz, Uint32 requestId,
                GnpyTopoImpl gnpyTopo, PceConstraints pceHardConstraints) throws GnpyException {
        this.elements = gnpyTopo.getElements();
        this.mapDisgNodeRefNode = gnpyTopo.getMapDisgNodeRefNode();
        this.mapNodeRefIp = gnpyTopo.getMapNodeRefIp();
        this.mapLinkFiber = gnpyTopo.getMapLinkFiber();
        this.mapFiberIp = gnpyTopo.getMapFiberIp();
        this.trxList = gnpyTopo.getTrxList();
        try {
            this.pathRequest = extractPathRequest(input, atoz, requestId.toJava(), pceHardConstraints);
            this.synchronization = extractSynchronization(requestId);
        } catch (NullPointerException e) {
            throw new GnpyException("In GnpyServiceImpl: one of the elements is null",e);
        }
    }

    public GnpyServiceImpl(PathComputationRequestInput input, ZToADirection ztoa, Uint32 requestId,
                GnpyTopoImpl gnpyTopo, PceConstraints pceHardConstraints) throws GnpyException {
        this.elements = gnpyTopo.getElements();
        this.mapDisgNodeRefNode = gnpyTopo.getMapDisgNodeRefNode();
        this.mapNodeRefIp = gnpyTopo.getMapNodeRefIp();
        this.mapLinkFiber = gnpyTopo.getMapLinkFiber();
        this.mapFiberIp = gnpyTopo.getMapFiberIp();
        this.trxList = gnpyTopo.getTrxList();
        try {
            pathRequest = extractPathRequest(input, ztoa, requestId.toJava(), pceHardConstraints);
            synchronization = extractSynchronization(requestId);
        } catch (NullPointerException e) {
            throw new GnpyException("In GnpyServiceImpl: one of the elements of service is null",e);
        }
    }

    private List<PathRequest> extractPathRequest(PathComputationRequestInput input, AToZDirection atoz, Long requestId,
        PceConstraints pceHardConstraints) throws GnpyException {

        // Create the source and destination nodes
        String sourceNode = input.getServiceAEnd().getNodeId();
        String destNode = input.getServiceZEnd().getNodeId();
        if (!trxList.contains(sourceNode) || !trxList.contains(destNode)) {
            throw new GnpyException("In GnpyServiceImpl: source and destination should be transmitter nodes");
        }

        // Create explicitRouteObjects
        List<AToZ> listAtoZ = atoz.getAToZ();
        if (listAtoZ != null) {
            extractRouteObjectIcludeAtoZ(listAtoZ);
        } else {
            extractHardConstraints(pceHardConstraints);
        }
        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
            .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();
        //Create Path Constraint
        Long atozWavelength = null;
        if (atoz.getAToZWavelengthNumber() != null) {
            atozWavelength = atoz.getAToZWavelengthNumber().toJava();
        }
        PathConstraints pathConstraints = createPathConstraints(atoz.getRate().toJava(), atozWavelength);

        // Create the path request
        List<PathRequest> pathRequestList = new ArrayList<>();
        PathRequest pathRequestEl = new PathRequestBuilder().setRequestId(requestId)
            .setSource(this.mapNodeRefIp.get(sourceNode)).setDestination(this.mapNodeRefIp.get(destNode))
            .setSrcTpId("srcTpId".getBytes(StandardCharsets.UTF_8))
            .setDstTpId("dstTpId".getBytes(StandardCharsets.UTF_8))
            .setBidirectional(false).setPathConstraints(pathConstraints).setPathConstraints(pathConstraints)
            .setExplicitRouteObjects(explicitRouteObjects).build();
        pathRequestList.add(pathRequestEl);
        LOG.debug("In GnpyServiceImpl: path request AToZ is extracted");
        return pathRequestList;
    }

    private List<PathRequest> extractPathRequest(PathComputationRequestInput input, ZToADirection ztoa, Long requestId,
        PceConstraints pceHardConstraints) throws GnpyException {
        // Create the source and destination nodes
        String sourceNode = input.getServiceZEnd().getNodeId();
        String destNode = input.getServiceAEnd().getNodeId();
        if (!trxList.contains(sourceNode) || !trxList.contains(destNode)) {
            throw new GnpyException("In GnpyServiceImpl: source and destination should be transmitter nodes");
        }
        // Create explicitRouteObjects
        List<ZToA> listZtoA = ztoa.getZToA();
        if (listZtoA != null) {
            extractRouteObjectIcludeZtoA(listZtoA);
        } else {
            extractHardConstraints(pceHardConstraints);
        }
        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
            .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();
        //Create Path Constraint
        Long ztoaWavelength = null;
        if (ztoa.getZToAWavelengthNumber() != null) {
            ztoaWavelength = ztoa.getZToAWavelengthNumber().toJava();
        }
        PathConstraints pathConstraints = createPathConstraints(ztoa.getRate().toJava(), ztoaWavelength);

        // Create the path request
        List<PathRequest> pathRequestList = new ArrayList<>();
        PathRequest pathRequestEl = new PathRequestBuilder().setRequestId(requestId)
            .setSource(this.mapNodeRefIp.get(sourceNode)).setDestination(this.mapNodeRefIp.get(destNode))
            .setSrcTpId("srcTpId".getBytes(StandardCharsets.UTF_8))
            .setDstTpId("dstTpId".getBytes(StandardCharsets.UTF_8))
            .setBidirectional(false).setPathConstraints(pathConstraints)
            .setExplicitRouteObjects(explicitRouteObjects).build();
        pathRequestList.add(pathRequestEl);
        LOG.debug("In GnpyServiceImpl: path request ZToA is extracted");
        return pathRequestList;
    }

    //Extract RouteObjectIncludeExclude list in the case of pre-computed path A-to-Z
    private void extractRouteObjectIcludeAtoZ(List<AToZ> listAtoZ) throws GnpyException {
        Long index = 0L;
        for (int i = 0; i < listAtoZ.size(); i++) {
            index = createResource(listAtoZ.get(i).getResource().getResource(),index);
        }
    }

    //Extract RouteObjectIncludeExclude list in the case of pre-computed path Z-to-A
    private void extractRouteObjectIcludeZtoA(List<ZToA> listZtoA) throws GnpyException {
        Long index = 0L;
        for (int i = 0; i < listZtoA.size(); i++) {
            index = createResource(listZtoA.get(i).getResource().getResource(),index);
        }
    }

    //Create a new resource node or link
    private Long createResource(@Nullable Resource resource, Long index) throws GnpyException {
        Long idx = index;
        if (resource
            instanceof
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                    .pce.resource.resource.resource.Node) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                .pce.resource.resource.resource.Node node =
                (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                    .pce.resource.resource.resource.Node) resource;
            if (node.getNodeId() == null) {
                throw new GnpyException("In gnpyServiceImpl: nodeId is null");
            }
            idx = addNodeToRouteObject(this.mapDisgNodeRefNode.get(node.getNodeId()),idx);
        }

        if (resource
            instanceof
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                    .pce.resource.resource.resource.Link) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                .pce.resource.resource.resource.Link link =
                (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
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

    // Create the list of nodes to include
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
        IpAddress ipAddress = this.mapNodeRefIp.get(nodeRef);
        if (ipAddress == null) {
            throw new GnpyException(String.format("In gnpyServiceImpl : NodeRef %s does not exist", nodeRef));
        }

        for (Elements element : this.elements) {
            if (element.getUid().contains(ipAddress.getIpv4Address().getValue())) {
                if ((this.currentNodeIpAddress == null) || (!this.currentNodeIpAddress.equals(ipAddress))) {
                    this.currentNodeIpAddress = ipAddress;
                    RouteObjectIncludeExclude routeObjectIncludeExclude =
                        addRouteObjectIncludeExclude(ipAddress, Uint32.valueOf(1),idx);
                    routeObjectIncludeExcludes.add(routeObjectIncludeExclude);
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
            IpAddress fiberIp = this.mapFiberIp.get(subLink);
            if (fiberIp == null) {
                throw new GnpyException(String.format("In gnpyServiceImpl addNodeRouteObject : fiberIp of %s is null",
                    subLink));
            }
            RouteObjectIncludeExclude routeObjectIncludeExclude =
                addRouteObjectIncludeExclude(fiberIp, Uint32.valueOf(1),idx);
            routeObjectIncludeExcludes.add(routeObjectIncludeExclude);
            idx += 1;
        }
        return idx;
    }

    // Add routeObjectIncludeExclude
    private RouteObjectIncludeExclude addRouteObjectIncludeExclude(IpAddress ipAddress, Uint32 teTpValue, Long index) {
        TeNodeId teNodeId = new TeNodeId(ipAddress);
        TeTpId teTpId = new TeTpId(teTpValue);
        NumUnnumHop numUnnumHop = new org.opendaylight.yang.gen.v1.gnpy.path.rev200202.explicit.route.hop.type.num
            .unnum.hop.NumUnnumHopBuilder()
                .setNodeId(teNodeId.getIpv4Address().getValue())
                .setLinkTpId(teTpId.getUint32().toString())
                .setHopType(TeHopType.STRICT).build();
        Type type1 = new NumUnnumHopBuilder().setNumUnnumHop(numUnnumHop).build();
        // Create routeObjectIncludeExclude element
        return new RouteObjectIncludeExcludeBuilder()
            .setIndex(index).setExplicitRouteUsage(RouteIncludeEro.class).setType(type1).build();
    }

    //Create the path constraints
    private PathConstraints createPathConstraints(Long rate, Long wavelengthNumber) {
        // Create EffectiveFreqSlot
        int freqNdex = 0;
        if (wavelengthNumber != null) {
            double freq = (MAX_CENTRAL_FREQ - FIX_CH * (wavelengthNumber - 1));
            freqNdex = (int) Math.round((freq - FLEX_CENTRAL_FREQ) / SLOT_BW);
        }
        List<EffectiveFreqSlot> effectiveFreqSlot = new ArrayList<>();
        EffectiveFreqSlot effectiveFreqSlot1 = new EffectiveFreqSlotBuilder().setM(NB_SLOT_BW).setN(freqNdex).build();
        effectiveFreqSlot.add(effectiveFreqSlot1);
        // Create Te-Bandwidth
        TeBandwidth teBandwidth = new TeBandwidthBuilder().setPathBandwidth(new BigDecimal(rate))
            .setTechnology("flexi-grid").setTrxType("openroadm-beta1")
            .setTrxMode("W100G").setEffectiveFreqSlot(effectiveFreqSlot)
            .setSpacing(BigDecimal.valueOf(FIX_CH * CONVERT_TH_HZ)).build();
        return new PathConstraintsBuilder().setTeBandwidth(teBandwidth).build();
    }

    //Create the synchronization
    private List<Synchronization> extractSynchronization(Uint32 requestId) {
        // Create RequestIdNumber
        List<Uint32> requestIdNumber = new ArrayList<>();
        requestIdNumber.add(requestId);
        // Create a synchronization
        Svec svec = new SvecBuilder().setRelaxable(true)
            .setDisjointness(new TePathDisjointness(true, true, false))
            .setRequestIdNumber(requestIdNumber).build();
        List<Synchronization> synchro = new ArrayList<>();
        Synchronization synchronization1 = new SynchronizationBuilder().setSynchronizationId(Long.valueOf(0))
                .setSvec(svec).build();
        synchro.add(synchronization1);
        return (synchro);
    }

    public List<PathRequest> getPathRequest() {
        return pathRequest;
    }

    public void setPathRequest(List<PathRequest> pathRequest) {
        this.pathRequest = pathRequest;
    }

    public List<Synchronization> getSynchronization() {
        return synchronization;
    }

    public void setSynchronization(List<Synchronization> synchronization) {
        this.synchronization = synchronization;
    }

}
