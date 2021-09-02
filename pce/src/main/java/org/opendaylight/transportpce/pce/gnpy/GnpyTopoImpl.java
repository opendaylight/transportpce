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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.Coordinate;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.Km;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.edfa.params.Operational;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.edfa.params.OperationalBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.Edfa;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.EdfaBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.FiberRoadmBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.Transceiver;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.TransceiverBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.fiberroadm.Params;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.fiberroadm.ParamsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.fiberroadm.params.fiberroadm.Fiber;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.fiberroadm.params.fiberroadm.FiberBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.fiberroadm.params.fiberroadm.Roadm;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.element.type.choice.element.type.fiberroadm.params.fiberroadm.RoadmBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.location.attributes.Location;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.location.attributes.LocationBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.topo.Connections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.topo.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.topo.ElementsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.topo.ElementsKey;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.topo.elements.Metadata;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.topo.elements.MetadataBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.SpanAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.amplified.link.section.element.section.element.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.amplified.link.section.element.section.element.ila.Ila;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.link.OMSAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
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
    //List of elements
    private Map<ElementsKey, Elements> elements = new HashMap<>();
    private List<Connections> connections = new ArrayList<>();
    //Mapping elements
    //Mapping between the ord-topo and ord-ntw node
    private Map<String, String> mapDisgNodeRefNode = new HashMap<>();
    //Mapping between the ord-ntw and node ip
    private Map<String, IpAddress> mapNodeRefIp = new HashMap<>();
    //Mapping between the ROADM-ROADM linkId/secElement and the linkId
    private Map<String, List<String>> mapLinkFiber = new HashMap<>();
    //Mapping between the ROADM-ROADM linkId/secElement and ipAddress
    private Map<String, IpAddress> mapFiberIp = new HashMap<>();
    //List of Xponders
    private List<String> trxList = new ArrayList<>();
    //Initialize the 32 bit identifiers for the edfa and the fiber.
    //These 32-bit identifiers are not ipv4 addresses (despite having ipv4Adresses format, dotted-decimal notation).
    //They are imposed by the GNPy yang model to identify network elements and not used for any routing purposes.
    private Ipv4Address edfaId;
    private Ipv4Address fiberId;
    private static final double LATITUDE = 0;
    private static final double LONGITUTE = 0;
    private static final String REGION = "N/A";
    private static final String CITY = "N/A";
    private static final int CONVERT_KM_M = 1000;
    private static final double TARGET_PCH_OUT_DB = -20;

    /*
     * Construct the ExtractTopoDataStoreImpl.
     */
    public GnpyTopoImpl(final NetworkTransactionService networkTransactionService) throws GnpyException {
        this.networkTransactionService = networkTransactionService;
        //32-bit identifier for the fiber. The dotted decimal notation has the format 243.x.x.x (0<=x<=255)
        fiberId = new Ipv4Address("243.0.0.1");
        //32-bit identifier for the edfa. The dotted decimal notation has the format 244.x.x.x (0<=x<=255)
        edfaId = new Ipv4Address("244.0.0.1");
        try {
            extractTopo();
        } catch (NullPointerException e) {
            throw new GnpyException("In GnpyTopoImpl: one of the elements is null",e);
        }
    }

    /*
     * extract the topology: all the elements have ipAddress as uid and maintain
     * a mapping structure to map between the nodeId and the ipAddress (uid)
     *
     */
    private void extractTopo() throws GnpyException {
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
            // read the configuration part of the data broker that concerns the openRoadm topology and get all the nodes
            java.util.Optional<Network> openRoadmTopo = this.networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, insIdOpenRoadmTopo).get();
            java.util.Optional<Network> openRoadmNet = this.networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, insIdrOpenRoadmNet).get();
            if (openRoadmNet.isPresent() && openRoadmTopo.isPresent()) {
                extractElements(openRoadmTopo,openRoadmNet);
                extractConnections(openRoadmTopo);
                LOG.debug("In GnpyTopoImpl : elements and connections are well extracted");
            }
            else {
                throw new GnpyException(
                    "In GnpyTopoImpl : openroadm topology network or openroadm network are not well mounted ...");
            }
        } catch (InterruptedException | ExecutionException e) {
            this.networkTransactionService.close();
            throw new GnpyException("In gnpyTopoImpl: error in reading the topology", e);
        }
        this.networkTransactionService.close();
    }

    private void extractElements(java.util.Optional<Network> openRoadmTopo,
            java.util.Optional<Network> openRoadmNet) throws GnpyException {
        if ((!openRoadmNet.isPresent()) || (!openRoadmTopo.isPresent())) {
            throw new GnpyException("In gnpyTopoImpl: openRoadmNet or openRoadmTopo is not present");
        }
        // Create the list of nodes
        Collection<Node> openRoadmNetNodeList = openRoadmNet.get().nonnullNode().values();
        Collection<Node> openRoadmTopoNodeList = openRoadmTopo.get().nonnullNode().values();
        List<String> nodesList = new ArrayList<>();

        if (openRoadmTopoNodeList.isEmpty() || openRoadmNetNodeList.isEmpty()) {
            throw new GnpyException("In gnpyTopoImpl: no nodes in the openradm topology or openroadm network");
        }
        // Create elements
        for (Node openRoadmTopoNode : openRoadmTopoNodeList) {
            // Retrieve the supporting node and the type of the node in openRoadm network
            Collection<SupportingNode> supportingNodeList = openRoadmTopoNode.nonnullSupportingNode().values();

            for (SupportingNode supportingNode : supportingNodeList) {
                if (!supportingNode.getNetworkRef().getValue().equals("openroadm-network")) {
                    continue;
                }
                IpAddress ipAddress = null;
                String nodeRef = supportingNode.getNodeRef().getValue();
                if (nodeRef == null) {
                    throw new GnpyException("In gnpyTopoImpl: nodeRef is null");
                }
                // Retrieve the mapping between the openRoadm topology and openRoadm network
                mapDisgNodeRefNode.put(openRoadmTopoNode.getNodeId().getValue(), nodeRef);
                Node1 openRoadmNetNode1 = null;
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                    .Node1 commonNetworkNode1 = null;
                for (Node openRoadmNetNode : openRoadmNetNodeList) {
                    if (openRoadmNetNode.getNodeId().getValue().equals(nodeRef)) {
                        openRoadmNetNode1 = openRoadmNetNode.augmentation(Node1.class);
                        commonNetworkNode1 = openRoadmNetNode.augmentation(org.opendaylight.yang.gen.v1
                            .http.org.openroadm.common.network.rev200529.Node1.class);
                        ipAddress = openRoadmNetNode1.getIp();
                        if (ipAddress == null) {
                            throw new GnpyException(String.format(
                                "In gnpyTopoImpl: ipAddress of node %s is null",nodeRef));
                        }
                        mapNodeRefIp.put(nodeRef, ipAddress);
                        break;
                    }
                }
                if (commonNetworkNode1 == null) {
                    throw new GnpyException(String.format("In gnpyTopoImpl: the node type of %s is null",nodeRef));
                }
                if (commonNetworkNode1.getNodeType().getName().equals("ROADM")) {
                    if (!nodesList.contains(nodeRef)) {
                        Elements element = createElementsRoadm(LATITUDE, LONGITUTE, nodeRef,
                                openRoadmNetNode1.getShelf(),TARGET_PCH_OUT_DB, ipAddress.getIpv4Address().getValue());
                        this.elements.put(element.key(),element);
                        nodesList.add(nodeRef);
                    }
                } else if (commonNetworkNode1.getNodeType().getName().equals("XPONDER")) {
                    if (!nodesList.contains(nodeRef)) {
                        Elements element = createElementsTransceiver(LATITUDE, LONGITUTE, nodeRef,
                                openRoadmNetNode1.getShelf(),ipAddress.getIpv4Address().getValue());
                        this.elements.put(element.key(),element);
                        nodesList.add(nodeRef);
                        trxList.add(nodeRef);
                    }
                } else {
                    throw new GnpyException("In gnpyTopoImpl: the type is not implemented");
                }
            }
        }
    }

    private void extractConnections(java.util.Optional<Network> openRoadmTopo) throws GnpyException {
        // Create the list of connections
        if (!openRoadmTopo.isPresent()) {
            throw new GnpyException("In gnpyTopoImpl: openroadmTopo is not present");
        }
        Network1 nw1 = openRoadmTopo.get().augmentation(Network1.class);
        Collection<Link> linksList = nw1.nonnullLink().values();
        // 1:EXPRESS-LINK    2:ADD-LINK       3:DROP-LINK
        // 4:ROADM-To-ROADM  5:XPONDER-INPUT  6:XPONDER-OUTPUT
        int[] externalLink = {OpenroadmLinkType.ROADMTOROADM.getIntValue(),OpenroadmLinkType.XPONDERINPUT.getIntValue(),
            OpenroadmLinkType.XPONDEROUTPUT.getIntValue()};

        if (linksList.isEmpty()) {
            throw new GnpyException("In gnpyTopoImpl: no links in the network");
        }

        for (Link link : linksList) {
            Link1 link1 = link.augmentation(Link1.class);
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529
                .Link1 openroadmNetworkLink1 = link.augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.network.topology.rev200529.Link1.class);
            if (link1.getLinkType() == null) {
                throw new GnpyException("In gnpyTopoImpl: the link type is null");
            }
            int linkType = link1.getLinkType().getIntValue();
            if (! IntStream.of(externalLink).anyMatch(x -> x == linkType)) {
                continue;
            }

            String srcId = mapDisgNodeRefNode.get(link.getSource().getSourceNode().getValue());
            IpAddress srcIp = mapNodeRefIp.get(srcId);
            String linkId = link.getLinkId().getValue();
            String destId = null;
            IpAddress destIp = null;
            if (linkType == OpenroadmLinkType.ROADMTOROADM.getIntValue()) {
                OMSAttributes omsAttributes = openroadmNetworkLink1.getOMSAttributes();
                if (omsAttributes == null) {
                    throw new GnpyException(String.format(
                        "In gnpyTopoImpl: OMS attributes do not exit for ROADM to ROADM link: %s",linkId));
                }
                //Case of amplified link
                if (omsAttributes.getAmplifiedLink() != null) {
                    srcIp = extractAmplifiedLink(omsAttributes, linkId, srcIp);
                }
                //Case of one span link
                if (omsAttributes.getSpan() != null) {
                    srcIp = extractSpanLink(omsAttributes, linkId, srcIp);
                }
            }
            // Create a new link
            destId = mapDisgNodeRefNode.get(link.getDestination().getDestNode().getValue());
            destIp = mapNodeRefIp.get(destId);
            createNewConnection(srcIp,destIp);
        }
    }

    private IpAddress extractAmplifiedLink(OMSAttributes omsAttributes, String linkId, IpAddress srcIp)
        throws GnpyException {

        List<AmplifiedLink> amplifiedLinkList = new ArrayList<>(omsAttributes.getAmplifiedLink()
            .nonnullAmplifiedLink().values());
        IpAddress destIp = null;
        if (!amplifiedLinkList.isEmpty()) {
            for (AmplifiedLink amplifiedLink: amplifiedLinkList) {
                String secElt = amplifiedLink .getSectionEltNumber().toString();
                //Case of ILA
                if (amplifiedLink.getSectionElement().getSectionElement() instanceof Ila) {
                    Ila ila = (Ila) amplifiedLink.getSectionElement().getSectionElement();
                    destIp = extractILAFromAmpLink(ila);
                }
                //Case of Span
                if (amplifiedLink.getSectionElement().getSectionElement() instanceof Span) {
                    Span span = (Span) amplifiedLink.getSectionElement().getSectionElement();
                    destIp = extractSpan(span.getSpan(), linkId, secElt);
                }
                // Create a new link
                if (createNewConnection(srcIp,destIp)) {
                    srcIp = destIp;
                }
            }
        }
        return srcIp;
    }

    private IpAddress extractSpanLink(OMSAttributes omsAttributes, String linkId, IpAddress srcIp)
        throws GnpyException {

        SpanAttributes span = omsAttributes.getSpan();
        IpAddress destIp = extractSpan(span, linkId, linkId);
        if (createNewConnection(srcIp, destIp)) {
            return destIp;
        }
        return srcIp;
    }

    private IpAddress extractILAFromAmpLink(Ila ila) throws GnpyException {
        String nodeId = ila.getNodeId().getValue();
        IpAddress ipEdfa = new IpAddress(edfaId);
        edfaId = incrementIdentifier(edfaId);
        mapDisgNodeRefNode.put(nodeId, nodeId);
        mapNodeRefIp.put(nodeId, ipEdfa);
        Elements element = createElementsEdfa(LATITUDE, LONGITUTE, REGION, CITY,
                ila.getGain().getValue(), ila.getTilt().getValue(),
                ila.getOutVoaAtt().getValue(), "std_medium_gain",
                ipEdfa.getIpv4Address().getValue());
        this.elements.put(element.key(),element);
        return ipEdfa;
    }

    private IpAddress extractSpan(SpanAttributes span, String linkId, String subLinkId) throws GnpyException {
        IpAddress ipFiber = new IpAddress(fiberId);

        if (!mapLinkFiber.containsKey(linkId)) {
            mapLinkFiber.put(linkId, new ArrayList<>());
        }
        mapLinkFiber.get(linkId).add(subLinkId);
        mapFiberIp.put(subLinkId, ipFiber);
        fiberId = incrementIdentifier(fiberId);
        double attIn = 0;
        double connIn = 0;
        double connOut = 0;
        String typeVariety = "SSMF";
        double length = 0;
        // Compute the length of the link
        for (LinkConcatenation linkConcatenation : span.nonnullLinkConcatenation().values()) {
            double srlgLength = linkConcatenation.getSRLGLength().toJava();
            //convert to kilometer
            length += srlgLength / CONVERT_KM_M;
        }
        if (length == 0) {
            throw new GnpyException(String.format(
                "In gnpyTopoImpl: length of the link %s is equal to zero",linkId));
        }
        double lossCoef = span.getSpanlossCurrent().getValue().doubleValue() / length;
        Elements element = createElementsFiber(LATITUDE, LONGITUTE, REGION, CITY,
            ipFiber.getIpv4Address().getValue(), length, attIn, lossCoef, connIn, connOut, typeVariety);
        this.elements.put(element.key(),element);
        return ipFiber;

    }

    /*
     * Method to create Fiber
     */
    private Elements createElementsFiber(double latitude, double longitude, String region, String city, String uidFiber,
            double length, double attIn, double lossCoef, double connIn, double connOut, String typeVariety) {
        // Create an amplifier after the ROADM
        Coordinate c1 = new Coordinate(BigDecimal.valueOf(latitude));
        Coordinate c2 = new Coordinate(BigDecimal.valueOf(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Fiber fiber = new FiberBuilder().setLength(BigDecimal.valueOf(length)).setLengthUnits(Km.class)
                .setAttIn(BigDecimal.valueOf(attIn)).setLossCoef(BigDecimal.valueOf(lossCoef))
                .setConIn(BigDecimal.valueOf(connIn))
                .setConOut(BigDecimal.valueOf(connOut)).build();
        Params params1 = new ParamsBuilder().setFiberroadm(fiber).build();
        return new ElementsBuilder().setUid(uidFiber)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.Fiber.class)
                .setTypeVariety(typeVariety).setMetadata(metadata1)
                .setElementType(new FiberRoadmBuilder().setParams(params1).build()).build();
    }

    /*
     * Method to create EDFA
     */
    private Elements createElementsEdfa(double latitude, double longitude, String region, String city,
            BigDecimal gainTarget, BigDecimal tiltTarget, BigDecimal outVoa, String typeVariety, String uidEdfa) {
        // Create an amplifier after the ROADM
        Coordinate c1 = new Coordinate(BigDecimal.valueOf(latitude));
        Coordinate c2 = new Coordinate(BigDecimal.valueOf(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Operational operational = new OperationalBuilder().setGainTarget(gainTarget).setTiltTarget(tiltTarget)
                .setOutVoa(outVoa).build();
        Edfa edfa = new EdfaBuilder()
                .setOperational(operational).build();
        return new ElementsBuilder().setUid(uidEdfa)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.Edfa.class)
                .setMetadata(metadata1).setElementType(edfa).setTypeVariety(typeVariety).build();
    }

    /*
     * Method to create ROADM
     */
    private Elements createElementsRoadm(double latitude, double longitude, String region, String city,
            double targetPchOutDb, String uidRoadm) {

        Coordinate c1 = new Coordinate(BigDecimal.valueOf(latitude));
        Coordinate c2 = new Coordinate(BigDecimal.valueOf(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Roadm roadm = new RoadmBuilder().setTargetPchOutDb(BigDecimal.valueOf(targetPchOutDb)).build();
        Params params1 = new ParamsBuilder().setFiberroadm(roadm).build();
        return new ElementsBuilder().setUid(uidRoadm)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.Roadm.class)
                .setMetadata(metadata1).setElementType(new FiberRoadmBuilder().setParams(params1).build()).build();
    }

    /*
     * Method to create Transceiver
     */
    private Elements createElementsTransceiver(double latitude, double longitude, String region, String city,
            String uidTrans) {
        Coordinate c1 = new Coordinate(BigDecimal.valueOf(latitude));
        Coordinate c2 = new Coordinate(BigDecimal.valueOf(longitude));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Transceiver transceiver = new TransceiverBuilder().build();
        return new ElementsBuilder().setUid(uidTrans)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev210831.Transceiver.class)
                .setMetadata(metadata1).setElementType(transceiver).build();
    }

    /*
     * Method to create Connection
     */
    private boolean createNewConnection(IpAddress srcIp, IpAddress destIp) throws GnpyException {
        if (srcIp == null || destIp == null) {
            throw new GnpyException("create new connection : null node IpAddress");
        }
        String fromNode = srcIp.getIpv4Address().getValue();
        String toNode = destIp.getIpv4Address().getValue();
        if (fromNode.equals(toNode)) {
            return false;
        }
        Connections connection = new ConnectionsBuilder().setFromNode(fromNode).setToNode(toNode).build();
        this.connections.add(connection);
        return true;
    }

    /*
     * Increment 32-bit identifier
     */
    private Ipv4Address incrementIdentifier(Ipv4Address id)  throws GnpyException {
        String ips = id.getValue();
        String [] fields = ips.split(Pattern.quote("."));
        int intF1 = Integer.parseInt(fields[1]);
        int intF2 = Integer.parseInt(fields[2]);
        int intF3 = Integer.parseInt(fields[3]);
        if (intF3 < 255) {
            intF3++;
        } else {
            if (intF2 < 255) {
                intF2++;
                intF3 = 0;
            } else {
                if (intF1 < 255) {
                    intF1++;
                    intF2 = 0;
                    intF3 = 0;
                } else {
                    throw new GnpyException("GnpyTopoImpl : the topology is not supported by gnpy");
                }
                fields[1] = Integer.toString(intF1);
            }
            fields[2] = Integer.toString(intF2);
        }
        fields[3] = Integer.toString(intF3);
        String nidString = fields[0] + "." + fields[1] + "." + fields[2] + "." + fields[3];
        return new Ipv4Address(nidString);
    }

    public Map<ElementsKey, Elements> getElements() {
        return elements;
    }

    public void setElements(Map<ElementsKey, Elements> elements) {
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

    public Map<String, List<String>> getMapLinkFiber() {
        return mapLinkFiber;
    }

    public void setMapLinkFiber(Map<String, List<String>> mapLinkFiber) {
        this.mapLinkFiber = mapLinkFiber;
    }

    public Map<String, IpAddress> getMapFiberIp() {
        return mapFiberIp;
    }

    public void setMapFiberIp(Map<String, IpAddress> mapFiberIp) {
        this.mapFiberIp = mapFiberIp;
    }

    public List<String> getTrxList() {
        return trxList;
    }

    public void setTrxList(List<String> trxList) {
        this.trxList = trxList;
    }
}
