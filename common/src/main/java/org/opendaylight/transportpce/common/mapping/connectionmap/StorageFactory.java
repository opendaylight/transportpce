/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.ConnectionMapStorage;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.StorageRev170206;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.StorageRev181019;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.StorageRev200529;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.TranslateRev170206;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.TranslateRev181019;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.TranslateRev200529;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.TransportPCE;

public class StorageFactory implements Factory {

    @Override
    public Storage storage(DataBroker dataBroker) {
        return new ConnectionMapStorage(
                dataBroker,
                new TransportPCE(
                    new TranslateRev170206(),
                    new TranslateRev181019(),
                    new TranslateRev200529()
                ),
            new StorageRev170206(),
            new StorageRev181019(),
            new StorageRev200529()
        );
    }
}
