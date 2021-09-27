/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.util.Collection;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiPortMappingListener implements DataTreeChangeListener<Nodes> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiPortMappingListener.class);

    private final TapiNetworkModelService tapiNetworkModelService;

    public TapiPortMappingListener(TapiNetworkModelService tapiNetworkModelService) {
        this.tapiNetworkModelService = tapiNetworkModelService;
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Nodes>> changes) {
        for (DataTreeModification<Nodes> change : changes) {
            LOG.debug("TAPI module: Change in Node = {}", change.getRootNode());
            // Data before needs to be not null
            if (change.getRootNode().getDataAfter() != null && change.getRootNode().getDataBefore() != null) {
                Nodes nodesAft = change.getRootNode().getDataAfter();
                Nodes nodesBef = change.getRootNode().getDataBefore();
                // TODO -> need to filter out the ones that are not after creation.
                //  (Mapping before = null & Mapping after != null) is the rule for a first time connected device
                String nodeId = nodesAft.getNodeId();
                Map<MappingKey, Mapping> mappingAft = nodesAft.getMapping();
                Map<MappingKey, Mapping> mappingBef = nodesBef.getMapping();
                LOG.info("Change in node {} with OR version = {}", nodeId,
                    nodesAft.getNodeInfo().getOpenroadmVersion().getName());
                if (mappingAft != null && mappingBef == null) {
                    LOG.info("New mapping for node {} = {}", nodeId, mappingAft);
                    LOG.info("As the mapping is now created for the first time, "
                        + "we can proceed with the creation of the node {} in the TAPI topology", nodeId);
                    this.tapiNetworkModelService.createTapiNode(nodeId,
                        nodesAft.getNodeInfo().getOpenroadmVersion().getIntValue(), nodesAft);
                } else {
                    LOG.warn("Mapping already existed in the datastore, which means that node {} already existed "
                        + "in TAPI topology. The action to take will be different", nodeId);
                }
            }
        }
    }
}
