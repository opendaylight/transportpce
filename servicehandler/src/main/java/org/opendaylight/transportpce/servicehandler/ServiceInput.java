/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.resiliency.ServiceResiliency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.create.input.ServiceZEndBuilder;

/**
 * Super class of {@link ServiceCreateInput} and {@link TempServiceCreateInput}.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
public class ServiceInput {
    private String serviceName;
    private String newServiceName;
    private String commonId;
    private ConnectionType connectionType;
    private SdncRequestHeader sdncRequestHeader;
    private HardConstraints hardConstraints;
    private SoftConstraints softConstraints;
    private ServiceEndpoint serviceAEnd;
    private ServiceEndpoint serviceZEnd;
    private String customer;
    private String customerContact;
    private ServiceResiliency serviceResiliency;
    private boolean serviceReconfigure;

    public ServiceInput(ServiceCreateInput serviceCreateInput) {
        this.serviceName = serviceCreateInput.getServiceName();
        this.commonId = serviceCreateInput.getCommonId();
        this.connectionType = serviceCreateInput.getConnectionType();
        this.sdncRequestHeader = serviceCreateInput.getSdncRequestHeader();
        this.hardConstraints = serviceCreateInput.getHardConstraints();
        this.softConstraints = serviceCreateInput.getSoftConstraints();
        this.serviceAEnd = serviceCreateInput.getServiceAEnd();
        this.serviceZEnd = serviceCreateInput.getServiceZEnd();
        this.customer = serviceCreateInput.getCustomer();
        this.customerContact = serviceCreateInput.getCustomerContact();
        this.serviceResiliency = serviceCreateInput.getServiceResiliency();
        this.serviceReconfigure = false;
    }

    public ServiceInput(ServiceReconfigureInput serviceReconfigureInput) {
        this.serviceName = serviceReconfigureInput.getServiceName();
        this.newServiceName = serviceReconfigureInput.getNewServiceName();
        this.sdncRequestHeader = new SdncRequestHeaderBuilder()
                .setRequestId(serviceReconfigureInput.getServiceName() + "-reconfigure")
                .setRpcAction(RpcActions.ServiceReconfigure)
                .build();
        this.commonId = serviceReconfigureInput.getCommonId();
        this.connectionType = serviceReconfigureInput.getConnectionType();
        this.hardConstraints = serviceReconfigureInput.getHardConstraints();
        this.softConstraints = serviceReconfigureInput.getSoftConstraints();
        this.serviceAEnd = serviceReconfigureInput.getServiceAEnd();
        this.serviceZEnd = serviceReconfigureInput.getServiceZEnd();
        this.customer = serviceReconfigureInput.getCustomer();
        this.customerContact = serviceReconfigureInput.getCustomerContact();
        this.serviceResiliency = serviceReconfigureInput.getServiceResiliency();
        this.serviceReconfigure = true;
    }

    public ServiceInput(ServiceDeleteInput serviceDeleteInput) {
        this.serviceName = serviceDeleteInput.getServiceDeleteReqInfo().getServiceName();
        this.sdncRequestHeader = serviceDeleteInput.getSdncRequestHeader();
        this.serviceReconfigure = false;
    }

    public ServiceInput(TempServiceCreateInput tempServiceCreateInput) {
        this.serviceName = tempServiceCreateInput.getCommonId();
        this.commonId = tempServiceCreateInput.getCommonId();
        this.connectionType = tempServiceCreateInput.getConnectionType();
        this.sdncRequestHeader = tempServiceCreateInput.getSdncRequestHeader();
        this.hardConstraints = tempServiceCreateInput.getHardConstraints();
        this.softConstraints = tempServiceCreateInput.getSoftConstraints();
        this.serviceAEnd = tempServiceCreateInput.getServiceAEnd();
        this.serviceZEnd = tempServiceCreateInput.getServiceZEnd();
        this.customer = tempServiceCreateInput.getCustomer();
        this.customerContact = tempServiceCreateInput.getCustomerContact();
        this.serviceResiliency = tempServiceCreateInput.getServiceResiliency();
        this.serviceReconfigure = false;
    }

    public ServiceInput(ServiceFeasibilityCheckInput serviceFeasibilityCheckInput) {
        this.serviceName = serviceFeasibilityCheckInput.getCommonId();
        this.commonId = serviceFeasibilityCheckInput.getCommonId();
        this.connectionType = serviceFeasibilityCheckInput.getConnectionType();
        this.sdncRequestHeader = serviceFeasibilityCheckInput.getSdncRequestHeader();
        this.hardConstraints = serviceFeasibilityCheckInput.getHardConstraints();
        this.softConstraints = serviceFeasibilityCheckInput.getSoftConstraints();
        this.serviceAEnd = serviceFeasibilityCheckInput.getServiceAEnd();
        this.serviceZEnd = serviceFeasibilityCheckInput.getServiceZEnd();
        this.customer = serviceFeasibilityCheckInput.getCustomer();
        this.customerContact = serviceFeasibilityCheckInput.getCustomerContact();
        this.serviceResiliency = serviceFeasibilityCheckInput.getServiceResiliency();
        this.serviceReconfigure = false;
    }

    public ServiceInput(TempServiceDeleteInput tempServiceDeleteInput) {
        String comId = tempServiceDeleteInput.getCommonId();
        this.serviceName = comId;
        this.commonId = comId;
        this.sdncRequestHeader = new SdncRequestHeaderBuilder()
                .setRequestId(comId)
                .build();
        this.serviceReconfigure = false;
    }

    public ServiceInput(ServiceRerouteInput serviceRerouteInput) {
        this.serviceName = serviceRerouteInput.getServiceName();
        this.sdncRequestHeader = serviceRerouteInput.getSdncRequestHeader();
        this.serviceResiliency = serviceRerouteInput.getServiceResiliency();
        this.serviceReconfigure = false;
    }

    public ServiceCreateInput getServiceCreateInput() {
        ServiceCreateInputBuilder serviceCreateInputBuilder = new ServiceCreateInputBuilder()
                .setCommonId(commonId)
                .setConnectionType(connectionType)
                .setSdncRequestHeader(sdncRequestHeader)
                .setHardConstraints(hardConstraints)
                .setSoftConstraints(softConstraints)
                .setServiceAEnd(new ServiceAEndBuilder(serviceAEnd).build())
                .setServiceZEnd(new ServiceZEndBuilder(serviceZEnd).build())
                .setCustomer(customer)
                .setCustomerContact(customerContact)
                .setServiceResiliency(serviceResiliency);
        if (isServiceReconfigure()) {
            serviceCreateInputBuilder.setServiceName(newServiceName);
        } else {
            serviceCreateInputBuilder.setServiceName(serviceName);
        }
        return serviceCreateInputBuilder.build();
    }

    public TempServiceCreateInput getTempServiceCreateInput() {
        return new TempServiceCreateInputBuilder()
                .setCommonId(commonId)
                .setConnectionType(connectionType)
                .setSdncRequestHeader(sdncRequestHeader)
                .setHardConstraints(hardConstraints)
                .setSoftConstraints(softConstraints)
                .setServiceAEnd(new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.temp.service
                        .create.input.ServiceAEndBuilder(serviceAEnd).build())
                .setServiceZEnd(new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.temp.service
                        .create.input.ServiceZEndBuilder(serviceZEnd).build())
                .setCustomer(customer)
                .setCustomerContact(customerContact)
                .setServiceResiliency(serviceResiliency)
                .build();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getCommonId() {
        return commonId;
    }

    public void setCommonId(String commonId) {
        this.commonId = commonId;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public SdncRequestHeader getSdncRequestHeader() {
        return sdncRequestHeader;
    }

    public void setSdncRequestHeader(SdncRequestHeader sdncRequestHeader) {
        this.sdncRequestHeader = sdncRequestHeader;
    }

    public ServiceEndpoint getServiceAEnd() {
        return serviceAEnd;
    }

    public void setServiceAEnd(ServiceEndpoint serviceAEnd) {
        this.serviceAEnd = serviceAEnd;
    }

    public ServiceEndpoint getServiceZEnd() {
        return serviceZEnd;
    }

    public void setServiceZEnd(ServiceEndpoint serviceZEnd) {
        this.serviceZEnd = serviceZEnd;
    }

    public HardConstraints getHardConstraints() {
        return hardConstraints;
    }

    public void setHardConstraints(HardConstraints hardConstraints) {
        this.hardConstraints = hardConstraints;
    }

    public SoftConstraints getSoftConstraints() {
        return softConstraints;
    }

    public void setSoftConstraints(SoftConstraints softConstraints) {
        this.softConstraints = softConstraints;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
    }

    public String getNewServiceName() {
        return newServiceName;
    }

    public void setNewServiceName(String newServiceName) {
        this.newServiceName = newServiceName;
    }

    public void setServiceResiliency(ServiceResiliency serviceResiliency) {
        this.serviceResiliency = serviceResiliency;
    }

    public ServiceResiliency getServiceResiliency() {
        return serviceResiliency;
    }

    public boolean isServiceReconfigure() {
        return serviceReconfigure;
    }

    public void setServiceReconfigure(boolean serviceReconfigure) {
        this.serviceReconfigure = serviceReconfigure;
    }
}
