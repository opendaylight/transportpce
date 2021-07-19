/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.transportpce.inventory.utils.StringUtils.getCurrentTimestamp;
import static org.opendaylight.transportpce.inventory.utils.StringUtils.prepareDashString;
import static org.opendaylight.transportpce.inventory.utils.StringUtils.prepareEmptyString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.sql.DataSource;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.inventory.query.Queries;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CpSlots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CpSlotsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.external.links.ExternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.external.links.ExternalLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.internal.links.InternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.internal.links.InternalLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.physical.links.PhysicalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.physical.links.PhysicalLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelf.Slots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelf.SlotsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelves.Shelves;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelves.ShelvesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfig;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfigKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.nbr.list.IfName;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.nbr.list.IfNameKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.attributes.Tcm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.attributes.TcmKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.ExpMsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.ExpMsiKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.RxMsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.RxMsiKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.TxMsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.TxMsiKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.otu.container.OtuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.wavelength.map.rev161014.wavelength.map.g.Wavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.wavelength.map.rev161014.wavelength.map.g.WavelengthsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
    justification = "TODO review the SQL statement generation process")
public class INode121 {

    private static final Logger LOG = LoggerFactory.getLogger(INode121.class);

    private final DataSource dataSource;
    private final DeviceTransactionManager deviceTransactionManager;

    public INode121(DataSource dataSource, DeviceTransactionManager deviceTransactionManager) {
        this.dataSource = dataSource;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public boolean addNode(String deviceId) {

        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> infoOpt =
                deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, infoIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        Info deviceInfo;
        if (!infoOpt.isPresent()) {
            LOG.warn("Could not get device info from DataBroker");
            return false;
        }
        deviceInfo = infoOpt.get();
        boolean sqlResult = false;
        String query = Queries.getQuery().deviceInfoInsert().get();
        LOG.info("Running {} query ", query);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            Object[] prepareParameters = prepareDeviceInfoParameters(deviceInfo);
            for (int i = 0; i < prepareParameters.length; i++) {
                LOG.debug("Parameter {} has value {}", i + 1, prepareParameters[i]);
                preparedStatement.setObject(i + 1, prepareParameters[i]);

            }
            int executeUpdate = preparedStatement.executeUpdate();
            LOG.info("{} entries were added", executeUpdate);
            sqlResult = true;

            LOG.debug("iNode AddNode call complete");
            getRoadmShelves(deviceId);
            LOG.debug("iNode getRoadmShelves call complete");
            getCircuitPacks(deviceId);
            LOG.debug("iNode getCircuitPacks call complete");

            LOG.debug("iNode persist interfaces call");
            persistDevInterfaces(deviceId, connection);
            LOG.debug("iNode persist interfaces call complete");


            LOG.debug("iNode persist protocols call");
            persistDevProtocols(deviceId, connection);
            LOG.debug("iNode persist protocols call complete");


            LOG.debug("iNode persist wavelength map call");
            persistDevWavelengthMap(deviceId, connection);
            LOG.debug("iNode persist wavelength map call complete");

            LOG.debug("iNode persist internal links map call");
            persistDevInternalLinks(deviceId, connection);
            LOG.debug("iNode persist internal links map call complete");

            LOG.debug("iNode persist Physical links map call");
            persistDevPhysicalLinks(deviceId, connection);
            LOG.debug("iNode persist Physical links map call complete");

            LOG.debug("iNode persist External links map call");
            persistDevExternalLinks(deviceId, connection);
            LOG.debug("iNode persist External links map call complete");

            LOG.debug("iNode persist degree map call");
            persistDevDegree(deviceId, connection);
            LOG.debug("iNode persist degree map call complete");

            LOG.debug("iNode persist srg map call");
            persistDevSrg(deviceId, connection);
            LOG.debug("iNode persist srg map call complete");

            LOG.debug("iNode persist Roadm Connections call");
            persistDevRoadmConnections(deviceId, connection);
            LOG.debug("iNode persist Roadm Connections call complete");

            LOG.debug("iNode persist Connection Map call");
            persistDevConnectionMap(deviceId, connection);
            LOG.debug("iNode persist Connection Map call complete");

        } catch (SQLException | InterruptedException | ExecutionException e) {
            LOG.error("Something wrong when storing node into DB", e);
        }
        return sqlResult;
    }

