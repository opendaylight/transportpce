/*
 * Copyright © 2022 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.OperationalModeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogInput {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogInput.class);
    private SdncRequestHeader sdncRequestHeader;
    private OperationalModeInfo operationalModeInfo;

    private org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes
            .to.catalog.input.OperationalModeInfo operationalModeInfoSpecific;

    public CatalogInput(AddOpenroadmOperationalModesToCatalogInput addORToCatalogInput) {
        LOG.info("CatalogInput AddOpenroadmOperationalModesToCatalogInput");
        setSdncRequestHeader(addORToCatalogInput.getSdncRequestHeader());
        setOperationalModeInfo(addORToCatalogInput.getOperationalModeInfo());
    }

    public CatalogInput(AddSpecificOperationalModesToCatalogInput addSpecificToCatalogInput) {
        LOG.info("CatalogInput AddSpecificOperationalModesToCatalogInput");
        setSdncRequestHeader(addSpecificToCatalogInput.getSdncRequestHeader());
        setOperationalModeInfoSpecific(addSpecificToCatalogInput.getOperationalModeInfo());
    }

    public SdncRequestHeader getSdncRequestHeader() {
        return sdncRequestHeader;
    }

    public void setSdncRequestHeader(SdncRequestHeader sdncRequestHeader) {
        this.sdncRequestHeader = sdncRequestHeader;
    }

    public OperationalModeInfo getOperationalModeInfo() {
        return operationalModeInfo;
    }

    public void setOperationalModeInfo(OperationalModeInfo operationalModeInfo) {
        this.operationalModeInfo = operationalModeInfo;
    }

    public org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes
            .to.catalog.input.OperationalModeInfo getOperationalModeInfoSpecific() {
        return operationalModeInfoSpecific;
    }

    public void setOperationalModeInfoSpecific(org.opendaylight.yang.gen.v1.http.org.openroadm
                                                       .service.rev211210.add.specific.operational.modes
                                                       .to.catalog.input.OperationalModeInfo
                                                       operationalModeInfoSpecific) {
        this.operationalModeInfoSpecific = operationalModeInfoSpecific;
    }
}
