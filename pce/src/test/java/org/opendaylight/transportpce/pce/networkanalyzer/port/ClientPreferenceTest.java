/*
 * Copyright © 2020 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClientPreferenceTest {

    @Test
    void preferredPort_returnTrue() {

        Map<String, Set<String>> nodePortPreference = new HashMap<>();
        nodePortPreference.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));

        Preference clientPreference = new ClientPreference(nodePortPreference);

        Assertions.assertTrue(clientPreference.isPreferredPort("ROADM-B-SRG1", "SRG1-PP1-TXRX"));
    }

    /**
     * The client prefer to use SRG1-PP1-TXRX on ROADM-B-SRG1.
     * Therefore, preferredPort returns false on SRG1-PP2-TXRX.
     */
    @Test
    void nonPreferredPort_returnFalse() {

        Map<String, Set<String>> nodePortPreference = new HashMap<>();
        nodePortPreference.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));

        Preference clientPreference = new ClientPreference(nodePortPreference);

        Assertions.assertFalse(clientPreference.isPreferredPort("ROADM-B-SRG1", "SRG1-PP2-TXRX"));
    }

    /**
     * In this scenario ROADM-A-SRG1 is missing from the client preferred list.
     * We treat this as the client has no opinion on what port
     * to use on ROADM-A-SRG1. Meaning, as far as the client goes, all
     * ports on ROADM-A-SRG1 are fine.
     */
    @Test
    void nodeMissingInPreferredList_returnTrue() {

        Map<String, Set<String>> nodePortPreference = new HashMap<>();
        nodePortPreference.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));

        Preference clientPreference = new ClientPreference(nodePortPreference);

        Assertions.assertTrue(clientPreference.isPreferredPort("ROADM-A-SRG1", "SRG1-PP2-TXRX"));

    }
}