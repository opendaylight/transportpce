/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.utils;


/**
 * Field Injection When Mocking Frameworks Fail.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public final class InjectField {

    private InjectField() {
    }

    public static void inject(final Object injectable, final String fieldname, final Object value) {
        try {
            final java.lang.reflect.Field field = injectable.getClass().getDeclaredField(fieldname);
            final boolean origionalValue = field.canAccess(injectable);
            field.setAccessible(true);
            field.set(injectable, value);
            field.setAccessible(origionalValue);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
