/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mc.capabilities.McCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mc.capabilities.McCapabilitiesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.DegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.McCapabilityProfile;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.McCapabilityProfileBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.McCapabilityProfileKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;


class PortMappingVersion710Test {

    @Test
    void oneDegree() {
        PortMappingVersion710 portMappingVersion710 = new PortMappingVersion710(
                mock(DataBroker.class), mock(DeviceTransactionManager.class));

        McCapabilityProfile mcProfile = new McCapabilityProfileBuilder()
                .setProfileName("mcProfile")
                .setMinSlots(Uint32.valueOf(3))
                .setMaxSlots(Uint32.valueOf(16))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("12.5"))
                .build();

        Map<McCapabilityProfileKey, McCapabilityProfile> avaialableMcProfiles = new HashMap<>();
        avaialableMcProfiles.put(mcProfile.key(), mcProfile);

        Degree d1 = new DegreeBuilder()
                .setDegreeNumber(Uint16.valueOf(1))
                .setMcCapabilityProfileName(Set.of("mcProfile"))
                .build();

        Map<Integer, Degree> degrees = new HashMap<>();
        degrees.put(d1.getDegreeNumber().intValue(), d1);

        McCapabilitiesKey expectedKey = new McCapabilitiesKey("DEG1-TTP-McCapabilityProfile{maxSlots=16, minSlots=3,"
                + " profileName=mcProfile, slotWidthGranularity=FrequencyGHz{value=12.5, UNITS=GHz}}");
        McCapabilities expected = new McCapabilitiesBuilder()
                .withKey(expectedKey)
                .setMinSlots(Uint32.valueOf(3))
                .setMaxSlots(Uint32.valueOf(16))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("12.5"))
                .build();

        Map<McCapabilitiesKey, McCapabilities> result = portMappingVersion710.createMcCapDegreeObject(
                degrees, avaialableMcProfiles, "node1");

        assertEquals(Map.of(expectedKey, expected), result);
    }

    @Test
    void twoDegreesTwoDifferentProfiles() {

        McCapabilityProfile mcProfile1 = new McCapabilityProfileBuilder()
                .setProfileName("mcProfile1")
                .setMinSlots(Uint32.valueOf(3))
                .setMaxSlots(Uint32.valueOf(16))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("12.5"))
                .build();

        Map<McCapabilityProfileKey, McCapabilityProfile> avaialableMcProfiles = new HashMap<>();
        avaialableMcProfiles.put(mcProfile1.key(), mcProfile1);

        McCapabilityProfile mcProfile2 = new McCapabilityProfileBuilder()
                .setProfileName("mcProfile2")
                .setMinSlots(Uint32.valueOf(4))
                .setMaxSlots(Uint32.valueOf(8))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("12.5"))
                .build();
        avaialableMcProfiles.put(mcProfile2.key(), mcProfile2);

        Degree d1 = new DegreeBuilder()
                .setDegreeNumber(Uint16.valueOf(1))
                .setMcCapabilityProfileName(Set.of("mcProfile1"))
                .build();

        Degree d2 = new DegreeBuilder()
                .setDegreeNumber(Uint16.valueOf(2))
                .setMcCapabilityProfileName(Set.of("mcProfile2"))
                .build();

        Map<Integer, Degree> degrees = new HashMap<>();
        degrees.put(d1.getDegreeNumber().intValue(), d1);
        degrees.put(d2.getDegreeNumber().intValue(), d2);

        McCapabilitiesKey expectedKey1 = new McCapabilitiesKey("DEG1-TTP-McCapabilityProfile{maxSlots=16, minSlots=3,"
                + " profileName=mcProfile1, slotWidthGranularity=FrequencyGHz{value=12.5, UNITS=GHz}}");
        McCapabilities expected1 = new McCapabilitiesBuilder()
                .withKey(expectedKey1)
                .setMinSlots(Uint32.valueOf(3))
                .setMaxSlots(Uint32.valueOf(16))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("12.5"))
                .build();

        McCapabilitiesKey expectedKey2 = new McCapabilitiesKey("DEG2-TTP-McCapabilityProfile{maxSlots=8, minSlots=4,"
                + " profileName=mcProfile2, slotWidthGranularity=FrequencyGHz{value=12.5, UNITS=GHz}}");
        McCapabilities expected2 = new McCapabilitiesBuilder()
                .withKey(expectedKey2)
                .setMinSlots(Uint32.valueOf(4))
                .setMaxSlots(Uint32.valueOf(8))
                .setSlotWidthGranularity(FrequencyGHz.getDefaultInstance("12.5"))
                .build();

        PortMappingVersion710 portMappingVersion710 = new PortMappingVersion710(
                mock(DataBroker.class), mock(DeviceTransactionManager.class));
        Map<McCapabilitiesKey, McCapabilities> result = portMappingVersion710.createMcCapDegreeObject(
                degrees, avaialableMcProfiles, "node2");
        assertEquals(Map.of(expectedKey1, expected1, expectedKey2, expected2), result);
    }
}
