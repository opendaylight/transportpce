/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PortMappingImpl implements PortMapping {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingImpl.class);

    private final DataBroker dataBroker;
    private final MappingUtils mappingUtils;
    private final PortMappingVersion221 portMappingVersion22;
    private final PortMappingVersion121 portMappingVersion121;

    public PortMappingImpl(DataBroker dataBroker, PortMappingVersion221 portMappingVersion22, MappingUtils mappingUtils,
                           PortMappingVersion121 portMappingVersion121) {

        this.dataBroker = dataBroker;
        this.mappingUtils = mappingUtils;
        this.portMappingVersion22 = portMappingVersion22;
        this.portMappingVersion121 = portMappingVersion121;
    }

    @Override
    public boolean createMappingData(String nodeId, String nodeVersion) {
        if (nodeVersion.equals(OPENROADM_DEVICE_VERSION_1_2_1)) {
            return portMappingVersion121.createMappingData(nodeId);
        }
        else if (nodeVersion.equals(OPENROADM_DEVICE_VERSION_2_2_1)) {
            return portMappingVersion22.createMappingData(nodeId);
        }
        else {
            return false;
        }
    }



    @Override
    public Mapping getMapping(String nodeId, String logicalConnPoint) {

        /*
         * Getting physical mapping corresponding to logical connection point
         */
        InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
            new NodesKey(nodeId)).child(Mapping.class, new MappingKey(logicalConnPoint)).build();
        try (ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Mapping> mapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, portMappingIID).get()
                .toJavaUtil();
            if (mapObject.isPresent()) {
                Mapping mapping = mapObject.get();
                LOG.info("Found mapping for the logical port {}. Mapping: {}", logicalConnPoint, mapping.toString());
                return mapping;
            } else {
                LOG.warn("Could not find mapping for logical connection point {} for nodeId {}", logicalConnPoint,
                    nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for logical connection point : {} for nodeId {}", logicalConnPoint,
                nodeId, ex);
        }
        return null;
    }




    @Override
    public void deleteMappingData(String nodeId) {
        LOG.info("Deleting Mapping Data corresponding at node '{}'", nodeId);
        WriteTransaction rw = this.dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        rw.delete(LogicalDatastoreType.CONFIGURATION, nodesIID);
        try {
            rw.submit().get(1, TimeUnit.SECONDS);
            LOG.info("Port mapping removal for node '{}'", nodeId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error for removing port mapping infos for node '{}'", nodeId);
        }

    }

    @Override
    public boolean updateMapping(String nodeId, Mapping oldMapping) {


        String openROADMversion = mappingUtils.getOpenRoadmVersion(nodeId);
        if (openROADMversion.equals(OPENROADM_DEVICE_VERSION_1_2_1)) {
            return portMappingVersion121.updateMapping(nodeId,oldMapping);
        }
        else if (openROADMversion.equals(OPENROADM_DEVICE_VERSION_2_2_1)) {
            return portMappingVersion22.updateMapping(nodeId,oldMapping);
        }

        else {
            return false;
        }
    }

    @Override
    public Nodes getNode(String nodeId) {
        InstanceIdentifier<Nodes> nodePortMappingIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
            new NodesKey(nodeId)).build();
        try (ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodePortMapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, nodePortMappingIID)
                .get().toJavaUtil();
            if (nodePortMapObject.isPresent()) {
                Nodes node = nodePortMapObject.get();
                LOG.info("Found node {} in portmapping.", nodeId);
                return node;
            } else {
                LOG.warn("Could not find node {} in portmapping.", nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to get node {} in portmapping", nodeId);
        }
        return null;
    }


}
