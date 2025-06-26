## Notifications specifications

This file contains a brief overview of the different notificationListeners used in this module, 
and their current use. 

#### NetworkModelNotificationHandler

* Listens to `TopologyUpdateResult` from the `NetworkModel`module.
* Only outputs logs of the notification statuses.

#### PceNotificationHandler

* Listens to `ServicePathRpcResult` from `transportpce.pce.rev240205`.
* if this result is a successful `PathComputationResult`, then it will create a new service and service path.
* if this result is a failed or unknown `PatchComputationResult`, then it will forward this as a NbiNotification.
* if this result is a sucessful `CancelResourceResult`, then it will delete the service fetched from the notification.
* It will send `NbiNotifications` about the status of the deletion.

#### RendererNotificationHandler

* Listens to `RendererRpcResultSp` from the `Renderer`module.

For `service-implementation-request` this applies.
* if the request is successful , then it tries to start the service. The result is forwarded as an `NbiNotification`
* if the request is failed, then it forward this as a `NbiNotification`.
* if the request is pending or unknown (default) then it logs this result as warning

For `service-delete` this applies:
* if the request is successful It tries to delete the service name from the otn-topology.
* if the request is pending it logs this as warning
* if the request failed it forwards this as an `NbiNotification`
* if the request unknown status (default), it logs this as an error.

#### ServiceListener

* Listens to `DataTreeChanged`
* if `DELETE` it do nothing
* if `DELETE` is combined with a service name that is beeing rerouted it will try to create a new service with the 
service name from the notification. This is actually rerouting step 2.
* if `WRITE` with a node that is taken out of service it forwards this as an NbiNotification.
Further if the `adminstrativeState` is `inService`, and there still exist a valid path, it will try to reroute the service 
by adding it to the rerouting services and then delete the service. This will trigger a new `DataTreeChanged`
notification that will start rerouting step 2 above.
* if `WRITE` with a node that is taken into service, and the `administrativeState` is `ìnService` then it
forwards this as an `NbiNotification`.





