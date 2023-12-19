/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupKey;
import org.opendaylight.yangtools.yang.common.Uint16;

class CacheTest {

    @Test
    void save() {

        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = Mockito.mock(Storage.class);
        Mockito.when(fakeStore.save("ROADM-A-SRG1", sharedRiskGroupMap)).thenReturn(true);

        Storage cache = new Cache(fakeStore);

        Assert.assertTrue(cache.save("ROADM-A-SRG1", sharedRiskGroupMap));

        Mockito.verify(fakeStore, Mockito.times(1)).save("ROADM-A-SRG1", sharedRiskGroupMap);
    }

    @Test
    void readFromStorage() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = Mockito.mock(Storage.class);
        Mockito.when(fakeStore.read("ROADM-A-SRG1")).thenReturn(sharedRiskGroupMap);

        Storage cache = new Cache(fakeStore);

        Assert.assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        Mockito.verify(fakeStore, Mockito.times(1)).read("ROADM-A-SRG1");
    }

    @Test
    void multipleReadRequestsOnlyReadOnceFromDatastore() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = Mockito.mock(Storage.class);
        Mockito.when(fakeStore.read("ROADM-A-SRG1")).thenReturn(sharedRiskGroupMap);

        Storage cache = new Cache(fakeStore);

        Assert.assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        Assert.assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        Mockito.verify(fakeStore, Mockito.times(1)).read("ROADM-A-SRG1");
    }

    @Test
    void cacheUpdatedOnWrite() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = Mockito.mock(Storage.class);
        Mockito.when(fakeStore.read("ROADM-A-SRG1")).thenReturn(sharedRiskGroupMap);
        Mockito.when(fakeStore.save("ROADM-A-SRG1", sharedRiskGroupMap)).thenReturn(true);

        Storage cache = new Cache(fakeStore);

        Assert.assertTrue(cache.save("ROADM-A-SRG1", sharedRiskGroupMap));

        Assert.assertEquals(sharedRiskGroupMap, cache.read("ROADM-A-SRG1"));

        cache.read("ROADM-B-SRG1");

        Mockito.verify(fakeStore, Mockito.times(0)).read("ROADM-A-SRG1");
        Mockito.verify(fakeStore, Mockito.times(1)).read("ROADM-B-SRG1");
    }

    @Test
    void assertReadDontReturnNull() {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder().setSrgNumber(Uint16.ONE).build();
        sharedRiskGroupMap.put(sharedRiskGroup.key(), sharedRiskGroup);

        Storage fakeStore = Mockito.mock(Storage.class);

        Storage cache = new Cache(fakeStore);

        Assert.assertNotNull(cache.read("ROADM-B-SRG1"));
    }
}