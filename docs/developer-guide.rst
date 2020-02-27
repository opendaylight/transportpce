.. _transportpce-dev-guide:

TransportPCE Developer Guide
============================

Overview
--------

TransportPCE describes an application running on top of the OpenDaylight
controller. Its primary function is to control an optical transport
infrastructure using a non-proprietary South Bound Interface (SBI). It may be
interconnected with Controllers of different layers (L2, L3 Controller…), a
higher layer Controller and/or an Orchestrator through non-proprietary
Application Programing Interfaces (APIs). Control includes the capability to
configure the optical equipment, and to provision services according to a
request coming from a higher layer controller and/or an orchestrator.
This capability may rely on the controller only or it may be delegated to
distributed (standardized) protocols.


Architecture
------------

TransportPCE modular architecture is described on the next diagram. Each main
function such as Topology management, Path Calculation Engine (PCE), Service
handler, Renderer \_responsible for the path configuration through optical
equipment\_ and Optical Line Management (OLM) is associated with a generic block
relying on open models, each of them communicating through published APIs.


.. figure:: ./images/tpce_architecture.jpg
   :alt: TransportPCE architecture

   TransportPCE architecture

Fluorine, Neon and Sodium releases of transportPCE are dedicated to the control
of WDM transport infrastructure. The WDM layer is built from colorless ROADMs
and transponders.

The interest of using a controller to provision automatically services strongly
relies on its ability to handle end to end optical services that spans through
the different network domains, potentially equipped with equipment coming from
different suppliers. Thus, interoperability in the optical layer is a key
element to get the benefit of automated control.

Initial design of TransportPCE leverages Open ROADM Multi-Source-Agreement (MSA)
which defines interoperability specifications, consisting of both Optical
interoperability and Yang data models.

