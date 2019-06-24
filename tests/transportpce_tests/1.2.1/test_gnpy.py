#!/usr/bin/env python

##############################################################################
#Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
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
import logging

class TransportGNPYtesting(unittest.TestCase):

    gnpy_process = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def __init_logfile(cls):
        if os.path.isfile("./transportpce_tests/gnpy.log"):
            os.remove("transportpce_tests/gnpy.log")

    @classmethod
    def __start_odl(cls):
        executable = "../karaf/target/assembly/bin/karaf"
        with open('transportpce_tests/odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable, "server"], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):
        cls.__start_odl()
        time.sleep(30)

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

    def setUp(self):
        time.sleep(2)

    #Connect the different topology
    def test_01_connect_clliNetwork(self):
        #Config ROADMA
        url = ("{}/config/ietf-network:networks/network/clli-network"
               .format(self.restconf_baseurl))
        data = {"network": [{
            "network-id": "clli-network",
            "network-types": {
                "org-openroadm-clli-network:clli-network": {}
            },
            "node": [
                {
                    "node-id": "NodeA",
                    "org-openroadm-clli-network:clli": "NodeA"
                },
                {
                    "node-id": "NodeB",
                    "org-openroadm-clli-network:clli": "NodeB"
                },
                {
                    "node-id": "NodeC",
                    "org-openroadm-clli-network:clli": "NodeC"
                }
            ]
            }]
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_02_connect_openroadmNetwork(self):
        #Config ROADMA
        url = ("{}/config/ietf-network:networks/network/openroadm-network"
               .format(self.restconf_baseurl))
        data = {"network": [{
            "network-id": "openroadm-network",
            "network-types": {
                "org-openroadm-common-network:openroadm-common-network": {}
            },
            "node": [
                {
                    "node-id": "ROADMB",
                    "supporting-node": [
                        {
                            "network-ref": "clli-network",
                            "node-ref": "NodeB"
                        }
                    ],
                    "org-openroadm-network:node-type": "ROADM",
                    "org-openroadm-network:vendor": "vendorA",
                    "org-openroadm-network:ip": "127.0.0.12",
                    "org-openroadm-network:model": "2"
                },
                {
                    "node-id": "ROADMC",
                    "supporting-node": [
                        {
                            "network-ref": "clli-network",
                            "node-ref": "NodeC"
                        }
                    ],
                    "org-openroadm-network:node-type": "ROADM",
                    "org-openroadm-network:vendor": "vendorA",
                    "org-openroadm-network:ip": "127.0.0.13",
                    "org-openroadm-network:model": "2"
                },
                {
                    "node-id": "XPDRB",
                    "supporting-node": [
                        {
                            "network-ref": "clli-network",
                            "node-ref": "NodeB"
                        }
                    ],
                    "org-openroadm-network:node-type": "XPONDER",
                    "org-openroadm-network:vendor": "vendorA",
                    "org-openroadm-network:ip": "127.0.0.14",
                    "org-openroadm-network:model": "1"
                },
                {
                    "node-id": "ROADMA",
                    "supporting-node": [
                        {
                            "network-ref": "clli-network",
                            "node-ref": "NodeA"
                        }
                    ],
                    "org-openroadm-network:node-type": "ROADM",
                    "org-openroadm-network:vendor": "vendorA",
                    "org-openroadm-network:ip": "127.0.0.11",
                    "org-openroadm-network:model": "2"
                },
                {
                    "node-id": "XPDRA",
                    "supporting-node": [
                        {
                            "network-ref": "clli-network",
                            "node-ref": "NodeA"
                        }
                    ],
                    "org-openroadm-network:node-type": "XPONDER",
                    "org-openroadm-network:vendor": "vendorA",
                    "org-openroadm-network:ip": "127.0.0.10",
                    "org-openroadm-network:model": "1"
                }
            ]
            }]
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_03_connect_openroadmTopology(self):
        #Config ROADMA
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        data = {"network": [{
            "network-id": "openroadm-topology",
            "node": [
                {
                    "node-id": "XPDRA-XPDR1",
                    "org-openroadm-network-topology:node-type": "XPONDER",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "XPDR1-CLIENT2",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK2"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-CLIENT"
                        },
                        {
                            "tp-id": "XPDR1-CLIENT1",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK1"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-CLIENT"
                        },
                        {
                            "tp-id": "XPDR1-NETWORK2",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK1"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-NETWORK",
                            "org-openroadm-network-topology:xpdr-network-attributes": {
                                "tail-equipment-id": "XPDR1-CLIENT2"
                            }
                        },
                        {
                            "tp-id": "XPDR1-NETWORK1",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK1"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-NETWORK",
                            "org-openroadm-network-topology:xpdr-network-attributes": {
                                "tail-equipment-id": "XPDR1-CLIENT1"
                            }
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "XPDRA"
                        }
                    ]
                },
                {
                    "node-id": "ROADMC-DEG2",
                    "org-openroadm-network-topology:degree-attributes": {
                        "degree-number": 2,
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "DEGREE",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "DEG2-CTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-CTP"
                        },
                        {
                            "tp-id": "DEG2-TTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-TTP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMC"
                        }
                    ]
                },
                {
                    "node-id": "ROADMC-SRG1",
                    "org-openroadm-network-topology:srg-attributes": {
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "SRG",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "SRG1-PP16-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP10-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP3-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP6-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP9-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP1-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP13-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP4-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP7-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP11-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP15-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP8-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-CP-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-CP"
                        },
                        {
                            "tp-id": "SRG1-PP12-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP14-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP2-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP5-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMC"
                        }
                    ]
                },
                {
                    "node-id": "ROADMA-DEG1",
                    "org-openroadm-network-topology:degree-attributes": {
                        "degree-number": 1,
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "DEGREE",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "DEG1-TTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-TTP"
                        },
                        {
                            "tp-id": "DEG1-CTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-CTP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMA"
                        }
                    ]
                },
                {
                    "node-id": "ROADMA-DEG2",
                    "org-openroadm-network-topology:degree-attributes": {
                        "degree-number": 2,
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "DEGREE",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "DEG2-CTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-CTP"
                        },
                        {
                            "tp-id": "DEG2-TTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-TTP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMA"
                        }
                    ]
                },
                {
                    "node-id": "ROADMB-SRG1",
                    "org-openroadm-network-topology:srg-attributes": {
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "SRG",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "SRG1-PP16-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP10-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP3-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP6-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP9-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP1-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP13-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP4-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP7-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP11-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP15-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP8-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-CP-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-CP"
                        },
                        {
                            "tp-id": "SRG1-PP12-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP14-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP2-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP5-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMB"
                        }
                    ]
                },
                {
                    "node-id": "ROADMC-DEG1",
                    "org-openroadm-network-topology:degree-attributes": {
                        "degree-number": 1,
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "DEGREE",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "DEG1-TTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-TTP"
                        },
                        {
                            "tp-id": "DEG1-CTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-CTP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMC"
                        }
                    ]
                },
                {
                    "node-id": "XPDRB-XPDR1",
                    "org-openroadm-network-topology:node-type": "XPONDER",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "XPDR1-CLIENT2",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK2"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-CLIENT"
                        },
                        {
                            "tp-id": "XPDR1-CLIENT1",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK1"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-CLIENT"
                        },
                        {
                            "tp-id": "XPDR1-NETWORK2",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK1"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-NETWORK",
                            "org-openroadm-network-topology:xpdr-network-attributes": {
                                "tail-equipment-id": "XPDR1-CLIENT2"
                            }
                        },
                        {
                            "tp-id": "XPDR1-NETWORK1",
                            "org-openroadm-network-topology:xpdr-client-attributes": {
                                "tail-equipment-id": "XPDR1-NETWORK1"
                            },
                            "org-openroadm-network-topology:tp-type": "XPONDER-NETWORK",
                            "org-openroadm-network-topology:xpdr-network-attributes": {
                                "tail-equipment-id": "XPDR1-CLIENT1"
                            }
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "XPDRB"
                        }
                    ]
                },
                {
                    "node-id": "ROADMB-DEG1",
                    "org-openroadm-network-topology:degree-attributes": {
                        "degree-number": 1,
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "DEGREE",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "DEG1-TTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-TTP"
                        },
                        {
                            "tp-id": "DEG1-CTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-CTP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMB"
                        }
                    ]
                },
                {
                    "node-id": "ROADMA-SRG1",
                    "org-openroadm-network-topology:srg-attributes": {
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "SRG",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "SRG1-PP16-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP10-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP3-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP6-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP9-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP1-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP13-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP4-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP7-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP11-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP15-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP8-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-CP-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-CP"
                        },
                        {
                            "tp-id": "SRG1-PP12-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP14-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP2-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        },
                        {
                            "tp-id": "SRG1-PP5-TXRX",
                            "org-openroadm-network-topology:tp-type": "SRG-TXRX-PP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMA"
                        }
                    ]
                },
                {
                    "node-id": "ROADMB-DEG2",
                    "org-openroadm-network-topology:degree-attributes": {
                        "degree-number": 2,
                        "available-wavelengths": [
                            {
                                "index": 94
                            },
                            {
                                "index": 93
                            },
                            {
                                "index": 96
                            },
                            {
                                "index": 95
                            },
                            {
                                "index": 42
                            },
                            {
                                "index": 41
                            },
                            {
                                "index": 44
                            },
                            {
                                "index": 43
                            },
                            {
                                "index": 38
                            },
                            {
                                "index": 37
                            },
                            {
                                "index": 40
                            },
                            {
                                "index": 39
                            },
                            {
                                "index": 34
                            },
                            {
                                "index": 33
                            },
                            {
                                "index": 36
                            },
                            {
                                "index": 35
                            },
                            {
                                "index": 30
                            },
                            {
                                "index": 29
                            },
                            {
                                "index": 32
                            },
                            {
                                "index": 31
                            },
                            {
                                "index": 58
                            },
                            {
                                "index": 57
                            },
                            {
                                "index": 60
                            },
                            {
                                "index": 59
                            },
                            {
                                "index": 54
                            },
                            {
                                "index": 53
                            },
                            {
                                "index": 56
                            },
                            {
                                "index": 55
                            },
                            {
                                "index": 50
                            },
                            {
                                "index": 49
                            },
                            {
                                "index": 52
                            },
                            {
                                "index": 51
                            },
                            {
                                "index": 46
                            },
                            {
                                "index": 45
                            },
                            {
                                "index": 48
                            },
                            {
                                "index": 47
                            },
                            {
                                "index": 74
                            },
                            {
                                "index": 73
                            },
                            {
                                "index": 76
                            },
                            {
                                "index": 75
                            },
                            {
                                "index": 70
                            },
                            {
                                "index": 69
                            },
                            {
                                "index": 72
                            },
                            {
                                "index": 71
                            },
                            {
                                "index": 66
                            },
                            {
                                "index": 65
                            },
                            {
                                "index": 68
                            },
                            {
                                "index": 67
                            },
                            {
                                "index": 62
                            },
                            {
                                "index": 61
                            },
                            {
                                "index": 64
                            },
                            {
                                "index": 63
                            },
                            {
                                "index": 90
                            },
                            {
                                "index": 89
                            },
                            {
                                "index": 92
                            },
                            {
                                "index": 91
                            },
                            {
                                "index": 86
                            },
                            {
                                "index": 85
                            },
                            {
                                "index": 88
                            },
                            {
                                "index": 87
                            },
                            {
                                "index": 82
                            },
                            {
                                "index": 81
                            },
                            {
                                "index": 84
                            },
                            {
                                "index": 83
                            },
                            {
                                "index": 78
                            },
                            {
                                "index": 77
                            },
                            {
                                "index": 80
                            },
                            {
                                "index": 79
                            },
                            {
                                "index": 10
                            },
                            {
                                "index": 9
                            },
                            {
                                "index": 12
                            },
                            {
                                "index": 11
                            },
                            {
                                "index": 6
                            },
                            {
                                "index": 5
                            },
                            {
                                "index": 8
                            },
                            {
                                "index": 7
                            },
                            {
                                "index": 2
                            },
                            {
                                "index": 1
                            },
                            {
                                "index": 4
                            },
                            {
                                "index": 3
                            },
                            {
                                "index": 26
                            },
                            {
                                "index": 25
                            },
                            {
                                "index": 28
                            },
                            {
                                "index": 27
                            },
                            {
                                "index": 22
                            },
                            {
                                "index": 21
                            },
                            {
                                "index": 24
                            },
                            {
                                "index": 23
                            },
                            {
                                "index": 18
                            },
                            {
                                "index": 17
                            },
                            {
                                "index": 20
                            },
                            {
                                "index": 19
                            },
                            {
                                "index": 14
                            },
                            {
                                "index": 13
                            },
                            {
                                "index": 16
                            },
                            {
                                "index": 15
                            }
                        ]
                    },
                    "org-openroadm-network-topology:node-type": "DEGREE",
                    "ietf-network-topology:termination-point": [
                        {
                            "tp-id": "DEG2-CTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-CTP"
                        },
                        {
                            "tp-id": "DEG2-TTP-TXRX",
                            "org-openroadm-network-topology:tp-type": "DEGREE-TXRX-TTP"
                        }
                    ],
                    "supporting-node": [
                        {
                            "network-ref": "openroadm-network",
                            "node-ref": "ROADMB"
                        }
                    ]
                }
            ],
            "network-types": {
                "org-openroadm-common-network:openroadm-common-network": {}
            },
            "ietf-network-topology:link": [
                {
                    "link-id": "ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX",
                    "source": {
                        "source-node": "ROADMA-DEG1",
                        "source-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "DROP-LINK",
                    "destination": {
                        "dest-node": "ROADMA-SRG1",
                        "dest-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX"
                },
                {
                    "link-id": "ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX",
                    "source": {
                        "source-node": "ROADMA-DEG2",
                        "source-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "DROP-LINK",
                    "destination": {
                        "dest-node": "ROADMA-SRG1",
                        "dest-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX"
                },
                {
                    "link-id": "ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMA-DEG1",
                        "source-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "EXPRESS-LINK",
                    "destination": {
                        "dest-node": "ROADMA-DEG2",
                        "dest-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX"
                },
                {
                    "link-id": "ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX",
                    "source": {
                        "source-node": "ROADMC-DEG1",
                        "source-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "DROP-LINK",
                    "destination": {
                        "dest-node": "ROADMC-SRG1",
                        "dest-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX"
                },
                {
                    "link-id": "ROADMB-DEG1-DEG1-CTP-TXRXtoROADMB-SRG1-SRG1-CP-TXRX",
                    "source": {
                        "source-node": "ROADMB-DEG1",
                        "source-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "DROP-LINK",
                    "destination": {
                        "dest-node": "ROADMB-SRG1",
                        "dest-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-SRG1-SRG1-CP-TXRXtoROADMB-DEG1-DEG1-CTP-TXRX"
                },
                {
                    "link-id": "ROADMB-DEG2-DEG2-CTP-TXRXtoROADMB-SRG1-SRG1-CP-TXRX",
                    "source": {
                        "source-node": "ROADMB-DEG2",
                        "source-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "DROP-LINK",
                    "destination": {
                        "dest-node": "ROADMB-SRG1",
                        "dest-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-SRG1-SRG1-CP-TXRXtoROADMB-DEG2-DEG2-CTP-TXRX"
                },
                {
                    "link-id": "ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX",
                    "source": {
                        "source-node": "ROADMC-DEG2",
                        "source-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "DROP-LINK",
                    "destination": {
                        "dest-node": "ROADMC-SRG1",
                        "dest-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX"
                },
                {
                    "link-id": "ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMC-DEG1",
                        "source-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "EXPRESS-LINK",
                    "destination": {
                        "dest-node": "ROADMC-DEG2",
                        "dest-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX"
                },
                {
                    "link-id": "ROADMB-DEG1-DEG1-CTP-TXRXtoROADMB-DEG2-DEG2-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMB-DEG1",
                        "source-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "EXPRESS-LINK",
                    "destination": {
                        "dest-node": "ROADMB-DEG2",
                        "dest-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-DEG2-DEG2-CTP-TXRXtoROADMB-DEG1-DEG1-CTP-TXRX"
                },
                {
                    "link-id": "ROADMA-DEG1-DEG1-TTP-TXRXtoROADMC-DEG2-DEG2-TTP-TXRX",
                    "source": {
                        "source-node": "ROADMA-DEG1",
                        "source-tp": "DEG1-TTP-TXRX"
                    },
                    "org-openroadm-network-topology:OMS-attributes": {
                        "opposite-link": "ROADMC-DEG2-DEG2-TTP-TXRXtoROADMA-DEG1-DEG1-TTP-TXRX",
                        "span": {
                            "spanloss-base": 11.4,
                            "link-concatenation": [
                                {
                                    "SRLG-Id": 0,
                                    "SRLG-length": 100000,
                                    "pmd": 0.5,
                                    "fiber-type": "smf"
                                }
                            ],
                            "spanloss-current": 12,
                            "engineered-spanloss": 12.2,
                            "clfi": "fiber1",
                            "auto-spanloss": "true"
                        }
                    },
                    "org-openroadm-network-topology:link-type": "ROADM-TO-ROADM",
                    "destination": {
                        "dest-node": "ROADMC-DEG2",
                        "dest-tp": "DEG2-TTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-DEG2-DEG2-TTP-TXRXtoROADMA-DEG1-DEG1-TTP-TXRX"
                },
                {
                    "link-id": "XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX",
                    "source": {
                        "source-node": "XPDRA-XPDR1",
                        "source-tp": "XPDR1-NETWORK1"
                    },
                    "org-openroadm-network-topology:opposite-link": "ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1",
                    "org-openroadm-network-topology:link-type": "XPONDER-OUTPUT",
                    "destination": {
                        "dest-node": "ROADMA-SRG1",
                        "dest-tp": "SRG1-PP1-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1"
                },
                {
                    "link-id": "ROADMC-DEG1-DEG1-TTP-TXRXtoROADMB-DEG2-DEG2-TTP-TXRX",
                    "source": {
                        "source-node": "ROADMC-DEG1",
                        "source-tp": "DEG1-TTP-TXRX"
                    },
                    "org-openroadm-network-topology:OMS-attributes": {
                        "opposite-link": "ROADMB-DEG2-DEG2-TTP-TXRXtoROADMC-DEG1-DEG1-TTP-TXRX",
                        "span": {
                            "spanloss-base": 11.4,
                            "link-concatenation": [
                                {
                                    "SRLG-Id": 0,
                                    "SRLG-length": 100000,
                                    "pmd": 0.5,
                                    "fiber-type": "smf"
                                }
                            ],
                            "spanloss-current": 12,
                            "engineered-spanloss": 12.2,
                            "clfi": "fiber2",
                            "auto-spanloss": "true"
                        }
                    },
                    "org-openroadm-network-topology:link-type": "ROADM-TO-ROADM",
                    "destination": {
                        "dest-node": "ROADMB-DEG2",
                        "dest-tp": "DEG2-TTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-DEG2-DEG2-TTP-TXRXtoROADMC-DEG1-DEG1-TTP-TXRX"
                },
                {
                    "link-id": "ROADMB-DEG1-DEG1-TTP-TXRXtoROADMA-DEG2-DEG2-TTP-TXRX",
                    "source": {
                        "source-node": "ROADMB-DEG1",
                        "source-tp": "DEG1-TTP-TXRX"
                    },
                    "org-openroadm-network-topology:OMS-attributes": {
                        "opposite-link": "ROADMA-DEG2-DEG2-TTP-TXRXtoROADMB-DEG1-DEG1-TTP-TXRX",
                        "span": {
                            "spanloss-base": 11.4,
                            "link-concatenation": [
                                {
                                    "SRLG-Id": 0,
                                    "SRLG-length": 100000,
                                    "pmd": 0.5,
                                    "fiber-type": "smf"
                                }
                            ],
                            "spanloss-current": 12,
                            "engineered-spanloss": 12.2,
                            "clfi": "fiber3",
                            "auto-spanloss": "true"
                        }
                    },
                    "org-openroadm-network-topology:link-type": "ROADM-TO-ROADM",
                    "destination": {
                        "dest-node": "ROADMA-DEG2",
                        "dest-tp": "DEG2-TTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-DEG2-DEG2-TTP-TXRXtoROADMB-DEG1-DEG1-TTP-TXRX"
                },
                {
                    "link-id": "ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMA-SRG1",
                        "source-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "ADD-LINK",
                    "destination": {
                        "dest-node": "ROADMA-DEG2",
                        "dest-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX"
                },
                {
                    "link-id": "ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMA-DEG2",
                        "source-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "EXPRESS-LINK",
                    "destination": {
                        "dest-node": "ROADMA-DEG1",
                        "dest-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX"
                },
                {
                    "link-id": "XPDRB-XPDR1-XPDR1-NETWORK1toROADMB-SRG1-SRG1-PP1-TXRX",
                    "source": {
                        "source-node": "XPDRB-XPDR1",
                        "source-tp": "XPDR1-NETWORK1"
                    },
                    "org-openroadm-network-topology:opposite-link": "ROADMB-SRG1-SRG1-PP1-TXRXtoXPDRB-XPDR1-XPDR1-NETWORK1",
                    "org-openroadm-network-topology:link-type": "XPONDER-OUTPUT",
                    "destination": {
                        "dest-node": "ROADMB-SRG1",
                        "dest-tp": "SRG1-PP1-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-SRG1-SRG1-PP1-TXRXtoXPDRB-XPDR1-XPDR1-NETWORK1"
                },
                {
                    "link-id": "ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMA-SRG1",
                        "source-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "ADD-LINK",
                    "destination": {
                        "dest-node": "ROADMA-DEG1",
                        "dest-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX"
                },
                {
                    "link-id": "ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1",
                    "source": {
                        "source-node": "ROADMA-SRG1",
                        "source-tp": "SRG1-PP1-TXRX"
                    },
                    "org-openroadm-network-topology:opposite-link": "XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX",
                    "org-openroadm-network-topology:link-type": "XPONDER-INPUT",
                    "destination": {
                        "dest-node": "XPDRA-XPDR1",
                        "dest-tp": "XPDR1-NETWORK1"
                    },
                    "org-openroadm-common-network:opposite-link": "XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX"
                },
                {
                    "link-id": "ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMC-DEG2",
                        "source-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "EXPRESS-LINK",
                    "destination": {
                        "dest-node": "ROADMC-DEG1",
                        "dest-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX"
                },
                {
                    "link-id": "ROADMB-DEG2-DEG2-TTP-TXRXtoROADMC-DEG1-DEG1-TTP-TXRX",
                    "source": {
                        "source-node": "ROADMB-DEG2",
                        "source-tp": "DEG2-TTP-TXRX"
                    },
                    "org-openroadm-network-topology:OMS-attributes": {
                        "opposite-link": "ROADMC-DEG1-DEG1-TTP-TXRXtoROADMB-DEG2-DEG2-TTP-TXRX",
                        "span": {
                            "spanloss-base": 11.4,
                            "link-concatenation": [
                                {
                                    "SRLG-Id": 0,
                                    "SRLG-length": 100000,
                                    "pmd": 0.5,
                                    "fiber-type": "smf"
                                }
                            ],
                            "spanloss-current": 12,
                            "engineered-spanloss": 12.2,
                            "clfi": "fiber4",
                            "auto-spanloss": "true"
                        }
                    },
                    "org-openroadm-network-topology:link-type": "ROADM-TO-ROADM",
                    "destination": {
                        "dest-node": "ROADMC-DEG1",
                        "dest-tp": "DEG1-TTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-DEG1-DEG1-TTP-TXRXtoROADMB-DEG2-DEG2-TTP-TXRX"
                },
                {
                    "link-id": "ROADMB-DEG2-DEG2-CTP-TXRXtoROADMB-DEG1-DEG1-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMB-DEG2",
                        "source-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "EXPRESS-LINK",
                    "destination": {
                        "dest-node": "ROADMB-DEG1",
                        "dest-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-DEG1-DEG1-CTP-TXRXtoROADMB-DEG2-DEG2-CTP-TXRX"
                },
                {
                    "link-id": "ROADMA-DEG2-DEG2-TTP-TXRXtoROADMB-DEG1-DEG1-TTP-TXRX",
                    "source": {
                        "source-node": "ROADMA-DEG2",
                        "source-tp": "DEG2-TTP-TXRX"
                    },
                    "org-openroadm-network-topology:OMS-attributes": {
                        "opposite-link": "ROADMB-DEG1-DEG1-TTP-TXRXtoROADMA-DEG2-DEG2-TTP-TXRX",
                        "span": {
                            "spanloss-base": 11.4,
                            "link-concatenation": [
                                {
                                    "SRLG-Id": 0,
                                    "SRLG-length": 100000,
                                    "pmd": 0.5,
                                    "fiber-type": "smf"
                                }
                            ],
                            "spanloss-current": 12,
                            "engineered-spanloss": 12.2,
                            "clfi": "fiber5",
                            "auto-spanloss": "true"
                        }
                    },
                    "org-openroadm-network-topology:link-type": "ROADM-TO-ROADM",
                    "destination": {
                        "dest-node": "ROADMB-DEG1",
                        "dest-tp": "DEG1-TTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-DEG1-DEG1-TTP-TXRXtoROADMA-DEG2-DEG2-TTP-TXRX"
                },
                {
                    "link-id": "ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMC-SRG1",
                        "source-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "ADD-LINK",
                    "destination": {
                        "dest-node": "ROADMC-DEG2",
                        "dest-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX"
                },
                {
                    "link-id": "ROADMC-DEG2-DEG2-TTP-TXRXtoROADMA-DEG1-DEG1-TTP-TXRX",
                    "source": {
                        "source-node": "ROADMC-DEG2",
                        "source-tp": "DEG2-TTP-TXRX"
                    },
                    "org-openroadm-network-topology:OMS-attributes": {
                        "opposite-link": "ROADMA-DEG1-DEG1-TTP-TXRXtoROADMC-DEG2-DEG2-TTP-TXRX",
                        "span": {
                            "spanloss-base": 11.4,
                            "link-concatenation": [
                                {
                                    "SRLG-Id": 0,
                                    "SRLG-length": 100000,
                                    "pmd": 0.5,
                                    "fiber-type": "smf"
                                }
                            ],
                            "spanloss-current": 12,
                            "engineered-spanloss": 12.2,
                            "clfi": "fiber6",
                            "auto-spanloss": "true"
                        }
                    },
                    "org-openroadm-network-topology:link-type": "ROADM-TO-ROADM",
                    "destination": {
                        "dest-node": "ROADMA-DEG1",
                        "dest-tp": "DEG1-TTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMA-DEG1-DEG1-TTP-TXRXtoROADMC-DEG2-DEG2-TTP-TXRX"
                },
                {
                    "link-id": "ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMC-SRG1",
                        "source-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "ADD-LINK",
                    "destination": {
                        "dest-node": "ROADMC-DEG1",
                        "dest-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX"
                },
                {
                    "link-id": "ROADMB-SRG1-SRG1-CP-TXRXtoROADMB-DEG1-DEG1-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMB-SRG1",
                        "source-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "ADD-LINK",
                    "destination": {
                        "dest-node": "ROADMB-DEG1",
                        "dest-tp": "DEG1-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-DEG1-DEG1-CTP-TXRXtoROADMB-SRG1-SRG1-CP-TXRX"
                },
                {
                    "link-id": "ROADMB-SRG1-SRG1-CP-TXRXtoROADMB-DEG2-DEG2-CTP-TXRX",
                    "source": {
                        "source-node": "ROADMB-SRG1",
                        "source-tp": "SRG1-CP-TXRX"
                    },
                    "org-openroadm-network-topology:link-type": "ADD-LINK",
                    "destination": {
                        "dest-node": "ROADMB-DEG2",
                        "dest-tp": "DEG2-CTP-TXRX"
                    },
                    "org-openroadm-common-network:opposite-link": "ROADMB-DEG2-DEG2-CTP-TXRXtoROADMB-SRG1-SRG1-CP-TXRX"
                },
                {
                    "link-id": "ROADMB-SRG1-SRG1-PP1-TXRXtoXPDRB-XPDR1-XPDR1-NETWORK1",
                    "source": {
                        "source-node": "ROADMB-SRG1",
                        "source-tp": "SRG1-PP1-TXRX"
                    },
                    "org-openroadm-network-topology:opposite-link": "XPDRB-XPDR1-XPDR1-NETWORK1toROADMB-SRG1-SRG1-PP1-TXRX",
                    "org-openroadm-network-topology:link-type": "XPONDER-INPUT",
                    "destination": {
                        "dest-node": "XPDRB-XPDR1",
                        "dest-tp": "XPDR1-NETWORK1"
                    },
                    "org-openroadm-common-network:opposite-link": "XPDRB-XPDR1-XPDR1-NETWORK1toROADMB-SRG1-SRG1-PP1-TXRX"
                }
            ]
            }]
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    #Test the gnpy
    def test_04_path_computation_xpdr_bi(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
              .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service-1",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-1"
                },
                "service-a-end": {
                    "node-id": "XPDRA",
                    "service-rate": "100",
                    "clli": "nodeA"
                },
                "service-z-end": {
                    "node-id": "XPDRB",
                    "service-rate": "100",
                    "clli": "nodeB"
                }
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['gnpy-response'][0]['path-dir'], 'A-to-Z')
        self.assertEqual(res['output']['gnpy-response'][0]['feasibility'],True)
        self.assertEqual(res['output']['gnpy-response'][1]['path-dir'], 'Z-to-A')
        self.assertEqual(res['output']['gnpy-response'][1]['feasibility'],True)
        time.sleep(5)

    #Disconnect the different topology
    def test_05_disconnect_openroadmTopology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_06_disconnect_openroadmNetwork(self):
        #Config ROADMA
        url = ("{}/config/ietf-network:networks/network/openroadm-network"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_07_disconnect_clliNetwork(self):
        url = ("{}/config/ietf-network:networks/network/clli-network"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

if __name__ == "__main__":
    #logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    unittest.main(verbosity=2)
