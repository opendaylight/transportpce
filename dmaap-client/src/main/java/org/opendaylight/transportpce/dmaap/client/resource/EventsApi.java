/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.opendaylight.transportpce.dmaap.client.resource.model.CreatedEvent;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationProcessService;

@Path("/events")
public interface EventsApi {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{topic}")
    CreatedEvent sendEvent(@PathParam("topic") String topic, PublishNotificationProcessService event);

}
