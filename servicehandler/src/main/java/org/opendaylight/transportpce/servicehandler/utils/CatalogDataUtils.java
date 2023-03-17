/*
 * Copyright © 2022 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.utils;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddOpenroadmOperationalModesToCatalogInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddSpecificOperationalModesToCatalogInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.OperationalModeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.AmplifiersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.GridParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.RoadmsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.XpondersPluggablesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.xponders.pluggables.XponderPluggableOpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.xponders.pluggables.XponderPluggableOpenroadmOperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.xponders.pluggables.XponderPluggableOpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes.to.catalog.input.operational.mode.info.SpecificOperationalModesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes.to.catalog.input.operational.mode.info.specific.operational.modes.SpecificOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes.to.catalog.input.operational.mode.info.specific.operational.modes.SpecificOperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes.to.catalog.input.operational.mode.info.specific.operational.modes.SpecificOperationalModeKey;

public final class CatalogDataUtils {

    private CatalogDataUtils() {

    }

    public static AddOpenroadmOperationalModesToCatalogInput buildAddORToCatalogInput() {
        Map<XponderPluggableOpenroadmOperationalModeKey, XponderPluggableOpenroadmOperationalMode> map = new
                HashMap<>();
        XponderPluggableOpenroadmOperationalModeBuilder modeBuilder = new
                XponderPluggableOpenroadmOperationalModeBuilder();
        XponderPluggableOpenroadmOperationalModeKey key = new
                XponderPluggableOpenroadmOperationalModeKey("testOROperationalMode");
        modeBuilder.setOpenroadmOperationalModeId(key.toString());
        map.put(key, modeBuilder.build());
        XpondersPluggablesBuilder xpondersPluggablesBuilder = new XpondersPluggablesBuilder()
                .setXponderPluggableOpenroadmOperationalMode(map);

        AddOpenroadmOperationalModesToCatalogInputBuilder builtInput = new
                AddOpenroadmOperationalModesToCatalogInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("load-OM-Catalog")
                        .setRequestSystemId("appname").setRpcAction(RpcActions.FillCatalogWithOrOperationalModes)
                        .build()).setOperationalModeInfo(new OperationalModeInfoBuilder().setGridParameters(new
                        GridParametersBuilder().build()).setXpondersPluggables(xpondersPluggablesBuilder
                        .build()).setRoadms(new RoadmsBuilder().build()).setAmplifiers(new AmplifiersBuilder()
                        .build()).build());
        return builtInput.build();
    }

    public static AddSpecificOperationalModesToCatalogInput buildAddSpecificToCatalogInput() {

        Map<SpecificOperationalModeKey, SpecificOperationalMode> map = new HashMap<>();
        SpecificOperationalModeKey key = new SpecificOperationalModeKey("testSpecificOperationalMode");
        SpecificOperationalModeBuilder modeBuilder = new SpecificOperationalModeBuilder();
        modeBuilder.setOperationalModeId(key.toString());
        map.put(key, modeBuilder.build());
        SpecificOperationalModesBuilder specificOperationalModesBuilder = new SpecificOperationalModesBuilder()
                .setSpecificOperationalMode(map);

        AddSpecificOperationalModesToCatalogInputBuilder builtInput = new
                AddSpecificOperationalModesToCatalogInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("load-specific-OM-Catalog")
                        .setRequestSystemId("test").setRpcAction(RpcActions.FillCatalogWithSpecificOperationalModes)
                        .build()).setOperationalModeInfo(new org.opendaylight.yang.gen.v1.http.org.openroadm.service
                        .rev211210.add.specific.operational.modes.to.catalog.input.OperationalModeInfoBuilder()
                        .setSpecificOperationalModes(specificOperationalModesBuilder.build()).build());
        return builtInput.build();
    }
}