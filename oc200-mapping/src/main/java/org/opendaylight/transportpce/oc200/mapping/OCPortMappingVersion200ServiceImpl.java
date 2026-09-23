/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.oc200.mapping;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.OCPortMappingVersionService;
import org.opendaylight.transportpce.common.metadata.OCMetaDataTransaction;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260908.mapping.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Service implementation for OpenConfig 200.
 *
 * <p>This service wraps OCPortMappingVersion200 with isolated OpenConfig 200
 * YANG dependencies.
 *
 * <p>Dependencies: DataBroker (required), DeviceTransactionManager (required),
 * OCMetaDataTransaction (required), NetworkTransactionService (optional, can be injected dynamically).
 */
@Component(service = OCPortMappingVersionService.class)
public class OCPortMappingVersion200ServiceImpl implements OCPortMappingVersionService {

    private static final Logger LOG = LoggerFactory.getLogger(
        OCPortMappingVersion200ServiceImpl.class);
    private static final String VERSION = "openconfig-200";

    private final OCPortMappingVersion200 delegate;

    @Activate
    public OCPortMappingVersion200ServiceImpl(
            @Reference DataBroker dataBroker,
            @Reference DeviceTransactionManager deviceTransactionManager,
            @Reference OCMetaDataTransaction ocMetaDataTransaction) {
        this.delegate = new OCPortMappingVersion200(
            dataBroker, deviceTransactionManager, ocMetaDataTransaction,
            null);
        LOG.info("OCPortMappingVersion200ServiceImpl activated - "
            + "DataBroker={}, DeviceTransactionManager={}, OCMetaDataTransaction={}",
            dataBroker != null, deviceTransactionManager != null,
            ocMetaDataTransaction != null);
    }

    /**
     * Called when NetworkTransactionService becomes available (DYNAMIC injection via OSGi).
     */
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL,
               policy = org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC)
    protected void setNetworkTransactionService(NetworkTransactionService service) {
        delegate.setNetworkTransactionService(service);
        LOG.info("OCPortMappingVersion200ServiceImpl: NetworkTransactionService injected");
    }

    /**
     * Called when NetworkTransactionService is removed.
     */
    protected void unsetNetworkTransactionService(NetworkTransactionService service) {
        LOG.info("OCPortMappingVersion200ServiceImpl: NetworkTransactionService removed");
        delegate.setNetworkTransactionService(null);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public boolean createMappingData(String nodeId, IpAddress ipAddress) {
        return delegate.createMappingData(nodeId, ipAddress);
    }

    @Override
    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        return delegate.updateMapping(nodeId, oldMapping);
    }
}
