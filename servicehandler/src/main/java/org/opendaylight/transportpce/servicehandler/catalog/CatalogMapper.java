/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.amplifier.parameters.Amplifier;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.OpenroadmOperationalModesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.AmplifiersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.GridParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.RoadmsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.XpondersPluggablesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.add.parameters.Add;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.drop.parameters.Drop;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.express.parameters.Express;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddOpenroadmOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.AddSpecificOperationalModesToCatalogInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.OperationalModeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.xponders.pluggables.XponderPluggableOpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.openroadm.operational.modes.to.catalog.input.operational.mode.info.xponders.pluggables.XponderPluggableOpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes.to.catalog.input.operational.mode.info.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes.to.catalog.input.operational.mode.info.specific.operational.modes.SpecificOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.add.specific.operational.modes.to.catalog.input.operational.mode.info.specific.operational.modes.SpecificOperationalModeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogMapper.class);
    OpenroadmOperationalModesBuilder objBuilder = new OpenroadmOperationalModesBuilder();

    org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
            .SpecificOperationalModesBuilder specificObjBuilder = new org.opendaylight.yang.gen.v1.http.org.openroadm
            .operational.mode.catalog.rev211210.operational.mode.catalog.SpecificOperationalModesBuilder();

    public org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
            .SpecificOperationalModes
        addSpecificToCatalog(AddSpecificOperationalModesToCatalogInput input) {
        LOG.info("Inside addSpecificToCatalog method of CatalogMapper");
        SpecificOperationalModes specificModesFromInput = input.getOperationalModeInfo().getSpecificOperationalModes();
        saveSpecificOperationalModes(specificModesFromInput);
        org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
                .SpecificOperationalModes objToSave = specificObjBuilder.build();
        return objToSave;
    }

    public OpenroadmOperationalModes addToCatalog(AddOpenroadmOperationalModesToCatalogInput input) {
        LOG.info("Inside addToCatalog method of CatalogMapper");
        OperationalModeInfo modesFromInput = input.getOperationalModeInfo();

        saveGridParameters(modesFromInput);
        saveXpondersPlugabbles(modesFromInput);
        saveRoadms(modesFromInput);
        saveAmplifiers(modesFromInput);

        OpenroadmOperationalModes objToSave = objBuilder.build();
        return objToSave;
    }

    private void saveAmplifiers(OperationalModeInfo modesFromInput) {
        Amplifier amplifier = modesFromInput.getAmplifiers().getAmplifier();
        AmplifiersBuilder amplifiersBuilder = new AmplifiersBuilder().setAmplifier(amplifier);
        objBuilder.setAmplifiers(amplifiersBuilder.build());
    }

    private void saveGridParameters(OperationalModeInfo modesFromInput) {
        GridParametersBuilder gridParametersBuilder = new GridParametersBuilder(modesFromInput.getGridParameters());
        objBuilder.setGridParameters(gridParametersBuilder.build());
    }

    private void saveXpondersPlugabbles(OperationalModeInfo modesFromInput) {
        Map<XponderPluggableOpenroadmOperationalModeKey, XponderPluggableOpenroadmOperationalMode> map =
                modesFromInput.getXpondersPluggables().getXponderPluggableOpenroadmOperationalMode();
        Iterator<Map.Entry<XponderPluggableOpenroadmOperationalModeKey, XponderPluggableOpenroadmOperationalMode>> itr =
                map.entrySet().iterator();

        XpondersPluggablesBuilder xpondersPluggablesBuilder = new XpondersPluggablesBuilder();
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode
                .catalog.openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalModeKey,
                org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode
                        .catalog.openroadm.operational.modes.xponders.pluggables
                        .XponderPluggableOpenroadmOperationalMode> mapFinal = new HashMap<>();

        while (itr.hasNext()) {
            Map.Entry<XponderPluggableOpenroadmOperationalModeKey, XponderPluggableOpenroadmOperationalMode> entry =
                    itr.next();

            org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
                    .openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalModeBuilder
                    modeBuilder  = new org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog
                    .rev211210.operational.mode.catalog.openroadm.operational.modes.xponders.pluggables
                    .XponderPluggableOpenroadmOperationalModeBuilder(entry.getValue());

            org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
                    .openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalModeKey
                    key = new org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210
                    .operational.mode.catalog.openroadm.operational.modes.xponders.pluggables
                    .XponderPluggableOpenroadmOperationalModeKey(entry.getKey().toString());

            modeBuilder.setOpenroadmOperationalModeId(entry.getValue().getOpenroadmOperationalModeId());
            org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode
                    .catalog.openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalMode
                    mode = modeBuilder.build();
            mapFinal.put(key, mode);
            xpondersPluggablesBuilder.setXponderPluggableOpenroadmOperationalMode(mapFinal);
            objBuilder.setXpondersPluggables(xpondersPluggablesBuilder
                    .setXponderPluggableOpenroadmOperationalMode(mapFinal).build());
        }
    }

    private void saveRoadms(OperationalModeInfo modesFromInput) {
        Add add = modesFromInput.getRoadms().getAdd();
        Drop drop = modesFromInput.getRoadms().getDrop();
        Express express = modesFromInput.getRoadms().getExpress();
        RoadmsBuilder roadmsBuilder = new RoadmsBuilder().setAdd(add).setDrop(drop).setExpress(express);
        objBuilder.setRoadms(roadmsBuilder.build());
    }

    private void saveSpecificOperationalModes(SpecificOperationalModes specificModesFromInput) {
        Map<SpecificOperationalModeKey, SpecificOperationalMode> map = specificModesFromInput
                .getSpecificOperationalMode();
        Iterator<Map.Entry<SpecificOperationalModeKey, SpecificOperationalMode>> itr = map.entrySet().iterator();

        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
                .specific.operational.modes.SpecificOperationalModeKey, org.opendaylight.yang.gen.v1.http.org.openroadm
                .operational.mode.catalog.rev211210.operational.mode.catalog.specific.operational.modes
                .SpecificOperationalMode> mapFinal = new HashMap<>();

        while (itr.hasNext()) {
            Map.Entry<SpecificOperationalModeKey, SpecificOperationalMode> entry = itr.next();

            org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
                    .specific.operational.modes.SpecificOperationalModeBuilder specificModeBuilder = new org
                    .opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational
                    .mode.catalog.specific.operational.modes.SpecificOperationalModeBuilder();
            specificModeBuilder.fieldsFrom(entry.getValue());

            org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog
                    .specific.operational.modes.SpecificOperationalModeKey specificModeKey = new org.opendaylight.yang
                    .gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.specific
                    .operational.modes.SpecificOperationalModeKey(entry.getKey().toString());

            specificModeBuilder.setOperationalModeId(entry.getValue().getOperationalModeId());
            specificModeBuilder.setConfigurableOutputPower(true);
            org.opendaylight.yang.gen.v1.http.org.openroadm
                    .operational.mode.catalog.rev211210.operational.mode.catalog.specific.operational.modes
                    .SpecificOperationalMode specificMode = specificModeBuilder.build();
            mapFinal.put(specificModeKey, specificMode);
            specificObjBuilder.setSpecificOperationalMode(mapFinal);
        }
    }
}
