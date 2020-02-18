/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.regex.PatternSyntaxException;

import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.NetconfSessionListener;
import org.opendaylight.netconf.api.NetconfTerminationReason;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.api.monitoring.SessionEvent;
import org.opendaylight.netconf.api.monitoring.SessionListener;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.impl.osgi.NetconfOperationRouter;
import org.opendaylight.netconf.notifications.NetconfNotification;
import org.opendaylight.netconf.util.messages.SendErrorExceptionUtil;
import org.opendaylight.netconf.util.messages.SubtreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NetconfServerSessionListener implements NetconfSessionListener<NetconfServerSession> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfServerSessionListener.class);
    private final SessionListener monitoringSessionListener;
    private final NetconfOperationRouter operationRouter;
    private final AutoCloseable onSessionDownCloseable;

    public NetconfServerSessionListener(final NetconfOperationRouter operationRouter,
            final NetconfMonitoringService monitoringService, final AutoCloseable onSessionDownCloseable) {
        this.operationRouter = operationRouter;
        this.monitoringSessionListener = monitoringService.getSessionListener();
        this.onSessionDownCloseable = onSessionDownCloseable;
    }

    @Override
    public void onSessionUp(final NetconfServerSession netconfNetconfServerSession) {
        monitoringSessionListener.onSessionUp(netconfNetconfServerSession);
    }

    @Override
    public void onSessionDown(final NetconfServerSession netconfNetconfServerSession, final Exception cause) {
        LOG.debug("Session {} down, reason: {}", netconfNetconfServerSession, cause.getMessage());
        onDown(netconfNetconfServerSession);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onDown(final NetconfServerSession netconfNetconfServerSession) {
        monitoringSessionListener.onSessionDown(netconfNetconfServerSession);

        try {
            operationRouter.close();
        } catch (final Exception closingEx) {
            LOG.debug("Ignoring exception while closing operationRouter", closingEx);
        }
        try {
            onSessionDownCloseable.close();
        } catch (final Exception ex) {
            LOG.debug("Ignoring exception while closing onSessionDownCloseable", ex);
        }
    }

    @Override
    public void onSessionTerminated(final NetconfServerSession netconfNetconfServerSession,
            final NetconfTerminationReason netconfTerminationReason) {
        LOG.debug("Session {} terminated, reason: {}", netconfNetconfServerSession,
                netconfTerminationReason.getErrorMessage());
        onDown(netconfNetconfServerSession);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public void onMessage(final NetconfServerSession session, final NetconfMessage netconfMessage) {
        try {

            Preconditions.checkState(operationRouter != null, "Cannot handle message, session up was not yet received");
            // there is no validation since the document may contain yang schemas
            final NetconfMessage message = processDocument(netconfMessage, session);
            final NetconfMessage modifiedmessage = modifyType(message);
            LOG.debug("Responding with message {}", modifiedmessage);
            session.sendMessage(message);
            monitoringSessionListener.onSessionEvent(SessionEvent.inRpcSuccess(session));
        } catch (final RuntimeException e) {
            // TODO: should send generic error or close session?
            LOG.error("Unexpected exception", e);
            session.onIncommingRpcFail();
            monitoringSessionListener.onSessionEvent(SessionEvent.inRpcFail(session));
            throw new IllegalStateException("Unable to process incoming message " + netconfMessage, e);
        } catch (final DocumentedException e) {
            LOG.trace("Error occurred while processing message", e);
            session.onOutgoingRpcError();
            session.onIncommingRpcFail();
            monitoringSessionListener.onSessionEvent(SessionEvent.inRpcFail(session));
            monitoringSessionListener.onSessionEvent(SessionEvent.outRpcError(session));
            SendErrorExceptionUtil.sendErrorMessage(session, e, netconfMessage);
        }
    }

    private NetconfMessage modifyType(NetconfMessage message) {
        Document doc = message.getDocument();
        NodeList result = doc.getElementsByTagName("components");
        if (result.getLength() > 0) {
            NodeList components = doc.getElementsByTagName("component");
            for (int i = 0; i < components.getLength(); i++) {
                try {
                    Element component = (Element) components.item(i);
                    Element state = getChild(component, "state");
                    if (state != null) {
                        LOG.debug("state gets : {}", state.getTextContent());
                        Element type = getChild(state, "type");
                        if (type != null) {
                            LOG.debug("type gets : {}", type.getTextContent());
                            LOG.debug("formatting ...");
                            String textContent = type.getTextContent();
                            LOG.debug("formatting : {}", textContent);
                            String[] splitValue = textContent.split("\\)");
                            String value = null;
                            String namespace = null;
                            if (splitValue.length == 2) {
                                namespace = splitValue[0];
                                value = splitValue[1];
                                if ((value != null) && (namespace != null)) {
                                    if (namespace.contains("http://openconfig.net/yang/platform-types")) {
                                        type.setAttribute("xmlns:oc-platform-types",
                                                "http://openconfig.net/yang/platform-types");
                                        type.setTextContent("oc-platform-types:" + value);
                                    } else if (namespace.contains("http://openconfig.net/yang/transport-types")) {
                                        type.setAttribute("xmlns:oc-opt-types",
                                                "http://openconfig.net/yang/transport-types");
                                        type.setTextContent("oc-opt-types:" + value);
                                    }
                                } else {
                                    LOG.debug("value or namespace is null !");
                                }
                            }
                        } else {
                            LOG.debug("tag <state> doesn't have type value !");
                        }
                    } else {
                        LOG.debug("tag <component> doesn't have state value !");
                    }
                } catch (PatternSyntaxException | ArrayIndexOutOfBoundsException | NullPointerException e) {
                    LOG.warn("failed to get type value!", e);
                }
            }
        } else {
            LOG.debug("No <components> tag present in xml doc");
        }
        return new NetconfMessage(doc);
    }

    private Element getChild(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if ((child instanceof Element) && name.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    public void onNotification(final NetconfServerSession session, final NetconfNotification notification) {
        monitoringSessionListener.onSessionEvent(SessionEvent.notification(session));
    }

    private NetconfMessage processDocument(final NetconfMessage netconfMessage, final NetconfServerSession session)
            throws DocumentedException {

        final Document incomingDocument = netconfMessage.getDocument();
        final Node rootNode = incomingDocument.getDocumentElement();

        if (rootNode.getLocalName().equals(XmlNetconfConstants.RPC_KEY)) {
            final Document responseDocument = XmlUtil.newDocument();
            checkMessageId(rootNode);

            Document rpcReply = operationRouter.onNetconfMessage(incomingDocument, session);

            rpcReply = SubtreeFilter.applyRpcSubtreeFilter(incomingDocument, rpcReply);

            session.onIncommingRpcSuccess();

            responseDocument.appendChild(responseDocument.importNode(rpcReply.getDocumentElement(), true));
            return new NetconfMessage(responseDocument);
        } else {
            // unknown command, send RFC 4741 p.70 unknown-element
            /*
             * Tag: unknown-element Error-type: rpc, protocol, application Severity: error
             * Error-info: <bad-element> : name of the unexpected element Description: An
             * unexpected element is present.
             */
            throw new DocumentedException("Unknown tag " + rootNode.getNodeName() + " in message:\n" + netconfMessage,
                    DocumentedException.ErrorType.PROTOCOL, DocumentedException.ErrorTag.UNKNOWN_ELEMENT,
                    DocumentedException.ErrorSeverity.ERROR, ImmutableMap.of("bad-element", rootNode.getNodeName()));
        }
    }

    private static void checkMessageId(final Node rootNode) throws DocumentedException {

        final NamedNodeMap attributes = rootNode.getAttributes();

        if (attributes.getNamedItemNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
                XmlNetconfConstants.MESSAGE_ID) != null) {
            return;
        }

        if (attributes.getNamedItem(XmlNetconfConstants.MESSAGE_ID) != null) {
            return;
        }

        throw new DocumentedException("Missing attribute " + rootNode.getNodeName(), DocumentedException.ErrorType.RPC,
                DocumentedException.ErrorTag.MISSING_ATTRIBUTE, DocumentedException.ErrorSeverity.ERROR,
                ImmutableMap.of("bad-attribute", XmlNetconfConstants.MESSAGE_ID, "bad-element",
                        XmlNetconfConstants.RPC_KEY));
    }
}
