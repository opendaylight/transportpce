#!/usr/bin/env python

##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################


import sys
sys.path.append('transportpce_tests/common')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


#
# Basic NetCONF device operations for openconfig
#


def metadata_input():
    url = {'rfc8040': '{}/data/open-terminal-meta-data:open-terminal-meta-data',
           'draft-bierman02': '{}/config/data/open-terminal-meta-data:open-terminal-meta-data'}
    body = {
        "open-terminal-meta-data:open-terminal-meta-data": {
            "transceiver-info": {
                "transceiver": [
                    {
                        "part-no": "Transceiver-part-1",
                        "operational-modes": {
                            "operational-mode": [
                                {
                                    "mode-id": 1,
                                    "catalog-id": "4308",
                                    "rate": "400"
                                }
                            ]
                        },
                        "supported-interface-capability": [
                            {
                                "interface-sequence": [
                                    {
                                        "position": 1,
                                        "interface-type": "openconfig-transport-types:PROT_OTUCN",
                                        "max-interfaces": 1
                                    },
                                    {
                                        "position": 2,
                                        "interface-type": "openconfig-transport-types:PROT_ODUCN",
                                        "max-interfaces": 1
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "part-no": "Transceiver-client-part-1",
                        "operational-modes": {
                            "operational-mode": [
                                {
                                    "mode-id": 1,
                                    "catalog-id": "4308",
                                    "rate": "100"
                                }
                            ]
                        },
                        "supported-interface-capability": [
                            {
                                "interface-sequence": [
                                    {
                                        "position": 1,
                                        "interface-type": "openconfig-transport-types:PROT_100GE",
                                        "max-interfaces": 1
                                    },
                                    {
                                        "position": 2,
                                        "interface-type": "openconfig-transport-types:PROT_ODU4",
                                        "max-interfaces": 1
                                    }
                                ]
                            }
                        ]
                    }
                ]
            },
            "line-card-info": {
                "line-card": [
                    {
                        "part-no": "Linecard component (4 x 400G CFP2-DC + 4 x 100GbE QSFP28 Ports)",
                        "xpdr-type": "MPDR",
                        "supported-port": [
                            {
                                "id": 1,
                                "component-name": "client-qsfp-1",
                                "type": "openconfig-transport-types:TERMINAL_CLIENT"
                            },
                            {
                                "id": 2,
                                "component-name": "client-qsfp-2",
                                "type": "openconfig-transport-types:TERMINAL_CLIENT"
                            },
                            {
                                "id": 3,
                                "component-name": "client-qsfp-3",
                                "type": "openconfig-transport-types:TERMINAL_CLIENT"
                            },
                            {
                                "id": 4,
                                "component-name": "client-qsfp-4",
                                "type": "openconfig-transport-types:TERMINAL_CLIENT"
                            },
                            {
                                "id": 5,
                                "component-name": "line-cfp2-1",
                                "type": "openconfig-transport-types:TERMINAL_LINE"
                            }
                        ],
                        "switch-fabric": [
                            {
                                "switch-fabric-id": 1,
                                "switch-fabric-type": "Blocking",
                                "non-blocking-list": [
                                    {
                                        "nbl-id": 1,
                                        "connectable-port": [
                                            5,
                                            1
                                        ]
                                    },
                                    {
                                        "nbl-id": 2,
                                        "connectable-port": [
                                            5,
                                            2
                                        ]
                                    },
                                    {
                                        "nbl-id": 3,
                                        "connectable-port": [
                                            5,
                                            3
                                        ]
                                    },
                                    {
                                        "nbl-id": 4,
                                        "connectable-port": [
                                            5,
                                            4
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        }
    }
    response = test_utils.put_request(url[test_utils.RESTCONF_VERSION], body)
    return response


def catlog_input():
    url = {'rfc8040': '{}/operations/org-openroadm-service:add-specific-operational-modes-to-catalog',
           'draft-bierman02': '{}/config/operations/org-openroadm-service:add-specific-operational-modes-to-catalog'}
    body = {
        "input": {
            "sdnc-request-header": {
                "request-id": "load-specific-OM-Catalog",
                "rpc-action": "fill-catalog-with-specific-operational-modes",
                "request-system-id": "test"
            },
            "operational-mode-info": {
                "specific-operational-modes": {
                    "specific-operational-mode": [
                        {
                            "operational-mode-id": "4308",
                            "baud-rate": "65.7",
                            "modulation-format": "dp-qam16",
                            "min-RX-osnr-tolerance": "23.000",
                            "min-central-frequency": "191.32500000",
                            "max-central-frequency": "196.12500000",
                            "central-frequency-granularity": "12.50000",
                            "min-spacing": "37.50000",
                            "line-rate": "505.1",
                            "min-TX-osnr": "36.000",
                            "TX-OOB-osnr": {
                                "WR-openroadm-operational-mode-id": "MW-WR-core",
                                "min-OOB-osnr-multi-channel-value": "31.000",
                                "min-OOB-osnr-single-channel-value": "43.000"
                            },
                            "output-power-range": {
                                "WR-openroadm-operational-mode-id": "MW-WR-core",
                                "min-output-power": "-5.000",
                                "max-output-power": "0.000"
                            },
                            "min-input-power-at-RX-osnr": "-14.000",
                            "max-input-power": "1.000",
                            "channel-width": "75.72000",
                            "fec-type": "org-openroadm-common-types:ofec",
                            "min-roll-off": "0.05",
                            "max-roll-off": "0.20",
                            "penalties": [
                                {
                                    "parameter-and-unit": "CD-ps/nm",
                                    "up-to-boundary": "4000.00",
                                    "penalty-value": "0.000"
                                },
                                {
                                    "parameter-and-unit": "CD-ps/nm",
                                    "up-to-boundary": "12000.00",
                                    "penalty-value": "0.500"
                                },
                                {
                                    "parameter-and-unit": "PDL-dB",
                                    "up-to-boundary": "1.00",
                                    "penalty-value": "0.500"
                                },
                                {
                                    "parameter-and-unit": "PDL-dB",
                                    "up-to-boundary": "2.00",
                                    "penalty-value": "1.000"
                                },
                                {
                                    "parameter-and-unit": "PDL-dB",
                                    "up-to-boundary": "4.00",
                                    "penalty-value": "2.500"
                                },
                                {
                                    "parameter-and-unit": "PMD-ps",
                                    "up-to-boundary": "10.00",
                                    "penalty-value": "0.000"
                                },
                                {
                                    "parameter-and-unit": "PMD-ps",
                                    "up-to-boundary": "20.00",
                                    "penalty-value": "0.500"
                                },
                                {
                                    "parameter-and-unit": "power-dBm",
                                    "up-to-boundary": "-14.00",
                                    "penalty-value": "0.000"
                                },
                                {
                                    "parameter-and-unit": "power-dBm",
                                    "up-to-boundary": "-16.00",
                                    "penalty-value": "1.000"
                                },
                                {
                                    "parameter-and-unit": "power-dBm",
                                    "up-to-boundary": "-18.00",
                                    "penalty-value": "2.000"
                                },
                                {
                                    "parameter-and-unit": "cross-talk-total-power-dB",
                                    "up-to-boundary": "13.00",
                                    "penalty-value": "0.300"
                                },
                                {
                                    "parameter-and-unit": "cross-talk-total-power-dB",
                                    "up-to-boundary": "15.00",
                                    "penalty-value": "0.500"
                                },
                                {
                                    "parameter-and-unit": "colorless-drop-adjacent-channel-crosstalk-GHz",
                                    "up-to-boundary": "4.10",
                                    "penalty-value": "0.500"
                                }
                            ]
                        }
                    ]
                }
            }
        }
    }
    response = test_utils.post_request(url[test_utils.RESTCONF_VERSION], body)
    return response


def del_metadata():
    url = {'rfc8040': '{}/data/open-terminal-meta-data:open-terminal-meta-data',
           'draft-bierman02': '{}/config/data/open-terminal-meta-data:open-terminal-meta-data'}
    response = test_utils.delete_request(url[test_utils.RESTCONF_VERSION].format('{}'))
    return {'status_code': response.status_code}
