/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.transform;

import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating RPC transformer instances.
 */
public final class TransformerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerFactory.class);

    private TransformerFactory() {
        // Prevent instantiation
    }

    /**
     * Create a transformer based on configuration.
     *
     * @param config Proxy configuration
     * @return RpcTransformer instance
     */
    public static RpcTransformer createTransformer(ProxyConfig config) {
        if (!config.isTransformationEnabled()) {
            LOG.info("Transformation disabled, using NoOpTransformer");
            return new NoOpTransformer();
        }

        String transformerClass = config.getTransformerClass();
        if (transformerClass == null || transformerClass.isEmpty()) {
            LOG.warn("Transformation enabled but no transformer class specified, using NoOpTransformer");
            return new NoOpTransformer();
        }

        try {
            LOG.info("Loading transformer class: {}", transformerClass);
            Class<?> clazz = Class.forName(transformerClass);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            if (!(instance instanceof RpcTransformer)) {
                LOG.error("Class {} does not implement RpcTransformer interface", transformerClass);
                return new NoOpTransformer();
            }

            LOG.info("Successfully loaded transformer: {}", transformerClass);
            return (RpcTransformer) instance;

        } catch (Exception e) {
            LOG.error("Failed to load transformer class: {}", transformerClass, e);
            LOG.warn("Falling back to NoOpTransformer");
            return new NoOpTransformer();
        }
    }
}
