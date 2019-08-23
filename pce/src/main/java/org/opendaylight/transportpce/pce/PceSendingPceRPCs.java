/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.gnpy.ConnectToGnpyServer;
import org.opendaylight.transportpce.pce.gnpy.GnpyResult;
import org.opendaylight.transportpce.pce.gnpy.GnpyUtilitiesImpl;
import org.opendaylight.transportpce.pce.graph.PceGraph;
import org.opendaylight.transportpce.pce.networkanalyzer.PceCalculation;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class for Sending
 * PCE requests :
 * - path-computation-request
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

    public PceSendingPceRPCs() {
        setPathDescription(null);
        this.input = null;
        this.networkTransaction = null;
    }

    public PceSendingPceRPCs(PathComputationRequestInput input,
        NetworkTransactionService networkTransaction) {
        setPathDescription(null);

        // TODO compliance check to check that input is not empty
        this.input = input;
        this.networkTransaction = networkTransaction;
    }

    public void cancelResourceReserve() {
        success = false;
        LOG.info("Wait for 10s til beginning the PCE cancelResourceReserve request");
        try {
            // sleep for 10s
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
        success = true;
        LOG.info("cancelResourceReserve ...");
    }

    public void pathComputationWithConstraints(PceConstraints hardConstraints, PceConstraints softConstraints)
            throws Exception {

        PceCalculation nwAnalizer =
            new PceCalculation(input, networkTransaction, hardConstraints, softConstraints, rc);
        nwAnalizer.calcPath();
        rc = nwAnalizer.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.error("In pathComputationWithConstraints, nwAnalizer: result = {}", rc.toString());
            return;
        }
        LOG.info("PceGraph ...");
        PceGraph graph = new PceGraph(nwAnalizer.getaendPceNode(),
            nwAnalizer.getzendPceNode(), nwAnalizer.getAllPceNodes(),
            hardConstraints, softConstraints, rc);
        graph.calcPath();
        rc = graph.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.warn("In pathComputation : Graph return without Path ");
            // TODO fix. This is quick workaround for algorithm problem
            if ((rc.getLocalCause() == PceResult.LocalCause.TOO_HIGH_LATENCY)
                && (hardConstraints.getPceMetrics() == PceMetric.HopCount)
                && (hardConstraints.getMaxLatency() != -1)) {
                hardConstraints.setPceMetrics(PceMetric.PropagationDelay);
                graph = patchRerunGraph(graph);
            }
            if (!rc.getStatus()) {
                LOG.error("In pathComputationWithConstraints, graph.calcPath: result = {}", rc.toString());
                return;
            }
        }
        LOG.info("PcePathDescription ...");
        PcePathDescription description = new PcePathDescription(graph.getPathAtoZ(), nwAnalizer.getAllPceLinks(), rc);
        description.buildDescriptions();
        rc = description.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.error("In pathComputationWithConstraints, description: result = {}", rc.toString());
            return;
        }
    }

    public void pathComputation() throws Exception {

        PceConstraintsCalc constraints = new PceConstraintsCalc(input, networkTransaction);
        pceHardConstraints = constraints.getPceHardConstraints();
        pceSoftConstraints = constraints.getPceSoftConstraints();
        pathComputationWithConstraints(pceHardConstraints, pceSoftConstraints);
        this.success = rc.getStatus();
        this.message = rc.getMessage();
        this.responseCode = rc.getResponseCode();

        if (!rc.getStatus()) {
            LOG.error("In pathComputation, pathComputationWithConstraints: result = {}", rc.toString());
            return;
        }

        // Verify the path with GNPy
        AToZDirection atoz = rc.getAtoZDirection();
        ZToADirection ztoa = rc.getZtoADirection();
        ConnectToGnpyServer connectToGnpy = new ConnectToGnpyServer();

        // Verify that the GNPy server exists
        if (connectToGnpy.isGnpyURLExist()) {
            LOG.info("Gnpy instance is connected T-PCE");
            GnpyUtilitiesImpl gnpy = new GnpyUtilitiesImpl(networkTransaction, input);
            if (gnpy.verifyComputationByGnpy(atoz, ztoa, pceHardConstraints)) {
                LOG.info("The path is feasible according to Gnpy");
                setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
            }
            else {
                LOG.info("The path is not feasible according to Gnpy");
                HardConstraints gnpyPathAsHC = null;
                gnpyPathAsHC = gnpy.askNewPathFromGnpy(atoz, ztoa, gnpyPathAsHC, pceHardConstraints);
                if (gnpyPathAsHC != null) {
                    LOG.info("GNPy succeed to find another path");
                    // Compute the path
                    PathComputationRequestInput inputFromGnpy = new PathComputationRequestInputBuilder()
                        .setServiceName(input.getServiceName())
                        .setHardConstraints(gnpyPathAsHC)
                        .setSoftConstraints(input.getSoftConstraints())
                        .setPceMetric(PceMetric.HopCount)
                        .setServiceAEnd(input.getServiceAEnd())
                        .setServiceZEnd(input.getServiceZEnd())
                        .build();
                    PceConstraintsCalc constraintsGnpy = new PceConstraintsCalc(inputFromGnpy, networkTransaction);
                    PceConstraints gnpyHardConstraints = constraintsGnpy.getPceHardConstraints();
                    PceConstraints gnpySoftConstraints = constraintsGnpy.getPceSoftConstraints();
                    pathComputationWithConstraints(gnpyHardConstraints, gnpySoftConstraints);
                    atoz = rc.getAtoZDirection();
                    ztoa = rc.getZtoADirection();
                    setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
                    if (gnpy.verifyComputationByGnpy(atoz, ztoa, pceHardConstraints)) {
                        LOG.info("In pathComputation: the new path computed by GNPy is valid");
                        setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
                        this.success = true;
                        this.message = "Path is calculated by GNPy ";
                        this.responseCode = ResponseCodes.RESPONSE_OK;
                    } else {
                        LOG.info("In pathComputation: the new path computed by GNPy is not valid");
                        this.success = false;
                        this.message = "No path available ";
                        this.responseCode = ResponseCodes.RESPONSE_FAILED;
                    }
                } else {
                    LOG.info("GNPy failed to find another path");
                    this.success = false;
                    this.message = "No path available by PCE and GNPy ";
                    this.responseCode = ResponseCodes.RESPONSE_FAILED;
                }
            }
            gnpyAtoZ = gnpy.getGnpyAtoZ();
            gnpyZtoA = gnpy.getGnpyZtoA();
        } else {
            LOG.info("in PCESendingPceRPCs: Cannot connect to GNPy!!");
            setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
            LOG.info("In pathComputation Graph is Found");
        }
    }

    private PceGraph patchRerunGraph(PceGraph graph) {
        LOG.info("In pathComputation patchRerunGraph : rerun Graph with metric = PROPAGATION-DELAY ");
        graph.setConstrains(pceHardConstraints, pceSoftConstraints);
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
