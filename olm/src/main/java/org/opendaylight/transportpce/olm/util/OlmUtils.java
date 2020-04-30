/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfo.OpenroadmVersion;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OlmUtils {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils.class);
    private static long DATABROKER_READ_TIMEOUT_SECONDS = 120;
    public static final long OLM_TIMER_1 = 120000; //#FUNCTESTVAL= 3000;
    public static final long OLM_TIMER_2 = 20000;  //#FUNCTESTVAL= 2000;

    /**
     * This static method returns the port mapping {@link Nodes} for node.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf node
     * @param db
     *            Databroker used to read data from data store.
     * @return {@link Nodes } from portMapping for given nodeId
     */
    public static Optional<Nodes> getNode(String nodeId, DataBroker db) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        try (ReadTransaction readTransaction = db.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, nodesIID)
                .get(DATABROKER_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            LOG.error("Unable to read Portmapping for nodeId {}", nodeId, ex);
            return Optional.empty();
        }
    }

    /**
     * This method retrieves list of current PMs for given nodeId,
     * resourceType, resourceName and Granularity.Currently vendorExtentions
     * are excluded but can be added back based on requirement
     *
     * <p>
     * 1. pmFetch This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * @param input
     *            Input parameter from the olm yang model get-pm rpc
     * @param deviceTransactionManager
     *            Device tx manager
     * @param openRoadmVersion
     *            OpenRoadm version number
     *
     * @return Result of the request list of PM readings
     */
    public static GetPmOutputBuilder pmFetch(GetPmInput input, DeviceTransactionManager deviceTransactionManager,
                                             OpenroadmVersion openRoadmVersion) {
        LOG.info("Getting PM Data for NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
            input.getResourceType(), input.getResourceIdentifier());
        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();
        if (openRoadmVersion.getIntValue() == 1) {
            pmOutputBuilder = OlmUtils121.pmFetch(input, deviceTransactionManager);
        } else {
            pmOutputBuilder = OlmUtils22.pmFetch(input, deviceTransactionManager);
        }
        return pmOutputBuilder;
    }

    private OlmUtils() {
    }

}
