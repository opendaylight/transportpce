/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.OtnLinkType.OTU4;

import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;

class TapiLinkAttributesTest {

    @Test
    void shouldThrowWhenLinkTypeAugmentationMissing() {
        Link link = mock(Link.class);
        when(link.augmentation(Link1.class)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TapiLinkAttributes.fromOpenRoadmLink(link));

        assertEquals("Can't process a link without a type", ex.getMessage());
    }

    @Test
    void shouldMapOtnLinkWithOtu4Subtype() {
        Link link = mock(Link.class);
        Link1 openroadmLink1 = mock(Link1.class);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.Link1 otnAug =
                mock(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902
                        .Link1.class);

        when(link.augmentation(Link1.class)).thenReturn(openroadmLink1);
        when(openroadmLink1.getLinkType()).thenReturn(OpenroadmLinkType.OTNLINK);
        when(link.augmentation(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.Link1.class))
                .thenReturn(otnAug);
        when(otnAug.getOtnLinkType()).thenReturn(OTU4);

        TapiLinkAttributes attrs = TapiLinkAttributes.fromOpenRoadmLink(link);

        assertEquals(TapiConstants.OTN_XPDR_XPDR_LINK, attrs.tapiLinkType());
        assertEquals(TapiConstants.XPDR, attrs.sourceNodeQualifier());
        assertEquals(TapiConstants.XPDR, attrs.destinationNodeQualifier());
        assertEquals(TapiConstants.I_OTSI, attrs.sourceTpQualifier());
        assertEquals(TapiConstants.I_OTSI, attrs.destinationTpQualifier());
        assertEquals(LayerProtocolName.PHOTONICMEDIA, attrs.layerProtocolName());
    }

    @Test
    void shouldMapNullLinkWithOduSubtype() {
        Link link = mock(Link.class);
        Link1 openroadmLink1 = mock(Link1.class);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.Link1 otnAug =
            mock(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.Link1.class);

        when(link.augmentation(Link1.class)).thenReturn(openroadmLink1);
        when(openroadmLink1.getLinkType()).thenReturn(OpenroadmLinkType.OTNLINK);
        when(link.augmentation(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.Link1.class))
                .thenReturn(otnAug);
        when(otnAug.getOtnLinkType()).thenReturn(null);

        TapiLinkAttributes attrs = TapiLinkAttributes.fromOpenRoadmLink(link);

        assertEquals(TapiConstants.OTN_XPDR_XPDR_LINK, attrs.tapiLinkType());
        assertEquals(TapiConstants.XPDR, attrs.sourceNodeQualifier());
        assertEquals(TapiConstants.XPDR, attrs.destinationNodeQualifier());
        assertEquals(TapiConstants.ODU, attrs.sourceTpQualifier());
        assertEquals(TapiConstants.ODU, attrs.destinationTpQualifier());
        assertEquals(LayerProtocolName.PHOTONICMEDIA, attrs.layerProtocolName());
    }

    @Test
    void shouldMapOtnLinkWithoutOtnAugmentationToPhotonicMediaDefaults() {
        Link link = mock(Link.class);
        Link1 openroadmLink1 = mock(Link1.class);

        when(link.augmentation(Link1.class)).thenReturn(openroadmLink1);
        when(openroadmLink1.getLinkType()).thenReturn(OpenroadmLinkType.OTNLINK);
        when(link.augmentation(
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.Link1.class))
                .thenReturn(null);

        TapiLinkAttributes attrs = TapiLinkAttributes.fromOpenRoadmLink(link);

        assertEquals(TapiConstants.OTN_XPDR_XPDR_LINK, attrs.tapiLinkType());
        assertEquals(TapiConstants.PHTNC_MEDIA, attrs.sourceNodeQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA, attrs.destinationNodeQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.sourceTpQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.destinationTpQualifier());
        assertEquals(LayerProtocolName.PHOTONICMEDIA, attrs.layerProtocolName());
    }

    @Test
    void shouldMapRoadmToRoadm() {
        Link link = mock(Link.class);
        Link1 openroadmLink1 = mock(Link1.class);

        when(link.augmentation(Link1.class)).thenReturn(openroadmLink1);
        when(openroadmLink1.getLinkType()).thenReturn(OpenroadmLinkType.ROADMTOROADM);

        TapiLinkAttributes attrs = TapiLinkAttributes.fromOpenRoadmLink(link);

        assertEquals(TapiConstants.OMS_RDM_RDM_LINK, attrs.tapiLinkType());
        assertEquals(TapiConstants.PHTNC_MEDIA, attrs.sourceNodeQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA, attrs.destinationNodeQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.sourceTpQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.destinationTpQualifier());
        assertEquals(LayerProtocolName.PHOTONICMEDIA, attrs.layerProtocolName());
    }

    @Test
    void shouldMapXponderInput() {
        Link link = mock(Link.class);
        Link1 openroadmLink1 = mock(Link1.class);

        when(link.augmentation(Link1.class)).thenReturn(openroadmLink1);
        when(openroadmLink1.getLinkType()).thenReturn(OpenroadmLinkType.XPONDERINPUT);

        TapiLinkAttributes attrs = TapiLinkAttributes.fromOpenRoadmLink(link);

        assertEquals(TapiConstants.OMS_XPDR_RDM_LINK, attrs.tapiLinkType());
        assertEquals(TapiConstants.PHTNC_MEDIA, attrs.sourceNodeQualifier());
        assertEquals(TapiConstants.XPDR, attrs.destinationNodeQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.sourceTpQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.destinationTpQualifier());
        assertEquals(LayerProtocolName.PHOTONICMEDIA, attrs.layerProtocolName());
    }

    @Test
    void shouldMapXponderOutput() {
        Link link = mock(Link.class);
        Link1 openroadmLink1 = mock(Link1.class);

        when(link.augmentation(Link1.class)).thenReturn(openroadmLink1);
        when(openroadmLink1.getLinkType()).thenReturn(OpenroadmLinkType.XPONDEROUTPUT);

        TapiLinkAttributes attrs = TapiLinkAttributes.fromOpenRoadmLink(link);

        assertEquals(TapiConstants.OMS_XPDR_RDM_LINK, attrs.tapiLinkType());
        assertEquals(TapiConstants.XPDR, attrs.sourceNodeQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA, attrs.destinationNodeQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.sourceTpQualifier());
        assertEquals(TapiConstants.PHTNC_MEDIA_OTS, attrs.destinationTpQualifier());
        assertEquals(LayerProtocolName.PHOTONICMEDIA, attrs.layerProtocolName());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenLinkTypeIsNull() {
        Link link = mock(Link.class);
        Link1 link1 = mock(Link1.class);

        when(link.augmentation(Link1.class)).thenReturn(link1);
        when(link1.getLinkType()).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> TapiLinkAttributes.fromOpenRoadmLink(link));
    }
}
