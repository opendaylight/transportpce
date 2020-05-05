/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.sql.DataSource;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.inventory.query.Queries;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CpSlots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.external.links.ExternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.internal.links.InternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.physical.links.PhysicalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelf.Slots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelves.Shelves;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfig;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.nbr.list.IfName;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.attributes.Tcm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.ExpMsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.RxMsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.opu.opu.msi.TxMsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.otu.container.OtuBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class INode221 {
    private static final Logger LOG = LoggerFactory.getLogger(INode221.class);

    private final DataSource dataSource;
    private final DeviceTransactionManager deviceTransactionManager;

    public INode221(DataSource dataSource, DeviceTransactionManager deviceTransactionManager) {
        this.dataSource = dataSource;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public boolean addNode(String deviceId) {

        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> infoOpt =
                deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, infoIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        Info deviceInfo;
        if (infoOpt.isPresent()) {
            deviceInfo = infoOpt.get();
        } else {
            LOG.warn("Could not get device info from DataBroker");
            return false;
        }
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

            LOG.info("iNode AddNode call complete");
            getRoadmShelves(deviceId);
            LOG.info("iNode getRoadmShelves call complete");
            getCircuitPacks(deviceId);
            LOG.debug("iNode getCircuitPacks call complete");

            LOG.debug("iNode persist interfaces call");
            persistDevInterfaces(deviceId, connection);
            LOG.debug("iNode persist interfaces call complete");

            LOG.debug("iNode persist interfaces call");
            persistDevInterfaces(deviceId, connection);
            LOG.debug("iNode persist interfaces call complete");

            LOG.debug("iNode persist protocols call");
            persistDevProtocols(deviceId, connection);
            LOG.debug("iNode persist protocols call complete");

            // LOG.debug("iNode persist wavelength map call");
            // persistDevWavelengthMap(deviceId, connection);
            // LOG.debug("iNode persist wavelength map call complete");

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

        } catch (SQLException e) {
            LOG.error("SQL exception ", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted exception ", e);
        } catch (ExecutionException e) {
            LOG.error("Execution exception ", e);
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
            LOG.error("SQL exception ", e);
        }
        return nodeExists == 0 ? false : true;
    }

    public void getRoadmShelves(String nodeId) throws InterruptedException, ExecutionException {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject = deviceTransactionManager.getDataFromDevice(nodeId,
                LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        LOG.info("Shelves size {}", deviceObject.get().getShelves().size());
        try (Connection connection = requireNonNull(dataSource.getConnection())) {
            for (int i = 0; i < deviceObject.get().getShelves().size(); i++) {
                Shelves shelve = deviceObject.get().getShelves().get(i);
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
            LOG.error("SQL exception ", e1);
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
        LOG.info("Circuit pack size {}", deviceObject.get().getCircuitPacks().size());

        try (Connection connection = requireNonNull(dataSource.getConnection())) {
            for (int i = 0; i < deviceObject.get().getCircuitPacks().size(); i++) {
                CircuitPacks cp = deviceObject.get().getCircuitPacks().get(i);

                if (cp.getCpSlots() != null) {
                    persistCircuitPacksSlots(nodeId, cp, connection);
                }
                LOG.info("Everything {}", cp);
                LOG.info("CP is {}", cp);

                persistPorts(cp, connection);

                persistCircuitPacks(nodeId, connection, cp);
            }
        } catch (SQLException e1) {
            LOG.error("SQL exception ", e1);
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
            LOG.error("SQL exception ", e);
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
            LOG.error("SQL exception ", e);
        }
    }

    private void persistShelveSlots(String nodeId, Shelves shelves, Connection connection) {
        String startTimetampStr = getCurrentTimestamp();
        for (int i = 0; i < shelves.getSlots().size(); i++) {
            Slots slot = shelves.getSlots().get(i);
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
                LOG.error("SQL exception ", e);
            }
        }
    }


    private void persistCircuitPacksSlots(String nodeId, CircuitPacks circuitPacks, Connection connection) {
        String startTimetampStr = getCurrentTimestamp();
        for (int i = 0; i < circuitPacks.getCpSlots().size(); i++) {
            CpSlots cpSlot = circuitPacks.getCpSlots().get(i);

            Object[] parameters = new Object[]{nodeId,
                circuitPacks.getCircuitPackName(),
                cpSlot.getSlotName(),
                cpSlot.getLabel(),
                cpSlot.getProvisionedCircuitPack(),
                null,
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
                LOG.error("SQL exception ", e);
            }
        }
    }

    private void persistPorts(CircuitPacks circuitPacks, Connection connection) {
        LOG.warn("Ports are not persisted yet");
    }


    /**
     * Prepares parameters for device insert query.
     *
     * @param deviceInfo device info
     * @return Object Object
     */
    private static Object[] prepareDeviceInfoParameters(Info deviceInfo) {
        String startTimetampStr = getCurrentTimestamp();
        //Integer maxNumBin15minHistoricalPm = null;
        //Integer maxNumBin24hourHistoricalPm = null;
        //String serialId = "";

        String nodeId = prepareDashString(deviceInfo.getNodeId());
        Long nodeNumber = deviceInfo.getNodeNumber().toJava();
        Integer nodeTypeEnu = deviceInfo.getNodeType().getIntValue();
        String clli = prepareDashString(deviceInfo.getClli());
        String vendor = prepareDashString(deviceInfo.getVendor());
        String model = prepareDashString(deviceInfo.getModel());
        String ipAddress = prepareDashString(deviceInfo.getIpAddress().getIpv4Address().getValue());
        String prefixLength = prepareDashString(deviceInfo.getPrefixLength());
        String defaultGateway = prepareDashString(deviceInfo.getDefaultGateway().getIpv4Address().getValue());
        Integer sourceEnum = deviceInfo.getSource().getIntValue();
        String currentIpAddress = prepareDashString(deviceInfo.getCurrentIpAddress().getIpv4Address().getValue());
        String currentPrefixLength = prepareDashString(deviceInfo.getCurrentPrefixLength());
        String currentDefaultGateway = prepareDashString(deviceInfo.getDefaultGateway().getIpv4Address().getValue());
        String macAddress = prepareDashString(deviceInfo.getMacAddress().getValue());
        String softwareVersion = prepareDashString(deviceInfo.getSoftwareVersion());
        String openroadmVersion = "2.2.1";
        String template = prepareDashString(deviceInfo.getTemplate());
        String currentDatetime = prepareDashString(deviceInfo.getCurrentDatetime().getValue());
        String geoLatitude =
            (deviceInfo.getGeoLocation() != null ? prepareDashString(deviceInfo.getGeoLocation().getLatitude()) : "");
        String geoLongitude =
            (deviceInfo.getGeoLocation() != null ? prepareDashString(deviceInfo.getGeoLocation().getLongitude()) : "");
        String maxDegrees = prepareDashString(deviceInfo.getMaxDegrees()); // max_degrees
        String maxSrgs = prepareDashString(deviceInfo.getMaxSrgs()); //max_srgs
        String swVersion = prepareDashString(deviceInfo.getSoftwareVersion()); //sw_version
        String swValidationTimer = prepareDashString(""); //sw_validation_timer
        String activationDateTime = prepareDashString(""); //activation_date_time
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
            //serialId,
            "",
            ipAddress,
            prefixLength,
            defaultGateway,
            sourceEnum,
            currentIpAddress,
            currentPrefixLength,
            currentDefaultGateway,
            macAddress,
            softwareVersion,
            openroadmVersion,
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
            prepareEmptyString(cpack.getOperationalState().getIntValue()),
            cpack.getCircuitPackCategory().getType().getName(),
            cpack.getCircuitPackCategory().getExtension(),
            (cpack.getEquipmentState() == null ? "" : cpack.getEquipmentState().getIntValue()),
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

    private Object[] prepareDevInterfaceParameters(String nodeId, Interface deviceInterface, Connection connection) {

        int administrativeStateEnu = deviceInterface.getAdministrativeState().getIntValue();
        int operationalState = deviceInterface.getOperationalState().getIntValue();
        int ethernetDuplexEnu = -1;
        int ethernetAutoNegotiationEnu = -1;
        int maintTestsignalTestpatternEnu = -1;
        int maintTestsignalTypeEnu = -1;
        int otuFecEnu = -1;
        int otuMaintTypeEnu = -1;
        //int otsFiberTypeEnu = -1;
        String name = deviceInterface.getName();
        String description = deviceInterface.getDescription();
        String type = deviceInterface.getType().getTypeName();
        String circuitId = deviceInterface.getCircuitId();
        String supportingInterface = deviceInterface.getSupportingInterface();
        String supportingCircuitPackName = deviceInterface.getSupportingCircuitPackName();
        String supportingPort = deviceInterface.getSupportingPort().toString();
        String ethernetSpeed = "";
        String ethernetFec = "";
        String ethernetMtu = "";
        String ethernetCurrSpeed = "";
        String ethernetCurrDuplex = "";
        //String mciMcttpMinFreq = "";
        //String mciMcttpMaxFreq = "";
        //String mciMcttpCenterFreq = "";
        //String mciMcttpSlotWidth = "";
        //String mciNmcCtpFrequency = "";
        //String mciNmcCtpWidth = "";
        String ochRate = "";
        //String ochFrequency = "";
        //String ochWidth = "";
        //String ochWavelengthNumber = "";
        String ochModulationFormat = "";
        String ochTransmitPower = "";
        String otsSpanLossReceive = "";
        String otsSpanLossTransmit = "";
        //String otsIngressSpanLossAgingMargin = "";
        //String otsEolMaxLoadPin = "";
        String oduRate = ""; //BUG in following case switch statement ???
        //String oduFunction = "";
        String oduMonitoringMode = "";
        //String oduNoOamFunction = "";
        String oduProactiveDelayMeasurementEnabled = "";
        //String oduPoaTribPortNumber = "";
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
        //String oduDegmIntervals = "";
        //String oduDegthrPercentage = "";
        String opuPayloadType = "";
        String opuRxPayloadType = "";
        String opuExpPayloadType = "";
        String opuPayloadInterface = "";
        String maintTestsignalEnabled = "";
        String maintTestsignalBiterrors = "";
        String maintTestsignalBiterrorsterminal = "";
        String maintTestsignalSyncseconds = "";
        String maintTestsignalSyncsecondsterminal = "";
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
        //String otuDegmIntervals = "";
        //String otuDegthrPercentage = "";
        String otuMaintLoopbackEnabled = "";
        //String mtOtuRate = "";
        //String mtOtuFec = "";
        //String mtOtuMaintLoopback = "";
        //String mtOtuEnabled = "";
        //String mtOtuType = "";

        switch (deviceInterface.getType().toString()) {

            case "ethernet":
                //EthernetBuilder ethIfBuilder = new EthernetBuilder();
                EthernetBuilder ethIfBuilder =
                    new EthernetBuilder(deviceInterface.augmentation(Interface1.class).getEthernet());
                ethernetSpeed = ethIfBuilder.getSpeed().toString();
                ethernetFec = ethIfBuilder.getFec().getName();
                ethernetDuplexEnu = ethIfBuilder.getDuplex().getIntValue();
                ethernetMtu = ethIfBuilder.getMtu().toString();
                ethernetAutoNegotiationEnu = ethIfBuilder.getAutoNegotiation().getIntValue();
                ethernetCurrSpeed = ethIfBuilder.getCurrSpeed();
                ethernetCurrDuplex = ethIfBuilder.getCurrDuplex();
                break;

            case "och":
                OchBuilder ochIfBuilder = new OchBuilder(deviceInterface.augmentation(
                    org.opendaylight.yang.gen.v1
                        .http.org.openroadm.optical.channel.interfaces.rev161014.Interface1.class)
                    .getOch());
                ochRate = ochIfBuilder.getRate().getName();
                //ochWavelengthNumber = ochIfBuilder.getWavelengthNumber().toString();
                ochModulationFormat = ochIfBuilder.getModulationFormat().getName();
                ochTransmitPower = ochIfBuilder.getTransmitPower().toString();
                break;

            case "ots":
                OtsBuilder otsIfBuilder = new OtsBuilder(deviceInterface.augmentation(
                    org.opendaylight.yang.gen.v1
                        .http.org.openroadm.optical.transport.interfaces.rev161014.Interface1.class)
                    .getOts());
                int otsFiberTypeEnu = otsIfBuilder.getFiberType().getIntValue();
                otsSpanLossReceive = otsIfBuilder.getSpanLossReceive().toString();
                otsSpanLossTransmit = otsIfBuilder.getSpanLossTransmit().toString();
                break;

            case "odu":
                OduBuilder oduIfBuilder = new OduBuilder(deviceInterface.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1.class)
                    .getOdu());
                oduRate = String.valueOf(oduIfBuilder.getRate());
                oduMonitoringMode = oduIfBuilder.getMonitoringMode().getName();
                oduProactiveDelayMeasurementEnabled = oduIfBuilder.isProactiveDelayMeasurementEnabled().toString();

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
                maintTestsignalEnabled = oduIfBuilder.getMaintTestsignal().isEnabled().toString();
                maintTestsignalTestpatternEnu = oduIfBuilder.getMaintTestsignal().getTestPattern().getIntValue();
                maintTestsignalTypeEnu = oduIfBuilder.getMaintTestsignal().getType().getIntValue();
                maintTestsignalBiterrors = oduIfBuilder.getMaintTestsignal().getBitErrors().toString();
                maintTestsignalBiterrorsterminal = oduIfBuilder.getMaintTestsignal().getBitErrorsTerminal().toString();
                maintTestsignalSyncseconds = oduIfBuilder.getMaintTestsignal().getSyncSeconds();
                maintTestsignalSyncsecondsterminal = oduIfBuilder.getMaintTestsignal().getSyncSecondsTerminal();
                break;

            case "otu":
                OtuBuilder otuIfBuilder = new OtuBuilder(deviceInterface.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1.class)
                    .getOtu());
                otuRate = otuIfBuilder.getRate().getName();
                otuFecEnu = otuIfBuilder.getFec().getIntValue();
                otuMaintLoopbackEnabled = otuIfBuilder.getMaintLoopback().isEnabled().toString();
                otuMaintTypeEnu = otuIfBuilder.getMaintLoopback().getType().getIntValue();
                break;

            default:
                LOG.error("Could not get interface type");
        }

        String startTimestamp = getCurrentTimestamp();

        return new Object[]{nodeId,
            name,
            description,
            type,
            Integer.toString(administrativeStateEnu),
            Integer.toString(operationalState),
            circuitId,
            supportingInterface,
            supportingCircuitPackName,
            supportingPort,
            ethernetSpeed,
            ethernetFec,
            Integer.toString(ethernetDuplexEnu),
            ethernetMtu,
            Integer.toString(ethernetAutoNegotiationEnu),
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
            //ochWavelengthNumber,
            "", "", "",
            ochModulationFormat,
            ochTransmitPower,
            //Integer.toString(otsFiberTypeEnu),
            "-1",
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
            "", "", "", "", "", "", "", "", "", "", "", "", "",
            opuPayloadType,
            opuRxPayloadType,
            opuExpPayloadType,
            opuPayloadInterface,
            maintTestsignalEnabled,
            Integer.toString(maintTestsignalTestpatternEnu),
            Integer.toString(maintTestsignalTypeEnu),
            maintTestsignalBiterrors,
            maintTestsignalBiterrorsterminal,
            maintTestsignalSyncseconds,
            maintTestsignalSyncsecondsterminal,
            otuRate,
            Integer.toString(otuFecEnu),
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
            "", "", "", "", "", "", "", "", "", "", "", "",
            otuMaintLoopbackEnabled,
            Integer.toString(otuMaintTypeEnu),
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
        int monitoringModeEnu = tcm.getMonitoringMode().getIntValue();
        String ltcActEnabled = tcm.isLtcActEnabled().toString();
        String proactiveDelayMeasurementEnabled = tcm.isProactiveDelayMeasurementEnabled().toString();
        //int tcmDirectionEnu = -1;
        //int timDetectModeEnu = -1;
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
            Integer.toString(monitoringModeEnu),
            ltcActEnabled,
            proactiveDelayMeasurementEnabled,
            //Integer.toString(tcmDirectionEnu),
            "-1",
            //txSapi,
            //txDapi,
            //txOperator,
            //acceptedSapi,
            //acceptedDapi,
            //acceptedOperator,
            //expectedSapi,
            //expectedDapi,
            //timActEnabled,
            "", "", "", "", "", "", "", "", "",
            //Integer.toString(timDetectModeEnu),
            "-1",
            //degmIntervals,
            //degthrPercentage,
            "", "",
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

        /*InstanceIdentifier<Interface> interfaceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
           .child(Interface.class);
        Optional<Interface> interfaceOpt =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, interfaceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT); */

        for (int i = 0; i < deviceObject.get().getInterface().size(); i++) {
            Interface deviceInterface;

            deviceInterface = deviceObject.get().getInterface().get(i);
        /*if (interfaceOpt.isPresent()) {
            deviceInterface = interfaceOpt.get();
        } else {
            LOG.warn("Could not get interface info");
            return false;
        }*/
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
                LOG.error("SQL exception ", e);
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

        }
        int adminstatusEnu =
            protocolObject.get().augmentation(Protocols1.class).getLldp().getGlobalConfig().getAdminStatus()
            .getIntValue();
        String msgTxtInterval =
            protocolObject.get().augmentation(Protocols1.class).getLldp().getGlobalConfig().getMsgTxInterval()
            .toString();
        String mxgTxHoldMultiplier =
            protocolObject.get().augmentation(Protocols1.class).getLldp().getGlobalConfig().getMsgTxHoldMultiplier()
            .toString();
        String startTimestamp = getCurrentTimestamp();
        persistDevProtocolLldpPortConfig(nodeId, connection);
        persistDevProtocolLldpNbrList(nodeId, connection);

        Object[] parameters = {nodeId,
            Integer.toString(adminstatusEnu),
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
            LOG.error("SQL exception ", e);
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

        }
        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < protocolObject.get().augmentation(Protocols1.class).getLldp().getPortConfig().size(); i++) {

            PortConfig portConfig =
                protocolObject.get().augmentation(Protocols1.class).getLldp().getPortConfig().get(i);
            String ifName = portConfig.getIfName();
            int adminStatusEnu = portConfig.getAdminStatus().getIntValue();

            Object[] parameters = {nodeId,
                ifName,
                Integer.toString(adminStatusEnu),
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
                LOG.error("SQL exception ", e);
            }

        }

    }

    private void persistDevProtocolLldpNbrList(String nodeId, Connection connection) {

        InstanceIdentifier<Protocols> protocolsIID =
                InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Protocols.class);
        Optional<Protocols> protocolObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION, protocolsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!protocolObject.isPresent() || protocolObject.get().augmentation(Protocols1.class) == null) {
            LOG.error("LLDP subtree is missing");

        }
        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < protocolObject.get().augmentation(Protocols1.class).getLldp().getNbrList().getIfName()
            .size(); i++) {

            IfName ifNameObj =
                protocolObject.get().augmentation(Protocols1.class).getLldp().getNbrList().getIfName().get(i);
            String ifName = ifNameObj.getIfName();
            String remotesysname = ifNameObj.getRemoteSysName();
            String remotemgmtaddresssubtype = ifNameObj.getRemoteMgmtAddressSubType().getName();
            String remotemgmtaddress = ifNameObj.getRemoteMgmtAddress().getIpv4Address().toString();
            int remoteportidsubtypeEnu = ifNameObj.getRemotePortIdSubType().getIntValue();
            String remoteportid = ifNameObj.getRemotePortId();
            int remotechassisidsubtypeEnu = ifNameObj.getRemoteChassisIdSubType().getIntValue();
            String remotechassisid = ifNameObj.getRemoteChassisId();

            Object[] parameters = {nodeId,
                ifName,
                remotesysname,
                remotemgmtaddresssubtype,
                remotemgmtaddress,
                Integer.toString(remoteportidsubtypeEnu),
                remoteportid,
                Integer.toString(remotechassisidsubtypeEnu),
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
                LOG.error("SQL exception ", e);
            }

        }
    }


    private void persistDevInternalLinks(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < deviceObject.get().getInternalLink().size(); i++) {
            InternalLink internalLink = deviceObject.get().getInternalLink().get(i);
            String internalLinkName = internalLink.getInternalLinkName();
            String sourceCircuitPackName = internalLink.getSource().getCircuitPackName();
            String sourcePortName = internalLink.getSource().getPortName().toString();
            String destinationCircuitPackName = internalLink.getDestination().getCircuitPackName();
            String destinationPortName = internalLink.getDestination().getPortName().toString();

            Object[] parameters = {nodeId,
                internalLinkName,
                sourceCircuitPackName,
                sourcePortName,
                destinationCircuitPackName,
                destinationPortName,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceInternalLinkInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("SQL exception ", e);
            }

        }
    }


    private void persistDevExternalLinks(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < deviceObject.get().getExternalLink().size(); i++) {
            ExternalLink externalLink = deviceObject.get().getExternalLink().get(i);
            String externalLinkName = externalLink.getExternalLinkName();
            String sourceNodeId = externalLink.getSource().getNodeId();
            String sourceCircuitPackName = externalLink.getSource().getCircuitPackName();
            String sourcePortName = externalLink.getSource().getPortName();
            String destinationNodeId = externalLink.getDestination().getNodeId();
            String destinationCircuitPackName = externalLink.getDestination().getCircuitPackName();
            String destinationPortName = externalLink.getDestination().getPortName();

            Object[] parameters = {nodeId,
                externalLinkName,
                sourceNodeId,
                sourceCircuitPackName,
                sourcePortName,
                destinationNodeId,
                destinationCircuitPackName,
                destinationPortName,
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceExternalLinkInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("SQL exception ", e);
            }

        }
    }

    private void persistDevPhysicalLinks(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < deviceObject.get().getPhysicalLink().size(); i++) {
            PhysicalLink physicalLink = deviceObject.get().getPhysicalLink().get(i);
            String physicalLinkName = physicalLink.getPhysicalLinkName();
            String sourceCircuitPackName = physicalLink.getSource().getCircuitPackName();
            String sourcePortName = physicalLink.getSource().getPortName().toString();
            String destinationCircuitPackName = physicalLink.getDestination().getCircuitPackName();
            String destinationPortName = physicalLink.getDestination().getPortName().toString();

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
                LOG.error("SQL exception ", e);
            }

        }
    }

    private void persistDevDegree(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < deviceObject.get().getDegree().size(); i++) {
            Degree degree = deviceObject.get().getDegree().get(i);
            String degreeNumber = degree.getDegreeNumber().toString();
            String maxWavelengths = degree.getMaxWavelengths().toString();
            String otdrPortCircuitPackName = degree.getOtdrPort().getCircuitPackName();
            String otdrPortPortName = degree.getOtdrPort().getPortName().toString();
            persistDevDegreeCircuitPack(nodeId, degree, degreeNumber, connection);
            persistDevDegreeConnectionPort(nodeId, degree, degreeNumber, connection);
            //String mcCapabilitiesSlotWidthGranularity = "";
            //String mcCapabilitiesCenterFreqGranularity = "";
            //String mcCapabilitiesMinSlots = "";
            //String mcCapabilitiesMaxSlots = "";

            Object[] parameters = {nodeId,
                degreeNumber,
                maxWavelengths,
                otdrPortCircuitPackName,
                otdrPortPortName,
                //mcCapabilitiesSlotWidthGranularity,
                //mcCapabilitiesCenterFreqGranularity,
                //mcCapabilitiesMinSlots,
                //mcCapabilitiesMaxSlots,
                "","","","",
                startTimestamp,
                startTimestamp
            };

            String query = Queries.getQuery().deviceDegreeInsert().get();
            LOG.info("Running {} query ", query);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("SQL exception ", e);
            }

        }
    }


    private void persistDevDegreeCircuitPack(String nodeId, Degree degree, String degreeNumber, Connection connection) {

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < degree.getCircuitPacks().size(); i++) {

            String circuitPackIndex = degree.getCircuitPacks().get(i).getIndex().toString();
            String circuitPackName = degree.getCircuitPacks().get(i).getCircuitPackName();

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
                LOG.error("SQL exception ", e);
            }

        }
    }

    private void persistDevDegreeConnectionPort(String nodeId, Degree degree, String degreeNumber,
        Connection connection) {

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < degree.getConnectionPorts().size(); i++) {

            String connectionPortIndex = degree.getConnectionPorts().get(i).getIndex().toString();
            String circuitPackName = degree.getConnectionPorts().get(i).getCircuitPackName();
            String portName = degree.getConnectionPorts().get(i).getPortName().toString();

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
                LOG.error("SQL exception ", e);
            }

        }
    }


    private void persistDevSrg(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < deviceObject.get().getSharedRiskGroup().size(); i++) {
            SharedRiskGroup sharedRiskGroup = deviceObject.get().getSharedRiskGroup().get(i);
            String maxAddDropPorts = sharedRiskGroup.getMaxAddDropPorts().toString();
            String srgNumber = sharedRiskGroup.getSrgNumber().toString();
            int wavelengthDuplicationEnu = sharedRiskGroup.getWavelengthDuplication().getIntValue();
            persistDevSrgCircuitPacks(nodeId, sharedRiskGroup, srgNumber, connection);
            //String currentProvisionedAddDropPorts = "";
            //String mcCapSlotWidthGranularity = "";
            //String mcCapCenterFreqGranularity = "";
            //String mcCapMinSlots = "";
            //String mcCapMaxSlots = "";

            Object[] parameters = {nodeId,
                maxAddDropPorts,
                //currentProvisionedAddDropPorts,
                "",
                srgNumber,
                //mcCapSlotWidthGranularity,
                //mcCapCenterFreqGranularity,
                //mcCapMinSlots,
                //mcCapMaxSlots,
                "","","","",
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
                LOG.error("SQL exception ", e);
            }

        }
    }

    private void persistDevSrgCircuitPacks(String nodeId, SharedRiskGroup sharedRiskGroup, String srgNumber,
        Connection connection) {

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < sharedRiskGroup.getCircuitPacks().size(); i++) {

            String circuitPackindex = sharedRiskGroup.getCircuitPacks().get(i).getIndex().toString();
            String circuitPackName = sharedRiskGroup.getCircuitPacks().get(i).getCircuitPackName();

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
                LOG.error("SQL exception ", e);
            }

        }
    }

    private void persistDevRoadmConnections(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < deviceObject.get().getRoadmConnections().size(); i++) {
            RoadmConnections roadmConnections = deviceObject.get().getRoadmConnections().get(i);
            int opticalcontrolmodeEnu = roadmConnections.getOpticalControlMode().getIntValue();
            //String connectionName = "";
            //String connectionNumber = "";
            //String wavelengthNumber = "";
            String targetOutputPower = roadmConnections.getTargetOutputPower().toString();
            String srcIf = roadmConnections.getSource().getSrcIf();
            String dstIf = roadmConnections.getDestination().getDstIf();

            Object[] parameters = {nodeId,
                //connectionName,
                //connectionNumber,
                //wavelengthNumber,
                "","","",
                Integer.toString(opticalcontrolmodeEnu),
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
                LOG.error("SQL exception ", e);
            }

        }
    }


    private void persistDevConnectionMap(String nodeId, Connection connection) {

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i < deviceObject.get().getConnectionMap().size(); i++) {
            ConnectionMap connectionMap = deviceObject.get().getConnectionMap().get(i);
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
                LOG.error("SQL exception ", e);
            }

        }
    }
