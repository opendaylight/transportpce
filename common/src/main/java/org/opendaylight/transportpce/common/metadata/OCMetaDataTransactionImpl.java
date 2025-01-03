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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.OpenTerminalMetaData;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class related to read metadata from md sal for openconfig node.
 */
@Component

public class OCMetaDataTransactionImpl implements OCMetaDataTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(OCMetaDataTransactionImpl.class);

    private final DataBroker dataBroker;

    @Activate
    public OCMetaDataTransactionImpl(@Reference DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * This method is used to get OpenTerminalMetaData.
     * from MD-Sal
     * @return OpenTerminalMetaData from md sal.
     */
    @Override
    public OpenTerminalMetaData getXPDROpenTerminalMetaData() {
        OpenTerminalMetaData terminalMetaData = null;
        DataObjectIdentifier<OpenTerminalMetaData> iidOTMD = DataObjectIdentifier.builder(OpenTerminalMetaData.class)
                .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<OpenTerminalMetaData> openTerminalMetaData =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, iidOTMD).get();
            if (openTerminalMetaData.isPresent()) {
                terminalMetaData = openTerminalMetaData.orElseThrow();
                LOG.info("Found OpenTerminalMetaData {} in Md-Sal.", terminalMetaData);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to get TerminalMetaData {} in Md-Sal", iidOTMD, e);
        }
        return  terminalMetaData;
    }
}
