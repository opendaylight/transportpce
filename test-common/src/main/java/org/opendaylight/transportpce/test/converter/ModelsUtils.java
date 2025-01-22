/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;


public final class ModelsUtils {


    public static final Set<YangModuleInfo> OPENROADM_MODEL_PATHS_71 = ImmutableSet.of(
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.alarm.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.alarm.pm.types.rev191129.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.amplifier.types.rev191129.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.attributes.rev200327.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.equipment.types.rev191129.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.link.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.node.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.optical.channel.types.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.state.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.common.types.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.equipment.states.types.rev191129.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.interfaces.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.layerrate.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.manifest.file.rev200327.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.network.resource.rev191129.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.otn.common.types.rev200327.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.pm.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.pm.types.rev200327.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.port.types.rev200327.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.probablecause.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.resource.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.resource.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.service.format.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.switching.pool.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.tca.rev200327.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.user.mgmt.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.database.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.de.operations.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.device.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.device.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.dhcp.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.ethernet.interfaces.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.file.transfer.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.fwdl.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.gcc.interfaces.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.gnmi.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.ip.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.ipv4.unicast.routing.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.ipv6.unicast.routing.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.key.chain.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.lldp.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.loopback.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.testsignal.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.media.channel.interfaces.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.network.media.channel.interfaces.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.optical.channel.interfaces.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.optical.operational.interfaces.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.optical.transport.interfaces.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529
            .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.otn.common.rev200327.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.otn.odu.interfaces.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.otn.otu.interfaces.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.otsi.group.interfaces.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.physical.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.pluggable.optics.holder.capability.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.port.capability.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.ppp.interfaces.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.prot.otn.linear.aps.rev200529.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.routing.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.rstp.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.security.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.de.swdl.rev200529.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.syslog.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.telemetry.types.rev191129.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.org.openroadm.wavelength.map.rev191129.YangModuleInfoImpl.getInstance());

    public static Set<YangModuleInfo> getYangModels71() {
        return OPENROADM_MODEL_PATHS_71;
    }

    private ModelsUtils() {
        throw new UnsupportedOperationException("Please do not instantiate utility class.");
    }
}
