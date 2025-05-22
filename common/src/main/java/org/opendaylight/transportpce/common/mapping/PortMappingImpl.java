/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.opendaylight.transportpce.common.StringConstants.OPENCONFIG_DEVICE_VERSION_1_9_0;
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
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.metadata.OCMetaDataTransaction;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.NodeDatamodelType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mc.capabilities.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PortMappingImpl implements PortMapping {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingImpl.class);

    private final DataBroker dataBroker;
    private final PortMappingVersion710 portMappingVersion710;
    private final PortMappingVersion221 portMappingVersion22;
    private final PortMappingVersion121 portMappingVersion121;
    private final OCPortMappingVersion190 ocPortMappingVersion190;

    @Activate
    public PortMappingImpl(@Reference DataBroker dataBroker,
            @Reference DeviceTransactionManager deviceTransactionManager,
            @Reference OCMetaDataTransaction ocMetaDataTransaction,
            @Reference NetworkTransactionService networkTransactionService) {
        this(dataBroker,
            new PortMappingVersion710(dataBroker, deviceTransactionManager),
            new PortMappingVersion221(dataBroker, deviceTransactionManager),
            new PortMappingVersion121(dataBroker, deviceTransactionManager),
            new OCPortMappingVersion190(dataBroker,deviceTransactionManager,ocMetaDataTransaction,
                        networkTransactionService));
    }

    public PortMappingImpl(DataBroker dataBroker, PortMappingVersion710 portMappingVersion710,
        PortMappingVersion221 portMappingVersion22, PortMappingVersion121 portMappingVersion121,
                           OCPortMappingVersion190 ocPortMappingVersion190) {

        this.dataBroker = dataBroker;
        this.portMappingVersion710 = portMappingVersion710;
        this.portMappingVersion22 = portMappingVersion22;
        this.portMappingVersion121 = portMappingVersion121;
        this.ocPortMappingVersion190 = ocPortMappingVersion190;
    }

    @Override
    public PortMappingVersion221 getPortMappingVersion221() {
        return portMappingVersion22;
    }

    @Override
    public PortMappingVersion710 getPortMappingVersion710() {
        return portMappingVersion710;
    }

    @Override
    public boolean createMappingData(String nodeId, String nodeVersion, IpAddress ipAddress) {
        return switch (nodeVersion) {
            case OPENROADM_DEVICE_VERSION_1_2_1 -> portMappingVersion121.createMappingData(nodeId);
            case OPENROADM_DEVICE_VERSION_2_2_1 -> portMappingVersion22.createMappingData(nodeId);
            case OPENROADM_DEVICE_VERSION_7_1 -> portMappingVersion710.createMappingData(nodeId);
            case OPENCONFIG_DEVICE_VERSION_1_9_0 -> ocPortMappingVersion190.createMappingData(nodeId, ipAddress);
            default -> {
                LOG.error("Unable to create mapping data for unmanaged device version");
                yield false;
            }
        };
    }

    @Override
    public Mapping getMapping(String nodeId, String logicalConnPoint) {

        /*
         * Getting physical mapping corresponding to logical connection point
         */
        DataObjectIdentifier<Mapping> portMappingIID = DataObjectIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey(nodeId))
                .child(Mapping.class, new MappingKey(logicalConnPoint))
                .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Mapping> mapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, portMappingIID).get();
            if (mapObject.isPresent()) {
                Mapping mapping = mapObject.orElseThrow();
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
        WithKey<Nodes, NodesKey> portMappingIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId))
            .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> portMapppingOpt = readTx.read(LogicalDatastoreType.CONFIGURATION, portMappingIID).get();
            if (portMapppingOpt.isEmpty()) {
                LOG.warn("Could not get portMapping for node {}", nodeId);
                return null;
            }
            Map<MappingKey, Mapping> mappings = portMapppingOpt.orElseThrow().getMapping();
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
    public void deleteMapping(String nodeId, String logicalConnectionPoint) {
        LOG.info("Deleting Mapping {} of node '{}'", logicalConnectionPoint, nodeId);
        WriteTransaction rw = this.dataBroker.newWriteOnlyTransaction();
        DataObjectIdentifier<Mapping> mappingIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId))
            .child(Mapping.class, new MappingKey(logicalConnectionPoint))
            .build();
        rw.delete(LogicalDatastoreType.CONFIGURATION, mappingIID);
        try {
            rw.commit().get(1, TimeUnit.SECONDS);
            LOG.info("Mapping {} removed for node '{}'", logicalConnectionPoint, nodeId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error for removing mapping {} for node '{}'", logicalConnectionPoint, nodeId, e);
        }
    }

    @Override
    public McCapabilities getMcCapbilities(String nodeId, String mcLcp) {
        /*
         * Getting physical mapping corresponding to logical connection point
         */
        DataObjectIdentifier<McCapabilities> mcCapabilitiesIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId))
            .child(McCapabilities.class, new McCapabilitiesKey(mcLcp))
            .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<McCapabilities> mcCapObject = readTx.read(LogicalDatastoreType.CONFIGURATION,
                mcCapabilitiesIID).get();
            if (mcCapObject.isPresent()) {
                McCapabilities mcCap = mcCapObject.orElseThrow();
                LOG.info("Found MC-cap for {} - {}. Mapping: {}", nodeId, mcLcp, mcCap.toString());
                return mcCap;
            }
            LOG.warn("Could not find mc-capabilities for logical connection point {} for nodeId {}", mcLcp, nodeId);
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for logical connection point : {} for nodeId {}", mcLcp,
                nodeId, ex);
        }
        return null;
    }


    @Override
    public void deletePortMappingNode(String nodeId) {
        LOG.info("Deleting Mapping Data corresponding at node '{}'", nodeId);
        WriteTransaction rw = this.dataBroker.newWriteOnlyTransaction();
        DataObjectIdentifier<Nodes> nodesIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId))
            .build();
        rw.delete(LogicalDatastoreType.CONFIGURATION, nodesIID);
        try {
            rw.commit().get(1, TimeUnit.SECONDS);
            LOG.info("Port mapping removal for node '{}'", nodeId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error for removing port mapping infos for node '{}'", nodeId, e);
        }
    }

    @Override
    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        LOG.info("update mapping called");
        OpenroadmNodeVersion openROADMversion = getNode(nodeId).getNodeInfo().getOpenroadmVersion();
        NodeDatamodelType datamodelType = getNode(nodeId).getDatamodelType();

        if (datamodelType != null && datamodelType.equals(NodeDatamodelType.OPENCONFIG)) {
            return ocPortMappingVersion190.updateMapping(nodeId, oldMapping);
        } else {
            return switch (openROADMversion) {
                case _121 -> portMappingVersion121.updateMapping(nodeId, oldMapping);
                case _221 -> portMappingVersion22.updateMapping(nodeId, oldMapping);
                case _71 -> portMappingVersion710.updateMapping(nodeId, oldMapping);
                default -> false;
            };
        }
    }

    @Override
    public Nodes getNode(String nodeId) {
        DataObjectIdentifier<Nodes> nodePortMappingIID = DataObjectIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey(nodeId))
                .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodePortMapObject =
                readTx.read(LogicalDatastoreType.CONFIGURATION, nodePortMappingIID).get();
            if (nodePortMapObject.isPresent()) {
                Nodes node = nodePortMapObject.orElseThrow();
                LOG.info("Found node {} in portmapping.", nodeId);
                return node;
            }
            LOG.warn("Could not find node {} in portmapping.", nodeId);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to get node {} in portmapping", nodeId, e);
        }
        return null;
    }

    @Override
    public boolean updatePortMappingWithOduSwitchingPools(String nodeId, DataObjectIdentifier<OduSwitchingPools> ospIID,
            Map<Uint16, List<DataObjectIdentifier<PortList>>> nbliidMap) {
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
    public boolean isNodeExist(String nodeId) {
        return this.getNode(nodeId) != null;
    }

    @Override
    public Mapping getMappingFromOtsInterface(String nodeId, String interfName) {
        WithKey<Nodes, NodesKey> nodePortmappingIID = DataObjectIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId))
            .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodePortmapppingOpt
                = readTx.read(LogicalDatastoreType.CONFIGURATION, nodePortmappingIID).get();
            if (nodePortmapppingOpt.isEmpty()) {
                LOG.warn("Could not get portMapping for node {}", nodeId);
                return null;
            }
            Map<MappingKey, Mapping> mappings = nodePortmapppingOpt.orElseThrow().getMapping();
            for (Mapping mapping : mappings.values()) {
                if (interfName.equals(mapping.getSupportingOts())) {
                    return mapping;
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to get mapping list for nodeId {}", nodeId, ex);
        }
        return null;
    }
}
