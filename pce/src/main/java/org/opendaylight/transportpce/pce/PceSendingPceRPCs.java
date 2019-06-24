/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.transportpce.pce.PceResult.LocalCause;
import org.opendaylight.transportpce.pce.gnpy.ConnectToGnpyServer;
import org.opendaylight.transportpce.pce.gnpy.ExtractTopoDataStoreImpl;
import org.opendaylight.transportpce.pce.gnpy.GnpyResult;
import org.opendaylight.transportpce.pce.gnpy.ServiceDataStoreOperationsImpl;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApiBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.ServiceFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.TopologyFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Connections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.service.PathRequest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.synchronization.info.Synchronization;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp.PceMetric;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private Long gnpyRequestId = new Long(0);
    private GnpyResult gnpyAtoZ;
    private GnpyResult gnpyZtoA;

    public PceSendingPceRPCs() {
        setPathDescription(null);
        this.input = null;
        this.dataBroker = null;
        this.gnpyAtoZ = null;
        this.gnpyZtoA = null;
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
            // sleep for 10s
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
        LOG.info("cancelResourceReserve ...");
    }

    public void pathComputation() throws Exception {
        // Comput the path according to the constraints of PCE
        rc = pathComputationPCE();

        LOG.info("setPathDescription ...");
        AToZDirection atoz = rc.getAtoZDirection();
        ZToADirection ztoa = rc.getZtoADirection();
        ConnectToGnpyServer connectToGnpy = new ConnectToGnpyServer();
        if ((atoz == null) || (atoz.getAToZ() == null)) {
            rc.setRC("400");
            LOG.warn("In PCE pathComputation: empty atoz path after description: result = {}", rc.toString());
            return;
        } else {
            // Send the computed path A-to-Z to GNPY tool
            if (connectToGnpy.isGnpyURLExist()) {
                ExtractTopoDataStoreImpl xtrTopo = new ExtractTopoDataStoreImpl(dataBroker, input, atoz, gnpyRequestId);
                gnpyRequestId++;
                List<Elements> elementsList1 = xtrTopo.getElements();
                List<Connections> connectionsList1 = xtrTopo.getConnections();
                List<PathRequest> pathRequestList1 = xtrTopo.getPathRequest();
                List<Synchronization> synchronizationList1 = xtrTopo.getSynchronization();
                String gnpyResponse1 = getGnpyResponse(elementsList1, connectionsList1, pathRequestList1,
                    synchronizationList1);
                // Analyze the response
                if (gnpyResponse1 != null) {
                    GnpyResult result = new GnpyResult(gnpyResponse1);
                    LOG.debug("GNPy result created");
                    result.analyzeResult();
                    this.gnpyAtoZ = result;
                } else {
                    LOG.error("No response from the GNPy server");
                }
            }
        }

        if ((ztoa == null) || (ztoa.getZToA() == null)) {
            rc.setRC("400");
            LOG.error("In pathComputation empty ztoa path after description: result = {}", rc.toString());
            return;
        } else {
            // Send the computed path Z-to-A to GNPY tool
            if (connectToGnpy.isGnpyURLExist()) {
                ExtractTopoDataStoreImpl xtrTopo = new ExtractTopoDataStoreImpl(dataBroker, input, ztoa, gnpyRequestId);
                gnpyRequestId++;
                List<Elements> elementsList2 = xtrTopo.getElements();
                List<Connections> connectionsList2 = xtrTopo.getConnections();
                List<PathRequest> pathRequestList2 = xtrTopo.getPathRequest();
                List<Synchronization> synchronizationList2 = xtrTopo.getSynchronization();
                String gnpyResponse2 = getGnpyResponse(elementsList2, connectionsList2, pathRequestList2,
                        synchronizationList2);
                // Analyze the response
                if (gnpyResponse2 != null) {
                    GnpyResult result = new GnpyResult(gnpyResponse2);
                    LOG.debug("GNPy result created");
                    result.analyzeResult();
                    this.gnpyZtoA = result;
                } else {
                    LOG.info("No response from the GNPy server");
                }
            }
        }
        // Set the description of the path
        setPathDescription(new PathDescriptionBuilder().setAToZDirection(atoz).setZToADirection(ztoa));
        LOG.info("In pathComputation Graph is Found");
    }

    public PceResult pathComputationPCE() {
        LOG.info("PathComputation ...");

        PceConstraintsCalc constraints = new PceConstraintsCalc(input, dataBroker);
        pceHardConstraints = constraints.getPceHardConstraints();
        pceSoftConstraints = constraints.getPceSoftConstraints();

        LOG.info("nwAnalizer ...");
        PceCalculation nwAnalizer = new PceCalculation(input, dataBroker, pceHardConstraints, pceSoftConstraints, rc);
        nwAnalizer.calcPath();
        rc = nwAnalizer.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.error("In pathComputation nwAnalizer: result = {}", rc.toString());
            return null;
        }

        LOG.info("PceGraph ...");
        LOG.warn("PathComputation: aPceNode '{}' - zPceNode '{}'", nwAnalizer.getaPceNode(), nwAnalizer.getzPceNode());
        PceGraph graph = new PceGraph(nwAnalizer.getaPceNode(), nwAnalizer.getzPceNode(), nwAnalizer.getAllPceNodes(),
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
                graph = patchRerunGraph(graph);
            }
            if (!rc.getStatus()) {
                LOG.error("In pathComputation graph.calcPath: result = {}", rc.toString());
                return null;
            }
        }

        LOG.info("PcePathDescription ...");
        PcePathDescription description = new PcePathDescription(graph.getPathAtoZ(), nwAnalizer.getAllPceLinks(), rc);
        description.buildDescriptions();
        rc = description.getReturnStructure();
        if (!rc.getStatus()) {
            LOG.error("In pathComputation description: result = {}", rc.toString());
            return null;
        }
        return rc;
    }

    private String getGnpyResponse(List<Elements> elementsList, List<Connections> connectionsList,
            List<PathRequest> pathRequestList, List<Synchronization> synchronizationList) throws Exception {
        GnpyApi gnpyApi = new GnpyApiBuilder()
                .setTopologyFile(
                        new TopologyFileBuilder().setElements(elementsList).setConnections(connectionsList).build())
                .setServiceFile(new ServiceFileBuilder().setPathRequest(pathRequestList).build()).build();
        InstanceIdentifier<GnpyApi> idGnpyApi = InstanceIdentifier.builder(GnpyApi.class).build();
        String gnpyJson;
        ServiceDataStoreOperationsImpl sd = new ServiceDataStoreOperationsImpl(dataBroker);
        gnpyJson = sd.createJsonStringFromDataObject(idGnpyApi, gnpyApi);
        LOG.debug("GNPy  Id: {} / json created : {}", idGnpyApi, gnpyJson);
        ConnectToGnpyServer connect = new ConnectToGnpyServer();
        String gnpyJsonModified = gnpyJson.replace("gnpy-eqpt-config:", "")
                .replace("gnpy-path-computation-simplified:", "").replace("gnpy-network-topology:", "");
        String gnpyResponse = connect.gnpyCnx(gnpyJsonModified);
        return gnpyResponse;
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
        return rc.getStatus();
    }

    public String getMessage() {
        return rc.getMessage();
    }

    public String getResponseCode() {
        return rc.getResponseCode();
    }

    public GnpyResult getGnpy_AtoZ() {
        return gnpyAtoZ;
    }

    public GnpyResult getGnpy_ZtoA() {
        return gnpyZtoA;
    }
}
