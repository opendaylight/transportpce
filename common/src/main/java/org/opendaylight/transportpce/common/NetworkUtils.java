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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationKey;
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

    public enum Operation {
        CREATE,
        DELETE
    }


    public static Span getOmsAttributesSpan(Link link) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1 link1 =
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
        double latency = Math.ceil(linkLength / GLASSCELERITY);
        LOG.debug("In NetworkUtils: The latency of link {} is extrapolated from link length and == {}",
            link.getLinkId(), latency);
        return (long) latency;
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
        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry :
                omsAttribute.nonnullLinkConcatenation().entrySet()) {
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
        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry :
                omsAttributesSpan.nonnullLinkConcatenation().entrySet()) {
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
