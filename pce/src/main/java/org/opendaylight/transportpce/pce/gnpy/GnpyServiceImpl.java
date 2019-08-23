/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.RouteIncludeEro;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.TeHopType;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.TeNodeId;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.TePathDisjointness;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.TeTpId;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.common.constraints_config.TeBandwidth;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.common.constraints_config.TeBandwidthBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.explicit.route.hop.Type;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.explicit.route.hop.type.NumUnnumHopBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.explicit.route.hop.type.num.unnum.hop.NumUnnumHop;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.generic.path.constraints.PathConstraints;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.generic.path.constraints.PathConstraintsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.gnpy.specific.parameters.EffectiveFreqSlot;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.gnpy.specific.parameters.EffectiveFreqSlotBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.path.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.path.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.path.route.objects.explicit.route.objects.RouteObjectIncludeExclude;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.path.route.objects.explicit.route.objects.RouteObjectIncludeExcludeBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.service.PathRequest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.service.PathRequestBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.synchronization.info.Synchronization;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.synchronization.info.SynchronizationBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.synchronization.info.synchronization.Svec;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.synchronization.info.synchronization.SvecBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to create the topology corresponding to GNPy requirements.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class GnpyServiceImpl {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyServiceImpl.class);
    private List<PathRequest> pathRequest = new ArrayList<>();
    private List<Synchronization> synchronization = new ArrayList<>();
    private Map<String, String> mapDisgNodeRefNode = new HashMap<String, String>();
    private Map<String, IpAddress> mapNodeRefIp = new HashMap<String, IpAddress>();
    private Map<String, String> mapLinkFiber = new HashMap<String, String>();
    private Map<String, IpAddress> mapFiberIp = new HashMap<String, IpAddress>();
    private List<Elements> elements = new ArrayList<>();

    /*
     * Construct the GnpyServiceImpl
     */
    public GnpyServiceImpl(PathComputationRequestInput input, AToZDirection atoz, Long requestId, GnpyTopoImpl gnpyTopo,
        PceConstraints pceHardConstraints) {
        this.elements = gnpyTopo.getElements();
        this.mapDisgNodeRefNode = gnpyTopo.getMapDisgNodeRefNode();
        this.mapNodeRefIp = gnpyTopo.getMapNodeRefIp();
        this.mapLinkFiber = gnpyTopo.getMapLinkFiber();
        this.mapFiberIp = gnpyTopo.getMapFiberIp();

        this.pathRequest = extractPathRequest(input, atoz, requestId, pceHardConstraints);
        this.synchronization = extractSynchronization(requestId);
    }

    public GnpyServiceImpl(PathComputationRequestInput input, ZToADirection ztoa, Long requestId, GnpyTopoImpl gnpyTopo,
        PceConstraints pceHardConstraints) {
        this.elements = gnpyTopo.getElements();
        this.mapDisgNodeRefNode = gnpyTopo.getMapDisgNodeRefNode();
        this.mapNodeRefIp = gnpyTopo.getMapNodeRefIp();
        this.mapLinkFiber = gnpyTopo.getMapLinkFiber();
        this.mapFiberIp = gnpyTopo.getMapFiberIp();

        pathRequest = extractPathRequest(input, ztoa, requestId, pceHardConstraints);
        synchronization = extractSynchronization(requestId);
    }

    //Create the pathRequest
    public List<PathRequest> extractPathRequest(PathComputationRequestInput input, AToZDirection atoz, Long requestId,
        PceConstraints pceHardConstraints) {
        // 1.1 Create explicitRouteObjects
        // 1.1.1. create RouteObjectIncludeExclude list
        List<RouteObjectIncludeExclude> routeObjectIncludeExcludes = new ArrayList<>();
        IpAddress ipAddressCurrent = null;
        Long index = (long) 0;
        // List of A to Z
        List<AToZ> listAtoZ = atoz.getAToZ();
        if (listAtoZ != null) {
            int atozSize = listAtoZ.size();
            for (int i = 0; i < atozSize; i++) {
                String nodeId = null;
                if (listAtoZ.get(i).getResource().getResource()
                    instanceof
                        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Node) {
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                        .pce.resource.resource.resource.Node node =
                        (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Node) listAtoZ.get(i).getResource().getResource();

                    nodeId = node.getNodeId();
                    if (nodeId != null) {
                        String nodeRef = this.mapDisgNodeRefNode.get(nodeId);
                        IpAddress ipAddress = this.mapNodeRefIp.get(nodeRef);
                        for (Elements element : this.elements) {
                            if (element.getUid().contains(ipAddress.getIpv4Address().getValue().toString())) {
                                if ((ipAddressCurrent == null) || (ipAddressCurrent != ipAddress)) {
                                    ipAddressCurrent = ipAddress;
                                    // Fill in routeObjectIncludeExcludes
                                    RouteObjectIncludeExclude routeObjectIncludeExclude1 = addRouteObjectIncludeExclude(
                                        ipAddress, 1, index);
                                    routeObjectIncludeExcludes.add(routeObjectIncludeExclude1);
                                    index++;
                                }
                                break;
                            }
                        }
                    } else {
                        LOG.warn("node ID is null");
                    }
                    // TODO else if termination point not implemented in this
                    // version
                } else if (listAtoZ.get(i).getResource().getResource()
                    instanceof
                        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Link) {
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                        .pce.resource.resource.resource.Link link =
                        (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Link) listAtoZ.get(i).getResource().getResource();

                    String clfi = this.mapLinkFiber.get(link.getLinkId());
                    IpAddress fiberIp = this.mapFiberIp.get(clfi);
                    if (clfi != null) {
                        RouteObjectIncludeExclude routeObjectIncludeExclude1 = addRouteObjectIncludeExclude(fiberIp, 1,
                            index);
                        routeObjectIncludeExcludes.add(routeObjectIncludeExclude1);
                        index++;
                    }
                }
            }
        }
        else {
            routeObjectIncludeExcludes = extractHardConstraints(pceHardConstraints);
            //TODO integrate the maxLatency/ Metric and max OSNR as additional constraints to GNPy
        }

        // 1.1. Create ExplicitRouteObjects
        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
            .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();
        // 1.2 Create a path constraints
        Long rate = atoz.getRate();
        // 1.2.1. Create EffectiveFreqSlot
        List<EffectiveFreqSlot> effectiveFreqSlot = new ArrayList<>();
        EffectiveFreqSlot effectiveFreqSlot1 = new EffectiveFreqSlotBuilder().setM(5).setN(8).build();
        effectiveFreqSlot.add(effectiveFreqSlot1);
        // 1.2.2. Create Te-Bandwidth
        TeBandwidth teBandwidth = new TeBandwidthBuilder().setPathBandwidth(new BigDecimal(rate))
            .setTechnology("flexi-grid").setTrxType("openroadm-beta1").setTrxMode("W100G")
            .setEffectiveFreqSlot(effectiveFreqSlot).setSpacing(new BigDecimal(50000000000.0)).build();
        PathConstraints pathConstraints = new PathConstraintsBuilder().setTeBandwidth(teBandwidth).build();
        // 1.3. Create the source and destination nodes
        String sourceNode = input.getServiceAEnd().getNodeId();
        String destNode = input.getServiceZEnd().getNodeId();
        // Create the path-request elements
        // Create the path request
        List<PathRequest> pathRequestList = new ArrayList<>();
        PathRequest pathRequest1 = new PathRequestBuilder().setRequestId(requestId)
            .setSource(this.mapNodeRefIp.get(sourceNode)).setDestination(this.mapNodeRefIp.get(destNode))
            .setSrcTpId("srcTpId".getBytes()).setDstTpId("dstTpId".getBytes()).setPathConstraints(pathConstraints)
            .setExplicitRouteObjects(explicitRouteObjects).build();
        pathRequestList.add(pathRequest1);
        return pathRequestList;
    }

    public List<PathRequest> extractPathRequest(PathComputationRequestInput input, ZToADirection ztoa, Long requestId,
        PceConstraints pceHardConstraints) {
        // 1.1 Create explicitRouteObjects
        // 1.1.1. create RouteObjectIncludeExclude list
        List<RouteObjectIncludeExclude> routeObjectIncludeExcludes = new ArrayList<>();
        IpAddress ipAddressCurrent = null;
        Long index = (long) 0;
        List<ZToA> listZtoA = ztoa.getZToA();
        if (listZtoA != null) {
            int ztoaSize = listZtoA.size();
            for (int i = 0; i < ztoaSize; i++) {
                String nodeId = null;
                if (listZtoA.get(i).getResource().getResource()
                    instanceof
                        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Node) {
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                        .pce.resource.resource.resource.Node node =
                        (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Node) listZtoA.get(i).getResource().getResource();
                    nodeId = node.getNodeId();
                    if (nodeId != null) {
                        String nodeRef = this.mapDisgNodeRefNode.get(nodeId);
                        IpAddress ipAddress = this.mapNodeRefIp.get(nodeRef);
                        for (Elements element : this.elements) {
                            if (element.getUid().contains(ipAddress.getIpv4Address().getValue().toString())) {
                                if ((ipAddressCurrent == null) || (ipAddressCurrent != ipAddress)) {
                                    ipAddressCurrent = ipAddress;
                                    // Fill in routeObjectIncludeExcludes
                                    RouteObjectIncludeExclude routeObjectIncludeExclude1 = addRouteObjectIncludeExclude(
                                        ipAddress, 1, index);
                                    routeObjectIncludeExcludes.add(routeObjectIncludeExclude1);
                                    index++;
                                }
                                break;
                            }
                        }
                    } else {
                        LOG.warn("node ID is null");
                    }
                    // TODO else if termination point not implemented in this
                    // version
                } else if (listZtoA.get(i).getResource().getResource()
                    instanceof
                        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Link) {

                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                        .pce.resource.resource.resource.Link link =
                        (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017
                            .pce.resource.resource.resource.Link) listZtoA.get(i).getResource().getResource();
                    String clfi = this.mapLinkFiber.get(link.getLinkId());
                    IpAddress fiberIp = this.mapFiberIp.get(clfi);
                    if (clfi != null) {
                        RouteObjectIncludeExclude routeObjectIncludeExclude1 =
                            addRouteObjectIncludeExclude(fiberIp, 1, index);
                        routeObjectIncludeExcludes.add(routeObjectIncludeExclude1);
                        index++;
                    }

                }
            }
        } else {
            routeObjectIncludeExcludes = extractHardConstraints(pceHardConstraints);
        }

        // 1.1. Create ExplicitRouteObjects
        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
            .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();
        // 1.2 Create a path constraints
        Long rate = ztoa.getRate();
        // 1.2.1. Create EffectiveFreqSlot
        List<EffectiveFreqSlot> effectiveFreqSlot = new ArrayList<>();
        EffectiveFreqSlot effectiveFreqSlot1 = new EffectiveFreqSlotBuilder().setM(5).setN(8).build();
        effectiveFreqSlot.add(effectiveFreqSlot1);
        // 1.2.2. Create Te-Bandwidth
        TeBandwidth teBandwidth = new TeBandwidthBuilder().setPathBandwidth(new BigDecimal(rate))
            .setTechnology("flexi-grid").setTrxType("openroadm-beta1").setTrxMode("W100G")
            .setEffectiveFreqSlot(effectiveFreqSlot).setSpacing(new BigDecimal(50000000000.0)).build();
        PathConstraints pathConstraints = new PathConstraintsBuilder().setTeBandwidth(teBandwidth).build();
        // 1.3. Create the source and destination nodes
        String sourceNode = input.getServiceZEnd().getNodeId();
        String destNode = input.getServiceAEnd().getNodeId();
        // Create the path-request elements
        // Create the path request
        List<PathRequest> pathRequestList = new ArrayList<>();
        PathRequest pathRequest1 = new PathRequestBuilder().setRequestId(requestId)
            .setSource(this.mapNodeRefIp.get(sourceNode)).setDestination(this.mapNodeRefIp.get(destNode))
            .setSrcTpId("srcTpId".getBytes()).setDstTpId("dstTpId".getBytes()).setPathConstraints(pathConstraints)
            .setExplicitRouteObjects(explicitRouteObjects).build();
        pathRequestList.add(pathRequest1);
        return pathRequestList;
    }

  //Create RouteObjectIncludeExclude list
    public List<RouteObjectIncludeExclude> extractHardConstraints(PceConstraints pceHardConstraints) {
        List<String> listNodeToInclude = getListToInclude(pceHardConstraints);
        List<RouteObjectIncludeExclude> routeObjectIncludeExcludes = new ArrayList<>();
        IpAddress ipAddressCurrent = null;
        Long index = (long) 0;
        if (listNodeToInclude != null) {
            for (int i = 0; i < listNodeToInclude.size(); i++) {
                String nodeId = listNodeToInclude.get(i);
                if (nodeId != null) {
                    IpAddress ipAddress = this.mapNodeRefIp.get(nodeId);
                    for (Elements element : this.elements) {
                        if (element.getUid().contains(ipAddress.getIpv4Address().getValue().toString())) {
                            if ((ipAddressCurrent == null) || (ipAddressCurrent != ipAddress)) {
                                ipAddressCurrent = ipAddress;
                                // Fill in routeObjectIncludeExcludes
                                RouteObjectIncludeExclude routeObjectIncludeExclude1 = addRouteObjectIncludeExclude(
                                    ipAddress, 1, index);
                                routeObjectIncludeExcludes.add(routeObjectIncludeExclude1);
                                index++;
                            }
                            break;
                        }
                    }
                } else {
                    LOG.warn("node ID is null");
                }
            }
        }
        return routeObjectIncludeExcludes;
    }

    // Create the synchronization
    public List<Synchronization> extractSynchronization(Long requestId) {
        // Create RequestIdNumber
        List<Long> requestIdNumber = new ArrayList<>();
        requestIdNumber.add(requestId);
        // Create a synchronization
        Svec svec = new SvecBuilder().setRelaxable(true).setDisjointness(new TePathDisjointness(true, true, false))
            .setRequestIdNumber(requestIdNumber).build();
        List<Synchronization> synchro = new ArrayList<>();
        Synchronization synchronization1 = new SynchronizationBuilder().setSynchronizationId(new Long(0)).setSvec(svec)
            .build();
        synchro.add(synchronization1);
        return (synchro);
    }

    // Add routeObjectIncludeExclude
    private RouteObjectIncludeExclude addRouteObjectIncludeExclude(IpAddress ipAddress, long teTpValue, long index) {
        TeNodeId teNodeId = new TeNodeId(ipAddress);
        TeTpId teTpId = new TeTpId(teTpValue);
        NumUnnumHop numUnnumHop = new org.opendaylight.yang.gen.v1.gnpy.path.rev190502.explicit.route.hop.type.num
            .unnum.hop.NumUnnumHopBuilder().setNodeId(teNodeId.getIpv4Address().getValue().toString())
            .setLinkTpId(teTpId.getUint32().toString()).setHopType(TeHopType.STRICT).build();
        Type type1 = new NumUnnumHopBuilder().setNumUnnumHop(numUnnumHop).build();
        // Create routeObjectIncludeExclude element 1
        RouteObjectIncludeExclude routeObjectIncludeExclude1 = new RouteObjectIncludeExcludeBuilder().setIndex(index)
            .setExplicitRouteUsage(RouteIncludeEro.class).setType(type1).build();
        return routeObjectIncludeExclude1;
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
