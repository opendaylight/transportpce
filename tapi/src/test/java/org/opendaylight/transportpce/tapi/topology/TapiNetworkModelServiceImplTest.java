/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ParentNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ParentNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.PhotonicMediaNodeEdgePointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.PhotonicMediaNodeEdgePointSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;


public class TapiNetworkModelServiceImplTest extends AbstractTest {

    @Mock
    private DeviceTransactionManager deviceTransactionManager;

    @Mock
    private NotificationPublishService notificationPublishService;

    private TapiNetworkModelServiceImpl service;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/openroadm-topology.xml",
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        NetworkTransactionService networkTransactionService = new NetworkTransactionImpl(getDataBroker());

        service = new TapiNetworkModelServiceImpl(
                networkTransactionService,
                deviceTransactionManager,
                mock(),
                notificationPublishService);
    }

    @Test
    public void testNoOccupiedFrequencies() {
        // -----------------------------
        // Prepare test input data
        // -----------------------------
        boolean srg = true;
        String nodeId = "ROADM-B";
        boolean withSip = true;
        String nepPhotonicSublayer = "PHOTONIC_MEDIA_OTS";

        Map<String, TerminationPoint1> tpMap = new HashMap<>();
        TerminationPoint1 tp = terminationPoint();

        tpMap.put("SRG13-PP9-TXRX", tp);

        // ------------------------------------
        // Invoke the method under test
        // ------------------------------------
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> result = service.populateNepsForRdmNode(
                srg,
                nodeId,
                tpMap,
                withSip,
                nepPhotonicSublayer);

        // ------------------------------------
        // Validate: the service should return NEPs built from the TP map
        // ------------------------------------
        assertNotNull(result, "Returned NEP map must not be null");
        assertFalse(result.isEmpty(), "NEP map must not be empty");

        // We expect exactly one NEP if the input map has one TP
        assertEquals(1, result.size(), "Unexpected number of NEPs returned");

        // ------------------------------------
        // Build expected NEP manually for comparison
        // ------------------------------------
        String nodeEdgePointUuid       = "c843d006-1482-3d9d-b064-42c4d9e29d85";
        String nodeUuid                = "cd038766-6de5-3d8e-9fa7-1023f8ff2f00";
        String topologyuuid            = "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7";
        String parentNodeEdgePointUuid = "59b629d8-1788-350f-b364-f5ef410c8b6e";
        String connectionEndpointUuid  = "dd28a23b-3d92-3d73-ba16-b11c0b520525";
        String cepName                 = "CEP+ROADM-B+PHOTONIC_MEDIA_OTS+SRG13-PP9-TXRX";
        String nodeEdgePhotonicName    = "ROADM-B+PHOTONIC_MEDIA_OTS+SRG13-PP9-TXRX";

        OwnedNodeEdgePoint expectedNep = expectedNep(
                nodeEdgePointUuid,
                nodeUuid,
                topologyuuid,
                parentNodeEdgePointUuid,
                connectionEndpointUuid,
                cepName,
                nodeEdgePhotonicName,
                spectrumCapabilityPac(191325000000000L, 196125000000000L)
        );

        OwnedNodeEdgePointKey expectedKey = expectedNep.key();

        // ------------------------------------
        // Validate
        // ------------------------------------
        assertTrue(result.containsKey(expectedKey), "Returned NEPs must contain expected key");

        OwnedNodeEdgePoint actualNep = result.get(expectedKey);
        assertNotNull(actualNep);

        assertEquals(expectedNep, actualNep, "NEP must match");
    }

    @Test
    public void testOccupiedFrequencies() {
        // -----------------------------
        // Prepare test input data
        // -----------------------------
        boolean srg = true;
        String nodeId = "ROADM-C1";
        boolean withSip = true;
        String nepPhotonicSublayer = "PHOTONIC_MEDIA_OTS";

        Map<String, TerminationPoint1> tpMap = new HashMap<>();
        TerminationPoint1 tp = terminationPoint();

        tpMap.put("SRG1-PP1-TXRX", tp);

        // ------------------------------------
        // Invoke the method under test
        // ------------------------------------
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> result = service.populateNepsForRdmNode(
                srg,
                nodeId,
                tpMap,
                withSip,
                nepPhotonicSublayer);

        // ------------------------------------
        // Validate: the service should return NEPs built from the TP map
        // ------------------------------------
        assertNotNull(result, "Returned NEP map must not be null");
        assertFalse(result.isEmpty(), "NEP map must not be empty");

        // We expect exactly one NEP if the input map has one TP
        assertEquals(3, result.size(), "Unexpected number of NEPs returned");

        // ------------------------------------
        // Build expected NEP manually for comparison
        // ------------------------------------
        String nodeEdgePointUuid       = "d5868a64-4032-3a4f-8024-9875eefd9718";
        String nodeUuid                = "4986dca9-2d59-3d79-b306-e11802bcf1e6";
        String topologyuuid            = "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7";
        String parentNodeEdgePointUuid = "abfc9b93-cfae-35a8-9ea9-7fb66b568927";
        String connectionEndpointUuid  = "b96b4b4f-f735-34c6-8c9b-00b29c6c2b26";
        String cepName                 = "CEP+ROADM-C1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX";
        String nodeEdgePhotonicName    = "ROADM-C1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX";

        OwnedNodeEdgePoint expectedNep = expectedNep(
                nodeEdgePointUuid,
                nodeUuid,
                topologyuuid,
                parentNodeEdgePointUuid,
                connectionEndpointUuid,
                cepName,
                nodeEdgePhotonicName,
                spectrumCapabilityPac(
                        191325000000000L, 196125000000000L,
                        196075000000000L, 196125000000000L
                )
        );

        OwnedNodeEdgePointKey expectedKey = expectedNep.key();

        // ------------------------------------
        // Validate
        // ------------------------------------
        assertTrue(result.containsKey(expectedKey), "Returned NEPs must contain expected key");

        OwnedNodeEdgePoint actualNep = result.get(expectedKey);
        assertNotNull(actualNep);

        assertEquals(expectedNep, actualNep, "NEP must match");
    }

    private OwnedNodeEdgePoint1 photonicMediaAugmentation(SpectrumCapabilityPac spectrumCapabilityPac) {
        PhotonicMediaNodeEdgePointSpec photonicMediaNodeEdgePointSpec = new PhotonicMediaNodeEdgePointSpecBuilder()
                .setSpectrumCapabilityPac(spectrumCapabilityPac)
                .build();

        return new OwnedNodeEdgePoint1Builder()
                .setPhotonicMediaNodeEdgePointSpec(photonicMediaNodeEdgePointSpec)
                .build();
    }

    private ConnectionEndPoint connectionEndPoint(
            String nodeUuid,
            String topologyuuid,
            String parentNodeEdgePointUuid,
            String connectionEndpointUuid,
            String nodeEdgePointUuid,
            Name connectionEdgePointName) {

        ClientNodeEdgePoint clientNodeEdgePoint = new ClientNodeEdgePointBuilder()
                .setNodeEdgePointUuid(Uuid.getDefaultInstance(nodeEdgePointUuid))
                .setNodeUuid(Uuid.getDefaultInstance(nodeUuid))
                .setTopologyUuid(Uuid.getDefaultInstance(topologyuuid))
                .build();

        ParentNodeEdgePoint parentNodeEdgePoint = new ParentNodeEdgePointBuilder()
                .setNodeEdgePointUuid(Uuid.getDefaultInstance(parentNodeEdgePointUuid))
                .setNodeUuid(Uuid.getDefaultInstance(nodeUuid))
                .setTopologyUuid(Uuid.getDefaultInstance(topologyuuid))
                .build();

        return new ConnectionEndPointBuilder()
                .setUuid(Uuid.getDefaultInstance(connectionEndpointUuid))
                .setClientNodeEdgePoint(Map.of(clientNodeEdgePoint.key(), clientNodeEdgePoint))
                .setConnectionPortRole(PortRole.SYMMETRIC)
                .setDirection(Direction.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setName(Map.of(connectionEdgePointName.key(), connectionEdgePointName))
                .setOperationalState(OperationalState.ENABLED)
                .setParentNodeEdgePoint(parentNodeEdgePoint)
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1
            cepListAugmentation(ConnectionEndPoint connectionEndPoint) {
        return new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .OwnedNodeEdgePoint1Builder()
                .setCepList(
                        new CepListBuilder()
                                .setConnectionEndPoint(Map.of(connectionEndPoint.key(), connectionEndPoint))
                                .build()
                ).build();
    }

    private OwnedNodeEdgePoint expectedNep(
            String nodeEdgePointUuid,
            String nodeUuid,
            String topologyuuid,
            String parentNodeEdgePointUuid,
            String connectionEndpointUuid,
            String cepName,
            String nodeEdgePhotonicName,
            SpectrumCapabilityPac spectrumCapabilityPac) {

        Name connectionEdgePointName = new NameBuilder()
                .setValueName("ConnectionEndPoint name")
                .setValue(cepName)
                .build();

        Name nodeEdgePointName = new NameBuilder()
                .setValueName("PHOTONIC_MEDIA_OTSNodeEdgePoint")
                .setValue(nodeEdgePhotonicName)
                .build();

        ConnectionEndPoint connectionEndPoint = connectionEndPoint(
                nodeUuid,
                topologyuuid,
                parentNodeEdgePointUuid,
                connectionEndpointUuid,
                nodeEdgePointUuid,
                connectionEdgePointName
        );

        return ownedNodeEdgePoint(
                parentNodeEdgePointUuid,
                nodeEdgePointName,
                photonicMediaAugmentation(spectrumCapabilityPac),
                cepListAugmentation(connectionEndPoint)
        );
    }

    private OwnedNodeEdgePoint ownedNodeEdgePoint(
            String parentNodeEdgePointUuid,
            Name nodeEdgePointName,
            OwnedNodeEdgePoint1 photonicMediaAugmentation,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1
                    cepListAugmentation) {

        SupportedCepLayerProtocolQualifierInstances supportedCepLayerProtocolQualifierInstances =
                new SupportedCepLayerProtocolQualifierInstancesBuilder()
                        .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                        .setNumberOfCepInstances(Uint64.ONE)
                        .build();

        return new OwnedNodeEdgePointBuilder()
                .setUuid(Uuid.getDefaultInstance(parentNodeEdgePointUuid))
                .setDirection(Direction.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setName(Map.of(nodeEdgePointName.key(), nodeEdgePointName))
                .setSupportedCepLayerProtocolQualifierInstances(List.of(supportedCepLayerProtocolQualifierInstances))
                .addAugmentation(photonicMediaAugmentation)
                .addAugmentation(cepListAugmentation).build();
    }

    private TerminationPoint1 terminationPoint() {
        // Build a TerminationPoint1 using the builder (from uploaded files)
        return new TerminationPoint1Builder()
            .setTpType(OpenroadmTpType.SRGTXRXPP)
            .setAdministrativeState(AdminStates.InService)
            .setOperationalState(State.InService)
            .build();
    }

    private SpectrumCapabilityPac spectrumCapabilityPac(long supportableLow, long supportableHigh) {
        return spectrumCapabilityPac(supportableLow, supportableHigh, 0L, 0L);
    }

    private SpectrumCapabilityPac spectrumCapabilityPac(
            long supportableLow, long supportableHigh,
            long occupiedLow, long occupiedHigh) {

        SpectrumCapabilityPacBuilder spectrumCapabilityPacBuilder = new SpectrumCapabilityPacBuilder();

        // Build supportable spectrum
        SupportableSpectrum supportableSpectrum = new SupportableSpectrumBuilder()
                .setLowerFrequency(Uint64.valueOf(supportableLow))
                .setUpperFrequency(Uint64.valueOf(supportableHigh))
                .build();

        spectrumCapabilityPacBuilder.setSupportableSpectrum(Map.of(supportableSpectrum.key(), supportableSpectrum));

        if (occupiedLow > 0 && occupiedHigh > 0) {
            // Occupied spectrum exists
            OccupiedSpectrum occupiedSpectrum = new OccupiedSpectrumBuilder()
                    .setLowerFrequency(Uint64.valueOf(occupiedLow))
                    .setUpperFrequency(Uint64.valueOf(occupiedHigh))
                    .build();

            spectrumCapabilityPacBuilder.setOccupiedSpectrum(Map.of(occupiedSpectrum.key(), occupiedSpectrum));

            // Compute available spectrum = supportable minus occupied
            AvailableSpectrum availableSpectrum = new AvailableSpectrumBuilder()
                    .setLowerFrequency(Uint64.valueOf(supportableLow))
                    .setUpperFrequency(Uint64.valueOf(occupiedLow))
                    .build();

            return spectrumCapabilityPacBuilder
                    .setAvailableSpectrum(Map.of(availableSpectrum.key(), availableSpectrum))
                    .build();
        }
        // No occupied spectrum → available = full supportable
        AvailableSpectrum availableSpectrum = new AvailableSpectrumBuilder()
                .setLowerFrequency(Uint64.valueOf(supportableLow))
                .setUpperFrequency(Uint64.valueOf(supportableHigh))
                .build();

        return spectrumCapabilityPacBuilder
                .setAvailableSpectrum(Map.of(availableSpectrum.key(), availableSpectrum))
                .build();
    }
}
