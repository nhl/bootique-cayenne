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

package io.bootique.cayenne.v42.test;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.cayenne.v42.test.persistence3.P3T1;
import io.bootique.cayenne.v42.test.persistence3.P3T3;
import io.bootique.cayenne.v42.test.persistence3.P3T4;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit5.BQApp;
import io.bootique.test.junit5.BQTest;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class CayenneTestDataManagerBuilder_DependenciesIT {

    @BQApp(skipRun = true)
    static final BQRuntime runtime = Bootique.app("-c", "classpath:config3.yml").autoLoadModules().createRuntime();

    @Test
    public void testDependentEntities1() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(runtime)
                .entitiesAndDependencies(P3T1.class)
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(4, tables.size());
        assertTrue(tables.contains("p3_t1"));
        assertTrue(tables.contains("p3_t1_t4"));
        assertTrue(tables.contains("p3_t2"));
        assertTrue(tables.contains("p3_t3"));
    }

    @Test
    public void testDependentEntities2() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(runtime)
                .entitiesAndDependencies(P3T4.class)
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(2, tables.size());
        assertTrue(tables.contains("p3_t4"));
        assertTrue(tables.contains("p3_t1_t4"));
    }

    @Test
    public void testDependentEntities3() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(runtime)
                .entitiesAndDependencies(P3T3.class)
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(1, tables.size());
        assertTrue(tables.contains("p3_t3"));
    }

    @Test
    public void testDependentTables1() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(runtime)
                .tablesAndDependencies("p3_t1_t4")
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(1, tables.size());
        assertTrue(tables.contains("p3_t1_t4"));
    }
}