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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.otn.renderer.input.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossConnectImpl implements CrossConnect {

    private static final Logger LOG = LoggerFactory.getLogger(CrossConnectImpl.class);

    protected CrossConnect crossConnect;
    private final MappingUtils mappingUtils;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl221;
    private CrossConnectImpl710 crossConnectImpl710;

    public CrossConnectImpl(DeviceTransactionManager deviceTransactionManager, MappingUtils mappingUtils,
                            CrossConnectImpl121 crossConnectImpl121,
                            CrossConnectImpl221 crossConnectImpl221,
                            CrossConnectImpl710 crossConnectImpl710) {
        this.mappingUtils = mappingUtils;
        this.crossConnectImpl121 = crossConnectImpl121;
        this.crossConnectImpl221 = crossConnectImpl221;
        this.crossConnectImpl710 = crossConnectImpl710;
        this.crossConnect = null;
    }

    public Optional<?> getCrossConnect(String nodeId, String connectionNumber) {
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        if (OPENROADM_DEVICE_VERSION_1_2_1.equals(openRoadmVersion)) {
            return crossConnectImpl121.getCrossConnect(nodeId,connectionNumber);
        }
        else if (OPENROADM_DEVICE_VERSION_2_2_1.equals(openRoadmVersion)) {
            return crossConnectImpl221.getCrossConnect(nodeId,connectionNumber);
        }
        return Optional.empty();
    }


    public Optional<String> postCrossConnect(String nodeId, String srcTp, String destTp,
            SpectrumInformation spectrumInformation) {
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        LOG.info("Cross Connect post request received for node {} with version {}",nodeId,openRoadmVersion);
        if (OPENROADM_DEVICE_VERSION_1_2_1.equals(openRoadmVersion)) {
            LOG.info("Device Version is 1.2.1");
            return crossConnectImpl121.postCrossConnect(nodeId, srcTp, destTp, spectrumInformation);
        }
        else if (OPENROADM_DEVICE_VERSION_2_2_1.equals(openRoadmVersion)) {
            LOG.info("Device Version is 2.2");
            return crossConnectImpl221.postCrossConnect(nodeId, srcTp, destTp,
                    spectrumInformation);
        }
        LOG.info("Device Version not found");
        return Optional.empty();

    }


    public List<String> deleteCrossConnect(String nodeId, String connectionNumber, Boolean isOtn) {

        switch(mappingUtils.getOpenRoadmVersion(nodeId)) {
            case OPENROADM_DEVICE_VERSION_1_2_1:
                return crossConnectImpl121.deleteCrossConnect(nodeId, connectionNumber);
            case OPENROADM_DEVICE_VERSION_2_2_1:
                return crossConnectImpl221.deleteCrossConnect(nodeId, connectionNumber, isOtn);
            case OPENROADM_DEVICE_VERSION_7_1:
                return crossConnectImpl710.deleteOtnCrossConnect(nodeId, connectionNumber);
            default:
                return null;
        }
    }

    public List<?> getConnectionPortTrail(String nodeId, String srcTp, String destTp, int lowerSpectralSlotNumber,
            int higherSpectralSlotNumber)
            throws OpenRoadmInterfaceException {
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        if (OPENROADM_DEVICE_VERSION_1_2_1.equals(openRoadmVersion)) {
            return crossConnectImpl121.getConnectionPortTrail(nodeId, srcTp, destTp,
                    lowerSpectralSlotNumber, higherSpectralSlotNumber);
        }
        else if (OPENROADM_DEVICE_VERSION_2_2_1.equals(openRoadmVersion)) {
            return crossConnectImpl221
                    .getConnectionPortTrail(nodeId, srcTp, destTp, lowerSpectralSlotNumber, higherSpectralSlotNumber);
        }
        return null;
    }

    public boolean setPowerLevel(String nodeId, String mode, BigDecimal powerValue, String connectionNumber) {
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        if (OPENROADM_DEVICE_VERSION_1_2_1.equals(openRoadmVersion) && OpticalControlMode.forName(mode).isPresent()) {
            return crossConnectImpl121.setPowerLevel(nodeId,OpticalControlMode.forName(mode).get(),
                powerValue,connectionNumber);
        }
        else if (OPENROADM_DEVICE_VERSION_2_2_1.equals(openRoadmVersion)
            && org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.OpticalControlMode.forName(mode)
            .isPresent()) {
            return crossConnectImpl221.setPowerLevel(nodeId,
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.OpticalControlMode.forName(mode)
                .get(), powerValue,connectionNumber);
        }
        return false;
    }

    @Override
    public Optional<String> postOtnCrossConnect(List<String> createdOduInterfaces, Nodes node)
            throws OpenRoadmInterfaceException {
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(node.getNodeId());

        if (OPENROADM_DEVICE_VERSION_2_2_1.equals(openRoadmVersion)) {
            return crossConnectImpl221.postOtnCrossConnect(createdOduInterfaces, node);
        }
        else if (OPENROADM_DEVICE_VERSION_7_1.equals(openRoadmVersion)) {
            return crossConnectImpl710.postOtnCrossConnect(createdOduInterfaces, node);
        }
        return Optional.empty();
    }
}
