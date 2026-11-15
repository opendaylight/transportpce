/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.Profile;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.ProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfile;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfile;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of TransformationService for transforming TAPI node and link
 * JSON data to SBI topology normalized nodes.
 */
@Component
public class TransformationServiceImpl implements TransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(
        TransformationServiceImpl.class);
    private static final String TAPI_TOPOLOGY_NAME = "TAPI Topology Name";
    private static final String ORIGINAL_UUID_NAME = "original-uuid";
    private static final String CONTROLLER_ID_NAME = "controller-id";
    private static final String SBI_TOPOLOGY_NAME_PREFIX = "SBI - ";
    private static final String SBI_TOPOLOGY_NAME_SUFFIX = " - Multi-layer - TAPI topology";

    private final BindingDOMCodecServices bindingDOMCodecServices;
    private final NetworkTransactionService networkTransactionService;
    private String controllerId;

    @Activate
    public TransformationServiceImpl(
            @Reference BindingDOMCodecServices bindingDOMCodecServices,
            @Reference NetworkTransactionService networkTransactionService) {
        this.bindingDOMCodecServices = bindingDOMCodecServices;
        this.networkTransactionService = networkTransactionService;
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }

    private EffectiveModelContext getModelContext() {
        return bindingDOMCodecServices.getRuntimeContext().modelContext();
    }

    @Override
    public ListenableFuture<Void> updateNodeInSbiTopology(String nodeJson,
            String sbiTopologyUuid) {
        try {
            transformNodeJson(nodeJson, sbiTopologyUuid);
            return Futures.immediateFuture(null);
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Failed to transform node JSON for topology {}",
                sbiTopologyUuid, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * Transforms node JSON to NormalizedNode and logs the result.
     *
     * @param nodeJson the node JSON string
     * @param sbiTopologyUuidStr the SBI topology UUID string
     * @throws IOException if transformation fails
     * @throws IllegalArgumentException if invalid arguments
     */
    private void transformNodeJson(String nodeJson, String sbiTopologyUuidStr)
            throws IOException, IllegalArgumentException {
        // Create SchemaInferenceStack pointing to Topology (parent of nodes)
        EffectiveModelContext modelContext = getModelContext();
        var stack = SchemaInferenceStack.of(modelContext);
        stack.enterSchemaTree(Context.QNAME);
        stack.enterSchemaTree(QName.create(
            "urn:onf:otcc:yang:tapi-topology", "2022-11-21",
            "topology-context"));
        stack.enterSchemaTree(QName.create(
            "urn:onf:otcc:yang:tapi-topology", "2022-11-21", "topology"));
        Inference topologyInference = stack.toInference();
        LOG.debug("Created SchemaInferenceStack with parent: Topology");

        try (JsonReader reader = new JsonReader(new StringReader(nodeJson))) {
            // Parse JSON with tapi-topology:node as root (NO WRAPPING!)
            NormalizationResultHolder result = new NormalizationResultHolder();
            try (NormalizedNodeStreamWriter streamWriter =
                    ImmutableNormalizedNodeStreamWriter.from(result);
                 JsonParserStream jsonParser = JsonParserStream.create(
                     streamWriter,
                     JSONCodecFactorySupplier.RFC7951.getShared(modelContext),
                     topologyInference)) {
                jsonParser.parse(reader);
            }
            LOG.debug("JSON parsing completed successfully (direct, no wrapping)");

            NormalizedNode normalizedNode = result.getResult().data();
            if (normalizedNode == null) {
                LOG.error("Normalized node is null after parsing");
                return;
            }

            LOG.debug("Normalized node created - Type: {}, Name: {}",
                normalizedNode.getClass().getSimpleName(),
                normalizedNode.name());

            QName topologyQName = QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "topology");
            QName nodeQName = QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "node");
            QName uuidQName = QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "uuid");

            if (normalizedNode instanceof MapNode mapNode) {
                LOG.debug("Processing MapNode with {} entries", mapNode.size());
                processMapNodeEntries(mapNode, topologyQName, nodeQName, uuidQName, sbiTopologyUuidStr);
            }
        }
    }

    private void processMapNodeEntries(MapNode mapNode, QName topologyQName,
            QName nodeQName, QName uuidQName, String sbiTopologyUuidStr) {
        // Capture field in local variable to avoid SpotBugs UWF warning in lambda
        final BindingDOMCodecServices codecServices = this.bindingDOMCodecServices;
        final Uuid newTopologyUuid = generateTransformedUuid(sbiTopologyUuidStr, controllerId);
        LOG.debug("Generated new topology UUID: {} from original: {} and controller: {}",
            newTopologyUuid.getValue(), sbiTopologyUuidStr, controllerId);

        mapNode.body().forEach(nodeEntry -> {
            LOG.debug("Processing node entry: {}", nodeEntry.name());
            try {
                var nodePath = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder()
                    .node(Context.QNAME)
                    .node(QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "topology-context"))
                    .node(topologyQName)
                    .nodeWithKey(topologyQName, uuidQName, sbiTopologyUuidStr)
                    .node(nodeQName)
                    .node(nodeEntry.name())
                    .build();

                var bindingEntry = codecServices.fromNormalizedNode(nodePath, nodeEntry);
                if (bindingEntry != null && bindingEntry.getValue() instanceof Node node) {
                    LOG.debug("Parsed node UUID: {}", node.getUuid().getValue());
                    Node transformedNode = transformNode(node);
                    if (saveNodeInTopology(transformedNode, newTopologyUuid, sbiTopologyUuidStr)) {
                        LOG.debug("Stored node {} in topology {} for controller {}",
                                node.getUuid().getValue(), newTopologyUuid.getValue(), controllerId);
                    }
                }
            } catch (IllegalArgumentException | ClassCastException e) {
                LOG.warn("Failed to convert node entry: {}", nodeEntry.name(), e);
            }
        });
    }

    private boolean saveNodeInTopology(Node node, Uuid topologyUuid, String originalTopologyUuid) {
        try {
            final DataObjectIdentifier<Topology> topologyIID =
                DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topologyUuid))
                .build();
            Map<NameKey, Name> topoNameMap = new HashMap<>();
            topoNameMap.put(new NameKey(TAPI_TOPOLOGY_NAME),
                new NameBuilder().setValueName(TAPI_TOPOLOGY_NAME)
                    .setValue(SBI_TOPOLOGY_NAME_PREFIX + controllerId + SBI_TOPOLOGY_NAME_SUFFIX)
                    .build());
            topoNameMap.put(new NameKey(ORIGINAL_UUID_NAME), new NameBuilder()
                    .setValueName(ORIGINAL_UUID_NAME)
                    .setValue(originalTopologyUuid).build());
            topoNameMap.put(new NameKey(CONTROLLER_ID_NAME), new NameBuilder()
                    .setValueName(CONTROLLER_ID_NAME)
                    .setValue(controllerId).build());
            Topology topologyShell = new TopologyBuilder().setUuid(topologyUuid)
                    .setLayerProtocolName(node.getLayerProtocolName())
                    .setName(topoNameMap).build();
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topologyIID, topologyShell);
            DataObjectIdentifier<Node> nodeIID = DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topologyUuid))
                .child(Node.class, node.key())
                .build();
            this.networkTransactionService.put(LogicalDatastoreType.OPERATIONAL, nodeIID, node);
            this.networkTransactionService.commit().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Failed to store node {} in topology {} for controller {}",
                    node.getUuid().getValue(), topologyUuid.getValue(), controllerId, e);
            return false;
        }
    }

    /**
     * Transforms a node by processing mapped-service-interface-point and profile.
     *
     * @param originalNode the original node from bottom controller
     * @return transformed node
     */
    private Node transformNode(Node originalNode) {
        NodeBuilder nodeBuilder = new NodeBuilder(originalNode);

        if (originalNode.getOwnedNodeEdgePoint() != null) {
            LOG.debug("Transforming node {} with {} owned-node-edge-points",
                originalNode.getUuid().getValue(),
                originalNode.getOwnedNodeEdgePoint().size());

            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> transformedOnepMap = new HashMap<>();

            originalNode.getOwnedNodeEdgePoint().values().forEach(onep -> {
                OwnedNodeEdgePointBuilder onepBuilder = new OwnedNodeEdgePointBuilder(onep);

                boolean transformed = false;

                if (onep.getMappedServiceInterfacePoint() != null && !onep.getMappedServiceInterfacePoint().isEmpty()) {
                    LOG.debug("ONEP {} has {} mapped-service-interface-points to transform",
                        onep.getUuid().getValue(),
                        onep.getMappedServiceInterfacePoint().size());
                    Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> transformedMsip =
                        transformMappedServiceInterfacePoint(onep.getMappedServiceInterfacePoint());
                    onepBuilder.setMappedServiceInterfacePoint(transformedMsip);
                    transformed = true;
                }

                if (onep.getProfile() != null && !onep.getProfile().isEmpty()) {
                    LOG.debug("ONEP {} has {} profiles to transform",
                        onep.getUuid().getValue(),
                        onep.getProfile().size());
                    Map<ProfileKey, Profile> transformedProfile =
                        transformProfile(onep.getProfile());
                    onepBuilder.setProfile(transformedProfile);
                    transformed = true;
                }

                if (onep.getSourceProfile() != null && !onep.getSourceProfile().isEmpty()) {
                    LOG.debug("ONEP {} has {} source-profiles to transform",
                        onep.getUuid().getValue(),
                        onep.getSourceProfile().size());
                    Map<SourceProfileKey, SourceProfile> transformedSourceProfile =
                        transformSourceProfile(onep.getSourceProfile());
                    onepBuilder.setSourceProfile(transformedSourceProfile);
                    transformed = true;
                }

                if (onep.getSinkProfile() != null && !onep.getSinkProfile().isEmpty()) {
                    LOG.debug("ONEP {} has {} sink-profiles to transform",
                        onep.getUuid().getValue(),
                        onep.getSinkProfile().size());
                    Map<SinkProfileKey, SinkProfile> transformedSinkProfile =
                        transformSinkProfile(onep.getSinkProfile());
                    onepBuilder.setSinkProfile(transformedSinkProfile);
                    transformed = true;
                }

                if (transformed) {
                    LOG.debug("ONEP {} transformation completed", onep.getUuid().getValue());
                }

                OwnedNodeEdgePoint transformedOnep = onepBuilder.build();
                transformedOnepMap.put(transformedOnep.key(), transformedOnep);
            });

            nodeBuilder.setOwnedNodeEdgePoint(transformedOnepMap);
            LOG.debug("Node {} transformation completed with {} transformed owned-node-edge-points",
                originalNode.getUuid().getValue(),
                transformedOnepMap.size());
        }

        return nodeBuilder.build();
    }

    /**
     * Transforms mapped-service-interface-point from bottom controller.
     * Maps SIP UUIDs from bottom controller to top controller SBI topology.
     *
     * @param msipMap the mapped service interface point map
     * @return transformed mapped service interface point map
     */
    private Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint>
            transformMappedServiceInterfacePoint(
                Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipMap) {
        if (msipMap == null || msipMap.isEmpty()) {
            LOG.debug("No mapped-service-interface-point to transform");
            return msipMap;
        }

        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> transformedMsipMap = new HashMap<>();

        msipMap.values().forEach(msip -> {
            Uuid originalSipUuid = msip.getServiceInterfacePointUuid();
            Uuid transformedSipUuid = generateTransformedUuid(originalSipUuid.getValue(), controllerId);

            MappedServiceInterfacePoint transformedMsip = new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(transformedSipUuid)
                .build();

            transformedMsipMap.put(transformedMsip.key(), transformedMsip);
            LOG.debug("Transformed SIP UUID from {} to {}", originalSipUuid.getValue(),
                transformedSipUuid.getValue());
        });

        return transformedMsipMap;
    }

    /**
     * Transforms profile from bottom controller.
     * Maps profile UUIDs from bottom controller to top controller SBI topology.
     *
     * @param profileMap the profile map
     * @return transformed profile map
     */
    private Map<ProfileKey, Profile> transformProfile(Map<ProfileKey, Profile> profileMap) {
        if (profileMap == null || profileMap.isEmpty()) {
            LOG.debug("No profile to transform");
            return profileMap;
        }

        Map<ProfileKey, Profile> transformedProfileMap = new HashMap<>();

        profileMap.values().forEach(profile -> {
            Uuid originalProfileUuid = profile.getProfileUuid();
            Uuid transformedProfileUuid = generateTransformedUuid(originalProfileUuid.getValue(), controllerId);

            Profile transformedProfile = new ProfileBuilder()
                .setProfileUuid(transformedProfileUuid)
                .build();

            transformedProfileMap.put(transformedProfile.key(), transformedProfile);
            LOG.debug("Transformed Profile UUID from {} to {}", originalProfileUuid.getValue(),
                transformedProfileUuid.getValue());
        });

        return transformedProfileMap;
    }

    /**
     * Transforms source-profile from bottom controller.
     * Maps source-profile UUIDs from bottom controller to top controller SBI topology.
     *
     * @param sourceProfileMap the source-profile map
     * @return transformed source-profile map
     */
    private Map<SourceProfileKey, SourceProfile> transformSourceProfile(
            Map<SourceProfileKey, SourceProfile> sourceProfileMap) {
        if (sourceProfileMap == null || sourceProfileMap.isEmpty()) {
            LOG.debug("No source-profile to transform");
            return sourceProfileMap;
        }

        Map<SourceProfileKey, SourceProfile> transformedSourceProfileMap = new HashMap<>();

        sourceProfileMap.values().forEach(sourceProfile -> {
            Uuid originalProfileUuid = sourceProfile.getProfileUuid();
            Uuid transformedProfileUuid = generateTransformedUuid(originalProfileUuid.getValue(), controllerId);

            SourceProfile transformedSourceProfile = new SourceProfileBuilder()
                .setProfileUuid(transformedProfileUuid)
                .build();

            transformedSourceProfileMap.put(transformedSourceProfile.key(), transformedSourceProfile);
            LOG.debug("Transformed SourceProfile UUID from {} to {}", originalProfileUuid.getValue(),
                transformedProfileUuid.getValue());
        });

        return transformedSourceProfileMap;
    }

    /**
     * Transforms sink-profile from bottom controller.
     * Maps sink-profile UUIDs from bottom controller to top controller SBI topology.
     *
     * @param sinkProfileMap the sink-profile map
     * @return transformed sink-profile map
     */
    private Map<SinkProfileKey, SinkProfile> transformSinkProfile(
            Map<SinkProfileKey, SinkProfile> sinkProfileMap) {
        if (sinkProfileMap == null || sinkProfileMap.isEmpty()) {
            LOG.debug("No sink-profile to transform");
            return sinkProfileMap;
        }

        Map<SinkProfileKey, SinkProfile> transformedSinkProfileMap = new HashMap<>();

        sinkProfileMap.values().forEach(sinkProfile -> {
            Uuid originalProfileUuid = sinkProfile.getProfileUuid();
            Uuid transformedProfileUuid = generateTransformedUuid(originalProfileUuid.getValue(), controllerId);

            SinkProfile transformedSinkProfile = new SinkProfileBuilder()
                .setProfileUuid(transformedProfileUuid)
                .build();

            transformedSinkProfileMap.put(transformedSinkProfile.key(), transformedSinkProfile);
            LOG.debug("Transformed SinkProfile UUID from {} to {}", originalProfileUuid.getValue(),
                transformedProfileUuid.getValue());
        });

        return transformedSinkProfileMap;
    }


    /**
     * Generates a new UUID based on original UUID and controller ID.
     * This common method is used for all UUID transformations (topology, SIP, profile, etc.).
     *
     * @param originalUuid the original UUID string
     * @param ctrlId the controller ID
     * @return new transformed UUID
     */
    private Uuid generateTransformedUuid(String originalUuid, String ctrlId) {
        String combinedString = originalUuid + ctrlId;
        String newUuid = UUID.nameUUIDFromBytes(combinedString.getBytes(StandardCharsets.UTF_8)).toString();
        return new Uuid(newUuid);
    }

    @Override
    public ListenableFuture<Void> updateLinkInSbiTopology(String linkJson, String sbiTopologyUuid) {
        try {
            transformLinkJson(linkJson, sbiTopologyUuid);
            return Futures.immediateFuture(null);
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Failed to transform link JSON for topology {}",
                sbiTopologyUuid, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * Transforms link JSON to NormalizedNode and stores in MDSAL.
     *
     * @param linkJson the link JSON string
     * @param sbiTopologyUuidStr the SBI topology UUID string
     * @throws IOException if transformation fails
     * @throws IllegalArgumentException if invalid arguments
     */
    private void transformLinkJson(String linkJson, String sbiTopologyUuidStr)
            throws IOException, IllegalArgumentException {
        EffectiveModelContext modelContext = getModelContext();
        var stack = SchemaInferenceStack.of(modelContext);
        stack.enterSchemaTree(Context.QNAME);
        stack.enterSchemaTree(QName.create(
            "urn:onf:otcc:yang:tapi-topology", "2022-11-21",
            "topology-context"));
        stack.enterSchemaTree(QName.create(
            "urn:onf:otcc:yang:tapi-topology", "2022-11-21", "topology"));
        Inference topologyInference = stack.toInference();
        LOG.debug("Created SchemaInferenceStack for link transformation with parent: Topology");

        try (JsonReader reader = new JsonReader(new StringReader(linkJson))) {

            NormalizationResultHolder result = new NormalizationResultHolder();
            try (NormalizedNodeStreamWriter streamWriter =
                    ImmutableNormalizedNodeStreamWriter.from(result);
                 JsonParserStream jsonParser = JsonParserStream.create(
                     streamWriter,
                     JSONCodecFactorySupplier.RFC7951.getShared(modelContext),
                     topologyInference)) {
                jsonParser.parse(reader);
            }
            LOG.debug("Link JSON parsing completed successfully (direct, no wrapping)");

            NormalizedNode normalizedNode = result.getResult().data();
            if (normalizedNode == null) {
                LOG.error("Normalized link node is null after parsing");
                return;
            }

            LOG.debug("Normalized link node created - Type: {}, Name: {}",
                normalizedNode.getClass().getSimpleName(),
                normalizedNode.name());

            QName topologyQName = QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "topology");
            QName linkQName = QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "link");
            QName uuidQName = QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "uuid");

            if (normalizedNode instanceof MapNode mapNode) {
                LOG.debug("Processing MapNode with {} link entries", mapNode.size());
                processLinkMapNodeEntries(mapNode, topologyQName, linkQName, uuidQName, sbiTopologyUuidStr);
            }
        }
    }

    /**
     * Processes link MapNode entries and stores them directly.
     *
     * @param mapNode the MapNode containing link entries
     * @param topologyQName the topology QName
     * @param linkQName the link QName
     * @param uuidQName the uuid QName
     * @param sbiTopologyUuidStr the SBI topology UUID string
     */
    private void processLinkMapNodeEntries(MapNode mapNode, QName topologyQName,
            QName linkQName, QName uuidQName, String sbiTopologyUuidStr) {
        final BindingDOMCodecServices codecServices = this.bindingDOMCodecServices;
        final Uuid newTopologyUuid = generateTransformedUuid(sbiTopologyUuidStr, controllerId);
        LOG.debug("Generated new topology UUID: {} from original: {} and controller: {}",
            newTopologyUuid.getValue(), sbiTopologyUuidStr, controllerId);

        mapNode.body().forEach(linkEntry -> {
            LOG.debug("Processing link entry: {}", linkEntry.name());
            try {
                var linkPath = org.opendaylight.yangtools.yang.data.api
                        .YangInstanceIdentifier.builder()
                    .node(Context.QNAME)
                    .node(QName.create("urn:onf:otcc:yang:tapi-topology", "2022-11-21", "topology-context"))
                    .node(topologyQName)
                    .nodeWithKey(topologyQName, uuidQName, sbiTopologyUuidStr)
                    .node(linkQName)
                    .node(linkEntry.name())
                    .build();

                var bindingEntry = codecServices.fromNormalizedNode(linkPath, linkEntry);
                if (bindingEntry != null && bindingEntry.getValue() instanceof Link link) {
                    LOG.debug("Parsed link UUID: {}", link.getUuid().getValue());
                    Link transformedLink = transformLink(link, newTopologyUuid);
                    if (saveLinkInTopology(transformedLink, newTopologyUuid, sbiTopologyUuidStr)) {
                        LOG.debug("Stored link {} in topology {} for controller {}",
                                link.getUuid().getValue(), newTopologyUuid.getValue(), controllerId);
                    }
                }
            } catch (IllegalArgumentException | ClassCastException e) {
                LOG.warn("Failed to convert link entry: {}", linkEntry.name(), e);
            }
        });
        LOG.debug("Completed processing all link entries for controller {}", controllerId);
    }

    /**
     * Transforms a link by rewriting the topology-uuid in each
     * node-edge-point reference to the new SBI topology UUID.
     * Node-uuid and node-edge-point-uuid remain unchanged.
     *
     * @param originalLink the original link from bottom controller
     * @param newTopologyUuid the transformed SBI topology UUID
     * @return transformed link with updated endpoint topology references
     */
    private Link transformLink(Link originalLink, Uuid newTopologyUuid) {
        if (originalLink.getNodeEdgePoint() == null
                || originalLink.getNodeEdgePoint().isEmpty()) {
            return originalLink;
        }

        Map<NodeEdgePointKey, NodeEdgePoint> transformedNeps = new HashMap<>();
        originalLink.getNodeEdgePoint().values().forEach(nep -> {
            NodeEdgePointKey newKey = new NodeEdgePointKey(
                    nep.getNodeEdgePointUuid(), nep.getNodeUuid(), newTopologyUuid);
            NodeEdgePoint transformedNep = new NodeEdgePointBuilder(nep)
                .withKey(newKey)
                .setTopologyUuid(newTopologyUuid)
                .build();
            transformedNeps.put(transformedNep.key(), transformedNep);
            LOG.debug("Link {} NEP: rewrote topology-uuid from {} to {}",
                    originalLink.getUuid().getValue(),
                    nep.getTopologyUuid().getValue(),
                    newTopologyUuid.getValue());
        });

        return new LinkBuilder(originalLink)
            .setNodeEdgePoint(transformedNeps)
            .build();
    }

    private boolean saveLinkInTopology(Link link, Uuid topologyUuid, String originalTopologyUuid) {
        try {
            final DataObjectIdentifier<Topology> topologyIID =
                DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topologyUuid))
                .build();
            Map<NameKey, Name> topoNameMap = new HashMap<>();
            topoNameMap.put(new NameKey(TAPI_TOPOLOGY_NAME),
                new NameBuilder().setValueName(TAPI_TOPOLOGY_NAME)
                    .setValue(SBI_TOPOLOGY_NAME_PREFIX + controllerId + SBI_TOPOLOGY_NAME_SUFFIX)
                    .build());
            topoNameMap.put(new NameKey(ORIGINAL_UUID_NAME),
                new NameBuilder().setValueName(ORIGINAL_UUID_NAME)
                    .setValue(originalTopologyUuid).build());
            topoNameMap.put(new NameKey(CONTROLLER_ID_NAME),
                new NameBuilder().setValueName(CONTROLLER_ID_NAME)
                    .setValue(controllerId).build());
            Topology topologyShell = new TopologyBuilder().setUuid(topologyUuid)
                    .setLayerProtocolName(link.getLayerProtocolName())
                    .setName(topoNameMap).build();
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL,
                    topologyIID, topologyShell);
            DataObjectIdentifier<Link> linkIID = DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topologyUuid))
                .child(Link.class, link.key())
                .build();
            this.networkTransactionService.put(LogicalDatastoreType.OPERATIONAL, linkIID, link);
            this.networkTransactionService.commit().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Failed to store link {} in topology {} for controller {}",
                    link.getUuid().getValue(), topologyUuid.getValue(), controllerId, e);
            return false;
        }
    }


    @Override
    public ListenableFuture<Void> updateProfileInContext(String profileJson) {
        try {
            transformProfileJson(profileJson);
            return Futures.immediateFuture(null);
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Failed to transform profile JSON for controller {}", controllerId, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * Transforms profile JSON to NormalizedNode, transforms UUID, adds name entries,
     * and merges into the common context datastore.
     *
     * @param profileJson the profile JSON string (single profile from bottom controller)
     * @throws IOException if JSON parsing fails
     * @throws IllegalArgumentException if invalid arguments
     */
    private void transformProfileJson(String profileJson)
            throws IOException, IllegalArgumentException {
        LOG.debug("Starting profile transformation for controller: {}", controllerId);

        EffectiveModelContext modelContext = getModelContext();
        var stack = SchemaInferenceStack.of(modelContext);
        stack.enterSchemaTree(Context.QNAME);
        Inference contextInference = stack.toInference();
        LOG.debug("Created SchemaInferenceStack for profile transformation with parent: Context");

        try (JsonReader reader = new JsonReader(new StringReader(profileJson))) {
            NormalizationResultHolder result = new NormalizationResultHolder();
            try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
                 JsonParserStream jsonParser = JsonParserStream.create(
                     streamWriter,
                     JSONCodecFactorySupplier.RFC7951.getShared(modelContext),
                     contextInference)) {
                jsonParser.parse(reader);
            }

            NormalizedNode normalizedNode = result.getResult().data();
            if (normalizedNode == null) {
                LOG.error("Normalized profile node is null after parsing");
                return;
            }

            LOG.debug("Profile normalized node created - Type: {}, Name: {}",
                normalizedNode.getClass().getSimpleName(), normalizedNode.name());

            QName contextQName = QName.create("urn:onf:otcc:yang:tapi-common", "2022-11-21", "profile");

            if (normalizedNode instanceof MapNode mapNode) {
                LOG.debug("Processing profile MapNode with {} entries", mapNode.size());
                processProfileMapNodeEntries(mapNode, contextQName);
            }
        }
    }

    /**
     * Processes profile MapNode entries, transforms each profile UUID and name,
     * then merges into the common context.
     */
    private void processProfileMapNodeEntries(MapNode mapNode, QName contextQName) {
        final BindingDOMCodecServices codecServices = this.bindingDOMCodecServices;
        mapNode.body().forEach(profileEntry -> {
            LOG.debug("Processing profile entry: {}", profileEntry.name());
            try {
                var profilePath = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder()
                    .node(Context.QNAME)
                    .node(contextQName)
                    .node(profileEntry.name())
                    .build();

                var bindingEntry = codecServices.fromNormalizedNode(profilePath, profileEntry);
                if (bindingEntry != null
                        && bindingEntry.getValue()
                            instanceof org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                                .rev221121.tapi.context.Profile originalProfile) {
                    String originalUuid = originalProfile.getUuid().getValue();
                    final Uuid newUuid = generateTransformedUuid(originalUuid, controllerId);

                    Map<NameKey, Name> nameMap = new HashMap<>();
                    if (originalProfile.getName() != null) {
                        nameMap.putAll(originalProfile.getName());
                    }
                    nameMap.put(new NameKey(CONTROLLER_ID_NAME),
                        new NameBuilder()
                            .setValueName(CONTROLLER_ID_NAME)
                            .setValue(controllerId)
                            .build());
                    nameMap.put(new NameKey(ORIGINAL_UUID_NAME),
                        new NameBuilder()
                            .setValueName(ORIGINAL_UUID_NAME)
                            .setValue(originalUuid)
                            .build());

                    var transformedProfile =
                        new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                                .rev221121.tapi.context.ProfileBuilder(originalProfile)
                            .withKey(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                                .rev221121.tapi.context.ProfileKey(newUuid))
                            .setUuid(newUuid)
                            .setName(nameMap)
                            .build();

                    LOG.debug("Transformed profile UUID from {} to {}",
                            originalUuid, newUuid.getValue());
                    if (saveProfileInContext(transformedProfile)) {
                        LOG.debug("Stored profile {} in context for controller {}",
                                transformedProfile.getUuid().getValue(), controllerId);
                    }
                }
            } catch (IllegalArgumentException | ClassCastException e) {
                LOG.warn("Failed to convert profile entry: {}", profileEntry.name(), e);
            }
        });
    }

    private boolean saveProfileInContext(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                .tapi.context.Profile profile) {
        try {
            var iid = DataObjectIdentifier.builder(Context.class)
                    .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                            .rev221121.tapi.context.Profile.class,
                        profile.key())
                    .build();
            this.networkTransactionService.put(LogicalDatastoreType.OPERATIONAL, iid, profile);
            this.networkTransactionService.commit().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Failed to store profile {} in context for controller {}",
                    profile.getUuid().getValue(), controllerId, e);
            return false;
        }
    }

    @Override
    public ListenableFuture<Void> updateServiceInterfacePointInContext(String sipJson) {
        try {
            transformSipJson(sipJson);
            return Futures.immediateFuture(null);
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Failed to transform SIP JSON for controller {}", controllerId, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * Transforms service interface point JSON to NormalizedNode, transforms UUID,
     * adds name entries, and merges into the common context datastore.
     *
     * @param sipJson the SIP JSON string (single SIP from bottom controller)
     * @throws IOException if JSON parsing fails
     * @throws IllegalArgumentException if invalid arguments
     */
    private void transformSipJson(String sipJson)
            throws IOException, IllegalArgumentException {
        LOG.debug("Starting SIP transformation for controller: {}", controllerId);

        EffectiveModelContext modelContext = getModelContext();
        var stack = SchemaInferenceStack.of(modelContext);
        stack.enterSchemaTree(Context.QNAME);
        Inference contextInference = stack.toInference();
        LOG.debug("Created SchemaInferenceStack for SIP transformation with parent: Context");

        try (JsonReader reader = new JsonReader(new StringReader(sipJson))) {
            NormalizationResultHolder result = new NormalizationResultHolder();
            try (NormalizedNodeStreamWriter streamWriter =
                    ImmutableNormalizedNodeStreamWriter.from(result);
                 JsonParserStream jsonParser = JsonParserStream.create(
                     streamWriter,
                     JSONCodecFactorySupplier.RFC7951.getShared(modelContext),
                     contextInference)) {
                jsonParser.parse(reader);
            }

            NormalizedNode normalizedNode = result.getResult().data();
            if (normalizedNode == null) {
                LOG.error("Normalized SIP node is null after parsing");
                return;
            }

            LOG.debug("SIP normalized node created - Type: {}, Name: {}",
                normalizedNode.getClass().getSimpleName(), normalizedNode.name());

            QName sipQName = QName.create("urn:onf:otcc:yang:tapi-common", "2022-11-21", "service-interface-point");

            if (normalizedNode instanceof MapNode mapNode) {
                LOG.debug("Processing SIP MapNode with {} entries", mapNode.size());
                processSipMapNodeEntries(mapNode, sipQName);
            }
        }
    }

    /**
     * Processes SIP MapNode entries, transforms each SIP UUID and name,
     * then merges into the common context.
     */
    private void processSipMapNodeEntries(MapNode mapNode, QName sipQName) {
        final BindingDOMCodecServices codecServices = this.bindingDOMCodecServices;
        mapNode.body().forEach(sipEntry -> {
            LOG.debug("Processing SIP entry: {}", sipEntry.name());
            try {
                var sipPath = org.opendaylight.yangtools.yang.data.api
                        .YangInstanceIdentifier.builder()
                    .node(Context.QNAME)
                    .node(sipQName)
                    .node(sipEntry.name())
                    .build();

                var bindingEntry = codecServices.fromNormalizedNode(sipPath, sipEntry);
                if (bindingEntry != null
                        && bindingEntry.getValue() instanceof ServiceInterfacePoint originalSip) {
                    String originalUuid = originalSip.getUuid().getValue();
                    final Uuid newUuid = generateTransformedUuid(originalUuid, controllerId);

                    Map<NameKey, Name> nameMap = new HashMap<>();
                    if (originalSip.getName() != null) {
                        nameMap.putAll(originalSip.getName());
                    }
                    nameMap.put(new NameKey(CONTROLLER_ID_NAME),
                        new NameBuilder()
                            .setValueName(CONTROLLER_ID_NAME)
                            .setValue(controllerId)
                            .build());
                    nameMap.put(new NameKey(ORIGINAL_UUID_NAME),
                        new NameBuilder()
                            .setValueName(ORIGINAL_UUID_NAME)
                            .setValue(originalUuid)
                            .build());

                    var transformedSip = new ServiceInterfacePointBuilder(originalSip)
                            .withKey(new ServiceInterfacePointKey(newUuid))
                            .setUuid(newUuid)
                            .setName(nameMap)
                            .build();

                    LOG.debug("Transformed SIP UUID from {} to {}",
                            originalUuid, newUuid.getValue());
                    if (saveSipInContext(transformedSip)) {
                        LOG.debug("Stored SIP {} in context for controller {}",
                                transformedSip.getUuid().getValue(), controllerId);
                    }
                }
            } catch (IllegalArgumentException | ClassCastException e) {
                LOG.warn("Failed to convert SIP entry: {}", sipEntry.name(), e);
            }
        });
    }

    private boolean saveSipInContext(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                .tapi.context.ServiceInterfacePoint sip) {
        try {
            var iid = DataObjectIdentifier.builder(Context.class)
                    .child(ServiceInterfacePoint.class, sip.key())
                    .build();
            this.networkTransactionService.put(
                LogicalDatastoreType.OPERATIONAL, iid, sip);
            this.networkTransactionService.commit().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.error("Failed to store SIP {} in context for controller {}",
                    sip.getUuid().getValue(), controllerId, e);
            return false;
        }
    }

    @Override
    public ListenableFuture<Void> cleanupTopology(String ctrlId) {
        LOG.info("Cleaning up SBI topology for controller: {}", ctrlId);
        try {
            DataObjectIdentifier<TopologyContext> topologyContextIID =
                DataObjectIdentifier.builder(Context.class)
                    .augmentation(Context1.class)
                    .child(TopologyContext.class)
                    .build();

            Optional<TopologyContext> topologyContextOpt =
                this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL, topologyContextIID).get();

            if (topologyContextOpt.isEmpty()) {
                LOG.info("No topology context found, nothing to clean for controller: {}",
                    ctrlId);
                return Futures.immediateFuture(null);
            }

            Map<TopologyKey, Topology> topologies =
                topologyContextOpt.orElseThrow().getTopology();
            if (topologies == null || topologies.isEmpty()) {
                LOG.info("No topologies found, nothing to clean for controller: {}",
                    ctrlId);
                return Futures.immediateFuture(null);
            }

            boolean deleted = false;
            for (Topology topology : topologies.values()) {
                if (hasControllerIdName(topology.getName(), ctrlId)) {
                    LOG.info("Deleting topology {} for controller: {}", topology.getUuid().getValue(), ctrlId);
                    DataObjectIdentifier<Topology> topologyIID =
                        DataObjectIdentifier.builder(Context.class)
                            .augmentation(Context1.class)
                            .child(TopologyContext.class)
                            .child(Topology.class, topology.key())
                            .build();
                    this.networkTransactionService.delete(
                        LogicalDatastoreType.OPERATIONAL, topologyIID);
                    deleted = true;
                }
            }

            if (deleted) {
                this.networkTransactionService.commit().get();
                LOG.info("Topology cleanup completed for controller: {}", ctrlId);
            } else {
                LOG.info("No topology found for controller: {}, nothing to delete", ctrlId);
            }
            return Futures.immediateFuture(null);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to cleanup topology for controller: {}", ctrlId, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public ListenableFuture<Void> cleanupProfiles(String ctrlId) {
        LOG.info("Cleaning up profiles for controller: {}", ctrlId);
        try {
            var contextIID = DataObjectIdentifier.builder(Context.class).build();

            var contextOpt =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, contextIID).get();

            if (contextOpt.isEmpty()) {
                LOG.info("No context found, nothing to clean for controller: {}", ctrlId);
                return Futures.immediateFuture(null);
            }

            var profiles = contextOpt.orElseThrow().getProfile();
            if (profiles == null || profiles.isEmpty()) {
                LOG.info("No profiles found, nothing to clean for controller: {}", ctrlId);
                return Futures.immediateFuture(null);
            }

            boolean deleted = false;
            for (var profile : profiles.values()) {
                if (hasControllerIdName(profile.getName(), ctrlId)) {
                    LOG.info("Deleting profile {} for controller: {}", profile.getUuid().getValue(), ctrlId);
                    var profileIID = DataObjectIdentifier.builder(Context.class)
                            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                                    .rev221121.tapi.context.Profile.class,
                                profile.key())
                            .build();
                    this.networkTransactionService.delete(
                        LogicalDatastoreType.OPERATIONAL, profileIID);
                    deleted = true;
                }
            }

            if (deleted) {
                this.networkTransactionService.commit().get();
                LOG.info("Profile cleanup completed for controller: {}", ctrlId);
            } else {
                LOG.info("No profiles found for controller: {}, nothing to delete", ctrlId);
            }
            return Futures.immediateFuture(null);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to cleanup profiles for controller: {}", ctrlId, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public ListenableFuture<Void> cleanupServiceInterfacePoints(String ctrlId) {
        LOG.info("Cleaning up service interface points for controller: {}", ctrlId);
        try {
            var contextIID = DataObjectIdentifier.builder(Context.class).build();

            var contextOpt = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL, contextIID).get();

            if (contextOpt.isEmpty()) {
                LOG.info("No context found, nothing to clean for controller: {}", ctrlId);
                return Futures.immediateFuture(null);
            }

            var sips = contextOpt.orElseThrow().getServiceInterfacePoint();
            if (sips == null || sips.isEmpty()) {
                LOG.info("No SIPs found, nothing to clean for controller: {}",
                    ctrlId);
                return Futures.immediateFuture(null);
            }

            boolean deleted = false;
            for (var sip : sips.values()) {
                if (hasControllerIdName(sip.getName(), ctrlId)) {
                    LOG.info("Deleting SIP {} for controller: {}",
                        sip.getUuid().getValue(), ctrlId);
                    var sipIID = DataObjectIdentifier.builder(Context.class)
                            .child(ServiceInterfacePoint.class, sip.key())
                            .build();
                    this.networkTransactionService.delete(
                        LogicalDatastoreType.OPERATIONAL, sipIID);
                    deleted = true;
                }
            }

            if (deleted) {
                this.networkTransactionService.commit().get();
                LOG.info("SIP cleanup completed for controller: {}", ctrlId);
            } else {
                LOG.info("No SIPs found for controller: {}, nothing to delete",
                    ctrlId);
            }
            return Futures.immediateFuture(null);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to cleanup SIPs for controller: {}", ctrlId, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * Checks whether a name map contains a {@code controller-id} name entry
     * matching the given controller identifier.
     *
     * @param nameMap the name map to inspect (may be {@code null})
     * @param ctrlId the controller identifier to match
     * @return {@code true} if a matching controller-id name entry exists
     */
    private boolean hasControllerIdName(Map<NameKey, Name> nameMap, String ctrlId) {
        if (nameMap == null) {
            return false;
        }
        Name controllerName = nameMap.get(new NameKey(CONTROLLER_ID_NAME));
        return controllerName != null && ctrlId.equals(controllerName.getValue());
    }
}
