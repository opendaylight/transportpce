/*
 * Copyright © 2017 ATT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullKafkaPublisherImpl implements KafkaPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(NullKafkaPublisherImpl.class);
    private static volatile KafkaPublisher sInstance = null;

    public static KafkaPublisher getPublisher() {
        if (sInstance == null) {
            synchronized (KafkaPublisher.class) {
                if (sInstance == null) {
                    LOG.info("Null Kafka Publisher set again");
                    sInstance = new NullKafkaPublisherImpl();
                }
            }
        }
        return sInstance;
    }

    public String publishNotification(String topicName, String key, String message) {
        LOG.info("kakfaPublisher SKIPPED: Topic {} Key {} Message {}", topicName, key, message);
        return "success";
    }

}
