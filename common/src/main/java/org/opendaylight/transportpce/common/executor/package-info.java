/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Bounded thread pools, per-request limits and RPC rejections for RPC implementations that offload blocking work.
 *
 * <p>An RPC implementation that must not block the caller's thread (for example the RESTCONF request thread)
 * submits its work to an executor and returns the resulting
 * {@link com.google.common.util.concurrent.ListenableFuture} instead. This package provides the pieces such an
 * implementation wires together:
 *
 * <ul>
 *   <li>{@link org.opendaylight.transportpce.common.executor.ExecutorPoolManager} (default implementation
 *       {@link org.opendaylight.transportpce.common.executor.DefaultExecutorPoolManager}) creates and owns the
 *       bounded executors work is submitted to.</li>
 *   <li>{@link org.opendaylight.transportpce.common.executor.limit.OperationLimit} caps how many operations of a
 *       kind, such as the requests of one RPC, are in progress at once, so that a flood of requests queues up as
 *       rejections rather than as unbounded work.</li>
 *   <li>{@link org.opendaylight.transportpce.common.executor.RpcRejections} (default implementation
 *       {@link org.opendaylight.transportpce.common.executor.DefaultRpcRejections}) turns a rejection, whether from
 *       a full pool or from an {@code OperationLimit}, into an RPC error for the client.</li>
 *   <li>{@link org.opendaylight.transportpce.common.executor.ExecutorPoolConfig} (default implementation
 *       {@link org.opendaylight.transportpce.common.executor.DefaultExecutorPoolConfig}) supplies the sizes,
 *       limits and shutdown timeout the other three read, so an operator can change them without changing code.</li>
 * </ul>
 *
 * <p>Everything else is a supporting detail of {@code DefaultExecutorPoolManager} and lives under
 * {@link org.opendaylight.transportpce.common.executor.pool}: the pool abstraction it creates, the shutdown
 * strategies it can be given, and the name/key types ({@code ThreadPoolName}, {@code RequestLimitName},
 * {@code OperationQueueName}, {@code PoolSize}) used to look values up in an {@code ExecutorPoolConfig}.
 *
 * <h2>Typical use</h2>
 *
 * <h3>Configuring pools, limits and queues</h3>
 *
 * <p>In OSGi, {@code DefaultExecutorPoolConfig} is activated from {@code org.opendaylight.transportpce.executorpool
 * .cfg} and injected as an {@code ExecutorPoolConfig} {@code @Reference}; components never construct it themselves.
 * Besides the shared defaults ({@code threads}, {@code queueSize}, {@code shutDownTimeoutSeconds}), the file can
 * override individual pools, RPCs and operation queues by name:
 * <pre>{@code
 * threads=8
 * queueSize=128
 * olm-get-pm.threads=16
 * service-delete.maxRequests=32
 * }</pre>
 *
 * <p>Outside OSGi, such as in lighty or in tests, construct {@code DefaultExecutorPoolConfig} directly: with no
 * arguments for the built-in defaults, or with explicit sizes and limits to override a specific pool, RPC, or
 * queue while leaving the rest at their defaults:
 * <pre>{@code
 * ExecutorPoolConfig poolConfig = new DefaultExecutorPoolConfig();
 *
 * ExecutorPoolConfig tunedConfig = new DefaultExecutorPoolConfig(
 *     Duration.ofSeconds(20),
 *     new PoolSize(4, 64),
 *     Map.of(ThreadPoolName.OLM_GET_PM, new PoolSize(16, 64)),
 *     Map.of(RequestLimitName.SERVICE_DELETE, 32));
 * }</pre>
 *
 * <h3>Offloading blocking work</h3>
 *
 * <p>Offloading an RPC's blocking work to a bounded executor, so a full pool fails the request instead of
 * blocking the caller (see {@code GetPmImpl}):
 * <pre>{@code
 * // once, when the component activates
 * ExecutorPoolManager pools = new DefaultExecutorPoolManager(poolConfig);
 * ListeningExecutorService executor = pools.executorService(MyRpcImpl.class, ThreadPoolName.OLM_GET_PM);
 * RpcRejections rpcRejections = new DefaultRpcRejections();
 *
 * // per request
 * try {
 *     return executor.submit(() -> doWork(input));
 * } catch (RejectedExecutionException e) {
 *     return rpcRejections.rejected(executor, ThreadPoolName.OLM_GET_PM, e);
 * }
 *
 * // when the component deactivates, which cancels requests still queued or running
 * pools.close();
 * }</pre>
 *
 * <h3>Limiting concurrent requests</h3>
 *
 * <p>Limiting how many requests of one RPC may be in progress at once, independently of the executor's queue (see
 * {@code RequestLimit} in the {@code renderer} module):
 * <pre>{@code
 * OperationLimit limit = new OperationLimit(maxRequests);
 *
 * // per request
 * Optional<OperationPermit> permit = limit.tryAcquire();
 * if (permit.isEmpty()) {
 *     return rpcRejections.tooManyRequests(rpcName);
 * }
 * ListenableFuture<RpcResult<T>> result = startRequest();
 * result.addListener(permit.orElseThrow()::release, MoreExecutors.directExecutor());
 * return result;
 * }</pre>
 */
package org.opendaylight.transportpce.common.executor;
