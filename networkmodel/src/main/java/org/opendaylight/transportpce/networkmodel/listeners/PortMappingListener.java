/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.LinkedList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.Nodes;
import org.opendaylight.yangtools.binding.DataObjectStep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Implementation that listens to any data change on
 * org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping object.
 */
public class PortMappingListener implements DataTreeChangeListener<Mapping> {

    private final NetworkModelService networkModelService;

    /**
     * Instantiate the PortMappingListener.
     * @param networkModelService Service that eases data handling in topology data-stores.
     */
    public PortMappingListener(NetworkModelService networkModelService) {
        this.networkModelService = networkModelService;
    }

    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<Mapping>> changes) {
        for (DataTreeModification<Mapping> change : changes) {
            Mapping oldMapping = change.getRootNode().dataBefore();
            if (oldMapping == null) {
                continue;
            }
            Mapping newMapping = change.getRootNode().dataAfter();
            if (newMapping == null) {
                continue;
            }
            if (oldMapping.getPortAdminState().equals(newMapping.getPortAdminState())
                    && oldMapping.getPortOperState().equals(newMapping.getPortOperState())) {
                return;
            }
            networkModelService.updateOpenRoadmTopologies(
                getNodeIdFromMappingDataTreeIdentifier(change.getRootPath()),
                newMapping);
        }
    }

    /**
     * Retrieve from the data change the node id that emits the device notification.
     * @param dataTreeIdentifier Instance Identifiers of the mapping change.
     * @return the node ID, parent of the data tree change.
     */
    protected String getNodeIdFromMappingDataTreeIdentifier(DataTreeIdentifier<Mapping> dataTreeIdentifier) {
        LinkedList<DataObjectStep<?>> path = new LinkedList<>();
        dataTreeIdentifier.path().getPathArguments().forEach(p -> path.add(p));
        path.removeLast();
        InstanceIdentifier<Nodes> portMappingNodeID = InstanceIdentifier.unsafeOf(path);
        return InstanceIdentifier.keyOf(portMappingNodeID).getNodeId();
    }
}
