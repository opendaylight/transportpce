/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
//import org.opendaylight.yang.gen.v1.gnpy.gnpy.eqpt.config.rev181119.EdfaVariety;
//import org.opendaylight.yang.gen.v1.gnpy.gnpy.eqpt.config.rev181119.FiberVariety;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.Coordinate;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.Km;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.edfa.params.Operational;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.edfa.params.OperationalBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.Edfa;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.EdfaBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.FiberRoadmBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.Transceiver;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.TransceiverBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.fiberroadm.Params;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.fiberroadm.ParamsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.fiberroadm.params.fiberroadm.Fiber;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.fiberroadm.params.fiberroadm.FiberBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.fiberroadm.params.fiberroadm.Roadm;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.element.type.choice.element.type.fiberroadm.params.fiberroadm.RoadmBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.location.attributes.Location;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.location.attributes.LocationBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Connections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.ElementsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.elements.Metadata;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.elements.MetadataBuilder;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.amplified.link.section.element.section.element.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.amplified.link.section.element.section.element.ila.Ila;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.OMSAttributes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to create the topology corresponding to GNPy requirements.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class ExtractTopoDataStoreImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractTopoDataStoreImpl.class);
    private final DataBroker dataBroker;
    // private final OpenRoadmTopology openRoadmTopology;
    // private final OpenRoadmInterfaces openRoadmInterfaces;
    private List<Elements> elements = new ArrayList<>();
    private List<Connections> connections = new ArrayList<>();
    private List<PathRequest> pathRequest = new ArrayList<>();
    private List<Synchronization> synchronization = new ArrayList<>();
    private Map<String, String> mapDisgNodeRefNode = new HashMap<String, String>();
    private Map<String, IpAddress> mapNodeRefIp = new HashMap<String, IpAddress>();
    private Map<String, String> mapLinkFiber = new HashMap<String, String>();
    private Map<String, IpAddress> mapFiberIp = new HashMap<String, IpAddress>();
    private static int convertKmM = 1000;

    /*
     * Construct the ExtractTopoDataStoreImpl.
     */
    @SuppressWarnings("unchecked")
    public ExtractTopoDataStoreImpl(final DataBroker dataBroker, PathComputationRequestInput input, AToZDirection atoz,
            Long requestId) {
        this.dataBroker = dataBroker;
        Map<String, List<?>> map = extractTopo();
        if (map.containsKey("Elements")) {
            elements = (List<Elements>) map.get("Elements");
        } else {
            elements = null;
        }
        if (map.containsKey("Elements")) {
            connections = (List<Connections>) map.get("Connections");
        } else {
            connections = null;
        }
        pathRequest = extractPathRequest(input, atoz, requestId);
        synchronization = extractSynchronization(requestId);
    }

    public ExtractTopoDataStoreImpl(final DataBroker dataBroker, PathComputationRequestInput input, ZToADirection ztoa,
            Long requestId) {
        this.dataBroker = dataBroker;
        Map<String, List<?>> map = extractTopo();
        if (map.containsKey("Elements")) {
            elements = (List<Elements>) map.get("Elements");
        } else {
            elements = null;
        }
        if (map.containsKey("Elements")) {
            connections = (List<Connections>) map.get("Connections");
        } else {
            connections = null;
        }
        pathRequest = extractPathRequest(input, ztoa, requestId);
        synchronization = extractSynchronization(requestId);
    }

    /*
     * extract the topology: all the elements have ipAddress as uid and maintain
     * a mapping structure to map between the nodeId and the ipAddress (uid)
     *
     */
    public Map<String, List<?>> extractTopo() {
        Map<String, List<?>> map = new HashMap<String, List<?>>();
        // Define the elements
        List<Elements> topoElements = new ArrayList<>();
        // Define the connections
        List<Connections> topoConnections = new ArrayList<>();
        // Define the instance identifier of the OpenRoadm topology
        InstanceIdentifier<Network> insIdOpenRoadmTopo = InstanceIdentifier
                .builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();
        // Define the instance identifier of the OpenRoadm network
        InstanceIdentifier<Network> insIdrOpenRoadmNet = InstanceIdentifier
                .builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))).build();
        ReadOnlyTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction();
        // Read the data broker
        try {
            // Initialize the reading of the data broker
            // read the configuration part of the data broker that concerns
            // the openRoadm topology and get all the nodes
            java.util.Optional<Network> openRoadmTopo = readOnlyTransaction
                    .read(LogicalDatastoreType.CONFIGURATION, insIdOpenRoadmTopo).get().toJavaUtil();
            java.util.Optional<Network> openRoadmNet = readOnlyTransaction
                    .read(LogicalDatastoreType.CONFIGURATION, insIdrOpenRoadmNet).get().toJavaUtil();
            if (openRoadmNet.isPresent()) {
                List<Node> openRoadmNetNodeList = openRoadmNet.get().getNode();
                if (openRoadmTopo.isPresent()) {
                    List<Node> openRoadmTopoNodeList = openRoadmTopo.get().getNode();
                    List<String> nodesList = new ArrayList<>();
                    // Create the list of nodes
                    if (!openRoadmTopoNodeList.isEmpty()) {
                        // Create elements
                        for (Node openRoadmTopoNode : openRoadmTopoNodeList) {
                            // Retrieve the supporting node and the type of the
                            // node in openRoadm network
                            List<SupportingNode> supportingNodeList = openRoadmTopoNode.getSupportingNode();
                            for (SupportingNode supportingNode : supportingNodeList) {
                                String nodeRef = supportingNode.getNodeRef().getValue();
                                IpAddress ipAddress = null;
                                // Retrieve the mapping between the openRoadm
                                // topology and openRoadm network
                                mapDisgNodeRefNode.put(openRoadmTopoNode.getNodeId().getValue(), nodeRef);
                                Node1 openRoadmNetNode1 = null;
                                for (Node openRoadmNetNode : openRoadmNetNodeList) {
                                    if (openRoadmNetNode.getNodeId().getValue().equals(nodeRef)) {
                                        openRoadmNetNode1 = openRoadmNetNode.augmentation(Node1.class);
                                        ipAddress = openRoadmNetNode1.getIp();
                                        mapNodeRefIp.put(nodeRef, ipAddress);
                                        break;
                                    }
                                }
                                if (openRoadmNetNode1.getNodeType().getName().equals("ROADM")) {
                                    if (!nodesList.contains(nodeRef)) {
                                        Elements element = addElementsRoadm(2, 0, nodeRef, openRoadmNetNode1.getShelf(),
                                                -20, ipAddress.getIpv4Address().getValue().toString());
                                        topoElements.add(element);
                                        nodesList.add(nodeRef);
                                    }
                                } else if (openRoadmNetNode1.getNodeType().getName().equals("XPONDER")) {
                                    if (!nodesList.contains(nodeRef)) {
                                        Elements element = addElementsTransceiver(2, 0, nodeRef,
                                                openRoadmNetNode1.getShelf(),
                                                ipAddress.getIpv4Address().getValue().toString());
                                        topoElements.add(element);
                                        nodesList.add(nodeRef);
                                    }
                                } else {
                                    LOG.warn("the type is not implemented");
                                }
                            }
                        }
                    } else {
                        LOG.warn("no nodes in the network");
                    }

                    // Create the list of connections
                    Network1 nw1 = openRoadmTopo.get().augmentation(Network1.class);
                    List<Link> linksList = nw1.getLink();
                    // 1:EXPRESS-LINK ; 2:ADD-LINK ; 3:DROP-LINK ;
                    // 4:ROADM-To-ROADM ; 5:XPONDER-INPUT ; 6:XPONDER-OUTPUT
                    int[] externalLink = {4,5,6};
                    int idFiber = 0;
                    int nbEDFA = 0;
                    if (!linksList.isEmpty()) {
                        LOG.warn("The link list is not empty");
                        for (Link link : linksList) {
                            Link1 link1 = link.augmentation(Link1.class);
                            int linkType = link1.getLinkType().getIntValue();
                            if (IntStream.of(externalLink).anyMatch(x -> x == linkType)) {
                                // Verify if the node is a ROADM
                                String srcId = mapDisgNodeRefNode.get(link.getSource().getSourceNode().getValue());
                                IpAddress srcIp = mapNodeRefIp.get(srcId);
                                String destId = null;
                                IpAddress destIp = null;
                                // Add the links between amplifiers
                                OMSAttributes omsAttributes = link1.getOMSAttributes();
                                if (omsAttributes != null) {
                                    if (omsAttributes.getAmplifiedLink() != null) {
                                        List<AmplifiedLink> amplifiedLinkList = omsAttributes.getAmplifiedLink()
                                                .getAmplifiedLink();
                                        if (!amplifiedLinkList.isEmpty()) {
                                            for (AmplifiedLink amplifiedLink : amplifiedLinkList) {
                                                Elements element1 = null;
                                                if (amplifiedLink.getSectionElement()
                                                        .getSectionElement() instanceof Ila) {
                                                    Ila ila = (Ila) amplifiedLink.getSectionElement()
                                                            .getSectionElement();
                                                    String nodeId = ila.getNodeId().getValue();
                                                    IpAddress ipEdfa = new IpAddress(
                                                            new Ipv4Address("1.1.1." + nbEDFA));
                                                    nbEDFA++;
                                                    mapDisgNodeRefNode.put(nodeId, nodeId);
                                                    mapNodeRefIp.put(nodeId, ipEdfa);
                                                    // class std_medium_gain
                                                    // implements EdfaVariety {}
                                                    element1 = addElementsEdfa(2, 0, "RLD", "Lannion_CAS",
                                                            ila.getGain().getValue(), ila.getTilt().getValue(),
                                                            ila.getOutVoaAtt().getValue(), "std_medium_gain",
                                                            ipEdfa.getIpv4Address().getValue().toString());
                                                } else if (amplifiedLink.getSectionElement()
                                                        .getSectionElement() instanceof Span) {
                                                    // Create the location
                                                    Span span = (Span) amplifiedLink.getSectionElement()
                                                            .getSectionElement();
                                                    String clfi = span.getSpan().getClfi();
                                                    IpAddress ipFiber = new IpAddress(
                                                            new Ipv4Address("2.2.2." + idFiber));
                                                    mapLinkFiber.put(link.getLinkId().getValue(), clfi);
                                                    mapFiberIp.put(clfi, ipFiber);
                                                    idFiber++;
                                                    // class SSMF implements
                                                    // FiberVariety {}
                                                    element1 = addElementsFiber(2, 0, "RLD", "Lannion_CAS",
                                                            ipFiber.getIpv4Address().getValue(), 20, 0, 0.2, 0, 0,
                                                            "SSMF");
                                                }
                                                if (element1 != null) {
                                                    topoElements.add(element1);
                                                    destId = element1.getUid();
                                                    destIp = null;
                                                    // Create a new link
                                                    if (srcId != destId) {
                                                        Connections connection = createNewConnection(srcId, srcIp,
                                                                destId, destIp);
                                                        topoConnections.add(connection);
                                                        srcId = destId;
                                                        srcIp = destIp;
                                                    }
                                                }
                                            }
                                        }
                                    } else if (omsAttributes.getSpan() != null) {
                                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                                            .networks.network.link.oms.attributes.@Nullable Span span
                                                = omsAttributes.getSpan();

                                        String clfi = span.getClfi();
                                        IpAddress ipFiber = new IpAddress(new Ipv4Address("2.2.2." + idFiber));
                                        mapLinkFiber.put(link.getLinkId().getValue(), clfi);
                                        mapFiberIp.put(clfi, ipFiber);
                                        idFiber++;

                                        double attIn = span.getSpanlossCurrent().getValue().doubleValue();
                                        double lossCoef = 0.2;
                                        double connIn = 0;
                                        double connOut = 0;
                                        String typeVariety = "SSMF";

                                        double length = 0; //convert to kilometer
                                        // Compute the length of the link
                                        List<LinkConcatenation> linkConcatenationList = span.getLinkConcatenation();
                                        for (LinkConcatenation linkConcatenation : linkConcatenationList) {
                                            double srlgLength = linkConcatenation.getSRLGLength();
                                            length += srlgLength / convertKmM; //convert to kilometer
                                        }

                                        Elements element1 = addElementsFiber(2, 0, "RLD", "Lannion_CAS",
                                                ipFiber.getIpv4Address().getValue(), length, attIn, lossCoef, connIn,
                                                connOut, typeVariety);

                                        topoElements.add(element1);
                                        // Create a new link
                                        destId = element1.getUid();
                                        destIp = null;
                                        if (srcId != destId) {
                                            Connections connection = createNewConnection(srcId, srcIp, destId, destIp);
                                            topoConnections.add(connection);
                                            srcId = destId;
                                            srcIp = destIp;
                                        }
                                    } else {
                                        // Add a fiber
                                        String clfi = "Fiber" + idFiber;
                                        IpAddress ipFiber = new IpAddress(new Ipv4Address("2.2.2." + idFiber));
                                        mapLinkFiber.put(link.getLinkId().getValue(), clfi);
                                        mapFiberIp.put(clfi, ipFiber);
                                        idFiber++;
                                        // Create a new element
                                        // class SSMF implements FiberVariety {}
                                        Elements element1 = addElementsFiber(2, 0, "RLD", "Lannion_CAS",
                                                ipFiber.getIpv4Address().getValue(), 20, 0, 0.2, 0, 0, "SSMF");
                                        topoElements.add(element1);
                                        // Create a new link
                                        destId = element1.getUid();
                                        destIp = null;
                                        if (srcId != destId) {
                                            Connections connection = createNewConnection(srcId, srcIp, destId, destIp);
                                            topoConnections.add(connection);
                                            srcId = destId;
                                            srcIp = destIp;
                                        }
                                    }
                                } else {
                                    LOG.warn("The oms attributes is null!");
                                }
                                // Create a new link
                                destId = mapDisgNodeRefNode.get(link.getDestination().getDestNode().getValue());
                                destIp = mapNodeRefIp.get(destId);
                                Connections connection = createNewConnection(srcId, srcIp, destId, destIp);
                                topoConnections.add(connection);
                            }
                        }
                    } else {
                        LOG.warn("no links in the network");
                    }
                } else {
                    LOG.warn("No nodes in the selected network ...");
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error reading the topology", e);
            readOnlyTransaction.close();
        }
        readOnlyTransaction.close();
        map.put("Elements", topoElements);
        map.put("Connections", topoConnections);
        return map;
    }

    /*
     * Create the pathRequest
     */
    public List<PathRequest> extractPathRequest(PathComputationRequestInput input, AToZDirection atoz, Long requestId) {
        // List of A to Z
        List<AToZ> listAtoZ = atoz.getAToZ();
        int atozSize = listAtoZ.size();
        // String modulationFormat = atoz.getModulationFormat();
        // Create the path request
        List<PathRequest> pathRequestList = new ArrayList<>();
        // Define the instance identifier
        // InstanceIdentifier<Network> nwInstanceIdentifier = InstanceIdentifier
        // .builder(Network.class, new NetworkKey(new
        // NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();

        // read the configuration part of the data broker that concerns the
        // nodes ID and get all the nodes
        // java.util.Optional<Network> networkObject = readOnlyTransaction
        // .read(LogicalDatastoreType.CONFIGURATION,
        // nwInstanceIdentifier).get().toJavaUtil();
        // 1.1 Create explicitRouteObjects
        // 1.1.1. create RouteObjectIncludeExclude list
        List<RouteObjectIncludeExclude> routeObjectIncludeExcludes = new ArrayList<>();
        IpAddress ipAddressCurrent = null;
        Long index = (long) 0;
        //ReadOnlyTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction();
        for (int i = 0; i < atozSize; i++) {
            // String idAtoZ = listAtoZ.get(i).getId();
            String nodeId = null;
            if (listAtoZ.get(i).getResource()
                    .getResource() instanceof org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                    .pathdescription.rev171017.pce.resource.resource.resource.Node) {
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                    .resource.resource.resource.Node node = (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c
                            ._interface.pathdescription.rev171017.pce.resource.resource.resource.Node) listAtoZ
                        .get(i).getResource().getResource();
                nodeId = node.getNodeId();
                if (nodeId != null) {
                    String nodeRef = mapDisgNodeRefNode.get(nodeId);
                    IpAddress ipAddress = mapNodeRefIp.get(nodeRef);
                    for (Elements element : elements) {
                        if (element.getUid().contains(ipAddress.getIpv4Address().getValue().toString())) {
                            String type = element.getType().getName();
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
            } else if (listAtoZ.get(i).getResource()
                    .getResource() instanceof org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                    .pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint) {
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                        .resource.resource.resource.TerminationPoint tp = (org.opendaylight.yang.gen.v1.http.org
                        .transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource
                        .TerminationPoint) listAtoZ.get(i).getResource().getResource();
                // Not used in this version
            } else if (listAtoZ.get(i).getResource()
                    .getResource() instanceof org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                    .pathdescription.rev171017.pce.resource.resource.resource.Link) {
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                    .resource.resource.resource.Link link = (org.opendaylight.yang.gen.v1.http.org.transportpce
                            .b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.Link) listAtoZ
                        .get(i).getResource().getResource();
                String clfi = mapLinkFiber.get(link.getLinkId());
                IpAddress fiberIp = mapFiberIp.get(clfi);
                if (clfi != null) {
                    RouteObjectIncludeExclude routeObjectIncludeExclude1 = addRouteObjectIncludeExclude(fiberIp, 1,
                            index);
                    routeObjectIncludeExcludes.add(routeObjectIncludeExclude1);
                    index++;
                }
            }
        }
        // Create ExplicitRouteObjects
        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
                .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();

        // 1. Create the path request element 1
        // Find parameters
        // String serviceName = input.getServiceName();
        String sourceNode = input.getServiceAEnd().getNodeId();
        String destNode = input.getServiceZEnd().getNodeId();

        // 1.2 Create a path constraints
        Long rate = atoz.getRate();
        // Long wavelengthNumber = atoz.getAToZWavelengthNumber();
        // Create EffectiveFreqSlot
        List<EffectiveFreqSlot> effectiveFreqSlot = new ArrayList<>();
        EffectiveFreqSlot effectiveFreqSlot1 = new EffectiveFreqSlotBuilder().setM(5).setN(8).build();
        effectiveFreqSlot.add(effectiveFreqSlot1);
        // Create Te-Bandwidth
        TeBandwidth teBandwidth = new TeBandwidthBuilder().setPathBandwidth(new BigDecimal(rate))
                .setTechnology("flexi-grid").setTrxType("openroadm-beta1").setTrxMode("W100G")
                .setEffectiveFreqSlot(effectiveFreqSlot).setSpacing(new BigDecimal(50000000000.0)).build();
        // .setMaxNbOfChannel(new Long(80)).setOutputPower(new
        // BigDecimal(0.0012589254117941673))
        PathConstraints pathConstraints = new PathConstraintsBuilder().setTeBandwidth(teBandwidth).build();
        // PathRequest pathRequest1 = new
        // PathRequestBuilder().setRequestId(new
        // Long(0)).setSource(mapNodeRefIp.get(sourceNode))
        // .setDestination(mapNodeRefIp.get(destNode)).setSrcTpId(input.getServiceAEnd().getTxDirection()
        //      .getPort().getPortName().getBytes())
        // .setDstTpId(input.getServiceAEnd().getRxDirection().getPort().getPortName().getBytes())
        //      .setPathConstraints(pathConstraints)
        // .setExplicitRouteObjects(explicitRouteObjects).build();
        PathRequest pathRequest1 = new PathRequestBuilder().setRequestId(requestId)
                .setSource(mapNodeRefIp.get(sourceNode)).setDestination(mapNodeRefIp.get(destNode))
                .setSrcTpId("srcTpId".getBytes()).setDstTpId("dstTpId".getBytes()).setPathConstraints(pathConstraints)
                .setExplicitRouteObjects(explicitRouteObjects).build();
        pathRequestList.add(pathRequest1);
        //readOnlyTransaction.close();
        return pathRequestList;
    }

    public List<PathRequest> extractPathRequest(PathComputationRequestInput input, ZToADirection ztoa, Long requestId) {
        // List of A to Z
        List<ZToA> listZToA = ztoa.getZToA();
        int ztoaSize = listZToA.size();
        // String modulationFormat = ztoa.getModulationFormat();
        // Create the path request
        List<PathRequest> servicePathRequest = new ArrayList<>();
        // Define the instance identifier
        InstanceIdentifier<Network> nwInstanceIdentifier = InstanceIdentifier
                .builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();
        //ReadOnlyTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction();

        // read the configuration part of the data broker that concerns the
        // nodes ID and get all the nodes
        // java.util.Optional<Network> networkObject = readOnlyTransaction
        // .read(LogicalDatastoreType.CONFIGURATION,
        // nwInstanceIdentifier).get().toJavaUtil();
        // 1.1 Create explicitRouteObjects
        // 1.1.1. create RouteObjectIncludeExclude list
        List<RouteObjectIncludeExclude> routeObjectIncludeExcludes = new ArrayList<>();
        IpAddress ipAddressCurrent = null;
        Long index = (long) 0;
        for (int i = 0; i < ztoaSize; i++) {
            // String idZtoA = listZToA.get(i).getId();
            String nodeId = null;
            if (listZToA.get(i).getResource()
                    .getResource() instanceof org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                    .pathdescription.rev171017.pce.resource.resource.resource.Node) {
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                    .resource.resource.resource.Node node = (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c
                    ._interface.pathdescription.rev171017.pce.resource.resource.resource.Node) listZToA.get(i)
                    .getResource().getResource();
                nodeId = node.getNodeId();
                if (nodeId != null) {
                    String nodeRef = mapDisgNodeRefNode.get(nodeId);
                    IpAddress ipAddress = mapNodeRefIp.get(nodeRef);
                    for (Elements element : elements) {
                        if (element.getUid().contains(ipAddress.getIpv4Address().getValue().toString())) {
                            // String type = element.getType().getName();
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
            } else if (listZToA.get(i).getResource()
                    .getResource() instanceof org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                    .pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint) {
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                    .resource.resource.resource.TerminationPoint tp = (org.opendaylight.yang.gen.v1.http.org
                    .transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource
                    .TerminationPoint) listZToA.get(i).getResource().getResource();
                // Not used in this version
            } else if (listZToA.get(i).getResource()
                    .getResource() instanceof org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                    .pathdescription.rev171017.pce.resource.resource.resource.Link) {
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                    .resource.resource.resource.Link link = (org.opendaylight.yang.gen.v1.http.org.transportpce
                    .b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.Link) listZToA.get(i)
                    .getResource().getResource();
                String clfi = mapLinkFiber.get(link.getLinkId());
                IpAddress fiberIp = mapFiberIp.get(clfi);
                if (clfi != null) {
                    RouteObjectIncludeExclude routeObjectIncludeExclude1 = addRouteObjectIncludeExclude(fiberIp, 1,
                            index);
                    routeObjectIncludeExcludes.add(routeObjectIncludeExclude1);
                    index++;
                }
            }
        }
        // Create ExplicitRouteObjects
        ExplicitRouteObjects explicitRouteObjects = new ExplicitRouteObjectsBuilder()
                .setRouteObjectIncludeExclude(routeObjectIncludeExcludes).build();

        // 1. Create the path request element 1
        // Find parameters
        // String serviceName = input.getServiceName();
        String sourceNode = input.getServiceZEnd().getNodeId();
        String destNode = input.getServiceAEnd().getNodeId();

        // 1.2 Create a path constraints
        Long rate = ztoa.getRate();
        // Long wavelengthNumber = ztoa.getZToAWavelengthNumber();
        // Create EffectiveFreqSlot
        List<EffectiveFreqSlot> effectiveFreqSlot = new ArrayList<>();
        EffectiveFreqSlot effectiveFreqSlot1 = new EffectiveFreqSlotBuilder().setM(5).setN(8).build();
        effectiveFreqSlot.add(effectiveFreqSlot1);
        // Create Te-Bandwidth
        TeBandwidth teBandwidth = new TeBandwidthBuilder().setPathBandwidth(new BigDecimal(rate))
                .setTechnology("flexi-grid").setTrxType("openroadm-beta1").setTrxMode("W100G")
                .setEffectiveFreqSlot(effectiveFreqSlot).setSpacing(new BigDecimal(50000000000.0)).build();
        // .setMaxNbOfChannel(new Long(80)).setOutputPower(new
        // BigDecimal(0.0012589254117941673))
        PathConstraints pathConstraints = new PathConstraintsBuilder().setTeBandwidth(teBandwidth).build();
        // PathRequest pathRequest1 = new
        // PathRequestBuilder().setRequestId(new
        // Long(0)).setSource(mapNodeRefIp.get(sourceNode))
        // .setDestination(mapNodeRefIp.get(destNode)).setSrcTpId(input.getServiceAEnd().getTxDirection()
        //      .getPort().getPortName().getBytes())
        // .setDstTpId(input.getServiceAEnd().getRxDirection().getPort().getPortName().getBytes())
        //      .setPathConstraints(pathConstraints)
        // .setExplicitRouteObjects(explicitRouteObjects).build();
        PathRequest pathRequest1 = new PathRequestBuilder().setRequestId(requestId)
                .setSource(mapNodeRefIp.get(sourceNode)).setDestination(mapNodeRefIp.get(destNode))
                .setSrcTpId("srcTpId".getBytes()).setDstTpId("dstTpId".getBytes()).setPathConstraints(pathConstraints)
                .setExplicitRouteObjects(explicitRouteObjects).build();
        servicePathRequest.add(pathRequest1);
        //readOnlyTransaction.close();
        return servicePathRequest;
    }

    /*
     * Create the synchronization
     */
    public List<Synchronization> extractSynchronization(Long requestId) {
        // Create RequestIdNumber
        List<Long> requestIdNumber = new ArrayList<>();
        requestIdNumber.add(0, new Long(0));
        // Create a synchronization
        Svec svec = new SvecBuilder().setRelaxable(true).setDisjointness(new TePathDisjointness(true, true, false))
                .setRequestIdNumber(requestIdNumber).build();
        List<Synchronization> synchro = new ArrayList<>();
        Synchronization synchronization1 = new SynchronizationBuilder().setSynchronizationId(new Long(0)).setSvec(svec)
                .build();
        synchro.add(synchronization1);
        return (synchro);
    }

    /*
     * Method to add Fiber
     */
    private Elements addElementsFiber(double latitude, double longitude, String region, String city, String clfi,
            double length, double attIn, double lossCoef, double connIn, double connOut, String typeVariety) {
        // Create an amplifier after the roadm
        Coordinate c1 = new Coordinate(new BigDecimal(latitude));
        Coordinate c2 = new Coordinate(new BigDecimal(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Fiber fiber = new FiberBuilder().setLength(new BigDecimal(length)).setLengthUnits(Km.class)
                .setAttIn(new BigDecimal(attIn)).setLossCoef(new BigDecimal(lossCoef)).setConIn(new BigDecimal(connIn))
                .setConOut(new BigDecimal(connOut)).build();
        Params params1 = new ParamsBuilder().setFiberroadm(fiber).build();
        // TypeElement Fiber = ; //new TypeElement(Fiber);
        Elements element1 = new ElementsBuilder().setUid(clfi)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.Fiber.class)
                .setTypeVariety(typeVariety).setMetadata(metadata1)
                .setElementType(new FiberRoadmBuilder().setParams(params1).build()).build();
        return element1;
    }

    /*
     * Method to add Edfa
     */
    private Elements addElementsEdfa(double latitude, double longitude, String region, String city,
            BigDecimal gainTarget, BigDecimal tiltTarget, BigDecimal outVoa, String typeVariety, String uidEdfa) {
        // Create an amplifier after the roadm
        Coordinate c1 = new Coordinate(new BigDecimal(latitude));
        Coordinate c2 = new Coordinate(new BigDecimal(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Operational operational = new OperationalBuilder().setGainTarget(gainTarget).setTiltTarget(tiltTarget)
                .setOutVoa(outVoa).build();
        Edfa edfa = new EdfaBuilder()
                // .setTypeVariety(typeVariety)
                .setOperational(operational).build();
        Elements element1 = new ElementsBuilder().setUid(uidEdfa) // Choose an
                                                                  // ip
                                                                  // address
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.Edfa.class)
                .setMetadata(metadata1).setElementType(edfa).setTypeVariety(typeVariety).build();
        return element1;
    }

    /*
     * Method to add ROADM
     */
    private Elements addElementsRoadm(double latitude, double longitude, String region, String city,
            double targetPchOutDb, String uidRoadm) {

        Coordinate c1 = new Coordinate(new BigDecimal(latitude));
        Coordinate c2 = new Coordinate(new BigDecimal(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        // Create the roadm
        Roadm roadm = new RoadmBuilder().setTargetPchOutDb(new BigDecimal(targetPchOutDb)).build();
        Params params1 = new ParamsBuilder().setFiberroadm(roadm).build();
        Elements element1 = new ElementsBuilder().setUid(uidRoadm)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.Roadm.class)
                .setMetadata(metadata1).setElementType(new FiberRoadmBuilder().setParams(params1).build()).build();
        return element1;
    }

    /*
     * Method to add Transceiver
     */
    private Elements addElementsTransceiver(double latitude, double longitude, String region, String city,
            String uidTrans) {
        Coordinate c1 = new Coordinate(new BigDecimal(latitude));
        Coordinate c2 = new Coordinate(new BigDecimal(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Transceiver transceiver = new TransceiverBuilder().build();
        Elements element1 = new ElementsBuilder().setUid(uidTrans)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.Transceiver.class)
                .setMetadata(metadata1).setElementType(transceiver).build();
        return element1;
    }

    /*
     * Add routeObjectIncludeExclude
     */
    private RouteObjectIncludeExclude addRouteObjectIncludeExclude(IpAddress ipAddress, long teTpValue, long index) {
        TeNodeId teNodeId = new TeNodeId(ipAddress);
        TeTpId teTpId = new TeTpId(teTpValue);
        NumUnnumHop numUnnumHop = new org.opendaylight.yang.gen.v1.gnpy.path.rev190502.explicit.route.hop.type.num
            .unnum.hop.NumUnnumHopBuilder()
            .setNodeId(teNodeId.getIpv4Address().getValue().toString()).setLinkTpId(teTpId.getUint32().toString())
            .setHopType(TeHopType.STRICT).build();
        Type type1 = new NumUnnumHopBuilder().setNumUnnumHop(numUnnumHop).build();
        // Create routeObjectIncludeExclude element 1
        RouteObjectIncludeExclude routeObjectIncludeExclude1 = new RouteObjectIncludeExcludeBuilder().setIndex(index)
                .setExplicitRouteUsage(RouteIncludeEro.class).setType(type1).build();
        return routeObjectIncludeExclude1;
    }

    private String fromToNodeForConnection(String id, IpAddress ip) {
        String fromToNode = id;
        if (ip != null) {
            fromToNode = ip.getIpv4Address().getValue().toString();
        }
        return (fromToNode);
    }

    private Connections createNewConnection(String srcId, IpAddress srcIp, String destId, IpAddress destIp) {
        String fromNode = srcId;
        String toNode = destId;
        if (srcIp != null) {
            fromNode = srcIp.getIpv4Address().getValue().toString();
        }
        if (destIp != null) {
            toNode = destIp.getIpv4Address().getValue().toString();
        }
        Connections connection1 = new ConnectionsBuilder().setFromNode(fromNode).setToNode(toNode).build();
        return (connection1);
    }

    public List<Elements> getElements() {
        return elements;
    }

    public void setElements(List<Elements> elements) {
        this.elements = elements;
    }

    public List<Connections> getConnections() {
        return connections;
    }

    public void setConnections(List<Connections> connections) {
        this.connections = connections;
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

    public List<PathRequest> createEmptyPathRequest(PathComputationRequestInput input, AToZDirection atoz) {
        // List of A to Z
        // List<AToZ> listAtoZ = atoz.getAToZ();
        // int atozSize = listAtoZ.size();

        // Create the path request
        List<PathRequest> pathRequestList = new ArrayList<>();

        // 1. Create the path request element 1
        // Find parameters
        // String serviceName = input.getServiceName();
        String sourceNode = input.getServiceAEnd().getNodeId();
        String destNode = input.getServiceZEnd().getNodeId();

        // 1.2 Create a path constraints
        Long rate = atoz.getRate();

        // Create EffectiveFreqSlot
        List<EffectiveFreqSlot> effectiveFreqSlot = new ArrayList<>();
        EffectiveFreqSlot effectiveFreqSlot1 = new EffectiveFreqSlotBuilder().setM(5).setN(8).build();
        effectiveFreqSlot.add(effectiveFreqSlot1);

        // Create Te-Bandwidth
        TeBandwidth teBandwidth = new TeBandwidthBuilder().setPathBandwidth(new BigDecimal(rate))
                .setTechnology("flexi-grid").setTrxType("openroadm-beta1").setTrxMode("W100G")
                .setEffectiveFreqSlot(effectiveFreqSlot).setSpacing(new BigDecimal(50000000000.0)).build();
        PathConstraints pathConstraints = new PathConstraintsBuilder().setTeBandwidth(teBandwidth).build();
        PathRequest pathRequest1 = new PathRequestBuilder().setRequestId(new Long(0))
                .setSource(mapNodeRefIp.get(sourceNode)).setDestination(mapNodeRefIp.get(destNode))
                .setSrcTpId("srcTpId".getBytes()).setDstTpId("dstTpId".getBytes()).setPathConstraints(pathConstraints)
                .build();
        pathRequestList.add(pathRequest1);
        return pathRequestList;
    }

}
