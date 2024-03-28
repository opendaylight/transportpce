/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces.message;

public class ErrorMessage implements Message {

    public static final String FAILED_CREATING_INTERFACE_NO_COM_NO_TX_TRANS =
        "Unable to communicate with %s (%s), device transaction for TX not found, failed creating interface %s!";

    public static final String FAILED_CREATING_INTERFACE_INTERRUPTED_COM_NO_TRANS =
        "Communication with %s (%s) interrupted. Failed obtaining device transaction when creating interface %s!";

    public static final String FAILED_CREATING_INTERFACE_NO_COM_NO_TRANS =
        "Cannot communicate with %s (%s). Failed obtaining device transaction when preparing to create interface %s!";

    public static final String FAILED_CREATING_INTERFACE_COM_INTERRUPTED =
        "Communication with %s (%s) interrupted, failed creating interface %s!";

    public static final String FAILED_CREATING_INTERFACE_NO_COM =
        "Cannot communicate with %s (%s), failed creating interface %s";

    public static final String FAILED_DELETING_INTERFACE_INTERFACE_NOT_FOUND =
        "Failed deleting interface %s on %s (%s), interface not found!";

    public static final String FAILED_DELETING_INTERFACE_NO_COM_NO_TX_TRANS =
        "Unable to communicate with %s (%s), device TX not present when attempting to delete interface %s!";

    public static final String FAILED_DELETING_INTERFACE_INTERRUPTED_COM_NO_TRANS =
        "Communication with %s (%s) interrupted, failed obtaining device transaction while deleting interface %s!";

    public static final String FAILED_DELETING_INTERFACE_NO_COM_NO_TRANS =
        "Communication error on node %s (%s), failed obtaining device transaction while deleting interface %s!";

    public static final String FAILED_DELETING_INTERFACE_COM_INTERRUPTED =
        "Communication with %s (%s) interrupted, failed deleting interface %s!";

    public static final String FAILED_DELETING_INTERFACE_NO_COM =
        "Communication error on node %s (%s), failed deleting interface %s!";

    public static final String FAILED_WRITING_EQUIPMENT_STATE_COM_INTERRUPTED_NO_TRANS =
        "Communication with %s (%s) interrupted, failed obtaining device transaction while writing equipment state!";
    public static final String FAILED_WRITING_EQUIPMENT_STATE_NO_COM_NO_TRANS =
        "Communication error with %s (%s), failed to obtain device transaction while writing equipment state!";

    public static final String FAILED_WRITING_EQUIPMENT_STATE_NO_COM_NO_TX_TRANS =
        "Communication error with %s (%s), device TX not present when while writing equipment state!";

    public static final String FAILED_WRITING_EQUIPMENT_STATE_COM_INTERRUPTED =
        "Communication with %s (%s) interrupted, failed to write equipment state!";

    public static final String FAILED_WRITING_EQUIPMENT_STATE_NO_COM =
        "Communication with %s (%s) failed, failed to write equipment state!";

    public static final String CIRCUIT_PACK_NOT_FOUND =
        "Could not find CircuitPack %s in equipment config datastore for node %s (%s)";

    public static final String FAILED_WRITING_INTERFACE_STATE_WHILE_DELETING =
        "Failed to set state of interface %s on node %s (%s) to %s while deleting it!";

    public static final String FAILED_READING_FROM_DEVICE_COM_INTERRUPTED =
        "Communication with %s interrupted, failed reading from device!";

    public static final String FAILED_READING_FROM_DEVICE_NO_COM =
        "Cannot communicate with %s, failed reading from device!";

    public static final String FAILED_READING_FROM_DEVICE_TIMEOUT =
        "Cannot communicate with %s, failed reading from device!";

    public static final String FAILED_READING_FROM_DEVICE_NO_COM_NO_TX_TRANS =
        "Unable to communicate with %s, device TX not present when attempting to read from device %s!";

    private final String version;

    public ErrorMessage(String version) {
        this.version = version;
    }

    @Override
    public String failedCreatingInterfaceNoComNoTxTrans(String nodeId, String interfaceName) {
        //Unable to communicate with %s, device transaction for TX not found, failed creating interface %s
        return String.format(FAILED_CREATING_INTERFACE_NO_COM_NO_TX_TRANS, nodeId, version, interfaceName);
    }

    @Override
    public String failedCreatingInterfaceComInterruptedNoTrans(String nodeId, String interfaceName) {
        //"Communication with %s interrupted. Failed obtaining device transaction when creating interface %s!"
        return String.format(FAILED_CREATING_INTERFACE_INTERRUPTED_COM_NO_TRANS, nodeId, version, interfaceName);
    }

    @Override
    public String failedCreatingInterfaceNoComNoTrans(String nodeId, String interfaceName) {
        //"Cannot communicate with %s. Failed obtaining device transaction when preparing to create interface %s!";
        return String.format(FAILED_CREATING_INTERFACE_NO_COM_NO_TRANS, nodeId, version, interfaceName);
    }

    @Override
    public String failedCreatingInterfaceComInterrupted(String nodeId, String interfaceName) {
        //"Communication with %s interrupted, failed creating interface %s!";
        return String.format(FAILED_CREATING_INTERFACE_COM_INTERRUPTED, nodeId, version, interfaceName);
    }

