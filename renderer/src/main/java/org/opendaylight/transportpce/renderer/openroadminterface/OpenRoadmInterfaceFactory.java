/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.util.List;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OperationalModeType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.AEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.ZEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev250110.OpucnTribSlotDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfaceFactory {

    private static final String OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG =
            "OTN functions are not supported by Openroadm models 1.2.1";
    private static  final String OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_2_2_1_MSG =
            "OTN functions are not supported by Openroadm models 2.2.1";
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfaceFactory.class);
    private final MappingUtils mappingUtils;
    private final OpenRoadmInterface121 openRoadmInterface121;
    private final OpenRoadmInterface221 openRoadmInterface221;
    private final OpenRoadmInterface710 openRoadmInterface710;
    private final OpenRoadmOtnInterface221 openRoadmOtnInterface221;
    private final OpenRoadmOtnInterface710 openRoadmOtnInterface710;

    public OpenRoadmInterfaceFactory(MappingUtils mappingUtils, PortMapping portMapping,
            OpenRoadmInterfaces openRoadmInterfaces) {
        this.mappingUtils = mappingUtils;
        this.openRoadmInterface121 = new OpenRoadmInterface121(portMapping, openRoadmInterfaces);
        this.openRoadmInterface221 = new OpenRoadmInterface221(portMapping, openRoadmInterfaces);
        this.openRoadmInterface710 = new OpenRoadmInterface710(portMapping, openRoadmInterfaces);
        this.openRoadmOtnInterface221 = new OpenRoadmOtnInterface221(portMapping, openRoadmInterfaces);
        this.openRoadmOtnInterface710 = new OpenRoadmOtnInterface710(portMapping, openRoadmInterfaces);
    }

    public String createOpenRoadmEthInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                // Below method differenciates 100G vs 400G based on supported-if-cap type
                return openRoadmInterface710.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }
    }

    public List<String> createOpenRoadmOchInterfaces(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterfaces(nodeId, logicalConnPoint,
                        spectrumInformation);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation);
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation);
            default:
                return null;
        }
    }


    /**
     * This methods creates an OCH interface on the given termination point on Roadm.
     *
     * @param nodeId           node ID
     * @param logicalConnPoint logical connection point
     * @param spectrumInformation spectrum information.
     * @param operationalMode operational mode name for the OCH/OTSI interface
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */
    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation, OperationalModeType operationalMode)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterface(nodeId, logicalConnPoint,
                        spectrumInformation);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOchInterface(nodeId, logicalConnPoint,
                        spectrumInformation);
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.createOpenRoadmOchOtsiOtsigroupInterface(nodeId, logicalConnPoint,
                    spectrumInformation, operationalMode);
            default:
                return null;
        }
    }

    /**
     * This methods creates an ODU interface on the given termination point.
     *
     * @param nodeId                 node ID
     * @param logicalConnPoint       logical connection point
     * @param isCTP                  to distinguish with a TTP odu interface
     * @param apiInfoA               sapi and dapi for A end of the service
     * @param apiInfoZ               sapi and dapi for Z end of the service
     * @param payloadType            payload type of the opu when terminated
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public String createOpenRoadmOdu4HOInterface(String nodeId, String logicalConnPoint, boolean isCTP,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ, String payloadType)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOdu4Interface(nodeId, logicalConnPoint);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOdu4HOInterface(nodeId, logicalConnPoint, isCTP,
                    apiInfoA, apiInfoZ, payloadType);
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.createOpenRoadmOdu4OducnOduflex(nodeId, logicalConnPoint,
                    apiInfoA, apiInfoZ);
            default:
                return null;
        }
    }

    public String createOpenRoadmOducn(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.createOpenRoadmOducnInterface(nodeId, logicalConnPoint);
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
     * @param apiInfoA            sapi and dapi for A end of the service
     * @param apiInfoZ            sapi and dapi for Z end of the service
     * @return Name of the interface if successful, otherwise return null.
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121
                        .createOpenRoadmOtu4Interface(nodeId, logicalConnPoint, supportOchInterface);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOtu4Interface(nodeId, logicalConnPoint, supportOchInterface,
                    apiInfoA, apiInfoZ);
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.createOpenRoadmOtu4OtucnInterface(nodeId, logicalConnPoint,
                    supportOchInterface, apiInfoA, apiInfoZ);
            default:
                return null;
        }
    }


    public String createOpenRoadmOchInterfaceName(String logicalConnectionPoint, String spectralSlotNumber) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,logicalConnectionPoint, spectralSlotNumber);
    }

    public String createOpenRoadmOmsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOmsInterface(nodeId, mapping);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface221.createOpenRoadmOmsInterface(nodeId, mapping);
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.createOpenRoadmOmsInterface(nodeId, mapping);
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
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.createOpenRoadmOtsInterface(nodeId, mapping);
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
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710.isUsedByXc(nodeId, interfaceName, xc, deviceTransactionManager);
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

    public String createOpenRoadmEth1GInterface(String nodeId,String logicalConnPoint)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface221.createOpenRoadmEth1GInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }
    }

    public String createOpenRoadmEth10GInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface221.createOpenRoadmEth10GInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }
    }

    public String createOpenRoadmEth100GInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                LOG.warn("Use Ethernet interface creation for 2.2.1, instead this method of Ether 100G");
                return openRoadmInterface221.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmOtnInterface710.createOpenRoadmEth100GInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }
    }

    public String createOpenRoadmOdu0Interface(String nodeId, String logicalConnPoint, String servicename,
            boolean isCTP, int tribPortNumber, int tribSlotIndex, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ,
            String payLoadType)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface221.createOpenRoadmOdu0Interface(nodeId, logicalConnPoint, servicename,
                    isCTP, tribPortNumber, tribSlotIndex, apiInfoA, apiInfoZ, payLoadType);
            default:
                return null;
        }
    }

    public String createOpenRoadmOdu2Interface(String nodeId, String logicalConnPoint, String servicename,
            boolean isCTP, int tribPortNumber, int tribSlotIndex, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ,
            String payLoadType)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface221.createOpenRoadmOdu2Interface(nodeId, logicalConnPoint,  servicename,
                    isCTP, tribPortNumber, tribSlotIndex, apiInfoA, apiInfoZ, payLoadType);
            default:
                return null;
        }
    }

    public String createOpenRoadmOdu2eInterface(String nodeId, String logicalConnPoint, String servicename,
            boolean isCTP, int tribPortNumber, int tribSlotIndex, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ,
            String payLoadType)
            throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmOtnInterface221.createOpenRoadmOdu2eInterface(nodeId, logicalConnPoint, servicename,
                    isCTP, tribPortNumber, tribSlotIndex, apiInfoA, apiInfoZ, payLoadType);
            default:
                return null;
        }
    }

    public String createOpenRoadmOtnOdu4LoInterface(String nodeId, String logicalConnPoint, String serviceName,
            String payLoad, boolean isNetworkPort, OpucnTribSlotDef minTribSlotNumber,
            OpucnTribSlotDef maxTribSlotNumber)
        throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_1_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                LOG.error(OTN_FUNTIONS_ARE_NOT_SUPPORTED_BY_OPENROADM_MODELS_2_2_1_MSG);
                return null;
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmOtnInterface710.createOpenRoadmOdu4Interface(nodeId, logicalConnPoint, serviceName,
                    payLoad, isNetworkPort, minTribSlotNumber, maxTribSlotNumber);
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

    public String createOpenRoadmOtnOduc4Interface(String nodeId, String logicalConnPoint,
            String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710
                    .createOpenRoadmOtnOducnInterface(nodeId, logicalConnPoint, supportingOtuInterface);
            default:
                return null;
        }
    }

    public String createOpenRoadmOtnOducnInterface(String anodeId, String alogicalConnPoint,
            String asupportingOtuInterface, String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(anodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return openRoadmInterface710
                    .createOpenRoadmOtnOducnInterface(anodeId, alogicalConnPoint, asupportingOtuInterface,
                        znodeId, zlogicalConnPoint);
            default:
                return null;
        }
    }
}
