package org.opendaylight.transportpce.tapi.utils;

import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNotificationListener implements org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification
        .rev181210.TapiNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNotificationListener.class);

    public TapiNotificationListener() {
        LOG.info("Tapi notification listener created??");
    }

    @Override
    public void onNotification(Notification notification) {
        LOG.info("New TAPI notification {}", notification);
    }
}
