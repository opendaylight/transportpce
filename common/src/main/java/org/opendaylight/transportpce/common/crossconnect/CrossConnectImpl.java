/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_7_1;

import java.util.List;
import java.util.Optional;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.otn.renderer.nodes.Nodes;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CrossConnectImpl implements CrossConnect {

    private static final Logger LOG = LoggerFactory.getLogger(CrossConnectImpl.class);

    private final MappingUtils mappingUtils;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl221;
    private CrossConnectImpl710 crossConnectImpl710;

    @Activate
    public CrossConnectImpl(@Reference DeviceTransactionManager deviceTransactionManager,
                            @Reference MappingUtils mappingUtils) {
        this(deviceTransactionManager, mappingUtils,
            new CrossConnectImpl121(deviceTransactionManager),
            new CrossConnectImpl221(deviceTransactionManager),
            new CrossConnectImpl710(deviceTransactionManager));
    }

    // TODO: DeviceTransactionManager is not used here
    public CrossConnectImpl(DeviceTransactionManager deviceTransactionManager, MappingUtils mappingUtils,
                            CrossConnectImpl121 crossConnectImpl121,
                            CrossConnectImpl221 crossConnectImpl221,
                            CrossConnectImpl710 crossConnectImpl710) {
        this.mappingUtils = mappingUtils;
        this.crossConnectImpl121 = crossConnectImpl121;
        this.crossConnectImpl221 = crossConnectImpl221;
        this.crossConnectImpl710 = crossConnectImpl710;
    }

    public Optional<?> getCrossConnect(String nodeId, String connectionNumber) {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case OPENROADM_DEVICE_VERSION_1_2_1:
                return crossConnectImpl121.getCrossConnect(nodeId,connectionNumber);
            case OPENROADM_DEVICE_VERSION_2_2_1:
                return crossConnectImpl221.getCrossConnect(nodeId,connectionNumber);
            case OPENROADM_DEVICE_VERSION_7_1:
                return crossConnectImpl710.getCrossConnect(nodeId, connectionNumber);
            default:
                return Optional.empty();
        }
    }


    public Optional<String> postCrossConnect(String nodeId, String srcTp, String destTp,
            SpectrumInformation spectrumInformation) {
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        LOG.info("Cross Connect post request received for node {} with version {}", nodeId, openRoadmVersion);
        switch (openRoadmVersion) {
            case OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.info("Device Version is 1.2.1");
                return crossConnectImpl121.postCrossConnect(nodeId, srcTp, destTp, spectrumInformation);
            case OPENROADM_DEVICE_VERSION_2_2_1:
                LOG.info("Device Version is 2.2.1");
                return crossConnectImpl221.postCrossConnect(nodeId, srcTp, destTp, spectrumInformation);
            case OPENROADM_DEVICE_VERSION_7_1:
                LOG.info("Device Version is 7.1");
                return crossConnectImpl710.postCrossConnect(nodeId, srcTp, destTp, spectrumInformation);
            default:
                LOG.info("Device Version not found");
                return Optional.empty();
        }
    }


    public List<String> deleteCrossConnect(String nodeId, String connectionNumber, Boolean isOtn) {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case OPENROADM_DEVICE_VERSION_1_2_1:
                return crossConnectImpl121.deleteCrossConnect(nodeId, connectionNumber);
            case OPENROADM_DEVICE_VERSION_2_2_1:
                return crossConnectImpl221.deleteCrossConnect(nodeId, connectionNumber, isOtn);
            case OPENROADM_DEVICE_VERSION_7_1:
                return crossConnectImpl710.deleteCrossConnect(nodeId, connectionNumber, isOtn);
            default:
                return null;
        }
    }

    public List<?> getConnectionPortTrail(String nodeId, String srcTp, String destTp, int lowerSpectralSlotNumber,
            int higherSpectralSlotNumber)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case OPENROADM_DEVICE_VERSION_1_2_1:
                return crossConnectImpl121
                    .getConnectionPortTrail(nodeId, srcTp, destTp, lowerSpectralSlotNumber, higherSpectralSlotNumber);
            case OPENROADM_DEVICE_VERSION_2_2_1:
                return crossConnectImpl221
                    .getConnectionPortTrail(nodeId, srcTp, destTp, lowerSpectralSlotNumber, higherSpectralSlotNumber);
            case OPENROADM_DEVICE_VERSION_7_1:
                return crossConnectImpl710
                    .getConnectionPortTrail(nodeId, srcTp, destTp, lowerSpectralSlotNumber, higherSpectralSlotNumber);
            default:
                return null;
        }
    }

    @Override
    public boolean setPowerLevel(String nodeId, String mode, Decimal64 powerValue, String connectionNumber) {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case OPENROADM_DEVICE_VERSION_1_2_1:
                if (OpticalControlMode.forName(mode) == null) {
                    return false;
                }
                return crossConnectImpl121.setPowerLevel(nodeId,
                    OpticalControlMode.forName(mode),
                    powerValue, connectionNumber);
            case OPENROADM_DEVICE_VERSION_2_2_1:
                if (org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.OpticalControlMode
                        .forName(mode) == null) {
                    return false;
                }
                return crossConnectImpl221.setPowerLevel(nodeId,
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.OpticalControlMode
                        .forName(mode),
                    powerValue, connectionNumber);
            case OPENROADM_DEVICE_VERSION_7_1:
                return crossConnectImpl710.setPowerLevel(nodeId,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.OpticalControlMode
                        .forName(mode),
                    powerValue, connectionNumber);
            default:
                return false;
        }
    }

    @Override
    public Optional<String> postOtnCrossConnect(List<String> createdOduInterfaces, Nodes node)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(node.getNodeId())) {
            case OPENROADM_DEVICE_VERSION_2_2_1:
                return crossConnectImpl221.postOtnCrossConnect(createdOduInterfaces, node);
            case OPENROADM_DEVICE_VERSION_7_1:
                return crossConnectImpl710.postOtnCrossConnect(createdOduInterfaces, node);
            default:
                return Optional.empty();
        }
    }
}
