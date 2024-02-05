/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsImpl;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OBJECTTYPE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OBJECTTYPEPROFILE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OBJECTTYPESERVICEINTERFACEPOINT;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OBJECTTYPETAPICONTEXT;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CONNECTIVITYOBJECTTYPE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CONNECTIVITYOBJECTTYPECONNECTION;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CONNECTIVITYOBJECTTYPECONNECTIONENDPOINT;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetSupportedNotificationTypes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetSupportedNotificationTypesInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetSupportedNotificationTypesOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetSupportedNotificationTypesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEATTRIBUTEVALUECHANGE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEOBJECTCREATION;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEOBJECTDELETION;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPEINTERRULEGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPELINK;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPENODE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPENODEEDGEPOINT;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPENODERULEGROUP;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public class GetSupportedNotificationTypesImpl implements GetSupportedNotificationTypes {

    private NbiNotificationsImpl nbiNotifications;

    public GetSupportedNotificationTypesImpl(NbiNotificationsImpl nbiNotifications) {
        this.nbiNotifications = nbiNotifications;
    }

    @Override
    public ListenableFuture<RpcResult<GetSupportedNotificationTypesOutput>> invoke(
            GetSupportedNotificationTypesInput input) {
        NotificationContext notificationContext = nbiNotifications.getNotificationContext();
        if (notificationContext == null) {
            return RpcResultBuilder.<GetSupportedNotificationTypesOutput>failed()
                .withError(ErrorType.APPLICATION, "Couldnt get Notification Context from Datastore")
                .buildFuture();
        }
        //TAPI 2.4 removes supported notification types from notif-subscription list and notification-context
        //No way to store what notification types are supported
        //Considers that by default all notification are supported
        Set<NOTIFICATIONTYPE> notificationTypeList = new HashSet<>();
        notificationTypeList.add(NOTIFICATIONTYPEOBJECTCREATION.VALUE);
        notificationTypeList.add(NOTIFICATIONTYPEOBJECTDELETION.VALUE);
        notificationTypeList.add(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE);
//
//        if (notificationContext.getNotifSubscription() == null) {
//            return RpcResultBuilder.success(new GetSupportedNotificationTypesOutputBuilder()
//                .setSupportedNotificationTypes(new HashSet<>())
//                .setSupportedObjectTypes(new HashSet<>()).build()).buildFuture();
//        }
//        Set<NOTIFICATIONTYPE> notificationTypeList = new HashSet<>();

        //TAPI 2.4 removes supported object types from notif-subscription list and notification-context
        //No way to store what object types are supported
        //Considers that by default all object are supported
        Set<OBJECTTYPE> objectTypeList = new HashSet<>();
        objectTypeList.add(OBJECTTYPESERVICEINTERFACEPOINT.VALUE);
        objectTypeList.add(OBJECTTYPETAPICONTEXT.VALUE);
        objectTypeList.add(OBJECTTYPEPROFILE.VALUE);
        objectTypeList.add(TOPOLOGYOBJECTTYPENODE.VALUE);
        objectTypeList.add(TOPOLOGYOBJECTTYPELINK.VALUE);
        objectTypeList.add(TOPOLOGYOBJECTTYPENODEEDGEPOINT.VALUE);
        objectTypeList.add(TOPOLOGYOBJECTTYPENODERULEGROUP.VALUE);
        objectTypeList.add(TOPOLOGYOBJECTTYPEINTERRULEGROUP.VALUE);
        objectTypeList.add(CONNECTIVITYOBJECTTYPE.VALUE);
        objectTypeList.add(CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE.VALUE);
        objectTypeList.add(CONNECTIVITYOBJECTTYPECONNECTIONENDPOINT.VALUE);
        objectTypeList.add(CONNECTIVITYOBJECTTYPECONNECTION.VALUE);
//        for (NotifSubscription notifSubscription:notificationContext.getNotifSubscription().values()) {
//            if (notifSubscription.getSupportedNotificationTypes() != null) {
//                notificationTypeList.addAll(notifSubscription.getSupportedNotificationTypes());
//            }
//            if (notifSubscription.getSupportedObjectTypes() != null) {
//                objectTypeList.addAll(notifSubscription.getSupportedObjectTypes());
//            }
//        }
        return RpcResultBuilder.success(new GetSupportedNotificationTypesOutputBuilder()
            .setSupportedNotificationTypes(notificationTypeList)
            .setSupportedObjectTypes(objectTypeList).build()).buildFuture();
    }

}
