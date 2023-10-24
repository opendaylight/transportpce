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
import os
import json
from ncclient import manager, xml_
import requests
import xmltodict
import re
import test_utils
import datetime
import time

import logging
"""
1. DEBUG
2. INFO
3. WARNING
4. ERROR
5. CRITICAL
"""
logger = logging.getLogger()
logger.level = logging.DEBUG
log_format = logging.Formatter("%(name)s - %(levelname)s - %(message)s")
stream_handler = logging.StreamHandler(sys.stdout)
stream_handler.setFormatter(log_format)
logger.addHandler(stream_handler)

TD_report_id = {
    "Ribbon": 1233,
    "Fujitsu": 1341
}

CHECKPOINTS = {
    '7.1': {
        'shelves': ['shelf-name', 'shelf-type', 'administrative-state', 'vendor', 'model', 'serial-id',
                    'operational-state', 'is-physical', 'is-passive', 'faceplate-label', 'slots'],
        'slots': ['slot-name'],
        'cps': ['circuit-pack-type', 'circuit-pack-name', 'administrative-state', 'vendor', 'model', 'serial-id',
                'operational-state',
                'shelf', 'slot', 'is-pluggable-optics', 'is-physical', 'is-passive', 'faceplate-label',
                'circuit-pack-category', 'cp-slots', 'ports'],
        'circuit-pack-category': ['type'],
        'cp-slots': ['slot-name'],
        'ports': ['port-name', 'port-direction', 'is-physical', 'faceplate-label', 'operational-state',
                  'port-capabilities'],
        'port-capabilities': ['supported-interface-capability'],
        'supported-interface-capability': ['if-cap-type'],
        'xponder': ['xpdr-number', 'xpdr-type', 'xpdr-port'],
        'xpdr-port': ['index', 'circuit-pack-name', 'port-name']
    },
    '2.2.1': {
        'shelves': ['shelf-name', 'shelf-type', 'vendor', 'model', 'serial-id', 'slots'],
        'slots': ['slot-name'],
        'cps': ['circuit-pack-type', 'circuit-pack-name', 'vendor', 'model', 'serial-id',
                'shelf', 'slot', 'is-pluggable-optics',
                'circuit-pack-category', 'cp-slots', 'ports'],
        'circuit-pack-category': ['type'],
        'cp-slots': ['slot-name'],
        'ports': ['port-name', 'port-direction', 'operational-state', 'port-capabilities'],
        'port-capabilities': ['supported-interface-capability'],
        'supported-interface-capability': ['if-cap-type'],
        'xponder': ['xpdr-number', 'xpdr-type', 'xpdr-port'],
        'xpdr-port': ['index', 'circuit-pack-name', 'port-name'],
        'degree': ['degree-number', 'max-wavelengths'],
        'shared-risk-group': ['max-add-drop-ports', 'current-provisioned-add-drop-ports', 'srg-number',
                              'wavelength-duplication']
    }
}

# TODO: replace 100G, 400G client hardcoded with this list
client_400G_capabilities = []
client_100G_capabilities = ["org-openroadm-port-types:if-100GE-ODU4", "org-openroadm-port-types:if-100GE",
                            "org-openroadm-port-types:if-OCH-OTU4-ODU4"]
client_10G_capabilities = ["org-openroadm-port-types:if-10GE-ODU2e", "org-openroadm-port-types:if-10GE-ODU2",
                           "org-openroadm-port-types:if-10GE"]
client_1G_capabilities = ["org-openroadm-port-types:if-1GE-ODU0"]

parent_odu_allocation = {
    '400': ['1.1', '1.10', '1.11', '1.12', '1.13', '1.14', '1.15', '1.16', '1.17', '1.18', '1.19', '1.2', '1.20', '1.3',
            '1.4', '1.5', '1.6', '1.7', '1.8', '1.9',
            '2.1', '2.10', '2.11', '2.12', '2.13', '2.14', '2.15', '2.16', '2.17', '2.18', '2.19', '2.2', '2.20', '2.3',
            '2.4', '2.5', '2.6', '2.7', '2.8', '2.9',
            '3.1', '3.10', '3.11', '3.12', '3.13', '3.14', '3.15', '3.16', '3.17', '3.18', '3.19', '3.2', '3.20', '3.3',
            '3.4', '3.5', '3.6', '3.7', '3.8', '3.9',
            '4.1', '4.10', '4.11', '4.12', '4.13', '4.14', '4.15', '4.16', '4.17', '4.18', '4.19', '4.2', '4.20', '4.3',
            '4.4', '4.5', '4.6', '4.7', '4.8', '4.9']
}


class PMFiltering:
    def __init__(self):
        self.filters = {
            'current-pm-entry': True,
            'pm-resource-type': '',
            'pm-resource-type-extension': '',
            'pm-resource-instance': '',
            'pm-retrieval-time': '',
            'current-pm_type': '',
            'current-pm_extension': '',
            'current-pm_location': '',
            'current-pm_direction': '',
            'current-pm_measurement_pmParameterValue': '',
            'current-pm_measurement_pmParameterUnit': '',
            'current-pm_measurement_validity': '',
            'current-pm_measurement_granularity': '15min'
        }


class AlarmFiltering:
    def __init__(self):
        self.filters = {
            'circuit-pack-name': '',
            'port-name': '',
            'interface-name': '',
            'resourceType': '',
            'cause': '',
            'location': '',
            'direction': ''
        }


def get_devices_from_TD(vendor):
    """
    Get vendor all device report, by vendor's report ID from TDX
    :param vendor:
    :return: response
    """
    #
    if vendor not in TD_report_id.keys():
        logger.error(f"Vendor name {vendor} not supported {list(TD_report_id.keys())}")
        return False
    url = ("https://atlas.utdallas.edu/TDWebApi/api/reports/"   # TDX url
           + str(TD_report_id[vendor])   # vendor ID
           + "?withData=true&dataSortExpression={dataSortExpression}")

    payload = ""
    headers = {
        'Authorization': 'Bearer ' + os.getenv("TOKEN")
    }
    logger.debug("Attempt to get TDX report from vendor: " + vendor + " " + str(
        TD_report_id[vendor]))  # , vendor, TD_report_id[vendor]
    response = requests.request("GET", url, headers=headers, data=payload)

    logger.info("response text: " + str(response.text))
    logger.debug("response code: " + str(response.status_code))

    return response


def get_device(td_vendor_response, nodeId):
    # parse response from TDX, get desired node from nodeId
    column_name = {}
    if td_vendor_response.status_code == 200:
        logger.debug("Get device report from TDX got response code = 200")
        for column in td_vendor_response.json()["DisplayedColumns"]:
            logger.debug(str(column))
            if column["HeaderText"] == "Name":
                column_name["Name"] = column['SortColumnName']
            elif column["HeaderText"] == "Vendor":
                column_name["Vendor"] = column["SortColumnName"]
            elif column["HeaderText"] == "Mgmt IP":
                column_name["Mgmt IP"] = column["SortColumnName"]
            elif column["HeaderText"] == "Netconf-port":
                column_name["Netconf-port"] = column["SortColumnName"]
            elif column["HeaderText"] == "User":
                column_name["User"] = column["SortColumnName"]
            elif column["HeaderText"] == "Password":
                column_name["Password"] = column["SortColumnName"]
            elif column["HeaderText"] == "Node-Type":
                column_name["Node-Type"] = column["SortColumnName"]
            elif column["HeaderText"] == "XPDR Type":
                column_name["XPDR Type"] = column["SortColumnName"]
            elif column["HeaderText"] == "Openroadm-version":
                column_name["Openroadm-version"] = column["SortColumnName"]
            elif column["HeaderText"] == "Netconf stream":
                column_name["Netconf stream"] = column["SortColumnName"]
            elif column["HeaderText"] == "Netconf stream name":
                column_name["Netconf stream name"] = column["SortColumnName"]
            elif column["HeaderText"] == "Notification":
                column_name["Notification"] = column["SortColumnName"]
            elif column["HeaderText"] == "XPDR network modules":
                column_name["XPDR network modules"] = column["SortColumnName"]
            elif column["HeaderText"] == "XPDR Network Port Speed":
                column_name["XPDR Network Port Speed"] = column["SortColumnName"]
            elif column["HeaderText"] == "XPDR Maximum ports":
                column_name["XPDR Maximum ports"] = column["SortColumnName"]
            else:
                pass
        _device = {
            "NAME": '',
            "VENDOR": '',
            "NODETYPE": '',
            "USERNAME": '',
            "PASSWORD": '',
            "HOST": '',
            "PORT": '',
            "OR_VERSION": ''
        }
        for device in td_vendor_response.json()["DataRows"]:
            if device[column_name["Name"]] == nodeId:
                _device["NAME"] = device[column_name["Name"]]
                _device["VENDOR"] = device[column_name["Vendor"]]
                _device["NODETYPE"] = device[column_name["Node-Type"]]
                _device["USERNAME"] = device[column_name["User"]]
                _device["PASSWORD"] = device[column_name["Password"]]
                _device["HOST"] = device[column_name["Mgmt IP"]]
                _device["PORT"] = device[column_name["Netconf-port"]]
                _device["OR_VERSION"] = device[column_name["Openroadm-version"]]

                # TODO: based on nodetype, add additional parameters

    else:
        logger.warning("device not found in TDX")
        _device = {
            "NAME": '',
            "VENDOR": '',
            "NODETYPE": '',
            "USERNAME": '',
            "PASSWORD": '',
            "HOST": '',
            "PORT": '',
            "OR_VERSION": ''
        }

    return _device


def get(nc, query='', device=True, tag=''):
    """ Just get and return the raw response filtered using the values in 'query'
        If device = 'True' the filter is set using the device and the query as tag of the device
        If device = 'False' the filter is set using the tag that has to be passed to the function """
    current_time = datetime.datetime.now()
    print('\n    + OpenROADMNE get starting -' + str(current_time))
    if nc is None or nc.connected is None or nc.connected is False:
        print('\n  - Error - The connection to the NE is down')
        return None
    tag_device = ''
    if device:
        tag_device = """<org-openroadm-device xmlns="http://org/openroadm/device">
            """ + query + """
        </org-openroadm-device>"""
        tag = ''
    rpc = """<nc:get xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
        <nc:filter type="subtree">
            """ + tag_device + tag + """
        </nc:filter>
    </nc:get>"""
    print("\n    - Querying for :  '%s'" % query)
    print("\n    - RPC body is: \n", rpc)
    nc_response = nc.dispatch(xml_.to_ele(rpc))
    print('\n    - got a nc_response in OpenROADMNE get')
    print(':\n' + str(nc_response))
    print('\n    + get ending - ' + str(
        datetime.datetime.now() - current_time))
    return nc_response


def get_PM(nc, PM_filter):
    """get and return the raw list of PM filtered using PM-filter passed as argument
        PM-filter is defined as class of its own"""
    current_time = datetime.datetime.now()
    print('    + OpenROADMNE get_PM starting -' + str(current_time))
    if nc is None or nc.connected is None or nc.connected is False:
        print('\n  - Error - The connection to the NE is down')
        return None
    # filter_tag = """              <current-pm-list xmlns="http://org/openroadm/pm">
    #         <current-pm-entry>
    #             <pm-resource-type>""" + PM_filter['pm-resource-type'] + """</pm-resource-type>
    #             <pm-resource-type-extension>""" + PM_filter['pm-resource-type-extension'] + """</pm-resource-type-extension>    
    #             <pm-resource-instance xmlns:org-openroadm-device="http://org/openroadm/device">""" + PM_filter['pm-resource-instance'] + """</pm-resource-instance>
    #             <retrieval-time>""" + PM_filter['pm-retrieval-time'] + """</retrieval-time>    
    #             <current-pm>
    #             <type>""" + PM_filter['current-pm_type'] + """</type>
    #             <extension>""" + PM_filter['current-pm_extension'] + """</extension>    
    #             <location>""" + PM_filter['current-pm_location'] + """</location>    
    #             <direction>""" + PM_filter['current-pm_direction'] + """</direction>
    #             <measurement>
    #                 <pmParameterValue>""" + PM_filter['current-pm_measurement_pmParameterValue'] + """</pmParameterValue>
    #                 <pmParameterUnit>""" + PM_filter['current-pm_measurement_pmParameterValue'] + """</pmParameterUnit>
    #                 <validity>""" + PM_filter['current-pm_measurement_pmParameterValue'] + """</validity>
    #                 <granularity>""" + PM_filter['current-pm_measurement_granularity'] + """</granularity>
    #             </measurement>
    #             </current-pm>
    #         </current-pm-entry>
    #         </current-pm-list>"""
    filter_tag = """              <current-pm-list xmlns="http://org/openroadm/pm">
            <current-pm-entry>
                <pm-resource-type>""" + PM_filter['pm-resource-type'] + """</pm-resource-type>
                <pm-resource-instance xmlns:a="http://org/openroadm/device">""" + PM_filter['pm-resource-instance'] + """</pm-resource-instance>
                <current-pm>
                <type>""" + PM_filter['current-pm_type'] + """</type>
                <measurement>
                    <granularity>""" + PM_filter['current-pm_measurement_granularity'] + """</granularity>
                </measurement>
                </current-pm>
            </current-pm-entry>
            </current-pm-list>"""
    PM_xml = get(nc, device=False, tag=filter_tag)
    print('\n    - got a response in the get_PM- ')
    print(':\n' + str(PM_xml))
    print('\n    + get_PM ending - ' + str(datetime.datetime.now() - current_time))
    return PM_xml


