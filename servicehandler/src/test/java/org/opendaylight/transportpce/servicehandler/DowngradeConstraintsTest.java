/*
 * Copyright © 2018 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendaylight.transportpce.servicehandler.utils.ConstraintsUtils.buildHardConstraint;
import static org.opendaylight.transportpce.servicehandler.utils.ConstraintsUtils.buildSoftConstraint;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.DiversityConstraints.DiversityType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.common.constraints.LinkIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.diversity.existing.service.constraints.ServiceIdentifierListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraints;

/**
 * Class to test downgrading and updating Constraints .
 * @author Ahmed Helmy ( ahmad.helmy@orange.com )
 * @author Gilles Thouenon (gilles.thouenon@orange.com)
 */
public class DowngradeConstraintsTest {

    @Test
    void testUpdateSoftConstraintsForCustomerCode() {
        // test no addition when hard customer-code is null or empty
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getCustomerCode(),
            "updated soft constraints should contain no customer code");
        Set<String> softCustomerCode = Set.of("soft-customer-code 3", "soft-customer-code 4");
        initialSoftConstraints =
            buildSoftConstraint(softCustomerCode, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getCustomerCode(), generatedSoftConstraints.getCustomerCode());

        // test addition of hard customer-code when no soft customer-code
        Set<String> hardCustomerCode = Set.of("hard-customer-code 1", "hard-customer-code 2");
        initialHardConstraints =
            buildHardConstraint(hardCustomerCode, false, null, null, null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(generatedSoftConstraints.getCustomerCode(),initialHardConstraints.getCustomerCode(),
            "updated soft constraints should contain the customer code of hard constraint");
        // test addition of hard customer-code when existing soft customer-code
        initialSoftConstraints =
            buildSoftConstraint(softCustomerCode, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
                initialHardConstraints, initialSoftConstraints);
        assertThat(generatedSoftConstraints.getCustomerCode())
            .withFailMessage("updated soft constraints should contain 4 customer code")
            .hasSize(4);
        assertThat(generatedSoftConstraints.getCustomerCode())
            .containsExactlyInAnyOrder("hard-customer-code 1", "hard-customer-code 2", "soft-customer-code 3",
                    "soft-customer-code 4");
    }

