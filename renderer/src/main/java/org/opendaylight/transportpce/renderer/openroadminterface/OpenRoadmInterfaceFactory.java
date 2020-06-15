/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.math.BigDecimal;
import java.util.List;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfaceFactory {

    private static final String OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG =
            "OTN funtions are not supported by Openroadm models 1.2.1";
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfaceFactory.class);
    private final MappingUtils mappingUtils;
    private final OpenRoadmInterface121 openRoadmInterface121;
    private final OpenRoadmInterface221 openRoadmInterface221;
    private final OpenRoadmOtnInterface221 openRoadmOtnInterface;

    public OpenRoadmInterfaceFactory(MappingUtils mappingUtils, OpenRoadmInterface121 openRoadmInterface121,
            OpenRoadmInterface221 openRoadmInterface221, OpenRoadmOtnInterface221 openRoadmOTNInterface) {
        this.mappingUtils = mappingUtils;
        this.openRoadmInterface121 = openRoadmInterface121;
        this.openRoadmInterface221 = openRoadmInterface221;
        this.openRoadmOtnInterface = openRoadmOTNInterface;
    }

    public String createOpenRoadmEthInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }
    }

    public List<String> createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber,
        BigDecimal centerFreq, BigDecimal slotWidth)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterface(nodeId, logicalConnPoint, waveNumber);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createFlexOCH(nodeId, logicalConnPoint, waveNumber,
                    centerFreq,slotWidth);
            default:
                return null;
        }
    }

    /**
     * This methods creates an OCH interface on the given termination point on
     * Roadm.
     *
     * @param nodeId           node ID
     * @param logicalConnPoint logical connection point
     * @param waveNumber       wavelength number of the OCH interface.
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */
    public List<String> createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterface(nodeId, logicalConnPoint, waveNumber);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createFlexOCH(nodeId, logicalConnPoint, waveNumber);
            default:
                return null;
        }
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber,
        OchAttributes.ModulationFormat format, BigDecimal centerFreq, BigDecimal slotWidth)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterface(nodeId, logicalConnPoint, waveNumber, format);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOchInterface(nodeId, logicalConnPoint, waveNumber,
                   centerFreq, slotWidth);
            default:
                return null;
        }
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber,
        OchAttributes.ModulationFormat format)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterface(nodeId, logicalConnPoint, waveNumber, format);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOchInterface(nodeId, logicalConnPoint, waveNumber);
            default:
                return null;
        }
    }

    /**
     * This methods creates an ODU interface on the given termination point.
     *
     * @param nodeId                 node ID
     * @param logicalConnPoint       logical connection point
     * @param supportingOtuInterface supporting OTU interface
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOdu4Interface(nodeId, logicalConnPoint,
                        supportingOtuInterface);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOdu4Interface(nodeId, logicalConnPoint,
                        supportingOtuInterface);
            default:
                return null;
        }
    }

    /**
     * This methods creates an ODU interface on the given termination point.
     *
     * @param anodeId                 node ID for A side
     * @param alogicalConnPoint       logical connection point for A side
     * @param asupportingOtuInterface supporting OTU interface
     * @param znodeId                 node ID for Z side
     * @param zlogicalConnPoint       logical connection point for Z side
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public String createOpenRoadmOdu4Interface(String anodeId, String alogicalConnPoint, String asupportingOtuInterface,
        String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(anodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOdu4Interface(anodeId, alogicalConnPoint,
                    asupportingOtuInterface);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOdu4Interface(anodeId, alogicalConnPoint,
                    asupportingOtuInterface, znodeId, zlogicalConnPoint);
            default:
                return null;
        }
    }

    /**
     * This methods creates an OTU interface on the given termination point.
     *
     * @param nodeId              node ID
     * @param logicalConnPoint    logical connection point
     * @param supportOchInterface supporting OCH interface
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121
                        .createOpenRoadmOtu4Interface(nodeId, logicalConnPoint, supportOchInterface);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221
                        .createOpenRoadmOtu4Interface(nodeId, logicalConnPoint, supportOchInterface);
            default:
                return null;
        }
    }

    /**
     * This methods creates an OTU interface on the given termination point.
     *
     * @param anodeId node ID for A side
     * @param alogicalConnPoint logical connection point for A side
     * @param asupportOchInterface supporting OCH interface
     * @param znodeId node ID for the Z side
     * @param zlogicalConnPoint logical connection point for Z side
     * @return Name of the interface if successful, otherwise return null.
     *
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public String createOpenRoadmOtu4Interface(String anodeId, String alogicalConnPoint, String asupportOchInterface,
        String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(anodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOtu4Interface(anodeId,
                        alogicalConnPoint, asupportOchInterface);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOtu4Interface(anodeId, alogicalConnPoint,
                        asupportOchInterface, znodeId, zlogicalConnPoint);
            default:
                return null;
        }
    }

    public String createOpenRoadmOchInterfaceName(String logicalConnectionPoint, Long waveNumber) {
        return logicalConnectionPoint + "-" + waveNumber;
    }

    public String createOpenRoadmOmsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOmsInterface(nodeId, mapping);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOmsInterface(nodeId, mapping);
            default:
                return null;
        }
    }

    public String createOpenRoadmOtsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOtsInterface(nodeId, mapping);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOtsInterface(nodeId, mapping);
            default:
                return null;
        }
    }

    public boolean isUsedByXc(String nodeId, String interfaceName, String xc,
                              DeviceTransactionManager deviceTransactionManager) {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.isUsedByXc(nodeId, interfaceName, xc, deviceTransactionManager);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.isUsedByXc(nodeId, interfaceName, xc, deviceTransactionManager);
            default:
                return false;
        }
    }

    public boolean isUsedByOtnXc(String nodeId, String interfaceName, String xc,
        DeviceTransactionManager deviceTransactionManager) {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return false;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.isUsedByOtnXc(nodeId, interfaceName, xc, deviceTransactionManager);
            default:
                return false;
        }
    }

    public String createOpenRoadmEth1GInterface(String nodeId,
                                                String logicalConnPoint) throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface.createOpenRoadmEth1GInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }
    }

    public String createOpenRoadmEth10GInterface(String nodeId,
                                                 String logicalConnPoint) throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface.createOpenRoadmEth10GInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }

    }

    public String createOpenRoadmOdu0Interface(String nodeId, String logicalConnPoint, String servicename,
        String payLoad, boolean isNetworkPort, int tribPortNumber, int tribSlot)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface.createOpenRoadmOdu0Interface(
                    nodeId, logicalConnPoint, servicename, payLoad, isNetworkPort, tribPortNumber, tribSlot);
            default:
                return null;
        }
    }

    public String createOpenRoadmOdu2Interface(String nodeId, String logicalConnPoint, String servicename,
        String payLoad, boolean isNetworkPort, int tribPortNumber, int tribSlotIndex)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface.createOpenRoadmOdu2Interface(
                    nodeId, logicalConnPoint, servicename, payLoad, isNetworkPort, tribPortNumber, tribSlotIndex);
            default:
                return null;
        }
    }

    public String createOpenRoadmOdu2eInterface(String nodeId, String logicalConnPoint, String servicename,
        String payLoad, boolean isNetworkPort, int tribPortNumber, int tribSlotIndex)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface.createOpenRoadmOdu2eInterface(
                        nodeId, logicalConnPoint, servicename, payLoad, isNetworkPort, tribPortNumber, tribSlotIndex);
            default:
                return null;
        }

    }

    public String createOpenRoadmOtnOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221
                        .createOpenRoadmOtnOdu4Interface(nodeId, logicalConnPoint, supportingOtuInterface);
            default:
                return null;
        }
    }

    public String createOpenRoadmOtnOdu4Interface(String anodeId, String alogicalConnPoint,
        String asupportingOtuInterface, String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(anodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221
                    .createOpenRoadmOtnOdu4Interface(anodeId, alogicalConnPoint, asupportingOtuInterface,
                        znodeId, zlogicalConnPoint);
            default:
                return null;
        }
    }
}
