/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.ConnectionMapStorage;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.StorageRev181019;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.StorageRev200529;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.TranslateRev181019;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.TranslateRev200529;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.TransportPCE;

/**
 * Default {@link Factory} implementation, wiring together the revision-specific translators
 * and storage handlers used to save and read connection maps.
 */
public class StorageFactory implements Factory {

    @Override
    public Storage storage(DataBroker dataBroker) {
        return new ConnectionMapStorage(
                dataBroker,
                new TransportPCE(
                    new TranslateRev181019(),
                    new TranslateRev200529()
                ),
            new StorageRev181019(),
            new StorageRev200529()
        );
    }
}
