/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device.read;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import io.fd.honeycomb.transportpce.device.configuration.PmConfiguration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmListBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
public class PmReaderFactory implements ReaderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PmReaderFactory.class);
    public static final InstanceIdentifier<CurrentPmList> PM_CONTAINER_ID =
            InstanceIdentifier.create(CurrentPmList.class);

    @Inject
    @Named("device-databroker")
    private DataBroker dataBroker;

    @Inject
    private PmConfiguration pmConfiguration;


    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        registry.add(new BindingBrokerReader<>(PM_CONTAINER_ID, dataBroker,LogicalDatastoreType.OPERATIONAL,
                CurrentPmListBuilder.class));
        writeXMLDataToOper();
    }

    /**
     * Write xml data from {@link PmConfiguration}
     * to operational data.
     *
     */
    public boolean writeXMLDataToOper() {
        Boolean res = false;
        LOG.info("writting xml pm file data to oper datastore");
        CurrentPmList pmList = this.pmConfiguration.getDataPm();
        if (pmList !=null && pmList.getCurrentPmEntry().size() > 0) {
            LOG.info("Getting pm info from xml file for device ");
            CurrentPmListBuilder result  = new CurrentPmListBuilder(pmList);
            InstanceIdentifier<CurrentPmList> iid = InstanceIdentifier.create(CurrentPmList.class);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null ) {
                LOG.info("WriteTransaction is ok, copy currentPmList to oper datastore");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, result.build());
                Future<Void> future = writeTx.submit();
                try {
                    Futures.getChecked(future, ExecutionException.class);
                    LOG.info("currentPmList writed to oper datastore");
                    res = true;
                } catch (ExecutionException e) {
                    LOG.error("Failed to write currentPmList  to oper datastore");
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("currentPmList data operation gets from xml file is null !");
        }
        return res;
    }

}
