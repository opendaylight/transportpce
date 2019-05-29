/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestAPICallsTest {
    private static final String CONFIG_URI="http://localhost:8183/restconf/config/org-openroadm-device:org-openroadm-device";
    private final String user = "admin";
    private final String pass = "admin";
    private String encodeUserPass;
    private String uri;
    private String input;
    private static final Logger LOG = LoggerFactory.getLogger(RestAPICallsTest.class);

    public RestAPICallsTest(String uriRest, String inputXml) {
        uri = uriRest;
        input = inputXml;
        String usernameAndPassword = this.user+":"+this.pass;
        try {
            byte[] encodeBytes = Base64.getEncoder().encode(usernameAndPassword.getBytes());
            this.encodeUserPass = new String(encodeBytes, "UTF-8");
            LOG.info("encode : {}",encodeUserPass);
        } catch (UnsupportedEncodingException e) {
            LOG.error("encode usernameAndPassword failed !");
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> stringXML() {
        return Arrays.asList(new Object[][]{
                {CONFIG_URI,DeviceOperToConfig.operToConfig()}
        });
    }

    /**
     *send PUT request via REST API.
     *
     * @param uri request url
     * @return String response
     * @throws IOException
     */
    @Test
    public void test1Put() throws IOException {
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty ("Authorization", "Basic " + encodeUserPass);
        conn.setRequestProperty("Content-Type", "application/xml");
        LOG.info("input xml : {}",input);
        if (input != null) {
            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();
            Assert.assertEquals(HttpURLConnection.HTTP_CREATED,conn.getResponseCode());
            conn.disconnect();
        } else {
            LOG.error("input xml gets is null !");
        }
        LOG.info("Test Succeed");
    }

    /**
     * send GET request via REST API.
     *
     * @param uri request url
     * @return String response
     * @throws IOException
     */
    @Test
    public void test2Get() throws IOException {
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty ("Authorization", "Basic " + encodeUserPass);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        Assert.assertEquals(HttpURLConnection.HTTP_OK,conn.getResponseCode());
//        Assert.assertThat(result, CoreMatchers.containsString("org-openroadm-device"));
        conn.disconnect();
        LOG.info("Test Succeed");
    }

    /**
     *send DELETE request via REST API.
     *
     * @param uri request url
     * @return String response
     * @throws IOException
     */
    @Test
    public void test3Delete() throws IOException {
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty ("Authorization", "Basic " + encodeUserPass);
        Assert.assertEquals(HttpURLConnection.HTTP_OK,conn.getResponseCode());
        conn.disconnect();
    }
}