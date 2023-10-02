/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathListBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;

@ExtendWith(MockitoExtension.class)
public class NetworkModelNotificationHandlerTest {

    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private static PathDescription pathDescription;
    private NetworkModelNotificationHandler networkModelListener;

    @BeforeEach
    void setUp() {
        pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(new AToZDirectionBuilder().setAToZ(new HashMap<>(createMapAtoZ())).build())
                .setZToADirection(new ZToADirectionBuilder().setZToA(new HashMap<>(createMapZtoA())).build())
                .build();
        networkModelListener = new NetworkModelNotificationHandler(notificationPublishService,
                serviceDataStoreOperations);
    }

    @Test
    void testChangePathElementStateZAShouldNotModifyPathDescriptionsElementStates() {
        Map<TopologyChangesKey, TopologyChanges> topologyChanges = Map.of(
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.InService)
                        .build(),
                new TopologyChangesKey("tpNodeIdA", "TpIdA2"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA2")
                        .setState(State.InService)
                        .build()
        );

        assertEquals(
            pathDescription.getZToADirection().getZToA(),
            networkModelListener.changePathElementStateZA(topologyChanges, pathDescription));
    }

    @Test
    void testChangePathElementStateZAShouldModifyPathDescriptionsElementStates() {
        Map<TopologyChangesKey, TopologyChanges> topologyChanges = Map.of(
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.OutOfService)
                        .build(),
                new TopologyChangesKey("tpNodeIdA", "TpIdA2"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA2")
                        .setState(State.OutOfService)
                        .build());

        Map<ZToAKey, ZToA> ztoamapExpected = pathDescription.getZToADirection().getZToA();
        ztoamapExpected.computeIfPresent(
                new ZToAKey("6"),
                (zToAKey, zToA) -> new ZToABuilder(zToA)
                        .setResource(new ResourceBuilder(zToA.getResource())
                                .setState(State.OutOfService)
                                .build())
                        .build());
        ztoamapExpected.computeIfPresent(
                new ZToAKey("4"),
                (zToAKey, zToA) -> new ZToABuilder(zToA)
                        .setResource(new ResourceBuilder(zToA.getResource())
                                .setState(State.OutOfService)
                                .build())
                        .build());
        assertEquals(ztoamapExpected, networkModelListener.changePathElementStateZA(topologyChanges, pathDescription));
    }

    @Test
    void testChangePathElementStateAZShouldNotModifyPathDescriptionsElementStates() {
        Map<TopologyChangesKey, TopologyChanges> topologyChanges = Map.of(
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.InService)
                        .build(),
                new TopologyChangesKey("tpNodeIdA", "TpIdA2"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA2")
                        .setState(State.InService)
                        .build());

        assertEquals(
            pathDescription.getAToZDirection().getAToZ(),
            networkModelListener.changePathElementStateAZ(topologyChanges, pathDescription));
    }

    @Test
    void testChangePathElementStateAZShouldModifyPathDescriptionsElementStates() {
        Map<TopologyChangesKey, TopologyChanges> topologyChanges = Map.of(
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.OutOfService)
                        .build(),
                new TopologyChangesKey("tpNodeIdA", "TpIdA2"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA2")
                        .setState(State.OutOfService)
                        .build());

        Map<AToZKey, AToZ> atozmapExpected = pathDescription.getAToZDirection().getAToZ();
        atozmapExpected.computeIfPresent(
                new AToZKey("0"),
                (aToZKey, aToZ) -> new AToZBuilder(aToZ)
                        .setResource(new ResourceBuilder(aToZ.getResource())
                                .setState(State.OutOfService)
                                .build())
                        .build());
        atozmapExpected.computeIfPresent(
                new AToZKey("2"),
                (aToZKey, aToZ) -> new AToZBuilder(aToZ)
                        .setResource(new ResourceBuilder(aToZ.getResource())
                                .setState(State.OutOfService)
                                .build())
                        .build());
        assertEquals(atozmapExpected, networkModelListener.changePathElementStateAZ(topologyChanges, pathDescription));
    }

