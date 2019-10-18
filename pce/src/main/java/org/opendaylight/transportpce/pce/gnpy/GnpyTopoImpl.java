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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.amplified.link.section.element.section.element.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.amplified.link.section.element.section.element.ila.Ila;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.OMSAttributes;
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

public class GnpyTopoImpl {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyTopoImpl.class);
    private final NetworkTransactionService networkTransactionService;
    private List<Elements> elements = new ArrayList<>();
    private List<Connections> connections = new ArrayList<>();
    //Mapping elements
    //Mapping between the ord-topo and ord-ntw node
    private Map<String, String> mapDisgNodeRefNode = new HashMap<String, String>();
    //Mapping between the ord-ntw and node ip
    private Map<String, IpAddress> mapNodeRefIp = new HashMap<String, IpAddress>();
    //Mapping between link id and clfi
    private Map<String, String> mapLinkFiber = new HashMap<String, String>();
    //Mapping between fiber clfi and ipAddress
    private Map<String, IpAddress> mapFiberIp = new HashMap<String, IpAddress>();
    private static int convertKmM = 1000;

    /*
     * Construct the ExtractTopoDataStoreImpl.
     */
    @SuppressWarnings("unchecked")
    public GnpyTopoImpl(final NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
        Map<String, List<?>> map = extractTopo();
        if (map.containsKey("Elements")) {
            elements = (List<Elements>) map.get("Elements");
        } else {
            elements = null;
        }
        if (map.containsKey("Connections")) {
            connections = (List<Connections>) map.get("Connections");
        } else {
            connections = null;
        }
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
        try {
            // Initialize the reading of the networkTransactionService
            // read the configuration part of the data broker that concerns
            // the openRoadm topology and get all the nodes
            java.util.Optional<Network> openRoadmTopo = this.networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, insIdOpenRoadmTopo).get();
            java.util.Optional<Network> openRoadmNet = this.networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, insIdrOpenRoadmNet).get();
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
                                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                                    .Node1 commonNetworkNode1 = null;
                                for (Node openRoadmNetNode : openRoadmNetNodeList) {
                                    if (openRoadmNetNode.getNodeId().getValue().equals(nodeRef)) {
                                        openRoadmNetNode1 = openRoadmNetNode.augmentation(Node1.class);
                                        commonNetworkNode1 = openRoadmNetNode.augmentation(org.opendaylight.yang.gen.v1
                                            .http.org.openroadm.common.network.rev181130.Node1.class);
                                        ipAddress = openRoadmNetNode1.getIp();
                                        mapNodeRefIp.put(nodeRef, ipAddress);
                                        break;
                                    }
                                }
                                if (commonNetworkNode1.getNodeType().getName().equals("ROADM")) {
                                //if (((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1)
                                //            openRoadmNetNode1).getNodeType().getName().equals("ROADM")) {
                                    if (!nodesList.contains(nodeRef)) {
                                        Elements element = addElementsRoadm(2, 0, nodeRef, openRoadmNetNode1.getShelf(),
                                                -20, ipAddress.getIpv4Address().getValue().toString());
                                        topoElements.add(element);
                                        nodesList.add(nodeRef);
                                    }
                                } else if (commonNetworkNode1.getNodeType().getName().equals("XPONDER")) {
                                //} else if (((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                                //            .Node1) openRoadmNetNode1).getNodeType().getName().equals("XPONDER")) {
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
                    // 1:EXPRESS-LINK    2:ADD-LINK       3:DROP-LINK
                    // 4:ROADM-To-ROADM  5:XPONDER-INPUT  6:XPONDER-OUTPUT
                    int[] externalLink = {4,5,6};
                    int idFiber = 0;
                    int nbEDFA = 0;
                    if (!linksList.isEmpty()) {
                        LOG.debug("The link list is not empty");
                        for (Link link : linksList) {
                            Link1 link1 = link.augmentation(Link1.class);
                            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                                .Link1 openroadmNetworkLink1 = link.augmentation(org.opendaylight.yang.gen.v1.http.org
                                .openroadm.network.topology.rev181130.Link1.class);
                            int linkType = link1.getLinkType().getIntValue();
                            // the previous line generates a warning
                            //  but the following cast in comment makes the gnpy tox test fail
                            // ((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1) link1)
                            if (IntStream.of(externalLink).anyMatch(x -> x == linkType)) {
                                // Verify if the node is a ROADM
                                String srcId = mapDisgNodeRefNode.get(link.getSource().getSourceNode().getValue());
                                IpAddress srcIp = mapNodeRefIp.get(srcId);
                                String clfi = link1.getClfi();
                                String destId = null;
                                IpAddress destIp = null;
                                // Add the links between amplifiers
                                OMSAttributes omsAttributes = null;
                                if (linkType == 4) {
                                    omsAttributes = openroadmNetworkLink1.getOMSAttributes();
                                }
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
                                                    element1 = addElementsEdfa(2, 0, "RLD", "Lannion_CAS",
                                                            ila.getGain().getValue(), ila.getTilt().getValue(),
                                                            ila.getOutVoaAtt().getValue(), "std_medium_gain",
                                                            ipEdfa.getIpv4Address().getValue().toString());
                                                } else if (amplifiedLink.getSectionElement()
                                                        .getSectionElement() instanceof Span) {
                                                    // Create the location
                                                    IpAddress ipFiber = new IpAddress(
                                                            new Ipv4Address("2.2.2." + idFiber));
                                                    mapLinkFiber.put(link.getLinkId().getValue(), clfi);
                                                    mapFiberIp.put(clfi, ipFiber);
                                                    idFiber++;
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
                                        IpAddress ipFiber = new IpAddress(new Ipv4Address("2.2.2." + idFiber));
                                        mapLinkFiber.put(link.getLinkId().getValue(), clfi);
                                        mapFiberIp.put(clfi, ipFiber);
                                        idFiber++;
                                        double attIn = 0;
                                        double connIn = 0;
                                        double connOut = 0;
                                        String typeVariety = "SSMF";
                                        double length = 0;
                                        // Compute the length of the link
                                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                                            .networks.network.link.oms.attributes.@Nullable Span span =
                                            omsAttributes.getSpan();
                                        List<LinkConcatenation> linkConcatenationList = span.getLinkConcatenation();
                                        for (LinkConcatenation linkConcatenation : linkConcatenationList) {
                                            double srlgLength = linkConcatenation.getSRLGLength();
                                            //convert to kilometer
                                            length += srlgLength / convertKmM;
                                        }
                                        double lossCoef = span.getSpanlossCurrent().getValue().doubleValue() / length;
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
                                        clfi = "Fiber" + idFiber;
                                        IpAddress ipFiber = new IpAddress(new Ipv4Address("2.2.2." + idFiber));
                                        mapLinkFiber.put(link.getLinkId().getValue(), clfi);
                                        mapFiberIp.put(clfi, ipFiber);
                                        idFiber++;
                                        // Create a new element
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
                                    LOG.warn("The oms attributes is null {} !",link1.getLinkType().getName());
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
            this.networkTransactionService.close();
        }
        this.networkTransactionService.close();
        map.put("Elements", topoElements);
        map.put("Connections", topoConnections);
        return map;
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
                .setOperational(operational).build();
        Elements element1 = new ElementsBuilder().setUid(uidEdfa)
                // Choose an ip address
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

    public Map<String, String> getMapDisgNodeRefNode() {
        return mapDisgNodeRefNode;
    }

    public void setMapDisgNodeRefNode(Map<String, String> mapDisgNodeRefNode) {
        this.mapDisgNodeRefNode = mapDisgNodeRefNode;
    }

    public Map<String, IpAddress> getMapNodeRefIp() {
        return mapNodeRefIp;
    }

    public void setMapNodeRefIp(Map<String, IpAddress> mapNodeRefIp) {
        this.mapNodeRefIp = mapNodeRefIp;
    }

    public Map<String, String> getMapLinkFiber() {
        return mapLinkFiber;
    }

    public void setMapLinkFiber(Map<String, String> mapLinkFiber) {
        this.mapLinkFiber = mapLinkFiber;
    }

    public Map<String, IpAddress> getMapFiberIp() {
        return mapFiberIp;
    }

    public void setMapFiberIp(Map<String, IpAddress> mapFiberIp) {
        this.mapFiberIp = mapFiberIp;
    }
}
