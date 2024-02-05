/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.olm.rpc.impl.CalculateSpanlossBaseImpl;
import org.opendaylight.transportpce.olm.rpc.impl.CalculateSpanlossCurrentImpl;
import org.opendaylight.transportpce.olm.rpc.impl.GetPmImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerResetImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerSetupImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerTurndownImpl;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;

@ExtendWith(MockitoExtension.class)
class OlmPowerServiceRpcImplTest extends AbstractTest {

    @Mock
    private OlmPowerService olmPowerService;
    @Mock
    private RpcProviderService rpcProviderService;

    @Test
    void testGetPmFailWithNodeIdNull() throws InterruptedException, ExecutionException {
        GetPmInput input = new GetPmInputBuilder()
            .setGranularity(PmGranularity._15min)
            .setResourceIdentifier(new ResourceIdentifierBuilder()
                    .setResourceName("ots-deg1").build())
            .setResourceType(ResourceTypeEnum.Interface)
            .build();
        ListenableFuture<RpcResult<GetPmOutput>> output = new GetPmImpl(olmPowerService).invoke(input);
        assertFalse(output.get().isSuccessful());
        assertNull(output.get().getResult());
        assertEquals(ErrorType.RPC, output.get().getErrors().get(0).getErrorType());
        assertEquals("Error with input parameters", output.get().getErrors().get(0).getMessage());
        assertEquals(ErrorSeverity.ERROR, output.get().getErrors().get(0).getSeverity());
        assertEquals(ErrorTag.OPERATION_FAILED, output.get().getErrors().get(0).getTag());
    }

    @Test
    void testGetPmWithSuccess() throws InterruptedException, ExecutionException {
        GetPmInput input = new GetPmInputBuilder()
            .setNodeId("nodeId")
            .build();
        when(this.olmPowerService.getPm(any())).thenReturn(new GetPmOutputBuilder().build());
        ListenableFuture<RpcResult<GetPmOutput>> output = new GetPmImpl(olmPowerService).invoke(input);
        assertTrue(output.get().isSuccessful());
        assertEquals(new GetPmOutputBuilder().build(), output.get().getResult());
    }

    @Test
    void testServicePowerSetup() throws InterruptedException, ExecutionException {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        when(this.olmPowerService.servicePowerSetup(any())).thenReturn(new ServicePowerSetupOutputBuilder().build());
        ListenableFuture<RpcResult<ServicePowerSetupOutput>> output =
            new ServicePowerSetupImpl(olmPowerService).invoke(input);
        assertTrue(output.get().isSuccessful());
        assertEquals(new ServicePowerSetupOutputBuilder().build(), output.get().getResult());
    }

    @Test
    void testServicePowerTurndown() throws InterruptedException, ExecutionException {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        when(this.olmPowerService.servicePowerTurndown(any()))
            .thenReturn(new ServicePowerTurndownOutputBuilder().build());
        var output = new ServicePowerTurndownImpl(olmPowerService).invoke(input);
        assertTrue(output.get().isSuccessful());
        assertEquals(new ServicePowerTurndownOutputBuilder().build(), output.get().getResult());
    }

    @Test
    void testCalculateSpanlossBase() throws InterruptedException, ExecutionException {
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInputAll();
        when(this.olmPowerService.calculateSpanlossBase(any()))
            .thenReturn(new CalculateSpanlossBaseOutputBuilder().build());
        var output = new CalculateSpanlossBaseImpl(olmPowerService).invoke(input);
        assertTrue(output.get().isSuccessful());
        assertEquals(new CalculateSpanlossBaseOutputBuilder().build(), output.get().getResult());
    }

    @Test
    void testCalculateSpanlossCurrent() throws InterruptedException, ExecutionException {
        CalculateSpanlossCurrentInput input = new CalculateSpanlossCurrentInputBuilder().build();
        when(this.olmPowerService.calculateSpanlossCurrent(any()))
            .thenReturn(new CalculateSpanlossCurrentOutputBuilder().build());
        var output = new CalculateSpanlossCurrentImpl(olmPowerService).invoke(input);
        assertTrue(output.get().isSuccessful());
        assertEquals(new CalculateSpanlossCurrentOutputBuilder().build(), output.get().getResult());
    }

    @Test
    void testServicePowerReset() throws InterruptedException, ExecutionException {
        ServicePowerResetInput input = OlmPowerServiceRpcImplUtil.getServicePowerResetInput();
        when(this.olmPowerService.servicePowerReset(any()))
            .thenReturn(new ServicePowerResetOutputBuilder().build());
        var output = new ServicePowerResetImpl(olmPowerService).invoke(input);
        assertTrue(output.get().isSuccessful());
        assertEquals(new ServicePowerResetOutputBuilder().build(), output.get().getResult());
    }
}
