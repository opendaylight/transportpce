/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApiBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.ServiceFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.gnpy.api.TopologyFileBuilder;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Connections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.network.topology.rev181214.topo.Elements;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.Result;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.path.properties.PathRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.service.PathRequest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.synchronization.info.Synchronization;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraints;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Class that implements the functions asked to gnpy.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class GnpyUtilitiesImpl {

    private PathComputationRequestInput input;
    private GnpyTopoImpl gnpyTopo = null;
    private GnpyResult gnpyAtoZ;
    private GnpyResult gnpyZtoA;
    private Uint32 requestId;
    private final GnpyConsumer gnpyConsumer;

    public GnpyUtilitiesImpl(NetworkTransactionService networkTransaction, PathComputationRequestInput input,
            GnpyConsumer gnpyConsumer)
        throws GnpyException {
        this.gnpyTopo = new GnpyTopoImpl(networkTransaction);
        this.input = input;
        this.gnpyAtoZ = null;
        this.gnpyZtoA = null;
        this.requestId = Uint32.valueOf(0);
        this.gnpyConsumer = gnpyConsumer;
    }

    public boolean verifyComputationByGnpy(AToZDirection atoz, ZToADirection ztoa, PceConstraints pceHardConstraints)
        throws GnpyException {

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

    @SuppressWarnings("checkstyle:illegalcatch")
    public GnpyResult gnpyResponseOneDirection(GnpyServiceImpl gnpySvc) throws GnpyException {
        requestId = Uint32.valueOf((requestId.toJava()) + 1);
        List<PathRequest> pathRequestList = new ArrayList<>(gnpySvc.getPathRequest().values());
        List<Synchronization> synchronizationList = gnpySvc.getSynchronization();
        // Send the computed path to GNPY tool
        List<Elements> elementsList = new ArrayList<>(gnpyTopo.getElements().values());
        List<Connections> connectionsList = gnpyTopo.getConnections();
        Result gnpyResponse = getGnpyResponse(elementsList, connectionsList, pathRequestList,
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
            throws GnpyException {

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

    public Result getGnpyResponse(List<Elements> elementsList, List<Connections> connectionsList,
        List<PathRequest> pathRequestList, List<Synchronization> synchronizationList) {
        GnpyApi gnpyApi = new GnpyApiBuilder()
            .setTopologyFile(
                new TopologyFileBuilder()
                .setElements(elementsList.stream().collect(Collectors.toMap(Elements::key, element -> element)))
                .setConnections(connectionsList).build())
            .setServiceFile(
                new ServiceFileBuilder()
                .setPathRequest(pathRequestList.stream()
                        .collect(Collectors.toMap(PathRequest::key, pathRequest -> pathRequest)))
                .build())
            .build();
        return gnpyConsumer.computePaths(gnpyApi);
    }

    public GnpyResult getGnpyAtoZ() {
        return gnpyAtoZ;
    }

    public GnpyResult getGnpyZtoA() {
        return gnpyZtoA;
    }
}
