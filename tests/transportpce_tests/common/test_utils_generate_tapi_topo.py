#!/usr/bin/env python

##############################################################################
# Copyright (c) 2025 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods
# pylint: disable=invalid-name
# pylint: disable=redefined-outer-name

import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
# pylint: disable=too-few-public-methods


class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None


class UuidServices2:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None


class UuidServices3:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None

# class TransportPCEtesting(unittest.TestCase):


processes = []
WAITING = 20  # nominal value is 300
NODE_VERSION = '2.2.1'
uuid_services = UuidServices()
uuid_services2 = UuidServices2()
uuid_services3 = UuidServices3()
del_serv_input_data = {"uuid": "TBD"}
tapi_topo = {"topology-id": "TBD"}


def init_simulators():
    print("Entering TestUtils setupClass starting simulators")
    # pylint: disable=consider-using-f-string
    print("Node version is {}".format(NODE_VERSION))
    processes = test_utils.start_sims([('spdra', NODE_VERSION),
                                       ('roadma', NODE_VERSION),
                                       ('roadmc', NODE_VERSION),
                                       ('xpdra', NODE_VERSION),
                                       ('xpdrc', NODE_VERSION),
                                       ('spdrc', NODE_VERSION)])
    return processes


def connect_xpdrs():
    print("Connecting SPDRA")
    test_utils.mount_device("SPDR-SA1", ('spdra', NODE_VERSION))
    time.sleep(2)
    print("Connecting SPDRC")
    test_utils.mount_device("SPDR-SC1", ('spdrc', NODE_VERSION))
    time.sleep(2)
    print("Connecting XPDRA")
    test_utils.mount_device("XPDR-A1", ('xpdra', NODE_VERSION))
    time.sleep(2)
    print("Connecting XPDRC")
    test_utils.mount_device("XPDR-C1", ('xpdrc', NODE_VERSION))
    time.sleep(2)


def connect_rdms():
    print("Connecting ROADMA")
    response = test_utils.mount_device("ROADM-A1", ('roadma', NODE_VERSION))
    if response.status_code in [200, 201, 202, 403]:
        print("Connecting ROADMA failed")
    time.sleep(2)
    print("Connecting ROADMC")
    response = test_utils.mount_device("ROADM-C1", ('roadmc', NODE_VERSION))
    if response.status_code in [200, 201, 202, 403]:
        print("Connecting ROADMC failed")
    time.sleep(2)


def interconnect_roadm_and_xpdr():
    # pylint: disable=too-many-branches
    # pylint: disable=too-many-statements
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRA1-X1 to ROADMA1 failed")
    else:
        print("Connection from SPDRA1-X1 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X1 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSA1-X2 to ROADMA1 failed")
    else:
        print("Connection from SPDRSA1-X2 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X2 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X2 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSA1-X3 to ROADMA1 failed")
    else:
        print("Connection from SPDRSA1-X3 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X3 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X3 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from XPDRA1-X1 to ROADMA1 failed")
    else:
        print("Connection from XPDRA1-X1 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to XPDRA1-X1 failed")
    else:
        print("Connection from ROADMA1 to XPDRA1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X1 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X1 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X1 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X2 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X2 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X2 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X2 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X3 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X3 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X3 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X3 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from XPDRC1-X1 to ROADMC1 failed")
    else:
        print("Connection from XPDRC1-X1 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to XPDRC1-X1 failed")
    else:
        print("Connection from ROADMC1 to XPDRC1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSA1-X2 to ROADMA1 failed")
    else:
        print("Connection from SPDRSA1-X2 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X2 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X2 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X2 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X2 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X2 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X2 succeeded")
    time.sleep(2)


def add_oms_attributes_to_roadm2roadm_links():
    # Config ROADMA-ROADMC oms-attributes
    data = {"span": {
        "auto-spanloss": "true",
        "spanloss-base": 11.4,
        "spanloss-current": 12,
        "engineered-spanloss": 12.2,
        "link-concatenation": [{
            "SRLG-Id": 0,
            "fiber-type": "smf",
            "SRLG-length": 100000,
            "pmd": 0.5}]}}
    response = test_utils.add_oms_attr_request(
        "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
    if response.status_code != requests.codes.created:
        print("OMS attributes add on link ROADMA-ROADMC failed")
    else:
        print("OMS attributes added on link ROADMA-ROADMC")
    # Config ROADMC-ROADMA oms-attributes
    data = {"span": {
        "auto-spanloss": "true",
        "spanloss-base": 11.4,
        "spanloss-current": 12,
        "engineered-spanloss": 12.2,
        "link-concatenation": [{
            "SRLG-Id": 0,
            "fiber-type": "smf",
            "SRLG-length": 100000,
            "pmd": 0.5}]}}
    response = test_utils.add_oms_attr_request(
        "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
    if response.status_code != requests.codes.created:
        print("OMS attributes add on link ROADMC-ROADMA failed")
    else:
        print("OMS attributes added on link ROADMC-ROADMA")


def cleanup_and_disconnect_devices():
    disconnect_xpdrA_C()
    disconnect_spdrA_C()


def disconnect_spdrA_C():
    response = test_utils.unmount_device("SPDR-SA1")
    if response.status_code not in [204]:
        print("disconnection of SPDR-SA1 failed")
        response = test_utils.unmount_device("SPDR-SC1")
    if response.status_code not in [204]:
        print("disconnection of SPDR-SC1 failed")


def disconnect_xpdrA_C():
    response = test_utils.unmount_device("XPDR-A1")
    if response.status_code not in [204]:
        print("disconnection of SPDR-SA1 failed")
        response = test_utils.unmount_device("XPDR-C1")
    if response.status_code not in [204]:
        print("disconnection of SPDR-SC1 failed")


def disconnect_roadmA_C():
    response = test_utils.unmount_device("ROADM-A1")
    if response.status_code not in [204]:
        print("disconnection of ROADM-A1 failed")
    response = test_utils.unmount_device("ROADM-C1")
    if response.status_code not in [204]:
        print("disconnection of ROADM-C1 failed")


def uninstall_Tapi_Feature():
    test_utils.uninstall_karaf_feature("odl-transportpce-tapi")
    time.sleep(5)
    response = test_utils.get_ietf_network_request('otn-topology', 'config')
    if requests.codes.ok == response['status_code']:
        print("Tapi Feature uninstallation :")
    if ('node' not in response['network'][0] and 'ietf-network-topology:link' not in response['network'][0]):
        print(" otn topology empty")
    response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
    if ((requests.codes.ok == response['status_code']) and
            ('node' not in response['network'][0] and 'ietf-network-topology:link' not in response['network'][0])):
        print("OpenROADM topology also empty")
        print("Confirm Tapi Feature correctly uninstalled")


if __name__ == "__main__":
    unittest.main(verbosity=2)
