/*
 * Copyright © 2025 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

class CatalogDataStoreOperationsImplTest extends AbstractTest {

    @Mock
    private NetworkTransactionService networkTransactionService;
    private CatalogDataStoreOperations catalogDataStoreOperations;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Answer<FluentFuture<CommitInfo>> answer = new Answer<FluentFuture<CommitInfo>>() {

            @Override
            public FluentFuture<CommitInfo> answer(InvocationOnMock invocation) throws Throwable {
                return CommitInfo.emptyFluentFuture();
            }

        };
        when(networkTransactionService.commit()).then(answer);

        catalogDataStoreOperations = new CatalogDataStoreOperationsImpl(
                networkTransactionService, getDataStoreContextUtil().getBindingDOMCodecServices());
    }

    @Test
    public void testAddOpenroadmOperationalModesToCatalog() throws Exception {
        OpenroadmOperationalModes operationalModes = mock(OpenroadmOperationalModes.class);

        catalogDataStoreOperations.addOpenroadmOperationalModesToCatalog(operationalModes);

        verify(networkTransactionService)
            .merge(eq(LogicalDatastoreType.CONFIGURATION), any(DataObjectIdentifier.class), eq(operationalModes));
    }

    @Test
    public void testAddSpecificOperationalModesToCatalog() throws Exception {
        SpecificOperationalModes specificOperationalModes = mock(SpecificOperationalModes.class);

        catalogDataStoreOperations.addSpecificOperationalModesToCatalog(specificOperationalModes);

        verify(networkTransactionService).merge(
                eq(LogicalDatastoreType.CONFIGURATION), any(DataObjectIdentifier.class), eq(specificOperationalModes));
    }

}