    public boolean nodeExists(String nodeId) {
        String selectTableSQL = "select count(*) node_exists from inv_dev_info where node_id = ?";
        int nodeExists = 0;
        LOG.info("Checking if {} exists in DB", nodeId);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStmt = connection.prepareStatement(selectTableSQL)) {
            preparedStmt.setString(1, nodeId);
            try (ResultSet rs = preparedStmt.executeQuery()) {
                while (rs.next()) {
                    nodeExists = rs.getInt("node_exists");
                    LOG.debug("Found {} devices matching {}", nodeExists, nodeId);
                }
            }
        } catch (SQLException e) {
            LOG.error("Something wrong when fetching node in DB", e);
        }
        return nodeExists == 0 ? false : true;
    }

    public void getRoadmShelves(String nodeId) throws InterruptedException, ExecutionException {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject = deviceTransactionManager.getDataFromDevice(nodeId,
                LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            return;
        }
        Map<ShelvesKey, Shelves> shelvesMap = deviceObject.get().nonnullShelves();
        LOG.info("Shelves size {}", shelvesMap.size());
        try (Connection connection = requireNonNull(dataSource.getConnection())) {
            for (Map.Entry<ShelvesKey, Shelves> shelveEntry : shelvesMap.entrySet()) {
                Shelves shelve = shelveEntry.getValue();
                String shelfName = shelve.getShelfName();
                LOG.info("Getting Shelve Details of {}", shelfName);
                if (shelve.getSlots() != null) {
                    LOG.info("Slot Size {} ", shelve.getSlots().size());
                    persistShelveSlots(nodeId, shelve, connection);
                } else {
                    LOG.info("No Slots for shelf {}", shelfName);
                }

                persistShelves(nodeId, connection, shelve);
            }
        } catch (SQLException e1) {
            LOG.error("Something wrong when fetching ROADM shelves in DB", e1);
        }
    }

    public void getCircuitPacks(String nodeId) throws InterruptedException, ExecutionException {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.warn("Device object {} was not found", nodeId);
            return;
        }
        Map<CircuitPacksKey, CircuitPacks> circuitPacksMap = deviceObject.get().nonnullCircuitPacks();
        LOG.info("Circuit pack size {}", circuitPacksMap.size());

        try (Connection connection = requireNonNull(dataSource.getConnection())) {
            for (Map.Entry<CircuitPacksKey, CircuitPacks> circuitPackEntry : circuitPacksMap.entrySet()) {
                CircuitPacks cp = circuitPackEntry.getValue();

                if (cp.getCpSlots() != null) {
                    persistCircuitPacksSlots(nodeId, cp, connection);
                }
                LOG.info("Everything {}", cp);
                LOG.info("CP is {}", cp);

                //persistPorts(cp, connection);
                if (cp.getPorts() != null) {
                    persistCPPorts(nodeId, connection, cp);
                }
                persistCircuitPacks(nodeId, connection, cp);
            }
        } catch (SQLException e1) {
            LOG.error("Something wrong when fetching Circuit Packs in DB", e1);
        }
    }

    private void persistCircuitPacks(String nodeId, Connection connection, CircuitPacks cp) {
        Object[] parameters = prepareCircuitPacksParameters(nodeId, cp);
        String query = Queries.getQuery().deviceCircuitPackInsert().get();
        LOG.info("Running {} query ", query);
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int j = 0; j < parameters.length; j++) {
                stmt.setObject(j + 1, parameters[j]);
            }
            stmt.execute();
            stmt.clearParameters();
        } catch (SQLException e) {
            LOG.error("Something wrong when storing Circuit Packs in DB", e);
        }
    }

    private void persistShelves(String nodeId, Connection connection, Shelves shelve) {
        Object[] shelvesParameter = prepareShelvesParameters(nodeId, shelve);
        String query = Queries.getQuery().deviceShelfInsert().get();
        LOG.info("Running {} query ", query);
        try (PreparedStatement preparedStmt = connection.prepareStatement(query)) {
            for (int j = 0; j < shelvesParameter.length; j++) {
                preparedStmt.setObject(j + 1, shelvesParameter[j]);
            }
            preparedStmt.execute();
            preparedStmt.clearParameters();
        } catch (SQLException e) {
            LOG.error("Something wrong when storing shelves in DB", e);
        }
    }

    private void persistShelveSlots(String nodeId, Shelves shelves, Connection connection) {
        String startTimetampStr = getCurrentTimestamp();
        Map<SlotsKey, Slots> slotsMap = shelves.nonnullSlots();
        for (Map.Entry<SlotsKey, Slots> slotEntry : slotsMap.entrySet()) {
            Slots slot = slotEntry.getValue();
            LOG.info("Getting Slot Details of {}", slot.getSlotName());
            Object[] parameters = new Object[]{nodeId,
                shelves.getShelfName(),
                slot.getSlotName(),
                slot.getLabel(),
                slot.getProvisionedCircuitPack(),
                "0",
                startTimetampStr,
                startTimetampStr};
            String query = Queries.getQuery().deviceShelfSlotInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing shelves slots in DB", e);
            }
        }
    }


    private void persistCircuitPacksSlots(String nodeId, CircuitPacks circuitPacks, Connection connection) {
        String startTimetampStr = getCurrentTimestamp();
        Map<CpSlotsKey, CpSlots> cpSlotsMap = circuitPacks.nonnullCpSlots();
        for (Map.Entry<CpSlotsKey, CpSlots> cpSlotEntry: cpSlotsMap.entrySet()) {
            CpSlots cpSlot = cpSlotEntry.getValue();

            Object[] parameters = new Object[]{nodeId,
                circuitPacks.getCircuitPackName(),
                cpSlot.getSlotName(),
                cpSlot.getLabel(),
                cpSlot.getProvisionedCircuitPack(),
                "-1",
                "-1",
                startTimetampStr,
                startTimetampStr};
            String query = Queries.getQuery().deviceCPSlotInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing Cirtcuits Packs slots in DB", e);
            }
        }
    }

    /**
     * Prepares parameters for device insert query.
     *
     * @param deviceInfo device information
     * @return Object an object
     */
    private static Object[] prepareDeviceInfoParameters(Info deviceInfo) {
        String startTimetampStr = getCurrentTimestamp();

        String nodeId = prepareDashString(deviceInfo.getNodeId());
        Long nodeNumber = deviceInfo.getNodeNumber().toJava();
        String nodeTypeEnu = deviceInfo.getNodeType().getName();
        String clli = prepareDashString(deviceInfo.getClli());
        String vendor = prepareDashString(deviceInfo.getVendor());
        String model = prepareDashString(deviceInfo.getModel());
        String serialId = prepareDashString(deviceInfo.getSerialId());
        String ipAddress = prepareDashString(deviceInfo.getIpAddress().getIpv4Address().getValue());
        String prefixLength = prepareDashString(deviceInfo.getPrefixLength());
        String defaultGateway = prepareDashString(deviceInfo.getDefaultGateway().getIpv4Address().getValue());
        String sourceEnum = deviceInfo.getSource().getName();
        String currentIpAddress = prepareDashString(deviceInfo.getCurrentIpAddress().getIpv4Address().getValue());
        String currentPrefixLength = prepareDashString(deviceInfo.getCurrentPrefixLength());
        String currentDefaultGateway = prepareDashString(deviceInfo.getDefaultGateway().getIpv4Address().getValue());
        String macAddress = prepareDashString(deviceInfo.getMacAddress().getValue());
        String softwareVersion = prepareDashString(deviceInfo.getSoftwareVersion());
        //String openroadmVersion = "1.2.1";
        String template = prepareDashString(deviceInfo.getTemplate());
        String currentDatetime = prepareDashString(deviceInfo.getCurrentDatetime().getValue());
        String geoLatitude = (deviceInfo.getGeoLocation() != null
            ? prepareDashString(deviceInfo.getGeoLocation().getLatitude()) : "");
        String geoLongitude = (deviceInfo.getGeoLocation() != null
            ? prepareDashString(deviceInfo.getGeoLocation().getLongitude()) : "");
        String maxDegrees = (deviceInfo.getMaxDegrees() == null ? "-1" : prepareDashString(deviceInfo.getMaxDegrees()));
        String maxSrgs = (deviceInfo.getMaxSrgs() == null ? "-1" : prepareDashString(deviceInfo.getMaxSrgs()));
        String swVersion = prepareDashString(deviceInfo.getSoftwareVersion()); //sw_version
        String swValidationTimer = prepareDashString(""); //sw_validation_timer
        String activationDateTime = prepareDashString(""); //activation_date_time
        //Integer maxNumBin15minHistoricalPm = null;
        //Integer maxNumBin24hourHistoricalPm = null;
        /*jsonDevInfo = JsonStringBuilder.getDevInfoJson().replace("$$NODE-ID$$",nodeId)
                .replace("$$NODE-NUMBER$$", nodeNumber)
                .replace("$$NODE-TYPE$$",nodeType)
                .replace("$$CLLI$$",clli)
                .replace("$$VENDOR$$",vendor)
                .replace("$$MODEL$$",model)
                .replace("$$SERIAL-ID$$",serialId)
                .replace("$$IPADDRESS$$",ipAddress)
                .replace("$$PREFIX-LENGTH$$",prefixLength)
                .replace("$$DEFAULTGATEWAY$$",defaultGateway)
                .replace("$$SOURCE$$",String.valueOf(source))
                .replace("$$CURRENT-IPADDRESS$$",currentIpAddress)
                .replace("$$CURRENT-PREFIX-LENGTH$$",currentPrefixLength)
                .replace("$$CURRENT-DEFAULTGATEWAY$$",currentDefailtGateway)
                .replace("$$MACADDRESS$$",macAddress)
                .replace("$$SOFTWAREVERSION$$",softwareVersion)
                .replace("$$OPENROADM-VERSION$$",openroadmVersion)
                .replace("$$TEMPLATE$$",template)
                .replace("$$CURRENT-DATETIME$$",currentDatetime)
                .replace("$$LATITUDE$$",latitude)
                .replace("$$LONGITUDE$$",longitude)
                .replace("$$MAX-DEGREES$$",maxDegrees)
                .replace("$$MAX-SRGS$$",maxSrgs)
                .replace("$$MAX-NUM-BIN-15MIN-HISTORICAL-PM$$",prepareDashString(""))
                .replace("$$MAX-NUM-BIN-24HOUR-HISTORICAL-PM$$",prepareDashString(""))
                .replace("$$SW-VERSION$$",swVersion)
                .replace("$$SW-VALIDATION-TIMER$$",swValidationTimer)
                .replace("$$ACTIVATION-DATE-TIME$$",activationDateTime);*/


        return new Object[]{
            nodeId,
            nodeNumber,
            nodeTypeEnu,
            clli,
            vendor,
            model,
            serialId,
            ipAddress,
            prefixLength,
            defaultGateway,
            sourceEnum,
            currentIpAddress,
            currentPrefixLength,
            currentDefaultGateway,
            macAddress,
            softwareVersion,
            //openroadmVersion,
            "1.2.1",
            template,
            currentDatetime,
            geoLatitude,
            geoLongitude,
            maxDegrees,
            maxSrgs,
            //maxNumBin15minHistoricalPm,
            //maxNumBin24hourHistoricalPm,
            null, null,
            swVersion,
            swValidationTimer,
            activationDateTime,
            startTimetampStr,
            startTimetampStr
        };
    }


    private static Object[] prepareShelvesParameters(String nodeId, Shelves shelve) {
        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            shelve.getShelfName(),
            shelve.getShelfType(),
            shelve.getRack(),
            shelve.getShelfPosition(),
            (shelve.getAdministrativeState() == null ? null : shelve.getAdministrativeState().getIntValue()),
            shelve.getVendor(),
            shelve.getModel(),
            shelve.getSerialId(),
            shelve.getType(),
            shelve.getProductCode(),
            (shelve.getManufactureDate() == null ? null : shelve.getManufactureDate().getValue()),
            shelve.getClei(),
            shelve.getHardwareVersion(),
            (shelve.getOperationalState() == null ? null : shelve.getOperationalState().getIntValue()),
            (shelve.getEquipmentState() == null ? null : shelve.getEquipmentState().getIntValue()),
            (shelve.getDueDate() == null ? null : shelve.getDueDate().getValue()),
            startTimestamp,
            startTimestamp};
    }


    private static Object[] prepareCPPortsParameters(String nodeId, CircuitPacks circuitPacks, Ports cpPort) {

        String circuitPackName = circuitPacks.getCircuitPackName();
        String portName = cpPort.getPortName();
        String portType = cpPort.getPortType();
        String portQualEnu = String.valueOf((cpPort.getPortQual() == null ? "-1" : cpPort.getPortQual().getName()));
        String portWavelengthTypeEnu = "-1"; //cpPort.getPortWavelengthType().getIntValue(); /* Check error*/
        String portDirectionEnu = String.valueOf((cpPort.getPortDirection() == null ? "" :
            cpPort.getPortDirection().getName()));
        String label = cpPort.getLabel();
        String circuitId = cpPort.getCircuitId();
        String administrativeStateEnu = (cpPort.getAdministrativeState() == null ? "" :
            cpPort.getAdministrativeState().getName());
        String operationalStateEnu =
            (cpPort.getOperationalState() == null ? "" : cpPort.getOperationalState().getName());
        String logicalConnectionPoint = cpPort.getLogicalConnectionPoint();
        String parentPortCircuitPackName = (cpPort.getPartnerPort() == null ? "" :
            (cpPort.getPartnerPort().getCircuitPackName() == null ? "" : cpPort.getPartnerPort().getCircuitPackName()));
        String partnerPortPortName = (cpPort.getPartnerPort() == null ? "" :
            (cpPort.getPartnerPort().getPortName() == null ? "" : cpPort.getPartnerPort().getPortName()));
        String partnerPortCircuitPackName = (cpPort.getParentPort() == null ? "" :
            (cpPort.getParentPort().getCircuitPackName() == null ? "" : cpPort.getParentPort().getCircuitPackName()));
        String parentPortPortName = (cpPort.getParentPort() == null ? "" :
            (cpPort.getParentPort().getPortName() == null ? "" : cpPort.getParentPort().toString()));
        String roadmPortPortPowerCapabilityMinRx = (cpPort.getRoadmPort() == null ? "" :
            (cpPort.getRoadmPort().getPortPowerCapabilityMinRx() == null ? "" :
                cpPort.getRoadmPort().getPortPowerCapabilityMinRx().toString()));
        String roadmPortPortPowerCapabilityMinTx = (cpPort.getRoadmPort() == null ? "" :
            (cpPort.getRoadmPort().getPortPowerCapabilityMinTx() == null ? "" :
                cpPort.getRoadmPort().getPortPowerCapabilityMinTx().toString()));
        String roadmPortPortPowerCapabilityMaxRx = (cpPort.getRoadmPort() == null ? "" :
            (cpPort.getRoadmPort().getPortPowerCapabilityMaxRx() == null ? "" :
                cpPort.getRoadmPort().getPortPowerCapabilityMaxRx().toString()));
        String roadmPortPortPowerCapabilityMaxTx = (cpPort.getRoadmPort() == null ? "" :
            (cpPort.getRoadmPort().getPortPowerCapabilityMaxTx() == null ? "" :
                cpPort.getRoadmPort().getPortPowerCapabilityMaxTx().toString()));
        //String roadmPortCapableWavelengths = "";
        //String roadmPortAvailableWavelengths = "";
        //String roadmPortUsedWavelengths = "";
        String transponderPortPortPowerCapabilityMinRx = (cpPort.getTransponderPort() == null ? "" :
            (cpPort.getTransponderPort().getPortPowerCapabilityMinRx() == null ? "" :
                cpPort.getTransponderPort().getPortPowerCapabilityMinRx().toString()));
        String transponderPortPortPowerCapabilityMinTx = (cpPort.getTransponderPort() == null ? "" :
            (cpPort.getTransponderPort().getPortPowerCapabilityMinTx() == null ? "" :
                cpPort.getTransponderPort().getPortPowerCapabilityMinTx().toString()));
        String transponderPortPortPowerCapabilityMaxRx = (cpPort.getTransponderPort() == null ? "" :
            (cpPort.getTransponderPort().getPortPowerCapabilityMaxRx() == null ? "" :
                cpPort.getTransponderPort().getPortPowerCapabilityMaxRx().toString()));
        String transponderPortPortPowerCapabilityMaxTx = (cpPort.getTransponderPort() == null ? "" :
            (cpPort.getTransponderPort().getPortPowerCapabilityMaxTx() == null ? "" :
                cpPort.getTransponderPort().getPortPowerCapabilityMaxTx().toString()));
        //String transponderPortCapableWavelengths = "";
        String otdrPortLaunchCableLength = (cpPort.getOtdrPort() == null ? "" :
            (cpPort.getOtdrPort().getLaunchCableLength() == null ? "" :
                cpPort.getOtdrPort().getLaunchCableLength().toString()));
        String otdrPortPortDirection = (cpPort.getOtdrPort() == null ? "-1" :
            (cpPort.getOtdrPort().getPortDirection() == null ? "-1" :
                Integer.toString(cpPort.getOtdrPort().getPortDirection().getIntValue())));
        //String ilaPortPortPowerCapabilityMixRx = "";
        //String ilaPortPortPowerCapabilityMixTx = "";
        //String ilaPortPortPowerCapabilityMaxRx = "";
        //String ilaPortPortPowerCapabilityMaxTx = "";

        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            circuitPackName,
            portName,
            portType,
            portQualEnu,
            portWavelengthTypeEnu,
            portDirectionEnu,
            label,
            circuitId,
            administrativeStateEnu,
            operationalStateEnu,
            logicalConnectionPoint,
            partnerPortCircuitPackName,
            partnerPortPortName,
            parentPortCircuitPackName,
            parentPortPortName,
            roadmPortPortPowerCapabilityMinRx,
            roadmPortPortPowerCapabilityMinTx,
            roadmPortPortPowerCapabilityMaxRx,
            roadmPortPortPowerCapabilityMaxTx,
            //roadmPortCapableWavelengths,
            //roadmPortAvailableWavelengths,
            //roadmPortUsedWavelengths,
            "", "", "",
            transponderPortPortPowerCapabilityMinRx,
            transponderPortPortPowerCapabilityMinTx,
            transponderPortPortPowerCapabilityMaxRx,
            transponderPortPortPowerCapabilityMaxTx,
            //transponderPortCapableWavelengths,
            "",
            otdrPortLaunchCableLength,
            otdrPortPortDirection,
            //ilaPortPortPowerCapabilityMixRx,
            //ilaPortPortPowerCapabilityMixTx,
            //ilaPortPortPowerCapabilityMaxRx,
            //ilaPortPortPowerCapabilityMaxTx,
            "", "", "", "",
            startTimestamp,
            startTimestamp
        };
    }


    private static Object[] prepareCircuitPacksParameters(String nodeId, CircuitPacks cpack) {
        String startTimestamp = getCurrentTimestamp();
        return new Object[]{nodeId,
            cpack.getCircuitPackName(),
            cpack.getCircuitPackType(),
            cpack.getCircuitPackProductCode(),
            (cpack.getAdministrativeState() == null ? "" : cpack.getAdministrativeState().getIntValue()),
            cpack.getVendor(),
            cpack.getModel(),
            cpack.getSerialId(),
            cpack.getType(),
            cpack.getProductCode(),
            (cpack.getManufactureDate() == null ? "" : cpack.getManufactureDate().getValue()),
            cpack.getClei(),
            cpack.getHardwareVersion(),
            (cpack.getOperationalState() == null ? -1 : cpack.getOperationalState().getIntValue()),
            cpack.getCircuitPackCategory().getType().getIntValue(),
            cpack.getCircuitPackCategory().getExtension(),
            (cpack.getEquipmentState() == null ? -1 : cpack.getEquipmentState().getIntValue()),
            cpack.getCircuitPackMode(),
            cpack.getShelf(),
            cpack.getSlot(),
            cpack.getSubSlot(),
            prepareEmptyString(cpack.getDueDate()),
            prepareEmptyString((cpack.getParentCircuitPack() == null) ? "" :
                ((cpack.getParentCircuitPack().getCircuitPackName() == null) ? "" :
                    cpack.getParentCircuitPack().getCircuitPackName())
            ),
            prepareEmptyString((cpack.getParentCircuitPack() == null) ? "" :
                ((cpack.getParentCircuitPack().getCpSlotName() == null) ? "" :
                    cpack.getParentCircuitPack().getCpSlotName())
            ),
            startTimestamp,
            startTimestamp};
    }


    private void persistCPPorts(String nodeId, Connection connection, CircuitPacks circuitPacks) {
        @NonNull
        Map<PortsKey, Ports> nonnullPorts = circuitPacks.nonnullPorts();
        for (Map.Entry<PortsKey, Ports> entry : nonnullPorts.entrySet()) {
            Object[] cpPortsParameters = prepareCPPortsParameters(nodeId, circuitPacks, entry.getValue());
            String query = Queries.getQuery().deviceCPPortInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement preparedStmt = connection.prepareStatement(query)) {
                for (int j = 0; j < cpPortsParameters.length; j++) {
                    preparedStmt.setObject(j + 1, cpPortsParameters[j]);
                }
                preparedStmt.execute();
                preparedStmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing Cirtcuits Packs Ports in DB", e);
            }
        }
    }


    private Object[] prepareDevInterfaceParameters(String nodeId, Interface deviceInterface, Connection connection) {

        String ethernetDuplexEnu = "";
        String ethernetAutoNegotiationEnu = "";
        String maintTestsignalTestpatternEnu = "";
        String maintTestsignalTypeEnu = "";
        String otuFecEnu = "";
        String otuMaintTypeEnu = "";
        //String otsFiberTypeEnu = "";
        String ethernetSpeed = "-1";
        String ethernetFec = "";
        String ethernetMtu = "-1";
        String ethernetCurrSpeed = "";
        String ethernetCurrDuplex = "-1";
        //String mciMcttpMinFreq = "";
        //String mciMcttpMaxFreq = "";
        //String mciMcttpCenterFreq = "";
        //String mciMcttpSlotWidth = "";
        //String mciNmcCtpFrequency = "";
        //String mciNmcCtpWidth = "";
        String ochRate = "";
        //String ochFrequency = "";
        //String ochWidth = "";
        String ochWavelengthNumber = "";
        String ochModulationFormat = "";
        String ochTransmitPower = "";
        String otsSpanLossReceive = "";
        String otsSpanLossTransmit = "";
        //String otsIngressSpanLossAgingMargin = "";
        //String otsEolMaxLoadPin = "";
        String oduRate = "";
        //String oduFunction = "";
        String oduMonitoringMode = "";
        //String oduNoOamFunction = "";
        String oduProactiveDelayMeasurementEnabled = "";
        //String oduPoaTribPortNumber = "-1";
        //String oduTxSapi = "";
        //String oduTxDapi = "";
        //String oduTxOperator = "";
        //String oduAcceptedSapi = "";
        //String oduAcceptedDapi = "";
        //String oduAcceptedOperator = "";
        //String oduExpectedSapi = "";
        //String oduExpectedDapi = "";
        //String oduTimActEnabled = "";
        //String oduTimDetectMode = "";
        //String oduDegmIntervals = "-1";
        //String oduDegthrPercentage = "-1";
        String opuPayloadType = "";
        String opuRxPayloadType = "";
        String opuExpPayloadType = "";
        String opuPayloadInterface = "";
        String maintTestsignalEnabled = "";
        String maintTestsignalBiterrors = "-1";
        String maintTestsignalBiterrorsterminal = "-1";
        String maintTestsignalSyncseconds = "-1";
        String maintTestsignalSyncsecondsterminal = "-1";
        String otuRate = "";
        //String otuTxSapi = "";
        //String otuTxDapi = "";
        //String otuTxOperator = "";
        //String otuAcceptedSapi = "";
        //String otuAcceptedDapi = "";
        //String otuAcceptedOperator = "";
        //String otuExpectedSapi = "";
        //String otuExpectedDapi = "";
        //String otuTimActEnabled = "";
        //String otuTimDetectMode = "";
        //String otuDegmIntervals = "-1";
        //String otuDegthrPercentage = "-1";
        String otuMaintLoopbackEnabled = "";
        //String mtOtuRate = "";
        //String mtOtuFec = "";
        //String mtOtuMaintLoopback = "";
        //String mtOtuEnabled = "";
        //String mtOtuType = "";

        String name = deviceInterface.getName();
        String description = deviceInterface.getDescription();
        String type = deviceInterface.getType().getTypeName();
        String administrativeStateEnu = deviceInterface.getAdministrativeState().getName();
        int operationalState = deviceInterface.getOperationalState().getIntValue();
        String circuitId = deviceInterface.getCircuitId();
        String supportingInterface = deviceInterface.getSupportingInterface();
        String supportingCircuitPackName = deviceInterface.getSupportingCircuitPackName();
        String supportingPort = deviceInterface.getSupportingPort();

        switch (deviceInterface.getType().toString()) {

            case "ethernet":
                //EthernetBuilder ethIfBuilder = new EthernetBuilder();
                EthernetBuilder ethIfBuilder = new EthernetBuilder(deviceInterface.augmentation(Interface1.class)
                    .getEthernet());
                ethernetSpeed = (ethIfBuilder.getSpeed() == null ? "-1" :
                    Integer.toString(ethIfBuilder.getSpeed().intValue()));
                ethernetFec = ethIfBuilder.getFec().getName();
                ethernetDuplexEnu = (ethIfBuilder.getDuplex() == null ? "" : ethIfBuilder.getDuplex().getName());
                ethernetMtu = ethIfBuilder.getMtu().toString();
                ethernetAutoNegotiationEnu = ethIfBuilder.getAutoNegotiation().getName();
                ethernetCurrSpeed = ethIfBuilder.getCurrSpeed();
                ethernetCurrDuplex = ethIfBuilder.getCurrDuplex();
                break;

            case "och":
                OchBuilder ochIfBuilder = new OchBuilder(deviceInterface.augmentation(
                    org.opendaylight.yang.gen.v1
                        .http.org.openroadm.optical.channel.interfaces.rev161014.Interface1.class)
                    .getOch());
                ochRate = ochIfBuilder.getRate().getName();
                ochWavelengthNumber = ochIfBuilder.getWavelengthNumber().toString();
                ochModulationFormat = ochIfBuilder.getModulationFormat().getName();
                ochTransmitPower = ochIfBuilder.getTransmitPower().toString();
                break;

            case "ots":
                OtsBuilder otsIfBuilder = new OtsBuilder(deviceInterface.augmentation(
                    org.opendaylight.yang.gen.v1
                        .http.org.openroadm.optical.transport.interfaces.rev161014.Interface1.class)
                    .getOts());
                //otsFiberTypeEnu = String.valueOf(otsIfBuilder.getFiberType().getIntValue());
                otsSpanLossReceive = otsIfBuilder.getSpanLossReceive().toString();
                otsSpanLossTransmit = otsIfBuilder.getSpanLossTransmit().toString();
                break;

            case "odu":
                OduBuilder oduIfBuilder = new OduBuilder(deviceInterface.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1.class)
                    .getOdu());
                oduRate = String.valueOf(oduIfBuilder.getRate());
                oduMonitoringMode = oduIfBuilder.getMonitoringMode().getName();
                oduProactiveDelayMeasurementEnabled = oduIfBuilder.getProactiveDelayMeasurementEnabled().toString();

                persistDevInterfaceTcm(nodeId, name, oduIfBuilder, connection);
                persistDevInterfaceOtnOduTxMsi(nodeId, name, oduIfBuilder, connection);
                persistDevInterfaceOtnOduRxMsi(nodeId, name, oduIfBuilder, connection);
                persistDevInterfaceOtnOduExpMsi(nodeId, name, oduIfBuilder, connection);

                opuPayloadType = oduIfBuilder.getOpu().getPayloadType();
                opuRxPayloadType = oduIfBuilder.getOpu().getRxPayloadType();
                opuExpPayloadType = oduIfBuilder.getOpu().getExpPayloadType();
                opuPayloadInterface = oduIfBuilder.getOpu().getPayloadInterface();
                        /*persistDevInterfaceOtnOduTxMsi(nodeId,name,oduIfBuilder,connection);
                        persistDevInterfaceOtnOduRxMsi(nodeId,name,oduIfBuilder,connection);
                        persistDevInterfaceOtnOduExpMsi(nodeId,name,oduIfBuilder,connection); */
                maintTestsignalEnabled = oduIfBuilder.getMaintTestsignal().getEnabled().toString();
                maintTestsignalTestpatternEnu = oduIfBuilder.getMaintTestsignal().getTestPattern().getName();
                maintTestsignalTypeEnu = oduIfBuilder.getMaintTestsignal().getType().getName();
                maintTestsignalBiterrors = Integer.toString(
                        oduIfBuilder.getMaintTestsignal().getBitErrors().intValue());
                maintTestsignalBiterrorsterminal = oduIfBuilder.getMaintTestsignal().getBitErrorsTerminal().toString();
                maintTestsignalSyncseconds = oduIfBuilder.getMaintTestsignal().getSyncSeconds();
                maintTestsignalSyncsecondsterminal = oduIfBuilder.getMaintTestsignal().getSyncSecondsTerminal();
                break;

            case "otu":
                OtuBuilder otuIfBuilder =
                    new OtuBuilder(deviceInterface.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1.class)
                    .getOtu());
                otuRate = otuIfBuilder.getRate().getName();
                otuFecEnu = otuIfBuilder.getFec().getName();
                otuMaintLoopbackEnabled = otuIfBuilder.getMaintLoopback().getEnabled().toString();
                otuMaintTypeEnu = otuIfBuilder.getMaintLoopback().getType().getName();
                break;

            default:
                LOG.error("could not get interface type");

        }

        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            name,
            description,
            type,
            administrativeStateEnu,
            Integer.toString(operationalState),
            circuitId,
            supportingInterface,
            supportingCircuitPackName,
            supportingPort,
            ethernetSpeed,
            ethernetFec,
            ethernetDuplexEnu,
            ethernetMtu,
            ethernetAutoNegotiationEnu,
            ethernetCurrSpeed,
            ethernetCurrDuplex,
            //mciMcttpMinFreq,
            //mciMcttpMaxFreq,
            //mciMcttpCenterFreq,
            //mciMcttpSlotWidth,
            //mciNmcCtpFrequency,
            //mciNmcCtpWidth,
            "", "", "", "", "", "",
            ochRate,
            //ochFrequency,
            //ochWidth,
            "", "",
            ochWavelengthNumber,
            ochModulationFormat,
            ochTransmitPower,
            //otsFiberTypeEnu,
            otsSpanLossReceive,
            otsSpanLossTransmit,
            //otsIngressSpanLossAgingMargin,
            //otsEolMaxLoadPin,
            "", "",
            oduRate,
            //oduFunction,
            "",
            oduMonitoringMode,
            //oduNoOamFunction,
            "",
            oduProactiveDelayMeasurementEnabled,
            //oduPoaTribPortNumber,
            //oduTxSapi,
            //oduTxDapi,
            //oduTxOperator,
            //oduAcceptedSapi,
            //oduAcceptedDapi,
            //oduAcceptedOperator,
            //oduExpectedSapi,
            //oduExpectedDapi,
            //oduTimActEnabled,
            //oduTimDetectMode,
            //oduDegmIntervals,
            //oduDegthrPercentage,
            "-1", "", "", "", "", "","", "", "", "", "", "-1", "-1",
            opuPayloadType,
            opuRxPayloadType,
            opuExpPayloadType,
            opuPayloadInterface,
            maintTestsignalEnabled,
            maintTestsignalTestpatternEnu,
            maintTestsignalTypeEnu,
            maintTestsignalBiterrors,
            maintTestsignalBiterrorsterminal,
            maintTestsignalSyncseconds,
            maintTestsignalSyncsecondsterminal,
            otuRate,
            otuFecEnu,
            //otuTxSapi,
            //otuTxDapi,
            //otuTxOperator,
            //otuAcceptedSapi,
            //otuAcceptedDapi,
            //otuAcceptedOperator,
            //otuExpectedSapi,
            //otuExpectedDapi,
            //otuTimActEnabled,
            //otuTimDetectMode,
            //otuDegmIntervals,
            //otuDegthrPercentage,
            "", "", "", "", "", "","", "", "", "", "-1", "-1",
            otuMaintLoopbackEnabled,
            otuMaintTypeEnu,
            //mtOtuRate,
            //mtOtuFec,
            //mtOtuMaintLoopback,
            //mtOtuEnabled,
            //mtOtuType,
            "", "", "", "", "",
            startTimestamp,
            startTimestamp
        };

    }

    private static Object[] prepareDevInterfaceTcmParameters(String nodeId, String interfaceName, Tcm tcm) {

        String layer = tcm.getLayer().toString();
        String monitoringModeEnu = tcm.getMonitoringMode().getName();
        String ltcActEnabled = tcm.getLtcActEnabled().toString();
        String proactiveDelayMeasurementEnabled = tcm.getProactiveDelayMeasurementEnabled().toString();
        //String tcmDirectionEnu = "";
        //String timDetectModeEnu = "";
        //String txSapi = "";
        //String txDapi = "";
        //String txOperator = "";
        //String acceptedSapi = "";
        //String acceptedDapi = "";
        //String acceptedOperator = "";
        //String expectedSapi = "";
        //String expectedDapi = "";
        //String timActEnabled = "";
        //String degmIntervals = "";
        //String degthrPercentage = "";
        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            interfaceName,
            layer,
            monitoringModeEnu,
            ltcActEnabled,
            proactiveDelayMeasurementEnabled,
            //tcmDirectionEnu,
            //txSapi,
            //txDapi,
            //txOperator,
            //acceptedSapi,
            //acceptedDapi,
            //acceptedOperator,
            //expectedSapi,
            //expectedDapi,
            //timActEnabled,
            //timDetectModeEnu,
            //degmIntervals,
            //degthrPercentage,
            "", "", "", "", "", "", "", "", "", "", "", "", "",
            startTimestamp,
            startTimestamp};

    }

    private static Object[] prepareDevInterfaceOtnOduTxMsiParameters(String nodeId, String interfaceName, TxMsi txMsi) {

        String tribSlot = txMsi.getTribSlot().toString();
        String odtuType = txMsi.getOdtuType().getTypeName();
        String tribPort = txMsi.getTribPort().toString();
        String tribPortPayload = txMsi.getTribPortPayload();

        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            interfaceName,
            tribSlot,
            odtuType,
            tribPort,
            tribPortPayload,
            startTimestamp,
            startTimestamp
        };

    }

    private static Object[] prepareDevInterfaceOtnOduRxMsiParameters(String nodeId, String interfaceName, RxMsi rxMsi) {

        String tribSlot = rxMsi.getTribSlot().toString();
        String odtuType = rxMsi.getOdtuType().getTypeName();
        String tribPort = rxMsi.getTribPort().toString();
        String tribPortPayload = rxMsi.getTribPortPayload();

        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            interfaceName,
            tribSlot,
            odtuType,
            tribPort,
            tribPortPayload,
            startTimestamp,
            startTimestamp
        };

    }


    private static Object[] prepareDevInterfaceOtnOduExpMsiParameters(String nodeId, String interfaceName,
        ExpMsi expMsi) {

        String tribSlot = expMsi.getTribSlot().toString();
        String odtuType = expMsi.getOdtuType().getTypeName();
        String tribPort = expMsi.getTribPort().toString();
        String tribPortPayload = expMsi.getTribPortPayload();

        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            interfaceName,
            tribSlot,
            odtuType,
            tribPort,
            tribPortPayload,
            startTimestamp,
            startTimestamp
        };

    }

    private void persistDevInterfaces(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            return;
        }
        Map<InterfaceKey, Interface> interfaceMap = deviceObject.get().nonnullInterface();
        for (Map.Entry<InterfaceKey, Interface> interfaceEntrySet : interfaceMap.entrySet()) {
            Interface deviceInterface = interfaceEntrySet.getValue();
            Object[] parameters = prepareDevInterfaceParameters(nodeId, deviceInterface, connection);

            String query = Queries.getQuery().deviceInterfacesInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices interfaces in DB", e);
            }
        }
    }

    private void persistDevProtocols(String nodeId, Connection connection) {

        InstanceIdentifier<Protocols> protocolsIID =
                InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Protocols.class);
        Optional<Protocols> protocolObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, protocolsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!protocolObject.isPresent() || protocolObject.get().augmentation(Protocols1.class) == null) {
            LOG.error("LLDP subtree is missing");
            return;
        }
        String adminstatusEnu = protocolObject.get().augmentation(Protocols1.class).getLldp().getGlobalConfig()
            .getAdminStatus().getName();
        String msgTxtInterval = protocolObject.get().augmentation(Protocols1.class).getLldp().getGlobalConfig()
            .getMsgTxInterval().toString();
        String mxgTxHoldMultiplier = protocolObject.get().augmentation(Protocols1.class).getLldp().getGlobalConfig()
            .getMsgTxHoldMultiplier().toString();
        String startTimestamp = getCurrentTimestamp();
        persistDevProtocolLldpPortConfig(nodeId, connection);
        persistDevProtocolLldpNbrList(nodeId, connection);

        Object[] parameters = {nodeId,
            adminstatusEnu,
            msgTxtInterval,
            mxgTxHoldMultiplier,
            startTimestamp,
            startTimestamp
        };

        String query = Queries.getQuery().deviceProtocolInsert().get();
        LOG.info("Running {} query ", query);
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int j = 0; j < parameters.length; j++) {
                stmt.setObject(j + 1, parameters[j]);
            }
            stmt.execute();
            stmt.clearParameters();
        } catch (SQLException e) {
            LOG.error("Something wrong when storing devices protocols in DB", e);
        }
    }


    private void persistDevProtocolLldpPortConfig(String nodeId, Connection connection) {

        InstanceIdentifier<Protocols> protocolsIID =
                InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Protocols.class);
        Optional<Protocols> protocolObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, protocolsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!protocolObject.isPresent() || protocolObject.get().augmentation(Protocols1.class) == null) {
            LOG.error("LLDP subtree is missing");
            return;
        }
        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<PortConfigKey, PortConfig> portConfigMap = protocolObject.get()
            .augmentation(Protocols1.class).getLldp().nonnullPortConfig();
        for (Map.Entry<PortConfigKey, PortConfig> entry : portConfigMap.entrySet()) {
            PortConfig portConfig = entry.getValue();
            String ifName = portConfig.getIfName();
            String adminStatusEnu = portConfig.getAdminStatus().getName();

            Object[] parameters = {nodeId,
                ifName,
                adminStatusEnu,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceProtocolPortConfigInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices protocols LLDP Port config in DB", e);
            }

        }

    }

    private void persistDevProtocolLldpNbrList(String nodeId, Connection connection) {

        InstanceIdentifier<Protocols> protocolsIID =
                InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Protocols.class);
        Optional<Protocols> protocolObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, protocolsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!protocolObject.isPresent()) {
            LOG.error("Protocols is missing");
            return;
        }
        if (protocolObject.get().augmentation(Protocols1.class).getLldp().getNbrList() == null) {
            protocolObject =
                    deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, protocolsIID,
                            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (protocolObject.get().augmentation(Protocols1.class).getLldp().getNbrList() == null) {
                LOG.error("LLDP nbrlist subtree is missing for {}", nodeId);
                return;
            }
        }
        String startTimestamp = getCurrentTimestamp();
        Map<IfNameKey, IfName> ifNameMap =
                protocolObject.get().augmentation(Protocols1.class).getLldp().getNbrList().nonnullIfName();
        for (Map.Entry<IfNameKey, IfName> ifNameEntry : ifNameMap.entrySet()) {

            IfName ifNameObj = ifNameEntry.getValue();
            String ifName = ifNameObj.getIfName();
            String remotesysname = ifNameObj.getRemoteSysName();
            String remotemgmtaddresssubtype = ifNameObj.getRemoteMgmtAddressSubType().getName();
            String remotemgmtaddress = ifNameObj.getRemoteMgmtAddress().getIpv4Address().toString();
            String remoteportidsubtypeEnu = ifNameObj.getRemotePortIdSubType().getName();
            String remoteportid = ifNameObj.getRemotePortId();
            String remotechassisidsubtypeEnu = ifNameObj.getRemoteChassisIdSubType().getName();
            String remotechassisid = ifNameObj.getRemoteChassisId();

            Object[] parameters = {nodeId,
                ifName,
                remotesysname,
                remotemgmtaddresssubtype,
                remotemgmtaddress,
                remoteportidsubtypeEnu,
                remoteportid,
                remotechassisidsubtypeEnu,
                remotechassisid,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceProtocolLldpNbrlistInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices protocols LLDP list number in DB", e);
            }

        }
    }

    private void persistDevInternalLinks(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            return;
        }
        if (deviceObject.get().getInternalLink() == null) {
            deviceObject = deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION,
                    deviceIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (deviceObject.get().getInternalLink() == null) {
                LOG.info("External links not found for {}", nodeId);
                return;
            }
        }
        @NonNull
        Map<InternalLinkKey, InternalLink> internalLinkMap = deviceObject.get().nonnullInternalLink();
        String startTimestamp = getCurrentTimestamp();
        for (Map.Entry<InternalLinkKey, InternalLink> internalLinkEntry: internalLinkMap.entrySet()) {
            InternalLink internalLink = internalLinkEntry.getValue();
            String internalLinkName = internalLink.getInternalLinkName();
            String sourceCircuitPackName = internalLink.getSource().getCircuitPackName();
            String sourcePortName = internalLink.getSource().getPortName();
            String destinationCircuitPackName = internalLink.getDestination().getCircuitPackName();
            String destinationPortName = internalLink.getDestination().getPortName();

            Object[] parameters = { nodeId, internalLinkName, sourceCircuitPackName, sourcePortName,
                destinationCircuitPackName, destinationPortName, startTimestamp, startTimestamp };
            String query = Queries.getQuery().deviceInternalLinkInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices internal links", e);
            }
        }
    }


    private void persistDevExternalLinks(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            return;
        }
        if (deviceObject.get().getExternalLink() == null) {
            deviceObject = deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION,
                    deviceIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (deviceObject.get().getExternalLink() == null) {
                LOG.info("External links not found for {}", nodeId);
                return;
            }
        }
        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<ExternalLinkKey, ExternalLink> externalLinkMap = deviceObject.get().nonnullExternalLink();
        for (Map.Entry<ExternalLinkKey, ExternalLink> externalLinkEntry: externalLinkMap.entrySet()) {
            ExternalLink externalLink = externalLinkEntry.getValue();
            String externalLinkName = externalLink.getExternalLinkName();
            String sourceNodeId = externalLink.getSource().getNodeId();
            String sourceCircuitPackName = externalLink.getSource().getCircuitPackName();
            String sourcePortName = externalLink.getSource().getPortName();
            String destinationNodeId = externalLink.getDestination().getNodeId();
            String destinationCircuitPackName = externalLink.getDestination().getCircuitPackName();
            String destinationPortName = externalLink.getDestination().getPortName();

            Object[] parameters = { nodeId, externalLinkName, sourceNodeId, sourceCircuitPackName,
                sourcePortName, destinationNodeId, destinationCircuitPackName, destinationPortName,
                startTimestamp, startTimestamp };

            String query = Queries.getQuery().deviceExternalLinkInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices external links", e);
            }
        }
    }

    private void persistDevPhysicalLinks(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.error("No device with node Id {}", nodeId);
            return;
        }
        if (deviceObject.get().getPhysicalLink() == null) {
            deviceObject =
                    deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, deviceIID,
                            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!deviceObject.isPresent()) {
                LOG.error("No device with node Id {}", nodeId);
                return;
            }
            if (deviceObject.get().getPhysicalLink() == null) {
                LOG.info("Physical links not found for {}", nodeId);
                return;
            }
        }

        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<PhysicalLinkKey, PhysicalLink> physicalLinkMap = deviceObject.get().nonnullPhysicalLink();
        for (Map.Entry<PhysicalLinkKey, PhysicalLink> physicalLinkEntry : physicalLinkMap.entrySet()) {
            PhysicalLink physicalLink = physicalLinkEntry.getValue();
            String physicalLinkName = physicalLink.getPhysicalLinkName();
            String sourceCircuitPackName = physicalLink.getSource().getCircuitPackName();
            String sourcePortName = physicalLink.getSource().getPortName();
            String destinationCircuitPackName = physicalLink.getDestination().getCircuitPackName();
            String destinationPortName = physicalLink.getDestination().getPortName();

            Object[] parameters = {nodeId,
                physicalLinkName,
                sourceCircuitPackName,
                sourcePortName,
                destinationCircuitPackName,
                destinationPortName,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().devicePhysicalLinkInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices physical links", e);
            }

        }
    }

    private void persistDevDegree(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);


        /*if (deviceObject.get().getDegree()==null){
            deviceObject =
                    deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, deviceIID,
                            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        } */
        if (!deviceObject.isPresent()) {
            LOG.error("Cannot get device for node {}", nodeId);
            return;
        }
        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<DegreeKey, Degree> degreeMap = deviceObject.get().nonnullDegree();
        for (Map.Entry<DegreeKey, Degree> degreeEntry : degreeMap.entrySet()) {
            Degree degree = degreeEntry.getValue();
            String degreeNumber = degree.getDegreeNumber().toString();
            String maxWavelengths = degree.getMaxWavelengths().toString();
            String otdrPortCircuitPackName =
                    (degree.getOtdrPort() == null ? "" : degree.getOtdrPort().getCircuitPackName());
            String otdrPortPortName =
                    (degree.getOtdrPort() == null ? "" : degree.getOtdrPort().getPortName());
            // String mcCapabilitiesSlotWidthGranularity = "";
            // String mcCapabilitiesCenterFreqGranularity = "";
            // String mcCapabilitiesMinSlots = "-1";
            // String mcCapabilitiesMaxSlots = "-1";
            persistDevDegreeCircuitPack(nodeId, degree, degreeNumber, connection);
            persistDevDegreeConnectionPort(nodeId, degree, degreeNumber, connection);

            Object[] parameters = { nodeId, degreeNumber, maxWavelengths, otdrPortCircuitPackName, otdrPortPortName,
                    // mcCapabilitiesSlotWidthGranularity,
                    // mcCapabilitiesCenterFreqGranularity,
                    // mcCapabilitiesMinSlots,
                    // mcCapabilitiesMaxSlots,
                "", "", "-1", "-1", startTimestamp, startTimestamp };

            String query = Queries.getQuery().deviceDegreeInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices degrees", e);
            }

        }
    }


    private void persistDevDegreeCircuitPack(String nodeId, Degree degree, String degreeNumber, Connection connection) {

        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacksKey,
                org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks>
            circuitPacksMap = degree.nonnullCircuitPacks();
        for (Map.Entry<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacksKey,
                org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks> circuitPackEntry :
                    circuitPacksMap.entrySet()) {

            String circuitPackIndex = circuitPackEntry.getValue().getIndex().toString();
            String circuitPackName = circuitPackEntry.getValue().getCircuitPackName();

            Object[] parameters = {nodeId,
                degreeNumber,
                circuitPackIndex,
                circuitPackName,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceDegreeCircuitPackInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices degrees circuit packs", e);
            }

        }
    }

    private void persistDevDegreeConnectionPort(String nodeId, Degree degree, String degreeNumber,
        Connection connection) {

        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<ConnectionPortsKey, ConnectionPorts> connectionPortsMap = degree.nonnullConnectionPorts();
        for (Map.Entry<ConnectionPortsKey, ConnectionPorts> portEntry : connectionPortsMap.entrySet()) {
            String connectionPortIndex = portEntry.getValue().getIndex().toString();
            String circuitPackName = portEntry.getValue().getCircuitPackName();
            String portName = portEntry.getValue().getPortName();

            Object[] parameters = {nodeId,
                degreeNumber,
                connectionPortIndex,
                circuitPackName,
                portName,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceDegreeConnectionPortInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices degrees connection ports", e);
            }

        }
    }


    private void persistDevSrg(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.error("No device found in operational datastore for node {}", nodeId);
            return;
        }

        if (deviceObject.get().getSharedRiskGroup() == null) {
            deviceObject =
                    deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, deviceIID,
                            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!deviceObject.isPresent()) {
                LOG.error("No device found in configuration datastore for node {}", nodeId);
                return;
            }
        }

        @NonNull
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = deviceObject.get().nonnullSharedRiskGroup();
        if (sharedRiskGroupMap.isEmpty()) {
            LOG.info("no srg found for node {} ", nodeId);
            return;
        }
        String startTimestamp = getCurrentTimestamp();
        for (Map.Entry<SharedRiskGroupKey, SharedRiskGroup> groupEntry : sharedRiskGroupMap.entrySet()) {
            SharedRiskGroup sharedRiskGroup = groupEntry.getValue();
            //String currentProvisionedAddDropPorts = "-1";
            //String mcCapSlotWidthGranularity = "";
            //String mcCapCenterFreqGranularity = "";
            //String mcCapMinSlots = "-1";
            //String mcCapMaxSlots = "-1";
            String maxAddDropPorts = sharedRiskGroup.getMaxAddDropPorts().toString();
            String srgNumber = sharedRiskGroup.getSrgNumber().toString();
            String wavelengthDuplicationEnu = sharedRiskGroup.getWavelengthDuplication().getName();
            persistDevSrgCircuitPacks(nodeId, sharedRiskGroup, srgNumber, connection);

            Object[] parameters = {nodeId,
                maxAddDropPorts,
                //currentProvisionedAddDropPorts,
                "-1",
                srgNumber,
                wavelengthDuplicationEnu,
                //mcCapSlotWidthGranularity,
                //mcCapCenterFreqGranularity,
                //mcCapMinSlots,
                //mcCapMaxSlots,
                "", "", "", "",
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceSharedRiskGroupInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices SRG", e);
            }

        }
    }


    private void persistDevSrgCircuitPacks(String nodeId, SharedRiskGroup sharedRiskGroup, String srgNumber,
        Connection connection) {

        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacksKey,
                org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks>
                circuitPacksMap = sharedRiskGroup.nonnullCircuitPacks();
        for (Map.Entry<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacksKey,
                org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> entry :
                    circuitPacksMap.entrySet()) {

            String circuitPackindex = entry.getValue().getIndex().toString();
            String circuitPackName = entry.getValue().getCircuitPackName();

            Object[] parameters = {nodeId,
                srgNumber,
                circuitPackindex,
                circuitPackName,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceSrgCircuitPackInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices SRG circuit packs", e);
            }

        }
    }

    private void persistDevRoadmConnections(String nodeId, Connection connection) {

        //int opticalcontrolmodeEnu=-1;

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.error("No device found in operational datastore for node {}", nodeId);
            return;
        }
        if (deviceObject.get().getRoadmConnections() == null) {
            deviceObject =
                    deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, deviceIID,
                            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!deviceObject.isPresent()) {
                LOG.error("No device found in configuration datastore for node {}", nodeId);
                return;
            }
        }

        @NonNull
        Map<RoadmConnectionsKey, RoadmConnections> connectionsMap = deviceObject.get().nonnullRoadmConnections();
        if (connectionsMap.isEmpty()) {
            LOG.info("ROADM Dev Connections not found!! for {}", nodeId);
            return;
        }
        String startTimestamp = getCurrentTimestamp();
        for (Map.Entry<RoadmConnectionsKey, RoadmConnections> entry : connectionsMap.entrySet()) {
            RoadmConnections roadmConnections = entry.getValue();
            String connectionNumber = roadmConnections.getConnectionNumber();
            //String connectionName = "";
            String wavelengthNumber = roadmConnections.getWavelengthNumber().toString();
            String opticalcontrolmodeEnu = roadmConnections.getOpticalControlMode().getName();
            String targetOutputPower = roadmConnections.getTargetOutputPower().toString();
            String srcIf = roadmConnections.getSource().getSrcIf();
            String dstIf = roadmConnections.getDestination().getDstIf();

            Object[] parameters = {nodeId,
                //connectionName,
                "",
                connectionNumber,
                wavelengthNumber,
                opticalcontrolmodeEnu,
                targetOutputPower,
                srcIf,
                dstIf,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceRoadmConnectionsInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices ROADM connection ", e);
            }
        }
    }


    private void persistDevConnectionMap(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.error("No device found in operational datastore for node {}", nodeId);
            return;
        }
        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<ConnectionMapKey, ConnectionMap> connectionsMap = deviceObject.get().nonnullConnectionMap();
        for (Map.Entry<ConnectionMapKey, ConnectionMap> entry : connectionsMap.entrySet()) {
            ConnectionMap connectionMap = entry.getValue();
            String connectionMapNumber = connectionMap.getConnectionMapNumber().toString();
            String sourceCircuitPackName = connectionMap.getSource().getCircuitPackName();
            String sourcePortName = connectionMap.getSource().getCircuitPackName();


            Object[] parameters = {nodeId,
                connectionMapNumber,
                sourceCircuitPackName,
                sourcePortName,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceConnectionMapInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices connection map", e);
            }

        }
    }

    private void persistDevWavelengthMap(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.error("No device found in operational datastore for node {}", nodeId);
            return;
        }

        String startTimestamp = getCurrentTimestamp();
        @NonNull
        Map<WavelengthsKey, Wavelengths> wavelengthsMap = deviceObject.get().getWavelengthMap().nonnullWavelengths();
        for (Map.Entry<WavelengthsKey, Wavelengths> entry : wavelengthsMap.entrySet()) {
            Wavelengths wavelengths = entry.getValue();
            String wavelengthNumber = wavelengths.getWavelengthNumber().toString();
            String centerFrequency = wavelengths.getCenterFrequency().toString();
            String wavelength = wavelengths.getWavelength().toString();


            Object[] parameters = {nodeId,
                wavelengthNumber,
                centerFrequency,
                wavelength,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceWavelengthInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices wavelength map", e);
            }

        }
    }


    private void persistDevInterfaceTcm(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {

        Map<TcmKey, Tcm> tcmMap = oduBuilder.getTcm();
        for (Map.Entry<TcmKey, Tcm> entry :  tcmMap.entrySet()) {
            Tcm tcm = entry.getValue();

            Object[] parameters = prepareDevInterfaceTcmParameters(nodeId, interfaceName, tcm);

            String query = Queries.getQuery().deviceInterfacesInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices interface tcm", e);
            }
        }
    }

    private void persistDevInterfaceOtnOduTxMsi(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {

        Map<TxMsiKey, TxMsi> txMsiMap = oduBuilder.getOpu().getMsi().nonnullTxMsi();
        for (Map.Entry<TxMsiKey, TxMsi> entry :  txMsiMap.entrySet()) {

            TxMsi txMsi = entry.getValue();

            Object[] parameters = prepareDevInterfaceOtnOduTxMsiParameters(nodeId, interfaceName, txMsi);

            String query = Queries.getQuery().deviceInterfaceOtnOduTxMsiInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices interface OTN ODU Tx MSI", e);
            }
        }
    }


    private void persistDevInterfaceOtnOduRxMsi(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {
        Map<RxMsiKey, RxMsi> rxMsiMap = oduBuilder.getOpu().getMsi().nonnullRxMsi();
        for (Map.Entry<RxMsiKey, RxMsi> entry : rxMsiMap.entrySet()) {
            RxMsi rxMsi = entry.getValue();

            Object[] parameters = prepareDevInterfaceOtnOduRxMsiParameters(nodeId, interfaceName, rxMsi);

            String query = Queries.getQuery().deviceInterfaceOtnOduRxMsiInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices interface OTN ODU Rx MSI", e);
            }
        }
    }


    private void persistDevInterfaceOtnOduExpMsi(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {
        @NonNull
        Map<ExpMsiKey, ExpMsi> expMsiMap = oduBuilder.getOpu().getMsi().nonnullExpMsi();
        for (Map.Entry<ExpMsiKey, ExpMsi> entry : expMsiMap.entrySet()) {
            ExpMsi expMsi = entry.getValue();

            Object[] parameters = prepareDevInterfaceOtnOduExpMsiParameters(nodeId, interfaceName, expMsi);

            String query = Queries.getQuery().deviceInterfaceOtnOduExpMsiInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Something wrong when storing devices interface OTN ODU Exp MSI", e);
            }
        }
    }


}
