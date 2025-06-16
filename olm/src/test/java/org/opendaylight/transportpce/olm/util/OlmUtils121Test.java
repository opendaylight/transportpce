/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.opendaylight.transportpce.olm.util.rev161014.OlmUtilsTestObjects;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlist;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlistBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Measurements;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.measurements.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPmKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.ResourceTypeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;


public class OlmUtils121Test {

    /*
     * Test to try and fetch a builder with nodes containing null value as pmValue.
     * This should result in a Runtime exception with a descriptive error message
     */
    @Test
    void testGetPmOutputBuilderFail() {

        //setup

        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Interface
            rrInterfaceResource = OlmUtilsTestObjects.newRrrInterface("TestInterface");

        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.Resource
                rrResource = OlmUtilsTestObjects.newRResource(rrInterfaceResource);

        org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Resource
                resource = OlmUtilsTestObjects.newPmResource(rrResource, new ResourceTypeBuilder()
                .setType(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.resource.types.rev161014.ResourceTypeEnum.Interface)
                .build());

        Measurement defectSeconds = OlmUtilsTestObjects.newMeasurement(PmNamesEnum.DefectSeconds,
                null,
               null,
                null);
        List<Measurements> measurementsList = List.of(OlmUtilsTestObjects.newMeasurements(defectSeconds));

        CurrentPm currentPm = OlmUtilsTestObjects.newCurrentPm("ROADM-TEST",
                org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity._15min,
                resource,
                measurementsList);

        CurrentPmlist pmList = new CurrentPmlistBuilder()
                .setCurrentPm(Map.of(currentPm.key(), currentPm)).build();

        GetPmInput input = org.opendaylight.transportpce.olm.util.rev210618.OlmUtilsTestObjects.newGetPmInput210618(
                "ROADM-TEST",
                org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum.Interface,
                org.opendaylight.yang.gen.v1.http.org.transportpce.common.types
                        .rev250325.PmGranularity._15min,
                new ResourceIdentifierBuilder().setResourceName("TestInterface").build());

        DeviceTransactionManager testMgr = Mockito.mock(DeviceTransactionManager.class);

        //Preconditions
        when(testMgr.getDataFromDevice(eq("ROADM-TEST"), any(), any(), anyLong(), any()))
                .thenReturn(Optional.of(pmList));

        //test
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            OlmUtils121.pmFetch(input, testMgr);
        });

        //asserts
        assertEquals("No ParameterValue found for node ROADM-TEST on parameter: DefectSeconds",
                exception.getMessage());
    }

    @Test
    void testPmFetchAll121() {
        //setup

        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Interface
                rrInterfaceResource = OlmUtilsTestObjects.newRrrInterface("test-interface-name");

        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.Resource
                rrResource = OlmUtilsTestObjects.newRResource(rrInterfaceResource);

        org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Resource
                resource = OlmUtilsTestObjects.newPmResource(rrResource, new ResourceTypeBuilder()
                .setType(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.resource.types.rev161014.ResourceTypeEnum.Interface)
                .build());

        Measurement measurement1 = OlmUtilsTestObjects.newMeasurement(PmNamesEnum.OpticalPowerInput,
                "test-extension",
                new PmDataType(Decimal64.valueOf("12.65")),
                "dBm");
        Measurement measurement2 = OlmUtilsTestObjects.newMeasurement(PmNamesEnum.OpticalPowerOutput,
                "test-extension2",
                new PmDataType(Decimal64.valueOf("7.48")),
                "dBm");
        Measurement measurement3 = OlmUtilsTestObjects.newMeasurement(PmNamesEnum.OpticalPower,
                "test-extension3",
                new PmDataType(Decimal64.valueOf("42.76")),
                "dBm");

        CurrentPm currentPm1 = OlmUtilsTestObjects.newCurrentPm("12",
                org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity._15min,
                resource,
                List.of(OlmUtilsTestObjects.newMeasurements(measurement1)));
        CurrentPm currentPm2 = OlmUtilsTestObjects.newCurrentPm("13",
                org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity._15min,
                resource,
                List.of(OlmUtilsTestObjects.newMeasurements(measurement2)));
        CurrentPm currentPm3 = OlmUtilsTestObjects.newCurrentPm("14",
                org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity._15min,
                resource,
                List.of(OlmUtilsTestObjects.newMeasurements(measurement3)));

        Map<CurrentPmKey, CurrentPm> currentPmMap = new HashMap<>();

        currentPmMap.put(currentPm1.key(), currentPm1);
        currentPmMap.put(currentPm2.key(), currentPm2);
        currentPmMap.put(currentPm3.key(), currentPm3);

        CurrentPmlist pmList = new CurrentPmlistBuilder()
                .setCurrentPm(currentPmMap).build();

        GetPmInput input = org.opendaylight.transportpce.olm.util.rev210618.OlmUtilsTestObjects.newGetPmInput210618(
                "ROADM-TEST",
                org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum.Interface,
                org.opendaylight.yang.gen.v1.http.org.transportpce.common.types
                        .rev250325.PmGranularity._15min);

        DeviceTransactionManager testMgr = Mockito.mock(DeviceTransactionManager.class);

        //Preconditions
        when(testMgr.getDataFromDevice(eq("ROADM-TEST"), any(), any(), anyLong(), any()))
                .thenReturn(Optional.of(pmList));

        //test
        Map<String, List<GetPmOutput>> result = OlmUtils121.pmFetchAll(input, testMgr);

        //Assert we got three getGmOutput objects in the result.
        assertEquals(3, result.get("test-interface-name").size());
    }
}

