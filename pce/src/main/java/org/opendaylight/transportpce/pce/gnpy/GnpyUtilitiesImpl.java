/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.util.List;

import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApiBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.ServiceFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.TopologyFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Connections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.generic.path.properties.path.properties.PathRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.service.PathRequest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.synchronization.info.Synchronization;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraints;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that implements the functions asked to gnpy.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class GnpyUtilitiesImpl {

    private static final Logger LOG = LoggerFactory.getLogger(GnpyResult.class);
    private NetworkTransactionService networkTransaction;
    private PathComputationRequestInput input;
    private GnpyTopoImpl gnpyTopo = null;
    private GnpyResult gnpyAtoZ;
    private GnpyResult gnpyZtoA;
    private Long requestId;

    public GnpyUtilitiesImpl(NetworkTransactionService networkTransaction, PathComputationRequestInput input) {
        this.networkTransaction = networkTransaction;
        this.gnpyTopo = new GnpyTopoImpl(networkTransaction);
        this.input = input;
        this.gnpyAtoZ = null;
        this.gnpyZtoA = null;
        this.requestId = (long) 0;
    }

    public boolean verifyComputationByGnpy(AToZDirection atoz, ZToADirection ztoa, PceConstraints pceHardConstraints)
        throws Exception {
        boolean isPcePathFeasible = false;
        List<Elements> elementsList = gnpyTopo.getElements();
        List<Connections> connectionsList = gnpyTopo.getConnections();

        if (atoz == null || atoz.getAToZ() == null) {
            LOG.error("In pceSendingPceRPC: empty atoz path");
        } else {
            GnpyServiceImpl gnpySvc = new GnpyServiceImpl(input, atoz, requestId, gnpyTopo, pceHardConstraints);
            requestId++;
            List<PathRequest> pathRequestList1 = gnpySvc.getPathRequest();
            List<Synchronization> synchronizationList1 = gnpySvc.getSynchronization();
            // Send the computed path A-to-Z to GNPY tool
            String gnpyResponse1 = getGnpyResponse(elementsList, connectionsList, pathRequestList1,
                synchronizationList1);
            // Analyze the response
            if (gnpyResponse1 != null) {
                GnpyResult result = new GnpyResult(gnpyResponse1, gnpyTopo);
                result.analyzeResult();
                this.gnpyAtoZ = result;
                isPcePathFeasible = this.gnpyAtoZ.getPathFeasibility();
            } else {
                LOG.error("No response from the GNPy server");
            }
        }

        if (ztoa == null || ztoa.getZToA() == null) {
            LOG.error("In pceSendingPceRPC: empty ztoa path");
            isPcePathFeasible = false;
        } else {
            GnpyServiceImpl gnpySvc = new GnpyServiceImpl(input, ztoa, requestId, gnpyTopo, pceHardConstraints);
            requestId++;
            List<PathRequest> pathRequestList2 = gnpySvc.getPathRequest();
            List<Synchronization> synchronizationList2 = gnpySvc.getSynchronization();
            // Send the computed path Z-to-A to GNPY tool
            String gnpyResponse2 = getGnpyResponse(elementsList, connectionsList, pathRequestList2,
                synchronizationList2);
            // Analyze the response
            if (gnpyResponse2 != null) {
                GnpyResult result = new GnpyResult(gnpyResponse2, gnpyTopo);
                result.analyzeResult();
                this.gnpyZtoA = result;
                isPcePathFeasible &= this.gnpyZtoA.getPathFeasibility();
            } else {
                LOG.error("No response from the GNPy server");
            }
        }
        return isPcePathFeasible;
    }

    public HardConstraints askNewPathFromGnpy(AToZDirection atoz, ZToADirection ztoa,
        HardConstraints gnpyPathAsHC, PceConstraints pceHardConstraints) throws Exception {
        boolean isPcePathFeasible = false;
        List<Elements> elementsList = gnpyTopo.getElements();
        List<Connections> connectionsList = gnpyTopo.getConnections();

        // Ask a new path A-to-Z
        if (atoz.getAToZWavelengthNumber() == null) {
            LOG.info("The wavelength is null!");
        }

        AToZDirection atoztmp = new AToZDirectionBuilder().setRate(atoz.getRate())
            .setAToZ(null).build();
        GnpyServiceImpl gnpySvc = new GnpyServiceImpl(input, atoztmp, requestId, gnpyTopo, pceHardConstraints);
        requestId++;
        List<PathRequest> pathRequestList1 = gnpySvc.getPathRequest();
        List<Synchronization> synchronizationList1 = gnpySvc.getSynchronization();

        // Send the computed path A-to-Z to GNPY tool
        String gnpyResponse1 = getGnpyResponse(elementsList, connectionsList, pathRequestList1, synchronizationList1);
        // Analyze the response
        if (gnpyResponse1 != null) {
            GnpyResult result = new GnpyResult(gnpyResponse1, gnpyTopo);
            LOG.debug("GNPy result created");
            isPcePathFeasible = result.getPathFeasibility();
            this.gnpyAtoZ = result;
            if (isPcePathFeasible) {
                List<PathRouteObjects> pathRouteObjectList = result.analyzeResult();
                gnpyPathAsHC = result.computeHardConstraintsFromGnpyPath(pathRouteObjectList);
            }
        } else {
            LOG.error("No response from the GNPy server");
        }
        return gnpyPathAsHC;
    }

    public String getGnpyResponse(List<Elements> elementsList, List<Connections> connectionsList,
        List<PathRequest> pathRequestList, List<Synchronization> synchronizationList) throws Exception {
        GnpyApi gnpyApi = new GnpyApiBuilder()
            .setTopologyFile(
                new TopologyFileBuilder().setElements(elementsList).setConnections(connectionsList).build())
            .setServiceFile(new ServiceFileBuilder().setPathRequest(pathRequestList).build()).build();
        InstanceIdentifier<GnpyApi> idGnpyApi = InstanceIdentifier.builder(GnpyApi.class).build();
        String gnpyJson;
        ServiceDataStoreOperationsImpl sd = new ServiceDataStoreOperationsImpl(networkTransaction);
        gnpyJson = sd.createJsonStringFromDataObject(idGnpyApi, gnpyApi);
        LOG.debug("GNPy Id: {} / json created : {}", idGnpyApi, gnpyJson);
        ConnectToGnpyServer connect = new ConnectToGnpyServer();
        String gnpyJsonModified = gnpyJson.replace("gnpy-eqpt-config:", "")
            .replace("gnpy-path-computation-simplified:", "").replace("gnpy-network-topology:", "");
        String gnpyResponse = connect.gnpyCnx(gnpyJsonModified);
        return gnpyResponse;
    }

    public GnpyResult getGnpyAtoZ() {
        return gnpyAtoZ;
    }

    public GnpyResult getGnpyZtoA() {
        return gnpyZtoA;
    }
}
