.. _transportpce-user-guide:

TransportPCE User Guide
=======================

Overview
--------

TransportPCE describes an application running on top of the OpenDaylight
controller. Its primary function is to control an optical transport
infrastructure using a non-proprietary South Bound Interface (SBI). It may be
interconnected with Controllers of different layers (L2, L3 Controller…),
a higher layer Controller and/or an Orchestrator through non-proprietary
Application Programing Interfaces (APIs). Control includes the capability to
configure the optical equipment, and to provision services according to a
request coming from a higher layer controller and/or an orchestrator.
This capability may rely on the controller only or it may be delegated to
distributed (standardized) protocols.

For the time being, it provides very basic alarm/fault management and performance monitoring,
but this function might be externalized to improve the scalability.
A Graphical User Interface (`Transport PCE_GUI <https://gitlab.com/Orange-OpenSource/lfn/odl/tpce_gui>`__)
has been developped separately and is not proposed here since automated control
does not imply user interactions at the transport controller level.

TransportPCE modular architecture is described on the next diagram. Each main
function such as Topology management, Path Calculation Engine (PCE), Service
handler, Renderer responsible for the path configuration through optical
equipment and Optical Line Management (OLM) is associated with a generic block
relying on open models, each of them communicating through published APIs.

.. figure:: ./images/TransportPCE-Diagram-Phosphorus.jpg
   :alt: TransportPCE architecture

   TransportPCE architecture

TransportPCE User-Facing Features
---------------------------------
-  **odl-transportpce**

   -  This feature contains all other features/bundles of TransportPCE project.
      If you install it, it provides all functions that the TransportPCE project
      can support.
      It exposes all Transportpce project specific models defined in "Service-path".
      These models complement OpenROADM models describing South and Northbound APIs, and define the
      data structure used to interconnect the generic blocks/functions described on the previous
      diagram.

-  **feature odl-transportpce-tapi**

   -  This feature provides transportPCE a limited support of TAPI version 2.1.1 Northbound interface.

-  **feature odl-transportpce-inventory**

   -  This feature is considered experimental. It provides transportPCE with an external connector to
      a MariaDB inventory currently limited to OpenROADM 1.2.1 devices.

-  **feature odl-transportpce-dmaap-client**

   -  This feature is considered experimental. It provides a REST client in order to send TPCE notifications
      to ONAP Dmaap Message router.

-  **feature odl-transportpce-nbinotifications**

   -  This feature is considered experimental. It provides transportPCE with connectors in order to read/write
      notifications stored in topics of a Kafka server.

How To Start
------------

Preparing for Installation
~~~~~~~~~~~~~~~~~~~~~~~~~~

1. Devices must support the standard OpenROADM Models more precisely versions 1.2.1, 2.2.1 or 7.1.0.
   Since Magnesium SR0, an OTN experimental support is provided for OpenROADM devices 2.2.1.
   Magnesium SR2 is the first release managing end-to-end OTN services, as OCH-OTU4,
   structured ODU4 or again 10GE-ODU2e services.
   This has also been extended to be supported on 7.1 devices

2. Devices must support configuration through NETCONF protocol/API.



Installation Feature
~~~~~~~~~~~~~~~~~~~~

Creation of services with TransportPCE controller on real optical devices takes a rather long while,
due to the fact that the output optical power level modification on interfaces requires time for stabilisation
level. By default values of TransportPCE timers are those recommended by OpenROADM MSA, respectively 120 000
and 20 000 seconds.
When running TransportPCE controller with honeynode simulators, which is the case of all TransportPCE functional tests,
we don't need so important timer values. You can considerably speed tests using respectively 3000 and 2000 seconds.
To that end, before starting karaf of the OpenDaylight TransportPCE controller, set OLM_TIMER1 and OLM_TIMER2 as environment variables.
For example::

    export OLM_TIMER1=3000 OLM_TIMER2=2000

To come back with per default values for these timers, just logout from OpenDaylight controller, unset your
environment variables, and start again the controller::

    unset OLM_TIMER1 OLM_TIMER2


Run OpenDaylight and install TransportPCE Service *odl-transportpce* as below::

   feature:install odl-transportpce

if you need TAPI limited support, then run::

   feature:install odl-transportpce-tapi

When installing the TAPI feature, you might encounter a heap memory size problem in Karaf.
In that case, consider increasing Karaf heap memory size.
For example by modifying the environment variables JAVA_MIN_MEM and JAVA_MAX_MEM before starting Karaf::

   export JAVA_MIN_MEM=1024M
   export JAVA_MAX_MEM=4069M
In Chlorine, installation of odl-transportpce-tapi feature may cause some other transportpce OSGi bundle to be
in failure. To restore the situation, just log out and log back in.

If you need the inventory external connector support limited to 1.2.1 OpenROADM devices, then run::

   feature:install odl-transportpce-inventory

If you need the Dmaap connector support, before running Opendaylight, set DMAAP_BASE_URL as environment variable.
For example, if the base url of your Dmaap server is "https://dmaap-mr:30226", then::

    export DMAAP_BASE_URL=https://dmaap-mr:30226

If your Dmaap server provides https connection through a self-signed certificate, do not forget to add the certificate
to the JAVA truststore::

    echo -n | openssl s_client -showcerts -connect dmaap-mr:30226 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/dmaap.crt
    keytool -import -v -trustcacerts -alias dmaap -file /tmp/dmaap.crt -keystore /etc/ssl/certs/java/cacerts -keypass changeit -storepass changeit -noprompt

where dmaap-mr:30226 is the url of your Dmaap server.

Then run in karaf::

   feature:install odl-transportpce-dmaap-client

If you need the NBI-notifications support, before installing odl-transportpce-nbinotifications feature,
make sure to run ZooKeeper and then the Kafka server.
By default, it is considered that the Kafka server is installed in localhost and listens on the 9092 port,
if it isn't the case then set the KAFKA_SERVER environment variable of your system or
modify the file *'transportpce/features/odl-transportpce-nbinotifications
/src/main/resources/org.opendaylight.transportpce.nbinotifications.cfg'*::

   suscriber.server=${env:KAFKA_SERVER:-[IP_ADDRESS]:[PORT]}
   publisher.server=${env:KAFKA_SERVER:-[IP_ADDRESS]:[PORT]}

*where [IP_ADDRESS] and [PORT] are respectively the IP address and the port that host the Kafka server.*

After that, run in karaf::

   feature:install odl-transportpce-nbinotifications

.. note::

    In Chlorine release, the odl-transportpce-swagger feature is no longer functional. Issue still under investigation.


For a more detailed overview of the TransportPCE, see the :ref:`transportpce-dev-guide`.
