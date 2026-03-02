/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.topology.nep;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.openroadm.TopologyNodeId;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenRoadmTerminationPointReader;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.OpenRoadmSpectrumRangeExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.SpectrumRanges;
import org.opendaylight.transportpce.tapi.topology.ORtoTapiTopoConversionTools;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIERMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRoadmNepFactory implements RoadmNepFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRoadmNepFactory.class);

    private final OpenRoadmTerminationPointReader openRoadmTerminationPointReader;

    private final OpenRoadmSpectrumRangeExtractor openRoadmSpectrumRangeExtractor;

    private final ORtoTapiTopoConversionTools tapiFactory;

    public DefaultRoadmNepFactory(
            OpenRoadmTerminationPointReader openRoadmTerminationPointReader,
            OpenRoadmSpectrumRangeExtractor openRoadmSpectrumRangeExtractor,
            ORtoTapiTopoConversionTools tapiFactory) {

        this.openRoadmTerminationPointReader = openRoadmTerminationPointReader;
        this.openRoadmSpectrumRangeExtractor = openRoadmSpectrumRangeExtractor;
        this.tapiFactory = tapiFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(
            boolean srg,
            String nodeId,
            Map<String, TerminationPoint1> tpMap,
            boolean withSip,
            String nepPhotonicSublayer,
            TapiLink tapiLink) {

        return populateNepsForRdmNode(srg, nodeId, tpMap, withSip, nepPhotonicSublayer, tapiLink, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(
            boolean srg,
            String nodeId,
            Map<String, TerminationPoint1> tpMap,
            boolean withSip,
            String nepPhotonicSublayer,
            TapiLink tapiLink,
            int depth) {

        LOG.info("Populating NEPs for ROADM node {} from {} TPs", nodeId, tpMap.size());
        // Create NEPs for MC and Photonic Media OTS/OMS
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        int tpCounter = 0;
        for (Map.Entry<String, TerminationPoint1> entry : tpMap.entrySet()) {
            final String tpId = entry.getKey();
            final TerminationPoint1 tp = entry.getValue();

            // PHOTONIC MEDIA NEP
            final String nepNameValue = String.join("+", nodeId, nepPhotonicSublayer, tpId);
            LOG.info("[depth={}] Photonic Media NEP name = {} (TP {} of {})",
                    depth,
                    nepNameValue,
                    tpCounter++,
                    tpMap.size());

            SupportedCepLayerProtocolQualifierInstancesBuilder sclpqiBd =
                    new SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setNumberOfCepInstances(Uint64.ONE);

            switch (nepPhotonicSublayer) {
                case TapiConstants.PHTNC_MEDIA_OMS:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROMS.VALUE);
                    break;
                case TapiConstants.PHTNC_MEDIA_OTS:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE);
                    break;
                case TapiConstants.MC:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIERMC.VALUE);
                    break;
                case TapiConstants.OTSI_MC:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE);
                    break;
                default:
                    break;
            }

            List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>(List.of(sclpqiBd.build()));

            OwnedNodeEdgePointBuilder onepBd = new OwnedNodeEdgePointBuilder();

            if (!nepPhotonicSublayer.equals(TapiConstants.MC) && !nepPhotonicSublayer.equals(TapiConstants.OTSI_MC)) {

                Map<Frequency, Frequency> usedFreqMap = new HashMap<>();
                Map<Frequency, Frequency> availableFreqMap = new HashMap<>();

                final TopologyNodeId nodeIdInTopology = TopologyNodeId.fromNodeAndTpId(nodeId, tpId);

                switch (tp.getTpType()) {
                    // Whatever is the TP and its type we consider that it is handled in a bidirectional way :
                    // same wavelength(s) used in both direction.
                    case SRGRXPP:
                    case SRGTXPP:
                    case SRGTXRXPP:
                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1
                                tp11 = getNetworkTerminationPoint11FromDatastore(nodeIdInTopology, tpId);

                        if (tp11 != null) {

                            SpectrumRanges srgRanges = openRoadmSpectrumRangeExtractor.extractRoadm(
                                    tp.getTpType(),
                                    tp11);
                            usedFreqMap = srgRanges.occupied();
                            availableFreqMap = srgRanges.available();

                            if (usedFreqMap != null && !usedFreqMap.isEmpty()) {
                                LOG.debug("[depth={}] TNMSI:populateNepsForRdmNode : Entering LOOP creating OTSiMC & MC"
                                                + " with usedFreqMap non empty {} for Node {}, tp {}",
                                        usedFreqMap,
                                        nodeId,
                                        tpMap,
                                        depth);

                                onepMap.putAll(
                                        populateNepsForRdmNode(
                                                srg,
                                                nodeId,
                                                new HashMap<>(Map.of(tpId, tp)),
                                                true,
                                                TapiConstants.MC,
                                                tapiLink,
                                                depth + 1));

                                onepMap.putAll(
                                        populateNepsForRdmNode(
                                                srg,
                                                nodeId,
                                                new HashMap<>(Map.of(tpId, tp)),
                                                true,
                                                TapiConstants.OTSI_MC,
                                                tapiLink,
                                                depth + 1));
                            }
                        }
                        break;
                    case DEGREERXTTP:
                    case DEGREETXTTP:
                    case DEGREETXRXTTP:
                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1
                                usedTp = getNetworkTerminationPoint11FromDatastore(nodeIdInTopology, tpId);

                        if (usedTp != null) {
                            SpectrumRanges degRanges = openRoadmSpectrumRangeExtractor.extractRoadm(
                                    tp.getTpType(),
                                    usedTp);
                            usedFreqMap = degRanges.occupied();
                            availableFreqMap = degRanges.available();
                        }
                        break;
                    default:
                        break;
                }

                LOG.debug("[depth={}] TNMSI:populateNepsForRdmNode : calling add Photonic NEP spec for Roadm", depth);
                onepBd = tapiFactory.addPhotSpecToRoadmOnep(
                        nodeId,
                        usedFreqMap,
                        availableFreqMap,
                        onepBd,
                        String.join("+", nodeId, nepPhotonicSublayer));
            }

            Name nepName = new NameBuilder()
                    .setValueName(nepPhotonicSublayer + "NodeEdgePoint")
                    .setValue(nepNameValue)
                    .build();

            onepBd
                    .setUuid(new Uuid(nameUuid(nodeId, nepPhotonicSublayer, tpId)))
                    .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                    .setName(Map.of(nepName.key(), nepName))
                    .setSupportedCepLayerProtocolQualifierInstances(sclpqiList)
                    .setDirection(Direction.BIDIRECTIONAL)
                    .setLinkPortRole(PortRole.SYMMETRIC)
                    .setAdministrativeState(tapiLink.setTapiAdminState(tp.getAdministrativeState().getName()))
                    .setOperationalState(tapiLink.setTapiOperationalState(tp.getOperationalState().getName()))
                    .setLifecycleState(LifecycleState.INSTALLED);

            //Create CEP for OTS Nep in SRG (For degree cep are created with OTS link) and add it to srgOtsCepMap:
            //Identify that we have an SRG through withSip set to true only for SRG
            if (withSip) {
                //TODO: currently do not add extension corresponding to channel to OTSiMC/MC CEP on OTS CEP. Although
                //not really required (One CEP per Tp) could complete with extension affecting High/lowFrequencyIndex
                //This affection would be done in the switch case on nepPhotonicSublayer
                int highFrequencyIndex = 0;
                int lowFrequencyIndex = 0;

                ConnectionEndPoint cep = tapiFactory.createCepRoadm(
                        lowFrequencyIndex,
                        highFrequencyIndex,
                        String.join("+", nodeId, tpId),
                        nepPhotonicSublayer,
                        null,
                        srg);

                LOG.debug("[depth={}] TNMSI:populateNepsForRdmNode : TopoInitialMapping, creating CEP for SRG", depth);

                CepList cepList = new CepListBuilder().setConnectionEndPoint(Map.of(cep.key(), cep)).build();

                OwnedNodeEdgePoint1 onep1Bldr = new OwnedNodeEdgePoint1Builder().setCepList(cepList).build();

                logCep(nodeId, tpId, cep, depth);

                onepBd.addAugmentation(onep1Bldr);
            }

            OwnedNodeEdgePoint onep = onepBd.build();

            logOnep(onep, nodeId, depth);

            onepMap.put(onep.key(), onep);
        }

        LOG.info("[depth={}] Done populating ROADM NEP: node={} sublayer={} tps={} withSip={} producedNeps={}",
                depth, nodeId, nepPhotonicSublayer, tpMap.size(), withSip, onepMap.size());
        return onepMap;
    }

    /**
     * Logs a brief summary of an {@link OwnedNodeEdgePoint} (NEP/ONEP) for the given ROADM node.
     * Uses the first available name value if present; otherwise logs {@code "<unnamed>"}.
     *
     * @param ownedNodeEdgePoint the owned node edge point to log
     * @param nodeId the ROADM node identifier
     */
    private void logOnep(OwnedNodeEdgePoint ownedNodeEdgePoint, String nodeId, int depth) {
        LOG.info("[depth={}] NEP {} for ROADM node {}.",
                depth,
                Optional.ofNullable(ownedNodeEdgePoint.getName())
                        .flatMap(m -> m.values().stream().findFirst())
                        .map(Name::getValue)
                        .orElse("<unnamed>"),
                nodeId);
    }

    /**
     * Logs that a ONEP is being populated for the given ROADM node and termination point (TP),
     * including the first available CEP name value if present; otherwise {@code "<unnamed>"}.
     * Also logs the full CEP object at debug level.
     *
     * @param nodeId the ROADM node identifier
     * @param tpId the termination point identifier
     * @param cep the connection end point associated with the TP
     */
    private void logCep(String nodeId, String tpId, ConnectionEndPoint cep, int depth) {
        String name = Optional
                .ofNullable(cep.getName())
                .flatMap(m -> m.values().stream().findFirst())
                .map(Name::getValue)
                .orElse("<unnamed>");

        LOG.info("[depth={}] Populate ONEP for ROADM node {} tp {} containing Cep with name {}.",
                depth,
                nodeId,
                tpId,
                name);

        LOG.debug("[depth={}] CEP {}", depth, cep);
    }

    /**
     * Creates a deterministic, name-based UUID string from the provided parts.
     *
     * <p>The UUID is computed by joining {@code parts} with {@code '+'}.
     *
     * @param parts
     *     The components to join (with {@code '+'}) into the name used for UUID generation.
     * @return
     *     A UUID string (canonical textual representation) deterministically derived from {@code parts}.
     */
    private static String nameUuid(String... parts) {
        String joined = String.join("+", parts);
        return UUID.nameUUIDFromBytes(joined.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
            .TerminationPoint1 getNetworkTerminationPoint11FromDatastore(TopologyNodeId nodeId, String tpId) {

        return openRoadmTerminationPointReader.readTopologyTerminationPoint1(nodeId, tpId).orElse(null);
    }
}
