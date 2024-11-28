/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.metadata;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.open.terminal.meta.data.rev240124.OpenTerminalMetaData;

/**
 * This class related to read metadata from md sal for openconfig node.
 */
public interface OCMetaDataTransaction {

    /**
     *  This Method is used to get OpenTerminalMetadata.
     *  from MD-Sal
     * @return OpenTerminalMetaData
     */
    OpenTerminalMetaData getXPDROpenTerminalMetaData();
}
