/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

/**
 * Service interface for version-specific OpenConfig interface implementations.
 *
 * <p>Implementations are registered as OSGi services with a version property.
 * This allows dynamic service selection at runtime without classloader conflicts
 * between incompatible YANG model versions (OC200 vs OC560).
 */
public interface OpenConfigInterfacesVersionService extends OpenConfigInterfaces {

    /**
     * Get the version identifier.
     *
     * @return version string (e.g., "openconfig-200", "openconfig-560")
     */
    String getVersion();
}
