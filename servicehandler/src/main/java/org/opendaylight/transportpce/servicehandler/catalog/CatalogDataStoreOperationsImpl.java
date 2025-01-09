/*
 * Copyright © 2023 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OperationalModeCatalog;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CatalogDataStoreOperationsImpl implements CatalogDataStoreOperations {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogDataStoreOperationsImpl.class);
    private static final String CATALOG_FILE = "apidoc-operational-modes-to-catalog-13_1-optical-spec-6_0.json";

    private BindingDOMCodecServices bindingDOMCodecServices;
    private NetworkTransactionService networkTransactionService;

    @Activate
    public CatalogDataStoreOperationsImpl(@Reference NetworkTransactionService networkTransactionService,
            @Reference BindingDOMCodecServices bindingDOMCodecServices) {
        this.networkTransactionService = networkTransactionService;
        this.bindingDOMCodecServices = bindingDOMCodecServices;
        loadORCatalog();
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

    private void loadORCatalog() {
        LOG.info("Loading openroadm operational mode catalog {}", CATALOG_FILE);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CATALOG_FILE)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found.");
            }
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonStringConverter<OperationalModeCatalog> convert = new JsonStringConverter<>(bindingDOMCodecServices);
            OperationalModeCatalog catalog = convert.createDataObjectFromJsonString(
                    YangInstanceIdentifier.of(OperationalModeCatalog.QNAME),
                    json,
                    JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
            DataObjectIdentifier<OperationalModeCatalog> ii = DataObjectIdentifier
                    .builder(OperationalModeCatalog.class)
                    .build();
            networkTransactionService.put(
                    LogicalDatastoreType.CONFIGURATION,
                    ii,
                    catalog);
            try {
                networkTransactionService.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error stroging openroadm operational mode catalog {} in the datastore", CATALOG_FILE, e);
            }
            LOG.info("datastore initialized with the OR catalog");
        } catch (IOException e) {
            LOG.error("Error reading openroadm operational mode catalog {}", CATALOG_FILE, e);
        }
    }
}
