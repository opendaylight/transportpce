/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.result;

import java.util.List;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRenderingResult;
import org.opendaylight.transportpce.renderer.provisiondevice.OLMRenderingResult;

public class WeightedResultMessage implements Message {


    @Override
    public String deviceRenderingResultMessage(
        List<DeviceRenderingResult> renderingResults,
        String defaultErrorMessage,
        String defaultSuccessMessage) {

        boolean failed = false;

        // Return the first not successful message
        for (DeviceRenderingResult deviceRenderingResult : renderingResults) {
            if (!deviceRenderingResult.isSuccess()) {
                failed = true;

                String resultMessage = deviceRenderingResult.getResultMessage();
                if (resultMessage != null && !resultMessage.isEmpty()) {
                    return resultMessage;
                }
            }
        }

        if (failed) {
            // At least on device rendering result was a failure with an empty error message.
            return defaultErrorMessage;
        }

        // Return the first not empty result message
        for (DeviceRenderingResult deviceRenderingResult : renderingResults) {
            String resultMessage = deviceRenderingResult.getResultMessage();
            if (resultMessage != null && !resultMessage.isEmpty()) {
                return resultMessage;
            }
        }

        // No device rendering errors found, but there also is no success message
        return defaultSuccessMessage;
    }

    @Override
    public String olmRenderingResultMessage(
        List<OLMRenderingResult> renderingResults,
        String defaultErrorMessage,
        String defaultSuccessMessage) {

        boolean failed = false;

        // Return the first not successful message
        for (OLMRenderingResult deviceRenderingResult : renderingResults) {
            if (!deviceRenderingResult.isSuccess()) {
                failed = true;

                String resultMessage = deviceRenderingResult.getResultMessage();
                if (resultMessage != null && !resultMessage.isEmpty()) {
                    return resultMessage;
                }
            }
        }

        if (failed) {
            // At least on device rendering result was a failure with an empty error message.
            return defaultErrorMessage;
        }

        // Return the first not empty result message
        for (OLMRenderingResult deviceRenderingResult : renderingResults) {
            String resultMessage = deviceRenderingResult.getResultMessage();
            if (resultMessage != null && !resultMessage.isEmpty()) {
                return resultMessage;
            }
        }

        // No device rendering errors found, but there also is no success message
        return defaultSuccessMessage;
    }
}
