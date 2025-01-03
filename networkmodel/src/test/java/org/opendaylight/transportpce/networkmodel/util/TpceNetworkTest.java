/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.networks.network.network.types.ClliNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.networks.network.network.types.OpenroadmCommonNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NetworkTypes;
import org.opendaylight.yangtools.binding.Augmentation;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class TpceNetworkTest extends AbstractTest {
    TpceNetwork tpceNetwork = new TpceNetwork(new NetworkTransactionImpl(getDataBroker()));

    @Test
    void createLayerClliTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("clli-network");
        DataObjectIdentifier<Network> nwIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("clli-network")))
            .build();
        Network createdClli = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().orElseThrow();
        assertNotNull(createdClli, "Clli layer should be created and not null");

        Augmentation<NetworkTypes> ordClli = new NetworkTypes1Builder()
            .setClliNetwork(new ClliNetworkBuilder().build())
            .build();
        assertNotNull(createdClli.getNetworkTypes().augmentation(NetworkTypes1.class),
            "clli augmentation should not be null");
        assertEquals(ordClli, createdClli.getNetworkTypes().augmentation(NetworkTypes1.class),
            "bad clli augmentation for network-types");
    }

    @Test
    void createLayerNetworkTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("openroadm-network");
        DataObjectIdentifier<Network> nwIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("openroadm-network")))
            .build();
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().orElseThrow();
        assertNotNull(createdOrdNetwork, "openroadm-network layer should be created and not null");
        commonNetworkAugmentationTest(createdOrdNetwork);
    }

    @Test
    void createLayerTopologyTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("openroadm-topology");
        DataObjectIdentifier<Network> nwIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("openroadm-topology")))
            .build();
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().orElseThrow();
        assertNotNull(createdOrdNetwork, "openroadm-logpology layer should be created and not null");
        commonNetworkAugmentationTest(createdOrdNetwork);
    }

    @Test
    void createLayerOtnTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("otn-topology");
        DataObjectIdentifier<Network> nwIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("otn-topology")))
            .build();
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().orElseThrow();
        assertNotNull(createdOrdNetwork, "otn-logpology layer should be created and not null");
        commonNetworkAugmentationTest(createdOrdNetwork);
    }

    @Test
    void createBadLayerTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("toto");
        DataObjectIdentifier<Network> nwIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("toto")))
            .build();
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().orElseThrow();
        assertNotNull(createdOrdNetwork, "toto layer should be created and not null");
        assertNull(createdOrdNetwork.getNetworkTypes().augmentation(NetworkTypes1.class),
            "toto layer should not have any network-type augmentation");
        assertNull(
            createdOrdNetwork.getNetworkTypes().augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.NetworkTypes1.class),
            "toto layer should not have any network-type augmentation");
    }

    private void commonNetworkAugmentationTest(Network createdOrdNetwork) {
        Augmentation<NetworkTypes> ordComNet
            = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.NetworkTypes1Builder()
                .setOpenroadmCommonNetwork(new OpenroadmCommonNetworkBuilder().build())
                .build();
        assertNotNull(
            createdOrdNetwork.getNetworkTypes().augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.NetworkTypes1.class),
            "common-network augmentation should not be null");
        assertEquals(
            ordComNet, createdOrdNetwork.getNetworkTypes().augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.NetworkTypes1.class),
            "bad common-network augmentation for network-types");
    }
}
