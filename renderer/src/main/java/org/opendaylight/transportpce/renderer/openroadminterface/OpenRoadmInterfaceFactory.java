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
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.RateIdentity;

public class OpenRoadmInterfaceFactory {

    private final MappingUtils mappingUtils;
    private final OpenRoadmInterface121 openRoadmInterface121;
    private final OpenRoadmInterface22 openRoadmInterface22;


    public OpenRoadmInterfaceFactory(MappingUtils mappingUtils, OpenRoadmInterface121 openRoadmInterface121,
        OpenRoadmInterface22 openRoadmInterface22) {
        this.mappingUtils = mappingUtils;
        this.openRoadmInterface121 = openRoadmInterface121;
        this.openRoadmInterface22 = openRoadmInterface22;
    }

    public String createOpenRoadmEthInterface(String nodeId,
                                              String logicalConnPoint) throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface22.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
            default:
                return null;
        }
    }


    /**
     * This methods creates an OCH interface on the given termination point on
     * Roadm.
     *
     * @param waveNumber wavelength number of the OCH interface.
     * @return Name of the interface if successful, otherwise return null.
     */

    public List<String> createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber)
        throws OpenRoadmInterfaceException {

        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterface(nodeId, logicalConnPoint,waveNumber);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface22.createFlexOCH(nodeId, logicalConnPoint,waveNumber);
            default:
                return null;
        }
    }


    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber,
        Class<? extends RateIdentity> rate, OchAttributes.ModulationFormat format) throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOchInterface(nodeId, logicalConnPoint, waveNumber,
                    rate, format);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface22.createOpenRoadmOchInterface(nodeId,logicalConnPoint,waveNumber);
            default:
                return null;
        }
    }

    /**
     * This methods creates an ODU interface on the given termination point.
     *
     * @return Name of the interface if successful, otherwise return null.
     */

    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOdu4Interface(nodeId, logicalConnPoint,
                    supportingOtuInterface);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface22.createOpenRoadmOdu4Interface(nodeId, logicalConnPoint,
                    supportingOtuInterface);
            default:
                return null;
        }
    }

    /**
     * This methods creates an OTU interface on the given termination point.
     *
     * @return Name of the interface if successful, otherwise return null.
     */

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface)
            throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOtu4Interface(nodeId,
                    logicalConnPoint,supportOchInterface);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface22.createOpenRoadmOtu4Interface(nodeId,logicalConnPoint, supportOchInterface);
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
                return openRoadmInterface121.createOpenRoadmOmsInterface(nodeId,mapping);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface22.createOpenRoadmOmsInterface(nodeId,mapping);
            default:
                return null;
        }
    }

    public String createOpenRoadmOtsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        switch (mappingUtils.getOpenRoadmVersion(nodeId)) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return openRoadmInterface121.createOpenRoadmOtsInterface(nodeId,mapping);
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return openRoadmInterface22.createOpenRoadmOtsInterface(nodeId,mapping);
            default:
                return null;
        }
    }





}
