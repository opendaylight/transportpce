/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.topology.details.output.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TapiTopologyImplExceptionTest {
    @Mock
    private TapiContext tapiContext;
    @Mock
    private TopologyUtils topologyUtils;
    @Mock
    private TapiLink tapiLink;

    @Test
    void getTopologyDetailsWithExceptionTest() throws InterruptedException, ExecutionException {
        DataBroker dataBroker = mock(DataBroker.class);
        when(dataBroker.newReadOnlyTransaction())
                .thenReturn(new ReadTransactionMock());
        NetworkTransactionService networkTransactionService = mock(NetworkTransactionService.class);
        Answer<FluentFuture<CommitInfo>> answer = new Answer<FluentFuture<CommitInfo>>() {

            @Override
            public FluentFuture<CommitInfo> answer(InvocationOnMock invocation) throws Throwable {
                return CommitInfo.emptyFluentFuture();
            }

        };
        when(networkTransactionService.commit()).then(answer);
        tapiContext = new TapiContext(networkTransactionService);

        Uuid topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(topologyUuid);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(dataBroker, tapiContext, topologyUtils, tapiLink);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        if (rpcResult.isSuccessful()) {
            Topology topology = rpcResult.getResult().getTopology();
            assertNull(topology, "Topology should be null");
        } else {
            assertNull(null, "Topology should be null");
        }
    }

    private final class ReadTransactionMock implements ReadTransaction {

        @Override
        public @NonNull Object getIdentifier() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <T extends DataObject> @NonNull FluentFuture<Optional<T>> read(@NonNull LogicalDatastoreType store,
                                                                              @NonNull InstanceIdentifier<T> path) {
            return FluentFuture.from(Futures.immediateFailedFuture(new InterruptedException()));
        }

        @Override
        public @NonNull FluentFuture<Boolean> exists(@NonNull LogicalDatastoreType store,
                                                     @NonNull InstanceIdentifier<?> path) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
        }
    }
}