/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.networks.network.network.types.ClliNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.networks.network.network.types.OpenroadmCommonNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NetworkTypes;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TpceNetworkTest extends AbstractTest {
    TpceNetwork tpceNetwork = new TpceNetwork(new NetworkTransactionImpl(new RequestProcessor(getDataBroker())));

    @Test
    public void createLayerClliTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("clli-network");
        InstanceIdentifier<Network> nwIID = InstanceIdentifier.create(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("clli-network")));
        Network createdClli = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().get();
        assertNotNull("Clli layer should be created and not null", createdClli);

        Augmentation<NetworkTypes> ordClli = new NetworkTypes1Builder()
            .setClliNetwork(new ClliNetworkBuilder().build())
            .build();
        assertNotNull("clli augmentation should not be null", createdClli.getNetworkTypes()
            .augmentation(NetworkTypes1.class));
        assertEquals("bad clli augmentation for network-types", ordClli,
            createdClli.getNetworkTypes().augmentation(NetworkTypes1.class));
    }

    @Test
    public void createLayerNetworkTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("openroadm-network");
        InstanceIdentifier<Network> nwIID = InstanceIdentifier.create(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("openroadm-network")));
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().get();
        assertNotNull("openroadm-network layer should be created and not null", createdOrdNetwork);
        commonNetworkAugmentationTest(createdOrdNetwork);
    }

    @Test
    public void createLayerTopologyTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("openroadm-topology");
        InstanceIdentifier<Network> nwIID = InstanceIdentifier.create(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("openroadm-topology")));
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().get();
        assertNotNull("openroadm-logpology layer should be created and not null", createdOrdNetwork);
        commonNetworkAugmentationTest(createdOrdNetwork);
    }

    @Test
    public void createLayerOtnTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("otn-topology");
        InstanceIdentifier<Network> nwIID = InstanceIdentifier.create(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("otn-topology")));
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().get();
        assertNotNull("otn-logpology layer should be created and not null", createdOrdNetwork);
        commonNetworkAugmentationTest(createdOrdNetwork);
    }

    @Test
    public void createBadLayerTest() throws InterruptedException, ExecutionException {
        tpceNetwork.createLayer("toto");
        InstanceIdentifier<Network> nwIID = InstanceIdentifier.create(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("toto")));
        Network createdOrdNetwork = getDataBroker().newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, nwIID).get().get();
        assertNotNull("toto layer should be created and not null", createdOrdNetwork);
        assertNull("toto layer should not have any network-type augmentation", createdOrdNetwork.getNetworkTypes()
            .augmentation(NetworkTypes1.class));
        assertNull("toto layer should not have any network-type augmentation", createdOrdNetwork.getNetworkTypes()
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
            .NetworkTypes1.class));
    }

    private void commonNetworkAugmentationTest(Network createdOrdNetwork) {
        Augmentation<NetworkTypes> ordComNet
            = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.NetworkTypes1Builder()
            .setOpenroadmCommonNetwork(new OpenroadmCommonNetworkBuilder().build())
            .build();
        assertNotNull("common-network augmentation should not be null", createdOrdNetwork.getNetworkTypes()
            .augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.NetworkTypes1.class));
        assertEquals("bad common-network augmentation for network-types", ordComNet, createdOrdNetwork.getNetworkTypes()
            .augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.NetworkTypes1.class));
    }
}
