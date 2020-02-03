/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import com.google.common.io.CharStreams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to connect to GNPy tool.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class ConnectToGnpyServer {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectToGnpyServer.class);
    static final String URL_GNPY = "http://127.0.0.1:8008/gnpy/api/v1.0/files";

    public String gnpyCnx(String jsonTxt) throws GnpyException  {
        String jsonRespTxt = null;

        try {
            URL url = new URL(URL_GNPY);
            String userCredentials = "gnpy:gnpy";
            String basicAuth = "Basic " + new String(java.util.Base64.getEncoder().encode(userCredentials.getBytes()));

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json");

            // Send the request to the GNPy
            OutputStream os = conn.getOutputStream();
            os.write(jsonTxt.getBytes());
            os.flush();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                throw new GnpyException(String.format(
                    "In connectToGnpyServer: could not connect to GNPy - response code: %s",conn.getResponseCode()));
            }
            InputStreamReader response = new InputStreamReader((conn.getInputStream()));
            if (response != null) {
                jsonRespTxt = CharStreams.toString(response);
            }
            conn.disconnect();
        } catch (IOException e) {
            throw new GnpyException("In connectToGnpyServer: excpetion",e);
        }
        return jsonRespTxt;
    }

    public boolean isGnpyURLExist() {
        boolean exist = false;
        try {
            URL url = new URL(URL_GNPY);
            String userCredentials = "gnpy:gnpy";
            String basicAuth = "Basic " + new String(java.util.Base64.getEncoder().encode(userCredentials.getBytes()));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("HEAD");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                LOG.info("In connectToGnpyServer: Gnpy instance is connected to T-PCE");
                exist = true;
            }
            conn.disconnect();
        }
        catch (IOException e) {
            LOG.warn("In connectToGnpyserver: could not connect to GNPy server {}",e.getMessage());
            return exist;
        }
        return exist;
    }

    public String readResponse(InputStreamReader response) throws GnpyException {
        String output = null;
        BufferedReader br = new BufferedReader(response);
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            output = sb.toString();
        } catch (IOException e) {
            throw new GnpyException("In connectToGnpyserver: could not read response",e);
        }
        return output;
    }
}
