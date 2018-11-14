/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.transportpce.pce.PceResult.LocalCause;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp.PceMetric;
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
    private DataBroker dataBroker;

    private PceConstraints pceHardConstraints = new PceConstraints();
    private PceConstraints pceSoftConstraints = new PceConstraints();

    public PceSendingPceRPCs() {
        setPathDescription(null);

        this.input = null;
        this.dataBroker = null;
    }

    public PceSendingPceRPCs(PathComputationRequestInput input, DataBroker dataBroker) {
        setPathDescription(null);

        // TODO compliance check to check that input is not empty
        this.input = input;
        this.dataBroker = dataBroker;
    }

    public void cancelResourceReserve() {
        LOG.info("Wait for 10s til beginning the PCE cancelResourceReserve request");
        try {
            Thread.sleep(10000); // sleep for 10s
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
        LOG.info("cancelResourceReserve ...");
    }

    public void pathComputation() {
        LOG.info("PathComputation ...");

        PceConstraintsCalc constraints = new PceConstraintsCalc(input, dataBroker);
        pceHardConstraints = constraints.getPceHardConstraints();
        pceSoftConstraints = constraints.getPceSoftConstraints();


        LOG.info("nwAnalizer ...");
        PceCalculation nwAnalizer = new PceCalculation(input, dataBroker,
                pceHardConstraints, pceSoftConstraints, rc);
        nwAnalizer.calcPath();
        rc = nwAnalizer.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.error("In pathComputation nwAnalizer: result = {}", rc.toString());
            return;
        }

        LOG.info("PceGraph ...");
        LOG.warn("PathComputation: aPceNode '{}' - zPceNode '{}'", nwAnalizer.getaPceNode(), nwAnalizer.getzPceNode());
        PceGraph graph = new PceGraph(
                nwAnalizer.getaPceNode(),
                nwAnalizer.getzPceNode(),
                nwAnalizer.getAllPceNodes(),
                pceHardConstraints, pceSoftConstraints, rc);
        graph.calcPath();
        rc = graph.getReturnStructure();
        if (!rc.getStatus()) {

            LOG.warn("In pathComputation : Graph return without Path ");

            // TODO fix. This is quick workaround for algorithm problem
            if ((rc.getLocalCause() == LocalCause.TOO_HIGH_LATENCY)
                && (pceHardConstraints.getPceMetrics() == PceMetric.HopCount)
                && (pceHardConstraints.getMaxLatency() != -1)) {

                pceHardConstraints.setPceMetrics(PceMetric.PropagationDelay);
                graph = patchRerunGraph(graph, pceHardConstraints, pceSoftConstraints);
            }

            if (!rc.getStatus()) {
                LOG.error("In pathComputation graph.calcPath: result = {}", rc.toString());
                return;
            }
        }

        LOG.info("PcePathDescription ...");
        PcePathDescription description = new PcePathDescription(
                graph.getPathAtoZ(),
                nwAnalizer.getAllPceLinks(), rc);
        description.buildDescriptions();
        rc = description.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.error("In pathComputation description: result = {}", rc.toString());
            return;
        }

        LOG.info("setPathDescription ...");
        AToZDirection atoz = rc.getAtoZDirection();
        ZToADirection ztoa = rc.getZtoADirection();
        if ((atoz == null) || (atoz.getAToZ() == null)) {
            rc.setRC("400");
            LOG.error("In pathComputation empty atoz path after description: result = {}", rc.toString());
            return;
        }
        if ((ztoa == null) || (ztoa.getZToA() == null)) {
            rc.setRC("400");
            LOG.error("In pathComputation empty ztoa path after description: result = {}", rc.toString());
            return;
        }
        setPathDescription(new PathDescriptionBuilder()
                .setAToZDirection(atoz)
                .setZToADirection(ztoa));
        LOG.info("In pathComputation Graph is Found");
    }

    private PceGraph patchRerunGraph(PceGraph graph, PceConstraints pceHardCons, PceConstraints pceSoftCons) {

        LOG.info("In pathComputation patchRerunGraph : rerun Graph with metric = PROPAGATION-DELAY ");
        graph.setConstrains(pceHardCons, pceSoftCons);
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
        return rc.getStatus();
    }

    public String getMessage() {
        return rc.getMessage();
    }

    public String getResponseCode() {
        return rc.getResponseCode();
    }

}
