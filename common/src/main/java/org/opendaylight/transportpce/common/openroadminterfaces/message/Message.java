/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces.message;

public interface Message {

    String failedCreatingInterfaceNoComNoTxTrans(String nodeId, String interfaceName);

    String failedCreatingInterfaceComInterruptedNoTrans(String nodeId, String interfaceName);

    String failedCreatingInterfaceNoComNoTrans(String nodeId, String interfaceName);

    String failedCreatingInterfaceComInterrupted(String nodeId, String interfaceName);

    String failedCreatingInterfaceNoCom(String nodeId, String interfaceName);

    String failedDeleteInterfaceNotFound(String nodeId, String interfaceName);

    String failedDeleteInterfaceNoComNoTXTrans(String nodeId, String interfaceName);

    String failedDeleteInterfaceComInterruptedNoTrans(String nodeId, String interfaceName);

    String failedDeleteInterfaceNoComNoTrans(String nodeId, String interfaceName);

    String failedDeleteInterfaceInterruptedCom(String nodeId, String interfaceName);

    String failedDeleteInterfaceNoCom(String nodeId, String interfaceName);

    String failedWritingEquipmentStateComInterruptedNoTrans(String nodeId);

    String failedWritingEquipmentStateNoComNoTrans(String nodeId);

    String failedWritingEquipmentStateNoComNoTxTrans(String nodeId);

    String failedWritingEquipmentStateNoCom(String nodeId);

    String failedWritingEquipmentStateComInterrupted(String nodeId);

    String failedWritingInterfaceStateWhileDeleting(String nodeId, String interfaceName, String state);

    String failedReadingFromDeviceNoComInterrupted(String nodeId);

    String failedReadingFromDeviceNoCom(String nodeId);

    String failedReadingFromDeviceTimeout(String nodeId);

    String failedReadingFromDeviceNoComNoTxTrans(String nodeId);

    String circuitPackNotFound(String nodeId, String circuitPack);
}
