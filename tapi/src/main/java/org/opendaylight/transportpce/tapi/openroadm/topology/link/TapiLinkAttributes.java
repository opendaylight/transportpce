/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;

public record TapiLinkAttributes(
        String tapiLinkType,
        String sourceNodeQualifier,
        String destinationNodeQualifier,
        String sourceTpQualifier,
        String destinationTpQualifier,
        LayerProtocolName layerProtocolName) {

    public static TapiLinkAttributes fromOpenRoadmLinkType(OpenroadmLinkType sourceLinkType) {
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
