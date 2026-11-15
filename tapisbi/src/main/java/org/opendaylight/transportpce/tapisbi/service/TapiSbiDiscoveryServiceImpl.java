/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.tapisbi.utils.TapiSbiHttpClient;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiController;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of TAPI SBI controller discovery.
 * Discovers topology and TAPI common-context data from southbound controllers.
 */
@Component
public class TapiSbiDiscoveryServiceImpl implements TapiSbiDiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(
        TapiSbiDiscoveryServiceImpl.class);

    private final DataBroker dataBroker;
    private final TapiSbiControllerService controllerService;
    private final TransformationService transformationService;

    @Activate
    public TapiSbiDiscoveryServiceImpl(@Reference DataBroker dataBroker,
            @Reference TapiSbiControllerService controllerService,
            @Reference TransformationService transformationService) {
        this.dataBroker = dataBroker;
        this.controllerService = controllerService;
        this.transformationService = transformationService;
        LOG.info("TapiSbiDiscoveryServiceImpl initialized");
    }

    @Override
    public void discoverController(TapiSbiController controller) {
        String controllerId = controller.getControllerId();
        String host = controller.getHost().stringValue();
        String port = controller.getPort().getValue().toString();
        String baseUrl = "http://" + host + ":" + port;
        String username = controller.getUsername();
        String password = controllerService.decryptPassword(controller.getPassword());

        LOG.info("Starting discovery for controller: {}, connecting to {}:{}", controllerId, host, port);
        discoverTopology(baseUrl, username, password, controllerId);
        discoverProfiles(baseUrl, username, password, controllerId);
        discoverServiceInterfacePoints(baseUrl, username, password, controllerId);
        LOG.info("Discovery completed successfully for controller: {}", controllerId);
    }

    @Override
    public void cleanupController(String controllerId) {
        LOG.info("Cleaning up controller: {}", controllerId);

        try {
            transformationService.cleanupTopology(controllerId).get();
            transformationService.cleanupProfiles(controllerId).get();
            transformationService.cleanupServiceInterfacePoints(controllerId).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cleanup interrupted for controller: " + controllerId, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Cleanup failed for controller: " + controllerId, e);
        }

        LOG.info("Cleanup completed for controller: {}", controllerId);
    }

    @Override
    public void rediscoverController(String controllerId) {
        LOG.info("Sync (re-discover) started for controller: {}", controllerId);

        TapiSbiController controller = controllerService.getTapiSbiControllerConfig(controllerId);
        if (controller == null) {
            LOG.error("Controller configuration not found for: {}", controllerId);
            throw new IllegalArgumentException("Controller configuration not found for: " + controllerId);
        }

        LOG.info("Removing old stale data for controller: {}", controllerId);
        cleanupController(controllerId);

        // If discovery fails partway, clean up any partial data that was written
        // so the controller is left in an empty (not stale) state.
        try {
            discoverController(controller);
            LOG.info("Sync completed successfully for controller: {}", controllerId);
        } catch (IllegalStateException | JsonParseException e) {
            LOG.error("Discovery failed during sync for controller: {}, cleaning up partial data",
                controllerId, e);
            cleanupController(controllerId);
            throw e;
        }
    }

    /**
     * Discovers topology from the southbound controller.
     * Makes REST call to TAPI Context API to retrieve topology information.
     */
    private void discoverTopology(String baseUrl, String username, String password, String controllerId) {
        LOG.info("Discovering topology for controller: {}", controllerId);

        String topologyUuidsUrl = baseUrl
            + "/rests/data/tapi-common:context/tapi-topology:topology-context?fields=topology(uuid)";
        LOG.debug("Fetching topology UUIDs from: {}", topologyUuidsUrl);

        JsonObject topologyUuidsResponse = TapiSbiHttpClient.executeGet(topologyUuidsUrl, username, password);
        if (topologyUuidsResponse == null) {
            String msg = "Failed to fetch topology UUIDs from controller: " + controllerId;
            LOG.error("Failed to fetch topology UUIDs from controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        String t0TopologyUuid = findT0TopologyUuid(baseUrl, username, password, topologyUuidsResponse);
        if (t0TopologyUuid == null) {
            String msg = "T0 topology not found for controller: " + controllerId;
            LOG.error("T0 topology not found for controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        LOG.info("Found T0 topology with UUID: {}", t0TopologyUuid);
        processNodes(baseUrl, username, password, t0TopologyUuid, controllerId);
        processLinks(baseUrl, username, password, t0TopologyUuid, controllerId);
        LOG.info("Topology discovery completed for controller: {}", controllerId);
    }

    /**
     * Finds the T0 topology UUID by iterating over all topologies and checking their names.
     */
    private String findT0TopologyUuid(String baseUrl, String username, String password,
            JsonObject topologyUuidsResponse) {
        JsonObject topologyContext = topologyUuidsResponse
            .getAsJsonObject("tapi-topology:topology-context");

        if (topologyContext == null || !topologyContext.has("topology")) {
            LOG.warn("No topology found in response");
            return null;
        }

        JsonArray topologies = topologyContext.getAsJsonArray("topology");

        for (JsonElement topoElement : topologies) {
            JsonObject topology = topoElement.getAsJsonObject();
            String uuid = topology.get("uuid").getAsString();

            // Fetch topology details to check name
            String topologyDetailsUrl = baseUrl
                + "/rests/data/tapi-common:context/tapi-topology:topology-context/topology="
                + uuid + "?fields=uuid;name";

            JsonObject topologyDetails = TapiSbiHttpClient.executeGet(
                topologyDetailsUrl, username, password);

            if (topologyDetails != null && isT0Topology(topologyDetails)) {
                return uuid;
            }
        }

        return null;
    }

    /**
     * Checks if the topology is a T0 topology by examining its name.
     */
    private boolean isT0Topology(JsonObject topologyDetails) {
        JsonArray topologyArray = topologyDetails.getAsJsonArray("tapi-topology:topology");
        if (topologyArray == null || topologyArray.isEmpty()) {
            return false;
        }

        JsonObject topology = topologyArray.get(0).getAsJsonObject();
        if (!topology.has("name")) {
            return false;
        }

        JsonArray names = topology.getAsJsonArray("name");
        for (JsonElement nameElement : names) {
            JsonObject nameObj = nameElement.getAsJsonObject();
            if (nameObj.has("value")) {
                String value = nameObj.get("value").getAsString();
                if (value.startsWith("T0-") || value.startsWith("T0 -")) {
                    LOG.info("Found T0 topology with name: {}", value);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Processes all nodes from the T0 topology.
     *
     * @param baseUrl the base URL of the bottom controller
     * @param username the username for authentication
     * @param password the password for authentication
     * @param topologyUuid the topology UUID (for reading nodes and as original-uuid reference)
     * @param controllerId the controller ID
     */
    private void processNodes(String baseUrl, String username, String password, String topologyUuid,
            String controllerId) {
        LOG.debug("Starting node processing for topology: {}", topologyUuid);
        String nodesUrl = baseUrl
            + "/rests/data/tapi-common:context/tapi-topology:topology-context/topology="
            + topologyUuid + "?fields=node(uuid)";
        LOG.debug("Fetching nodes from: {}", nodesUrl);

        JsonObject nodesResponse = TapiSbiHttpClient.executeGet(nodesUrl, username, password);

        if (nodesResponse == null) {
            String msg = "Failed to fetch nodes from T0 topology for controller: " + controllerId;
            LOG.error("Failed to fetch nodes from T0 topology for controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        JsonArray topologyArray = nodesResponse.getAsJsonArray("tapi-topology:topology");
        if (topologyArray == null || topologyArray.isEmpty()) {
            String msg = "No topology found in nodes response for controller: " + controllerId;
            LOG.error("No topology found in nodes response for controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        JsonObject topology = topologyArray.get(0).getAsJsonObject();
        if (!topology.has("node")) {
            LOG.error("No nodes found in T0 topology for controller: {}", controllerId);
            return;
        }

        JsonArray nodes = topology.getAsJsonArray("node");
        if (nodes == null || nodes.isEmpty()) {
            LOG.error("No nodes found in T0 topology for controller: {}", controllerId);
            return;
        }
        LOG.info("Found {} nodes in T0 topology", nodes.size());

        int nodeIndex = 0;
        for (JsonElement nodeElement : nodes) {
            nodeIndex++;
            JsonObject nodeUuidObj = nodeElement.getAsJsonObject();
            String nodeUuid = nodeUuidObj.get("uuid").getAsString();

            LOG.debug("Processing node {}/{}: {}", nodeIndex, nodes.size(), nodeUuid);

            String nodeDetailsUrl = baseUrl
                + "/rests/data/tapi-common:context/tapi-topology:topology-context/topology="
                + topologyUuid + "/node=" + nodeUuid;

            JsonObject nodeDetails = TapiSbiHttpClient.executeGet(
                nodeDetailsUrl, username, password);

            if (nodeDetails != null) {
                transformNode(nodeDetails, topologyUuid, controllerId);
            } else {
                throw new IllegalStateException("Failed to fetch details for node " + nodeUuid
                    + " for controller " + controllerId);
            }
        }
        LOG.info("Completed processing {} nodes", nodes.size());
    }

    /**
     * Transforms a node from TAPI format to SBI topology normalized node.
     *
     * @param nodeDetails JsonObject containing the complete node information
     * @param topologyUuid the topology UUID from bottom controller
     * @param controllerId the controller ID
     */
    private void transformNode(JsonObject nodeDetails, String topologyUuid, String controllerId) {
        String nodeJson = nodeDetails.toString();

        if (transformationService instanceof TransformationServiceImpl) {
            ((TransformationServiceImpl) transformationService).setControllerId(controllerId);
        }

        try {
            transformationService.updateNodeInSbiTopology(nodeJson, topologyUuid).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to transform node for topology " + topologyUuid
                + " and controller " + controllerId, e);
        }
    }

    /**
     * Processes all links from the T0 topology.
     *
     * @param baseUrl the base URL of the bottom controller
     * @param username the username for authentication
     * @param password the password for authentication
     * @param topologyUuid the topology UUID (for reading links and as original-uuid reference)
     * @param controllerId the controller ID
     */
    private void processLinks(String baseUrl, String username, String password,
            String topologyUuid,
            String controllerId) {
        LOG.debug("Starting link processing for topology: {}", topologyUuid);
        String linksUrl = baseUrl
            + "/rests/data/tapi-common:context/tapi-topology:topology-context/topology="
            + topologyUuid + "?fields=link(uuid)";
        LOG.debug("Fetching links from: {}", linksUrl);

        JsonObject linksResponse = TapiSbiHttpClient.executeGet(linksUrl, username, password);

        if (linksResponse == null) {
            String msg = "Failed to fetch links from T0 topology for controller: " + controllerId;
            LOG.error("Failed to fetch links from T0 topology for controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        JsonArray topologyArray = linksResponse.getAsJsonArray("tapi-topology:topology");
        if (topologyArray == null || topologyArray.isEmpty()) {
            String msg = "No topology found in links response for controller: " + controllerId;
            LOG.error("No topology found in links response for controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        JsonObject topology = topologyArray.get(0).getAsJsonObject();
        JsonArray links = topology.getAsJsonArray("link");

        if (links == null || links.isEmpty()) {
            LOG.error("No links found in T0 topology for controller: {}", controllerId);
            return;
        }

        LOG.info("Found {} links in T0 topology", links.size());

        int linkIndex = 0;
        for (JsonElement linkElement : links) {
            linkIndex++;
            JsonObject linkObj = linkElement.getAsJsonObject();
            String linkUuid = linkObj.get("uuid").getAsString();

            LOG.debug("Processing link {}/{}: {}", linkIndex, links.size(), linkUuid);

            String linkDetailsUrl = baseUrl
                + "/rests/data/tapi-common:context/tapi-topology:topology-context/topology="
                + topologyUuid + "/link=" + linkUuid;

            JsonObject linkDetails = TapiSbiHttpClient.executeGet(
                linkDetailsUrl, username, password);

            if (linkDetails != null) {
                LOG.debug("Calling transformLink for link {}/{}: {}", linkIndex, links.size(), linkUuid);
                transformLink(linkDetails, topologyUuid, controllerId);
                LOG.debug("Link {}/{} transformation completed", linkIndex, links.size());
            } else {
                throw new IllegalStateException("Failed to fetch details for link " + linkUuid
                    + " for controller " + controllerId);
            }
        }

        LOG.info("Completed processing {} links", links.size());
    }

    /**
     * Transforms a link from TAPI format to SBI topology normalized link.
     *
     * @param linkDetails JsonObject containing the complete link information
     * @param topologyUuid the topology UUID from bottom controller
     * @param controllerId the controller ID
     */
    private void transformLink(JsonObject linkDetails, String topologyUuid, String controllerId) {
        LOG.debug("transformLink() called with topology UUID {} and controller {}", topologyUuid, controllerId);
        String linkJson = linkDetails.toString();
        if (transformationService instanceof TransformationServiceImpl) {
            ((TransformationServiceImpl) transformationService).setControllerId(controllerId);
        }
        try {
            transformationService.updateLinkInSbiTopology(linkJson, topologyUuid).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to transform link for topology " + topologyUuid
                + " and controller " + controllerId, e);
        }
    }

    /**
     * Discovers profiles from the southbound controller.
     * Makes REST call to TAPI Context API to retrieve profile information.
     */
    private void discoverProfiles(String baseUrl, String username, String password, String controllerId) {
        LOG.info("Discovering profiles for controller: {}", controllerId);

        String profileUuidsUrl = baseUrl + "/rests/data/tapi-common:context?fields=profile(uuid)";
        LOG.debug("Fetching profile UUIDs from: {}", profileUuidsUrl);

        JsonObject profileUuidsResponse = TapiSbiHttpClient.executeGet(profileUuidsUrl, username, password);
        if (profileUuidsResponse == null) {
            String msg = "Failed to fetch profile UUIDs from controller: " + controllerId;
            LOG.error("Failed to fetch profile UUIDs from controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        processProfiles(baseUrl, username, password, profileUuidsResponse, controllerId);
        LOG.info("Profile discovery completed for controller: {}", controllerId);
    }

    /**
     * Processes all profiles from the context.
     */
    private void processProfiles(String baseUrl, String username, String password,
            JsonObject profileUuidsResponse, String controllerId) {
        JsonObject context = profileUuidsResponse.getAsJsonObject("tapi-common:context");

        if (context == null || !context.has("profile")) {
            LOG.info("No profiles found in context for controller: {}", controllerId);
            return;
        }

        JsonArray profiles = context.getAsJsonArray("profile");
        LOG.info("Found {} profiles in context", profiles.size());

        int profileIndex = 0;
        for (JsonElement profileElement : profiles) {
            profileIndex++;
            JsonObject profileUuidObj = profileElement.getAsJsonObject();
            String profileUuid = profileUuidObj.get("uuid").getAsString();
            LOG.debug("Processing profile {}/{}: {}", profileIndex, profiles.size(), profileUuid);
            String profileDetailsUrl = baseUrl + "/rests/data/tapi-common:context/profile=" + profileUuid;
            JsonObject profileDetails = TapiSbiHttpClient.executeGet(profileDetailsUrl, username, password);

            if (profileDetails != null) {
                LOG.debug("Calling transformProfile for profile {}/{}: {}", profileIndex, profiles.size(), profileUuid);
                transformProfile(profileDetails, controllerId);
                LOG.debug("Profile {}/{} transformation completed", profileIndex, profiles.size());
            } else {
                throw new IllegalStateException("Failed to fetch details for profile " + profileUuid
                    + " for controller " + controllerId);
            }
        }
        LOG.info("Completed processing {} profiles", profiles.size());
    }

    /**
     * Transforms a profile from TAPI format into the TAPI common context.
     */
    private void transformProfile(JsonObject profileDetails, String controllerId) {
        LOG.debug("transformProfile() called for controller {} ", controllerId);
        String profileJson = profileDetails.toString();

        if (transformationService instanceof TransformationServiceImpl) {
            ((TransformationServiceImpl) transformationService).setControllerId(controllerId);
        }

        try {
            transformationService.updateProfileInContext(profileJson).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to transform profile for controller " + controllerId, e);
        }
    }

    /**
     * Discovers service interface points from the southbound controller.
     * Makes REST call to TAPI Context API to retrieve SIP information.
     */
    private void discoverServiceInterfacePoints(
            String baseUrl, String username, String password, String controllerId) {
        LOG.info("Discovering service interface points for controller: {}", controllerId);

        String sipUuidsUrl = baseUrl + "/rests/data/tapi-common:context?fields=service-interface-point(uuid)";
        LOG.debug("Fetching SIP UUIDs from: {}", sipUuidsUrl);

        JsonObject sipUuidsResponse = TapiSbiHttpClient.executeGet(sipUuidsUrl, username, password);
        if (sipUuidsResponse == null) {
            String msg = "Failed to fetch SIP UUIDs from controller: " + controllerId;
            LOG.error("Failed to fetch SIP UUIDs from controller: {}", controllerId);
            throw new IllegalStateException(msg);
        }

        processSips(baseUrl, username, password, sipUuidsResponse, controllerId);
        LOG.info("SIP discovery completed for controller: {}", controllerId);
    }

    /**
     * Processes all service interface points from the context.
     */
    private void processSips(String baseUrl, String username, String password,
            JsonObject sipUuidsResponse, String controllerId) {
        JsonObject context = sipUuidsResponse.getAsJsonObject("tapi-common:context");

        if (context == null || !context.has("service-interface-point")) {
            LOG.info("No SIPs found in context for controller: {}", controllerId);
            return;
        }

        JsonArray sips = context.getAsJsonArray("service-interface-point");
        LOG.info("Found {} SIPs in context", sips.size());

        int sipIndex = 0;
        for (JsonElement sipElement : sips) {
            sipIndex++;
            JsonObject sipUuidObj = sipElement.getAsJsonObject();
            String sipUuid = sipUuidObj.get("uuid").getAsString();

            LOG.debug("Processing SIP {}/{}: {}", sipIndex, sips.size(), sipUuid);

            String sipDetailsUrl = baseUrl + "/rests/data/tapi-common:context/service-interface-point=" + sipUuid;
            JsonObject sipDetails = TapiSbiHttpClient.executeGet(sipDetailsUrl, username, password);

            if (sipDetails != null) {
                LOG.debug("Calling transformSip for SIP {}/{}: {}", sipIndex, sips.size(), sipUuid);
                transformSip(sipDetails, controllerId);
                LOG.debug("SIP {}/{} transformation completed", sipIndex, sips.size());
            } else {
                throw new IllegalStateException("Failed to fetch details for SIP " + sipUuid
                    + " for controller " + controllerId);
            }
        }
        LOG.info("Completed processing {} SIPs", sips.size());
    }

    /**
     * Transforms a service interface point from TAPI format into the TAPI common context.
     */
    private void transformSip(JsonObject sipDetails, String controllerId) {
        LOG.debug("transformSip() called for controller {} ", controllerId);
        String sipJson = sipDetails.toString();

        if (transformationService instanceof TransformationServiceImpl) {
            ((TransformationServiceImpl) transformationService).setControllerId(controllerId);
        }

        try {
            transformationService.updateServiceInterfacePointInContext(sipJson).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to transform SIP for controller " + controllerId, e);
        }
    }
}
