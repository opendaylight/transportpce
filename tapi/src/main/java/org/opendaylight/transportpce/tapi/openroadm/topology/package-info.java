/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * OpenROADM → TAPI topology translation helpers.
 *
 * <h2>Purpose</h2>
 *
 * <p>This package family contains the building blocks used to translate
 * OpenROADM topology information (termination points + spectrum availability)
 * into the TAPI topology model, primarily by deriving:
 * <ul>
 *   <li>TAPI {@code owned-node-edge-point} names (NEP names)</li>
 *   <li>TAPI {@code SpectrumCapabilityPac} instances per NEP</li>
 *   <li>Deterministic datastore identifiers (IIDs) for reading/updating NEPs in MD-SAL</li>
 * </ul>
 *
 * <h2>Runnable examples</h2>
 *
 * <p>For an end-to-end, executable example that builds Spectrum Capability PACs for a Termination point in
 * OpenROADM topology and verifies the expected frequency ranges, see:
 * <pre>{@code
 * src/test/java/org/opendaylight/transportpce/tapi/openroadm/topology/changes/TapiTopologyChangesExtractorTest.java
 * }</pre>
 *
 * <p>The test demonstrates:
 * <ul>
 *   <li>how {@code TerminationPointMapping} drives per-NEP capability generation</li>
 *   <li>default vs. occupied-spectrum behavior for the sample topology</li>
 *   <li>reusing one computed Spectrum Capability PAC for multiple NEP names derived from the same TP</li>
 * </ul>
 *
 * <h2>High-level pipeline</h2>
 *
 * <p>The intended end-to-end flow is:
 * <ol>
 *   <li><b>Topology change events</b>
 *       (e.g., {@code TopologyUpdateResult}/{@code TopologyChanges})
 *       are converted into
 *       {@link org.opendaylight.transportpce.tapi.openroadm.topology
 *       .terminationpoint.mapping.TerminationPointMapping}
 *       objects. Each mapping represents a resolved OpenROADM termination
 *       point identity and one-or-more derived TAPI NEP names for that
 *       termination point.</li>
 *
 *   <li><b>Spectrum capability extraction</b> is performed by
 *       {@link org.opendaylight.transportpce.tapi.openroadm.topology.changes
 *       .TopologyChangesExtractor}
 *       (default:
 *       {@link org.opendaylight.transportpce.tapi.openroadm.topology.changes
 *       .TapiTopologyChangesExtractor}):
 *       for each {@code TerminationPointMapping}, it reads the corresponding
 *       OpenROADM termination point from the datastore and produces one
 *       {@link org.opendaylight.transportpce.tapi.openroadm.topology
 *       .terminationpoint.mapping.OwnedNodeEdgePointSpectrumCapability}
 *       per NEP name, reusing the computed Spectrum Capability PAC for all NEP names
 *       derived from the same termination point.</li>
 *
 *   <li><b>Datastore addressing</b> uses deterministic UUIDs computed from
 *       semantic seed strings via
 *       {@link org.opendaylight.transportpce.tapi.openroadm.topology.datastore
 *       .TapiIdentifierFactory}.
 *       The resulting
 *       {@link org.opendaylight.transportpce.tapi.openroadm.topology.datastore
 *       .NepIdentifier}
 *       contains both the {@code iid} (used by MD-SAL repositories) and the
 *       human-readable seeds used for UUID derivation.</li>
 * </ol>
 *
 * <h2>Termination point → NEP mapping</h2>
 *
 * <p>A single OpenROADM termination point may correspond to multiple NEPs
 * in TAPI, typically one per photonic sublayer. The mapping is expressed as:
 * <pre>{@code
 * OpenroadmTpType -> Set<NepPhotonicSublayer> -> Set<OwnedNodeEdgePointName>
 * }</pre>
 *
 * <p>Example
 * <pre>{@code
 * (ROADM-C1-SRG1, SRG1-PP1-TXRX) ->
 *     [ROADM-C1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX, ROADM-C1+OTSi_MEDIA_CHANNEL+SRG1-PP1-TXRX]}
 * </pre>
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link org.opendaylight.transportpce.tapi.openroadm.topology
 *       .terminationpoint.mapping.TerminationPointId}
 *       – resolved identity for an OpenROADM termination point, including TP
 *       type and supporting node id.</li>
 *   <li>{@link org.opendaylight.transportpce.tapi.openroadm.topology
 *       .terminationpoint.mapping.TapiPhotonicSublayerMapper}
 *       – translates {@code OpenroadmTpType} to applicable TAPI photonic
 *       sublayers.</li>
 *   <li>{@link org.opendaylight.transportpce.tapi.openroadm.topology
 *       .terminationpoint.mapping.OwnedNodeEdgePointName}
 *       – value object holding a TAPI {@code Name} plus its
 *       {@code NepPhotonicSublayer}.</li>
 * </ul>
 *
 * <h2>Deterministic UUID/IID strategy</h2>
 *
 * <p>TAPI topology objects are addressed using UUID keys. For repeatable
 * updates, UUIDs are derived deterministically using
 * {@link java.util.UUID#nameUUIDFromBytes(byte[])}.
 *
 * <p>The factory derives three UUIDs from seed strings:
 * <ul>
 *   <li><b>topology UUID</b> from {@code topologySeed}
 *       (often a stable topology name)</li>
 *   <li><b>node UUID</b> from
 *       {@code nodeSeed = "<supportingNodeId>+<NepPhotonicLayer>"}</li>
 *   <li><b>nep UUID</b> from {@code nepSeed = "<NEP name string>"}</li>
 * </ul>
 *
 * <p>{@link org.opendaylight.transportpce.tapi.openroadm.topology
 * .terminationpoint.NepPhotonicLayer}
 * is a coarse grouping (coarser than sublayer) used to keep node identity
 * stable across multiple sublayers.
 *
 * <h2>Spectrum capability rules</h2>
 *
 * <p>Spectrum capability PACs are built using grid parameters from
 * {@link org.opendaylight.transportpce.tapi.openroadm.topology
 * .terminationpoint.spectrum.TapiSpectrumGridConfig}.
 * Implementations typically:
 * <ul>
 *   <li>Always set {@code supportable-spectrum} from the grid config</li>
 *   <li>Set {@code available-spectrum} from extracted ranges
 *       (or a default range when missing)</li>
 *   <li>Set {@code occupied-spectrum} from extracted ranges when present;
 *       otherwise derive it as the complement of {@code available-spectrum}
 *       within supportable range when possible</li>
 * </ul>
 *
 * <p><b>Absent vs. empty semantics</b>: occupied spectrum is only set when
 * there is something to report.
 *
 * <h2>Concrete example</h2>
 *
 * <p>Given an OpenROADM TP:
 * <pre>{@code
 * nodeId="ROADM-C1-SRG1", tpId="SRG1-PP1-TXRX",
 * tpType=SRGTXRXPP, supportingNodeId="ROADM-C1"
 * }</pre>
 *
 * <p>It may map to multiple NEPs (example sublayers):
 * <pre>{@code
 * ROADM-C1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX
 * ROADM-C1+OTSi_MEDIA_CHANNEL+SRG1-PP1-TXRX
 * }</pre>
 *
 * <p>These NEPs are children of a node UUID with the 'seed' of:
 * <pre>{@code
 * nodeSeed = "ROADM-C1+PHOTONIC_MEDIA"
 * }</pre>
 *
 * <p>Translating OpenROADM Termination point name to the TAPI equivalent is done by:
 * {@link org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping
 * .OpenRoadmToTapiTerminationPointMappingFactory}
 *
 * <h2>Frequency bitmap → SpectrumCapabilityPac</h2>
 *
 * <p><i>Examples below are illustrative and omit irrelevant elements for clarity.</i>
 *
 * <p>Scenario: an OpenROADM termination point {@code avail-freq-maps/freq-map}
 * changes. The translation updates the corresponding TAPI
 * {@code spectrum-capability-pac} on the derived owned NEP(s).
 *
 * <p>OpenROADM (fragment):
 * <pre>{@code
 * <network>
 *   <network-id>openroadm-topology</network-id>
 *   <node>
 *     <node-id>ROADM-C1-SRG1</node-id>
 *     <termination-point>
 *       <tp-id>SRG1-PP1-TXRX</tp-id>
 *       <tp-type>SRG-TXRX-PP</tp-type>
 *       <pp-attributes>
 *         <avail-freq-maps>
 *           <map-name>cband</map-name>
 *           <freq-map-granularity>6.25</freq-map-granularity>
 *           <start-edge-freq>191.325</start-edge-freq>
 *           <effective-bits>768</effective-bits>
 *           <freq-map>/// ... ///////////8A</freq-map>
 *         </avail-freq-maps>
 *       </pp-attributes>
 *     </termination-point>
 *     <supporting-node>
 *       <network-ref>openroadm-network</network-ref>
 *       <node-ref>ROADM-C1</node-ref>
 *     </supporting-node>
 *   </node>
 * <network>
 * }</pre>
 *
 * <p>Derived TAPI Spectrum Capability PAC (fragment, Hz):
 * The example below is illustrative and omit irrelevant elements for clarity.
 * <pre>{@code
 * <context>
 *   <topology-context>
 *     <topology>
 *       <name>
 *         <value>T0 - Full Multi-layer topology</value>
 *       </name>
 *
 *       <node>
 *         <name>
 *           <value>ROADM-C1+PHOTONIC_MEDIA</value>
 *         </name>
 *
 *         <owned-node-edge-point>
 *           <name>
 *             <value>ROADM-C1+OTSi_MEDIA_CHANNEL+SRG1-PP1-TXRX</value>
 *           </name>
 *
 *           <photonic-media-node-edge-point-spec>
 *             <spectrum-capability-pac>
 *               <available-spectrum>
 *                 lower-frequency: 191325000000000
 *                 upper-frequency: 196075000000000
 *               </available-spectrum>
 *
 *               <occupied-spectrum>
 *                 lower-frequency: 196075000000000
 *                 upper-frequency: 196125000000000
 *               </occupied-spectrum>
 *
 *               <supportable-spectrum>
 *                 lower-frequency: 191325000000000
 *                 upper-frequency: 196125000000000
 *               </supportable-spectrum>
 *             </spectrum-capability-pac>
 *           </photonic-media-node-edge-point-spec>
 *         </owned-node-edge-point>
 *       </node>
 *     </topology>
 *   </topology-context>
 * </context>
 * }</pre>
 *
 * <h2>Update datastore overview</h2>
 *
 * <ol>
 *   <li>Read OpenROADM topology from the datastore:
 *     {@link org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenRoadmTopologyRepository}</li>
 *   <li>Create mappings for changed termination points:
 *     {@link org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping
 *     .TerminationPointMappingFactory}.</li>
 *   <li>Extract Spectrum Capability Pac from OpenROADM TPs:
 *     {@link org.opendaylight.transportpce.tapi.openroadm.topology.changes.TopologyChangesExtractor}.</li>
 *   <li>Read existing owned NEP from datastore and update the existing owned NEP(s):
 *     {@link org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OwnedNodeEdgePointRepository}.</li>
 * </ol>
 *
 */
package org.opendaylight.transportpce.tapi.openroadm.topology;
