/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.metadata;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev250626.OpenTerminalMetaData;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read metadata from md-sal for OpenConfig node.
 *
 * <p>Activation Strategy: immediate=true, DataBroker=OPTIONAL (DYNAMIC policy).
 * This ensures the service is registered even if DataBroker is unavailable initially.
 */
@Component(service = OCMetaDataTransaction.class, immediate = true)
public class OCMetaDataTransactionImpl implements OCMetaDataTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(OCMetaDataTransactionImpl.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC)
    private volatile DataBroker dataBroker;

    @org.osgi.service.component.annotations.Activate
    public void activate() {
        LOG.info("OCMetaDataTransactionImpl ACTIVATED - service registered in OSGi registry. "
            + "DataBroker available: {}", dataBroker != null);
    }

    @org.osgi.service.component.annotations.Modified
    public void modified() {
        LOG.info("OCMetaDataTransactionImpl MODIFIED - DataBroker available: {}", dataBroker != null);
    }

    public OCMetaDataTransactionImpl() {
    }

    /**
     * This method is used to get OpenTerminalMetaData.
     * from MD-Sal
     * @return OpenTerminalMetaData from md sal.
     */
    @Override
    public OpenTerminalMetaData getXPDROpenTerminalMetaData() {
        if (dataBroker == null) {
            LOG.error("DataBroker not yet available in OCMetaDataTransactionImpl - "
                + "metadata operations will fail. This may happen during early startup.");
            return null;
        }
        OpenTerminalMetaData terminalMetaData = null;
        DataObjectIdentifier<OpenTerminalMetaData> iidOTMD = DataObjectIdentifier.builder(OpenTerminalMetaData.class)
                .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<OpenTerminalMetaData> openTerminalMetaData =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, iidOTMD).get();
            if (openTerminalMetaData.isPresent()) {
                terminalMetaData = openTerminalMetaData.orElseThrow();
                LOG.debug("Found OpenTerminalMetaData {} in Md-Sal.", terminalMetaData);
            } else {
                LOG.warn("No OpenTerminalMetaData found in configuration datastore - metadata may not be provisioned");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to get open-terminal-meta-data from Md-Sal", e);
        }
        return  terminalMetaData;
    }
}
