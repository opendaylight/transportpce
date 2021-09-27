/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Collection;
import java.util.LinkedList;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.Nodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMappingListener implements DataTreeChangeListener<Mapping> {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingListener.class);

    private final NetworkModelService networkModelService;

    public PortMappingListener(NetworkModelService networkModelService) {
        this.networkModelService = networkModelService;
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Mapping>> changes) {
        for (DataTreeModification<Mapping> change : changes) {
            if (change.getRootNode().getDataBefore() != null && change.getRootNode().getDataAfter() != null) {
                Mapping oldMapping = change.getRootNode().getDataBefore();
                Mapping newMapping = change.getRootNode().getDataAfter();
                if (oldMapping.getPortAdminState().equals(newMapping.getPortAdminState())
                    || oldMapping.getPortOperState().equals(newMapping.getPortOperState())) {
                    return;
                } else {
                    LinkedList<PathArgument> path = new LinkedList<>();
                    path.addAll((Collection<? extends PathArgument>) change.getRootPath().getRootIdentifier()
                        .getPathArguments());
                    path.removeLast();
                    @SuppressWarnings("unchecked") InstanceIdentifier<Nodes> portmappintNodeID =
                        (InstanceIdentifier<Nodes>) InstanceIdentifier.create(path);
                    String nodeId = InstanceIdentifier.keyOf(portmappintNodeID).getNodeId();
                    networkModelService.updateOpenRoadmTopologies(nodeId, newMapping);
                }
            }
        }
    }
}
