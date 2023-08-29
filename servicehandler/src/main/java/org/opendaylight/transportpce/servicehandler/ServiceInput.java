/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.resiliency.ServiceResiliency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceZEndBuilder;

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
        setServiceName(serviceCreateInput.getServiceName());
        setCommonId(serviceCreateInput.getCommonId());
        setConnectionType(serviceCreateInput.getConnectionType());
        setSdncRequestHeader(serviceCreateInput.getSdncRequestHeader());
        setHardConstraints(serviceCreateInput.getHardConstraints());
        setSoftConstraints(serviceCreateInput.getSoftConstraints());
        setServiceAEnd(serviceCreateInput.getServiceAEnd());
        setServiceZEnd(serviceCreateInput.getServiceZEnd());
        setCustomer(serviceCreateInput.getCustomer());
        setCustomerContact(serviceCreateInput.getCustomerContact());
        setServiceResiliency(serviceCreateInput.getServiceResiliency());
        setServiceReconfigure(false);
    }

    public ServiceInput(ServiceReconfigureInput serviceReconfigureInput) {
        setServiceName(serviceReconfigureInput.getServiceName());
        setNewServiceName(serviceReconfigureInput.getNewServiceName());
        setSdncRequestHeader(new SdncRequestHeaderBuilder()
                .setRequestId(serviceReconfigureInput.getServiceName() + "-reconfigure")
                .setRpcAction(RpcActions.ServiceReconfigure).build());
        setCommonId(serviceReconfigureInput.getCommonId());
        setConnectionType(serviceReconfigureInput.getConnectionType());
        setHardConstraints(serviceReconfigureInput.getHardConstraints());
        setSoftConstraints(serviceReconfigureInput.getSoftConstraints());
        setServiceAEnd(serviceReconfigureInput.getServiceAEnd());
        setServiceZEnd(serviceReconfigureInput.getServiceZEnd());
        setCustomer(serviceReconfigureInput.getCustomer());
        setCustomerContact(serviceReconfigureInput.getCustomerContact());
        setServiceResiliency(serviceReconfigureInput.getServiceResiliency());
        setServiceReconfigure(true);
    }

    public ServiceInput(ServiceDeleteInput serviceDeleteInput) {
        setServiceName(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName());
        setSdncRequestHeader(serviceDeleteInput.getSdncRequestHeader());
        setServiceReconfigure(false);
    }

    public ServiceInput(TempServiceCreateInput tempServiceCreateInput) {
        setServiceName(tempServiceCreateInput.getCommonId());
        setCommonId(tempServiceCreateInput.getCommonId());
        setConnectionType(tempServiceCreateInput.getConnectionType());
        setSdncRequestHeader(tempServiceCreateInput.getSdncRequestHeader());
        setHardConstraints(tempServiceCreateInput.getHardConstraints());
        setSoftConstraints(tempServiceCreateInput.getSoftConstraints());
        setServiceAEnd(tempServiceCreateInput.getServiceAEnd());
        setServiceZEnd(tempServiceCreateInput.getServiceZEnd());
        setCustomer(tempServiceCreateInput.getCustomer());
        setCustomerContact(tempServiceCreateInput.getCustomerContact());
        setServiceResiliency(tempServiceCreateInput.getServiceResiliency());
        setServiceReconfigure(false);
    }

    public ServiceInput(ServiceFeasibilityCheckInput serviceFeasibilityCheckInput) {
        setServiceName(serviceFeasibilityCheckInput.getCommonId());
        setCommonId(serviceFeasibilityCheckInput.getCommonId());
        setConnectionType(serviceFeasibilityCheckInput.getConnectionType());
        setSdncRequestHeader(serviceFeasibilityCheckInput.getSdncRequestHeader());
        setHardConstraints(serviceFeasibilityCheckInput.getHardConstraints());
        setSoftConstraints(serviceFeasibilityCheckInput.getSoftConstraints());
        setServiceAEnd(serviceFeasibilityCheckInput.getServiceAEnd());
        setServiceZEnd(serviceFeasibilityCheckInput.getServiceZEnd());
        setCustomer(serviceFeasibilityCheckInput.getCustomer());
        setCustomerContact(serviceFeasibilityCheckInput.getCustomerContact());
        setServiceResiliency(serviceFeasibilityCheckInput.getServiceResiliency());
        setServiceReconfigure(false);
    }

    public ServiceInput(TempServiceDeleteInput tempServiceDeleteInput) {
        String comId = tempServiceDeleteInput.getCommonId();
        setServiceName(comId);
        setCommonId(comId);
        setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId(comId).build());
        setServiceReconfigure(false);
    }

    public ServiceInput(ServiceRerouteInput serviceRerouteInput) {
        setServiceName(serviceRerouteInput.getServiceName());
        setSdncRequestHeader(serviceRerouteInput.getSdncRequestHeader());
        setServiceResiliency(serviceRerouteInput.getServiceResiliency());
        setServiceReconfigure(false);
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
                .setServiceAEnd(new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service
                        .create.input.ServiceAEndBuilder(serviceAEnd).build())
                .setServiceZEnd(new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service
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
