/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.test.stub;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMMountPointServiceAdapter;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;

public class MountPointServiceStub implements MountPointService {

    MountPoint returnedMountPoint;

    public MountPointServiceStub(MountPoint usedMountPoint) {
        this.returnedMountPoint = usedMountPoint;
    }

    @Override
    public Optional<MountPoint> findMountPoint(@NonNull DataObjectIdentifier<?> path) {
        if (returnedMountPoint == null) {
            return Optional.empty();
        }
        return Optional.of(returnedMountPoint);
    }

    @Override
    public @NonNull Registration registerListener(DataObjectReference<?> path, MountPointListener listener) {
        return new BindingDOMMountPointServiceAdapter(null, null).registerListener(path, listener);
    }
}
