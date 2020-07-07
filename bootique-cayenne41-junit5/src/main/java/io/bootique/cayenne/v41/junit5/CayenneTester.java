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
package io.bootique.cayenne.v41.junit5;

import io.bootique.cayenne.v41.CayenneModule;
import io.bootique.cayenne.v41.junit5.tester.*;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQBeforeMethodCallback;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.ObjEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Collection;
import java.util.HashSet;

/**
 * A JUnit5 extension that manages test schema, data and Cayenne runtime state between tests. A single CayenneTester
 * can be used with a single {@link io.bootique.BQRuntime}. If you have multiple BQRuntimes in a test, you will need to
 * declare a separate CayenneTester for each one of them.
 *
 * @since 2.0
 */
public class CayenneTester implements BQBeforeMethodCallback {

    private boolean refreshCayenneCaches;
    private boolean deleteBeforeEachTest;
    private boolean skipSchemaCreation;
    private Collection<Class<? extends Persistent>> entities;
    private Collection<Class<? extends Persistent>> entityGraphRoots;
    private Collection<String> tables;
    private Collection<String> tableGraphRoots;
    private Collection<RelatedEntity> relatedTables;

    private CayenneTesterBootiqueHook bootiqueHook;
    private CayenneRuntimeManager runtimeManager;
    private CommitCounter commitCounter;

    public static CayenneTester create() {
        return new CayenneTester();
    }

    protected CayenneTester() {

        this.bootiqueHook = new CayenneTesterBootiqueHook()
                .onInit(r -> resolveRuntimeManager(r))
                .onInit(r -> createSchema());

        this.refreshCayenneCaches = true;
        this.deleteBeforeEachTest = false;
        this.skipSchemaCreation = false;
        this.commitCounter = new CommitCounter();
    }

    public CayenneTester doNoRefreshCayenneCaches() {
        this.refreshCayenneCaches = false;
        return this;
    }

    public CayenneTester skipSchemaCreation() {
        this.skipSchemaCreation = true;
        return this;
    }

    /**
     * Configures the Tester to manage a subset of entities out a potentially very large number of entities in the model.
     *
     * @param entities a list of entities to manage (create schema for, delete test data, etc.)
     * @return this tester
     */
    public CayenneTester entities(Class<? extends Persistent>... entities) {

        if (this.entities == null) {
            this.entities = new HashSet<>();
        }

        for (Class<? extends Persistent> e : entities) {
            this.entities.add(e);
        }

        return this;
    }

    public CayenneTester entitiesAndDependencies(Class<? extends Persistent>... entities) {
        if (this.entityGraphRoots == null) {
            this.entityGraphRoots = new HashSet<>();
        }

        for (Class<? extends Persistent> e : entities) {
            this.entityGraphRoots.add(e);
        }

        return this;
    }

    public CayenneTester tables(String... tables) {

        if (this.tables == null) {
            this.tables = new HashSet<>();
        }

        for (String t : tables) {
            this.tables.add(t);
        }

        return this;
    }

    public CayenneTester tablesAndDependencies(String... tables) {

        if (this.tableGraphRoots == null) {
            this.tableGraphRoots = new HashSet<>();
        }

        for (String t : tables) {
            this.tableGraphRoots.add(t);
        }

        return this;
    }

    public CayenneTester relatedTables(Class<? extends Persistent> entityType, Property<?> relationship) {

        if (this.relatedTables == null) {
            this.relatedTables = new HashSet<>();
        }

        this.relatedTables.add(new RelatedEntity(entityType, relationship.getName()));
        return this;
    }

    /**
     * Configures the Tester to delete data corresponding to the tester's entity model before each test.
     *
     * @return this tester
     */
    public CayenneTester deleteBeforeEachTest() {
        this.deleteBeforeEachTest = true;
        return this;
    }

    /**
     * Returns a new Bootique module that registers Cayenne test extensions. This module should be passed to a single
     * test {@link io.bootique.BQRuntime} to associate the tester with that runtime. This would allow the tester to
     * interact with Cayenne stack inside that runtime to perform its functions through the test lifecycle.
     *
     * @return a new Bootique module that registers Cayenne test extensions
     */
    public BQModule moduleWithTestHooks() {
        return this::configure;
    }

    protected void configure(Binder binder) {
        CayenneModule.extend(binder).addSyncFilter(commitCounter);

        binder.bind(CayenneTesterBootiqueHook.class)
                // wrapping the hook in provider to be able to run the checks for when this tester is erroneously
                // used for multiple runtimes
                .toProviderInstance(new CayenneTesterBootiqueHookProvider(bootiqueHook))
                // using "initOnStartup" to cause immediate Cayenne initialization. Any downsides?
                .initOnStartup();
    }

    public ServerRuntime getRuntime() {
        return bootiqueHook.getRuntime();
    }

    protected CayenneRuntimeManager getRuntimeManager() {
        Assertions.assertNotNull(runtimeManager, "Cayenne runtime is not resolved. Called outside of test lifecycle?");
        return runtimeManager;
    }

    public String getTableName(Class<? extends Persistent> entity) {
        ObjEntity e = getRuntime().getDataDomain().getEntityResolver().getObjEntity(entity);
        if (e == null) {
            throw new IllegalStateException("Type is not mapped in Cayenne: " + entity);
        }

        return e.getDbEntity().getName();
    }

    /**
     * Checks whether Cayenne performed the expected number of DB commits within a single test method.
     */
    public void assertCommitCount(int expected) {
        commitCounter.assertCount(expected);
    }

    protected void resolveRuntimeManager(ServerRuntime runtime) {
        this.runtimeManager = CayenneRuntimeManager
                .builder(runtime.getDataDomain())
                .entities(entities)
                .entityGraphRoots(entityGraphRoots)
                .tables(tables)
                .tableGraphRoots(tableGraphRoots)
                .relatedEntities(relatedTables)
                .build();
    }

    protected void createSchema() {
        if (!skipSchemaCreation) {
            getRuntimeManager().createSchema();
        }
    }

    @Override
    public void beforeMethod(BQTestScope scope, ExtensionContext context) {

        // Skipping cleanup workflow steps before the first test. Assuming things are clean.
        // TODO: are we shooting ourselves in the foot with this? Is it reasonable to expect a dirty
        //   DB before the first test?

        if (!bootiqueHook.initIfNeeded()) {

            if (refreshCayenneCaches) {
                getRuntimeManager().refreshCaches();
            }

            if (deleteBeforeEachTest) {
                getRuntimeManager().deleteData();
            }

            commitCounter.resetCounter();
        }
    }
}
