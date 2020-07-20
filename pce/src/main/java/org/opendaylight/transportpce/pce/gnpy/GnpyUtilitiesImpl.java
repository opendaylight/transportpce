/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApiBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.ServiceFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.TopologyFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Connections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.ElementsKey;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.generic.path.properties.path.properties.PathRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.service.PathRequest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.service.PathRequestKey;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.synchronization.info.Synchronization;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraints;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that implements the functions asked to gnpy.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class GnpyUtilitiesImpl {

    private static final Logger LOG = LoggerFactory.getLogger(GnpyUtilitiesImpl.class);
    private NetworkTransactionService networkTransaction;
    private PathComputationRequestInput input;
    private GnpyTopoImpl gnpyTopo = null;
    private GnpyResult gnpyAtoZ;
    private GnpyResult gnpyZtoA;
    private Uint32 requestId;

    public GnpyUtilitiesImpl(NetworkTransactionService networkTransaction, PathComputationRequestInput input)
        throws GnpyException {

        this.networkTransaction = networkTransaction;
        this.gnpyTopo = new GnpyTopoImpl(networkTransaction);
        this.input = input;
        this.gnpyAtoZ = null;
        this.gnpyZtoA = null;
        this.requestId = Uint32.valueOf(0);
    }

    public boolean verifyComputationByGnpy(AToZDirection atoz, ZToADirection ztoa, PceConstraints pceHardConstraints)
        throws GnpyException, Exception {

        if (atoz == null || atoz.getAToZ() == null || ztoa == null || ztoa.getZToA() == null) {
            throw new GnpyException("In GnpyUtilities: the path transmitted to Gnpy is null");
        }

        GnpyServiceImpl gnpySvc1 = new GnpyServiceImpl(input, atoz, requestId, gnpyTopo, pceHardConstraints);
        this.gnpyAtoZ = gnpyResponseOneDirection(gnpySvc1);
        boolean isPcePathFeasible = false;
        isPcePathFeasible = this.gnpyAtoZ.getPathFeasibility();
        GnpyServiceImpl gnpySvc2 = new GnpyServiceImpl(input, ztoa, requestId, gnpyTopo, pceHardConstraints);
        this.gnpyZtoA = gnpyResponseOneDirection(gnpySvc2);
        isPcePathFeasible &= this.gnpyZtoA.getPathFeasibility();
        return isPcePathFeasible;
    }

    public GnpyResult gnpyResponseOneDirection(GnpyServiceImpl gnpySvc) throws GnpyException, Exception {
        requestId = Uint32.valueOf((requestId.toJava()) + 1);
        Map<PathRequestKey,PathRequest> pathRequestMap = gnpySvc.getPathRequest();
        List<Synchronization> synchronizationList = gnpySvc.getSynchronization();
        // Send the computed path to GNPY tool
        Map<ElementsKey,Elements> elementsMap = gnpyTopo.getElements();
        List<Connections> connectionsList = gnpyTopo.getConnections();
        String gnpyResponse = getGnpyResponse(elementsMap, connectionsList, pathRequestMap,
            synchronizationList);
        // Analyze the response
        if (gnpyResponse == null) {
            throw new GnpyException("In GnpyUtilities: no response from GNPy server");
        }
        GnpyResult result = new GnpyResult(gnpyResponse, gnpyTopo);
        result.analyzeResult();
        return result;
    }

    public HardConstraints askNewPathFromGnpy(PceConstraints pceHardConstraints)
            throws GnpyException, Exception {

        AToZDirection atoztmp = new AToZDirectionBuilder()
            .setRate(input.getServiceAEnd().getServiceRate())
            .build();
        GnpyServiceImpl gnpySvc = new GnpyServiceImpl(input, atoztmp, requestId, gnpyTopo, pceHardConstraints);
        GnpyResult result = gnpyResponseOneDirection(gnpySvc);

        if (result == null) {
            throw new GnpyException("In GnpyUtilities: no result from the GNPy server");
        }

        if (!result.getPathFeasibility()) {
            return null;
        }
        List<PathRouteObjects> pathRouteObjectList = result.analyzeResult();
        return result.computeHardConstraintsFromGnpyPath(pathRouteObjectList);
    }

    public String getGnpyResponse(Map<ElementsKey,Elements> elementsMap, List<Connections> connectionsList,
        Map<PathRequestKey,PathRequest> pathRequestMap, List<Synchronization> synchronizationList)
                throws GnpyException, Exception {
        GnpyApi gnpyApi = new GnpyApiBuilder()
            .setTopologyFile(
                new TopologyFileBuilder().setElements(elementsMap).setConnections(connectionsList).build())
            .setServiceFile(
                new ServiceFileBuilder().setPathRequest(pathRequestMap).build())
            .build();
        InstanceIdentifier<GnpyApi> idGnpyApi = InstanceIdentifier.builder(GnpyApi.class).build();
        String gnpyJson;
        ServiceDataStoreOperationsImpl sd = new ServiceDataStoreOperationsImpl(networkTransaction);
        gnpyJson = sd.createJsonStringFromDataObject(idGnpyApi, gnpyApi);
        LOG.debug("GNPy Id: {} / json created : {}", idGnpyApi, gnpyJson);
        ConnectToGnpyServer connect = new ConnectToGnpyServer();
        String gnpyJsonModified = gnpyJson
            .replace("gnpy-eqpt-config:", "")
            .replace("gnpy-path-computation-simplified:", "")
            .replace("gnpy-network-topology:", "");

        return connect.returnGnpyResponse(gnpyJsonModified);
    }

    public GnpyResult getGnpyAtoZ() {
        return gnpyAtoZ;
    }

    public GnpyResult getGnpyZtoA() {
        return gnpyZtoA;
    }
}
