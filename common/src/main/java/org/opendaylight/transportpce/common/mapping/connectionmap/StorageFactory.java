/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.mapping.adapter.NodeConnectionMapRev181019;
import org.opendaylight.transportpce.common.mapping.adapter.NodeConnectionMapRev231026;
import org.slf4j.Logger;

public class StorageFactory implements Factory {

    @Override
    public Storage storage(DataBroker dataBroker, Logger log) {
        return new ConnectionMapStorage(
                dataBroker,
                new NodeConnectionMapRev181019(),
                new NodeConnectionMapRev231026(),
                log
        );
    }

}
