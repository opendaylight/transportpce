/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathOutput;

public interface OtnDeviceRendererService {
    OtnServicePathOutput setupOtnServicePath(OtnServicePathInput input);

    OtnServicePathOutput deleteOtnServicePath(OtnServicePathInput input);
}
