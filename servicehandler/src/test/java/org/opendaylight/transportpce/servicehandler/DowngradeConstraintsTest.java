/*
 * Copyright © 2018 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.transportpce.servicehandler.utils.ConstraintsUtils.buildHardConstraint;
import static org.opendaylight.transportpce.servicehandler.utils.ConstraintsUtils.buildSoftConstraint;

import java.util.Arrays;
import java.util.List;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.DiversityConstraints.DiversityType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.common.constraints.LinkIdentifierKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service.constraints.ServiceIdentifierListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraints;

/**
 * Class to test downgrading and updating Constraints .
 * @author Ahmed Helmy ( ahmad.helmy@orange.com )
 * @author Gilles Thouenon (gilles.thouenon@orange.com)
 */
public class DowngradeConstraintsTest {

    @Test
    public void testUpdateSoftConstraintsForCustomerCode() {
        // test no addition when hard customer-code is null or empty
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no customer code",
            generatedSoftConstraints.getCustomerCode());
        List<String> softCustomerCode = Arrays.asList("soft-customer-code 3", "soft-customer-code 4");
        initialSoftConstraints =
            buildSoftConstraint(softCustomerCode, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getCustomerCode(), generatedSoftConstraints.getCustomerCode());

        // test addition of hard customer-code when no soft customer-code
        List<String> hardCustomerCode = Arrays.asList("hard-customer-code 1", "hard-customer-code 2");
        initialHardConstraints =
            buildHardConstraint(hardCustomerCode, false, null, null, null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain the customer code of hard constraint",
            generatedSoftConstraints.getCustomerCode(),
            initialHardConstraints.getCustomerCode());
        // test addition of hard customer-code when existing soft customer-code
        initialSoftConstraints =
            buildSoftConstraint(softCustomerCode, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat("updated soft constraints should contain 4 customer code",
            generatedSoftConstraints.getCustomerCode(), hasSize(4));
        assertThat(generatedSoftConstraints.getCustomerCode(),
            containsInAnyOrder("hard-customer-code 1", "hard-customer-code 2", "soft-customer-code 3",
                "soft-customer-code 4"));
    }

