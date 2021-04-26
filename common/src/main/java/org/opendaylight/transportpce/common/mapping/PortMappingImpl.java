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
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_7_1_0;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfo.OpenroadmVersion;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PortMappingImpl implements PortMapping {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingImpl.class);

    private final DataBroker dataBroker;
    private final PortMappingVersion710 portMappingVersion710;
    private final PortMappingVersion221 portMappingVersion22;
    private final PortMappingVersion121 portMappingVersion121;

    public PortMappingImpl(DataBroker dataBroker, PortMappingVersion710 portMappingVersion710,
        PortMappingVersion221 portMappingVersion22, PortMappingVersion121 portMappingVersion121) {

        this.dataBroker = dataBroker;
        this.portMappingVersion710 = portMappingVersion710;
        this.portMappingVersion22 = portMappingVersion22;
        this.portMappingVersion121 = portMappingVersion121;
    }

    @Override
    public boolean createMappingData(String nodeId, String nodeVersion) {
        switch (nodeVersion) {
            case OPENROADM_DEVICE_VERSION_1_2_1:
                return portMappingVersion121.createMappingData(nodeId);
            case OPENROADM_DEVICE_VERSION_2_2_1:
                return portMappingVersion22.createMappingData(nodeId);
            case OPENROADM_DEVICE_VERSION_7_1_0:
                return portMappingVersion710.createMappingData(nodeId);
            default:
                LOG.error("Unable to create mapping data for unmanaged openroadm device version");
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
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Mapping> mapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, portMappingIID).get();
            if (mapObject.isPresent()) {
                Mapping mapping = mapObject.get();
                LOG.info("Found mapping for {} - {}. Mapping: {}", nodeId, logicalConnPoint, mapping.toString());
                return mapping;
            }
            LOG.warn("Could not find mapping for logical connection point {} for nodeId {}", logicalConnPoint, nodeId);
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for logical connection point : {} for nodeId {}", logicalConnPoint,
                nodeId, ex);
        }
        return null;
    }


    @Override
    public McCapabilities getMcCapbilities(String nodeId, String mcLcp) {
        /*
         * Getting physical mapping corresponding to logical connection point
         */
        InstanceIdentifier<McCapabilities> mcCapabilitiesIID = InstanceIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId)).child(McCapabilities.class, new McCapabilitiesKey(mcLcp)).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<McCapabilities> mcCapObject = readTx.read(LogicalDatastoreType.CONFIGURATION,
                mcCapabilitiesIID).get();
            if (mcCapObject.isPresent()) {
                McCapabilities mcCap = mcCapObject.get();
                LOG.info("Found MC-cap for {} - {}. Mapping: {}", nodeId, mcLcp, mcCap.toString());
                return mcCap;
            }
            LOG.warn("Could not find mapping for logical connection point {} for nodeId {}", mcLcp, nodeId);
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for logical connection point : {} for nodeId {}", mcLcp,
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
            rw.commit().get(1, TimeUnit.SECONDS);
            LOG.info("Port mapping removal for node '{}'", nodeId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error for removing port mapping infos for node '{}'", nodeId);
        }

    }

    @Override
    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        OpenroadmVersion openROADMversion = getNode(nodeId).getNodeInfo().getOpenroadmVersion();
        switch (openROADMversion.getIntValue()) {
            case 1:
                return portMappingVersion121.updateMapping(nodeId, oldMapping);
            case 2:
                MappingBuilder oldMapping2Bldr221 = new MappingBuilder()
                        .setLogicalConnectionPoint(oldMapping.getLogicalConnectionPoint())
                        .setPortDirection(oldMapping.getPortDirection());
                if (oldMapping.getConnectionMapLcp() != null) {
                    oldMapping2Bldr221.setConnectionMapLcp(oldMapping.getConnectionMapLcp());
                }
                if (oldMapping.getPartnerLcp() != null) {
                    oldMapping2Bldr221.setPartnerLcp(oldMapping.getPartnerLcp());
                }
                if (oldMapping.getPortQual() != null) {
                    oldMapping2Bldr221.setPortQual(oldMapping.getPortQual());
                }
                if (oldMapping.getSupportingCircuitPackName() != null) {
                    oldMapping2Bldr221.setSupportingCircuitPackName(oldMapping.getSupportingCircuitPackName());
                }
                if (oldMapping.getSupportingOms() != null) {
                    oldMapping2Bldr221.setSupportingOms(oldMapping.getSupportingOms());
                }
                if (oldMapping.getSupportingOts() != null) {
                    oldMapping2Bldr221.setSupportingOts(oldMapping.getSupportingOts());
                }
                if (oldMapping.getSupportingPort() != null) {
                    oldMapping2Bldr221.setSupportingPort(oldMapping.getSupportingPort());
                }
                return portMappingVersion22.updateMapping(nodeId, oldMapping2Bldr221.build());
            case 3:
                MappingBuilder oldMapping2Bldr710 = new MappingBuilder()
                        .setLogicalConnectionPoint(oldMapping.getLogicalConnectionPoint())
                        .setPortDirection(oldMapping.getPortDirection());
                if (oldMapping.getConnectionMapLcp() != null) {
                    oldMapping2Bldr710.setConnectionMapLcp(oldMapping.getConnectionMapLcp());
                }
                if (oldMapping.getPartnerLcp() != null) {
                    oldMapping2Bldr710.setPartnerLcp(oldMapping.getPartnerLcp());
                }
                if (oldMapping.getPortQual() != null) {
                    oldMapping2Bldr710.setPortQual(oldMapping.getPortQual());
                }
                if (oldMapping.getSupportingCircuitPackName() != null) {
                    oldMapping2Bldr710.setSupportingCircuitPackName(oldMapping.getSupportingCircuitPackName());
                }
                if (oldMapping.getSupportingOms() != null) {
                    oldMapping2Bldr710.setSupportingOms(oldMapping.getSupportingOms());
                }
                if (oldMapping.getSupportingOts() != null) {
                    oldMapping2Bldr710.setSupportingOts(oldMapping.getSupportingOts());
                }
                if (oldMapping.getSupportingPort() != null) {
                    oldMapping2Bldr710.setSupportingPort(oldMapping.getSupportingPort());
                }
                return portMappingVersion710.updateMapping(nodeId, oldMapping2Bldr710.build());
            default:
                return false;
        }
    }

    @Override
    public Nodes getNode(String nodeId) {
        InstanceIdentifier<Nodes> nodePortMappingIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
            new NodesKey(nodeId)).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodePortMapObject =
                readTx.read(LogicalDatastoreType.CONFIGURATION, nodePortMappingIID).get();
            if (nodePortMapObject.isPresent()) {
                Nodes node = nodePortMapObject.get();
                LOG.info("Found node {} in portmapping.", nodeId);
                return node;
            }
            LOG.warn("Could not find node {} in portmapping.", nodeId);
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to get node {} in portmapping", nodeId);
        }
        return null;
    }


}
