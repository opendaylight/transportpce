/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.oms.attributes.Span;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationKey;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtils.class);
    private static final double GLASSCELERITY = 2.99792458 * 1e5 / 1.5;
    private static final double PMD_CONSTANT = 0.04;
    private static final String NETWORK_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG =
        "NetworkUtils: No Link augmentation available. {}";

    private NetworkUtils() {
    }

    public static final String CLLI_NETWORK_ID = "clli-network";

    public static final String UNDERLAY_NETWORK_ID = "openroadm-network";

    public static final String OVERLAY_NETWORK_ID = "openroadm-topology";

    public static final String OTN_NETWORK_ID = "otn-topology";

    public enum Operation {
        CREATE,
        DELETE
    }


//    public static String getCLLI(Node node) {
//        // TODO STUB retrieve CLLI from node. for now it is supporting node ID of the first supp node
//        return node.nonnullSupportingNode().values().iterator().next().getNodeRef().getValue();
//    }

//    public static List<Long> getSRLG(Link link) {
//        Span omsAttributesSpan = getOmsAttributesSpan(link);
//        if (omsAttributesSpan == null) {
//            LOG.debug("No concatenation for this link");
//            return new ArrayList<>();
//        }
//        List<Long> srlgList = new ArrayList<>();
//        Map<LinkConcatenationKey, LinkConcatenation> linkList = omsAttributesSpan.nonnullLinkConcatenation();
//        for (LinkConcatenation lc : linkList.values()) {
//            if (lc != null && lc.getSRLGId() != null) {
//                srlgList.add(lc.getSRLGId().toJava());
//            }
//        }
//        return srlgList;
//    }

//    public static List<Long> getSRLGfromLink(Link link) {
//        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1 linkC = link
//                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class);
//        if (linkC == null) {
//            LOG.error(NETWORK_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG, link.getLinkId().getValue());
//            return new ArrayList<>();
//        }
//        List<Long> srlgList = new ArrayList<>();
//        for (LinkConcatenation lc : linkC.nonnullLinkConcatenation().values()) {
//            if (lc != null && lc.getSRLGId() != null) {
//                srlgList.add(lc.getSRLGId().toJava());
//            } else {
//                LOG.debug("No concatenation or SLRG id for this link");
//            }
//        }
//        return srlgList;
//    }

//    public static String getSupNetworkNode(Node node) {
//        for (SupportingNode snode : node.nonnullSupportingNode().values()) {
//            if (NetworkUtils.UNDERLAY_NETWORK_ID.equals(snode.getNetworkRef().getValue())) {
//                return snode.getNodeRef().getValue();
//            }
//        }
//        return null;
//    }

//    public static String getSupClliNode(Node node) {
//        for (SupportingNode snode : node.nonnullSupportingNode().values()) {
//            if (NetworkUtils.CLLI_NETWORK_ID.equals(snode.getNetworkRef().getValue())) {
//                return snode.getNodeRef().getValue();
//            }
//        }
//        return null;
//    }
//
//    public static SortedMap<String, String> getAllSupNode(Node node) {
//        TreeMap<String, String> allSupNodes = new TreeMap<>();
//        for (SupportingNode supnode : node.nonnullSupportingNode().values()) {
//            allSupNodes.put(supnode.getNetworkRef().getValue(),
//                    supnode.getNodeRef().getValue());
//        }
//        return allSupNodes;
//    }
//
//    public static String getSupLink(Link link) {
//        Iterator<SupportingLink> supportingLinkIterator = link.nonnullSupportingLink().values().iterator();
//        if (!supportingLinkIterator.hasNext()) {
//            return "";
//        }
//        SupportingLink first = supportingLinkIterator.next();
//        if (first == null || first.getLinkRef() == null) {
//            return "";
//        }
//        return first.getLinkRef().toString();
//    }
//
//
//    public static Long getAvailableBandwidth(Link link) {
//        if (link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
//            .Link1.class) != null
//            && link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
//                .Link1.class).getAvailableBandwidth() != null) {
//            return link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
//                .Link1.class).getAvailableBandwidth().toJava();
//        } else {
//            LOG.warn("NetworkUtils no Available Bandwidth available for link {}", link.getLinkId());
//            return 0L;
//        }
//    }
//
//    public static Long getUsedBandwidth(Link link) {
//        if (link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
//            .Link1.class) != null
//            && link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
//                .Link1.class).getUsedBandwidth() != null) {
//            return link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
//                .Link1.class).getUsedBandwidth().toJava();
//        } else {
//            LOG.warn("NetworkUtils no Available Bandwidth available for link {}", link.getLinkId());
//            return 0L;
//        }
//    }

    public static Span getOmsAttributesSpan(Link link) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1 link1 = null;
        link1 =
            link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1.class);

        if (link1 == null) {
            LOG.error(NETWORK_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG, link.getLinkId().getValue());
            return null;
        }
        if (link1.getOMSAttributes() == null) {
            LOG.error("NetworkUtils No Link getOMSAttributes available. {}", link.getLinkId().getValue());
            return null;
        }
        return link1.getOMSAttributes().getSpan();
    }
