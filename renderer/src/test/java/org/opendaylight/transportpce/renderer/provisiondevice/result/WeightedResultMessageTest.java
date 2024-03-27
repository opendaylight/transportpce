/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.result;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRenderingResult;

public class WeightedResultMessageTest {

    private final String defaultSuccessMsg = "Default success";

    private final String defaultErrorMsg = "Default error";

    private final WeightedResultMessage weightedResultMessage = new WeightedResultMessage(
            defaultSuccessMsg,
            defaultErrorMsg
    );

    private List<DeviceRenderingResult> deviceRenderingResults;

    @Before
    public void setUp() throws Exception {
        deviceRenderingResults = new ArrayList<>();
    }

    @Test
    public void emptyResults_returnDefaultSuccessMessage() {
        Assert.assertEquals(defaultSuccessMsg, weightedResultMessage.resultMessage(new ArrayList<>()));
    }

    @Test
    public void mixedSuccessMessages_returnNonEmptySuccessMessage() {
        DeviceRenderingResult deviceRenderingResult = DeviceRenderingResult.ok(null, null, null);
        deviceRenderingResults.add(DeviceRenderingResult.ok(null, null, null));
        Assert.assertEquals(defaultSuccessMsg, weightedResultMessage.resultMessage(deviceRenderingResults));
    }

    @Test
    public void successWithEmptyMessage_returnDefaultSuccessMessage() {
        deviceRenderingResults.add(DeviceRenderingResult.failed(""));

        Assert.assertEquals(defaultErrorMsg, weightedResultMessage.resultMessage(deviceRenderingResults));
    }

    @Test
    public void failWithEmptyMessage_returnDefaultErrorMessage() {
        deviceRenderingResults.add(DeviceRenderingResult.failed(""));

        Assert.assertEquals(defaultErrorMsg, weightedResultMessage.resultMessage(deviceRenderingResults));
    }

    @Test
    public void mixedFailAndSuccessWithEmptyMessage_returnDefaultErrorMessage() {
        deviceRenderingResults.add(DeviceRenderingResult.ok(null, null, null));
        deviceRenderingResults.add(DeviceRenderingResult.failed(""));

        Assert.assertEquals(defaultErrorMsg, weightedResultMessage.resultMessage(deviceRenderingResults));
    }

    @Test
    public void mixedFailWithMixedMessagingEmptyMessage_returnNonEmptyErrorMessage() {
        deviceRenderingResults.add(DeviceRenderingResult.failed(""));
        String failed = "Operation not successful";
        deviceRenderingResults.add(DeviceRenderingResult.failed(failed));

        Assert.assertEquals(failed, weightedResultMessage.resultMessage(deviceRenderingResults));
    }

    @Test
    public void mixedFailAndSuccessWithMixedMessaging_returnNonEmptyErrorMessage() {
        deviceRenderingResults.add(DeviceRenderingResult.failed(""));
        deviceRenderingResults.add(DeviceRenderingResult.ok(null, null, null));
        String failed = "Operation not successful";
        deviceRenderingResults.add(DeviceRenderingResult.failed(failed));

        Assert.assertEquals(failed, weightedResultMessage.resultMessage(deviceRenderingResults));
    }
}