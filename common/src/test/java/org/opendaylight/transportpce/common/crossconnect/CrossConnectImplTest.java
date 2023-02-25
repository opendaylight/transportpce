/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yangtools.yang.common.Decimal64;

public class CrossConnectImplTest {

    private CrossConnectImpl crossConnectImpl = null;
    private static DeviceTransactionManager deviceTransactionManager = mock(DeviceTransactionManager.class);
    private CrossConnectImpl121 crossConnectImpl121 = mock(CrossConnectImpl121.class);
    private CrossConnectImpl221 crossConnectImpl221 = mock(CrossConnectImpl221.class);
    private CrossConnectImpl710 crossConnectImpl710 = mock(CrossConnectImpl710.class);
    private MappingUtils mappingUtils = mock(MappingUtils.class);
    private SpectrumInformation spectrumInfo = mock(SpectrumInformation.class);

    @BeforeEach
    void setUp() {
        crossConnectImpl = new CrossConnectImpl(deviceTransactionManager, mappingUtils, crossConnectImpl121,
            crossConnectImpl221, crossConnectImpl710);
    }


    @Test
    void getCrossConnect() {
        when(mappingUtils.getOpenRoadmVersion("NodeId")).thenReturn(OPENROADM_DEVICE_VERSION_2_2_1);
        when(crossConnectImpl221.getCrossConnect(any(), any())).thenReturn(Optional.empty());
        Optional<?> res = crossConnectImpl.getCrossConnect("NodeId", "122");
        assertFalse(res.isPresent(), "Optional object should be empty");

        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(OPENROADM_DEVICE_VERSION_2_2_1);
        when(crossConnectImpl221.getCrossConnect(any(), any())).thenReturn(Optional.of(mock(RoadmConnections.class)));
        res = crossConnectImpl.getCrossConnect("NodeId", "122");
        assertTrue(res.isPresent(), "Optional object should have a value");
    }

    @Test
    void postCrossConnect() {
        when(mappingUtils.getOpenRoadmVersion(anyString())).thenReturn("bad node version");
        Optional<?> res = crossConnectImpl.postCrossConnect("nodeId", "srcTp", "destTp", spectrumInfo);
        assertFalse(res.isPresent(), "Optional object should be empty");

        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(OPENROADM_DEVICE_VERSION_2_2_1);
        when(crossConnectImpl221.postCrossConnect(any(), any(), any(), any()))
                .thenReturn(Optional.of("Connection Number"));
        res = crossConnectImpl.postCrossConnect("100", "srcTp", "destTp", spectrumInfo);
        assertTrue(res.isPresent(), "Optional object should have a value");
    }

    @Test
    void deleteCrossConnect() {
        when(mappingUtils.getOpenRoadmVersion(anyString())).thenReturn("bad node version");
        List<String> res = crossConnectImpl.deleteCrossConnect("nodeId", "100", false);
        assertNull(res);

        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(OPENROADM_DEVICE_VERSION_1_2_1);
        when(crossConnectImpl121.deleteCrossConnect(any(), any()))
                .thenReturn(List.of("interface1", "interface2"));
        res = crossConnectImpl.deleteCrossConnect("nodeId", "100", false);
        assertEquals(res.size(), 2);
    }

    @Test
    void setPowerLevel() {
        when(mappingUtils.getOpenRoadmVersion(anyString())).thenReturn(OPENROADM_DEVICE_VERSION_1_2_1);
        boolean res = crossConnectImpl.setPowerLevel("nodeId", "bad mode", Decimal64.valueOf("1"), "connection number");
        assertFalse(res, "Power Level sgould be false");

        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(OPENROADM_DEVICE_VERSION_1_2_1);
        when(crossConnectImpl121.setPowerLevel(any(), any(), any(), any()))
                .thenReturn(true);
        crossConnectImpl.setPowerLevel("nodeId", "power", Decimal64.valueOf("1"), "connection number");
        assertTrue(true);
    }
}
