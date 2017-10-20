/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce;

import com.google.common.util.concurrent.FutureCallback;
import org.slf4j.Logger;

/**
 * Class to log future logging from datastore actions (write, modify, delete..).
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         bealf of Orange
 */
public class LoggingFuturesCallBack<V> implements FutureCallback<V> {

    private Logger log;
    private String message;

    public LoggingFuturesCallBack(String message, Logger log) {
        this.message = message;
        this.log = log;
    }

    @Override
    public void onFailure(Throwable ex) {
        log.warn(message, ex);

    }

    @Override
    public void onSuccess(V arg0) {
        log.info("Success! {} ", arg0);

    }

}