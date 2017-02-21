/*
 * Copyright © 2016 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RendererProvider.class);

    private final DataBroker dataBroker;

    private final MountPointService mountPointService;

    public RendererProvider(final DataBroker dataBroker, final MountPointService mountPointService) {
        this.dataBroker = dataBroker;
        this.mountPointService = mountPointService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("RendererProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("RendererProvider Closed");
    }
}
