/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

public class ConnectToGnpyServerTest extends JerseyTest {
    static {
        //we must hardcoded port because it's hardcoded in ConnectToGnpyServer
        System.setProperty("jersey.config.test.container.port", "8008");
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(GnpyStub.class);
    }

    @Test
    public void isGnpyURLExistTest() throws GnpyException, IOException {
        ConnectToGnpyServer connectToGnpy = new ConnectToGnpyServer();
        assertTrue(connectToGnpy.isGnpyURLExist());
    }

    @Test
    public void returnGnpyResponseTest() throws GnpyException, IOException {
        ConnectToGnpyServer connectToGnpy = new ConnectToGnpyServer();
        String result = connectToGnpy.returnGnpyResponse(Files.readString(
                Paths.get("src", "test", "resources", "gnpy", "gnpy_request.json"), StandardCharsets.US_ASCII));
        assertNotNull("result should not be null", result);
        assertTrue("Result should not be empty", !result.isEmpty());
    }

}