    @Test
    public void testUpdateSoftConstraintsForDiversity() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no diversity constraint",
            generatedSoftConstraints.getDiversity());
        List<String> softDiversityServiceid = Arrays.asList("soft-service 3");
        initialSoftConstraints =
            buildSoftConstraint(null, false, softDiversityServiceid, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals(initialSoftConstraints.getDiversity(), generatedSoftConstraints.getDiversity());

        // test addition of hard diversity when no soft diversity
        List<String> hardDiversityServiceid = Arrays.asList("hard-service 1", "hard-service 2");
        initialHardConstraints =
            buildHardConstraint(null, false, hardDiversityServiceid, null, null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain the diversity of hard constraint",
            generatedSoftConstraints.getDiversity(),
            initialHardConstraints.getDiversity());

        // test addition of hard diversity when existing soft diversity
        initialSoftConstraints =
            buildSoftConstraint(null, false, softDiversityServiceid, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat("updated soft constraints should contain diversity with 3 services",
            generatedSoftConstraints.getDiversity().getServiceIdentifierList().size(), is(3));
        assertEquals("updated soft constraints should have diversity type of serial",
            DiversityType.Serial,
            generatedSoftConstraints.getDiversity().getDiversityType());
        assertThat(generatedSoftConstraints.getDiversity().getServiceIdentifierList(),
            IsMapContaining.hasKey(new ServiceIdentifierListKey("hard-service 1")));
        assertThat(generatedSoftConstraints.getDiversity().getServiceIdentifierList(),
            IsMapContaining.hasKey(new ServiceIdentifierListKey("hard-service 2")));
        assertThat(generatedSoftConstraints.getDiversity().getServiceIdentifierList(),
            IsMapContaining.hasKey(new ServiceIdentifierListKey("soft-service 3")));
    }

    @Test
    public void testUpdateSoftConstraintsForExclude() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no exclude constraint",
            generatedSoftConstraints.getExclude());

        initialSoftConstraints = buildSoftConstraint(null, false, null, "link", null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should not be changed",
            initialSoftConstraints.getExclude(), generatedSoftConstraints.getExclude());

        // test addition of hard exclude with fiber list when no soft exclude
        initialHardConstraints =
            buildHardConstraint(null, false, null, "fiber1", null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain the exclude constraint of hard constraint",
            generatedSoftConstraints.getExclude(), initialHardConstraints.getExclude());

        // test addition of hard exclude with fiber list when existing soft
        // exclude with fiber list
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, "fiber2", null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat("updated soft constraints should contain exclude with 3 fiber bundles",
            generatedSoftConstraints.getExclude().getFiberBundle().size(), is(3));
        assertThat(generatedSoftConstraints.getExclude().getFiberBundle(),
            containsInAnyOrder("fiber-1", "fiber-2", "fiber-3"));

        // test addition of hard exclude with link list when no soft exclude
        initialHardConstraints = buildHardConstraint(null, false, null, "link1", null, null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain the exclude constraint of hard constraint",
            generatedSoftConstraints.getExclude(), initialHardConstraints.getExclude());

        // test addition of hard exclude with link list when existing soft
        // exclude with link list
        initialSoftConstraints = buildSoftConstraint(null, false, null, "link2", null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat("updated soft constraints should contain exclude with 3 links",
            generatedSoftConstraints.getExclude().getLinkIdentifier().size(), is(3));
        assertThat(generatedSoftConstraints.getExclude().getLinkIdentifier(),
            IsMapContaining.hasKey(new LinkIdentifierKey("link-id 1", "openroadm-topology")));
        assertThat(generatedSoftConstraints.getExclude().getLinkIdentifier(),
            IsMapContaining.hasKey(new LinkIdentifierKey("link-id 2", "openroadm-topology")));
        assertThat(generatedSoftConstraints.getExclude().getLinkIdentifier(),
            IsMapContaining.hasKey(new LinkIdentifierKey("link-id 3", "openroadm-topology")));
    }

    @Test
    public void testUpdateSoftConstraintsForInclude() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no include constraint",
            generatedSoftConstraints.getInclude());

        initialSoftConstraints = buildSoftConstraint(null, false, null, null, "link", null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should not be changed",
            initialSoftConstraints.getInclude(), generatedSoftConstraints.getInclude());

        // test addition of hard include with fiber list when no soft include
        initialHardConstraints =
            buildHardConstraint(null, false, null, null, "fiber1", null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain the include constraint of hard constraint",
            generatedSoftConstraints.getInclude(), initialHardConstraints.getInclude());

        // test addition of hard include with fiber list when existing soft
        // include with fiber list
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, "fiber2", null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat("updated soft constraints should contain exclude with 3 fiber bundles",
            generatedSoftConstraints.getInclude().getFiberBundle().size(), is(3));
        assertThat(generatedSoftConstraints.getInclude().getFiberBundle(),
            containsInAnyOrder("fiber-1", "fiber-2", "fiber-3"));

        // test addition of hard include with link list when no soft include
        initialHardConstraints = buildHardConstraint(null, false, null, null, "link1", null, false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain the include constraint of hard constraint",
            generatedSoftConstraints.getInclude(), initialHardConstraints.getInclude());

        // test addition of hard include with link list when existing soft
        // include with link list
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, "link2", null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertThat("updated soft constraints should contain include with 3 links",
            generatedSoftConstraints.getInclude().getLinkIdentifier().size(), is(3));
        assertThat(generatedSoftConstraints.getInclude().getLinkIdentifier(),
            IsMapContaining.hasKey(new LinkIdentifierKey("link-id 1", "openroadm-topology")));
        assertThat(generatedSoftConstraints.getInclude().getLinkIdentifier(),
            IsMapContaining.hasKey(new LinkIdentifierKey("link-id 2", "openroadm-topology")));
        assertThat(generatedSoftConstraints.getInclude().getLinkIdentifier(),
            IsMapContaining.hasKey(new LinkIdentifierKey("link-id 3", "openroadm-topology")));
    }

    @Test
    public void testUpdateSoftConstraintsForLatency() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no latency constraint",
            generatedSoftConstraints.getLatency());

        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, Double.valueOf(12.2), false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should not be changed",
            initialSoftConstraints.getLatency(), generatedSoftConstraints.getLatency());
        assertEquals("updated soft constraints value should be '12.2'",
            (float) 12.2, generatedSoftConstraints.getLatency().getMaxLatency().floatValue(), 0.0f);

        // test addition of hard latency when no soft latency
        initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, Double.valueOf(16.59), false, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints value should be '16.59'",
            (float) 16.59, generatedSoftConstraints.getLatency().getMaxLatency().floatValue(), 0.0f);

        // test addition of hard latency when existing different soft latency
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, Double.valueOf(12.2), false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints value should be '12.2'",
            (float) 12.2, generatedSoftConstraints.getLatency().getMaxLatency().floatValue(), 0.0f);
    }

    @Test
    public void testUpdateSoftConstraintsForDistance() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no distance constraint",
            generatedSoftConstraints.getDistance());

        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, "750.2", null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should not be changed",
            initialSoftConstraints.getDistance(), generatedSoftConstraints.getDistance());
        assertEquals("updated soft constraints value should be '750.2'",
            (float) 750.2, generatedSoftConstraints.getDistance().getMaxDistance().floatValue(), 0.0f);

        // test addition of hard distance when no soft distance
        initialHardConstraints = buildHardConstraint(null, false, null, null, null, null, false, false, "555.5", null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints value should be '555.5'",
            (float) 555.5, generatedSoftConstraints.getDistance().getMaxDistance().floatValue(), 0.0f);

        // test addition of hard distance when existing different soft distance
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, "750.2", null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints value should be '555.5'",
            (float) 555.5, generatedSoftConstraints.getDistance().getMaxDistance().floatValue(), 0.0f);
    }

    @Test
    public void testUpdateSoftConstraintsForHopCount() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no hop-count constraint",
            generatedSoftConstraints.getHopCount());

        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, true, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should not be changed",
            initialSoftConstraints.getHopCount(), generatedSoftConstraints.getHopCount());
        assertEquals("updated soft constraints max-wdm-hop-count should be '3'",
            3, generatedSoftConstraints.getHopCount().getMaxWdmHopCount().intValue());
        assertEquals("updated soft constraints max-otn-hop-count should be '5'",
            5, generatedSoftConstraints.getHopCount().getMaxOtnHopCount().intValue());

        // test addition of hard hop-count when no soft hop-count
        initialHardConstraints = buildHardConstraint(null, false, null, null, null, null, true, false, null, null);
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain hard constraint",
            initialHardConstraints.getHopCount(), generatedSoftConstraints.getHopCount());
        assertEquals("updated soft constraints max-wdm-hop-count should be '3'",
            3, generatedSoftConstraints.getHopCount().getMaxWdmHopCount().intValue());
        assertEquals("updated soft constraints max-otn-hop-count should be '5'",
            5, generatedSoftConstraints.getHopCount().getMaxOtnHopCount().intValue());

        // test addition of hard hop-count when existing soft hop-count
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, true, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints max-wdm-hop-count should be '3'",
            3, generatedSoftConstraints.getHopCount().getMaxWdmHopCount().intValue());
        assertEquals("updated soft constraints max-otn-hop-count should be '5'",
            5, generatedSoftConstraints.getHopCount().getMaxOtnHopCount().intValue());
    }

    @Test
    public void testUpdateSoftConstraintsForCoRouting() {
        HardConstraints initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertNull("updated soft constraints should contain no co-routing constraint",
            generatedSoftConstraints.getCoRouting());

        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, true, false, null, "coRouting1");
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should not be changed",
            initialSoftConstraints.getCoRouting(), generatedSoftConstraints.getCoRouting());
        assertEquals("updated soft constraints should contain 2 co-routed services",
            2, generatedSoftConstraints.getCoRouting().getServiceIdentifierList().size());
        assertTrue(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                    .routing.ServiceIdentifierListKey("service 1"))
            .getServiceApplicability().getEquipment().getRoadmSrg());
        assertNull(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                        .routing.ServiceIdentifierListKey("service 1"))
            .getServiceApplicability().getLink());
        assertNull(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                        .routing.ServiceIdentifierListKey("service 2"))
            .getServiceApplicability().getEquipment());
        assertTrue(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                    .routing.ServiceIdentifierListKey("service 2"))
            .getServiceApplicability().getLink());

        // test addition of hard co-routing when no soft co-routing
        initialHardConstraints =
            buildHardConstraint(null, false, null, null, null, null, true, false, null, "coRouting2");
        initialSoftConstraints = buildSoftConstraint(null, false, null, null, null, null, false, false, null, null);
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain hard constraint",
            initialHardConstraints.getCoRouting(), generatedSoftConstraints.getCoRouting());
        assertEquals("updated soft constraints should contain 1 co-routed service",
            1, generatedSoftConstraints.getCoRouting().getServiceIdentifierList().size());
        assertTrue(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                        .routing.ServiceIdentifierListKey("service 3"))
            .getServiceApplicability().getSite());
        assertNull(generatedSoftConstraints.getCoRouting().getServiceIdentifierList()
            .get(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                        .routing.ServiceIdentifierListKey("service 3"))
            .getServiceApplicability().getLink());

        // test addition of hard hop-count when existing soft hop-count
        initialSoftConstraints =
            buildSoftConstraint(null, false, null, null, null, null, true, false, null, "coRouting1");
        generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
            initialHardConstraints, initialSoftConstraints);
        assertEquals("updated soft constraints should contain 3 co-routed service",
            3, generatedSoftConstraints.getCoRouting().getServiceIdentifierList().size());
        assertThat(generatedSoftConstraints.getCoRouting().getServiceIdentifierList(),
            IsMapContaining.hasKey(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                        .routing.ServiceIdentifierListKey("service 1")));
        assertThat(generatedSoftConstraints.getCoRouting().getServiceIdentifierList(),
            IsMapContaining.hasKey(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                        .routing.ServiceIdentifierListKey("service 2")));
        assertThat(generatedSoftConstraints.getCoRouting().getServiceIdentifierList(),
            IsMapContaining.hasKey(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co
                        .routing.ServiceIdentifierListKey("service 3")));
    }

    @Test
    public void testDowngradeHardConstraints() {
        HardConstraints initialHardConstraints = null;
        HardConstraints genHardConstraints = DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
        assertNotNull("generated hard-constraints should be empty, not null", genHardConstraints);
        initialHardConstraints = new HardConstraintsBuilder().build();
        genHardConstraints = DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
        assertNotNull("generated hard-constraints should be empty, not null", genHardConstraints);
        initialHardConstraints = buildHardConstraint(null, false, null, "link1", null, Double.valueOf(12.8), false,
            false, null, null);
        genHardConstraints = DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
        assertEquals("Latency value should be 12.8",
            (long) 12.8, genHardConstraints.getLatency().getMaxLatency().longValue());
        assertNull("generated hard constraints should only contain max-latency value",
            genHardConstraints.getCoRouting());
        assertNull("generated hard constraints should only contain max-latency value",
            genHardConstraints.getExclude());
        assertNull("generated hard constraints should only contain max-latency value",
            genHardConstraints.getInclude());
    }

    @Test
    public void testConvertToSoftConstraints() {
        HardConstraints initialHardConstraints = null;
        SoftConstraints genSoftConstraints = DowngradeConstraints.convertToSoftConstraints(initialHardConstraints);
        assertNotNull("generated soft constraints should never be null", genSoftConstraints);
        assertNull("generated soft constraints should be empty", genSoftConstraints.getExclude());
        assertNull("generated soft constraints should be empty", genSoftConstraints.getCoRouting());
        assertNull("generated soft constraints should be empty", genSoftConstraints.getLatency());

        List<String> hardCustomerCode = Arrays.asList("customer-code 1", "customer-code 2");
        initialHardConstraints =
            buildHardConstraint(hardCustomerCode, false, null, "link1", "node", null, false, false, null, null);
        genSoftConstraints = DowngradeConstraints.convertToSoftConstraints(initialHardConstraints);
        assertEquals("generated soft constraints should contain customer-code items", 2,
            genSoftConstraints.getCustomerCode().size());
        assertTrue(genSoftConstraints.getCustomerCode().contains("customer-code 1"));
        assertTrue(genSoftConstraints.getCustomerCode().contains("customer-code 2"));
        assertNotNull("generated soft constraints should contain exclude constraint", genSoftConstraints.getExclude());
        assertEquals("generated soft constraints should contain exclude constraint with one link-id",
            1, genSoftConstraints.getExclude().getLinkIdentifier().values().size());
        assertEquals("link-id 1",
            genSoftConstraints.getExclude().getLinkIdentifier().values().stream().findAny().get().getLinkId());
        assertEquals("openroadm-topology",
            genSoftConstraints.getExclude().getLinkIdentifier().values().stream().findAny().get().getLinkNetworkId());
        assertNotNull("generated soft constraints should contain include constraint", genSoftConstraints.getInclude());
        assertEquals("generated soft constraints should contain include constraint with two node-id",
            2, genSoftConstraints.getInclude().getNodeId().size());
        assertTrue(genSoftConstraints.getInclude().getNodeId().contains(new NodeIdType("node-id-1")));
        assertTrue(genSoftConstraints.getInclude().getNodeId().contains(new NodeIdType("node-id-3")));
        assertNull("generated soft constraints should not contain any latency constraint",
            genSoftConstraints.getLatency());
        assertNull("generated soft constraints should not contain any max-distance constraint",
            genSoftConstraints.getDistance());
        assertNull("generated soft constraints should not contain any co-routing constraint",
            genSoftConstraints.getCoRouting());
    }
}
