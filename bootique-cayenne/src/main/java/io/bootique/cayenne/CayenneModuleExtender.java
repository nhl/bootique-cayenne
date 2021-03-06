/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.cayenne;

import io.bootique.ModuleExtender;
import io.bootique.cayenne.annotation.CayenneConfigs;
import io.bootique.cayenne.annotation.CayenneListener;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.SetBuilder;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.di.Module;

/**
 * @since 0.19
 */
public class CayenneModuleExtender extends ModuleExtender<CayenneModuleExtender> {

    private SetBuilder<DataChannelFilter> filters;
    private SetBuilder<Object> listeners;
    private SetBuilder<String> projects;
    private SetBuilder<Module> modules;

    public CayenneModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneModuleExtender initAllExtensions() {
        contributeListeners();
        contributeFilters();
        contributeModules();
        contributeProjects();
        return this;
    }

    public CayenneModuleExtender addFilter(DataChannelFilter filter) {
        contributeFilters().addInstance(filter);
        return this;
    }

    public CayenneModuleExtender addFilter(Class<? extends DataChannelFilter> filterType) {
        contributeFilters().add(filterType);
        return this;
    }

    public CayenneModuleExtender addListener(Object listener) {
        contributeListeners().addInstance(listener);
        return this;
    }

    public CayenneModuleExtender addListener(Class<?> listenerType) {
        contributeListeners().add(listenerType);
        return this;
    }

    public CayenneModuleExtender addProject(String projectConfig) {
        contributeProjects().addInstance(projectConfig);
        return this;
    }

    public CayenneModuleExtender addModule(Module module) {
        contributeModules().addInstance(module);
        return this;
    }

    public CayenneModuleExtender addModule(Class<? extends Module> moduleType) {
        contributeModules().add(moduleType);
        return this;
    }

    public CayenneModuleExtender addModule(Key<? extends Module> moduleKey) {
        contributeModules().add(moduleKey);
        return this;
    }

    protected SetBuilder<DataChannelFilter> contributeFilters() {
        return filters != null ? filters : (filters = newSet(DataChannelFilter.class));
    }

    protected SetBuilder<Object> contributeListeners() {
        return listeners != null ? listeners : (listeners = newSet(Object.class, CayenneListener.class));
    }

    protected SetBuilder<String> contributeProjects() {
        return projects != null ? projects : (projects = newSet(String.class, CayenneConfigs.class));
    }

    protected SetBuilder<Module> contributeModules() {
        return modules != null ? modules : (modules = newSet(Module.class));
    }
}
