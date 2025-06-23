/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.olm.util.rev200529.OlmUtilsTestObjects;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.alarm.pm.types.rev191129.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.alarm.pm.types.rev191129.Location;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev200529.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev200529.CurrentPmListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev200529.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev200529.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev200529.current.pm.list.CurrentPmEntryKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev200529.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev200529.current.pm.val.group.MeasurementKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev200327.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev200327.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev200327.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev200327.Validity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev191129.ResourceTypeEnum;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;

public class OlmUtils710Test {
    @Test
    void testPmFetchAll710() {
        //setup

        PmGranularity pmGran =  PmGranularity._15min;

        Measurement measurement = OlmUtilsTestObjects.newMeasurement(pmGran,
                new PmDataType(Decimal64.valueOf("12.65")),
                "dBm",
                Validity.Complete);
        Measurement measurement2 = OlmUtilsTestObjects.newMeasurement(pmGran,
                new PmDataType(Decimal64.valueOf("6.65")),
                "dBm",
                Validity.Complete);
        Measurement measurement3 = OlmUtilsTestObjects.newMeasurement(pmGran,
                new PmDataType(Decimal64.valueOf("7.54")),
                "dBm",
                Validity.Complete);

        MeasurementKey measurementKey = new MeasurementKey(pmGran);

        Map<MeasurementKey, Measurement> measurementMap = new HashMap<>();
        measurementMap.put(measurementKey, measurement);
        Map<MeasurementKey, Measurement> measurementMap2 = new HashMap<>();
        measurementMap2.put(measurementKey, measurement2);
        Map<MeasurementKey, Measurement> measurementMap3 = new HashMap<>();
        measurementMap3.put(measurementKey, measurement3);

        CurrentPm cpm1 = OlmUtilsTestObjects.newCurrentPm(PmNamesEnum.OpticalPowerOutput, measurementMap,
                "test-extension1", Direction.Tx, Location.NearEnd);
        CurrentPm cpm2 = OlmUtilsTestObjects.newCurrentPm(PmNamesEnum.OpticalPowerInput, measurementMap2,
                "test-extension2", Direction.Tx, Location.NearEnd);
        CurrentPm cpm3 = OlmUtilsTestObjects.newCurrentPm(PmNamesEnum.TotalOpticalPowerInput, measurementMap3,
                "test-extension3", Direction.Tx, Location.NearEnd);

        DataObjectIdentifier<Interface> interfaceOId = OlmUtilsTestObjects.newDataObjectIdentifierInterface(
                "test-interface-name");

        CurrentPmEntry cpe = OlmUtilsTestObjects.newCurrentPmEntry(interfaceOId, List.of(cpm1, cpm2, cpm3),
                ResourceTypeEnum.Interface, "test-extension", "2021-03-15T13:45:32Z");

        Map<CurrentPmEntryKey, CurrentPmEntry> pmMap = new HashMap<>();
        pmMap.put(cpe.key(), cpe);
        CurrentPmList pmList = new CurrentPmListBuilder().setCurrentPmEntry(pmMap).build();

        DeviceTransactionManager testMgr = Mockito.mock(DeviceTransactionManager.class);

        org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.PmGranularity
                granularity = org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325
                .PmGranularity._15min;

        GetPmInput input = org.opendaylight.transportpce.olm.util.rev210618.OlmUtilsTestObjects
                .newGetPmInput210618("ROADM-TEST",
                        org.opendaylight.yang.gen.v1.http
                                .org.openroadm.resource.types.rev161014.ResourceTypeEnum.Interface,
                        granularity);

        //Preconditions
        when(testMgr.getDataFromDevice(eq("ROADM-TEST"), any(), any(), anyLong(), any()))
                .thenReturn(Optional.of(pmList));
        //test
        Map<String, List<GetPmOutput>> result = OlmUtils710.pmFetchAll(input, testMgr);

        //Assert we got three getGmOutput objects in the result.
        assertEquals(3, result.get("test-interface-name").size());
    }
}

