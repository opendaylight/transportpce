/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.controllers.tpce.exception;

public class TechnicalException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1359762809539335449L;

    public TechnicalException() {
        super();
    }

    public TechnicalException(String message) {
        super(message);
    }
}
