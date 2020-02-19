/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro.activation;

import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;

import java.util.Optional;

@BindConfig(value = "activation", syntax = Syntax.JSON)
public class ActivationConfig {

    @InjectConfig("modules-resource-path")
    public String modulesResourcePath;

    @InjectConfig("yang-modules-index-path")
    public String yangModulesIndexPath;

    public String getModulesResourcePath() {
        return Optional.ofNullable(modulesResourcePath).orElse("../modules/");
    }

    public String getYangModulesIndexPath() {
        return Optional.ofNullable(yangModulesIndexPath).orElse("../yang-mapping/");
    }

}
