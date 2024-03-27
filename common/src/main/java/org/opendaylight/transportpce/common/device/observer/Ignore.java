/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device.observer;

import org.slf4j.event.Level;

public class Ignore implements Subscriber {

    @Override
    public void event(Level level, String message) {
        return;
    }

    @Override
    public void error(String message) {
        return;
    }

    @Override
    public void warn(String message) {
        return;
    }

    @Override
    public String first(Level level) {
        return "";
    }

    @Override
    public String first(Level level, String defaultMessage) {
        return "";
    }

    @Override
    public String first(Level level, String defaultMessage, int count) {
        return "";
    }

    @Override
    public String last(Level level) {
        return "";
    }

    @Override
    public String last(Level level, String defaultMessage) {
        return "";
    }

    @Override
    public String[] messages(Level level) {
        return new String[0];
    }

}
