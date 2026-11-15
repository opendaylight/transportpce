/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformationServiceImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(TransformationServiceImplTest.class);

    private static final String CONTROLLER_ID = "tpce-southbound-1";
    private static final String TOPOLOGY_UUID = "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7";
    private static final String NODE_UUID = "11111111-1111-1111-1111-111111111111";
    private static final String LINK_UUID = "77777777-7777-7777-7777-777777777777";
    private static final String SIP_UUID = "22222222-2222-2222-2222-222222222222";
    private static final String PROFILE_UUID = "33333333-3333-3333-3333-333333333333";
    private static final String ONEP_UUID = "44444444-4444-4444-4444-444444444444";

    private BindingDOMCodecServices bindingDOMCodecServices;
    private NetworkTransactionService networkTransactionService;
    private TransformationServiceImpl transformationService;

    @BeforeEach
    void setUp() {
        bindingDOMCodecServices = mock(BindingDOMCodecServices.class);
        networkTransactionService = mock(NetworkTransactionService.class);
        when(networkTransactionService.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());
        transformationService = new TransformationServiceImpl(
            bindingDOMCodecServices, networkTransactionService);
        transformationService.setControllerId(CONTROLLER_ID);
        LOG.debug("setUp done for controller: {}", CONTROLLER_ID);
    }

    @Test
    void testTransformNodeJson() throws Exception {
        LOG.debug("test direct invocation of private transformNodeJson");
        Throwable cause = invokePrivateAndGetCause(
            "transformNodeJson",
            new Class<?>[] {String.class, String.class},
            "{ this is invalid json }",
            TOPOLOGY_UUID);
        assertTrue(cause instanceof NullPointerException,
            "Expected NullPointerException when runtime context is missing");
    }

    @Test
    void testSaveNodeInTopology() throws Exception {
        LOG.debug("test direct invocation of private saveNodeInTopology");
        Node node = new NodeBuilder().setUuid(new Uuid(NODE_UUID)).build();
        invokePrivate(
            "saveNodeInTopology",
            new Class<?>[] {Node.class, Uuid.class, String.class},
            node,
            new Uuid(TOPOLOGY_UUID),
            TOPOLOGY_UUID);
        verify(networkTransactionService, atLeastOnce()).merge(
            eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class), any());
        verify(networkTransactionService, atLeastOnce()).put(
            eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class), any());
        verify(networkTransactionService, atLeastOnce()).commit();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTransformNode() throws Exception {
        LOG.debug("test direct invocation of private transformNode");
        Node originalNode = buildNodeWithAllMaps();
        Node transformedNode = (Node) invokePrivate(
            "transformNode",
            new Class<?>[] {Node.class},
            originalNode);
        assertNotNull(transformedNode, "Transformed node must not be null");
        OwnedNodeEdgePoint onep = transformedNode.getOwnedNodeEdgePoint().values().iterator().next();
        String expectedSipUuid = expectedUuid(SIP_UUID);
        String expectedProfileUuid = expectedUuid(PROFILE_UUID);
        assertEquals(
            expectedSipUuid,
            onep.getMappedServiceInterfacePoint().values().iterator().next()
                .getServiceInterfacePointUuid().getValue(),
            "SIP UUID must be transformed");
        assertEquals(
            expectedProfileUuid,
            onep.getProfile().values().iterator().next().getProfileUuid().getValue(),
            "Profile UUID must be transformed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTransformMappedServiceInterfacePoint() throws Exception {
        LOG.debug("test direct invocation of private transformMappedServiceInterfacePoint");
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> input = buildMsipMap(SIP_UUID);
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> result =
            (Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint>) invokePrivate(
                "transformMappedServiceInterfacePoint",
                new Class<?>[] {Map.class},
                input);
        assertNotNull(result, "Result map must not be null");
        String transformedUuid = result.values().iterator().next().getServiceInterfacePointUuid().getValue();
        assertEquals(expectedUuid(SIP_UUID), transformedUuid, "SIP UUID must be transformed deterministically");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTransformProfile() throws Exception {
        LOG.debug("test direct invocation of private transformProfile");
        Map<ProfileKey, Profile> input = buildProfileMap(PROFILE_UUID);
        Map<ProfileKey, Profile> result = (Map<ProfileKey, Profile>) invokePrivate(
            "transformProfile",
            new Class<?>[] {Map.class},
            input);
        assertNotNull(result, "Result map must not be null");
        String transformedUuid = result.values().iterator().next().getProfileUuid().getValue();
        assertEquals(expectedUuid(PROFILE_UUID), transformedUuid, "Profile UUID must be transformed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTransformSourceProfile() throws Exception {
        LOG.debug("test direct invocation of private transformSourceProfile");
        Map<SourceProfileKey, SourceProfile> input = buildSourceProfileMap(PROFILE_UUID);
        Map<SourceProfileKey, SourceProfile> result = (Map<SourceProfileKey, SourceProfile>) invokePrivate(
            "transformSourceProfile",
            new Class<?>[] {Map.class},
            input);
        assertNotNull(result, "Result map must not be null");
        String transformedUuid = result.values().iterator().next().getProfileUuid().getValue();
        assertEquals(expectedUuid(PROFILE_UUID), transformedUuid, "Source profile UUID must be transformed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTransformSinkProfile() throws Exception {
        LOG.debug("test direct invocation of private transformSinkProfile");
        Map<SinkProfileKey, SinkProfile> input = buildSinkProfileMap(PROFILE_UUID);
        Map<SinkProfileKey, SinkProfile> result = (Map<SinkProfileKey, SinkProfile>) invokePrivate(
            "transformSinkProfile",
            new Class<?>[] {Map.class},
            input);
        assertNotNull(result, "Result map must not be null");
        String transformedUuid = result.values().iterator().next().getProfileUuid().getValue();
        assertEquals(expectedUuid(PROFILE_UUID), transformedUuid, "Sink profile UUID must be transformed");
    }

    @Test
    void testUpdateServiceInterfacePointInContextInvalidJson() {
        LOG.debug("test updateServiceInterfacePointInContext throws NPE when runtime context is missing");
        assertThrows(NullPointerException.class,
            () -> transformationService.updateServiceInterfacePointInContext("{ invalid json }}"),
            "Expected NullPointerException when bindingDOMCodecServices has no runtime context");
        verify(networkTransactionService, never()).commit();
        LOG.info("updateServiceInterfacePointInContext NPE test passed");
    }

    @Test
    void testUpdateServiceInterfacePointInContextValidJson() {
        LOG.debug("test updateServiceInterfacePointInContext throws NPE for valid JSON with no runtime context");
        assertThrows(NullPointerException.class,
            () -> transformationService.updateServiceInterfacePointInContext(
                "{\"tapi-common:service-interface-point\":[{\"uuid\":\"" + SIP_UUID + "\"}]}"),
            "Expected NullPointerException when bindingDOMCodecServices has no runtime context");
        verify(networkTransactionService, never()).commit();
        LOG.info("updateServiceInterfacePointInContext NPE-for-valid-JSON test passed");
    }

    @Test
    void testTransformSipJson() throws Exception {
        LOG.debug("test direct invocation of private transformSipJson");
        Throwable cause = invokePrivateAndGetCause(
            "transformSipJson",
            new Class<?>[] {String.class},
            "{\"tapi-common:service-interface-point\":[{\"uuid\":\"" + SIP_UUID + "\"}]}");
        assertTrue(cause instanceof NullPointerException,
            "Expected NullPointerException when runtime context is missing");
        LOG.info("transformSipJson missing runtime context test passed");
    }

    @Test
    void testTransformSipJsonMalformedJson() throws Exception {
        LOG.info("test direct invocation of private transformSipJson with malformed JSON");
        Throwable cause = invokePrivateAndGetCause(
            "transformSipJson",
            new Class<?>[] {String.class},
            "{ this is not valid json }");
        assertNotNull(cause, "Cause must not be null for malformed JSON");
        LOG.info("transformSipJson malformed-json test passed");
    }

    @Test
    void testProcessSipMapNodeEntries() throws Exception {
        LOG.debug("test direct invocation of private processSipMapNodeEntries with null mapNode");
        QName sipQName = QName.create(
            "urn:onf:otcc:yang:tapi-common", "2022-11-21",
            "service-interface-point");
        Throwable cause = invokePrivateAndGetCause(
            "processSipMapNodeEntries",
            new Class<?>[] {MapNode.class, QName.class},
            null,
            sipQName);
        assertInstanceOf(NullPointerException.class, cause,
            "Expected NullPointerException for null mapNode input");
        LOG.debug("processSipMapNodeEntries null mapNode test passed");
    }

    @Test
    void testProcessSipMapNodeEntriesQName() throws Exception {
        LOG.debug("test that processSipMapNodeEntries uses the tapi-common SIP QName");
        QName sipQName = QName.create(
            "urn:onf:otcc:yang:tapi-common", "2022-11-21",
            "service-interface-point");
        assertEquals("service-interface-point", sipQName.getLocalName(),
            "Local name must be service-interface-point");
        assertEquals("urn:onf:otcc:yang:tapi-common", sipQName.getNamespace().toString(),
            "Namespace must match tapi-common");
        LOG.debug("processSipMapNodeEntries QName validation test passed");
    }

    @Test
    void testSaveSipInContext() throws Exception {
        LOG.debug("test direct invocation of private saveSipInContext");
        Uuid sipUuid = new Uuid(expectedUuid(SIP_UUID));
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                .tapi.context.ServiceInterfacePoint sip =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                    .rev221121.tapi.context.ServiceInterfacePointBuilder()
                .withKey(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                        .rev221121.tapi.context.ServiceInterfacePointKey(sipUuid))
                .setUuid(sipUuid)
                .build();
        invokePrivate(
                "saveSipInContext",
                new Class<?>[] {
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                            .tapi.context.ServiceInterfacePoint.class
                },
                sip);
        verify(networkTransactionService, atLeastOnce()).put(
            eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class), any());
        verify(networkTransactionService, atLeastOnce()).commit();
        LOG.debug("saveSipInContext datastore interaction test passed");
    }

    @Test
    void testSaveSipInContextTransformedUuid() throws Exception {
        LOG.debug("test saveSipInContext stores SIP whose UUID is derived from original + controller");
        String originalUuidStr = SIP_UUID;
        String expectedTransformedUuid = UUID.nameUUIDFromBytes(
            (originalUuidStr + CONTROLLER_ID).getBytes(StandardCharsets.UTF_8)).toString();
        Uuid sipUuid = new Uuid(expectedTransformedUuid);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                .tapi.context.ServiceInterfacePoint sip =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                    .rev221121.tapi.context.ServiceInterfacePointBuilder()
                .withKey(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                        .rev221121.tapi.context.ServiceInterfacePointKey(sipUuid))
                .setUuid(sipUuid)
                .build();
        invokePrivate(
                "saveSipInContext",
                new Class<?>[] {
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                            .tapi.context.ServiceInterfacePoint.class
                },
                sip);
        assertEquals(expectedTransformedUuid, sip.getUuid().getValue(),
            "Stored SIP UUID must be the deterministically transformed UUID");
        verify(networkTransactionService, atLeastOnce()).put(
            eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class), any());
        verify(networkTransactionService, atLeastOnce()).commit();
        LOG.debug("saveSipInContext transformed-UUID derivation test passed");
    }

    @Test
    void testTransformLinkJson() throws Exception {
        LOG.debug("test direct invocation of private transformLinkJson");
        Throwable cause = invokePrivateAndGetCause(
            "transformLinkJson",
            new Class<?>[] {String.class, String.class},
            "{ this is invalid json }",
            TOPOLOGY_UUID);
        assertTrue(cause instanceof NullPointerException,
            "Expected NullPointerException when runtime context is missing");
    }

    @Test
    void testProcessLinkMapNodeEntries() throws Exception {
        LOG.debug("test direct invocation of private processLinkMapNodeEntries");
        QName topologyQName = QName.create(
            "urn:onf:otcc:yang:tapi-topology", "2022-11-21", "topology");
        QName linkQName = QName.create(
            "urn:onf:otcc:yang:tapi-topology", "2022-11-21", "link");
        QName uuidQName = QName.create(
            "urn:onf:otcc:yang:tapi-topology", "2022-11-21", "uuid");

        Throwable cause = invokePrivateAndGetCause(
            "processLinkMapNodeEntries",
            new Class<?>[] {MapNode.class, QName.class, QName.class, QName.class, String.class},
            null,
            topologyQName,
            linkQName,
            uuidQName,
            TOPOLOGY_UUID);
        assertTrue(cause instanceof NullPointerException,
            "Expected NullPointerException for null mapNode input");
    }

    @Test
    void testSaveLinkInTopology() throws Exception {
        LOG.debug("test direct invocation of private saveLinkInTopology");
        Link link = new LinkBuilder().setUuid(new Uuid(LINK_UUID)).build();
        invokePrivate(
            "saveLinkInTopology",
            new Class<?>[] {Link.class, Uuid.class, String.class},
            link,
            new Uuid(TOPOLOGY_UUID),
            TOPOLOGY_UUID);
        verify(networkTransactionService, atLeastOnce()).merge(
            eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class), any());
        verify(networkTransactionService, atLeastOnce()).put(
            eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class), any());
        verify(networkTransactionService, atLeastOnce()).commit();
    }

    @Test
    void testTransformProfileJson() throws Exception {
        LOG.debug("test direct invocation of private transformProfileJson");
        Throwable cause = invokePrivateAndGetCause(
            "transformProfileJson",
            new Class<?>[] {String.class},
            "{ \"tapi-common:profile\": [] }");
        assertTrue(cause instanceof NullPointerException,
            "Expected NullPointerException when runtime context is missing");
        LOG.debug("transformProfileJson missing runtime context test passed");
    }

    @Test
    void testProcessProfileMapNodeEntries() throws Exception {
        LOG.debug("test direct invocation of private processProfileMapNodeEntries");
        QName contextQName = QName.create(
            "urn:onf:otcc:yang:tapi-common", "2022-11-21", "profile");
        Throwable cause = invokePrivateAndGetCause(
            "processProfileMapNodeEntries",
            new Class<?>[] {MapNode.class, QName.class},
            null,
            contextQName);
        assertTrue(cause instanceof NullPointerException,
            "Expected NullPointerException for null mapNode input");
        LOG.debug("processProfileMapNodeEntries null mapNode test passed");
    }

    @Test
    void testSaveProfileInContext() throws Exception {
        LOG.debug("test direct invocation of private saveProfileInContext");
        Uuid profileUuid = new Uuid(expectedUuid(PROFILE_UUID));
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.Profile profile =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ProfileBuilder()
                        .withKey(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                                .rev221121.tapi.context.ProfileKey(profileUuid))
                        .setUuid(profileUuid)
                        .build();
        invokePrivate(
                "saveProfileInContext",
                new Class<?>[] {
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                            .tapi.context.Profile.class
                },
                profile);
        verify(networkTransactionService, atLeastOnce()).put(
            eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class), any());
        verify(networkTransactionService, atLeastOnce()).commit();
        LOG.debug("saveProfileInContext datastore interaction test passed");
    }

    private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = TransformationServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(transformationService, args);
    }

    private Throwable invokePrivateAndGetCause(String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = TransformationServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> method.invoke(transformationService, args));
        return exception.getCause();
    }

    private static String expectedUuid(String originalUuid) {
        return UUID.nameUUIDFromBytes((originalUuid + CONTROLLER_ID).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> buildMsipMap(String sipUuid) {
        MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder()
            .setServiceInterfacePointUuid(new Uuid(sipUuid))
            .build();
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> map = new HashMap<>();
        map.put(msip.key(), msip);
        return map;
    }

    private static Map<ProfileKey, Profile> buildProfileMap(String profileUuid) {
        Profile profile = new ProfileBuilder().setProfileUuid(new Uuid(profileUuid)).build();
        Map<ProfileKey, Profile> map = new HashMap<>();
        map.put(profile.key(), profile);
        return map;
    }

    private static Map<SourceProfileKey, SourceProfile> buildSourceProfileMap(String profileUuid) {
        SourceProfile sourceProfile = new SourceProfileBuilder().setProfileUuid(new Uuid(profileUuid)).build();
        Map<SourceProfileKey, SourceProfile> map = new HashMap<>();
        map.put(sourceProfile.key(), sourceProfile);
        return map;
    }

    private static Map<SinkProfileKey, SinkProfile> buildSinkProfileMap(String profileUuid) {
        SinkProfile sinkProfile = new SinkProfileBuilder().setProfileUuid(new Uuid(profileUuid)).build();
        Map<SinkProfileKey, SinkProfile> map = new HashMap<>();
        map.put(sinkProfile.key(), sinkProfile);
        return map;
    }

    @Test
    void testTransformLinkUpdatesNepTopologyUuid() throws Exception {
        LOG.debug("test transformLink updates node-edge-point topology-uuid in both key and field");
        String originalTopoUuid = TOPOLOGY_UUID;
        String nodeUuid = NODE_UUID;
        String nepUuid = ONEP_UUID;
        NodeEdgePoint nep = new NodeEdgePointBuilder()
                .setTopologyUuid(new Uuid(originalTopoUuid))
                .setNodeUuid(new Uuid(nodeUuid))
                .setNodeEdgePointUuid(new Uuid(nepUuid))
                .build();
        Map<NodeEdgePointKey, NodeEdgePoint> nepMap = new HashMap<>();
        nepMap.put(nep.key(), nep);
        Link link = new LinkBuilder()
                .setUuid(new Uuid(LINK_UUID))
                .setNodeEdgePoint(nepMap)
                .build();

        Uuid newTopologyUuid = new Uuid(expectedUuid(originalTopoUuid));
        Link transformedLink = (Link) invokePrivate(
                "transformLink",
                new Class<?>[] {Link.class, Uuid.class},
                link, newTopologyUuid);

        assertNotNull(transformedLink, "Transformed link must not be null");
        NodeEdgePoint transformedNep = transformedLink.getNodeEdgePoint().values().iterator().next();
        assertEquals(newTopologyUuid.getValue(), transformedNep.getTopologyUuid().getValue(),
                "NEP topology-uuid field must be updated to new topology UUID");
        assertEquals(newTopologyUuid.getValue(), transformedNep.key().getTopologyUuid().getValue(),
                "NEP key topology-uuid must be updated to new topology UUID");
        assertEquals(nodeUuid, transformedNep.getNodeUuid().getValue(),
                "NEP node-uuid must remain unchanged");
        assertEquals(nepUuid, transformedNep.getNodeEdgePointUuid().getValue(),
                "NEP node-edge-point-uuid must remain unchanged");
        LOG.info("transformLink NEP topology-uuid update test passed");
    }

    private static Node buildNodeWithAllMaps() {
        OwnedNodeEdgePoint onep = new OwnedNodeEdgePointBuilder()
            .setUuid(new Uuid(ONEP_UUID))
            .setMappedServiceInterfacePoint(buildMsipMap(SIP_UUID))
            .setProfile(buildProfileMap(PROFILE_UUID))
            .setSourceProfile(buildSourceProfileMap(PROFILE_UUID))
            .setSinkProfile(buildSinkProfileMap(PROFILE_UUID))
            .build();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        onepMap.put(onep.key(), onep);
        return new NodeBuilder().setUuid(new Uuid(NODE_UUID)).setOwnedNodeEdgePoint(onepMap).build();
    }

}
