#!/usr/bin/env python
##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import json
import os
import psutil
import requests
import signal
import shutil
import subprocess
import time
import unittest


class TransportPCEtesting(unittest.TestCase):

    odl_process = None
    honeynode_topology = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def _get_file(cls):
        honeynode_topology_file = "sample_configs/honeynode-topo.xml"
        if os.path.isfile(honeynode_topology_file):
            with open(honeynode_topology_file, 'r') as honeynode_file:
                cls.honeynode_topo = honeynode_file.read();

    @classmethod
    def __start_odl(cls):
        executable = "../karaf/target/assembly/bin/karaf"
        with open('odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable, "server"], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):  # a class method called before tests in an individual class run.
        cls._get_file()
        cls.__start_odl()
        time.sleep(90)

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

    def setUp(self):  # instruction executed before each test method
        print ("execution of {}".format(self.id().split(".")[-1]))

#    def test_01_restconfAPI(self):
#        url = ("{}/operational/network-topology:network-topology/topology/"
#        "topology-netconf/node/controller-config".format(self.restconf_baseurl))
#        headers = {'content-type': 'application/json'}
#        response = requests.request("GET", url, headers=headers, auth=('admin', 'admin'))
#        self.assertEqual(response.status_code, requests.codes.ok)
#        res = response.json()
#        self.assertEqual(res['node'] [0] ['netconf-node-topology:connection-status'],
#                         'connected')
#        time.sleep(1)

    # Load honeynode topology
    def test_02_load_honeynode_topology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        body = self.honeynode_topo
        headers = {'content-type': 'application/xml',
        "Accept": "application/json"}
        response = requests.request(
            "PUT", url, data=body, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_03_check_link_xpdra_roadma(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/link/"
               "XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request("GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        time.sleep(1)

    def test_04_check_link_xpdrc_roadmc(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/link/"
               "XPDRC-XPDR1-XPDR1-NETWORK1toROADMC-SRG1-SRG1-PP1-TXRX".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request("GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        time.sleep(1)

    def test_05_check_link_roadma_xpdra(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/link/"
               "ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request("GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        time.sleep(1)

    def test_06_check_link_roadmc_xpdrc(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/link/"
               "ROADMC-SRG1-SRG1-PP1-TXRXtoXPDRC-XPDR1-XPDR1-NETWORK1".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request("GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        time.sleep(1)

    # Create Service 'test' with correct parameters
    def test_07_create_service_success(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-name": "test",
                "common-id": "ASATT1234567",
                "connection-type": "infrastructure",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "XPDRA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "XPDRC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    # Create Service 'test' with not compliant parameter : no 'sdnc-request-header' parameter
    def test_08_create_service_failed(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "service-name": "test",
                "common-id": "ASATT1234567",
                "connection-type": "infrastructure",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "XPDRA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "XPDRC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service sndc-request-header is not set',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Create Service 'test' with not compliant parameter : no 'tx-direction' for serviceAEnd
    def test_09_create_service_failed(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-name": "test",
                "common-id": "ASATT1234567",
                "connection-type": "infrastructure",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "XPDRA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "XPDRC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service TxDirection is not correctly set',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Get 'test' service created
    def test_10_verify_service_created(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/test"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        time.sleep(1)

    # get non existing service
    def test_11_get_non_existing_service(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/test1"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)

    #reconfigure 'test' to be 'test-new'
    def test_12_reconfigure_service_test(self):
        url = ("{}/operations/org-openroadm-service:service-reconfigure"
              .format(self.restconf_baseurl))
        data = {"input": {
                "service-name": "test",
                "new-service-name": "test-new",
                "common-id": "ASATT1234567",
                "connection-type": "infrastructure",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "XPDRA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "XPDRC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                }
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
            res['output']['status-message'])
        time.sleep(30)

    # get new service 'test-new'
    def test_13_verify_service_reconfigure(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/test-new"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        time.sleep(1)

    # Delete  service
    def test_14_delete_service_reconfigure(self):
        url = ("{}/operations/org-openroadm-service:service-delete"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "test-new",
                    "due-date": "2016-11-28T00:00:01Z",
                    "tail-retention": "no"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    # Verify 'test' service deleted
    def test_15_verify_service_reconfigure_deleted(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/test-new"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)

    # Create Temp Service 'ASATT1234567' with correct parameters
    def test_16_create_temp_service_success(self):
        url = ("{}/operations/org-openroadm-service:temp-service-create"
              .format(self.restconf_baseurl))
        data = {
            "input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "temp-service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "common-id": "ASATT1234567",
                "connection-type": "infrastructure",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "ROADMA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "ROADMC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(20)


    # Create Temp Service not compliant with no common-id
    def test_17_create_temp_service_failed(self):
        url = ("{}/operations/org-openroadm-service:temp-service-create"
              .format(self.restconf_baseurl))
        data = {
            "input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "temp-service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "connection-type": "infrastructure",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "ROADMA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "ROADMC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service Name (common-id for Temp service) is not set',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Get 'ASATT1234567' temp service created
    def test_18_verify_temp_service_created(self):
        url = ("{}/operational/org-openroadm-service:temp-service-list/services/ASATT1234567"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        time.sleep(1)

    # get non existing service
    def test_19_get_temp_service_non_existed(self):
        url = ("{}/operational/org-openroadm-service:temp-service-list/services/test1"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)

    # Delete temp service
    def test_20_delete_temp_service_created(self):
        url = ("{}/operations/org-openroadm-service:temp-service-delete"
              .format(self.restconf_baseurl))
        data = {"input": {
                "common-id": "ASATT1234567"
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

# Verify 'test' service deleted
    def test_21_verify_temp_service_deleted(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/ASATT1234567"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
