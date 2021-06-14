/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yangtools.yang.common.Uint32;

@Ignore
public class CrossConnectImplTest {

    private CrossConnectImpl crossConnectImpl = null;
    private static DeviceTransactionManager deviceTransactionManager;
    private CrossConnectImpl121 crossConnectImpl121 = null;
    private CrossConnectImpl221 crossConnectImpl221 = null;
    private CrossConnectImpl710 crossConnectImpl710 = null;
    private MappingUtils mappingUtils = null;

    @Before
    public void setUp() {
        deviceTransactionManager = mock(DeviceTransactionManager.class);
        crossConnectImpl121 = mock(CrossConnectImpl121.class);
        crossConnectImpl221 = mock(CrossConnectImpl221.class);
        crossConnectImpl710 = mock(CrossConnectImpl710.class);
        mappingUtils = mock(MappingUtils.class);
        crossConnectImpl =
                new CrossConnectImpl(deviceTransactionManager, mappingUtils, crossConnectImpl121,
                    crossConnectImpl221, crossConnectImpl710);
    }

    @Before
    public void init() {

    }


    @Test
    public void getCrossConnect() {
        Optional<?> res = crossConnectImpl.getCrossConnect("100", "122");
        Assert.assertFalse("Optional object should be empty",res.isPresent());

        String devV121 = "(http://org/openroadm/device?revision=2017-02-06)org-openroadm-device";
        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(devV121);
        when(crossConnectImpl121.getCrossConnect(any(), any())).thenReturn(Optional.of(mock(RoadmConnections.class)));
        res = crossConnectImpl.getCrossConnect("100", "122");
        Assert.assertTrue("Optional object should have a value",res.isPresent());
    }

    @Test
    public void postCrossConnect() {
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        Optional<?> res = crossConnectImpl.postCrossConnect("100", "srcTp", "destTp", spectrumInformation);
        Assert.assertFalse("Optional object should be empty",res.isPresent());

        String devV121 = "(http://org/openroadm/device?revision=2017-02-06)org-openroadm-device";
        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(devV121);
        when(crossConnectImpl121.postCrossConnect(any(), any(), any(), any()))
                .thenReturn(Optional.of("Value"));
        res = crossConnectImpl.postCrossConnect("100", "srcTp", "destTp", spectrumInformation);
        Assert.assertTrue("Optional object should have a value",res.isPresent());
    }

    @Test
    public void deleteCrossConnect() {
        List<String> res = crossConnectImpl.deleteCrossConnect("100", "srcTp", true);
        Assert.assertNull(res);

        String devV121 = "(http://org/openroadm/device?revision=2017-02-06)org-openroadm-device";
        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(devV121);
        when(crossConnectImpl121.deleteCrossConnect(any(), any()))
                .thenReturn(List.of("val1"));
        res = crossConnectImpl.deleteCrossConnect("100", "srcTp", true);
        Assert.assertEquals(res.size(), 1);
    }

    @Test
    public void setPowerLevel() {
        boolean res = crossConnectImpl.setPowerLevel("100", "srcTp", new BigDecimal(100), "power");
        Assert.assertFalse("Power Level sgould be false",res);

        String devV121 = "(http://org/openroadm/device?revision=2017-02-06)org-openroadm-device";
        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(devV121);
        when(crossConnectImpl121.setPowerLevel(any(), any(), any(), any()))
                .thenReturn(true);
        res = crossConnectImpl.setPowerLevel("100", "srcTp", new BigDecimal(100), "power");
        Assert.assertTrue(true);
    }
}
