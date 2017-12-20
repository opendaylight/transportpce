/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StatementBuilder {

    private final PreparedStatement statement;
    private int index;

    private StatementBuilder(PreparedStatement statement) {
        this.statement = statement;
        this.index = 1;
    }

    public StatementBuilder setParameter(String value) throws SQLException {
        this.statement.setString(this.index++, value);
        return this;
    }

    public StatementBuilder setParameter(Long value) throws SQLException {
        this.statement.setLong(this.index++, value);
        return this;
    }

    public StatementBuilder setParameters(String... strings) throws SQLException {
        for (int i = 0; i < strings.length; i++) {
            statement.setString(i + 1, strings[i]);
        }
        return this;
    }

    public StatementBuilder reset() {
        this.index = 1;
        return this;
    }

    public static StatementBuilder builder(PreparedStatement statement) {
        return new StatementBuilder(statement);
    }
}
