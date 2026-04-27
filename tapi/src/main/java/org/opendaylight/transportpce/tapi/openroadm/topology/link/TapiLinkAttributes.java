/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;

/**
 * Representation of TAPI link attributes derived from an OpenROADM {@link Link}.
 *
 * <p>This record encapsulates the mapping between OpenROADM link semantics and the
 * corresponding TAPI link attributes used in TransportPCE. It translates link types
 * and augmentations into TAPI-specific qualifiers and protocol information.
 *
 * <p>The mapping logic is primarily driven by:
 * <ul>
 *   <li>{@link OpenroadmLinkType} (mandatory)</li>
 *   <li>Optional OTN link-type augmentation</li>
 * </ul>
 *
 * <p>Special handling applies for {@link OpenroadmLinkType#OTNLINK}, where additional
 * augmentation data is used to distinguish between photonic media and ODU-based links.
 */
public record TapiLinkAttributes(
        String tapiLinkType,
        String sourceNodeQualifier,
        String destinationNodeQualifier,
        String sourceTpQualifier,
        String destinationTpQualifier,
        LayerProtocolName layerProtocolName) {

    public static TapiLinkAttributes fromOpenRoadmLink(Link link) {
        Link1 link1 = link.augmentation(Link1.class);
        if (link1 == null) {
            throw new IllegalArgumentException("Can't process a link without a type");
        }

        OpenroadmLinkType linkType = link1.getLinkType();

        if (linkType == null) {
            throw new IllegalArgumentException("Can't process a link with null link type");
        }

        if (OpenroadmLinkType.OTNLINK.equals(linkType)) {
            return fromOpenRoadmOtnLinkType(link);
        }

        return fromOpenRoadmLinkType(linkType);
    }

    private static TapiLinkAttributes fromOpenRoadmOtnLinkType(Link link) {
        var otnLinkTypeAugmentation = link.augmentation(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.Link1.class);

        if (otnLinkTypeAugmentation != null) {
            switch (otnLinkTypeAugmentation.getOtnLinkType()) {
                case OTU4 -> {
                    return new TapiLinkAttributes(
                            TapiConstants.OTN_XPDR_XPDR_LINK,
                            TapiConstants.XPDR,
                            TapiConstants.XPDR,
                            TapiConstants.I_OTSI,
                            TapiConstants.I_OTSI,
                            LayerProtocolName.PHOTONICMEDIA);
                }
                case ODTU4 -> {
                    return new TapiLinkAttributes(
                            TapiConstants.OTN_XPDR_XPDR_LINK,
                            TapiConstants.XPDR,
                            TapiConstants.XPDR,
                            TapiConstants.E_ODU,
                            TapiConstants.E_ODU,
                            LayerProtocolName.ODU);
                }
                case null, default -> {
                    return new TapiLinkAttributes(
                            TapiConstants.OTN_XPDR_XPDR_LINK,
                            TapiConstants.XPDR,
                            TapiConstants.XPDR,
                            TapiConstants.ODU,
                            TapiConstants.ODU,
                            LayerProtocolName.PHOTONICMEDIA);
                }
            }
        } else {
            return new TapiLinkAttributes(
                    TapiConstants.OTN_XPDR_XPDR_LINK,
                    TapiConstants.PHTNC_MEDIA,
                    TapiConstants.PHTNC_MEDIA,
                    TapiConstants.PHTNC_MEDIA_OTS,
                    TapiConstants.PHTNC_MEDIA_OTS,
                    LayerProtocolName.PHOTONICMEDIA);
        }
    }

    private static TapiLinkAttributes fromOpenRoadmLinkType(OpenroadmLinkType sourceLinkType) {
        switch (sourceLinkType) {
            case ROADMTOROADM,
                 ADDLINK,
                 DROPLINK -> {
                return new TapiLinkAttributes(
                        TapiConstants.OMS_RDM_RDM_LINK,
                        TapiConstants.PHTNC_MEDIA,
                        TapiConstants.PHTNC_MEDIA,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        LayerProtocolName.PHOTONICMEDIA);
            }

            case EXPRESSLINK -> {
                return new TapiLinkAttributes(
                        TapiConstants.TRANSITIONAL_LINK,
                        TapiConstants.PHTNC_MEDIA,
                        TapiConstants.PHTNC_MEDIA,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        LayerProtocolName.PHOTONICMEDIA);
            }

            case XPONDERINPUT -> {
                return new TapiLinkAttributes(
                        TapiConstants.OMS_XPDR_RDM_LINK,
                        TapiConstants.PHTNC_MEDIA,
                        TapiConstants.XPDR,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        LayerProtocolName.PHOTONICMEDIA);
            }

            case XPONDEROUTPUT -> {
                return new TapiLinkAttributes(
                        TapiConstants.OMS_XPDR_RDM_LINK,
                        TapiConstants.XPDR,
                        TapiConstants.PHTNC_MEDIA,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        TapiConstants.PHTNC_MEDIA_OTS,
                        LayerProtocolName.PHOTONICMEDIA);
            }

            case OTNLINK -> {
                throw new IllegalStateException("Unsupported link type: " + sourceLinkType);
            }

            default -> throw new IllegalStateException("Unexpected value: " + sourceLinkType);
        }
    }
}
