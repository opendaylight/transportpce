/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.controllers.tpce.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TpceBanner {

    private static final String[] BANNER = {
            " ___________     ___________________ ___________",
            " \\__    ___/     \\______   \\_   ___ \\\\_   _____/",
            "   |    |  ______ |     ___/    \\  \\/ |    __)_",
            "   |    | /_____/ |    |   \\     \\____|        \\",
            "   |____|         |____|    \\______  /_______  /",
            "                                   \\/        \\/",
            ".__  .__       .__     __              .__          ",
            "|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    ",
            "|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\",
            "|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )",
            "|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/",
            "/_____/     \\/      \\/      \\/           ",
            "Starting lighty.io TransportPCE application ...", "https://lighty.io/",
            "https://github.com/PantheonTechnologies/lighty-core" };

    private static final Logger LOG = LoggerFactory.getLogger(TpceBanner.class);

    private TpceBanner() {

    }

    public static void print() {
        for (String line : BANNER) {
            LOG.info(line);
        }
        LOG.info(":: Version :: {}", getVersion());
    }

    private static String getVersion() {
        Package tpcePackage = TpceBanner.class.getPackage();
        if (tpcePackage != null && tpcePackage.getImplementationVersion() != null) {
            return tpcePackage.getImplementationVersion();
        }
        return "not defined";
    }

}
