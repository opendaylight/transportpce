/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class which handles strings in inventory project.
 *
 */
public final class StringUtils {
    public static final String DEFAULT_SQL_DATE = "yyyy-MM-dd HH:mm:ss";

    private StringUtils() {
        // hiding the constructor
    }

    /**
     * Returns the current timestamp formatted with {@link StringUtils#DEFAULT_SQL_DATE}.
     *
     * @see StringUtils#getTimestamp(Date)
     * @return Timestamp represenation of the given date
     */
    public static String getCurrentTimestamp() {
        return getTimestamp(new Date());
    }

    /**
     * This method will format the provided {@link Date} with the {@link StringUtils#DEFAULT_SQL_DATE} format.
     *
     * @param date link date
     * @return string represenation of the given date
     */
    public static String getTimestamp(Date date) {
        SimpleDateFormat myTimeStamp = new SimpleDateFormat(DEFAULT_SQL_DATE);
        return myTimeStamp.format(date);
    }

    /**
     * Checks the input object for null and if it's null returns a dash instead.
     *
     * @param object an object
     * @return if object is null a dash is returned, otherwise {@link Object#toString()}
     */
    public static String prepareDashString(Object object) {
        return prepareString(object, "");
    }

    /**
     * Checks the input object for null and if's null returns an empty string instead.
     *
     * @param object an object
     * @return if object is null an empty string is returned, otherwise {@link Object#toString()}
     */
    public static String prepareEmptyString(Object object) {
        return prepareString(object, "");
    }

    /**
     * Checks if the given object is null and returns its representation given by replacement.
     *
     */
    public static String prepareString(Object objectString, String replacement) {
        return objectString == null ? replacement : objectString.toString();
    }

    /**
     * Checks if the given object is null and returns -1 .
     *
     */
    public static String prepareEmptyInt(Object object) {
        return (object == null ? "-1" : object.toString());
    }
}