def parse_rpc_reply(nc_response):  # nc_response should be the GetReply.xml, add .xml before pass the input
    """ this function parse a response and return the parsed response
        if a group block is present, only the  content of data block is returned"""
    # TODO is there a way to have all parse in one unique parsing function?
    current_time = datetime.datetime.now()
    parsed_data = ""
    print('\n    + container parse_rpc_reply starting - ' + str(current_time))

    # only one of them will work
    try:
        print("try rpc-type body parse head ...")
        parsed_data = xmltodict.parse(nc_response)['rpc-reply']
    except Exception as err:
        print('rpc-reply type is not used. ', err)

    try:
        print("try nc:rpc-type body parse head ...")
        parsed_data = xmltodict.parse(nc_response)['nc:rpc-reply']
    except Exception as err:
        print('nc:rpc-reply type is not used. ', err)

    try:
        if bool(parsed_data['data']):
            # print ('data present')
            parsed_data = parsed_data['data']
    except Exception as err:
        print('data NOT present', err)
    if bool(parsed_data):
        print('\n    - we parsed something')
        print(':\n' + str(parsed_data))
    else:
        print('\n    - Error - Parsed response is empty')
    # except Exception as err:
    #    self.results = '  - Response parsing Exception - ' + str(err)
    print('\n    + parse_rpc_reply ending - ' + str(datetime.datetime.now() - current_time))
    return parsed_data


def parse_PM(PM_xml):  # 1x
    print('\n    + parse_PM starting')
    parsed_data = parse_rpc_reply(PM_xml)
    print('    - PM xml parsed in parse_PM')
    parsed_PM_list = parsed_data['current-pm-list']['current-pm-entry']
    print('\n    - response in the parse_PM parsed')
    if parsed_data is None:
        print('\n  -  unexpected condition encountered in parse_alarms')
    else:
        print('\n    - parsed response in the check_streams is not None')
        print('\n  - parse_PM found ' + str(len(parsed_PM_list)) + ' PM')
    print('\n    + parse_PM ending \n')
    return parsed_PM_list


def RPC_PM_Port(_my_NE_nc, x_dest_CP, x_dest_Port, P_side='opticalPowerInput'):
    PM_filter = PMFiltering()  # create a new PM filtering and change the default filtering value that needs changing
    PM_filter.filters['pm-resource-type'] = "port"
    PM_filter.filters[
        'pm-resource-instance'] = """/a:org-openroadm-device/a:circuit-packs[a:circuit-pack-name=""" + x_dest_CP + """]/a:ports[a:port-name=""" + x_dest_Port + """]"""
    if _my_NE_nc is None or _my_NE_nc.connected is None or _my_NE_nc.connected is False:
        return None
    if P_side == 'opticalPowerInput':
        PM_filter.filters['current-pm_type'] = 'opticalPowerInput'
    elif P_side == 'opticalPowerOutput':
        PM_filter.filters['current-pm_type'] = 'opticalPowerOutput'
    else:
        print('  - Warning - %s is not supported ' % P_side)
    nc_response = get_PM(_my_NE_nc, PM_filter.filters)  # get PM using the filtering prepared above
    parsed_PM = parse_PM(nc_response.xml)  # parse the PM - response in xml log_format -
    # parsed and extracted to the level of ['current-pm-list']['current-pm-entry']
    return parsed_PM['current-pm']['measurement']['pmParameterValue']


def RPC_PM_Interface(_my_NE_nc, IF_name, P_side='opticalPowerOutput'):
    PM_filter = PMFiltering()  # create a new PM filtering and change the default filtering value that needs changing
    PM_filter.filters['pm-resource-type'] = "interface"
    # PM_filter.filters['pm-resource-instance'] = '/org-openroadm-device:org-openroadm-device/org-openroadm-device:interface[org-openroadm-device:name=' + IF_name + ']'
    PM_filter.filters['pm-resource-instance'] = '/a:org-openroadm-device/a:interface[a:name=' + IF_name + ']'
    if _my_NE_nc is None or _my_NE_nc.connected is None or _my_NE_nc.connected is False:
        return None
    if P_side == 'opticalPowerOutput':
        PM_filter.filters['current-pm_type'] = 'opticalPowerOutput'
    elif P_side == 'opticalPowerInput':
        PM_filter.filters['current-pm_type'] = 'opticalPowerInput'
    elif P_side == 'preFECCorrectedErrors':
        PM_filter.filters['current-pm_type'] = 'preFECCorrectedErrors'
    elif P_side == 'FECUncorrectableBlocks':
        PM_filter.filters['current-pm_type'] = 'FECUncorrectableBlocks'
    else:
        print('  - Warning - %s is not supported ' % P_side)
    nc_response = get_PM(_my_NE_nc, PM_filter.filters)  # get PM using the filtering from above
    parsed_PM = parse_PM(nc_response.xml)  # parse the PM - response in xml log_format -
    # parsed and extracted to the level of ['current-pm-list']['current-pm-entry']
    return parsed_PM['current-pm']['measurement']['pmParameterValue']


def get_all_Alarm(nc):
    """get and return the raw list of Alarm filtered using Alarm-filter passed as argument
        Alarm-filter is defined as class of its own"""
    current_time = datetime.datetime.now()
    print('    + OpenROADMNE get_Alarm starting -' + str(current_time))
    if nc is None or nc.connected is None or nc.connected is False:
        print('\n  - Error - The connection to the NE is down')
        return None
    filter_tag = """
            <active-alarm-list xmlns="http://org/openroadm/alarm">
                </active-alarm-list>"""
    try:
        Alarm_xml = get(nc, device=False, tag=filter_tag)
    except Exception as e:
        print("error raised:", e)
    print('\n    - got a response in the get_Alarm- ')
    print(':\n' + str(Alarm_xml))
    print('\n    + get_Alarm ending - ' + str(datetime.datetime.now() - current_time))
    return Alarm_xml


def parse_Alarm(Alarm_xml):  # 1x
    print('\n    + parse_Alarm starting')
    parsed_data = parse_rpc_reply(Alarm_xml)
    print('    - PM xml parsed in parse_Alarm')
    parsed_Alarm_list = parsed_data['active-alarm-list']['activeAlarms']
    print('\n    - response in the parse_Alarm parsed')
    if parsed_data is None:
        print('\n  -  unexpected condition encountered in parse_alarms')
    else:
        print('\n    - parsed response in the check_streams is not None')
        print('\n  - parse_Alarm found ' + str(len(parsed_Alarm_list)) + ' Alarms')
    print('\n    + parse_Alarm ending \n')
    return parsed_Alarm_list


def check_alarm_existance(alarmList, alarmType: str, resourceName: str, cp_name: str):
    alarmExist = False
    for alarm in alarmList:
        if alarm["resource"]["resourceType"] == alarmType:
            if alarmType == "interface" and alarm["resource"]["resource"]["interface-name"] == resourceName:
                logger.debug("alarm found: " + alarm["probableCause"]["cause"])
                alarmExist = True
                break
            elif alarmType == "port" \
                    and alarm["resource"]["resource"]["port"]["port-name"] == resourceName \
                    and alarm["resource"]["resource"]["port"]["circuit-pack-name"] == cp_name:
                logger.debug("alarm found: " + alarm["probableCause"]["cause"])
                alarmExist = True
                break
            else:
                pass
    return alarmExist


