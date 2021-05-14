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
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_7_1;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.switching.pool.lcp.SwitchingPoolLcp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
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
            case OPENROADM_DEVICE_VERSION_7_1:
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
    public Mapping getMapping(String nodeId, String circuitPackName, String portName) {
        KeyedInstanceIdentifier<Nodes, NodesKey> portMappingIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> portMapppingOpt = readTx.read(LogicalDatastoreType.CONFIGURATION, portMappingIID).get();
            if (!portMapppingOpt.isPresent()) {
                LOG.warn("Could not get portMapping for node {}", nodeId);
                return null;
            }
            Map<MappingKey, Mapping> mappings = portMapppingOpt.get().getMapping();
            for (Mapping mapping : mappings.values()) {
                if (circuitPackName.equals(mapping.getSupportingCircuitPackName())
                    && portName.equals(mapping.getSupportingPort())) {
                    return mapping;
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to get mapping list for nodeId {}", nodeId, ex);
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
        OpenroadmNodeVersion openROADMversion = getNode(nodeId).getNodeInfo().getOpenroadmVersion();
        switch (openROADMversion.getIntValue()) {
            case 1:
                return portMappingVersion121.updateMapping(nodeId, oldMapping);
            case 2:
                return portMappingVersion22.updateMapping(nodeId, oldMapping);
            case 3:
                return portMappingVersion710.updateMapping(nodeId, oldMapping);
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

    @Override
    public boolean updatePortMappingWithOduSwitchingPools(String nodeId, InstanceIdentifier<OduSwitchingPools> ospIID,
            Map<Uint16, List<InstanceIdentifier<PortList>>> nbliidMap) {
        OpenroadmNodeVersion openROADMversion = getNode(nodeId).getNodeInfo().getOpenroadmVersion();
        switch (openROADMversion.getIntValue()) {
            case 3:
                return portMappingVersion710.updatePortMappingWithOduSwitchingPools(nodeId, ospIID, nbliidMap);
            default:
                LOG.error("Update of the port-mapping [odu-switching-pool] not available for this device version {}",
                    openROADMversion);
                return false;
        }
    }

    @Override
    public boolean updatePortMappingWithOduSwitchingPools(String nodeId, SwitchingPoolLcp switchingPoolLcp) {
        OpenroadmNodeVersion openROADMversion = getNode(nodeId).getNodeInfo().getOpenroadmVersion();
        switch (openROADMversion.getIntValue()) {
            case 3:
                return portMappingVersion710.updatePortMappingWithOduSwitchingPools(nodeId, switchingPoolLcp);
            default:
                LOG.error("Update of the port-mapping [odu-switching-pool] not available for this device version {}",
                        openROADMversion);
                return false;
        }
    }
}
