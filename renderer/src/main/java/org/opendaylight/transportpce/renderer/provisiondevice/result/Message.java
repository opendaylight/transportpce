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

public interface Message {

    /**
     * Turn a list of rendering results into one 'weighted' response.
     */
    String deviceRenderingResultMessage(List<DeviceRenderingResult> renderingResults,
                                        String defaultErrorMessage,
                                        String defaultSuccessMessage);

    /**
     * Turn a list of rendering results into one 'weighted' response.
     */
    String olmRenderingResultMessage(List<OLMRenderingResult> renderingResults,
                                     String defaultErrorMessage,
                                     String defaultSuccessMessage);

}
