/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.stub;

import java.util.Optional;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MountPointServiceStub implements MountPointService {

    MountPoint returnedMountPoint;

    public MountPointServiceStub(MountPoint usedMountPoint) {
        this.returnedMountPoint = usedMountPoint;
    }

    @Override
    public Optional<MountPoint> getMountPoint(InstanceIdentifier<?> mountPoint) {
        if (returnedMountPoint == null) {
            return Optional.empty();
        }
        return Optional.of(returnedMountPoint);
    }

    @Override
    public <T extends MountPointListener> ListenerRegistration<T> registerListener(InstanceIdentifier<?> path,
        T listener) {
        return null;
    }
}
