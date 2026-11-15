/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.tapisbi.utils.TapiSbiHttpClient;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiController;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.common.Uint16;

@ExtendWith(MockitoExtension.class)
class TapiSbiDiscoveryServiceImplTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private TapiSbiControllerService controllerService;
    @Mock
    private TransformationService transformationService;
    @Mock
    private TapiSbiController controller;

    private TapiSbiDiscoveryServiceImpl discoveryService;

    @BeforeEach
    void setup() {
        discoveryService = new TapiSbiDiscoveryServiceImpl(dataBroker, controllerService, transformationService);
    }

    private void stubController() {
        when(controller.getControllerId()).thenReturn("ctrl-1");
        when(controller.getHost()).thenReturn(new Host(new IpAddress(new Ipv4Address("192.168.1.1"))));
        when(controller.getPort()).thenReturn(new PortNumber(Uint16.valueOf(8182)));
        when(controller.getUsername()).thenReturn("admin");
        when(controller.getPassword()).thenReturn("encryptedSecret");
        when(controllerService.decryptPassword("encryptedSecret")).thenReturn("secret");
    }

    private void stubCleanup() {
        when(transformationService.cleanupTopology("ctrl-1")).thenReturn(Futures.immediateFuture(null));
        when(transformationService.cleanupProfiles("ctrl-1")).thenReturn(Futures.immediateFuture(null));
        when(transformationService.cleanupServiceInterfacePoints("ctrl-1"))
            .thenReturn(Futures.immediateFuture(null));
    }

    @Test
    void testDiscoverControllerHttpFails() {
        stubController();
        try (MockedStatic<TapiSbiHttpClient> httpClient = mockStatic(TapiSbiHttpClient.class)) {
            httpClient.when(() -> TapiSbiHttpClient.executeGet(anyString(), anyString(), anyString()))
                .thenReturn(null);

            assertThrows(IllegalStateException.class, () -> discoveryService.discoverController(controller));
        }
    }

    @Test
    void testDiscoverControllerContinuesWhenNodeListIsAbsent() {
        stubController();
        try (MockedStatic<TapiSbiHttpClient> httpClient = mockStatic(TapiSbiHttpClient.class)) {
            httpClient.when(() -> TapiSbiHttpClient.executeGet(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    return switch (url) {
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context?fields=topology(uuid)" ->
                            topologyUuidResponse();
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context/topology=topo-1?fields=uuid;name" ->
                            topologyDetailsResponse();
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context/topology=topo-1?fields=node(uuid)" ->
                            topologyResponseWithout("node");
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context/topology=topo-1?fields=link(uuid)" ->
                            topologyResponseWithout("link");
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context?fields=profile(uuid)" ->
                            emptyContextResponse("profile");
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context?fields=service-interface-point(uuid)" ->
                            emptyContextResponse("service-interface-point");
                        default -> throw new AssertionError("Unexpected URL: " + url);
                    };
                });

            assertDoesNotThrow(() -> discoveryService.discoverController(controller));
        }
    }

    @Test
    void testDiscoverControllerContinuesWhenLinkListIsAbsent() {
        stubController();
        try (MockedStatic<TapiSbiHttpClient> httpClient = mockStatic(TapiSbiHttpClient.class)) {
            httpClient.when(() -> TapiSbiHttpClient.executeGet(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    return switch (url) {
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context?fields=topology(uuid)" ->
                            topologyUuidResponse();
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context/topology=topo-1?fields=uuid;name" ->
                            topologyDetailsResponse();
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context/topology=topo-1?fields=node(uuid)" ->
                            topologyResponseWithout("node");
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context/tapi-topology:topology-context/topology=topo-1?fields=link(uuid)" ->
                            topologyResponseWithout("link");
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context?fields=profile(uuid)" ->
                            emptyContextResponse("profile");
                        case "http://192.168.1.1:8182/rests/data/tapi-common:context?fields=service-interface-point(uuid)" ->
                            emptyContextResponse("service-interface-point");
                        default -> throw new AssertionError("Unexpected URL: " + url);
                    };
                });

            assertDoesNotThrow(() -> discoveryService.discoverController(controller));
        }
    }

    @Test
    void testDiscoverControllerFailsWhenT0TopologyIsMissing() {
        stubController();
        try (MockedStatic<TapiSbiHttpClient> httpClient = mockStatic(TapiSbiHttpClient.class)) {
            httpClient.when(() -> TapiSbiHttpClient.executeGet(anyString(), anyString(), anyString()))
                .thenReturn(topologyUuidResponse());

            assertThrows(IllegalStateException.class, () -> discoveryService.discoverController(controller));
        }
    }

    @Test
    void testRediscoverControllerConfigNotFound() {
        when(controllerService.getTapiSbiControllerConfig("ctrl-missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
            () -> discoveryService.rediscoverController("ctrl-missing"));

        verify(transformationService, never()).cleanupTopology(anyString());
    }

    @Test
    void testRediscoverControllerCallsCleanupThenDiscover() {
        stubController();
        stubCleanup();
        when(controllerService.getTapiSbiControllerConfig("ctrl-1")).thenReturn(controller);

        try (MockedStatic<TapiSbiHttpClient> httpClient = mockStatic(TapiSbiHttpClient.class)) {
            httpClient.when(() -> TapiSbiHttpClient.executeGet(anyString(), anyString(), anyString()))
                .thenReturn(null);

            assertThrows(IllegalStateException.class, () -> discoveryService.rediscoverController("ctrl-1"));
        }
        verify(transformationService, atLeast(2)).cleanupTopology("ctrl-1");
    }

    private JsonObject topologyUuidResponse() {
        var topology = new JsonObject();
        topology.addProperty("uuid", "topo-1");
        var topologies = new JsonArray();
        topologies.add(topology);
        var topologyContext = new JsonObject();
        topologyContext.add("topology", topologies);
        var response = new JsonObject();
        response.add("tapi-topology:topology-context", topologyContext);
        return response;
    }

    private JsonObject topologyDetailsResponse() {
        var name = new JsonObject();
        name.addProperty("value", "T0-Topology");
        var names = new JsonArray();
        names.add(name);
        var topology = new JsonObject();
        topology.add("name", names);
        var topologyArray = new JsonArray();
        topologyArray.add(topology);
        var response = new JsonObject();
        response.add("tapi-topology:topology", topologyArray);
        return response;
    }

    private JsonObject topologyResponseWithout(String omittedCollection) {
        var topology = new JsonObject();
        if (!"node".equals(omittedCollection)) {
            topology.add("node", new JsonArray());
        }
        if (!"link".equals(omittedCollection)) {
            topology.add("link", new JsonArray());
        }
        var topologies = new JsonArray();
        topologies.add(topology);
        var response = new JsonObject();
        response.add("tapi-topology:topology", topologies);
        return response;
    }

    private JsonObject emptyContextResponse(String collection) {
        var context = new JsonObject();
        context.add(collection, new JsonArray());
        var response = new JsonObject();
        response.add("tapi-common:context", context);
        return response;
    }
}