    @Test
    void testAllElementsinPathinServiceShouldReturnFalse() {
        Map<AToZKey, AToZ> atozmap = pathDescription.getAToZDirection().getAToZ();
        atozmap.computeIfPresent(
                new AToZKey("0"),
                (aToZKey, aToZ) -> new AToZBuilder(aToZ)
                        .setResource(new ResourceBuilder(aToZ.getResource())
                                .setState(State.OutOfService)
                                .build())
                        .build());
        Map<ZToAKey, ZToA> ztoamap = pathDescription.getZToADirection().getZToA();
        ztoamap.computeIfPresent(
                new ZToAKey("6"),
                (zToAKey, zToA) -> new ZToABuilder(zToA)
                        .setResource(new ResourceBuilder(zToA.getResource())
                                .setState(State.OutOfService)
                                .build())
                        .build());
        assertFalse(networkModelListener.allElementsinPathinService(atozmap, ztoamap));
    }

    @Test
    void testAllElementsinPathinServiceShouldReturnTrue() {
        assertTrue(networkModelListener.allElementsinPathinService(pathDescription.getAToZDirection().getAToZ(),
                pathDescription.getZToADirection().getZToA()));
    }

    @Test
    void testUpdateServicePathsShouldNotModifyServiceState() {
        Map<ServicePathsKey, ServicePaths> servicePathMap = Map.of(new ServicePathsKey("service-path 1"),
                new ServicePathsBuilder()
                        .setServicePathName("service-path 1")
                        .setPathDescription(pathDescription)
                        .build());

        when(serviceDataStoreOperations.getServicePaths())
            .thenReturn(Optional.of(new ServicePathListBuilder().setServicePaths(servicePathMap).build()));
        when(serviceDataStoreOperations.modifyServicePath(any(PathDescription.class), anyString()))
            .thenReturn(OperationResult.ok(""));
        when(serviceDataStoreOperations.getService(anyString()))
            .thenReturn(Optional.of(new ServicesBuilder()
                    .setServiceName("serviceTest")
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build()));

        Map<TopologyChangesKey, TopologyChanges> topologyChanges = Map.of(
                new TopologyChangesKey("tpNodeIdC", "TpIdC1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdC")
                        .setTpId("TpIdC1")
                        .setState(State.OutOfService)
                        .build(),
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.InService)
                        .build());

