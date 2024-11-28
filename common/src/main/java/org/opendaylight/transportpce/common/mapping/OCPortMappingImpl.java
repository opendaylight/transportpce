/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;
import static org.opendaylight.transportpce.common.StringConstants.OPENCONFIG_DEVICE_VERSION_1_9_0;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.metadata.OCMetaDataTransaction;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OCPortMappingImpl implements  OCPortMapping {

    private static final Logger LOG = LoggerFactory.getLogger(OCPortMappingImpl.class);

    private final DataBroker dataBroker;
    private final OCPortMappingVersion190 ocPortMappingVersion190;

    @Activate
    public OCPortMappingImpl(@Reference DataBroker dataBroker ,
                             @Reference DeviceTransactionManager deviceTransactionManager,
                             @Reference OCMetaDataTransaction ocMetaDataTransaction,
                             @Reference NetworkTransactionService networkTransactionService) {
        this(dataBroker , new OCPortMappingVersion190(dataBroker,deviceTransactionManager,ocMetaDataTransaction,
                networkTransactionService));
    }

    /**
     * constructor of OCPortMappingImpl.
     * @param dataBroker
     *            data broker
     * @param ocPortMappingVersion190
     *            ocPortMappingVersion190
     */
    public OCPortMappingImpl(DataBroker dataBroker, OCPortMappingVersion190 ocPortMappingVersion190) {
        this.dataBroker = dataBroker;
        this.ocPortMappingVersion190 = ocPortMappingVersion190;
    }

    /**
     * This method creates port mapping data for a given device.
     * @param nodeId
     *            node ID
     * @param nodeVersion
     *            node version
     * @param ipAddress
     *           ipaddress
     * @return true/false based on status of operation
     */
    @Override
    public boolean createMappingData(String nodeId, String nodeVersion, IpAddress ipAddress) {
        switch (nodeVersion) {
            case OPENCONFIG_DEVICE_VERSION_1_9_0:
                return ocPortMappingVersion190.createMappingData(nodeId, ipAddress);
            default:
                LOG.error("unable to create mapping data for unmanaged openconfig version devices");
                return false;
        }
    }
}
