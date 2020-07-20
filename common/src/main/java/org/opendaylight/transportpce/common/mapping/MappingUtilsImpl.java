/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.nodes.NodeInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingUtilsImpl implements MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtilsImpl.class);

    private final DataBroker dataBroker;

    public MappingUtilsImpl(DataBroker dataBroker) {

        this.dataBroker = dataBroker;

    }

    public String getOpenRoadmVersion(String nodeId) {
        /*
         * Getting physical mapping corresponding to logical connection point
         */
        InstanceIdentifier<NodeInfo> nodeInfoIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey(nodeId)).child(NodeInfo.class).build();
        try (ReadTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            Optional<NodeInfo> nodeInfoObj =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, nodeInfoIID).get();
            if (nodeInfoObj.isPresent()) {
                NodeInfo nodInfo = nodeInfoObj.get();
                switch (nodInfo.getOpenroadmVersion()) {
                    case _221:
                        return StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;
                    case _121:
                        return StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
                    default:
                        LOG.warn("unknown openROADM device version");
                }
            } else {
                LOG.warn("Could not find mapping for nodeId {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for nodeId {}",nodeId, ex);
        }
        return null;
    }
}
