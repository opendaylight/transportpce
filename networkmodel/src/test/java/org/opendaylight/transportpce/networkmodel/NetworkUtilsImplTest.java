/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.DeleteLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitInterDomainLinks;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRdmXpdrLinks;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRoadmNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitXpdrRdmLinks;


@ExtendWith(MockitoExtension.class)
class NetworkUtilsImplTest {
    @Mock
    DataBroker dataBroker;
    @Mock
    RpcProviderService rpcProvider;

    @Test
    void networkUtilsInitTest() {
        new NetworkUtilsImpl(dataBroker, rpcProvider);

        verify(rpcProvider, times(1)).registerRpcImplementations(
                any(DeleteLink.class), any(InitRoadmNodes.class), any(InitXpdrRdmLinks.class),
                any(InitRdmXpdrLinks.class), any(InitInterDomainLinks.class));
    }
}
