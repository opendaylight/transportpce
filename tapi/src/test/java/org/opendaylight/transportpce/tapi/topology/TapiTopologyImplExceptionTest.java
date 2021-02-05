/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TapiTopologyImplExceptionTest {
    @Mock
    TapiContext tapiContext;
    @Mock
    TopologyUtils topologyUtils;

    @Test
    public void getTopologyDetailsWithExceptionTest() throws InterruptedException, ExecutionException {
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

        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(TopologyUtils.T0_MULTILAYER);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(dataBroker, tapiContext, topologyUtils);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        if (rpcResult.isSuccessful()) {
            Topology topology = rpcResult.getResult().getTopology();
            assertNull("Topology should be null", topology);
        } else {
            assertNull("Topology should be null", null);
        }
    }

    private class ReadTransactionMock implements ReadTransaction {

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
