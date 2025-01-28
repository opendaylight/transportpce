/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.Coordinate;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.Km;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.edfa.params.Operational;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.edfa.params.OperationalBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.Edfa;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.EdfaBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.FiberRoadmBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.Transceiver;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.TransceiverBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.fiberroadm.Params;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.fiberroadm.ParamsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.fiberroadm.params.fiberroadmfused.Fiber;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.fiberroadm.params.fiberroadmfused.FiberBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.fiberroadm.params.fiberroadmfused.Roadm;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.element.type.choice.element.type.fiberroadm.params.fiberroadmfused.RoadmBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.location.attributes.Location;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.location.attributes.LocationBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.topo.Connections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.topo.ConnectionsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.topo.ElementsBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.topo.ElementsKey;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.topo.elements.Metadata;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.topo.elements.MetadataBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.SpanAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes.amplified.link.section.element.section.element.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.amplified.link.attributes.amplified.link.section.element.section.element.ila.Ila;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.OMSAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
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
    private List<String> elementsList = new ArrayList<>();
    //Mapping elements
    //Mapping between the ord-topo and ord-ntw node
    private Map<String, String> mapDisgNodeRefNode = new HashMap<>();
    //Mapping between the ROADM-ROADM linkId/secElement and the linkId
    private Map<String, List<String>> mapLinkFiber = new HashMap<>();
    //List of Xponders
    private List<String> trxList = new ArrayList<>();
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
        extractTopo();
    }

    /*
     * extract the topology
     *
     */
    private void extractTopo() throws GnpyException {
        // Define the instance identifier of the OpenRoadm topology
        DataObjectIdentifier<Network> insIdOpenRoadmTopo = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
                .build();
        // Define the instance identifier of the OpenRoadm network
        DataObjectIdentifier<Network> insIdrOpenRoadmNet = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_NETWORK)))
                .build();
        try {
            // Initialize the reading of the networkTransactionService
            // read the configuration part of the data broker that concerns the openRoadm topology and get all the nodes
            Optional<Network> openRoadmTopo = this.networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, insIdOpenRoadmTopo).get();
            Optional<Network> openRoadmNet = this.networkTransactionService
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
            throw new GnpyException("In gnpyTopoImpl: error in reading the topology", e);
        }
    }

    private void extractElements(java.util.Optional<Network> openRoadmTopo,
            java.util.Optional<Network> openRoadmNet) throws GnpyException {
        if ((!openRoadmNet.isPresent()) || (!openRoadmTopo.isPresent())) {
            throw new GnpyException("In gnpyTopoImpl: openRoadmNet or openRoadmTopo is not present");
        }
        // Create the list of nodes
        Collection<Node> openRoadmNetNodeList = openRoadmNet.orElseThrow().nonnullNode().values();
        Collection<Node> openRoadmTopoNodeList = openRoadmTopo.orElseThrow().nonnullNode().values();

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
                String nodeRef = supportingNode.getNodeRef().getValue();
                if (nodeRef == null) {
                    throw new GnpyException("In gnpyTopoImpl: nodeRef is null");
                }
                // Retrieve the mapping between the openRoadm topology and openRoadm network
                mapDisgNodeRefNode.put(openRoadmTopoNode.getNodeId().getValue(), nodeRef);
                Node1 openRoadmNetNode1 = null;
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                    .Node1 commonNetworkNode1 = null;
                for (Node openRoadmNetNode : openRoadmNetNodeList) {
                    if (openRoadmNetNode.getNodeId().getValue().equals(nodeRef)) {
                        openRoadmNetNode1 = openRoadmNetNode.augmentation(Node1.class);
                        commonNetworkNode1 = openRoadmNetNode.augmentation(org.opendaylight.yang.gen.v1
                            .http.org.openroadm.common.network.rev230526.Node1.class);
                        break;
                    }
                }
                if (commonNetworkNode1 == null) {
                    throw new GnpyException(String.format("In gnpyTopoImpl: the node type of %s is null",nodeRef));
                }
                if (commonNetworkNode1.getNodeType().getName().equals("ROADM")) {
                    if (!elementsList.contains(nodeRef)) {
                        Elements element = createElementsRoadm(LATITUDE, LONGITUTE, nodeRef,
                                openRoadmNetNode1.getShelf(),TARGET_PCH_OUT_DB, nodeRef);
                        this.elements.put(element.key(),element);
                        elementsList.add(nodeRef);
                    }
                } else if (commonNetworkNode1.getNodeType().getName().equals("XPONDER")) {
                    if (!elementsList.contains(nodeRef)) {
                        Elements element = createElementsTransceiver(LATITUDE, LONGITUTE, nodeRef,
                                openRoadmNetNode1.getShelf(), nodeRef);
                        this.elements.put(element.key(),element);
                        elementsList.add(nodeRef);
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
        Network1 nw1 = openRoadmTopo.orElseThrow().augmentation(Network1.class);
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
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                .Link1 openroadmNetworkLink1 = link.augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.network.topology.rev230526.Link1.class);
            if (link1.getLinkType() == null) {
                throw new GnpyException("In gnpyTopoImpl: the link type is null");
            }
            int linkType = link1.getLinkType().getIntValue();
            if (! IntStream.of(externalLink).anyMatch(x -> x == linkType)) {
                continue;
            }

            String srcId = mapDisgNodeRefNode.get(link.getSource().getSourceNode().getValue());
            String linkId = link.getLinkId().getValue();
            String destId = null;
            if (linkType == OpenroadmLinkType.ROADMTOROADM.getIntValue()) {
                OMSAttributes omsAttributes = openroadmNetworkLink1.getOMSAttributes();
                if (omsAttributes == null) {
                    throw new GnpyException(String.format(
                        "In gnpyTopoImpl: OMS attributes do not exit for ROADM to ROADM link: %s",linkId));
                }
                //Case of amplified link
                if (omsAttributes.getAmplifiedLink() != null) {
                    srcId = extractAmplifiedLink(omsAttributes, linkId, srcId);
                }
                //Case of one span link
                if (omsAttributes.getSpan() != null) {
                    srcId = extractSpanLink(omsAttributes, linkId, srcId);
                }
            }
            // Create a new link
            destId = mapDisgNodeRefNode.get(link.getDestination().getDestNode().getValue());
            createNewConnection(srcId,destId);
        }
    }

    private String extractAmplifiedLink(OMSAttributes omsAttributes, String linkId, String srcId)
        throws GnpyException {

        List<AmplifiedLink> amplifiedLinkList = new ArrayList<>(omsAttributes.getAmplifiedLink()
            .nonnullAmplifiedLink().values());
        String destId = null;
        if (!amplifiedLinkList.isEmpty()) {
            for (AmplifiedLink amplifiedLink: amplifiedLinkList) {
                String secElt = amplifiedLink .getSectionEltNumber().toString();
                //Case of ILA
                if (amplifiedLink.getSectionElement().getSectionElement() instanceof Ila) {
                    Ila ila = (Ila) amplifiedLink.getSectionElement().getSectionElement();
                    destId = extractILAFromAmpLink(ila);
                }
                //Case of Span
                if (amplifiedLink.getSectionElement().getSectionElement() instanceof Span) {
                    Span span = (Span) amplifiedLink.getSectionElement().getSectionElement();
                    destId = extractSpan(span.getSpan(), linkId, secElt);
                }
                // Create a new link
                if (createNewConnection(srcId,destId)) {
                    srcId = destId;
                }
            }
        }
        return srcId;
    }

    private String extractSpanLink(OMSAttributes omsAttributes, String linkId, String srcId)
        throws GnpyException {

        SpanAttributes span = omsAttributes.getSpan();
        String destId = extractSpan(span, linkId, linkId);
        if (createNewConnection(srcId, destId)) {
            return destId;
        }
        return srcId;
    }

    private String extractILAFromAmpLink(Ila ila) throws GnpyException {
        String nodeId = ila.getNodeId().getValue();
        mapDisgNodeRefNode.put(nodeId, nodeId);
        Elements element = createElementsEdfa(LATITUDE, LONGITUTE, REGION, CITY,
                ila.getGain().getValue().decimalValue(), ila.getTilt().getValue().decimalValue(),
                ila.getOutVoaAtt().getValue().decimalValue(), "std_medium_gain",
                nodeId);
        this.elements.put(element.key(),element);
        return nodeId;
    }

    private String extractSpan(SpanAttributes span, String linkId, String subLinkId) throws GnpyException {
        if (!mapLinkFiber.containsKey(linkId)) {
            mapLinkFiber.put(linkId, new ArrayList<>());
        }
        mapLinkFiber.get(linkId).add(subLinkId);
        //mapFiberIp.put(subLinkId, ipFiber);
        //fiberId = incrementIdentifier(fiberId);
        double attIn = 0;
        double connIn = 0;
        double connOut = 0;
        String typeVariety = "SSMF";
        double length = 0;
        // Compute the length of the link
        for (LinkConcatenation linkConcatenation : span.nonnullLinkConcatenation().values()) {
            double srlgLength = linkConcatenation.getSRLGLength().doubleValue();
            //convert to kilometer
            length += srlgLength / CONVERT_KM_M;
        }
        if (length == 0) {
            throw new GnpyException(String.format(
                "In gnpyTopoImpl: length of the link %s is equal to zero",linkId));
        }
        double lossCoef = span.getSpanlossCurrent().getValue().doubleValue() / length;
        Elements element = createElementsFiber(LATITUDE, LONGITUTE, REGION, CITY,
                subLinkId, length, attIn, lossCoef, connIn, connOut, typeVariety);
        this.elements.put(element.key(),element);
        return subLinkId;
    }

    /*
     * Method to create Fiber
     */
    private Elements createElementsFiber(double latitude, double longitude, String region, String city, String uidFiber,
            double length, double attIn, double lossCoef, double connIn, double connOut, String typeVariety) {
        // Create an amplifier after the ROADM
        Coordinate c1 = new Coordinate(Decimal64.valueOf(String.valueOf(latitude)));
        Coordinate c2 = new Coordinate(Decimal64.valueOf(String.valueOf(longitude)));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Fiber fiber = new FiberBuilder()
            .setLength(Decimal64.valueOf(String.valueOf(length)))
            .setLengthUnits(Km.VALUE)
            .setAttIn(Decimal64.valueOf(String.valueOf(attIn)))
            .setLossCoef(Decimal64.valueOf(String.valueOf(lossCoef)).scaleTo(5, RoundingMode.CEILING))
            .setConIn(Decimal64.valueOf(String.valueOf(connIn)))
            .setConOut(Decimal64.valueOf(String.valueOf(connOut)))
            .build();
        Params params1 = new ParamsBuilder().setFiberroadmfused(fiber).build();
        return new ElementsBuilder().setUid(uidFiber)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.Fiber.VALUE)
                .setTypeVariety(typeVariety).setMetadata(metadata1)
                .setElementType(new FiberRoadmBuilder().setParams(params1).build()).build();
    }

    /*
     * Method to create EDFA
     */
    private Elements createElementsEdfa(double latitude, double longitude, String region, String city,
            BigDecimal gainTarget, BigDecimal tiltTarget, BigDecimal outVoa, String typeVariety, String uidEdfa) {
        // Create an amplifier after the ROADM
        Coordinate c1 = new Coordinate(Decimal64.valueOf(String.valueOf(latitude)));
        Coordinate c2 = new Coordinate(Decimal64.valueOf(String.valueOf(longitude)));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Operational operational = new OperationalBuilder()
            .setGainTarget(Decimal64.valueOf(gainTarget))
            .setTiltTarget(Decimal64.valueOf(tiltTarget))
            .setOutVoa(Decimal64.valueOf(outVoa))
            .build();
        Edfa edfa = new EdfaBuilder()
                .setOperational(operational).build();
        return new ElementsBuilder().setUid(uidEdfa)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.Edfa.VALUE)
                .setMetadata(metadata1).setElementType(edfa).setTypeVariety(typeVariety).build();
    }

    /*
     * Method to create ROADM
     */
    private Elements createElementsRoadm(double latitude, double longitude, String region, String city,
            double targetPchOutDb, String uidRoadm) {
        Coordinate c1 = new Coordinate(Decimal64.valueOf(String.valueOf(latitude)));
        Coordinate c2 = new Coordinate(Decimal64.valueOf(String.valueOf(longitude)));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Roadm roadm = new RoadmBuilder()
            .setTargetPchOutDb(Decimal64.valueOf(String.valueOf(targetPchOutDb)))
            .build();
        Params params1 = new ParamsBuilder().setFiberroadmfused(roadm).build();
        return new ElementsBuilder().setUid(uidRoadm)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.Roadm.VALUE)
                .setMetadata(metadata1).setElementType(new FiberRoadmBuilder().setParams(params1).build()).build();
    }

    /*
     * Method to create Transceiver
     */
    private Elements createElementsTransceiver(double latitude, double longitude, String region, String city,
            String uidTrans) {
        Coordinate c1 = new Coordinate(Decimal64.valueOf(String.valueOf(latitude)));
        Coordinate c2 = new Coordinate(Decimal64.valueOf(String.valueOf(longitude)));
        Location location1 = new LocationBuilder().setRegion(region).setCity(city).setLatitude(c1).setLongitude(c2)
                .build();
        Metadata metadata1 = new MetadataBuilder().setLocation(location1).build();
        Transceiver transceiver = new TransceiverBuilder().build();
        return new ElementsBuilder().setUid(uidTrans)
                .setType(org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev220615.Transceiver.VALUE)
                .setMetadata(metadata1).setElementType(transceiver).build();
    }

    /*
     * Method to create Connection
     */
    private boolean createNewConnection(String fromNode, String toNode) throws GnpyException {
        if (fromNode == null || toNode == null) {
            throw new GnpyException("create new connection : null node IpAddress");
        }
        if (fromNode.equals(toNode)) {
            return false;
        }
        Connections connection = new ConnectionsBuilder().setFromNode(fromNode).setToNode(toNode).build();
        this.connections.add(connection);
        return true;
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

    public List<String> getElementsList() {
        return elementsList;
    }

    public void setElementsList(List<String> elementsList) {
        this.elementsList = elementsList;
    }

    public Map<String, String> getMapDisgNodeRefNode() {
        return mapDisgNodeRefNode;
    }

    public void setMapDisgNodeRefNode(Map<String, String> mapDisgNodeRefNode) {
        this.mapDisgNodeRefNode = mapDisgNodeRefNode;
    }

    public Map<String, List<String>> getMapLinkFiber() {
        return mapLinkFiber;
    }

    public void setMapLinkFiber(Map<String, List<String>> mapLinkFiber) {
        this.mapLinkFiber = mapLinkFiber;
    }

    public List<String> getTrxList() {
        return trxList;
    }

    public void setTrxList(List<String> trxList) {
        this.trxList = trxList;
    }
}
