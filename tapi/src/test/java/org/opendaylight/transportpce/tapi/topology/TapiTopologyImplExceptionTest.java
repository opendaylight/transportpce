/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyDetailsImpl;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsOutput;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;

@ExtendWith(MockitoExtension.class)
public class TapiTopologyImplExceptionTest {
    @Mock
    private RpcService rpcService;
    @Mock
    private NetworkTransactionService networkTransactionService;
    @Mock
    private TapiContext tapiContext;
    @Mock
    private TopologyUtils topologyUtils;
    @Mock
    private TapiLink tapiLink;

    @Test
    void getTopologyDetailsWithExceptionTest() throws InterruptedException, ExecutionException {
        when(networkTransactionService.read(any(), any()))
            .thenReturn(FluentFuture.from(Futures.immediateFailedFuture(new InterruptedException())));
        when(rpcService.getRpc(GetTopologyDetails.class))
            .thenReturn(new GetTopologyDetailsImpl(tapiContext, topologyUtils, tapiLink, networkTransactionService));
        Uuid topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(topologyUuid);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = rpcService
                .getRpc(GetTopologyDetails.class).invoke(input);
        assertFalse(result.get().isSuccessful(), "RpcResult is not successful");
        assertNull(result.get().getResult(), "RpcResult result should be null");
        assertEquals(ErrorType.RPC, result.get().getErrors().get(0).getErrorType());
        assertEquals("Error building topology", result.get().getErrors().get(0).getMessage());
    }
}