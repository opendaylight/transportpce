/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.tapisbi.utils.BasicEncryptionUtil;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ControllerBehaviourSettings1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.TapiSbiControllers;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiControllerKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ControllerBehaviourSettings;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for reading and writing TAPI SBI controller configuration in the config datastore.
 */
@Component(service = TapiSbiControllerService.class)
public class TapiSbiControllerService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiControllerService.class);

    private final DataBroker dataBroker;

    @Activate
    public TapiSbiControllerService(@Reference DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        LOG.info("TapiSbiControllerService initialized");
    }

    /**
     * Retrieves a specific TAPI SBI controller by controller-id from the config datastore.
     *
     * @param controllerId the unique controller identifier
     * @return TapiSbiController or null if not found
     */
    public TapiSbiController getTapiSbiControllerConfig(String controllerId) {
        LOG.info("Reading TAPI SBI controller configuration for controller-id: {}", controllerId);
        if (controllerId == null || controllerId.trim().isEmpty()) {
            LOG.warn("Controller ID is null or empty");
            return null;
        }
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(
                LogicalDatastoreType.CONFIGURATION, buildControllerIID(controllerId)).get().orElse(null);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to read TAPI SBI controller configuration from datastore", e);
            return null;
        }
    }

    /**
     * Retrieves all TAPI SBI controllers from the config datastore.
     *
     * @return list of TapiSbiController entries, empty list if none found
     */
    public List<TapiSbiController> getAllTapiSbiControllers() {
        LOG.info("Reading all TAPI SBI controller configurations from datastore");
        var tapiSbiControllersIID = DataObjectIdentifier.builder(ControllerBehaviourSettings.class)
            .augmentation(ControllerBehaviourSettings1.class)
            .child(TapiSbiControllers.class)
            .build();
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            Optional<TapiSbiControllers> optionalControllers = readTransaction.read(
                LogicalDatastoreType.CONFIGURATION, tapiSbiControllersIID).get();
            if (optionalControllers.isEmpty()) {
                return Collections.emptyList();
            }
            Map<TapiSbiControllerKey, TapiSbiController> controllerMap =
                optionalControllers.orElseThrow().getTapiSbiController();
            if (controllerMap == null || controllerMap.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(controllerMap.values());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to read TAPI SBI controllers from datastore", e);
            return Collections.emptyList();
        }
    }

    /**
     * Builds the DataObjectIdentifier for a TapiSbiController entry with the given controller-id.
     *
     * @param controllerId the unique controller identifier
     * @return DataObjectIdentifier pointing to the controller entry in the config datastore
     */
    public DataObjectIdentifier<TapiSbiController> buildControllerIID(String controllerId) {
        return DataObjectIdentifier.builder(ControllerBehaviourSettings.class)
            .augmentation(ControllerBehaviourSettings1.class)
            .child(TapiSbiControllers.class)
            .child(TapiSbiController.class, new TapiSbiControllerKey(controllerId))
            .build();
    }

    /**
     * Checks whether a controller with the given ID already exists in the config datastore.
     *
     * @param controllerId the unique controller identifier
     * @return {@code true} if the controller entry is present
     */
    public boolean controllerExists(String controllerId) {
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(
                LogicalDatastoreType.CONFIGURATION, buildControllerIID(controllerId)).get().isPresent();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to check if controller exists: {}", controllerId, e);
            return false;
        }
    }

    /**
     * Checks whether any registered controller already uses the given host and port combination.
     *
     * @param host the host address to check
     * @param port the port value to check
     * @return {@code true} if a controller with the same host and port is already registered
     */
    public boolean controllerExistsByHostAndPort(String host, String port) {
        return getAllTapiSbiControllers().stream().anyMatch(c ->
            host.equalsIgnoreCase(c.getHost().stringValue())
                && port.equals(c.getPort().getValue().toString()));
    }

    /**
     * Writes a TAPI SBI controller entry to the config datastore.
     *
     * @param controller the controller entry to persist
     * @return {@code true} if the commit succeeded
     */
    public boolean saveController(TapiSbiController controller) {
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION,
            buildControllerIID(controller.getControllerId()), controller);
        try {
            writeTx.commit().get();
            LOG.debug("Controller {} saved to datastore", controller.getControllerId());
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to save controller {} to datastore", controller.getControllerId(), e);
            return false;
        }
    }

    /**
     * Removes a TAPI SBI controller entry from the config datastore.
     *
     * @param controllerId the unique controller identifier to remove
     * @return {@code true} if the commit succeeded
     */
    public boolean deleteController(String controllerId) {
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, buildControllerIID(controllerId));
        try {
            writeTx.commit().get();
            LOG.debug("Controller {} deleted from datastore", controllerId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete controller {} from datastore", controllerId, e);
            return false;
        }
    }

    /**
     * Decrypts the stored password for use in HTTP Basic Auth calls to a southbound controller.
     *
     * @param encryptedPassword the encrypted password from the datastore
     * @return decrypted plain-text password
     */
    public String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
            return "";
        }
        return BasicEncryptionUtil.decryptPassword(encryptedPassword);
    }
}
