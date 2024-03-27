/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.result;

import java.util.List;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRenderingResult;

public class WeightedResultMessage implements Message {

    private final String defaultSuccessMessage;

    private final String defaultErrorMessage;

    public WeightedResultMessage(String defaultErrorMessage) {
        this("", defaultErrorMessage);

    }

    public WeightedResultMessage(String defaultSuccessMessage, String defaultErrorMessage) {
        this.defaultSuccessMessage = defaultSuccessMessage;
        this.defaultErrorMessage = defaultErrorMessage;
    }

    @Override
    public String resultMessage(List<DeviceRenderingResult> renderingResults) {

        boolean failed = false;

        // Return the first not successful message
        for (OperationResult deviceRenderingResult : renderingResults) {
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
        for (OperationResult deviceRenderingResult : renderingResults) {
            String resultMessage = deviceRenderingResult.getResultMessage();
            if (resultMessage != null && !resultMessage.isEmpty()) {
                return resultMessage;
            }
        }

        // No device rendering errors found, but there also is no success message
        return defaultSuccessMessage;
    }
}
