/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.topology;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to build Supernode
 * topology (fakepce.xml file has to be
 * in src/main/ressources folder
 * to be loaded in taget/classes).
 *
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
public class Topology {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(Topology.class);
    /** file must be in src/main/resources in order to be in
     * target/classes after compilation.
     */

    /** Structure of Supernode topology. */
    private Network network;
    public BundleContext bcontext;
    /** String to get Supernode topolgy info freom xml file. */
    private String xml = null;

    /**
     * load fakepce.xml file
     * and convert the informations
     * in Network structure.
     *
     */
    public void start() {
        setNetwork(null);
        XmlMapper xmlMapper = new XmlMapper();
        try {
            InputStream is = FrameworkUtil.getBundle(Topology.class).getBundleContext()
                    .getBundle().getEntry("/fakepce.xml").openStream();
            /*File file = new File("target/classes/fakepce.xml");
            InputStream is = new FileInputStream(file);*/
            xml = inputStreamToString(is);
            if (xml != null) {
                setNetwork(xmlMapper.readValue(xml, Network.class));
                LOG.info("Network : " + network.toString());
            } else {
                LOG.info("String xml is null");
            }
        } catch (IOException e) {
            LOG.error("The file fakepce.xml not found", e);
        }
    }

    /**
     * get xml file
     * content.
     *
     * @param is InputStream
     * @return String xml file content
     * @throws IOException exception raised
     */
    private String inputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    public static void main(String[] args) {
        Topology topo = new Topology();
        topo.start();
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setBcontext(BundleContext bcontext) {
        this.bcontext = bcontext;
    }

    public BundleContext getBcontext() {
        return this.bcontext;
    }
}
