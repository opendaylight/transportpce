/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util.rev161014;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PmDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Measurements;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Resource;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.measurements.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.measurements.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmMeasurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.pm.measurement.PmParameterNameBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.ResourceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.InterfaceBuilder;

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

    public static Interface
        newRrrInterface(String interfaceName) {
        return new InterfaceBuilder()
                .setInterfaceName(interfaceName)
                .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.Resource
        newRResource(org.opendaylight.yang.gen.v1.http.org.openroadm.resource
                                   .rev161014.resource.resource.Resource resource) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.ResourceBuilder()
                .setResource(resource)
                .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Resource
        newPmResource(org.opendaylight.yang.gen.v1.http.org.openroadm.resource
                                    .rev161014.resource.Resource resource,
                  ResourceType type) {
        return new ResourceBuilder()
                .setResourceType(type)
                .setResource(resource)
                .build();
    }

    public static Measurement newMeasurement(PmNamesEnum typeEnum,
                                             String extension,
                                             PmDataType parameterValue,
                                             String parameterUnit) {
        return new MeasurementBuilder()
                .setPmParameterName(new PmParameterNameBuilder()
                        .setType(typeEnum)
                        .setExtension(extension)
                        .build())
                .setPmParameterValue(parameterValue)
                .setValidity(PmMeasurement.Validity.Complete)
                .setLocation(PmMeasurement.Location.NearEnd)
                .setDirection(PmDirection.Rx)
                .setPmParameterUnit(parameterUnit)
                .build();
    }

    public static Measurements
        newMeasurements(Measurement measurement) {
        return new MeasurementsBuilder()
                .setMeasurement(measurement)
                .build();
    }

    public static CurrentPm
        newCurrentPm(String id,
                 PmGranularity gran,
                 Resource resource,
                 List<Measurements> measurementsList) {
        return (new CurrentPmBuilder())
                .setId(id)
                .setGranularity(gran)
                .setResource(resource)
                .setMeasurements(measurementsList)
                .build();
    }
}