/*
    private void persistDevWavelengthMap(String nodeId, Connection connection) {


        String wavelengthNumber="", centerFrequency="", wavelength="";;

        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);


        String startTimestamp = getCurrentTimestamp();
        for (int i = 0; i<deviceObject.get().getWavelengthMap().getWavelengths().size(); i++) {
            Wavelengths wavelengths = deviceObject.get().getWavelengthMap().getWavelengths().get(i);
            wavelengthNumber=wavelengths.getWavelengthNumber().toString();
            centerFrequency=wavelengths.getCenterFrequency().toString();
            wavelength=wavelengths.getWavelength().toString();


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
                LOG.error("SQL exception ", e);
            }

        }
    }

*/

    private void persistDevInterfaceTcm(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {

        for (int i = 0; i < oduBuilder.getTcm().size(); i++) {
            Tcm tcm;

            tcm = oduBuilder.getTcm().get(i);

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
                LOG.error("SQL exception ", e);
            }
        }
    }

    private void persistDevInterfaceOtnOduTxMsi(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {

        for (int i = 0; i < oduBuilder.getTcm().size(); i++) {
            TxMsi txMsi;

            txMsi = oduBuilder.getOpu().getMsi().getTxMsi().get(i);

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
                LOG.error("SQL exception ", e);
            }
        }
    }


    private void persistDevInterfaceOtnOduRxMsi(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {

        for (int i = 0; i < oduBuilder.getTcm().size(); i++) {
            RxMsi rxMsi;

            rxMsi = oduBuilder.getOpu().getMsi().getRxMsi().get(i);

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
                LOG.error("SQL exception ", e);
            }
        }
    }


    private void persistDevInterfaceOtnOduExpMsi(String nodeId, String interfaceName, OduBuilder oduBuilder,
        Connection connection) {

        for (int i = 0; i < oduBuilder.getTcm().size(); i++) {
            ExpMsi expMsi;

            expMsi = oduBuilder.getOpu().getMsi().getExpMsi().get(i);

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
                LOG.error("SQL exception ", e);
            }
        }
    }


}
