/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.topology.changes.TopologyChangesExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.NepIdentifier;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenRoadmTopologyRepository;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OwnedNodeEdgePointRepository;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.TapiIdentifierFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointSpectrumCapability;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointMapping;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointMappingFactory;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.PhotonicMediaNodeEdgePointSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopologyUpdate implements TopologyUpdate {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopologyUpdate.class);

    private final OwnedNodeEdgePointRepository ownedNodeEdgePointRepository;

    private final TerminationPointMappingFactory terminationPointMappingFactory;

    private final OpenRoadmTopologyRepository openRoadmTopologyRepository;

    private final TapiIdentifierFactory tapiIdentifierFactory;

    private final TopologyChangesExtractor topologyChangesExtractor;

    /**
     * Applies OpenROADM topology update results to the TAPI topology.
     *
     * <p>This implementation processes {@link TopologyUpdateResult} change events and, for each affected
     * OpenROADM termination point, updates the corresponding TAPI owned node edge point (NEP) by writing
     * derived photonic-media attributes (currently Spectrum Capability PAC).
     *
     * <h4>Why the OpenROADM {@link Network} is read up-front</h4>
     *
     * <p>Mapping a change entry ({@code nodeId + tpId}) to a {@link TerminationPointMapping} requires
     * resolving the OpenROADM termination point type and supporting-node information from the OpenROADM
     * topology model. To ensure all termination points are resolved against a consistent view, this class
     * reads the OpenROADM {@link Network} snapshot once per invocation and passes it to the
     * {@link TerminationPointMappingFactory}.
     *
     * <h4>Processing steps</h4>
     * <ol>
     *   <li>Validate that the update result contains changes (otherwise no-op).</li>
     *   <li>Read the OpenROADM topology {@link Network} snapshot.</li>
     *   <li>Create {@link TerminationPointMapping}s for the changed termination points.</li>
     *   <li>Derive {@link OwnedNodeEdgePointSpectrumCapability} objects via {@link TopologyChangesExtractor}.</li>
     *   <li>Read each affected TAPI NEP and update its photonic-media augmentation with the derived PAC.</li>
     * </ol>
     */
    public OpenRoadmTopologyUpdate(
            OwnedNodeEdgePointRepository ownedNodeEdgePointRepository,
            TerminationPointMappingFactory terminationPointMappingFactory,
            OpenRoadmTopologyRepository openRoadmTopologyRepository,
            TapiIdentifierFactory tapiIdentifierFactory,
            TopologyChangesExtractor topologyChangesExtractor) {

        this.ownedNodeEdgePointRepository = ownedNodeEdgePointRepository;
        this.terminationPointMappingFactory = terminationPointMappingFactory;
        this.openRoadmTopologyRepository = openRoadmTopologyRepository;
        this.tapiIdentifierFactory = tapiIdentifierFactory;
        this.topologyChangesExtractor = topologyChangesExtractor;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method reads the OpenROADM topology {@link Network} once per invocation and uses it as a
     * resolution snapshot for all termination point mappings derived from the supplied update result.
     * This avoids repeatedly reading topology data and ensures consistent TP type resolution across all changes.
     */
    @Override
    public boolean copyToTAPI(TopologyUpdateResult topologyUpdateResult) {
        LOG.info("TAPI - Received topology update");
        LOG.debug("Topology update result: {}", topologyUpdateResult);

        if (isNoOp(topologyUpdateResult)) {
            LOG.info("TAPI - No topology changes to apply");
            return true;
        }

        Network openRoadmTopo = readOpenRoadmTopology();
        Set<TerminationPointMapping> mappings = createMappings(topologyUpdateResult, openRoadmTopo);
        Set<OwnedNodeEdgePointSpectrumCapability> scaps = buildSpectrumCapabilities(mappings);

        applySpectrumCapabilities(scaps);

        LOG.info("Done copying OpenROADM topology updates to TAPI");
        return true;
    }

    /**
     * Checks whether the supplied update result contains any actionable topology changes.
     *
     * <p>This method treats {@code null} inputs and empty change sets as a no-op.
     *
     * @param result topology update result; may be {@code null}
     * @return {@code true} if no changes should be applied, otherwise {@code false}
     */
    private static boolean isNoOp(TopologyUpdateResult result) {
        return result == null || result.nonnullTopologyChanges().isEmpty();
    }

    /**
     * Creates OpenROADM-to-TAPI termination point mappings for all termination points referenced
     * by the supplied update result.
     *
     * <p>The mappings are derived using the provided OpenROADM topology snapshot, ensuring that
     * termination point type resolution and supporting-node lookups are consistent for all changes.
     *
     * @param result topology update result containing termination point changes
     * @param topo OpenROADM topology snapshot used for type resolution
     * @return a set of termination point mappings; never {@code null}
     */
    private Set<TerminationPointMapping> createMappings(TopologyUpdateResult result, Network topo) {
        return terminationPointMappingFactory.create(result, topo);
    }

    /**
     * Builds spectrum capability information for a set of already-resolved termination point mappings.
     *
     * <p>This typically results in one spectrum capability object per NEP name contained in the mappings.
     *
     * @param mappings termination point mappings used as input
     * @return derived spectrum capability objects; never {@code null}
     */
    private Set<OwnedNodeEdgePointSpectrumCapability> buildSpectrumCapabilities(Set<TerminationPointMapping> mappings) {
        return topologyChangesExtractor.spectrumCapabilityPacs(mappings);
    }

    /**
     * Applies the supplied spectrum capability updates to the corresponding NEPs in the TAPI datastore.
     *
     * <p>Each element in {@code caps} is treated independently. If a referenced NEP does not exist,
     * the update for that NEP is skipped and processing continues.
     *
     * @param caps set of spectrum capability updates to apply
     */
    private void applySpectrumCapabilities(Set<OwnedNodeEdgePointSpectrumCapability> caps) {
        LOG.info("TAPI - Updating {} objects in topology", caps.size());
        for (OwnedNodeEdgePointSpectrumCapability cap : caps) {
            updateSingleNep(cap);
        }
    }

    /**
     * Applies a single spectrum capability update to one specific TAPI NEP.
     *
     * <p>This method:
     * <ol>
     *   <li>derives the TAPI NEP identifier from the OpenROADM termination point and NEP name</li>
     *   <li>reads the current NEP from the datastore</li>
     *   <li>updates the photonic-media augmentation (Spectrum Capability PAC)</li>
     *   <li>writes the updated NEP back to the datastore</li>
     * </ol>
     *
     * <p>If the referenced NEP does not exist, the update is skipped.
     *
     * @param cap derived spectrum capability information for one NEP
     */
    private void updateSingleNep(OwnedNodeEdgePointSpectrumCapability cap) {
        NepIdentifier nepId = toNepIdentifier(cap);

        Optional<OwnedNodeEdgePoint> existing = ownedNodeEdgePointRepository.read(nepId);
        if (existing.isEmpty()) {
            LOG.warn("Owned node edge point {} not found in TAPI Topology", nepId.toLogString());
            return;
        }

        OwnedNodeEdgePoint updated = withSpectrumCapabilityPac(existing.orElseThrow(), cap);
        ownedNodeEdgePointRepository.update(nepId, updated);
    }

    /**
     * Computes the {@link NepIdentifier} (IID + seed context) for a derived spectrum capability update.
     *
     * <p>This identifier is used for datastore operations and also provides a human-friendly
     * log string via {@link NepIdentifier#toLogString()}.
     *
     * @param cap derived spectrum capability information for one NEP
     * @return resolved NEP identifier; never {@code null}
     */
    private NepIdentifier toNepIdentifier(OwnedNodeEdgePointSpectrumCapability cap) {
        return tapiIdentifierFactory.nepIdentifier(
                TapiConstants.T0_FULL_MULTILAYER,
                cap.terminationPointId(),
                cap.nepName());
    }

    /**
     * Creates a copy of the supplied NEP with an updated Spectrum Capability PAC.
     *
     * <p>This method preserves all existing NEP attributes and augmentations, while ensuring the
     * {@link OwnedNodeEdgePoint1} augmentation exists and contains an updated
     * photonic-media node-edge-point spec.
     *
     * @param current current NEP from the datastore
     * @param cap derived spectrum capability information to apply
     * @return updated NEP instance
     */
    private static OwnedNodeEdgePoint withSpectrumCapabilityPac(
            OwnedNodeEdgePoint current,
            OwnedNodeEdgePointSpectrumCapability cap) {

        OwnedNodeEdgePoint1 existingAug = current.augmentation(OwnedNodeEdgePoint1.class);
        OwnedNodeEdgePoint1Builder augBuilder =
                existingAug == null ? new OwnedNodeEdgePoint1Builder() : new OwnedNodeEdgePoint1Builder(existingAug);

        return new OwnedNodeEdgePointBuilder(current)
                .addAugmentation(
                        augBuilder.setPhotonicMediaNodeEdgePointSpec(
                                        new PhotonicMediaNodeEdgePointSpecBuilder()
                                                .setSpectrumCapabilityPac(cap.spectrumCapabilityPac())
                                                .build())
                                .build())
                .build();
    }

    /**
     * Reads the OpenROADM topology {@link Network} from the datastore.
     *
     * <p>The OpenROADM topology is used as a resolution snapshot when building termination point mappings
     * (nodeId/tpId -&gt; OpenROADM TP type -&gt; derived TAPI NEP names).
     *
     * @return OpenROADM topology network; never {@code null}
     * @throws NoSuchElementException if the topology cannot be read from the datastore
     */
    private Network readOpenRoadmTopology() {
        Network openroadmTopo;

        openroadmTopo = openRoadmTopologyRepository.read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing OpenROADM topology in CONFIGURATION datastore ("
                                + InstanceIdentifiers.OPENROADM_TOPOLOGY_II
                                + "). Cannot apply topology update in TAPI."));

        return openroadmTopo;
    }
}
