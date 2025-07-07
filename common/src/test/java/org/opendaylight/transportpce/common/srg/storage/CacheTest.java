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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroupKey;
import org.opendaylight.yangtools.yang.common.Uint16;

class CacheTest {

    @Test
    void save() {

        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = mock(Storage.class);
        when(fakeStore.save("ROADM-A-SRG1", sharedRiskGroupMap)).thenReturn(true);

        Storage cache = new Cache(fakeStore);

        assertTrue(cache.save("ROADM-A-SRG1", sharedRiskGroupMap));

        verify(fakeStore, times(1)).save("ROADM-A-SRG1", sharedRiskGroupMap);
    }

    @Test
    void readFromStorage() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = mock(Storage.class);
        when(fakeStore.read(new NetworkNodesKey("ROADM-A-SRG1"))).thenReturn(sharedRiskGroupMap);

        Storage cache = new Cache(fakeStore);

        assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        verify(fakeStore, times(1)).read(new NetworkNodesKey("ROADM-A-SRG1"));
    }

    @Test
    void multipleReadRequestsOnlyReadOnceFromDatastore() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = mock(Storage.class);
        when(fakeStore.read(new NetworkNodesKey("ROADM-A-SRG1"))).thenReturn(sharedRiskGroupMap);

        Storage cache = new Cache(fakeStore);

        assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        verify(fakeStore, times(1)).read(new NetworkNodesKey("ROADM-A-SRG1"));
    }

    @Test
    void cacheUpdatedOnWrite() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = mock(Storage.class);
        when(fakeStore.read("ROADM-A-SRG1")).thenReturn(sharedRiskGroupMap);
        when(fakeStore.save("ROADM-A-SRG1", sharedRiskGroupMap)).thenReturn(true);

        Storage cache = new Cache(fakeStore);

        assertTrue(cache.save("ROADM-A-SRG1", sharedRiskGroupMap));

        assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        cache.read("ROADM-B-SRG1");

        verify(fakeStore, times(0)).read("ROADM-A-SRG1");
        verify(fakeStore, times(1)).read(new NetworkNodesKey("ROADM-B-SRG1"));
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