def create_pm_honeynode(host: str, port: str, interfaceName: str):
    url = "{}/operations/pm-handling:pm-interact"
    body = {
        "input": {
            "rpc-action": "create",
            "pm-to-be-set-or-created": {
                "current-pm-entry": [
                    {
                        "pm-resource-instance": "/org-openroadm-device:org-openroadm-device/org-openroadm-device:interface[org-openroadm-device:name=" + interfaceName + "]",
                        "pm-resource-type": "interface",
                        "pm-resource-type-extension": "",
                        "start-time": "2018-06-06T10:00:00+00:00",
                        "retrieval-time": "2018-06-07T13:22:58+00:00",
                        "current-pm": [
                            {
                                "type": "opticalPowerInput",
                                "extension": "",
                                "location": "nearEnd",
                                "direction": "rx",
                                "measurement": [
                                    {
                                        "granularity": "24Hour",
                                        "pmParameterValue": -3,
                                        "pmParameterUnit": "dBm",
                                        "validity": "complete"
                                    },
                                    {
                                        "granularity": "15min",
                                        "pmParameterValue": -5.5,
                                        "pmParameterUnit": "dBm",
                                        "validity": "complete"
                                    }
                                ]
                            },
                            {
                                "type": "opticalPowerOutput",
                                "extension": "",
                                "location": "nearEnd",
                                "direction": "tx",
                                "measurement": [
                                    {
                                        "granularity": "24Hour",
                                        "pmParameterValue": -6.7,
                                        "pmParameterUnit": "dBm",
                                        "validity": "complete"
                                    },
                                    {
                                        "granularity": "15min",
                                        "pmParameterValue": -3.2,
                                        "pmParameterUnit": "dBm",
                                        "validity": "complete"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        }
    }
    response = requests.request("POST", url.format("http://" + host + ":8130" + "/restconf"),
                                data=json.dumps(body), headers=TYPE_APPLICATION_JSON,
                                auth=(os.getenv("USERNAME"), os.getenv("PASSWORD")),
                                timeout=REQUEST_TIMEOUT)
    print(response.json())
    if response.status_code == requests.codes.ok and \
            response.json()['output']['status-message'] == "The PMs has been successfully created !":
        print("The PMs has been successfully set !")
        time.sleep(2)
        return True
    else:
        print("PM creation failed")
        return False


def delete_pm_honeynode(host: str, port: str, interfaceName: str):
    url = "{}/operations/pm-handling:pm-interact"
    body = {
        "input": {
            "rpc-action": "delete",
            "pm-to-get-clear-or-delete": {
                "current-pm-entry": [
                    {
                        "pm-resource-instance": "/org-openroadm-device:org-openroadm-device/org-openroadm-device:interface[org-openroadm-device:name=" + interfaceName + "]",
                        "pm-resource-type": "interface",
                        "pm-resource-type-extension": "",
                        "current-pm": [
                            {
                                "type": "opticalPowerInput",
                                "extension": "",
                                "location": "nearEnd",
                                "direction": "rx"
                            },
                            {
                                "type": "opticalPowerOutput",
                                "extension": "",
                                "location": "nearEnd",
                                "direction": "tx"
                            }
                        ]
                    }
                ]
            }
        }
    }
    response = requests.request("POST", url.format("http://" + host + ":8130" + "/restconf"),
                                data=json.dumps(body), headers=TYPE_APPLICATION_JSON,
                                auth=(os.getenv("USERNAME"), os.getenv("PASSWORD")),
                                timeout=REQUEST_TIMEOUT)
    print(response.json())
    if response.status_code == requests.codes.ok and \
            response.json()['output']['status-message'] == "The PMs has been successfully deleted !":
        print("The PMs has been successfully deleted !")
        time.sleep(2)
        return True
    else:
        print("PM delete failed")
        return False


def check_ping(host, device):
    if host == device["HOST"]:
        logger.debug("HOST from TDX matches file input!")
    else:
        logger.warning("HOST does not match! Using local HOST value")

    response = os.system("ping -c 1 " + host)
    # and then check the response...
    if response == 0:
        logger.debug(str(response))
        pingstatus = "Network Active"
    else:
        logger.warning(str(response))
        pingstatus = "Network Error"

    return pingstatus


def make_netconf_connection(host: str, port: str, username="openroadm", password="openroadm", timeout=30):
    try:
        conn = manager.connect(host=host, port=port, username=username, password=password, timeout=timeout,
                               hostkey_verify=False, look_for_keys=False, allow_agent=False)
        if conn.connected == True:
            logger.debug("connection status is " + str(conn.connected))
            logger.debug("connection session_id is " + str(conn.session_id))
            return conn
    except TimeoutError:
        logger.error(
            'Timeout when trying to connect to ' + str(host) + ":" + str(port) + " after " + str(timeout) + " seconds")
    except Exception as e:
        logger.error('Error raised: ', exc_info=True)

    return False


def disconnect_netconf(conn):
    logger.debug("Closing session... ")
    try:
        conn.close_session()
        if conn.connected == False:
            logger.debug("connection status is " + str(conn.connected))
            return True
    except Exception:
        logger.error('Error raised: ', exc_info=True)

    return False


def check_device_netconf_connection(device, host: str, port: str, username="openroadm", password="openroadm",
                                    timeout=30):
    # Check if device ip and netconf port can be reached by username and password
    netconfconnection = False
    netconfdisconnection = False

    if host == device["HOST"] and port == str(device["PORT"]) and username == device["USERNAME"] and password == device[
        "PASSWORD"]:
        logger.debug("Device Parameters match!")

    else:
        logger.warning("Device Parameters do not match! Using local Parameters value")
        logger.debug(
            "Parameters from TDX: " + device["HOST"] + " " + str(device["PORT"]) + " " + device["USERNAME"] + " " +
            device["PASSWORD"])
        logger.debug("Parameters from localfile: " + host + " " + port + " " + username + " " + password)

    conn = make_netconf_connection(host=host, port=port, username=username, password=password, timeout=timeout)
    if conn:
        netconfconnection = True
    if netconfconnection == True and disconnect_netconf(conn):
        netconfdisconnection = True

    return netconfconnection, netconfdisconnection


def check_device_netconf_capabilities(host: str, port: str, username="openroadm", password="openroadm", timeout=30):
    conn = make_netconf_connection(host=host, port=port, username=username, password=password, timeout=timeout)
    # TODO: temperoraly just counting the number of supported capabilities, need to check if this list contains all the necessary capabilities
    numCapabilities = 0
    if conn:
        for c in conn.server_capabilities:
            logger.debug(str(c))
            numCapabilities += 1

        disconnect_netconf(conn)
    logger.debug("number of capabilities is : " + str(numCapabilities))
    return numCapabilities


# check_device_openroadm_container: get device openroadm, accept container tag, e.g., info, cp, ...
# check_device_container: get the device all, accept container tag, e.g., netconf, ...
def check_device_openroadm_container(tag: str, host: str, port: str, username="openroadm", password="openroadm",
                                     timeout=30):
    running_config = False
    dict = {
        'info': '''<info></info>''',
        'cp': '''<circuit-packs></circuit-packs>''',
        '': ''
    }  # TODO: more container tags
    if dict.get(tag) is None:
        logger.error('Invalid tag value.')
        raise ValueError('Invalid tag value.')

    conn = make_netconf_connection(host=host, port=port, username=username, password=password, timeout=timeout)
    if conn:

        filter = '''
            <nc:filter type="subtree" xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
                <org-openroadm-device xmlns="http://org/openroadm/device">''' + dict.get(tag) + '''
                </org-openroadm-device>
            </nc:filter>
        '''

        try:
            running_config = conn.get(filter)
        except Exception:
            logger.error('Error raised: ', exc_info=True)

        # close connection
        disconnect_netconf(conn)

    return running_config


def check_device_container(tag: str, host: str, port: str, username="openroadm", password="openroadm", timeout=30):
    running_config = False
    dict = {
        'netconf': '''<netconf xmlns="urn:ietf:params:xml:ns:netmod:notification"></netconf>'''
    }  # TODO: more container tags
    if dict.get(tag) is None:
        logger.error("Invalid tag value.")
        raise ValueError('Invalid tag value.')

    conn = make_netconf_connection(host=host, port=port, username=username, password=password, timeout=timeout)
    if conn:

        filter = '''
            <nc:filter type="subtree" xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">''' + dict.get(tag) + '''
            </nc:filter>
        '''

        try:
            running_config = conn.get(filter)
        except Exception:
            logger.error('Error raised: ', exc_info=True)

        # close connection
        disconnect_netconf(conn)

    return running_config


def check_device_netconf_container(running_config):
    streamExist = False
    accepted_stream_names = ['OPENROADM', 'NETCONF']
    try:
        conf_json = xmltodict.parse(str(running_config))
        streams = conf_json['rpc-reply']["data"]['netconf']['streams']['stream']
        logger.info('netconf stream is/are: ' + str(streams))
        if isinstance(streams, list):
            for stream in streams:
                if stream['name'] in accepted_stream_names:
                    logger.info("Stream " + stream['name'] + " is available.")
                    streamExist = True
                else:
                    logger.info("Stream " + stream['name'] + " is not valid.")
        elif isinstance(streams, dict):
            if streams['name'] in accepted_stream_names:
                logger.info("Stream " + streams['name'] + " is available.")
                streamExist = True
            else:
                logger.info("Stream " + streams['name'] + " is not valid.")
        else:
            logger.error("stream is empty or not a list or json type")
            raise ValueError("stream is empty or not a list or json type")
    except Exception:
        logger.error('Error raised: ', exc_info=True)

    return streamExist


def check_device_info_container(running_config):
    vendorFound = False
    modelFound = False
    serialIdFound = False
    if running_config:
        conf_json = xmltodict.parse(str(running_config))
        logger.info("info container: " + str(conf_json['rpc-reply']["data"]['org-openroadm-device']['info']))
        logger.info("vendor is: " + conf_json['rpc-reply']["data"]['org-openroadm-device']['info']['vendor'])
        vendorFound = True
        logger.info("model is: " + conf_json['rpc-reply']["data"]['org-openroadm-device']['info']['model'])
        modelFound = True
        logger.info("serial id is: " + conf_json['rpc-reply']["data"]['org-openroadm-device']['info']['serial-id'])
        serialIdFound = True

    return vendorFound, modelFound, serialIdFound


def check_device_netconf_get_yang(running_config):
    # TODO: import pyang and use yang2dsdl to validate xml against yang model
    # KNOWN ISSUE: it will find the first error and stop.
    if running_config:
        # print(running_config)
        # print(xmltodict.parse(str(running_config)))

        return True

    return False


def parse_slot(slot):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['slots']:
        try:
            logger.debug(key + " " + str(slot[key]))
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in slot " + str(slot))
            logger.error('set valid to False')
            valid = False
    return valid


def parse_shelf(shelf):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['shelves']:
        try:
            logger.debug(key + " " + str(shelf[key]))
            if key == 'slots':
                if isinstance(shelf[key], list):
                    logger.debug("list of slots in shelf " + shelf['shelf-name'])
                    for slot in shelf[key]:
                        if parse_slot(slot) == False:
                            logger.error('set valid to False')
                            valid = False
                else:
                    logger.debug("single slot in shelf " + shelf['shelf-name'])
                    if parse_slot(shelf[key]) == False:
                        logger.error('set valid to False')
                        valid = False
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in shelf " + str(shelf))
            logger.error('set valid to False')
            valid = False
    return valid


def check_shelves(shelves):
    if isinstance(shelves, list):
        logger.debug("list of shelves")
        allValidShelves = True
        for shelf in shelves:
            if parse_shelf(shelf) == False:
                allValidShelves = False
        return allValidShelves
    else:
        logger.debug("shelves has only one shelf")
        return parse_shelf(shelves)


def parse_circuit_pack_category(circuit_pack_category):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['circuit-pack-category']:
        try:
            logger.debug(key + " " + str(circuit_pack_category[key]))
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in circuit-pack-category " + str(circuit_pack_category))
            logger.error('set valid to False')
            valid = False
    return valid


def parse_cp_slot(cp_slot):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['cp-slots']:
        try:
            logger.debug(key + " " + str(cp_slot[key]))
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in cp_slots " + str(cp_slot))
            logger.error('set valid to False')
            valid = False
    return valid


def parse_supported_interface_capability(supported_interface_capability):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['supported-interface-capability']:
        try:
            logger.debug(key + " " + str(supported_interface_capability[key]))
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in supported-interface-capability " + str(supported_interface_capability))
            logger.error('set valid to False')
            valid = False
    return valid


def parse_port_capability(port_capability):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['port-capabilities']:
        try:
            logger.debug(key + " " + str(port_capability[key]))
            if key == 'supported-interface-capability':
                if isinstance(port_capability[key], list):
                    logger.debug("list of supported-interface-capability in port_capability " + str(
                        port_capability['supported-interface-capability']))
                    for supported_interface_capability in port_capability[key]:
                        if parse_supported_interface_capability(supported_interface_capability) == False:
                            logger.error('set valid to False')
                            valid = False
                else:
                    logger.debug("single supported_interface_capability in port-capabilities " + str(
                        port_capability['supported-interface-capability']))
                    if parse_supported_interface_capability(port_capability[key]) == False:
                        logger.error('set valid to False')
                        valid = False
        except Exception:
            logger.warning('Error raised: ', exc_info=True)
            logger.warning(key + " not exists in port-capabilities " + str(port_capability))
            if key in ['supported-interface-capability']:
                continue
            else:
                logger.error('set valid to False')
                valid = False
    return valid


def parse_port(port):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['ports']:
        try:
            logger.debug(key + " " + str(port[key]))
            if key == 'port-capabilities':
                if parse_port_capability(port[key]) == False:
                    logger.error('set valid to False')
                    valid = False

        except Exception:
            logger.warning('Error raised: ', exc_info=True)
            logger.warning(key + " not exists in port " + str(port))
            if key in ['port-capabilities']:
                continue
            else:
                logger.error('set valid to False')
                valid = False
    return valid


def parse_cp(cp):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['cps']:
        try:
            logger.debug(key + " " + str(cp[key]))

            if key == 'circuit-pack-category':
                if parse_circuit_pack_category(cp[key]) == False:
                    logger.error('set valid to False')
                    valid = False
            if key == 'cp-slots':
                if isinstance(cp[key], list):
                    logger.debug("list of cp-slots in cp " + cp['circuit-pack-name'])
                    for cp_slot in cp[key]:
                        if parse_cp_slot(cp_slot) == False:
                            logger.error('set valid to False')
                            valid = False
                else:
                    logger.debug("single cp-slots in cp " + cp['circuit-pack-name'])
                    if parse_cp_slot(cp[key]) == False:
                        logger.error('set valid to False')
                        valid = False
            if key == 'ports':
                if isinstance(cp[key], list):
                    logger.debug("list of ports in cp " + cp['circuit-pack-name'])
                    for port in cp[key]:
                        if parse_port(port) == False:
                            logger.error('set valid to False')
                            valid = False
                else:
                    logger.debug("single port in cp " + cp['circuit-pack-name'])
                    if parse_port(cp[key]) == False:
                        logger.error('set valid to False')
                        valid = False


        except Exception:
            logger.warning('Error raised: ', exc_info=True)
            logger.warning(key + " not exists in cp " + str(cp))
            if key in ['circuit-pack-category', 'cp-slots',
                       'ports']:  # only when the field is mandatory, set valid to False
                continue
            else:
                logger.error('set valid to False')
                valid = False
    return valid


def check_cps(circuitPacks):
    if isinstance(circuitPacks, list):
        logger.debug("list of circuitPacks")
        allValidCps = True
        for cp in circuitPacks:
            if parse_cp(cp) == False:
                allValidCps = False
        return allValidCps
    else:
        logger.debug("circuit-Packs has only one circuit pack")
        return parse_cp(circuitPacks)


# xpdr port in xponder tree
def parse_xpdr_port(xpdr_port):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['xpdr-port']:
        try:
            logger.debug(key + " " + str(xpdr_port[key]))
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in xpdr-port under xponder tree" + str(xpdr_port))
            logger.error('set valid to False')
            valid = False
    return valid


def parse_xponder(xponder):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['xponder']:
        try:
            logger.debug(key + " " + str(xponder[key]))
            if key == 'xpdr-port':
                if isinstance(xponder[key], list):
                    logger.debug("list of xpdr-port in xponder " + str(xponder['xpdr-port']))
                    for xpdr_port in xponder[key]:
                        if parse_xpdr_port(xpdr_port) == False:
                            logger.error('set valid to False')
                            valid = False
                else:
                    logger.debug("single xpdr-port in xponder " + str(xponder['xpdr-port']))
                    if parse_xpdr_port(xponder[key]) == False:
                        logger.error('set valid to False')
                        valid = False

        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in xponder" + str(xponder))
            logger.error('set valid to False')
            valid = False
    return valid


def check_xponder(xponder_tree):
    if isinstance(xponder_tree, list):
        logger.debug("list of xponder_tree")
        allValidXponderTrees = True
        for xponder in xponder_tree:
            if parse_xponder(xponder) == False:
                allValidXponderTrees = False
        return allValidXponderTrees
    else:
        logger.debug("Xponder has only one xponder")
        return parse_xponder(xponder_tree)


def parse_degree(deg):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['degree']:
        try:
            logger.info(key + " " + str(deg[key]))
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in degree" + str(deg))
            logger.error('set valid to False')
            valid = False
    return valid


def check_degree(degree):
    if isinstance(degree, list):
        logger.info("list of degrees: " + str(len(degree)))
        allValidDegree = True
        for deg in degree:
            if parse_degree(deg) == False:
                allValidDegree = False
        return allValidDegree
    else:
        logger.info("degree has only one deg")
        return parse_degree(degree)


def parse_srg(srg):
    valid = True
    for key in CHECKPOINTS[os.getenv('OR_VERSION')]['shared-risk-group']:
        try:
            logger.info(key + " " + str(srg[key]))
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error(key + " not exists in shared-risk-group " + str(srg))
            logger.error('set valid to False')
            valid = False
    return valid


def check_srg(srgs):
    if isinstance(srgs, list):
        logger.info("list of srgs: " + str(len(srgs)))
        allValidSrg = True
        for srg in srgs:
            if parse_srg(srg) == False:
                allValidSrg = False
        return allValidSrg
    else:
        logger.info("srgs has only one srg")
        return parse_srg(srgs)


# check device configuration, container by container
def check_device_get(running_config):
    # Currently put msa version as an environment variable in env file, validation will check the version in the dict
    logger.info("device nodetype: " + os.getenv("NODETYPE"))
    logger.info("device type: " + os.getenv("TYPE"))
    logger.info(str(running_config))
    conf_json = xmltodict.parse(str(running_config))

    validConfig = True

    try:
        shelves = conf_json['rpc-reply']['data']['org-openroadm-device']['shelves']
        if check_shelves(shelves) == False:
            logger.error('shelf valid False')
            validConfig = False
    except Exception:
        logger.error('Error raised: ', exc_info=True)
        logger.error('shelf valid False')
        logger.error('set valid to False')
        validConfig = False

    try:
        circuitPacks = conf_json['rpc-reply']['data']['org-openroadm-device']['circuit-packs']
        if check_cps(circuitPacks) == False:
            logger.error('CP valid False')
            validConfig = False
    except Exception:
        logger.error('Error raised: ', exc_info=True)
        logger.error('CP valid False')
        logger.error('set valid to False')
        validConfig = False

    if os.getenv("TYPE") in ['tpdr', 'mpdr', 'swpdr']:
        try:
            xponder_tree = conf_json['rpc-reply']['data']['org-openroadm-device']['xponder']
            if check_xponder(xponder_tree) == False:
                logger.error('xponder tree valid False')
                validConfig = False
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error('xponder tree valid False')
            logger.error('set valid to False')
            validConfig = False

    if os.getenv("TYPE") == 'rdm':
        # check degree, shared-risk-group
        # TODO: check ots and oms interfaces
        # TODO: check flex grid or fix grid
        try:
            degree = conf_json['rpc-reply']["data"]['org-openroadm-device']['degree']
            if check_degree(degree) == False:
                logger.error('degree valid False')
                validConfig = False
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error('degree valid False')
            logger.error('set valid to False')
            validConfig = False

        try:
            shared_risk_group = conf_json['rpc-reply']["data"]['org-openroadm-device']['shared-risk-group']
            if check_srg(shared_risk_group) == False:
                logger.error('srg valid False')
                validConfig = False
        except Exception:
            logger.error('Error raised: ', exc_info=True)
            logger.error('srg valid False')
            logger.error('set valid to False')
            validConfig = False

    return validConfig


def mount_device(node: str, host: str, port: int, username: str, password: str):
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}',
           'draft-bierman02': '{}/config/network-topology:network-topology/topology/topology-netconf/node/{}'}
    body = {'node': [{
        'node-id': node,
        'netconf-node-topology:username': username,
        'netconf-node-topology:password': password,
        'netconf-node-topology:host': host,
        'netconf-node-topology:port': port,
        'netconf-node-topology:tcp-only': 'false',
        'netconf-node-topology:pass-through': {}}]}
    response = test_utils.put_request(url[test_utils.RESTCONF_VERSION].log_format('{}', node), body)
    if test_utils.wait_until_log_contains(test_utils.TPCE_LOG,
                                          'Triggering notification stream NETCONF for node ' + node, 180):
        logger.info('Node ' + node + ' correctly added to tpce topology')
    else:
        logger.warning('Node ' + node + ' still not added to tpce topology')
        if response.status_code == requests.codes.ok:
            logger.info('It was probably loaded at start-up')
        # TODO an else-clause to abort test would probably be nice here
    return response


def check_mount_device_error(node: str, host: str, port: int, username: str, password: str):
    noError = True
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}',
           'draft-bierman02': '{}/config/network-topology:network-topology/topology/topology-netconf/node/{}'}
    body = {'node': [{
        'node-id': node,
        'netconf-node-topology:username': username,
        'netconf-node-topology:password': password,
        'netconf-node-topology:host': host,
        'netconf-node-topology:port': port,
        'netconf-node-topology:tcp-only': 'false',
        'netconf-node-topology:pass-through': {}}]}
    response = test_utils.put_request(url[test_utils.RESTCONF_VERSION].log_format('{}', node), body)
    if test_utils.wait_until_log_contains(test_utils.TPCE_LOG, 'Error', 10):
        logger.error('Error found!!!')
        noError = False
    else:
        logger.info('no error found')
        if response.status_code == requests.codes.ok:
            logger.info('It was probably loaded at start-up')
    return noError, response


def search_log_backward(log_file, regexp):
    expFound = False
    with open(log_file, 'r', encoding='utf-8') as file:
        last_bit = file.readlines()[-10:]
    for line in last_bit:
        if re.search(regexp, line):
            expFound = True
            break

    return expFound


def check_notification_subscription(node: str):
    openroadmNotificationSub = "OPENROADM stream not subscribed"
    netconfNotificationSub = "NETCONF stream not subscribed"
    if search_log_backward(test_utils.TPCE_LOG, 'OPENROADM subscription is true'):
        openroadmNotificationSub = "OPENROADM stream subscribed"
        logger.info('TPCE successfully subscribes to node ' + node + ' through OPENROADM stream')
        return openroadmNotificationSub
    else:
        logger.warning('TPCE failed to subscribe to node ' + node + ' through OPENROADM stream')

    if search_log_backward(test_utils.TPCE_LOG, 'NETCONF subscription is true'):
        netconfNotificationSub = "NETCONF stream subscribed"
        logger.info('TPCE successfully subscribes to node ' + node + ' through NETCONF stream')
        return netconfNotificationSub
    else:
        logger.warning('TPCE failed to subscribe to node ' + node + ' through NETCONF stream')

    # both not found
    return False


def get_portmapping_bylcp(mapping_list, logicalConnectionPoint: str):
    for port in mapping_list:
        if port['logical-connection-point'] == logicalConnectionPoint:
            return port

    logger.error(logicalConnectionPoint + " not found!")

    return


# parse portmapping and generate port list config for interface rendering
def parsePortmapping(
        portmapping_node):  # check network and degree ports first, and then append client and srg ports in corresponding network or degree ports.
    mapping_list = portmapping_node['mapping']
    logger.info(mapping_list)

    # if the node has mpdr or switchponder, it will have switching pool
    try:
        switching_pool_list = portmapping_node['switching-pool-lcp']
        logger.info(switching_pool_list)
    except Exception as e:
        switching_pool_list = []
        logger.warning('Error raised: ', exc_info=True)

    if not isinstance(mapping_list, list):
        logger.error('node portmapping is not a list')
        return False

    # TODO: need to consider blocking list and non-blocking list
    config = {}
    # parse all network ports first
    # TODO: in the future, node type could be hybrid, xponder ports and rdm ports mixed
    for port in mapping_list:
        try:
            port_type = port['xpdr-type']
            port_qual = port['port-qual']
        except Exception as e:
            logger.warning("not an xponder type port or error in port: " + str(port))
            logger.warning('Error raised: ', exc_info=True)

            continue

        if port_type == 'tpdr' or port_type == 'mpdr' or port_type == 'switch':
            if port_qual == 'xpdr-network' or port_qual == 'switch-network':
                logger.debug("find a network type port " + port['logical-connection-point'])
                config[port['logical-connection-point']] = {
                    "logical-connection-point": port['logical-connection-point'],
                    "xpdr-type": port["xpdr-type"],
                    "port-qual": port["port-qual"],
                    "supported-interface-capability": port["supported-interface-capability"],
                    "linked-client-ports": [],
                    "interconnect-bandwidth-unit": 0
                }
        # TODO: regen type in the future
        else:
            logger.warning("port type not supported")

    # parse all DEG ports first
    # TODO: in the future, node type could be hybrid, xponder ports and rdm ports mixed
    logger.debug("Now check if there are deg ports")
    for port in mapping_list:
        try:
            port_type = port['xpdr-type']
            port_qual = port['port-qual']
            logger.debug("an xponder type port, skipping")
            continue
        except Exception as e:
            logger.debug("not an xponder type port : " + str(port))

        if port['logical-connection-point'][0:3] == "DEG":
            logger.debug("DEG type port: " + port['logical-connection-point'])

            config[port['logical-connection-point']] = {
                "logical-connection-point": port['logical-connection-point'],
                "roadm-type": "rdm",
                "port-oper-state": port['port-oper-state'],
                "port-admin-state": port['port-admin-state'],
                "supporting-port": port['supporting-port'],
                "supporting-circuit-pack-name": port['supporting-circuit-pack-name'],
                "port-direction": port['port-direction'],
                "linked-srg-ports": []
            }  # TODO: roadm-type here could in the future be cd or cdc roadm. it should match env file TYPE variable
        else:
            logger.warning("port type not supported")

    # parse 
    # 1) all tpdr client ports, it is one to one mapping, and put it config list under its coresponding network port
    # 2) or srg port

    # (1) check tpdr client ports
    for port in mapping_list:
        try:
            port_type = port['xpdr-type']
            port_qual = port['port-qual']
        except Exception as e:
            logger.warning("error in port: " + str(port))
            logger.warning('Error raised: ', exc_info=True)
            continue

        if port_type == 'tpdr':
            if port_qual == 'xpdr-client':
                # TODO: when missing internal-link in tpdr configuration, portmapping will not show connection-map-lcp
                try:
                    config[port['connection-map-lcp']]["linked-client-ports"].append(
                        {
                            "logical-connection-point": port['logical-connection-point'],
                            "port-qual": port["port-qual"],
                            "supported-interface-capability": port["supported-interface-capability"]
                        })
                except Exception as e:
                    logger.error("error in port-mapping: " + port['logical-connection-point'])
                    logger.error('Error raised: ', exc_info=True)

        else:
            logger.debug("not a tpdr type of port")

    # (2) check srg ports
    for port in mapping_list:
        logger.debug("Finding SRG ports...")
        try:
            port_type = port['xpdr-type']
            port_qual = port['port-qual']
            logger.debug("an xponder type port, skipping")
            continue
        except Exception as e:
            logger.debug("not an xponder type port : " + str(port))

        if port['logical-connection-point'][0:3] == "SRG":
            logger.debug("SRG type port: " + port['logical-connection-point'])

            for port_name, p in config.items():
                try:
                    p["roadm-type"]
                except Exception as e:
                    logger.debug("not a degree type port : " + str(port_name))
                    continue

                # otherwise it is rdm/deg port
                config[p['logical-connection-point']]["linked-srg-ports"].append({
                    "logical-connection-point": port['logical-connection-point'],
                    "roadm-type": "rdm",
                    "port-oper-state": port['port-oper-state'],
                    "port-admin-state": port['port-admin-state'],
                    "supporting-port": port['supporting-port'],
                    "supporting-circuit-pack-name": port['supporting-circuit-pack-name'],
                    "port-direction": port['port-direction'],
                })
        else:
            logger.warning("not srg port we are looking for...")

    # for mpdr or switchponder
    for switchingPool in switching_pool_list:
        # warning: currently in tpce portmapping, if it is a blocking list, in each non-blocking-list, there is one and only pair of network and client port.
        #          not sure if it is possible that, in a blocking list, there are multiple non-blocking-list s, each has multiple ports. But this doesn't make sense, because
        #          in this case, it should be seperated XPDR.
        #          If the switching type is non-blocking, there should be only one group of non-blocking-list.
        logger.info(
            "switching pool number: " + str(switchingPool["switching-pool-number"]) + "; switching pool type: " +
            switchingPool["switching-pool-type"])
        if switchingPool['switching-pool-type'] == 'blocking':
            for nbl in switchingPool['non-blocking-list']:
                for port in nbl['lcp-list']:  # should be only two elements
                    if port in config.keys():
                        network_port = port
                        config[network_port]['interconnect-bandwidth-unit'] = nbl['interconnect-bandwidth-unit']
                        try:  # if client_port shows first, client_port will be defined. For every pair, it will be deleted after each found.
                            client_port
                        except:
                            continue

                        client_port_mapping = get_portmapping_bylcp(mapping_list=mapping_list,
                                                                    logicalConnectionPoint=client_port)

                        config[network_port]['linked-client-ports'].append(
                            {
                                "logical-connection-point": client_port,
                                "port-qual": client_port_mapping['port-qual'],
                                "supported-interface-capability": client_port_mapping['supported-interface-capability']
                            }
                        )

                        del network_port, client_port
                    else:
                        client_port = port
                        try:
                            network_port
                        except:
                            continue

                        client_port_mapping = get_portmapping_bylcp(mapping_list=mapping_list,
                                                                    logicalConnectionPoint=client_port)

                        config[network_port]['linked-client-ports'].append(
                            {
                                "logical-connection-point": client_port,
                                "port-qual": client_port_mapping['port-qual'],
                                "supported-interface-capability": client_port_mapping['supported-interface-capability']
                            }
                        )

                        del network_port, client_port

        elif switchingPool['switching-pool-type'] == 'non-blocking':
            # non-blocking switching-pool-type, swpdr
            logger.info("non-blocking type of switching pool, number of non-blocking-list" + str(
                len(switchingPool['non-blocking-list'])))
            if len(switchingPool[
                       'non-blocking-list']) == 1:  # non-blocking type of switching pool type should have only one non-blocking-list

                for port in switchingPool['non-blocking-list'][0]['lcp-list']:
                    print("now parsing lcp-list port: ", port)
                    if port in config.keys():  # it is a network port, it's already in the config list
                        network_port = port
                        config[network_port]['interconnect-bandwidth-unit'] = switchingPool['non-blocking-list'][0][
                            'interconnect-bandwidth-unit']
                        for other_port in switchingPool['non-blocking-list'][0]['lcp-list']:
                            if other_port not in config.keys():  # means it is a client port
                                client_port = other_port
                                client_port_mapping = get_portmapping_bylcp(mapping_list=mapping_list,
                                                                            logicalConnectionPoint=client_port)

                                config[network_port]['linked-client-ports'].append(
                                    {
                                        "logical-connection-point": client_port,
                                        "port-qual": client_port_mapping['port-qual'],
                                        "supported-interface-capability": client_port_mapping[
                                            'supported-interface-capability']
                                    }
                                )

            else:
                logger.error("Error! should be only one non-blocking-list")

        else:
            pass
    # TODO, check if all fields are filled, for example, all linked-client-ports have at least one element, currently for xpdr, interface_renderer test case will check empty client list
    logger.info(config)
    return config


# example of port config
'''
{
    "XPDR1-NETWORK1": {
        "interconnect-bandwidth-unit": 1000000000,
        "linked-client-ports": [
            {
                "logical-connection-point": "XPDR1-CLIENT1",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR1-CLIENT4",
                "port-qual": "xpdr-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-10GE-ODU2",
                    "org-openroadm-port-types:if-10GE-ODU2e",
                    "org-openroadm-port-types:if-10GE"
                ]
            },
            {
                "logical-connection-point": "XPDR1-CLIENT3",
                "port-qual": "xpdr-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-10GE-ODU2",
                    "org-openroadm-port-types:if-10GE-ODU2e",
                    "org-openroadm-port-types:if-10GE"
                ]
            },
            {
                "logical-connection-point": "XPDR1-CLIENT2",
                "port-qual": "xpdr-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-10GE-ODU2",
                    "org-openroadm-port-types:if-10GE-ODU2e"
                ]
            }
        ],
        "logical-connection-point": "XPDR1-NETWORK1",
        "port-qual": "xpdr-network",
        "supported-interface-capability": [
            "org-openroadm-port-types:if-OCH-OTU4-ODU4"
        ],
        "xpdr-type": "mpdr"
    },
    "XPDR2-NETWORK1": {
        "interconnect-bandwidth-unit": 1000000000,
        "linked-client-ports": [
            {
                "logical-connection-point": "XPDR2-CLIENT1",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT3",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT2",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT4",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            }
        ],
        "logical-connection-point": "XPDR2-NETWORK1",
        "port-qual": "switch-network",
        "supported-interface-capability": [
            "org-openroadm-port-types:if-OCH-OTU4-ODU4"
        ],
        "xpdr-type": "switch"
    },
    "XPDR2-NETWORK2": {
        "interconnect-bandwidth-unit": 1000000000,
        "linked-client-ports": [
            {
                "logical-connection-point": "XPDR2-CLIENT1",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT3",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT2",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT4",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            }
        ],
        "logical-connection-point": "XPDR2-NETWORK2",
        "port-qual": "switch-network",
        "supported-interface-capability": [
            "org-openroadm-port-types:if-OCH-OTU4-ODU4"
        ],
        "xpdr-type": "switch"
    },
    "XPDR2-NETWORK3": {
        "interconnect-bandwidth-unit": 1000000000,
        "linked-client-ports": [
            {
                "logical-connection-point": "XPDR2-CLIENT1",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT3",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT2",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT4",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            }
        ],
        "logical-connection-point": "XPDR2-NETWORK3",
        "port-qual": "switch-network",
        "supported-interface-capability": [
            "org-openroadm-port-types:if-OCH-OTU4-ODU4"
        ],
        "xpdr-type": "switch"
    },
    "XPDR2-NETWORK4": {
        "interconnect-bandwidth-unit": 1000000000,
        "linked-client-ports": [
            {
                "logical-connection-point": "XPDR2-CLIENT1",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT3",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT2",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            },
            {
                "logical-connection-point": "XPDR2-CLIENT4",
                "port-qual": "switch-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-100GE-ODU4",
                    "org-openroadm-port-types:if-100GE"
                ]
            }
        ],
        "logical-connection-point": "XPDR2-NETWORK4",
        "port-qual": "switch-network",
        "supported-interface-capability": [
            "org-openroadm-port-types:if-OCH-OTU4-ODU4"
        ],
        "xpdr-type": "switch"
    },
    "XPDR3-NETWORK1": {
        "interconnect-bandwidth-unit": 1000000000,
        "linked-client-ports": [
            {
                "logical-connection-point": "XPDR3-CLIENT3",
                "port-qual": "xpdr-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-1GE-ODU0"
                ]
            },
            {
                "logical-connection-point": "XPDR3-CLIENT2",
                "port-qual": "xpdr-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-1GE-ODU0"
                ]
            },
            {
                "logical-connection-point": "XPDR3-CLIENT1",
                "port-qual": "xpdr-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-1GE-ODU0"
                ]
            },
            {
                "logical-connection-point": "XPDR3-CLIENT4",
                "port-qual": "xpdr-client",
                "supported-interface-capability": [
                    "org-openroadm-port-types:if-1GE-ODU0"
                ]
            }
        ],
        "logical-connection-point": "XPDR3-NETWORK1",
        "port-qual": "xpdr-network",
        "supported-interface-capability": [
            "org-openroadm-port-types:if-OCH-OTU4-ODU4"
        ],
        "xpdr-type": "mpdr"
    }
}
'''
'''
{
    "DEG1-TTP-TXRX": {
        "linked-srg-ports": [
            {
                "logical-connection-point": "SRG1-PP2-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C2"
            },
            {
                "logical-connection-point": "SRG1-PP4-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C4"
            },
            {
                "logical-connection-point": "SRG3-PP4-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C4"
            },
            {
                "logical-connection-point": "SRG3-PP3-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C3"
            },
            {
                "logical-connection-point": "SRG1-PP3-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C3"
            },
            {
                "logical-connection-point": "SRG3-PP2-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C2"
            },
            {
                "logical-connection-point": "SRG1-PP1-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C1"
            },
            {
                "logical-connection-point": "SRG3-PP1-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C1"
            }
        ],
        "logical-connection-point": "DEG1-TTP-TXRX",
        "port-admin-state": "InService",
        "port-direction": "bidirectional",
        "port-oper-state": "InService",
        "roadm-type": "rdm",
        "supporting-circuit-pack-name": "1/0",
        "supporting-port": "L1"
    },
    "DEG2-TTP-TXRX": {
        "linked-srg-ports": [
            {
                "logical-connection-point": "SRG1-PP2-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C2"
            },
            {
                "logical-connection-point": "SRG1-PP4-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C4"
            },
            {
                "logical-connection-point": "SRG3-PP4-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C4"
            },
            {
                "logical-connection-point": "SRG3-PP3-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C3"
            },
            {
                "logical-connection-point": "SRG1-PP3-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C3"
            },
            {
                "logical-connection-point": "SRG3-PP2-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C2"
            },
            {
                "logical-connection-point": "SRG1-PP1-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "3/0",
                "supporting-port": "C1"
            },
            {
                "logical-connection-point": "SRG3-PP1-TXRX",
                "port-admin-state": "InService",
                "port-direction": "bidirectional",
                "port-oper-state": "InService",
                "roadm-type": "rdm",
                "supporting-circuit-pack-name": "5/0",
                "supporting-port": "C1"
            }
        ],
        "logical-connection-point": "DEG2-TTP-TXRX",
        "port-admin-state": "InService",
        "port-direction": "bidirectional",
        "port-oper-state": "InService",
        "roadm-type": "rdm",
        "supporting-circuit-pack-name": "2/0",
        "supporting-port": "L1"
    }
}'''


def is_subset2(a: dict, b: dict):
    for k, v in a.items():
        # print(k, v)
        if k in b:
            if isinstance(v, dict) and isinstance(b[k], dict):
                # print(v,b[k])
                if not is_subset2(v, b[k]):
                    # print(v,b[k])
                    return False
            elif isinstance(v, list) and isinstance(b[k], list):
                # print(v,b[k])
                if not sorted(v) == sorted(b[k]):
                    # print(v,b[k])
                    return False
            else:
                if not v == b[k]:
                    # print(v,b[k])
                    return False
        else:
            # print(k,v)
            return False
    return True


def interface_test(device_port_mapping: dict):  # search "set False"
    createCheck, configCheck, pmCheck, alarmCheck, deleteCheck = True, True, True, True, True

    for key, value in device_port_mapping.items():  # key: port name in port mapping, it could be network port or degree port
        logger.info("now check " + key)

        # use this to temporary skip the in use port(s)
        if key == "XPDR2-NETWORK1":
            continue

        try:
            type = value['xpdr-type']
            logger.info('an xponder type port')
        except:
            logger.info('not an xponder type')

        try:
            type = value['roadm-type']
            logger.info('a ROADM type port')
        except:
            logger.info('not a ROADM type')

        if type == 'tpdr':  # tpdr type
            logger.debug("this is a tpdr type port")
            # check network and client interfaces capability and make sure they match

            if len(value[
                       "linked-client-ports"]) == 0:  # if there is no client port under this network port, it is wrong
                logger.error("empty client ports in port " + key)
                logger.error("set False createCheck")
                createCheck = False
                continue
            #################################### Case 1: 400G tpdr ######################################################################
            if 'org-openroadm-port-types:if-otsi-otsigroup' in value['supported-interface-capability'] \
                    and ("org-openroadm-port-types:if-400GE" in value['linked-client-ports'][0][
                'supported-interface-capability']):

                ################################ STEP 1: interface rendering and check rpc response output
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_400GE',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qam16',
                        'operation': 'create',
                        'nodes': [{'node-id': os.getenv('NODEID'),
                                   'src-tp': value['linked-client-ports'][0]['logical-connection-point'],
                                   'dest-tp': key}],
                        'center-freq': 196.08125,
                        'nmc-width': 75,
                        'min-freq': 196.0375,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 755,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug(response)
                # check a) create rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Interfaces created successfully for nodes: ' in \
                        response['output']['result']:
                    expected_subset_response = {
                        'node-id': os.getenv('NODEID'),
                        'otu-interface-id': [key + '-OTUC4'],
                        "eth-interface-id": [value['linked-client-ports'][0]['logical-connection-point'] + '-ETHERNET']}
                    expected_sorted_odu_list = [key + '-ODUC4',
                                                key + '-ODUFLEX']
                    expected_sorted_och_list = [key + '-755:768',
                                                key + '-OTSIGROUP-400G']
                    subset = {k: v for k, v in response['output']['node-interface'][0].items() if
                              k in expected_subset_response}
                    if subset == expected_subset_response and \
                            sorted(response['output']['node-interface'][0]['odu-interface-id']) == sorted(
                        expected_sorted_odu_list) and \
                            sorted(response['output']['node-interface'][0]['och-interface-id']) == sorted(
                        expected_sorted_och_list):

                        logger.debug(key + ' interface rendering output matches expected')
                    else:
                        logger.error("set False createCheck")
                        createCheck = False
                        logger.error(key + ' and ' + value['linked-client-ports'][0][
                            'logical-connection-point'] + ' interface rendering output does not match expected')

                    del response
                    pass
                else:
                    logger.error("set False createCheck")
                    logger.error(key + ' and ' + value['linked-client-ports'][0][
                        'logical-connection-point'] + ' interface rendering response code or output result does not match expected')
                    createCheck = False
                    del response

                ################################ STEP 2: check interface config here
                # 2.1 otsi interface
                response = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface", key + "-755:768")
                logger.debug(response['interface'])
                if response['status_code'] == requests.codes.ok:
                    input_dict_1 = {
                        'name': key + '-755:768',
                        'administrative-state': 'inService',
                        'type': 'org-openroadm-interfaces:otsi',
                        'org-openroadm-optical-tributary-signal-interfaces:otsi': {
                            "frequency": "196.08125",
                            "otsi-rate": "org-openroadm-common-optical-channel-types:R400G-otsi",
                            "fec": "org-openroadm-common-types:ofec",
                            "transmit-power": "-5.0",
                            "provision-mode": "explicit",
                            "modulation-log_format": "dp-qam16",
                            "flexo": {
                                "foic-type": "org-openroadm-common-optical-channel-types:foic4.8",
                                "iid": [1, 2, 3, 4]
                            }
                        }
                    }

                    # TODO: not sure which is better, compare to the method in test02_otn_renderer
                    if not is_subset2(input_dict_1, response['interface'][0]):
                        logger.error("set False configCheck")
                        logger.error(key + "-755:768 configuration wrong")
                        configCheck = False
                    else:
                        logger.debug(key + "-755:768 configuration matches")
                else:
                    logger.error("set False configCheck")
                    logger.error(key + "-755:768 try to get configuration wrong")
                    configCheck = False
                del response, input_dict_1

                # 2.2 otsi-group interface
                response = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                   key + "-OTSIGROUP-400G")
                logger.debug(response['interface'])
                if response['status_code'] == requests.codes.ok:
                    input_dict_1 = {
                        'name': key + '-OTSIGROUP-400G',
                        'administrative-state': 'inService',
                        'supporting-interface-list': [key + '-755:768'],
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'org-openroadm-otsi-group-interfaces:otsi-group': {
                            "group-id": 1,
                            "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"
                        }
                    }
                    if not is_subset2(input_dict_1, response['interface'][0]):
                        logger.error("set False configCheck")
                        logger.error(key + "-OTSIGROUP-400G configuration does not match")
                        configCheck = False
                    else:
                        logger.debug(key + "-OTSIGROUP-400G configuration matches")
                else:
                    logger.error("set False configCheck")
                    logger.error(key + "-OTSIGROUP-400G try to get configuration wrong")
                    configCheck = False
                del response, input_dict_1

                # 2.3 otuc4 interface
                response = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface", key + "-OTUC4")
                logger.debug(response['interface'])
                if response['status_code'] == requests.codes.ok:
                    input_dict_1 = {
                        'name': key + '-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-interface-list': [key + '-OTSIGROUP-400G'],
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'org-openroadm-otn-otu-interfaces:otu': {
                            "rate": "org-openroadm-otn-common-types:OTUCn",
                            "degthr-percentage": 100,
                            "tim-detect-mode": "Disabled",
                            "tim-act-enabled": False,
                            "otucn-n-rate": 4,
                            "degm-intervals": 2
                        }
                    }
                    if not is_subset2(input_dict_1, response['interface'][0]):
                        logger.error("set False configCheck")
                        logger.error(key + "-OTUC4 configuration does not match")
                        configCheck = False
                    else:
                        logger.debug(key + "-OTUC4 configuration matches")
                else:
                    logger.error("set False configCheck")
                    logger.error(key + "-OTUC4 try to get configuration wrong")
                    configCheck = False
                del response, input_dict_1

                # 2.4 oduc4 interface
                response = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface", key + "-ODUC4")
                logger.debug(response['interface'])
                if response['status_code'] == requests.codes.ok:
                    input_dict_1 = {
                        'name': key + '-ODUC4',
                        'administrative-state': 'inService',
                        'supporting-interface-list': [key + '-OTUC4'],
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'org-openroadm-otn-odu-interfaces:odu': {
                            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                            'tim-detect-mode': 'Disabled',
                            'rate': 'org-openroadm-otn-common-types:ODUCn',
                            "degm-intervals": 2,
                            "degthr-percentage": 100,
                            "monitoring-mode": "terminated",
                            "oducn-n-rate": 4,
                            'opu': {
                                'payload-type': '22',
                                'exp-payload-type': '22'
                            }
                        }
                    }
                    if not is_subset2(input_dict_1, response['interface'][0]):
                        logger.error("set False configCheck")
                        logger.error(key + "-ODUC4 configuration does not match")
                        configCheck = False
                    else:
                        logger.debug(key + "-ODUC4 configuration matches")
                else:
                    logger.error("set False configCheck")
                    logger.error(key + "-OTUC4 try to get configuration wrong")
                    configCheck = False
                del response, input_dict_1

                # 2.5 ODUFLEX interface
                response = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface", key + "-ODUFLEX")
                logger.debug(response['interface'])
                if response['status_code'] == requests.codes.ok:
                    input_dict_1 = {
                        'name': key + '-ODUFLEX',
                        'administrative-state': 'inService',
                        'supporting-interface-list': [key + '-ODUC4'],
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'org-openroadm-otn-odu-interfaces:odu': {
                            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
                            'tim-detect-mode': 'Disabled',
                            'rate': 'org-openroadm-otn-common-types:ODUflex-cbr',
                            'oduflex-cbr-service': 'org-openroadm-otn-common-types:ODUflex-cbr-400G',
                            "degm-intervals": 2,
                            "degthr-percentage": 100,
                            "monitoring-mode": "terminated",
                            'opu': {
                                'payload-type': '32',
                                'exp-payload-type': '32'
                            },
                            'parent-odu-allocation': {
                                'opucn-trib-slots': parent_odu_allocation['400'],
                                'trib-port-number': 1
                            }
                        }
                    }
                    if not is_subset2(input_dict_1, response['interface'][0]):
                        logger.error("set False configCheck")
                        logger.error(key + "-ODUFLEX configuration does not match")
                        configCheck = False
                    else:
                        logger.debug(key + "-ODUFLEX configuration matches")
                else:
                    logger.error("set False configCheck")
                    logger.error(key + "-ODUFLEX try to get configuration wrong")
                    configCheck = False
                del response, input_dict_1

                # 2.6 ethernet interface
                response = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                   value['linked-client-ports'][0][
                                                                       'logical-connection-point'] + "-ETHERNET")
                logger.debug(response['interface'])
                if response['status_code'] == requests.codes.ok:
                    input_dict_1 = {
                        'name': value['linked-client-ports'][0]['logical-connection-point'] + "-ETHERNET",
                        'administrative-state': 'inService',
                        'type': 'org-openroadm-interfaces:ethernetCsmacd',
                        'org-openroadm-ethernet-interfaces:ethernet': {
                            'fec': 'org-openroadm-common-types:rsfec',
                            # 'auto-negotiation': 'enabled',
                            'speed': 400000,
                            # 'duplex': 'full'
                        }
                    }
                    if not is_subset2(input_dict_1, response['interface'][0]):
                        logger.error("set False configCheck")
                        logger.error(value['linked-client-ports'][0][
                                         'logical-connection-point'] + "-ETHERNET configuration does not match")
                        configCheck = False
                    else:
                        logger.debug(value['linked-client-ports'][0][
                                         'logical-connection-point'] + "-ETHERNET configuration matches")
                else:
                    logger.error("set False configCheck")
                    logger.error(value['linked-client-ports'][0][
                                     'logical-connection-point'] + "-ETHERNET try to get configuration wrong")
                    configCheck = False
                del response, input_dict_1

                ################################ Step 3: check pm here
                # create pm only for honeynode
                # create_pm_honeynode(host=os.getenv('HOST'),port=os.getenv('PORT'), interfaceName="'"+ key +"-755:768'")

                conn = make_netconf_connection(host=os.getenv('HOST'), port=os.getenv('PORT'),
                                               username=os.getenv('USERNAME'), password=os.getenv('PASSWORD'),
                                               timeout=30)
                if conn:
                    # TODO: add pm to check here
                    try:
                        RPC_PM_Interface(conn, "'" + key + "-755:768'", P_side='opticalPowerOutput')
                        logger.debug("Get PM")
                    except:
                        logger.error('Error raised: ', exc_info=True)
                        logger.error("set False pmCheck")
                        pmCheck = False
                else:
                    logger.error("set False pmCheck")
                    pmCheck = False
                disconnect_netconf(conn)
                # delete pm only for honeynode
                # delete_pm_honeynode(host=os.getenv('HOST'),port=os.getenv('PORT'), interfaceName="'"+ key +"-755:768'")

                ################################ Step 4: check alarm here
                conn = make_netconf_connection(host=os.getenv('HOST'), port=os.getenv('PORT'),
                                               username=os.getenv('USERNAME'), password=os.getenv('PASSWORD'),
                                               timeout=200)
                if conn:
                    # TODO: add alarm to check here
                    try:
                        activeAlarms = parse_Alarm(get_all_Alarm(conn).xml)
                        if check_alarm_existance(activeAlarms, "interface", key + "-755:768", ''):
                            logger.debug("Alarm found for " + key + "-755:768")
                            del activeAlarms
                        else:
                            logger.error("Alarm not found for " + key + "-755:768")
                            logger.error("set False alarmCheck")
                            alarmCheck = False
                    except:
                        logger.warning('Error raised: ', exc_info=True)
                        logger.error("set False alarmCheck")
                        alarmCheck = False

                else:
                    logger.error("failed to get Alarm")
                    logger.error("set False alarmCheck")
                    alarmCheck = False
                disconnect_netconf(conn)

                ################################ Step 5: delete interface and check the existance of the interfaces
                time.sleep(5)
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_OTUC4',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qam16',
                        'operation': 'delete',
                        'nodes': [{'node-id': os.getenv('NODEID'),
                                   'src-tp': value['linked-client-ports'][0]['logical-connection-point'],
                                   'dest-tp': key}],
                        'center-freq': 196.1,
                        'nmc-width': 75,
                        'min-freq': 196.0375,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 755,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug("Deleting interfaces ...")
                logger.debug(response)
                time.sleep(2)
                # check a) delete rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Request processed' in response['output']['result']:
                    response_oduc4 = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                             key + "-ODUC4")
                    if response_oduc4['status_code'] not in [requests.codes.conflict,
                                                             requests.codes.service_unavailable]:
                        logger.error(key + "-ODUC4 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_oduc4

                    response_oduflex = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                               key + "-ODUFLEX")
                    if response_oduflex['status_code'] not in [requests.codes.conflict,
                                                               requests.codes.service_unavailable]:
                        logger.error(key + "-ODUFLEX still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_oduflex

                    response_eth = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                           value['linked-client-ports'][0][
                                                                               'logical-connection-point'] + "-ETHERNET")
                    if response_eth['status_code'] not in [requests.codes.conflict, requests.codes.service_unavailable]:
                        logger.error(
                            value['linked-client-ports'][0]['logical-connection-point'] + "-ETHERNET still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_eth

                    response_otuc4 = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                             key + "-OTUC4")
                    if response_otuc4['status_code'] not in [requests.codes.conflict,
                                                             requests.codes.service_unavailable]:
                        logger.error(key + "-OTUC4 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_otuc4

                    response_otsig = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                             key + "-OTSIGROUP-400G")
                    if response_otsig['status_code'] not in [requests.codes.conflict,
                                                             requests.codes.service_unavailable]:
                        logger.error(key + "-OTSIGROUP-400G still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_otsig

                    response_otsi = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                            key + "-755:768")
                    if response_otsi['status_code'] not in [requests.codes.conflict,
                                                            requests.codes.service_unavailable]:
                        logger.error(key + "-755:768 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_otsi

                else:
                    logger.error("delete response code wrong or output result not match expected")
                    logger.error("set False deleteCheck")
                    deleteCheck = False
                del response

            #################################### Case 2: 100G tpdr ######################################################################
            elif "org-openroadm-port-types:if-OCH-OTU4-ODU4" in value['supported-interface-capability'] \
                    and not set(client_100G_capabilities).isdisjoint(
                value["linked-client-ports"][0]["supported-interface-capability"]):
                ################################ STEP 1: interface rendering and check rpc response output
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_100G',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qpsk',
                        'operation': 'create',
                        'nodes': [{'node-id': os.getenv('NODEID'),
                                   'src-tp': value['linked-client-ports'][0]['logical-connection-point'],
                                   'dest-tp': key}],
                        'center-freq': 196.1,
                        'nmc-width': 40,
                        'min-freq': 196.075,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 761,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug(response)
                # check a) create rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Interfaces created successfully for nodes: ' in \
                        response['output']['result']:
                    expected_subset_response = {
                        'node-id': os.getenv('NODEID'),
                        'otu-interface-id': [key + '-OTU'],
                        "odu-interface-id": [key + "-ODU4"],
                        "och-interface-id": [key + "-761:768"],
                        "eth-interface-id": [value['linked-client-ports'][0]['logical-connection-point'] + '-ETHERNET']
                    }

                    subset = {k: v for k, v in response['output']['node-interface'][0].items() if
                              k in expected_subset_response}
                    if sorted(subset) == sorted(expected_subset_response):
                        logger.debug(key + ' interface rendering output matches expected')
                    else:
                        logger.error("set False createCheck")
                        createCheck = False
                        logger.error(key + ' interface rendering output does not match expected')
                else:
                    logger.error("set False createCheck")
                    createCheck = False

                del response

                # TODO: ################################ STEP 2: check interface config here
                # add code here

                # Step 3: check pm here
                # create pm only for honeynode
                # create_pm_honeynode(host=os.getenv('HOST'),port=os.getenv('PORT'), interfaceName="'"+ key +"-755:768'")

                conn = make_netconf_connection(host=os.getenv('HOST'), port=os.getenv('PORT'),
                                               username=os.getenv('USERNAME'), password=os.getenv('PASSWORD'),
                                               timeout=200)
                if conn:
                    # TODO: add pm to check here, temp check for fujitsu 100G
                    # if RPC_PM_Interface(conn, "'"+ key +"-761:768'", P_side ='opticalPowerOutput'):
                    try:
                        RPC_PM_Port(conn, "'1/0/2/E1'", "'1'",
                                    P_side='opticalPowerOutput')  # 1/0/2/E1 for fujitsu tpdr 100G network port 2
                        logger.debug("Get PM")
                    except:
                        logger.error('Error raised: ', exc_info=True)
                        logger.error("set False pmCheck")
                        pmCheck = False
                else:
                    logger.error("Connection failed")
                    logger.error("set False pmCheck")
                    pmCheck = False
                disconnect_netconf(conn)
                # delete pm only for honeynode
                # delete_pm_honeynode(host=os.getenv('HOST'),port=os.getenv('PORT'), interfaceName="'"+ key +"-755:768'")

                # TODO: Step 4: check alarm here

                conn = make_netconf_connection(host=os.getenv('HOST'), port=os.getenv('PORT'),
                                               username=os.getenv('USERNAME'), password=os.getenv('PASSWORD'),
                                               timeout=200)
                if conn:
                    # TODO: add alarm to check here, temp check for fujitsu 100G
                    try:
                        activeAlarms = parse_Alarm(get_all_Alarm(conn).xml)
                        if check_alarm_existance(activeAlarms, "interface", key + "-761:768", ''):
                            logger.debug("Alarm found for " + key + "-761:768")
                        else:
                            logger.error("Alarm not found for " + key + "-761:768")
                            logger.error("set False alarmCheck")
                            alarmCheck = False
                    except:
                        logger.error('Error raised: ', exc_info=True)
                        logger.error("set False alarmCheck")
                        alarmCheck = False
                else:
                    logger.error("failed to get Alarm")
                    logger.error("set False alarmCheck")
                    alarmCheck = False
                disconnect_netconf(conn)

                # Step 5: delete interface and check the existance of the interfaces
                time.sleep(30)
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_100G',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qpsk',
                        'operation': 'delete',
                        'nodes': [{'node-id': os.getenv('NODEID'),
                                   'src-tp': value['linked-client-ports'][0]['logical-connection-point'],
                                   'dest-tp': key}],
                        'center-freq': 196.1,
                        'nmc-width': 40,
                        'min-freq': 196.075,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 761,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug("Deleting interfaces ...")
                logger.debug(response)
                time.sleep(30)
                # check a) delete rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Request processed' in response['output']['result']:
                    response_otu = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                           key + "-OTU")
                    if response_otu['status_code'] not in [requests.codes.conflict, requests.codes.service_unavailable]:
                        logger.error(key + "-OTU still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_otu

                    response_odu4 = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                            key + "-ODU4")
                    if response_odu4['status_code'] not in [requests.codes.conflict,
                                                            requests.codes.service_unavailable]:
                        logger.error(key + "-ODU4 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_odu4

                    response_eth = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                           value['linked-client-ports'][0][
                                                                               'logical-connection-point'] + "-ETHERNET")
                    if response_eth['status_code'] not in [requests.codes.conflict, requests.codes.service_unavailable]:
                        logger.error(
                            value['linked-client-ports'][0]['logical-connection-point'] + "-ETHERNET still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_eth

                    response_och = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                           key + "-761:768")
                    if response_och['status_code'] not in [requests.codes.conflict, requests.codes.service_unavailable]:
                        logger.error(key + "-761:768 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_och

                else:
                    logger.error("delete interface process response code or output result not match expected")
                    logger.error("set False deleteCheck")
                    deleteCheck = False
                del response

            else:
                logger.error(key + ' and ' + value['linked-client-ports'][0][
                    'logical-connection-point'] + ' port capability does not match')
                logger.error("set False createCheck")
                createCheck = False
        # TODO: network port may support multiple speed, 400, 300, 200, 'supported-interface-capability' may contain multiple speed, need to loopin and check
        elif type == 'mpdr':
            logger.debug("this is a mpdr type port")
            pass
        elif type == 'switch':
            logger.debug("this is a switch type port")
            #################################### Case 1: 100G swpdr ######################################################################
            if 'org-openroadm-port-types:if-OCH-OTU4-ODU4' in value['supported-interface-capability']:
                ################################ STEP 1: interface rendering and check rpc response output, OCH and OTU interfaces
                createCheck_networkport = True
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_100G',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qpsk',
                        'operation': 'create',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'dest-tp': key}],
                        'center-freq': 196.1,
                        'nmc-width': 40,
                        'min-freq': 196.075,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 761,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug(response)
                # check a) create rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and \
                        'Interfaces created successfully for nodes: ' in response['output']['result'] and \
                        response['output']['success'] == True:
                    expected_subset_response = {
                        'node-id': os.getenv('NODEID'),
                        'otu-interface-id': [key + '-OTU'],
                        'och-interface-id': [key + '-761:768']
                    }
                    subset = {k: v for k, v in response['output']['node-interface'][0].items() if
                              k in expected_subset_response}
                    if sorted(subset) == sorted(expected_subset_response):
                        logger.info(key + ' interface rendering output matches expected')
                    else:
                        logger.error("set False createCheck")
                        createCheck = False
                        createCheck_networkport = False
                        logger.error(key + ' interface rendering output does not match expected')

                else:
                    logger.error("create network interface failed.")
                    logger.error(response)
                    logger.error("set False createCheck")
                    createCheck = False
                    createCheck_networkport = False
                del response

                ################################ STEP 2: interface rendering and check rpc response output, ODU interfaces
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'otn-service-path',
                    {
                        'service-name': 'service_100G',
                        'service-log_format': 'ODU',
                        'service-rate': 100,
                        'operation': 'create',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'network-tp': key}]
                    })
                logger.debug(response)
                # check a) create rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and \
                        'Otn Service path was set up successfully for node :' in response['output']['result'] and \
                        response['output']['success'] == True:
                    expected_subset_response = {
                        'node-id': os.getenv('NODEID'),
                        'odu-interface-id': [key + '-ODU4']
                    }
                    subset = {k: v for k, v in response['output']['node-interface'][0].items() if
                              k in expected_subset_response}
                    logger.debug(subset)
                    logger.debug(expected_subset_response)
                    if sorted(subset) == sorted(expected_subset_response):
                        logger.info(key + ' interface rendering output matches expected')
                        pass
                    else:
                        logger.error("set False createCheck")
                        createCheck = False
                        createCheck_networkport = False
                        logger.error(key + ' interface rendering output does not match expected')

                else:
                    logger.error("create network interface failed.")
                    logger.error("set False createCheck")
                    logger.error(response)
                    createCheck = False
                    createCheck_networkport = False
                del response

                # TODO: like tpdr 400G 2.1, need to get the interface configuration from device get, and check the integrity of the interfaces
                # add code here
                ##

                if createCheck_networkport == True:  # network interfaces creation fine, proceed with client interfaces
                    logger.info("starting creating client interfaces for swpdr")
                    for client_port in value['linked-client-ports']:
                        ################################ STEP 2.1:create client interfaces and cross-connections
                        createCheck_clientport = True
                        if not set(client_10G_capabilities).isdisjoint(client_port["supported-interface-capability"]):
                            logger.debug("creating interfaces on client port " + client_port[
                                "logical-connection-point"] + " client speed 10G")
                            service_rate = 10
                        elif not set(client_1G_capabilities).isdisjoint(client_port["supported-interface-capability"]):
                            logger.debug("creating interfaces on client port " + client_port[
                                "logical-connection-point"] + " client speed 1G")
                            service_rate = 1
                        else:
                            logger.error("client port " + client_port[
                                "logical-connection-point"] + " client speed not supported under this xponder tree")
                            service_rate = None
                            logger.error("set False createCheck")
                            createCheck = False
                            createCheck_clientport = False

                        if createCheck_clientport == True:
                            response = test_utils.transportpce_api_rpc_request(
                                'transportpce-device-renderer', 'otn-service-path',
                                {
                                    'service-name': 'service_eth',
                                    'service-log_format': 'Ethernet',
                                    'service-rate': service_rate,
                                    'operation': 'create',
                                    'nodes': [{'node-id': os.getenv('NODEID'),
                                               'network-tp': key,
                                               'client-tp': client_port['logical-connection-point']}],
                                    'ethernet-encoding': "eth encode",
                                    "trib-slot": [
                                        "1"
                                    ],
                                    "trib-port-number": "1"
                                })
                            logger.debug(response)
                            # check a) create rpc response and b) existance of interfaces
                            if response['status_code'] == requests.codes.ok and \
                                    'Otn Service path was set up successfully for node :' in response['output'][
                                'result'] and \
                                    response['output']['success'] == True:
                                expected_subset_response = {
                                    "node-id": os.getenv('NODEID'),
                                    "odu-interface-id": [
                                        key + ("-ODU2e:service_eth" if service_rate == 10 else "-ODU0:service_eth"),
                                        client_port['logical-connection-point'] + (
                                            "-ODU2e:service_eth" if service_rate == 10 else "-ODU0:service_eth")
                                    ],
                                    "eth-interface-id": [
                                        client_port['logical-connection-point'] + (
                                            "-ETHERNET10G" if service_rate == 10 else "-ETHERNET1G")
                                    ],
                                    "connection-id": [
                                        client_port['logical-connection-point'] + (
                                            "-ODU2e-x-" if service_rate == 10 else "-ODU0-x-") + key + (
                                            "-ODU0" if service_rate == 10 else "-ODU0")
                                    ]
                                }
                                subset = {k: v for k, v in response['output']['node-interface'][0].items() if
                                          k in expected_subset_response}
                                logger.debug(subset)
                                logger.debug(expected_subset_response)
                                if sorted(subset) == sorted(expected_subset_response):
                                    logger.info(client_port[
                                                    'logical-connection-point'] + ' interface rendering output matches expected')
                                    pass
                                else:
                                    logger.error("set False createCheck")
                                    createCheck = False
                                    logger.error(client_port[
                                                     'logical-connection-point'] + ' interface rendering output does not match expected')
                                pass
                            else:
                                logger.error("create client interface failed.")
                                logger.error(response)
                                logger.error("set False createCheck")
                                createCheck = False
                            del response
                        else:
                            logger.error("skipping creating interfaces on client port " + client_port[
                                "logical-connection-point"])
                            continue

                        # TODO: like tpdr 400G 2.1, need to get the interface configuration from device get, and check the integrity of the interfaces
                        # add code here
                        ##

                        ################################ STEP 2.2: delete this client interfaces
                        if createCheck_clientport == True:
                            response = test_utils.transportpce_api_rpc_request(
                                'transportpce-device-renderer', 'otn-service-path',
                                {
                                    'service-name': 'service_eth',
                                    'service-log_format': 'Ethernet',
                                    'service-rate': service_rate,
                                    'operation': 'delete',
                                    'nodes': [{'node-id': os.getenv('NODEID'),
                                               'network-tp': key,
                                               'client-tp': client_port['logical-connection-point']}],
                                    'ethernet-encoding': "eth encode",
                                    "trib-slot": [
                                        "1"
                                    ],
                                    "trib-port-number": "1"
                                })
                            logger.debug(response)
                            time.sleep(5)
                            # check a) delete rpc response and b) existance of interfaces
                            if response['status_code'] == requests.codes.ok and 'Request processed' in \
                                    response['output']['result']:
                                # TODO: check existance of crossconnecion first
                                response_networkODU = test_utils.check_node_attribute_request(os.getenv('NODEID'),
                                                                                              "interface", key + (
                                                                                                  "-ODU2e:service_eth" if service_rate == 10 else "-ODU0:service_eth"))
                                if response_networkODU['status_code'] not in [requests.codes.conflict,
                                                                              requests.codes.service_unavailable]:
                                    logger.error(key + (
                                        "-ODU2e:service_eth" if service_rate == 10 else "-ODU0:service_eth") + " still found")
                                    logger.error("set False deleteCheck")
                                    deleteCheck = False
                                del response_networkODU

                                response_clientODU = test_utils.check_node_attribute_request(os.getenv('NODEID'),
                                                                                             "interface", client_port[
                                                                                                 'logical-connection-point'] + (
                                                                                                 "-ODU2e:service_eth" if service_rate == 10 else "-ODU0:service_eth"))
                                if response_clientODU['status_code'] not in [requests.codes.conflict,
                                                                             requests.codes.service_unavailable]:
                                    logger.error(client_port['logical-connection-point'] + (
                                        "-ODU2e:service_eth" if service_rate == 10 else "-ODU0:service_eth") + " still found")
                                    logger.error("set False deleteCheck")
                                    deleteCheck = False
                                del response_clientODU

                                response_clientEth = test_utils.check_node_attribute_request(os.getenv('NODEID'),
                                                                                             "interface", client_port[
                                                                                                 'logical-connection-point'] + (
                                                                                                 "-ETHERNET10G" if service_rate == 10 else "-ETHERNET1G"))
                                if response_clientEth['status_code'] not in [requests.codes.conflict,
                                                                             requests.codes.service_unavailable]:
                                    logger.error(client_port['logical-connection-point'] + (
                                        "-ETHERNET10G" if service_rate == 10 else "-ETHERNET1G") + " still found")
                                    logger.error("set False deleteCheck")
                                    deleteCheck = False
                                del response_clientEth

                            else:
                                logger.error("delete response code or output result doesn't match")
                                logger.error("set False deleteCheck")
                                deleteCheck = False
                            del response
                        else:
                            logger.warning("skipping deleting interfaces on client port " + client_port[
                                "logical-connection-point"] + " because client port check failed")
                        del service_rate, createCheck_clientport

                else:
                    # network interfaces creation wrong, skip client interfaces
                    logger.error("network interfaces creation wrong, skip client interfaces")

                del createCheck_networkport

                ################################ STEP 3, 4 check pm and alarm
                # TODO
                ################################ STEP 5: delete network ODU interface and check the existance of the interfaces
                time.sleep(5)
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'otn-service-path',
                    {
                        'service-name': 'service_100G',
                        'service-log_format': 'ODU',
                        'service-rate': 100,
                        'operation': 'delete',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'network-tp': key}]
                    })
                logger.debug(response)
                time.sleep(2)
                # check a) delete rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Request processed' in response['output']['result']:
                    response_odu4 = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                            key + "-ODU4")
                    if response_odu4['status_code'] not in [requests.codes.conflict,
                                                            requests.codes.service_unavailable]:
                        logger.error(key + "-ODU4 not deleted!")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_odu4

                else:
                    logger.error("delete interface response code or output result doesn't match")
                    logger.error("set False deleteCheck")
                    deleteCheck = False
                del response

                # Step 6: delete network OCH OTU interface and check the existance of the interfaces
                time.sleep(5)
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_100G',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qpsk',
                        'operation': 'delete',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'dest-tp': key}],
                        'center-freq': 196.1,
                        'nmc-width': 40,
                        'min-freq': 196.075,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 761,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug(response)
                time.sleep(2)
                # check a) delete rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Request processed' in response['output']['result']:
                    response_otu = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                           key + "-OTU")
                    if response_otu['status_code'] not in [requests.codes.conflict, requests.codes.service_unavailable]:
                        logger.error(key + "-OTU not deleted!")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_otu

                    response_och = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                           key + "-761:768")
                    if response_och['status_code'] not in [requests.codes.conflict, requests.codes.service_unavailable]:
                        logger.error(key + "-761:768 not deleted!")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_och

                else:
                    logger.error("delete interface response code or output result doesn't match")
                    logger.error("set False deleteCheck")
                    deleteCheck = False
                del response
            #################################### Case 2: 400G swpdr in the future ######################################################################
            else:
                pass
        elif type == 'rdm':
            logger.debug("this is a rdm type port, degree port")

            if len(value["linked-srg-ports"]) == 0:
                logger.error("empty srg ports in port " + key)
                logger.error("set False createCheck")
                createCheck = False
                continue

            for srgPort in value["linked-srg-ports"]:
                #################################### STEP 1: interface rendering and check rpc response output
                #         due to the unidirectional roadm connections, this rpc needs to be called twice, with reverse src and dest 
                # first srg to deg direction
                logger.debug("creating tunnel from " + srgPort['logical-connection-point'] + " to " + key)
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qam16',
                        'operation': 'create',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'src-tp': srgPort['logical-connection-point'],
                                   'dest-tp': key}],
                        'center-freq': 196.08125,
                        'nmc-width': 75,
                        'min-freq': 196.0375,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 755,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug(response)

                # check a) create rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Interfaces created successfully for nodes: ' in \
                        response['output']['result']:
                    expected_subset_response = {
                        'node-id': os.getenv('NODEID'),
                        'connection-id': [key + '-' + srgPort['logical-connection-point'] + '-755:768']}
                    expected_sorted_och_list = [key + '-mc-755:768',
                                                key + '-nmc-755:768',
                                                srgPort['logical-connection-point'] + '-nmc-755:768',
                                                ]
                    subset = {k: v for k, v in response['output']['node-interface'][0].items() if
                              k in expected_subset_response}
                    if sorted(subset) == sorted(expected_subset_response) and \
                            sorted(response['output']['node-interface'][0]['och-interface-id']) == sorted(
                        expected_sorted_och_list):
                        logger.debug(key + ' interface rendering output matches expected')
                    else:
                        createCheck = False
                        logger.error("set False createCheck")
                        logger.error(key + ' interface rendering output does not match expected')

                else:
                    createCheck = False
                    logger.error("interface rendering response code or output result not match")
                    logger.error("set False createCheck")
                del response

                # second: deg to srg direction
                logger.debug("creating tunnel from " + key + " to " + srgPort['logical-connection-point'])
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qam16',
                        'operation': 'create',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'src-tp': key,
                                   'dest-tp': srgPort['logical-connection-point']}],
                        'center-freq': 196.08125,
                        'nmc-width': 75,
                        'min-freq': 196.0375,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 755,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug(response)

                # check a) create rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Interfaces created successfully for nodes: ' in \
                        response['output']['result']:
                    expected_subset_response = {
                        'node-id': os.getenv('NODEID'),
                        'connection-id': [srgPort['logical-connection-point'] + '-' + key + '-755:768']}
                    expected_sorted_och_list = [key + '-mc-755:768',
                                                key + '-nmc-755:768',
                                                srgPort['logical-connection-point'] + '-nmc-755:768',
                                                ]
                    subset = {k: v for k, v in response['output']['node-interface'][0].items() if
                              k in expected_subset_response}
                    if sorted(subset) == sorted(expected_subset_response) and \
                            sorted(response['output']['node-interface'][0]['och-interface-id']) == sorted(
                        expected_sorted_och_list):
                        logger.debug(key + ' interface rendering output matches expected')
                    else:
                        createCheck = False
                        logger.error("set False createCheck")
                        logger.error(key + ' interface rendering output does not match expected')

                else:
                    createCheck = False
                    logger.error("interface rendering response code or output result not match")
                    logger.error("set False createCheck")
                del response

                #################################### step 2: check interfaces and roadm connection config
                # TODO

                #################################### Step 3: check pm here
                # create pm only for honeynode
                # create_pm_honeynode(host=os.getenv('HOST'),port=os.getenv('PORT'), interfaceName="'"+ key +"-755:768'")

                conn = make_netconf_connection(host=os.getenv('HOST'), port=os.getenv('PORT'),
                                               username=os.getenv('USERNAME'), password=os.getenv('PASSWORD'),
                                               timeout=30)
                if conn:
                    # TODO: add pm to check here
                    logger.debug("Getting degree interface PM...")
                    try:
                        RPC_PM_Interface(conn, "'" + key + "-755:768'", P_side='opticalPowerOutput')
                    except:
                        logger.error('Error raised: ', exc_info=True)
                        logger.error("set False pmCheck")
                        logger.error("Couldn't get degree interface PM")
                        pmCheck = False
                else:
                    logger.error("Connection failed")
                    logger.error("set False pmCheck")
                    pmCheck = False
                # delete pm only for honeynode
                # delete_pm_honeynode(host=os.getenv('HOST'),port=os.getenv('PORT'), interfaceName="'"+ key +"-755:768'")

                #################################### STEP 4: check alarm here
                # TODO

                #################################### STEP 5: delete interface and check the existance of the interfaces
                #         also need to delete twice with different src and dest
                time.sleep(5)
                # first
                logger.debug("delete tunnel from " + srgPort['logical-connection-point'] + " to " + key)
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qam16',
                        'operation': 'delete',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'src-tp': srgPort['logical-connection-point'],
                                   'dest-tp': key}],
                        'center-freq': 196.08125,
                        'nmc-width': 75,
                        'min-freq': 196.0375,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 755,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug("Deleting interfaces ...")
                logger.debug(response)
                time.sleep(2)
                # check a) delete rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Request processed' in response['output']['result']:
                    response_roadm_connection = test_utils.check_node_attribute_request(os.getenv('NODEID'),
                                                                                        "roadm-connections", srgPort[
                                                                                            'logical-connection-point'] + '-' + key + '-755:768')
                    if response_roadm_connection['status_code'] not in [requests.codes.conflict,
                                                                        requests.codes.service_unavailable]:
                        logger.error(srgPort['logical-connection-point'] + '-' + key + '-755:768 still found')
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_roadm_connection

                else:
                    logger.error("set False deleteCheck")
                    deleteCheck = False
                del response

                # second
                time.sleep(5)
                logger.debug("delete tunnel from " + key + " to " + srgPort['logical-connection-point'])
                response = test_utils.transportpce_api_rpc_request(
                    'transportpce-device-renderer', 'service-path',
                    {
                        'service-name': 'service_',
                        'wave-number': '0',
                        'modulation-log_format': 'dp-qam16',
                        'operation': 'delete',
                        'nodes': [{'node-id': os.getenv('NODEID'), 'src-tp': key,
                                   'dest-tp': srgPort['logical-connection-point']}],
                        'center-freq': 196.08125,
                        'nmc-width': 75,
                        'min-freq': 196.0375,
                        'max-freq': 196.125,
                        'lower-spectral-slot-number': 755,
                        'higher-spectral-slot-number': 768
                    })
                logger.debug("Deleting interfaces ...")
                logger.debug(response)
                time.sleep(2)
                # check a) delete rpc response and b) existance of interfaces
                if response['status_code'] == requests.codes.ok and 'Request processed' in response['output']['result']:
                    response_degree_nmc = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                                  key + '-nmc-755:768')
                    if response_degree_nmc['status_code'] not in [requests.codes.conflict,
                                                                  requests.codes.service_unavailable]:
                        logger.error(key + "-nmc-755:768 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_degree_nmc

                    response_degree_mc = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                                 key + '-mc-755:768')
                    if response_degree_mc['status_code'] not in [requests.codes.conflict,
                                                                 requests.codes.service_unavailable]:
                        logger.error(key + "-mc-755:768 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_degree_mc

                    response_srg_nmc = test_utils.check_node_attribute_request(os.getenv('NODEID'), "interface",
                                                                               srgPort[
                                                                                   'logical-connection-point'] + '-nmc-755:768')
                    if response_srg_nmc['status_code'] not in [requests.codes.conflict,
                                                               requests.codes.service_unavailable]:
                        logger.error(srgPort['logical-connection-point'] + "-nmc-755:768 still found")
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_srg_nmc

                    response_roadm_connection = test_utils.check_node_attribute_request(os.getenv('NODEID'),
                                                                                        "roadm-connections",
                                                                                        key + '-' + srgPort[
                                                                                            'logical-connection-point'] + '-755:768')
                    if response_roadm_connection['status_code'] not in [requests.codes.conflict,
                                                                        requests.codes.service_unavailable]:
                        logger.error(key + '-' + srgPort['logical-connection-point'] + '-755:768 still found')
                        logger.error("set False deleteCheck")
                        deleteCheck = False
                    del response_roadm_connection


                else:
                    logger.error("set False deleteCheck")
                    deleteCheck = False
                del response

                # TODO: only test one pair for now to save time
                break

        else:
            pass

    return createCheck, configCheck, pmCheck, alarmCheck, deleteCheck
