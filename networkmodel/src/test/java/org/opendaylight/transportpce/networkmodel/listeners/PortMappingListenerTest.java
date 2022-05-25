/*
 * Copyright © 2022 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.mapping.Mapping;


@RunWith(MockitoJUnitRunner.class)
public class PortMappingListenerTest {

    @Mock
    private NetworkModelService networkModelService;
    private PortMappingListener portMappingListenerSpy;

    @Before
    public void setUp() {
        portMappingListenerSpy = Mockito.spy(new PortMappingListener(networkModelService));
        doReturn("NodeID").when(portMappingListenerSpy).getNodeIdFromMappingDataTreeIdentifier(any());
    }

    @Test
    public void testOnDataTreeChangedWhenMappingOperAndAdminDidntChange() {
        final Collection<DataTreeModification<Mapping>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Mapping> ch = mock(DataTreeModification.class);
        changes.add(ch);
        @SuppressWarnings("unchecked") final DataObjectModification<Mapping> mappingObject =
                mock(DataObjectModification.class);
        final Mapping oldMapping = mock(Mapping.class);
        final Mapping newMapping = mock(Mapping.class);

        when(ch.getRootNode()).thenReturn(mappingObject);
        when(mappingObject.getDataBefore()).thenReturn(oldMapping);
        when(mappingObject.getDataAfter()).thenReturn(newMapping);
        when(oldMapping.getPortAdminState()).thenReturn("InService");
        when(oldMapping.getPortOperState()).thenReturn("InService");
        when(newMapping.getPortAdminState()).thenReturn("InService");
        when(newMapping.getPortOperState()).thenReturn("InService");

        portMappingListenerSpy.onDataTreeChanged(changes);
        verify(networkModelService, never()).updateOpenRoadmTopologies(anyString(), any(Mapping.class));
    }

    @Test
    public void testOnDataTreeChangedWhenMappingAdminChanged() {
        final Collection<DataTreeModification<Mapping>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Mapping> ch = mock(DataTreeModification.class);
        changes.add(ch);
        @SuppressWarnings("unchecked") final DataObjectModification<Mapping> mappingObject =
                mock(DataObjectModification.class);
        final Mapping oldMapping = mock(Mapping.class);
        final Mapping newMapping = mock(Mapping.class);

        when(ch.getRootNode()).thenReturn(mappingObject);
        when(mappingObject.getDataBefore()).thenReturn(oldMapping);
        when(mappingObject.getDataAfter()).thenReturn(newMapping);
        when(oldMapping.getPortAdminState()).thenReturn("InService");
        when(newMapping.getPortAdminState()).thenReturn("OutOfService");

        portMappingListenerSpy.onDataTreeChanged(changes);
        verify(networkModelService, times(1)).updateOpenRoadmTopologies(anyString(), any(Mapping.class));
    }

    @Test
    public void testOnDataTreeChangedWhenMappingOperChanged() {
        final Collection<DataTreeModification<Mapping>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Mapping> ch = mock(DataTreeModification.class);
        changes.add(ch);
        @SuppressWarnings("unchecked") final DataObjectModification<Mapping> mappingObject =
                mock(DataObjectModification.class);
        final Mapping oldMapping = mock(Mapping.class);
        final Mapping newMapping = mock(Mapping.class);

        when(ch.getRootNode()).thenReturn(mappingObject);
        when(mappingObject.getDataBefore()).thenReturn(oldMapping);
        when(mappingObject.getDataAfter()).thenReturn(newMapping);
        when(oldMapping.getPortAdminState()).thenReturn("InService");
        when(oldMapping.getPortOperState()).thenReturn("InService");
        when(newMapping.getPortAdminState()).thenReturn("InService");
        when(newMapping.getPortOperState()).thenReturn("OutOfService");

        portMappingListenerSpy.onDataTreeChanged(changes);
        verify(networkModelService, times(1)).updateOpenRoadmTopologies(anyString(), any(Mapping.class));
    }
}
