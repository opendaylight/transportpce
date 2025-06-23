/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util.rev181019;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.Location;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPmKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.MeasurementKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.Validity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev181019.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev181019.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.binding.BindingInstanceIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;


/*
 * a class with static methods to make the creation of test objects simpler.
 *
 * Format is simplified in a somewhat systematic way. For example to instantiate the
 * org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Interface object
 * you would call newRrrInterface161014("test interface");
 *
 * Generally the syntax will be the initial letters of the path after the revision number up to the actual object,
 * then the class name, followed by the revision number.
 *
 * Please extend this class with more revision objects if needed for future tests.
 */

public final class OlmUtilsTestObjects {

    private OlmUtilsTestObjects() {

    }

    public static
        org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev181019.resource.resource.resource.Interface
        newRrrInterface(String interfaceName) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.resource
            .rev181019.resource.resource.resource.InterfaceBuilder()
            .setInterfaceName(interfaceName)
            .build();
    }

    public static Resource newRResource(Resource resource) {
        return new ResourceBuilder()
                .setResource(resource.getResource())
                .build();
    }

    public static CurrentPmEntry newCurrentPmEntry(BindingInstanceIdentifier bindingInstanceIdentifier,
                                                   List<CurrentPm> currentPmList,
                                                   ResourceTypeEnum resourceType,
                                                   String resourceTypeExtension,
                                                   String retrievalTime) {
        Map<CurrentPmKey, CurrentPm> pmMap = new HashMap<>();
        for (CurrentPm currentPm : currentPmList) {
            pmMap.put(currentPm.key(), currentPm);
        }

        CurrentPmEntry cpe = new CurrentPmEntryBuilder()
                .setCurrentPm(pmMap)
                .setPmResourceInstance(bindingInstanceIdentifier)
                .setPmResourceType(resourceType)
                .setPmResourceTypeExtension(resourceTypeExtension)
                .setRetrievalTime(new DateAndTime(retrievalTime))
                .build();
        return cpe;
    }

    public static DataObjectIdentifier<Interface> newDataObjectIdentifierInterface(String interfaceName) {
        return DataObjectIdentifier
                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName))
                .build();
    }

    public static CurrentPm newCurrentPm(PmNamesEnum pmType,
                                         Map<MeasurementKey,Measurement> measurement,
                                         String extensionName,
                                         Direction direction,
                                         Location location) {
        return new CurrentPmBuilder()
                .setType(pmType)
                .setDirection(direction)
                .setExtension(extensionName)
                .setLocation(location)
                .setMeasurement(measurement)
                .build();
    }

    public static Measurement newMeasurement(PmGranularity gran,
                                             PmDataType value,
                                             String parameterUnit,
                                             Validity validity) {
        return new MeasurementBuilder()
                .setGranularity(gran)
                .setPmParameterValue(value)
                .setPmParameterUnit(parameterUnit)
                .setValidity(validity)
                .build();
    }
}