//
//    public static OpenroadmLinkType calcType(Link link) {
//        Link1 link1 = null;
//        OpenroadmLinkType tmplType = null;
//        // ID and type
//        link1 = link.augmentation(Link1.class);
//        if (link1 == null) {
//            LOG.error(NETWORK_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG, link.getLinkId().getValue());
//            return null;
//        }
//
//        tmplType = link1.getLinkType();
//
//        if (tmplType == null) {
//            LOG.error("NetworkUtils No Link type available. {}", link.getLinkId().getValue());
//            return null;
//        }
//        return tmplType;
//    }

    //Compute the link latency : if the latency is not defined, the latency is computed from the length
    public static Long calcLatency(Link link) {
        var augLinkLatency = link.augmentation(Link1.class).getLinkLatency();
        if (augLinkLatency != null) {
            return augLinkLatency.toJava();
        }
        Double linkLength = calcLength(link);
        if (linkLength == null) {
            LOG.debug("In NetworkUtils: cannot compute the latency for the link {}", link.getLinkId().getValue());
            return 1L;
        }
        LOG.debug("In NetworkUtils: The latency of link {} is extrapolated from link length and == {}",
            link.getLinkId(), linkLength / GLASSCELERITY);
        return (long) Math.ceil(linkLength / GLASSCELERITY);
    }

    public static Double calcLength(Link link) {
        var augLinkLength = link.augmentation(Link1.class).getLinkLength();
        if (augLinkLength != null) {
            return augLinkLength.doubleValue();
        }
        var omsAttribute = getOmsAttributesSpan(link);
        if (omsAttribute == null) {
            LOG.debug("In NetworkUtils: cannot compute the length for the link {}", link.getLinkId().getValue());
            return null;
        }
        double linkLength = 0;
        Map<LinkConcatenationKey, LinkConcatenation> linkConcatenationMap =
            omsAttribute.nonnullLinkConcatenation();
        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry : linkConcatenationMap.entrySet()) {
            // Length is expressed in meter according to OpenROADM MSA
            if (entry == null || entry.getValue() == null || entry.getValue().getSRLGLength() == null) {
                LOG.debug("In NetworkUtils: cannot compute the length for the link {}", link.getLinkId().getValue());
                return null;
            }
            linkLength += entry.getValue().getSRLGLength().doubleValue();
            LOG.debug("In NetworkUtils: The length of the link {} == {}", link.getLinkId(), linkLength / 1000.0);
        }
        return linkLength / 1000.0;
    }

    //Calculate CD and PMD of the link from link length
    private static Map<String, Double> calcCDandPMDfromLength(Double length) {
        return length == null
            ? new HashMap<>()
            : new HashMap<>(
                Map.of(
                    "CD", 16.5 * length,
                    "PMD2", Math.pow(length * PMD_CONSTANT, 2)));
    }

    //Calculate CD and PMD of the link
    public static Map<String, Double> calcCDandPMD(Link link) {
        double linkCd = 0.0;
        double linkPmd2 = 0.0;
        var omsAttributesSpan = getOmsAttributesSpan(link);
        Double length = calcLength(link);
        if (omsAttributesSpan == null) {
            LOG.debug("NetworkUtils {} no OMS present, assume G.652 fiber, calculation based on fiber length of {} km",
                link.getLinkId(), calcLength(link));
            return calcCDandPMDfromLength(calcLength(link));
        }
        Map<LinkConcatenationKey, LinkConcatenation> linkConcatenationMap =
            omsAttributesSpan.nonnullLinkConcatenation();
        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry : linkConcatenationMap.entrySet()) {
            // If the link-concatenation list is not populated or partially populated CD &
            // PMD shall be derived from link-length (expressed in km in OR topology)
            if (entry == null || entry.getValue() == null || entry.getValue().getSRLGLength() == null
                    || entry.getValue().augmentation(LinkConcatenation1.class).getFiberType() == null) {
                if (length > 0.0) {
                    LOG.debug("NetworkUtils: no OMS present; cd & PMD for the link {} extrapolated from link length {}"
                        + "assuming SMF fiber type", link.getLinkId().getValue(), length);
                    return calcCDandPMDfromLength(length);
                }
                // If Link-length upper attributes not present or incorrectly populated, no way
                // to calculate CD & PMD
                LOG.error("In NetworkUtils: no Link length declared and no OMS present for the link {}."
                    + " No Way to compute CD and PMD", link.getLinkId().getValue());
                return Map.of();
            }
            // SRLG length is expressed in OR topology in meter
            var entryAug = entry.getValue().augmentation(LinkConcatenation1.class);
            linkCd += entry.getValue().getSRLGLength().doubleValue() / 1000.0
                * retrieveCdFromFiberType(entryAug.getFiberType());
            if (entryAug.getPmd() == null
                    || entryAug.getPmd().getValue().doubleValue() == 0.0
                    || entryAug.getPmd().getValue().toString().isEmpty()) {
                linkPmd2 += Math.pow(
                    entry.getValue().getSRLGLength().doubleValue() / 1000.0
                        * retrievePmdFromFiberType(entryAug.getFiberType()),
                    2);
            } else {
                linkPmd2 += Math.pow(entryAug.getPmd().getValue().doubleValue(), 2);
            }
        }
        LOG.debug("In NetworkUtils: The CD and PMD2 of link {} are respectively {} ps and {} ps",
            link.getLinkId(), linkCd, linkPmd2);
        return Map.of("CD", linkCd, "PMD2", linkPmd2);
    }

    // compute default spanLoss and power correction from fiber length
    // when no OMS attribute defined
    private static Map<String, Double> calcDefaultSpanLoss(Link link) {
        var augLinkLength = link.augmentation(Link1.class).getLinkLength();
        if (augLinkLength == null || augLinkLength.doubleValue() == 0) {
            LOG.error("In NetworkUtils, no link length present or length declared = 0,"
                + " unable to calculate default span Loss ");
            return new HashMap<>();
        }
        long linkLength = augLinkLength.longValue();
        LOG.warn("In NetworkUtils {}, assume G.652 fiber, calculation "
                + "based on fiber length of {} km and typical loss of 0.25dB per Km ",
            link.getLinkId(), linkLength);
        return new HashMap<>(
            Map.of(
                "SpanLoss", linkLength * 0.25,
                "PoutCorrection", retrievePower(FiberType.Smf)
            ));
    }

    // Compute the attenuation of a span from OMS attribute
    public static Map<String, Double> calcSpanLoss(Link link) {
        var omsAttributesSpan = getOmsAttributesSpan(link);
        if (omsAttributesSpan == null) {
            return calcDefaultSpanLoss(link);
        }
        Collection<LinkConcatenation> linkConcatenationList =
            omsAttributesSpan.nonnullLinkConcatenation().values();
        if (linkConcatenationList == null) {
            LOG.error("In NetworkUtils : Null field in the OmsAttrubtesSpan");
            return calcDefaultSpanLoss(link);
        }
        Iterator<LinkConcatenation> linkConcatenationiterator = linkConcatenationList.iterator();
        if (!linkConcatenationiterator.hasNext()) {
            return calcDefaultSpanLoss(link);
        }
        // Reference of power to be launched at input of ROADM (dBm)
        return new HashMap<>(Map.of(
            "PoutCorrection", retrievePower(
                linkConcatenationiterator.next().augmentation(LinkConcatenation1.class).getFiberType()) - 2.0,
            "SpanLoss", omsAttributesSpan.getSpanlossCurrent().getValue().doubleValue()));
    }

    private static double retrievePower(FiberType fiberType) {
        switch (fiberType) {
            case Smf:
                return 2;
            case Eleaf:
                return 1;
            case Truewavec:
                return -1;
            case Oleaf:
            case Dsf:
            case Truewave:
            case NzDsf:
            case Ull:
            default:
                return 0;
        }
    }

    private static double retrievePmdFromFiberType(FiberType fiberType) {
        return fiberType.toString().equalsIgnoreCase("Dsf")
            ? 0.2
            : PMD_CONSTANT;
    }

    private static double retrieveCdFromFiberType(FiberType fiberType) {
        switch (fiberType) {
            case Dsf:
                return 0.0;
            case Truewavec:
                return 3.0;
            case Eleaf:
            case Oleaf:
            case NzDsf:
                return 4.3;
            case Truewave:
                return 4.4;
            case Smf:
            case Ull:
            default:
                return 16.5;
        }
    }

}
