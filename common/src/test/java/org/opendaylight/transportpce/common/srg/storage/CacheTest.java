/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.shared.risk.group.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.shared.risk.group.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.shared.risk.group.SharedRiskGroupKey;
import org.opendaylight.yangtools.yang.common.Uint16;

class CacheTest {

    @Test
    void readFromStorage() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = mock(Storage.class);
        when(fakeStore.read(new NodesKey("ROADM-A-SRG1"))).thenReturn(sharedRiskGroupMap);

        Storage cache = new Cache(fakeStore);

        assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        verify(fakeStore, times(1)).read(new NodesKey("ROADM-A-SRG1"));
    }

    @Test
    void multipleReadRequestsOnlyReadOnceFromDatastore() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = mock(Storage.class);
        when(fakeStore.read(new NodesKey("ROADM-A-SRG1"))).thenReturn(sharedRiskGroupMap);

        Storage cache = new Cache(fakeStore);

        assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        verify(fakeStore, times(1)).read(new NodesKey("ROADM-A-SRG1"));
    }

    @Test
    void assertReadDontReturnNull() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = mock(Storage.class);

        Storage cache = new Cache(fakeStore);

        assertNotNull(cache.read("ROADM-B-SRG1"));
    }
}
