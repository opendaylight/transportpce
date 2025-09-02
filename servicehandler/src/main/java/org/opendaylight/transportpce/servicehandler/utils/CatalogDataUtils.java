/*
 * Copyright © 2023 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.utils;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.AddOpenroadmOperationalModesToCatalogInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.AddSpecificOperationalModesToCatalogInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.OperationalModeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.AmplifiersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.GridParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.RoadmsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.XpondersPluggablesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.xponders.pluggables.XponderPluggableOpenroadmOperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.xponders.pluggables.XponderPluggableOpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.specific.operational.modes.to.catalog.input.operational.mode.info.SpecificOperationalModesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.specific.operational.modes.to.catalog.input.operational.mode.info.specific.operational.modes.SpecificOperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.specific.operational.modes.to.catalog.input.operational.mode.info.specific.operational.modes.SpecificOperationalModeKey;

public final class CatalogDataUtils {

    private CatalogDataUtils() {

    }

    public static AddOpenroadmOperationalModesToCatalogInput buildAddORToCatalogInput() {
        XponderPluggableOpenroadmOperationalModeKey key =
            new XponderPluggableOpenroadmOperationalModeKey("testOROperationalMode");
        return new AddOpenroadmOperationalModesToCatalogInputBuilder()
            .setSdncRequestHeader(new SdncRequestHeaderBuilder()
                .setRequestId("load-OM-Catalog")
                .setRequestSystemId("appname")
                .setRpcAction(RpcActions.FillCatalogWithOrOperationalModes)
                .build())
            .setOperationalModeInfo(new OperationalModeInfoBuilder()
                .setGridParameters(new GridParametersBuilder().build())
                .setXpondersPluggables(
                    new XpondersPluggablesBuilder()
                        .setXponderPluggableOpenroadmOperationalMode(new HashMap<>(Map.of(
                            key,
                            new XponderPluggableOpenroadmOperationalModeBuilder()
                                .setOpenroadmOperationalModeId(key.toString())
                                .build())))
                        .build())
                .setRoadms(new RoadmsBuilder().build())
                .setAmplifiers(new AmplifiersBuilder().build())
                .build())
            .build();
    }

    public static AddSpecificOperationalModesToCatalogInput buildAddSpecificToCatalogInput() {
        SpecificOperationalModeKey key = new SpecificOperationalModeKey("testSpecificOperationalMode");
        return new AddSpecificOperationalModesToCatalogInputBuilder()
            .setSdncRequestHeader(new SdncRequestHeaderBuilder()
                .setRequestId("load-specific-OM-Catalog")
                .setRequestSystemId("test")
                .setRpcAction(RpcActions.FillCatalogWithSpecificOperationalModes)
                .build())
            .setOperationalModeInfo(new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110
                    .add.specific.operational.modes.to.catalog.input.OperationalModeInfoBuilder()
                .setSpecificOperationalModes(
                    new SpecificOperationalModesBuilder()
                        .setSpecificOperationalMode(
                            new HashMap<>(Map.of(
                                key,
                                new SpecificOperationalModeBuilder()
                                    .setOperationalModeId(key.toString())
                                    .build()))
                            )
                        .build())
                .build())
            .build();
    }
}
