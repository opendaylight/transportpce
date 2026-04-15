/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.listener;

public class TapiSbiNotificationSubcriptionService {

    public TapiSbiNotificationSubcriptionService() {
    }

    // TODO: develop this class to provide subscription to required notification for TAPI-SBI, considering the following
    // Notification Tree fragment :
//    +--rw notif-subscription* [uuid]
//    |  +--ro notification* [notification-uuid]
//    |  |  +--ro notification-uuid    -> /tapi-common:context/tapi-notification:notification-context/notification/uuid
//    |  +--ro event-notification* [event-notification-uuid]
//    |  |  +--ro event-notification-uuid
//                         -> /tapi-common:context/tapi-notification:notification-context/event-notification/uuid
//    |  +--rw notification-channel
//    |  |  +--rw stream-address?     string
//    |  |  +--ro next-sequence-no?   uint64
//    |  |  +--rw local-id?           string
//    |  |  +--rw name* [value-name]
//    |  |     +--rw value-name    string
//    |  |     +--rw value?        string
//    |  +--rw subscription-filter* [local-id]
//    |  |  +--rw requested-notification-types*   notification-type
//    |  |  +--rw requested-object-types*         tapi-common:object-type
//    |  |  +--rw requested-layer-protocols*      tapi-common:layer-protocol-name
//    |  |  +--rw requested-object-identifier*    tapi-common:uuid
//    |  |  +--rw include-content?                boolean
//    |  |  +--rw local-id                        string
//    |  |  +--rw name* [value-name]
//    |  |     +--rw value-name    string
//    |  |     +--rw value?        string
//    |  +--rw subscription-state?     subscription-state
//    |  +--rw uuid                    uuid
//    |  +--rw name* [value-name]
//    |     +--rw value-name    string
//    |     +--rw value?        string

}