    @Override
    public String failedCreatingInterfaceNoCom(String nodeId, String interfaceName) {
        //"Cannot communicate with %s, failed creating interface %s";
        return String.format(FAILED_CREATING_INTERFACE_NO_COM, nodeId, version, interfaceName);
    }

    @Override
    public String failedDeleteInterfaceNotFound(String nodeId, String interfaceName) {
        //"Failed deleting interface %s on %s, interface not found!";
        return String.format(FAILED_DELETING_INTERFACE_INTERFACE_NOT_FOUND, nodeId, version, interfaceName);
    }

    @Override
    public String failedDeleteInterfaceNoComNoTXTrans(String nodeId, String interfaceName) {
        //"Unable to communicate with %s, device TX not present when attempting to delete interface %s!";
        return String.format(FAILED_DELETING_INTERFACE_NO_COM_NO_TX_TRANS, nodeId, version, interfaceName);
    }

    @Override
    public String failedDeleteInterfaceComInterruptedNoTrans(String nodeId, String interfaceName) {
        //"Communication with %s interrupted, failed obtaining device transaction while deleting interface %s!";
        return String.format(FAILED_DELETING_INTERFACE_INTERRUPTED_COM_NO_TRANS, nodeId, version, interfaceName);
    }

    @Override
    public String failedDeleteInterfaceNoComNoTrans(String nodeId, String interfaceName) {
        //"Communication error on node %s, failed obtaining device transaction while deleting interface %s!";
        return String.format(FAILED_DELETING_INTERFACE_NO_COM_NO_TRANS, nodeId, version, interfaceName);
    }

    @Override
    public String failedDeleteInterfaceInterruptedCom(String nodeId, String interfaceName) {
        //"Communication with %s interrupted, failed deleting interface %s!";
        return String.format(FAILED_DELETING_INTERFACE_COM_INTERRUPTED, nodeId, version, interfaceName);
    }

    @Override
    public String failedDeleteInterfaceNoCom(String nodeId, String interfaceName) {
        //"Communication error on node %s, failed deleting interface %s!";
        return String.format(FAILED_DELETING_INTERFACE_NO_COM, nodeId, version, interfaceName);
    }

    @Override
    public String failedWritingEquipmentStateComInterruptedNoTrans(String nodeId) {
        //"Communication with %s interrupted, failed obtaining device transaction while writing equipment state!";
        return String.format(FAILED_WRITING_EQUIPMENT_STATE_COM_INTERRUPTED_NO_TRANS, nodeId, version);
    }

    @Override
    public String failedWritingEquipmentStateNoComNoTrans(String nodeId) {
        //"Communication error with %s, failed to obtain device transaction while writing equipment state!";
        return String.format(FAILED_WRITING_EQUIPMENT_STATE_NO_COM_NO_TRANS, nodeId, version);
    }

    @Override
    public String failedWritingEquipmentStateNoComNoTxTrans(String nodeId) {
        return String.format(FAILED_WRITING_EQUIPMENT_STATE_NO_COM_NO_TX_TRANS, nodeId, version);
    }

    @Override
    public String failedWritingEquipmentStateNoCom(String nodeId) {
        //"Communication with %s failed, failed to write equipment state!";
        return String.format(FAILED_WRITING_EQUIPMENT_STATE_NO_COM, nodeId, version);
    }

    @Override
    public String failedWritingEquipmentStateComInterrupted(String nodeId) {
        //"Communication with %s interrupted, failed to write equipment state!";
        return String.format(FAILED_WRITING_EQUIPMENT_STATE_COM_INTERRUPTED, nodeId, version);
    }

    @Override
    public String failedWritingInterfaceStateWhileDeleting(String nodeId, String interfaceName, String state) {
        //"Failed to set state of interface %s on node %s (%s) to %s while deleting it!"
        return String.format(
                FAILED_WRITING_INTERFACE_STATE_WHILE_DELETING,
                interfaceName,
                nodeId,
                version,
                state
        );
    }

    @Override
    public String failedReadingFromDeviceNoComInterrupted(String nodeId) {
        return String.format(FAILED_READING_FROM_DEVICE_COM_INTERRUPTED, nodeId);
    }

    @Override
    public String failedReadingFromDeviceNoCom(String nodeId) {
        return String.format(FAILED_READING_FROM_DEVICE_NO_COM, nodeId);
    }

    @Override
    public String failedReadingFromDeviceTimeout(String nodeId) {
        return String.format(FAILED_READING_FROM_DEVICE_TIMEOUT, nodeId);
    }

    @Override
    public String failedReadingFromDeviceNoComNoTxTrans(String nodeId) {
        return String.format(FAILED_READING_FROM_DEVICE_NO_COM_NO_TX_TRANS, nodeId);
    }

    @Override
    public String circuitPackNotFound(String nodeId, String circuitPack) {
        //"Communication with %s interrupted, failed to write equipment state!";
        return String.format(CIRCUIT_PACK_NOT_FOUND, circuitPack, nodeId, version);
    }

}
