/*
 * Copyright © 2022 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OperationalModeCatalog;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogDataStoreOperationsImpl implements CatalogDataStoreOperations {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogDataStoreOperationsImpl.class);

    private DataBroker dataBroker;

    public CatalogDataStoreOperationsImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Store OpenroadmOperationalModes object in the config data store.
     */
    @Override
    public void addOpenroadmOperationalModesToCatalog(OpenroadmOperationalModes objToSave) {
        LOG.info("Inside addOpenroadmOperationalModesToCatalog method of CatalogDataStoreOperationsImpl");
        try {
            InstanceIdentifier<OpenroadmOperationalModes> instanceIdentifier =
                InstanceIdentifier.create(OperationalModeCatalog.class).child(OpenroadmOperationalModes.class);
            WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, instanceIdentifier , objToSave);
            writeTransaction.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
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
            InstanceIdentifier<SpecificOperationalModes> instanceIdentifier = InstanceIdentifier
                    .create(OperationalModeCatalog.class).child(SpecificOperationalModes.class);
            WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, instanceIdentifier , objToSave);
            writeTransaction.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("Warning addSpecificOperationalModesToCatalog CatalogDataStoreOperationsImpl");
        }
    }
}
