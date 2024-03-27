/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.OperatorConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.frequency.interval.FrequencyIntervalFactory;
import org.opendaylight.transportpce.pce.frequency.service.ServiceFrequency;
import org.opendaylight.transportpce.pce.frequency.spectrum.FrequencySpectrum;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.SpectrumIndex;
import org.opendaylight.transportpce.pce.gnpy.GnpyException;
import org.opendaylight.transportpce.pce.gnpy.GnpyResult;
import org.opendaylight.transportpce.pce.gnpy.GnpyUtilitiesImpl;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.transportpce.pce.graph.PceGraph;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.transportpce.pce.input.ServiceCreateClientInput;
import org.opendaylight.transportpce.pce.networkanalyzer.PceCalculation;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PceConstraintMode;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.reroute.request.input.Endpoints;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class for Sending
 * PCE requests :
 * - path-computation-request
 * - path-computation-reroute
 * - cancel-resource-reserve.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class PceSendingPceRPCs {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceSendingPceRPCs.class);
    /* define procedure success (or not ). */
    private PceResult rc = new PceResult();

    /*
     * define type of request<br> <code>true</code> pathcomputation <br>
     * <code>false</code> cancelresourcereserve .
     */
    private PathDescriptionBuilder pathDescription;
    private PathComputationRequestInput input;
    private NetworkTransactionService networkTransaction;
    private PceConstraints pceHardConstraints = new PceConstraints();
    private PceConstraints pceSoftConstraints = new PceConstraints();
    private GnpyResult gnpyAtoZ;
    private GnpyResult gnpyZtoA;
    private Boolean success;
    private String message;
    private String responseCode;
    private final GnpyConsumer gnpyConsumer;
    private PortMapping portMapping;
    // Define the termination points whose reservation status is not taken into account during the pruning process
    private Endpoints endpoints;

    public PceSendingPceRPCs(GnpyConsumer gnpyConsumer) {
        setPathDescription(null);
        this.input = null;
        this.networkTransaction = null;
        this.gnpyConsumer = gnpyConsumer;
    }

    public PceSendingPceRPCs(PathComputationRequestInput input, NetworkTransactionService networkTransaction,
                             GnpyConsumer gnpyConsumer, PortMapping portMapping) {
        this.gnpyConsumer = gnpyConsumer;
        setPathDescription(null);
        // TODO compliance check to check that input is not empty
        this.input = input;
        this.networkTransaction = networkTransaction;
        this.portMapping = portMapping;
        this.endpoints = null;
    }

    public PceSendingPceRPCs(PathComputationRequestInput input, NetworkTransactionService networkTransaction,
                             GnpyConsumer gnpyConsumer, PortMapping portMapping,
                             Endpoints endpoints) {
        this.gnpyConsumer = gnpyConsumer;
        setPathDescription(null);
        this.input = input;
        this.networkTransaction = networkTransaction;
        this.portMapping = portMapping;
        this.endpoints = endpoints;
    }

    public void cancelResourceReserve() {
        success = false;
        LOG.info("Wait for 10s til beginning the PCE cancelResourceReserve request");
        try {
            // sleep for 10s
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            LOG.error("in PCESendingPceRPC: ",e);
        }
        success = true;
        LOG.info("cancelResourceReserve ...");
    }

    private void pathComputationWithConstraints(PceConstraints hardConstraints, PceConstraints softConstraints,
            PceConstraintMode mode) {

        rc = new PceResult();
        PceCalculation nwAnalizer = new PceCalculation(input, networkTransaction, hardConstraints, softConstraints, rc,
                portMapping, endpoints);
        nwAnalizer.retrievePceNetwork();
        rc = nwAnalizer.getReturnStructure();
        String serviceType = nwAnalizer.getServiceType();
        if (!rc.getStatus()) {
            LOG.error("In pathComputationWithConstraints, nwAnalizer: result = {}", rc);
            return;
        }
        OperatorConstraints opConstraints = new OperatorConstraints(networkTransaction);
        ClientInput clientInput = new ServiceCreateClientInput(
                this.input,
                new FrequencyIntervalFactory(
                        new ServiceFrequency(),
                        GridConstant.EFFECTIVE_BITS,
                        GridConstant.GRANULARITY
                ),
                new FrequencySpectrum(
                        new SpectrumIndex(
                                GridConstant.START_EDGE_FREQUENCY,
                                GridConstant.GRANULARITY,
                                GridConstant.EFFECTIVE_BITS
                        ),
                        GridConstant.EFFECTIVE_BITS
                ),
                new ServiceFrequency(),
                GridConstant.GRANULARITY
        );
        LOG.info("PceGraph ...");
        PceGraph graph = new PceGraph(nwAnalizer.getaendPceNode(), nwAnalizer.getzendPceNode(),
                nwAnalizer.getAllPceNodes(), nwAnalizer.getAllPceLinks(), hardConstraints,
                rc, serviceType, networkTransaction, mode, opConstraints.getBitMapConstraint(input.getCustomerName()),
                clientInput);
        graph.calcPath();
        rc = graph.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.warn("In pathComputationWithConstraints : Graph return without Path ");
            // TODO fix. This is quick workaround for algorithm problem
            if ((rc.getLocalCause() == PceResult.LocalCause.TOO_HIGH_LATENCY)
                && (hardConstraints.getPceMetrics() == PceMetric.HopCount)
                && (hardConstraints.getMaxLatency() != -1)) {
                hardConstraints.setPceMetrics(PceMetric.PropagationDelay);
                graph = patchRerunGraph(graph);
            }

            if (rc.getLocalCause() == PceResult.LocalCause.HD_NODE_INCLUDE) {
                graph.setKpathsToBring(graph.getKpathsToBring() * 10);
                graph = patchRerunGraph(graph);
            }

            if (!rc.getStatus()) {
                LOG.error("In pathComputationWithConstraints, graph.calcPath: result = {}", rc);
                return;
            }
        }
        LOG.info("PcePathDescription ...");
        PcePathDescription description = new PcePathDescription(graph.getPathAtoZ(), nwAnalizer.getAllPceLinks(), rc);
        description.buildDescriptions();
        rc = description.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.error("In pathComputationWithConstraints, description: result = {}", rc);
        }
    }

    public void pathComputation() throws Exception {

        PceConstraintsCalc constraints = new PceConstraintsCalc(input, networkTransaction);
        pceHardConstraints = constraints.getPceHardConstraints();
        pceSoftConstraints = constraints.getPceSoftConstraints();
        pathComputationWithConstraints(pceHardConstraints, pceSoftConstraints, PceConstraintMode.Loose);
        this.success = rc.getStatus();
        this.message = rc.getMessage();
        this.responseCode = rc.getResponseCode();

        AToZDirection atoz = null;
        ZToADirection ztoa = null;
        if (rc.getStatus()) {
            atoz = rc.getAtoZDirection();
            ztoa = rc.getZtoADirection();
        }

        //Connect to Gnpy to check path feasibility and recompute another path in case of path non-feasibility
        try {
            if (gnpyConsumer.isAvailable()) {
                GnpyUtilitiesImpl gnpy = new GnpyUtilitiesImpl(networkTransaction, input,
                        gnpyConsumer);
                if (rc.getStatus() && gnpyToCheckFeasiblity(atoz,ztoa,gnpy)) {
                    setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
                    return;
                }
                callGnpyToComputeNewPath(gnpy);
            } else {
                setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
            }
        }
        catch (GnpyException e) {
            LOG.error("Exception raised by GNPy {}",e.getMessage());
            setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
        }
    }

    private boolean gnpyToCheckFeasiblity(AToZDirection atoz, ZToADirection ztoa, GnpyUtilitiesImpl gnpy)
            throws GnpyException {

        //Call GNPy for path verification
        if (gnpy.verifyComputationByGnpy(atoz, ztoa, pceHardConstraints)) {
            LOG.info("In pceSendingPceRPC: the path is feasible according to Gnpy");
            gnpyAtoZ = gnpy.getGnpyAtoZ();
            gnpyZtoA = gnpy.getGnpyZtoA();
            return true;
        }
        return false;
    }

    private void callGnpyToComputeNewPath(GnpyUtilitiesImpl gnpy) throws GnpyException {

        //Call GNPy in the case of non feasibility
        LOG.info("In pceSendingPceRPC: the path is not feasible according to Gnpy");
        HardConstraints gnpyPathAsHC = null;
        gnpyPathAsHC = gnpy.askNewPathFromGnpy(pceHardConstraints);
        if (gnpyPathAsHC == null) {
            LOG.info("In pceSendingPceRPC: GNPy failed to find another path");
            this.success = false;
            this.message = "No path available by PCE and GNPy ";
            this.responseCode = ResponseCodes.RESPONSE_FAILED;
            gnpyAtoZ = gnpy.getGnpyAtoZ();
            gnpyZtoA = gnpy.getGnpyZtoA();
            return;
        }

        LOG.info("In pceSendingPceRPC: GNPy succeed to find another path");
        // Compute the path
        PathComputationRequestInput inputFromGnpy = new PathComputationRequestInputBuilder()
            .setServiceName(input.getServiceName())
            .setHardConstraints(gnpyPathAsHC)
            .setSoftConstraints(input.getSoftConstraints())
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceAEnd(input.getServiceAEnd())
            .setServiceZEnd(input.getServiceZEnd())
            .build();
        PceConstraintsCalc constraintsGnpy = new PceConstraintsCalc(inputFromGnpy, networkTransaction);
        PceConstraints gnpyHardConstraints = constraintsGnpy.getPceHardConstraints();
        PceConstraints gnpySoftConstraints = constraintsGnpy.getPceSoftConstraints();
        pathComputationWithConstraints(gnpyHardConstraints, gnpySoftConstraints, PceConstraintMode.Strict);
        AToZDirection atoz = rc.getAtoZDirection();
        ZToADirection ztoa = rc.getZtoADirection();
        if (gnpyToCheckFeasiblity(atoz, ztoa,gnpy)) {
            LOG.info("In pceSendingPceRPC: the new path computed by GNPy is valid");
            this.success = true;
            this.message = "Path is calculated by GNPy";
            this.responseCode = ResponseCodes.RESPONSE_OK;
            setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
        } else {
            LOG.info("In pceSendingPceRPC: the new path computed by GNPy is not valid");
            this.success = false;
            if (rc.getLocalCause() != null) {
                this.message = String.format("No path available (%s)", rc.getLocalCause());
                LOG.error("No path available ({})", rc.getLocalCause());
            } else {
                this.message = "No path available (the new path computed by GNPy is not valid)";
                LOG.error("No path available (the new path computed by GNPy is not valid)");
            }
            this.responseCode = ResponseCodes.RESPONSE_FAILED;
            setPathDescription(new PathDescriptionBuilder().setAToZDirection(null).setZToADirection(null));
        }
    }

    private PceGraph patchRerunGraph(PceGraph graph) {
        LOG.info("In pathComputation patchRerunGraph : rerun Graph with metric = PROPAGATION-DELAY ");
        graph.setConstrains(pceHardConstraints);
        graph.calcPath();
        return graph;
    }

    public PathDescriptionBuilder getPathDescription() {
        return pathDescription;
    }

    private void setPathDescription(PathDescriptionBuilder pathDescription) {
        this.pathDescription = pathDescription;
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    public GnpyResult getGnpyAtoZ() {
        return gnpyAtoZ;
    }

    public GnpyResult getGnpyZtoA() {
        return gnpyZtoA;
    }
}
