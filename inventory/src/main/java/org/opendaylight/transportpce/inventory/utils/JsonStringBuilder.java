/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.utils;

public final class JsonStringBuilder {

    public static String getDevInfoJson() {

        String devInfoJson = "{ \"info\" : { \n"
            + "\"node-id\": \"$$NODE-ID$$\",\n"
            + "\"node-number\": \"$$NODE-NUMBER$$\",\n"
            + "\"node-type\":\"$$NODE-TYPE$$\",\n"
            + "\"clli\":\"$$CLLI$$\",\n"
            + "\"vendor\":\"$$VENDOR$$\",\n"
            + "\"model\":\"$$MODEL$$\",\n"
            + "\"serial-id\":\"$$SERIAL-ID$$\",\n"
            + "\"ipAddress\":\"$$IPADDRESS$$\",\n"
            + "\"prefix-length\":\"$$PREFIX-LENGTH$$\",\n"
            + "\"defaultGateway\":\"$$DEFAULTGATEWAY$$\",\n"
            + "\"source\":\"$$SOURCE$$\",\n"
            + "\"current-ipAddress\":\"$$CURRENT-IPADDRESS$$\",\n"
            + "\"current-prefix-length\":\"$$CURRENT-PREFIX-LENGTH$$\",\n"
            + "\"current-defaultGateway\":\"$$CURRENT-DEFAULTGATEWAY$$\",\n"
            + "\"macAddress\":\"$$MACADDRESS$$\",\n"
            + "\"softwareVersion\":\"$$SOFTWAREVERSION$$\",\n"
            + "\"openroadm-version\":\"$$OPENROADM-VERSION$$\",\n"
            + "\"template\":\"$$TEMPLATE$$\",\n"
            + "\"current-datetime\":\"$$CURRENT-DATETIME$$\",\n"
            + "\"geoLocation\": {\n"
            + "\"latitude\":\"$$LATITUDE$$\",\n"
            + "\"longitude\":\"$$LONGITUDE$$\"\n"
            + "},\n"
            + "\"max-degrees\":\"$$MAX-DEGREES$$\",\n"
            + "\"max-srgs\":\"$$MAX-SRGS$$\",\n"
            + "\"max-num-bin-15min-historical-pm\":\"$$MAX-NUM-BIN-15MIN-HISTORICAL-PM$$\",\n"
            + "\"max-num-bin-24hour-historical-pm\":\"$$MAX-NUM-BIN-24HOUR-HISTORICAL-PM$$\",\n"
            + "\"pending-sw\":{\n"
            + "\"sw-version\":\"$$SW-VERSION$$\",\n"
            + "\"sw-validation-timer\":\"$$SW-VALIDATION-TIMER$$\",\n"
            + "\"activation-date-time\":\"$$ACTIVATION-DATE-TIME$$\"\n"
            + "}\n"
            + "}\n"
            + "}";
        return devInfoJson;
    }

    private JsonStringBuilder() {
        //not called
    }

}