        networkModelListener.updateServicePaths(new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChanges).build());
        verify(serviceDataStoreOperations, never())
            .modifyService(anyString(), any(State.class), any(AdminStates.class));
    }

    @Test
    void testUpdateServicePathsShouldModifyServiceState() {
        Map<ServicePathsKey, ServicePaths> servicePathMap = Map.of(
                new ServicePathsKey("service-path 1"),
                new ServicePathsBuilder()
                        .setServicePathName("service-path 1")
                        .setPathDescription(pathDescription)
                        .build());

        when(serviceDataStoreOperations.getServicePaths())
            .thenReturn(Optional.of(new ServicePathListBuilder().setServicePaths(servicePathMap).build()));
        when(serviceDataStoreOperations.modifyServicePath(any(PathDescription.class), anyString()))
            .thenReturn(OperationResult.ok(""));
        when(serviceDataStoreOperations.getService(anyString()))
            .thenReturn(Optional.of(new ServicesBuilder()
                    .setServiceName("serviceTest")
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build()));
        when(serviceDataStoreOperations.modifyService(anyString(), any(State.class), any(AdminStates.class)))
            .thenReturn(OperationResult.ok(""));

        Map<TopologyChangesKey, TopologyChanges> topologyChanges = Map.of(
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.OutOfService)
                        .build());

        networkModelListener.updateServicePaths(new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChanges).build());
        verify(serviceDataStoreOperations, times(1))
            .modifyService(anyString(), eq(State.OutOfService), any(AdminStates.class));
    }

    @Test
    void testOnTopologyUpdateResultWhenNeverWired() {
        NetworkModelNotificationHandler networkModelListenerMocked = Mockito.mock(
            NetworkModelNotificationHandler.class);
        doCallRealMethod().when(networkModelListenerMocked).onTopologyUpdateResult(any(TopologyUpdateResult.class));

        Map<TopologyChangesKey, TopologyChanges> topologyChanges1 = Map.of(
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.OutOfService)
                        .build());
        Map<TopologyChangesKey, TopologyChanges> topologyChanges2 = Map.of(
                new TopologyChangesKey("tpNodeIdC", "TpIdC1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdC")
                        .setTpId("TpIdC1")
                        .setState(State.OutOfService)
                        .build());

        networkModelListenerMocked.onTopologyUpdateResult(new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChanges1).build());
        networkModelListenerMocked.onTopologyUpdateResult(new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChanges2).build());
        verify(networkModelListenerMocked, times(2)).updateServicePaths(any(TopologyUpdateResult.class));
    }

    @Test
    void testOnTopologyUpdateResultWhenAlreadyWired() {
        NetworkModelNotificationHandler networkModelListenerMocked = Mockito.mock(
            NetworkModelNotificationHandler.class);
        doCallRealMethod().when(networkModelListenerMocked).onTopologyUpdateResult(any(TopologyUpdateResult.class));

        Map<TopologyChangesKey, TopologyChanges> topologyChanges = Map.of(
                new TopologyChangesKey("tpNodeIdA", "TpIdA1"),
                new TopologyChangesBuilder()
                        .setNodeId("tpNodeIdA")
                        .setTpId("TpIdA1")
                        .setState(State.OutOfService)
                        .build());
        TopologyUpdateResult topologyUpdateResult = new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChanges).build();

        networkModelListenerMocked.onTopologyUpdateResult(topologyUpdateResult);
        networkModelListenerMocked.onTopologyUpdateResult(topologyUpdateResult);
        verify(networkModelListenerMocked, times(1)).updateServicePaths(any(TopologyUpdateResult.class));
    }

    private Map<AToZKey, AToZ> createMapAtoZ() {
        Map<AToZKey, AToZ> atozmap = new HashMap<>();
        atozmap.put(
                new AToZKey("0"),
                new AToZBuilder()
                        .setId("0")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdA")
                                        .setTpId("TpIdA1")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        atozmap.put(
                new AToZKey("1"),
                new AToZBuilder()
                        .setId("1")
                        .setResource(new ResourceBuilder()
                                .setResource(new NodeBuilder()
                                        .setNodeId("NodeIdA")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        atozmap.put(
                new AToZKey("2"),
                new AToZBuilder()
                        .setId("2")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdA")
                                        .setTpId("TpIdA2")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        atozmap.put(
                new AToZKey("3"),
                new AToZBuilder()
                        .setId("3")
                        .setResource(new ResourceBuilder()
                                .setResource(new LinkBuilder()
                                        .setLinkId("LinkIdAZ")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        atozmap.put(
                new AToZKey("4"),
                new AToZBuilder()
                        .setId("4")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdZ")
                                        .setTpId("TpIdZ2")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        atozmap.put(
                new AToZKey("5"),
                new AToZBuilder()
                        .setId("5")
                        .setResource(new ResourceBuilder()
                                .setResource(new NodeBuilder()
                                        .setNodeId("NodeIdZ")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        atozmap.put(
                new AToZKey("6"),
                new AToZBuilder()
                        .setId("6")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdZ")
                                        .setTpId("TpIdZ1")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        return atozmap;
    }

    private Map<ZToAKey, ZToA> createMapZtoA() {
        Map<ZToAKey, ZToA> ztoamap = new HashMap<>();
        ztoamap.put(
                new ZToAKey("0"),
                new ZToABuilder()
                        .setId("0")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdZ")
                                        .setTpId("TpIdZ1")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        ztoamap.put(
                new ZToAKey("1"),
                new ZToABuilder()
                        .setId("1")
                        .setResource(new ResourceBuilder()
                                .setResource(new NodeBuilder()
                                        .setNodeId("NodeIdZ")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        ztoamap.put(
                new ZToAKey("2"),
                new ZToABuilder()
                        .setId("2")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdZ")
                                        .setTpId("TpIdZ2")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        ztoamap.put(
                new ZToAKey("3"),
                new ZToABuilder()
                        .setId("3")
                        .setResource(new ResourceBuilder()
                                .setResource(new LinkBuilder()
                                        .setLinkId("LinkIdAZ")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        ztoamap.put(
                new ZToAKey("4"),
                new ZToABuilder()
                        .setId("4")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdA")
                                        .setTpId("TpIdA2")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        ztoamap.put(
                new ZToAKey("5"),
                new ZToABuilder()
                        .setId("5")
                        .setResource(new ResourceBuilder()
                                .setResource(new NodeBuilder()
                                        .setNodeId("NodeIdA")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        ztoamap.put(
                new ZToAKey("6"),
                new ZToABuilder()
                        .setId("6")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdA")
                                        .setTpId("TpIdA1")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build());
        return ztoamap;
    }
}
