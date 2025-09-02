/*
 * Copyright © 2023 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.OpenroadmOperationalModesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.SpecificOperationalModesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.openroadm.operational.modes.AmplifiersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.openroadm.operational.modes.GridParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.openroadm.operational.modes.RoadmsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.openroadm.operational.modes.XpondersPluggablesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110.operational.mode.catalog.specific.operational.modes.SpecificOperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.add.openroadm.operational.modes.to.catalog.input.OperationalModeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class CatalogMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogMapper.class);

    private CatalogMapper() {
    }

    static SpecificOperationalModesBuilder specificObjBuilder = new SpecificOperationalModesBuilder();

    static OpenroadmOperationalModesBuilder objBuilder = new OpenroadmOperationalModesBuilder();

    /**
     * Preparation of SpecificOperationalModes object which will be stored in the config data store
     * and returning the same.
     */
    public static SpecificOperationalModes createSpecificModesToSave(AddSpecificOperationalModesToCatalogInput input) {
        LOG.info("Inside createSpecificModesToSave method of CatalogMapper");
        saveSpecificOperationalModes(input.getOperationalModeInfo().getSpecificOperationalModes());
        return specificObjBuilder.build();
    }

    /**
     * Preparation of OpenroadmOperationalModes object which will be stored in the config data store
     * and returning the same.
     */
    public static OpenroadmOperationalModes createORModesToSave(AddOpenroadmOperationalModesToCatalogInput input) {
        LOG.info("Inside createORModesToSave method of CatalogMapper");
        OperationalModeInfo modesFromInput = input.getOperationalModeInfo();
        saveGridParameters(modesFromInput);
        saveXpondersPlugabbles(modesFromInput);
        saveRoadms(modesFromInput);
        saveAmplifiers(modesFromInput);
        return objBuilder.build();
    }

    /**
     * Preparation of Amplifiers for OpenroadmOperationalModes object.
     */
    private static void saveAmplifiers(OperationalModeInfo modesFromInput) {
        objBuilder.setAmplifiers(
                new AmplifiersBuilder()
                        .setAmplifier(modesFromInput.getAmplifiers().getAmplifier())
                        .build());
    }

    /**
     * Preparation of Grid Parameters for OpenroadmOperationalModes object.
     */
    private static void saveGridParameters(OperationalModeInfo modesFromInput) {
        objBuilder.setGridParameters(new GridParametersBuilder(modesFromInput.getGridParameters()).build());
    }

    /**
     * Preparation of Xponders Pluggables for OpenroadmOperationalModes object.
     */
    private static void saveXpondersPlugabbles(OperationalModeInfo modesFromInput) {
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110
                .operational.mode.catalog.openroadm.operational.modes.xponders.pluggables
                .XponderPluggableOpenroadmOperationalModeKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110
                .operational.mode.catalog.openroadm.operational.modes.xponders.pluggables
                .XponderPluggableOpenroadmOperationalMode> mapFinal = new HashMap<>();
        for (var entry : modesFromInput.getXpondersPluggables().getXponderPluggableOpenroadmOperationalMode()
                .entrySet()) {
            mapFinal.put(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110
                    .operational.mode.catalog.openroadm.operational.modes.xponders.pluggables
                    .XponderPluggableOpenroadmOperationalModeKey(entry.getKey().toString()),
                new XponderPluggableOpenroadmOperationalModeBuilder(entry.getValue())
                    .setOpenroadmOperationalModeId(entry.getValue().getOpenroadmOperationalModeId())
                    .build());
            objBuilder.setXpondersPluggables(
                new XpondersPluggablesBuilder().setXponderPluggableOpenroadmOperationalMode(mapFinal).build());
        }
    }

    /**
     * Preparation of Roadms for OpenroadmOperationalModes object.
     */
    private static void saveRoadms(OperationalModeInfo modesFromInput) {
        objBuilder.setRoadms(
            new RoadmsBuilder()
                .setAdd(modesFromInput.getRoadms().getAdd())
                .setDrop(modesFromInput.getRoadms().getDrop())
                .setExpress(modesFromInput.getRoadms().getExpress())
                .build());
    }

    /**
     * Preparation of Specific Operational Modes for SpecificOperationalModes object.
     */
    private static  void saveSpecificOperationalModes(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110
           .add.specific.operational.modes.to.catalog.input.operational.mode.info.SpecificOperationalModes
                specificModesFromInput) {
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110
                .operational.mode.catalog.specific.operational.modes.SpecificOperationalModeKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110
                .operational.mode.catalog.specific.operational.modes.SpecificOperationalMode>
            mapFinal = new HashMap<>();
        for (var entry : specificModesFromInput.getSpecificOperationalMode().entrySet()) {
            SpecificOperationalModeBuilder specificModeBuilder = new SpecificOperationalModeBuilder();
            specificModeBuilder.fieldsFrom(entry.getValue());
            mapFinal.put(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev250110
                    .operational.mode.catalog.specific.operational.modes.SpecificOperationalModeKey(
                        entry.getKey().toString()),
                specificModeBuilder
                    .setOperationalModeId(entry.getValue().getOperationalModeId())
                    .setConfigurableOutputPower(true)
                    .build());
            specificObjBuilder.setSpecificOperationalMode(mapFinal);
        }
    }
}