Experimental support of OTN layer is introduced in Magnesium release of
OpenDaylight. By experimental, we mean not all features can be accessed through
northbound API based on RESTCONF encoded OpenROADM Service model. In the meanwhile,
"east/west" APIs shall be used to trigger a path computation in the PCE (using
path-computation -request RPC) and to create services (using otn-service-path RPC.
OTN support will be improved in the following magnesium releases.



Module description
~~~~~~~~~~~~~~~~~~

ServiceHandler
^^^^^^^^^^^^^^

Service Handler handles request coming from a higher level controller or an orchestrator
through the northbound API, as defined in the Open ROADM service model. Current
implementation addresses the following rpcs: service-create, temp-service-create,
service–delete, temp-service-delete, service-reroute, and service-restoration. It checks the
request consistency and trigs path calculation sending rpcs to the PCE. If a valid path is
returned by the PCE, path configuration is initiated relying on Renderer and OLM. At the
confirmation of a successful service creation, the Service Handler updates the service-
list/temp-service-list in the MD-SAL. For service deletion, the Service Handler relies on the
Renderer and the OLM to delete connections and reset power levels associated with the
service. The service-list is updated following a successful service deletion. In Neon SR0 is
added the support for service from ROADM to ROADM, which brings additional flexibility and
notably allows reserving resources when transponders are not in place at day one.
The full support of OTN services, including OTU, HO-ODU and LO-ODU will be introduced
in next release of Magnesium.

PCE
^^^^^^^^^^^^^^

The Path Computation Element (PCE) is the component responsible for path
calculation. An interface allows the Service Handler or external components such as an
orchestrator to request a path computation and get a response from the PCE
including the computed path(s) in case of success, or errors and indication of
the reason for the failure in case the request cannot be satisfied. Additional
parameters can be provided by the PCE in addition to the computed paths if
requested by the client module. An interface to the Topology Management module
allows keeping PCE aligned with the latest changes in the topology. Information
about current and planned services is available in the MD-SAL data store.

Current implementation of PCE allows finding the shortest path, minimizing either the hop
count (default) or the propagation delay. Wavelength is assigned considering a fixed grid of
96 wavelengths. In Neon SR0, the PCE calculates the OSNR, on the base of incremental
noise specifications provided in Open RAODM MSA. The support of unidirectional ports is
also added. PCE handles the following constraints as hard constraints:

-   **Node exclusion**
-   **SRLG exclusion**
-   **Maximum latency**

In Magnesium SR0, the interconnection of the PCE with GNPY (Gaussian Noise Python), an
open-source library developed in the scope of the Telecom Infra Project for building route
planning and optimizing performance in optical mesh networks, is fully supported.

If the OSNR calculated by the PCE is too close to the limit defined in OpenROADM
specifications, the PCE forwards through a REST interface to GNPY external tool the topology
and the pre-computed path translated in routing constraints. GNPy calculates a set of Quality of
Transmission metric for this path using its own library which includes models for OpenROADM.
The result is sent back to the PCE. If the path is validated, the PCE sends back a response to
the service handler. In case of invalidation of the path by GNPY, the PCE sends a new request to
GNPY, including only the constraints expressed in the path-computation-request initiated by the
Service Handler. GNPy then tries to calculate a path based on these relaxed constraints. The result
of the path computation is provided to the PCE which translates the path according to the topology
handled in transportPCE and forwards the results to the Service Handler.

GNPy relies on SNR and takes into account the linear and non-linear impairments to check feasibility.
In the related tests, GNPy module runs externally in a docker and the communication with T-PCE is
ensured via HTTPs.

Topology Management
^^^^^^^^^^^^^^^^^^^^^^^^

Topology management module builds the Topology according to the Network model
defined in OpenROADM. The topology is aligned with IETF I2RS RFC8345 model.
It includes several network layers:

-  **CLLI layer corresponds to the locations that host equipment**
-  **Network layer corresponds to a first level of disaggregation where we
   separate Xponders (transponder, muxponders or switchponders) from ROADMs**
-  **Topology layer introduces a second level of disaggregation where ROADMs
   Add/Drop modules ("SRGs") are separated from the degrees which includes line
   amplifiers and WSS that switch wavelengths from one to another degree**
-  **OTN layer introduced in Magnesium includes transponders as well as switch-ponders
   having the ability to switch OTN containers from client to line cards. SR0 release
   includes creation of the switching pool (used to model cross-connect matrices),
   tributary-ports and tributary-slots at the initial connection of NETCONF devices.
   However, the population of OTN links, and the adjustment of the tributary ports/slots
   pull occupancy when OTN services are created will be handled in later Magnesium release.**


Renderer
^^^^^^^^

The Renderer module, on request coming from the Service Handler through a service-
implementation-request /service delete rpc, sets/deletes the path corresponding to a specific
service between A and Z ends. The path description provided by the service-handler to the
renderer is based on abstracted resources (nodes, links and termination-points), as provided
by the PCE module. The renderer converts this path-description in a path topology based on
device resources (circuit-packs, ports,…).

The conversion from abstracted resources to device resources is performed relying on the
portmapping module which maintains the connections between these different resource types.
Portmapping module also allows to keep the topology independant from the devices releases.
In Neon (SR0), portmapping module has been enriched to support both openroadm 1.2.1 and 2.2.1
device models. The full support of openroadm 2.2.1 device models (both in the topology management
and the renderingfunction) has been added in Neon SR1. In Magnesium, portmapping is enriched with
the supported-interface-capability, OTN supporting-interfaces, and switching-pools (reflecting
cross-connection capabilities of OTN switch-ponders).

After the path is provided, the renderer first checks what are the existing interfaces on the
ports of the different nodes that the path crosses. It then creates missing interfaces. After all
needed interfaces have been created it sets the connections required in the nodes and
notifies the Service Handler on the status of the path creation. Path is created in 2 steps
(from A to Z and Z to A). In case the path between A and Z could not be fully created, a
rollback function is called to set the equipment on the path back to their initial configuration
(as they were before invoking the Renderer).

Magnesium brings the support of OTN services. SR0 supports the creation of OTU4, ODU4, ODU2/ODU2e
and ODU1 interfaces. The creation of these interfaces must be triggered through otn-service-path
RPC. Full support (service-implementation-request /service delete rpc, topology alignement after
the service has been created) will be provided in later releases of Magnesium.


OLM
^^^^^^^^

Optical Line Management module implements two main features: it is responsible
for setting up the optical power levels on the different interfaces, and is in
charge of adjusting these settings across the life of the optical
infrastructure.

After the different connections have been established in the ROADMS, between 2
Degrees for an express path, or between a SRG and a Degree for an Add or Drop
path; meaning the devices have set WSS and all other required elements to
provide path continuity, power setting are provided as attributes of these
connections. This allows the device to set all complementary elements such as
VOAs, to guaranty that the signal is launched at a correct power level
(in accordance to the specifications) in the fiber span. This also applies
to X-Ponders, as their output power must comply with the specifications defined
for the Add/Drop ports (SRG) of the ROADM. OLM has the responsibility of
calculating the right power settings, sending it to the device, and check the
PM retrieved from the device to verify that the setting was correctly applied
and the configuration was successfully completed.

Key APIs and Interfaces
-----------------------

External API
~~~~~~~~~~~~

North API, interconnecting the Service Handler to higher level applications
relies on the Service Model defined in the MSA. The Renderer and the OLM are
developed to allow configuring Open ROADM devices through a southbound
Netconf/Yang interface and rely on the MSA’s device model.

ServiceHandler Service
^^^^^^^^^^^^^^^^^^^^^^

-  RPC call

   -  service-create (given service-name, service-aend, service-zend)

   -  service-delete (given service-name)

   -  service-reroute (given service-name, service-aend, service-zend)

   -  service-restoration (given service-name, service-aend, service-zend)

   -  temp-service-create (given common-id, service-aend, service-zend)

   -  temp-service-delete (given common-id)

-  Data structure

   -  service list : made of services
   -  temp-service list : made of temporary services
   -  service : composed of service-name, topology wich describes the detailed path (list of used resources)

-  Notification

   - service-rpc-result : result of service RPC
   - service-notification : service has been added, modified or removed

Netconf Service
^^^^^^^^^^^^^^^

-  RPC call

   -  connect-device : PUT
   -  disconnect-device : DELETE
   -  check-connected-device : GET

-  Data Structure

   -  node list : composed of netconf nodes in topology-netconf

Internal APIs
~~~~~~~~~~~~~

Internal APIs define REST APIs to interconnect TransportPCE modules :

-   Service Handler to PCE
-   PCE to Topology Management
-   Service Handler to Renderer
-   Renderer to OLM

Pce Service
^^^^^^^^^^^

-  RPC call

   -  path-computation-request (given service-name, service-aend, service-zend)

   -  cancel-resource-reserve (given service-name)

-  Notification

   - service-path-rpc-result : result of service RPC

Renderer Service
^^^^^^^^^^^^^^^^

-  RPC call

   -  service-implementation-request (given service-name, service-aend, service-zend)

   -  service-delete (given service-name)

   -  otn-service-path (given service-name, service-aend, service-zend) used in SR0 as
      an intermediate solution to address directly the renderer from a REST NBI for
      otn-service creation. Otn service-creation through service-implementation-request
      call from the Service Handler will be supported in later Magnesium releases

-  Data structure

   -  service path list : composed of service paths
   -  service path : composed of service-name, path description giving the list of abstracted elements (nodes, tps, links)

-  Notification

   - service-path-rpc-result : result of service RPC

Topology Management Service
^^^^^^^^^^^^^^^^^^^^^^^^^^^

-  Data structure

   -  network list : composed of networks(openroadm-topology, netconf-topology)
   -  node list : composed of node-id
   -  link list : composed of link-id
   -  node : composed of roadm, xponder
      link : composed of links of different types (roadm-to-roadm, express, add-drop ...)

OLM Service
^^^^^^^^^^^

-  RPC call

   -  get-pm (given node-id)

   -  service-power-setup

   -  service-power-turndown

   -  service-power-reset

   -  calculate-spanloss-base

   -  calculate-spanloss-current

odl-transportpce-stubmodels
^^^^^^^^^^^^^^^^^^^^^^^^^^^

   -  This feature provides function to be able to stub some of TransportPCE modules, pce and
      renderer (Stubpce and Stubrenderer).
      Stubs are used for development purposes and can be used for some of the functionnal tests.

Interfaces to external software
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It defines the interfaces implemented to interconnect TransportPCE modules with other software in
order to perform specific tasks

GNPy interface
^^^^^^^^^^^^^^

-  Request structure

   -  topology : composed of list of elements and connections
   -  service : source, destination, explicit-route-objects, path-constraints

-  Response structure

   -  path-properties/path-metric : OSNR-0.1nm, OSNR-bandwidth, SNR-0.1nm, SNR-bandwidth,
   -  path-properties/path-route-objects : composed of path elements


Running transportPCE project
----------------------------

To use transportPCE controller, the first step is to connect the controller to optical nodes
through the NETCONF connector.

.. note::

    In the current version, only optical equipment compliant with open ROADM datamodels are managed
    by transportPCE.


Connecting nodes
~~~~~~~~~~~~~~~~

To connect a node, use the following JSON RPC

**REST API** : *POST /restconf/config/network-topology:network-topology/topology/topology-netconf/node/<node-id>*

**Sample JSON Data**

.. code:: json

    {
        "node": [
            {
                "node-id": "<node-id>",
                "netconf-node-topology:tcp-only": "false",
                "netconf-node-topology:reconnect-on-changed-schema": "false",
                "netconf-node-topology:host": "<node-ip-address>",
                "netconf-node-topology:default-request-timeout-millis": "120000",
                "netconf-node-topology:max-connection-attempts": "0",
                "netconf-node-topology:sleep-factor": "1.5",
                "netconf-node-topology:actor-response-wait-time": "5",
                "netconf-node-topology:concurrent-rpc-limit": "0",
                "netconf-node-topology:between-attempts-timeout-millis": "2000",
                "netconf-node-topology:port": "<netconf-port>",
                "netconf-node-topology:connection-timeout-millis": "20000",
                "netconf-node-topology:username": "<node-username>",
                "netconf-node-topology:password": "<node-password>",
                "netconf-node-topology:keepalive-delay": "300"
            }
        ]
    }


Then check that the netconf session has been correctly established between the controller and the
node. the status of **netconf-node-topology:connection-status** must be **connected**

**REST API** : *GET /restconf/operational/network-topology:network-topology/topology/topology-netconf/node/<node-id>*


Node configuration discovery
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once the controller is connected to the node, transportPCE application automatically launchs a
discovery of the node configuration datastore and creates **Logical Connection Points** to any
physical ports related to transmission. All *circuit-packs* inside the node configuration are
analyzed.

Use the following JSON RPC to check that function internally named *portMapping*.

**REST API** : *GET /restconf/config/portmapping:network*

.. note::

    In ``org-openroadm-device.yang``, four types of optical nodes can be managed:
        * rdm: ROADM device (optical switch)
        * xpdr: Xponder device (device that converts client to optical channel interface)
        * ila: in line amplifier (optical amplifier)
        * extplug: external pluggable (an optical pluggable that can be inserted in an external unit such as a router)

    TransportPCE currently supports rdm and xpdr

Depending on the kind of open ROADM device connected, different kind of *Logical Connection Points*
should appear, if the node configuration is not empty:

-  DEG<degree-number>-TTP-<port-direction>: created on the line port of a degree on a rdm equipment
-  SRG<srg-number>-PP<port-number>: created on the client port of a srg on a rdm equipment
-  XPDR<number>-CLIENT<port-number>: created on the client port of a xpdr equipment
-  XPDR<number>-NETWORK<port-number>: created on the line port of a xpdr equipment

    For further details on openROADM device models, see `openROADM MSA white paper <https://0201.nccdn.net/1_2/000/000/134/c50/Open-ROADM-MSA-release-2-Device-White-paper-v1-1.pdf>`__.

Optical Network topology
~~~~~~~~~~~~~~~~~~~~~~~~

Before creating an optical connectivity service, your topology must contain at least two xpdr
devices connected to two different rdm devices. Normally, the *openroadm-topology* is automatically
created by transportPCE. Nevertheless, depending on the configuration inside optical nodes, this
topology can be partial. Check that link of type *ROADMtoROADM* exists between two adjacent rdm
nodes.

**REST API** : *GET /restconf/config/ietf-network:network/openroadm-topology*

If it is not the case, you need to manually complement the topology with *ROADMtoROADM* link using
the following REST RPC:


**REST API** : *POST /restconf/operations/networkutils:init-roadm-nodes*

**Sample JSON Data**

.. code:: json

    {
      "networkutils:input": {
        "networkutils:rdm-a-node": "<node-id-A>",
        "networkutils:deg-a-num": "<degree-A-number>",
        "networkutils:termination-point-a": "<Logical-Connection-Point>",
        "networkutils:rdm-z-node": "<node-id-Z>",
        "networkutils:deg-z-num": "<degree-Z-number>",
        "networkutils:termination-point-z": "<Logical-Connection-Point>"
      }
    }

*<Logical-Connection-Point> comes from the portMapping function*.

Unidirectional links between xpdr and rdm nodes must be created manually. To that end use the two
following REST RPCs:

From xpdr to rdm:
^^^^^^^^^^^^^^^^^

**REST API** : *POST /restconf/operations/networkutils:init-xpdr-rdm-links*

**Sample JSON Data**

.. code:: json

    {
      "networkutils:input": {
        "networkutils:links-input": {
          "networkutils:xpdr-node": "<xpdr-node-id>",
          "networkutils:xpdr-num": "1",
          "networkutils:network-num": "<xpdr-network-port-number>",
          "networkutils:rdm-node": "<rdm-node-id>",
          "networkutils:srg-num": "<srg-number>",
          "networkutils:termination-point-num": "<Logical-Connection-Point>"
        }
      }
    }

From rdm to xpdr:
^^^^^^^^^^^^^^^^^

**REST API** : *POST /restconf/operations/networkutils:init-rdm-xpdr-links*

**Sample JSON Data**

.. code:: json

    {
      "networkutils:input": {
        "networkutils:links-input": {
          "networkutils:xpdr-node": "<xpdr-node-id>",
          "networkutils:xpdr-num": "1",
          "networkutils:network-num": "<xpdr-network-port-number>",
          "networkutils:rdm-node": "<rdm-node-id>",
          "networkutils:srg-num": "<srg-number>",
          "networkutils:termination-point-num": "<Logical-Connection-Point>"
        }
      }
    }

OTN topology
~~~~~~~~~~~~

Before creating an OTN service, your topology must contain at least two xpdr devices of MUXPDR or
SWITCH type connected to two different rdm devices. To check that these xpdr are present in the
OTN topology, use the following command on the REST API :

**REST API** : *GET /restconf/config/ietf-network:network/otn-topology*

An optical connectivity service shall have been created in a first setp. In Magnesium SR0, the OTN
links are not automatically populated in the topology after the Och, OTU4 and ODU4 interfaces have
been created on the two network ports of the xpdr. Thus the otn link must be posted manually through
the REST API (APIDoc).

**REST API** : *POST /restconf/config/ietf-network:network/otn-topology/link*
**REST API** : to complete


Creating a service
~~~~~~~~~~~~~~~~~~

In Magnesium SR0, two different kind of services can be created with transportPCE using the Northbound
REST API:

- 100GE services from client port to client port of two transponders (TPDR)
- an OTU-4/ODU-4 service from network port to network port of two Xponders (MUXPDR/SWITCH)

For the creation of 1GE and 10GE services the user has to check using internal API the connectivity
between nodes and the possibility to create a service through the path-computation-request rpc. The
service creation is performed using internal otn-service-path rpc.

100GE service creation:
^^^^^^^^^^^^^^^^^^^^^^^

Use the following REST RPC to invoke *service handler* module in order to create a bidirectional
end-to-end optical connectivity service between two xpdr over an optical network composed of rdm
nodes.

**REST API** : *POST /restconf/operations/org-openroadm-service:service-create*

**Sample JSON Data**

.. code:: json

    {
        "input": {
            "sdnc-request-header": {
                "request-id": "request-1",
                "rpc-action": "service-create",
                "request-system-id": "appname"
            },
            "service-name": "test1",
            "common-id": "commonId",
            "connection-type": "service",
            "service-a-end": {
                "service-rate": "100",
                "node-id": "<xpdr-node-id>",
                "service-format": "Ethernet",
                "clli": "<ccli-name>",
                "tx-direction": {
                    "port": {
                        "port-device-name": "<xpdr-client-port>",
                        "port-type": "fixed",
                        "port-name": "<xpdr-client-port-number>",
                        "port-rack": "000000.00",
                        "port-shelf": "Chassis#1"
                    },
                    "lgx": {
                        "lgx-device-name": "Some lgx-device-name",
                        "lgx-port-name": "Some lgx-port-name",
                        "lgx-port-rack": "000000.00",
                        "lgx-port-shelf": "00"
                    }
                },
                "rx-direction": {
                    "port": {
                        "port-device-name": "<xpdr-client-port>",
                        "port-type": "fixed",
                        "port-name": "<xpdr-client-port-number>",
                        "port-rack": "000000.00",
                        "port-shelf": "Chassis#1"
                    },
                    "lgx": {
                        "lgx-device-name": "Some lgx-device-name",
                        "lgx-port-name": "Some lgx-port-name",
                        "lgx-port-rack": "000000.00",
                        "lgx-port-shelf": "00"
                    }
                },
                "optic-type": "gray"
            },
            "service-z-end": {
                "service-rate": "100",
                "node-id": "<xpdr-node-id>",
                "service-format": "Ethernet",
                "clli": "<ccli-name>",
                "tx-direction": {
                    "port": {
                        "port-device-name": "<xpdr-client-port>",
                        "port-type": "fixed",
                        "port-name": "<xpdr-client-port-number>",
                        "port-rack": "000000.00",
                        "port-shelf": "Chassis#1"
                    },
                    "lgx": {
                        "lgx-device-name": "Some lgx-device-name",
                        "lgx-port-name": "Some lgx-port-name",
                        "lgx-port-rack": "000000.00",
                        "lgx-port-shelf": "00"
                    }
                },
                "rx-direction": {
                    "port": {
                        "port-device-name": "<xpdr-client-port>",
                        "port-type": "fixed",
                        "port-name": "<xpdr-client-port-number>",
                        "port-rack": "000000.00",
                        "port-shelf": "Chassis#1"
                    },
                    "lgx": {
                        "lgx-device-name": "Some lgx-device-name",
                        "lgx-port-name": "Some lgx-port-name",
                        "lgx-port-rack": "000000.00",
                        "lgx-port-shelf": "00"
                    }
                },
                "optic-type": "gray"
            },
            "due-date": "yyyy-mm-ddT00:00:01Z",
            "operator-contact": "some-contact-info"
        }
    }

Most important parameters for this REST RPC are the identification of the two physical client ports
on xpdr nodes.This RPC invokes the *PCE* module to compute a path over the *openroadm-topology* and
then invokes *renderer* and *OLM* to implement the end-to-end path into the devices.

OTU4/ODU4 service creation :
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Use the following REST RPC to invoke *service handler* module in order to create a bidirectional
end-to-end optical connectivity service between two xpdr over an optical network composed of rdm
nodes.

XXXXXXXXXXXXXXX (TO BE COMPLETED )XXXXXXXXXXXXXXXXX


1GE/ODU0 and 10GE/ODU2e service creation :
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Use the following REST RPC to invoke *PCE* module in order to check connectivity between xponder
nodes and the availability of a supporting optical connectivity between the network-ports of the
nodes.

**REST API** : to complete

After end-to-end optical connectivity between the two xpdr has been checked, use the otn-service-path
rpc to invoke the *Renderer* and create the corresponding interfaces :

- 1GE and ODU0 interfaces for 1GE services
- 10GE and ODU2e interfaces for 10GE services

The following example corresponds to the creation of a 10GE service

**REST API** : to complete

.. note::
    OTN links are not automatically populated in the topology after the ODU2e interfaces have
    been created on the two client ports of the xpdr. The otn link can be posted manually through
    the REST API (APIDoc).

.. note::
    With Magnesium SR0, the service-list corresponding to 1GE/10GE and OTU4/ODU4 services is not
    updated in the datastore after the interfaces have been created in the device.


Deleting a service
~~~~~~~~~~~~~~~~~~

**REST API** : to complete according to what can be done with 100GE/OTU4/1GE/10GE services

Deleting a 100GE service
^^^^^^^^^^^^^^^^^^^^^^^^

Use the following REST RPC to invoke *service handler* module in order to delete a given optical
connectivity service.

**REST API** : *POST /restconf/operations/org-openroadm-service:service-delete*

**Sample JSON Data**

.. code:: json

    {
        "input": {
            "sdnc-request-header": {
                "request-id": "request-1",
                "rpc-action": "service-delete",
                "request-system-id": "appname",
                "notification-url": "http://localhost:8585/NotificationServer/notify"
            },
            "service-delete-req-info": {
                "service-name": "test1",
                "tail-retention": "no"
            }
        }
    }

Most important parameters for this REST RPC is the *service-name*.


Help
----

-  `TransportPCE Wiki archive <https://wiki-archive.opendaylight.org/view/TransportPCE:Main>`__

-  TransportPCE Mailing List
   (`developer <https://lists.opendaylight.org/mailman/listinfo/transportpce-dev>`__)
