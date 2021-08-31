/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.controllers.tpce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaintTest {
    private static final Logger LOG = LoggerFactory.getLogger(MaintTest.class);
    private Main main = new Main();
    private HttpClient client = new HttpClient();

    @Before
    @SuppressWarnings("checkstyle:Illegalcatch")
    public void init() {
        try {
            client.start();
        } catch (Exception e) {
            LOG.error("An error occured during test init", e);
            fail("cannot init test ");
        }
    }

    @After
    @SuppressWarnings("checkstyle:Illegalcatch")
    public void stop() {
        try {
            main.shutdown();
            client.stop();
        } catch (Exception e) {
            LOG.error("An error occured during test shutdown", e);
        }
    }

    @Test
    public void startNoConfigFileTest() throws Exception {
        main.start(null, false, "3000", "2000", true);
        ContentResponse response = client.GET("http://localhost:8181/restconf/config/ietf-network:networks/network/openroadm-topology");
        assertEquals("Response code should be 200", 200, response.getStatus());
    }

    @Test
    public void startConfigFileTest() throws Exception {
        File configFile = new File("src/test/resources/config.json");
        main.start(configFile.getAbsolutePath(), false, "3000", "2000", true);
        ContentResponse response = client.GET("http://localhost:8888/restconfCustom/config/ietf-network:networks/network/openroadm-topology");
        assertEquals("Response code should be 200", 200, response.getStatus());
    }
}
