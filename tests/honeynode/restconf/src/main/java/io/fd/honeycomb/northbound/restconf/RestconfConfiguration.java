/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.northbound.restconf;

import java.util.Optional;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;

//TODO - HONEYCOMB-377 - do not include module to active modules if disabled
@BindConfig(value = "restconf", syntax = Syntax.JSON)
public class RestconfConfiguration {

    @InjectConfig("restconf-http-enabled")
    public String restconfHttp;
    @InjectConfig("restconf-binding-address")
    public Optional<String> restconfBindingAddress;
    @InjectConfig("restconf-port")
    public Optional<Integer> restconfPort;
    @InjectConfig("restconf-https-enabled")
    public String restconfHttps;
    @InjectConfig("restconf-https-binding-address")
    public Optional<String> restconfHttpsBindingAddress;
    @InjectConfig("restconf-https-port")
    public Optional<Integer> restconfHttpsPort;
    /**
     * Restconf keystore file name. It will be loaded from the classpath so must be present in one of the folders
     * packaged with the distribution e.g. cert/
     */
    @InjectConfig("restconf-keystore")
    public Optional<String> restconfKeystore = Optional.of("/honeycomb-keystore");
    @InjectConfig("restconf-keystore-password")
    public Optional<String> keystorePassword;
    @InjectConfig("restconf-keystore-manager-password")
    public Optional<String> keystoreManagerPassword;
    /**
     * Restconf truststore file name. It will be loaded from the classpath so must be present in one of the folders
     * packaged with the distribution e.g. cert/
     */
    @InjectConfig("restconf-truststore")
    public Optional<String> restconfTruststore;
    @InjectConfig("restconf-truststore-password")
    public Optional<String> truststorePassword;
    @InjectConfig("restconf-websocket-address")
    public Optional<String> restconfWebsocketAddress;
    @InjectConfig("restconf-websocket-port")
    public Optional<Integer> restconfWebsocketPort;
    @InjectConfig("restconf-root-path")
    public Optional<String> restconfRootPath = Optional.of("/restconf");
    @InjectConfig("restconf-pool-max-size")
    public Optional<Integer> restPoolMaxSize = Optional.of(10);
    @InjectConfig("restconf-pool-min-size")
    public Optional<Integer> restPoolMinSize = Optional.of(1);
    @InjectConfig("restconf-acceptors-size")
    public Optional<Integer> acceptorsSize = Optional.of(1);
    @InjectConfig("restconf-selectors-size")
    public Optional<Integer> selectorsSize = Optional.of(1);
    @InjectConfig("restconf-https-acceptors-size")
    public Optional<Integer> httpsAcceptorsSize = Optional.of(1);
    @InjectConfig("restconf-https-selectors-size")
    public Optional<Integer> httpsSelectorsSize = Optional.of(1);

    public boolean isRestconfHttpEnabled() {
        return Boolean.valueOf(restconfHttp);
    }

    public boolean isRestconfHttpsEnabled() {
        return Boolean.valueOf(restconfHttps);
    }

    public boolean isRestconfEnabled() {
        return isRestconfHttpEnabled() || isRestconfHttpsEnabled();
    }

    @Override
    public String toString() {
        return "RestconfConfiguration{" +
            "restconfHttp='" + restconfHttp + '\'' +
            ", restconfBindingAddress=" + restconfBindingAddress +
            ", restconfPort=" + restconfPort +
            ", restconfHttps='" + restconfHttps + '\'' +
            ", restconfHttpsBindingAddress=" + restconfHttpsBindingAddress +
            ", restconfHttpsPort=" + restconfHttpsPort +
            ", restconfKeystore=" + restconfKeystore +
            ", keystorePassword=" + keystorePassword +
            ", keystoreManagerPassword=" + keystoreManagerPassword +
            ", restconfTruststore=" + restconfTruststore +
            ", truststorePassword=" + truststorePassword +
            ", restconfWebsocketAddress=" + restconfWebsocketAddress +
            ", restconfWebsocketPort=" + restconfWebsocketPort +
            ", restconfRootPath=" + restconfRootPath +
            ", restPoolMaxSize=" + restPoolMaxSize +
            ", restPoolMinSize=" + restPoolMinSize +
            ", acceptorsSize=" + acceptorsSize +
            ", selectorsSize=" + selectorsSize +
            ", httpsAcceptorsSize=" + httpsAcceptorsSize +
            ", httpsSelectorsSize=" + httpsSelectorsSize +
            '}';
    }
}
