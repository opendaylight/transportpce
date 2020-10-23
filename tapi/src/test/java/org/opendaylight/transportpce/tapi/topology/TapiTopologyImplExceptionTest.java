/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.tapi.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.Topology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TapiTopologyImplExceptionTest {

    @Test
    public void getTopologyDetailsWithExceptionTest() throws InterruptedException, ExecutionException {
        DataBroker dataBroker = Mockito.mock(DataBroker.class);
        when(dataBroker.newReadOnlyTransaction())
            .thenReturn(new ReadTransactionMock());

        GetTopologyDetailsInput input = TopologyDataUtils.buildGetTopologyDetailsInput(NetworkUtils.OTN_NETWORK_ID);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(dataBroker);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        Topology topology = rpcResult.getResult().getTopology();
        assertNull("Topology should be null", topology);
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
