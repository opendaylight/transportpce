## Notifications specifications

This file contains a brief overview of the different notificationListeners used in this module,
and their current use.

#### TapiNetworkNotificationHandler

* Listens to the general `Notification`
* if notificationType is `NOTIFICATIONTYPEATTRIBUTEVALUECHANGE` and targetObjectType is `TOPOLOGYOBJECTTYPENODEEDGEPOINT`
then it updates the connections and the connectivity services with regards to the changes. It then send an
* `NbiNotification`for each connectivityService updated.

#### TapiPceNotificationHandler

* Listens to `ServicePathRpcresult` from the `pce` module.

For a `path-computation-result` this applies:
* if this result is a successful , then it creates connections from the `ServicePathResult`
and stores it in the database.
* if this result is pending, it will log a warning
* if this result is failed, or unkown (default) then it will log an error.

For a `cancel-resource-result` this applies:
* If the result is failed, then it logs this as info.
* If the result is pending, then it logs this as a warning.
* If the result is unknown, then it logs an error
* If the result is successful, then it deletes the connections and connectivity associated with the servicename
in the result.

#### TapiRendererNotificationHandler

* Listens to `RendererRpcResultSp` from the `renderer`module.

For a result of type `service-implementation-request` this applies:

* If status is pending or unknown (default) it will log this as a warning.
* If status is failed it will delete the associated connections and connectivity of the associated service.
* If status is successful it will update the connections and connectivity of the associated service,
and it will also send an `NbiNotification` that the service has been published to TAPI.

#### TapiServiceNotificationHandler

* Listens to `ServiceRpcResultSh` from the `servicehandler`module.
* Whenever triggered it will log an error message "Avoid dataBroker error " and the name of the databroker.
