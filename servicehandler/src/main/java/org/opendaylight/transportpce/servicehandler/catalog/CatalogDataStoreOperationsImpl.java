/*
 * Copyright © 2023 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OperationalModeCatalog;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CatalogDataStoreOperationsImpl implements CatalogDataStoreOperations {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogDataStoreOperationsImpl.class);

    private NetworkTransactionService networkTransactionService;

    @Activate
    public CatalogDataStoreOperationsImpl(@Reference NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * Store OpenroadmOperationalModes object in the config data store.
     */
    @Override
    public void addOpenroadmOperationalModesToCatalog(OpenroadmOperationalModes objToSave) {
        LOG.info("Inside addOpenroadmOperationalModesToCatalog method of CatalogDataStoreOperationsImpl");
        try {
            DataObjectIdentifier<OpenroadmOperationalModes> instanceIdentifier = DataObjectIdentifier
                    .builder(OperationalModeCatalog.class)
                    .child(OpenroadmOperationalModes.class)
                    .build();
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, instanceIdentifier , objToSave);
            networkTransactionService.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("Warning addOpenroadmOperationalModesToCatalog CatalogDataStoreOperationsImpl");
        }
    }

    /**
     * Store SpecificOperationalModes object in the config data store.
     */
    @Override
    public void addSpecificOperationalModesToCatalog(SpecificOperationalModes objToSave) {
        LOG.info("Inside addSpecificOperationalModesToCatalog method of CatalogDataStoreOperationsImpl");
        try {
            DataObjectIdentifier<SpecificOperationalModes> instanceIdentifier = DataObjectIdentifier
                    .builder(OperationalModeCatalog.class).child(SpecificOperationalModes.class)
                    .build();
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, instanceIdentifier , objToSave);
            networkTransactionService.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("Warning addSpecificOperationalModesToCatalog CatalogDataStoreOperationsImpl");
        }
    }
}
