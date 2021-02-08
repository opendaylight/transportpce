/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyStub;


public class JerseyServer extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(GnpyStub.class);
    }
}
