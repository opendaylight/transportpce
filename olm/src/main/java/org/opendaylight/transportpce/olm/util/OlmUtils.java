/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;

import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OlmUtils {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils.class);

    private OlmUtils() {
    }

    /**
     * This method retrieves list of current PMs for given nodeId,
     * resourceType, resourceName and Granularity. Currently vendorExtentions
     * are excluded but can be added back based on requirement.
     *
     * <p>This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * @param input
     *            Input parameter from the olm yang model get-pm rpc
     * @param deviceTransactionManager
     *            Device tx manager
     * @param openRoadmVersion
     *            OpenRoadm version number
     *
     * @return Result of the request list of PM readings
     */
    public static GetPmOutputBuilder pmFetch(GetPmInput input, DeviceTransactionManager deviceTransactionManager,
                                             OpenroadmNodeVersion openRoadmVersion) {
        LOG.info("Getting PM Data for NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
            input.getResourceType(), input.getResourceIdentifier());
        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();
        List<Measurements> measurements = new ArrayList<>();
        switch (openRoadmVersion) {
            case OpenroadmNodeVersion._121:
                measurements = OlmUtils121.pmFetch(input, deviceTransactionManager);
                break;
            case OpenroadmNodeVersion._221:
                measurements = OlmUtils221.pmFetch(input, deviceTransactionManager);
                break;
            case OpenroadmNodeVersion._71:
                measurements = OlmUtils710.pmFetch(input, deviceTransactionManager);
                break;
            default:
                LOG.error("Unsupported OpenRoadm version {}", openRoadmVersion.getIntValue());
                pmOutputBuilder = new GetPmOutputBuilder();
        }
        if (measurements.isEmpty()) {
            logMeasurementError(input);
        } else {
            pmOutputBuilder.setNodeId(input.getNodeId()).setResourceType(input.getResourceType())
                    .setResourceIdentifier(input.getResourceIdentifier()).setGranularity(input
                            .getGranularity()).setMeasurements(measurements);
            logMeasurementSuccess(input);
        }
        return pmOutputBuilder;
    }

    /**
     * This method retrieves list of current PMs for given nodeId,
     * resourceType, resourceName and Granularity. Currently vendorExtentions
     * are excluded but can be added back based on requirement.
     *
     * <p>This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * @param input
     *            Input parameter from the olm yang model get-pm rpc
     * @param deviceTransactionManager
     *            Device tx manager
     * @param openRoadmVersion
     *            OpenRoadm version number
     *
     * @return Result of the request list of PM readings
     */
    public static Map<String, List<GetPmOutput>> pmFetchAll(GetPmInput input,
                                                            DeviceTransactionManager deviceTransactionManager,
                                                            OpenroadmNodeVersion openRoadmVersion) {
        LOG.info("Getting All PM Data for NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
                input.getResourceType(), input.getResourceIdentifier());
        Map<String, List<GetPmOutput>> pmOutputMap;
        switch (openRoadmVersion) {
            case OpenroadmNodeVersion._121:
                pmOutputMap = OlmUtils121.pmFetchAll(input, deviceTransactionManager);
                break;
            case OpenroadmNodeVersion._221:
                pmOutputMap = OlmUtils221.pmFetchAll(input, deviceTransactionManager);
                break;
            case OpenroadmNodeVersion._71:
                pmOutputMap = OlmUtils710.pmFetchAll(input, deviceTransactionManager);
                break;
            default:
                LOG.error("Unsupported OpenRoadm version {}", openRoadmVersion.getIntValue());
                pmOutputMap = new HashMap<>();
        }
        if (pmOutputMap.isEmpty()) {
            logMeasurementError(input);
        } else {
            logMeasurementSuccess(input);
        }
        return pmOutputMap;
    }

    public static boolean setSpanLoss(String realNodeId,
                                      String interfaceName,
                                      BigDecimal spanLoss,
                                      String direction,
                                      OpenroadmNodeVersion nodeVersion,
                                      OpenRoadmInterfaces openRoadmInterfaces) {
        LOG.info("Setting spanloss for node: {} - {}", realNodeId, interfaceName);
        switch (nodeVersion) {
            case OpenroadmNodeVersion._121:
                return OlmUtils121.setSpanLoss(realNodeId, interfaceName, spanLoss, direction,openRoadmInterfaces);
            case OpenroadmNodeVersion._221:
                return OlmUtils221.setSpanLoss(realNodeId, interfaceName, spanLoss, direction, openRoadmInterfaces);
            case OpenroadmNodeVersion._71:
                return OlmUtils710.setSpanLoss(realNodeId, interfaceName, spanLoss, direction, openRoadmInterfaces);
            default:
                LOG.error("Unsupported OpenRoadm version {}", nodeVersion.getIntValue());
                return false;
        }
    }

    private static void logMeasurementSuccess(GetPmInput input) {
        LOG.info(
                "PM data found successfully for node: {}, resource type: {}, resource name: {}, "
                        + "pm type: {}, extention: {}, location: {} and direction: {}",
                input.getNodeId(), input.getResourceType(),
                getResourceIdentifierAsString(input.getResourceIdentifier()),
                input.getPmNameType(), input.getPmExtension(), input.getLocation(),
                input.getDirection());
    }

    private static void logMeasurementError(GetPmInput input) {
        LOG.error(
                "No Matching PM data found for node: {}, resource type: {}, resource name: {}, "
                        + "pm type: {}, extention: {}, location: {} and direction: {}",
                input.getNodeId(), input.getResourceType(),
                getResourceIdentifierAsString(input.getResourceIdentifier()),
                input.getPmNameType(), input.getPmExtension(), input.getLocation(),
                input.getDirection());
    }

    private static String getResourceIdentifierAsString(ResourceIdentifier resourceIdentifier) {
        if (resourceIdentifier == null) {
            return "<no resource identifier>";
        } else if (Strings.isNullOrEmpty(resourceIdentifier.getCircuitPackName())) {
            return resourceIdentifier.getResourceName();
        } else {
            return resourceIdentifier.getResourceName() + ", circuit pack name: "
                    + resourceIdentifier.getCircuitPackName();
        }
    }
}
