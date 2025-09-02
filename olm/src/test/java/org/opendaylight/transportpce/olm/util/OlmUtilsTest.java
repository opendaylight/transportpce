/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import static org.mockito.Mockito.times;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250902.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifierBuilder;

public class OlmUtilsTest {

    // Verifies that the correct version is called when PmFetch is called
    @Test
    public void testPmFetch() {
        try (MockedStatic<OlmUtils710> olmUtils710MockedStatic = Mockito.mockStatic(OlmUtils710.class)) {
            OlmUtils.pmFetch(createGmInput(), null, OpenroadmNodeVersion._71);
            olmUtils710MockedStatic.verify(() -> OlmUtils710.pmFetch(createGmInput(), null), times(1));
        }
        try (MockedStatic<OlmUtils121> olmUtils121MockedStatic = Mockito.mockStatic(OlmUtils121.class)) {
            OlmUtils.pmFetch(createGmInput(), null, OpenroadmNodeVersion._121);
            olmUtils121MockedStatic.verify(() -> OlmUtils121.pmFetch(createGmInput(), null), times(1));
        }
        try (MockedStatic<OlmUtils221> olmUtils221MockedStatic = Mockito.mockStatic(OlmUtils221.class)) {
            OlmUtils.pmFetch(createGmInput(), null, OpenroadmNodeVersion._221);
            olmUtils221MockedStatic.verify(() -> OlmUtils221.pmFetch(createGmInput(), null), times(1));
        }
    }

    // Verifies that the correct version is called when PmFetchAll is called
    @Test
    public void testPmFetchAll() {
        try (MockedStatic<OlmUtils710> olmUtils710MockedStatic = Mockito.mockStatic(OlmUtils710.class)) {
            OlmUtils.pmFetchAll(createGmInput(), null, OpenroadmNodeVersion._71);
            olmUtils710MockedStatic.verify(() -> OlmUtils710.pmFetchAll(createGmInput(), null), times(1));
        }
        try (MockedStatic<OlmUtils121> olmUtils121MockedStatic = Mockito.mockStatic(OlmUtils121.class)) {
            OlmUtils.pmFetchAll(createGmInput(), null, OpenroadmNodeVersion._121);
            olmUtils121MockedStatic.verify(() -> OlmUtils121.pmFetchAll(createGmInput(), null), times(1));
        }
        try (MockedStatic<OlmUtils221> olmUtils221MockedStatic = Mockito.mockStatic(OlmUtils221.class)) {
            OlmUtils.pmFetchAll(createGmInput(), null, OpenroadmNodeVersion._221);
            olmUtils221MockedStatic.verify(() -> OlmUtils221.pmFetchAll(createGmInput(), null), times(1));
        }
    }

    private GetPmInput createGmInput() {
        return org.opendaylight.transportpce.olm.util.rev210618.OlmUtilsTestObjects.newGetPmInput210618(
                "ROADM-TEST",
                org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum.Interface,
                org.opendaylight.yang.gen.v1.http.org.transportpce.common.types
                        .rev250325.PmGranularity._15min,
                new ResourceIdentifierBuilder().setResourceName("test-interface-name").build());
    }
}
