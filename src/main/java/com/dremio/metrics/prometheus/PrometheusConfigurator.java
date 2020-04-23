/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.metrics.prometheus;

import java.io.IOException;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.dremio.telemetry.api.config.ConfigModule;
import com.dremio.telemetry.api.config.ReporterConfigurator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.Module;
import com.google.common.base.Objects;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.HTTPServer;

/**
 * Configurator for JMX
 */
@JsonTypeName("prometheus")
public class PrometheusConfigurator extends ReporterConfigurator {

    private final int port;
    private volatile HTTPServer server;

    @JsonCreator
    public PrometheusConfigurator(@JsonProperty("port") int port) {
        super();
        this.port = (port > 0) ? port : Integer.parseInt(System.getProperty("dremio.prometheus.port", "12543"));

    }

    @Override
    public void configureAndStart(String name, MetricRegistry registry, MetricFilter filter) {
        CollectorRegistry.defaultRegistry.register(new DropwizardExports(registry));//todo filter and rates maybe switch to pushgateway
        try {
            server = new HTTPServer(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrometheusConfigurator that = (PrometheusConfigurator) o;
        return port == that.port;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(port);
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Module that may be added to a jackson object mapper
     * so it can parse jmx config.
     */
    public static class Module extends ConfigModule {
        @Override
        public void setupModule(com.fasterxml.jackson.databind.Module.SetupContext context) {
            context.registerSubtypes(PrometheusConfigurator.class);
        }
    }
}