    @Test
    void testUpdateSoftConstraintsForDiversity() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getDiversity(),
            "updated soft constraints should contain no diversity constraint");
        Set<String> softDiversityServiceid = Set.of("soft-service 3");
        initialSoftConstraints =
            buildSoftConstraint(null, false, softDiversityServiceid, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getDiversity(), generatedSoftConstraints.getDiversity());

        // test addition of hard diversity when no soft diversity
        Set<String> hardDiversityServiceid = Set.of("hard-service 1", "hard-service 2");
        initialHardConstraints =
            buildHardConstraint(null, false, hardDiversityServiceid, null, null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(generatedSoftConstraints.getDiversity(), initialHardConstraints.getDiversity(),
            "updated soft constraints should contain the diversity of hard constraint");

        // test addition of hard diversity when existing soft diversity
        initialSoftConstraints =
            buildSoftConstraint(null, false, softDiversityServiceid, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat(generatedSoftConstraints.getDiversity().getServiceIdentifierList())
            .withFailMessage("updated soft constraints should contain diversity with 3 services")
            .hasSize(3);
        assertEquals(DiversityType.Serial, generatedSoftConstraints.getDiversity().getDiversityType(),
            "updated soft constraints should have diversity type of serial");
        assertThat(generatedSoftConstraints.getDiversity().getServiceIdentifierList())
            .containsKeys(new ServiceIdentifierListKey("hard-service 1"),
                    new ServiceIdentifierListKey("hard-service 2"), new ServiceIdentifierListKey("soft-service 3"));
    }

    @Test
    void testUpdateSoftConstraintsForExclude() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getExclude(),
            "updated soft constraints should contain no exclude constraint");

        initialSoftConstraints = buildSoftConstraint(null, false, null, "link", null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getExclude(), generatedSoftConstraints.getExclude(),
            "updated soft constraints should not be changed");

        // test addition of hard exclude with fiber list when no soft exclude
        initialHardConstraints =
            buildHardConstraint(null, false, null, "fiber1", null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(generatedSoftConstraints.getExclude(), initialHardConstraints.getExclude(),
            "updated soft constraints should contain the exclude constraint of hard constraint");

        // test addition of hard exclude with fiber list when existing soft
        // exclude with fiber list
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, "fiber2", null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat(generatedSoftConstraints.getExclude().getFiberBundle())
            .withFailMessage("updated soft constraints should contain exclude with 3 fiber bundles")
            .hasSize(3);
        assertThat(generatedSoftConstraints.getExclude().getFiberBundle())
            .containsExactlyInAnyOrder("fiber-1", "fiber-2", "fiber-3");

        // test addition of hard exclude with link list when no soft exclude
        initialHardConstraints = buildHardConstraint(null, false, null, "link1", null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(generatedSoftConstraints.getExclude(), initialHardConstraints.getExclude(),
            "updated soft constraints should contain the exclude constraint of hard constraint");

        // test addition of hard exclude with link list when existing soft
        // exclude with link list
        initialSoftConstraints = buildSoftConstraint(null, false, null, "link2", null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat(generatedSoftConstraints.getExclude().getLinkIdentifier())
            .withFailMessage("updated soft constraints should contain exclude with 3 links")
            .hasSize(3);
        assertThat(generatedSoftConstraints.getExclude().getLinkIdentifier())
            .contains(new LinkIdentifierBuilder()
                    .setLinkId("link-id 1")
                    .setLinkNetworkId("openroadm-topology")
                    .build());
        assertThat(generatedSoftConstraints.getExclude().getLinkIdentifier())
            .contains(new LinkIdentifierBuilder()
                    .setLinkId("link-id 2")
                    .setLinkNetworkId("openroadm-topology")
                    .build());
        assertThat(generatedSoftConstraints.getExclude().getLinkIdentifier())
            .contains(new LinkIdentifierBuilder()
                    .setLinkId("link-id 3")
                    .setLinkNetworkId("openroadm-topology")
                    .build());
    }

    @Test
    void testUpdateSoftConstraintsForInclude() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getInclude(),
            "updated soft constraints should contain no include constraint");

        initialSoftConstraints = buildSoftConstraint(null, false, null, null, "link", null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getInclude(), generatedSoftConstraints.getInclude(),
            "updated soft constraints should not be changed");

        // test addition of hard include with fiber list when no soft include
        initialHardConstraints =
            buildHardConstraint(null, false, null, null, "fiber1", null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(generatedSoftConstraints.getInclude(), initialHardConstraints.getInclude(),
            "updated soft constraints should contain the include constraint of hard constraint");

        // test addition of hard include with fiber list when existing soft
        // include with fiber list
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, "fiber2", null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat(generatedSoftConstraints.getInclude().getFiberBundle())
            .withFailMessage("updated soft constraints should contain exclude with 3 fiber bundles")
            .hasSize(3);
        assertThat(generatedSoftConstraints.getInclude().getFiberBundle())
            .containsExactlyInAnyOrder("fiber-1", "fiber-2", "fiber-3");

        // test addition of hard include with link list when no soft include
        initialHardConstraints = buildHardConstraint(null, false, null, null, "link1", null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(generatedSoftConstraints.getInclude(), initialHardConstraints.getInclude(),
            "updated soft constraints should contain the include constraint of hard constraint");

        // test addition of hard include with link list when existing soft
        // include with link list
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, "link2", null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat(generatedSoftConstraints.getInclude().getLinkIdentifier())
            .withFailMessage("updated soft constraints should contain include with 3 links")
            .hasSize(3);
        assertThat(generatedSoftConstraints.getInclude().getLinkIdentifier())
            .contains(new LinkIdentifierBuilder()
                    .setLinkId("link-id 1")
                    .setLinkNetworkId("openroadm-topology")
                    .build());
        assertThat(generatedSoftConstraints.getInclude().getLinkIdentifier())
            .contains(new LinkIdentifierBuilder()
                    .setLinkId("link-id 2")
                    .setLinkNetworkId("openroadm-topology")
                    .build());
        assertThat(generatedSoftConstraints.getInclude().getLinkIdentifier())
            .contains(new LinkIdentifierBuilder()
                    .setLinkId("link-id 3")
                    .setLinkNetworkId("openroadm-topology")
                    .build());
    }

    @Test
    void testUpdateSoftConstraintsForLatency() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getLatency(),
            "updated soft constraints should contain no latency constraint");

        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, Double.valueOf(12.2), false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getLatency(), generatedSoftConstraints.getLatency(),
            "updated soft constraints should not be changed");
        assertEquals((float) 12.2, generatedSoftConstraints.getLatency().getMaxLatency().floatValue(), 0.0f,
            "updated soft constraints value should be '12.2'");

        // test addition of hard latency when no soft latency
        initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, Double.valueOf(16.59), false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals((float) 16.59, generatedSoftConstraints.getLatency().getMaxLatency().floatValue(), 0.0f,
            "updated soft constraints value should be '16.59'");

        // test addition of hard latency when existing different soft latency
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, Double.valueOf(12.2), false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals((float) 12.2, generatedSoftConstraints.getLatency().getMaxLatency().floatValue(), 0.0f,
            "updated soft constraints value should be '12.2'");
    }

    @Test
    void testUpdateSoftConstraintsForDistance() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getDistance(),
            "updated soft constraints should contain no distance constraint");

        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, "750.2", null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getDistance(), generatedSoftConstraints.getDistance(),
            "updated soft constraints should not be changed");
        assertEquals((float) 750.2, generatedSoftConstraints.getDistance().getMaxDistance().floatValue(), 0.0f,
            "updated soft constraints value should be '750.2'");

        // test addition of hard distance when no soft distance
        initialHardConstraints = buildHardConstraint(null, false, null, null, null, null, false, false, "555.5", null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals((float) 555.5, generatedSoftConstraints.getDistance().getMaxDistance().floatValue(), 0.0f,
            "updated soft constraints value should be '555.5'");

        // test addition of hard distance when existing different soft distance
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, "750.2", null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals((float) 555.5, generatedSoftConstraints.getDistance().getMaxDistance().floatValue(), 0.0f,
            "updated soft constraints value should be '555.5'");
    }

    @Test
    void testUpdateSoftConstraintsForHopCountAndTEmetric() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getHopCount(),
            "updated soft constraints should contain no hop-count constraint");

        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, true, true, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getHopCount(), generatedSoftConstraints.getHopCount(),
            "updated soft constraints should not be changed");
        assertEquals(3, generatedSoftConstraints.getHopCount().getMaxWdmHopCount().intValue(),
            "updated soft constraints max-wdm-hop-count should be '3'");
        assertEquals(5, generatedSoftConstraints.getHopCount().getMaxOtnHopCount().intValue(),
            "updated soft constraints max-otn-hop-count should be '5'");
        assertEquals(8, generatedSoftConstraints.getTEMetric().getMaxWdmTEMetric().intValue(),
            "updated soft constraints max-wdm-TE-metric should be '8'");
        assertEquals(11, generatedSoftConstraints.getTEMetric().getMaxOtnTEMetric().intValue(),
            "updated soft constraints max-otn-TE-metric should be '11'");

        // test addition of hard hop-count when no soft hop-count
        initialHardConstraints = buildHardConstraint(null, false, null, null, null, null, true, true, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialHardConstraints.getHopCount(), generatedSoftConstraints.getHopCount(),
            "updated soft constraints should contain hard constraint");
        assertEquals(3, generatedSoftConstraints.getHopCount().getMaxWdmHopCount().intValue(),
            "updated soft constraints max-wdm-hop-count should be '3'");
        assertEquals(5, generatedSoftConstraints.getHopCount().getMaxOtnHopCount().intValue(),
            "updated soft constraints max-otn-hop-count should be '5'");
        assertEquals(8, generatedSoftConstraints.getTEMetric().getMaxWdmTEMetric().intValue(),
            "updated soft constraints max-wdm-TE-metric should be '8'");
        assertEquals(11, generatedSoftConstraints.getTEMetric().getMaxOtnTEMetric().intValue(),
            "updated soft constraints max-otn-TE-metric should be '11'");

        // test addition of hard hop-count when existing soft hop-count
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, true, true, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(3, generatedSoftConstraints.getHopCount().getMaxWdmHopCount().intValue(),
            "updated soft constraints max-wdm-hop-count should be '3'");
        assertEquals(5, generatedSoftConstraints.getHopCount().getMaxOtnHopCount().intValue(),
            "updated soft constraints max-otn-hop-count should be '5'");
        assertEquals(8, generatedSoftConstraints.getTEMetric().getMaxWdmTEMetric().intValue(),
            "updated soft constraints max-wdm-TE-metric should be '8'");
        assertEquals(11, generatedSoftConstraints.getTEMetric().getMaxOtnTEMetric().intValue(),
            "updated soft constraints max-otn-TE-metric should be '11'");
    }

    @Test
    void testUpdateSoftConstraintsForCoRouting() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull(generatedSoftConstraints.getCoRouting(),
            "updated soft constraints should contain no co-routing constraint");

        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, true, false, null, "coRouting1");
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getCoRouting(), generatedSoftConstraints.getCoRouting(),
            "updated soft constraints should not be changed");
        assertEquals(2, generatedSoftConstraints.getCoRouting().getServiceIdentifierList().size(),
            "updated soft constraints should contain 2 co-routed services");
        assertTrue(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                    .routing.ServiceIdentifierListKey("service 1"))
            .getServiceApplicability().getEquipment().getRoadmSrg());
        assertNull(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                        .routing.ServiceIdentifierListKey("service 1"))
            .getServiceApplicability().getLink());
        assertNull(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                        .routing.ServiceIdentifierListKey("service 2"))
            .getServiceApplicability().getEquipment());
        assertTrue(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                    .routing.ServiceIdentifierListKey("service 2"))
            .getServiceApplicability().getLink());

        // test addition of hard co-routing when no soft co-routing
        initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, true, false, null, "coRouting2");
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialHardConstraints.getCoRouting(), generatedSoftConstraints.getCoRouting(),
            "updated soft constraints should contain hard constraint");
        assertEquals(1, generatedSoftConstraints.getCoRouting().getServiceIdentifierList().size(),
            "updated soft constraints should contain 1 co-routed service");
        assertTrue(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                        .routing.ServiceIdentifierListKey("service 3"))
            .getServiceApplicability().getSite());
        assertNull(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                        .routing.ServiceIdentifierListKey("service 3"))
            .getServiceApplicability().getLink());

        // test addition of hard hop-count when existing soft hop-count
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, true, false, null, "coRouting1");
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(3, generatedSoftConstraints.getCoRouting().getServiceIdentifierList().size(),
            "updated soft constraints should contain 3 co-routed service");
        assertThat(generatedSoftConstraints.getCoRouting().getServiceIdentifierList())
            .containsKeys(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                        .routing.ServiceIdentifierListKey("service 1"),
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                        .routing.ServiceIdentifierListKey("service 2"),
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co
                        .routing.ServiceIdentifierListKey("service 3"));
    }

    @Test
    void testDowngradeHardConstraints() {
        HardConstraints initialHardConstraints = null;
        HardConstraints genHardConstraints = DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
        assertNotNull(genHardConstraints, "generated hard-constraints should be empty, not null");
        initialHardConstraints = new HardConstraintsBuilder().build();
        genHardConstraints = DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
        assertNotNull(genHardConstraints, "generated hard-constraints should be empty, not null");
        initialHardConstraints = buildHardConstraint(null, false, null, "link1", null, Double.valueOf(12.8), false,
            false, null, null);
        genHardConstraints = DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
        assertEquals((long) 12.8, genHardConstraints.getLatency().getMaxLatency().longValue(),
            "Latency value should be 12.8");
        assertNull(genHardConstraints.getCoRouting(),
            "generated hard constraints should only contain max-latency value");
        assertNull(genHardConstraints.getExclude(), "generated hard constraints should only contain max-latency value");
        assertNull(genHardConstraints.getInclude(), "generated hard constraints should only contain max-latency value");
    }

    @Test
    void testConvertToSoftConstraints() {
        HardConstraints initialHardConstraints = null;
        SoftConstraints genSoftConstraints = DowngradeConstraints.convertToSoftConstraints(initialHardConstraints);
        assertNotNull(genSoftConstraints, "generated soft constraints should never be null");
        assertNull(genSoftConstraints.getExclude(), "generated soft constraints should be empty");
        assertNull(genSoftConstraints.getCoRouting(), "generated soft constraints should be empty");
        assertNull(genSoftConstraints.getLatency(), "generated soft constraints should be empty");

        Set<String> hardCustomerCode = Set.of("customer-code 1", "customer-code 2");
        initialHardConstraints =
            buildHardConstraint(hardCustomerCode, false, null, "link1", "node", null, false, false, null, null);
        genSoftConstraints = DowngradeConstraints.convertToSoftConstraints(initialHardConstraints);
        assertEquals(2, genSoftConstraints.getCustomerCode().size(),
            "generated soft constraints should contain customer-code items");
        assertTrue(genSoftConstraints.getCustomerCode().contains("customer-code 1"));
        assertTrue(genSoftConstraints.getCustomerCode().contains("customer-code 2"));
        assertNotNull(genSoftConstraints.getExclude(), "generated soft constraints should contain exclude constraint");
        assertEquals(1, genSoftConstraints.getExclude().getLinkIdentifier().size(),
            "generated soft constraints should contain exclude constraint with one link-id");
        assertEquals("link-id 1",
            genSoftConstraints.getExclude().getLinkIdentifier().stream().findAny().orElseThrow().getLinkUuid());
        assertEquals("openroadm-topology",
            genSoftConstraints.getExclude().getLinkIdentifier().stream().findAny().orElseThrow()
                .getLinkNetworkId());
        assertNotNull(genSoftConstraints.getInclude(), "generated soft constraints should contain include constraint");
        assertEquals(2, genSoftConstraints.getInclude().getNodeId().size(),
            "generated soft constraints should contain include constraint with two node-id");
        assertTrue(genSoftConstraints.getInclude().getNodeId().contains(new NodeIdType("node-id-1")));
        assertTrue(genSoftConstraints.getInclude().getNodeId().contains(new NodeIdType("node-id-3")));
        assertNull(genSoftConstraints.getLatency(),
            "generated soft constraints should not contain any latency constraint");
        assertNull(genSoftConstraints.getDistance(),
            "generated soft constraints should not contain any max-distance constraint");
        assertNull(genSoftConstraints.getCoRouting(),
            "generated soft constraints should not contain any co-routing constraint");
    }
}
