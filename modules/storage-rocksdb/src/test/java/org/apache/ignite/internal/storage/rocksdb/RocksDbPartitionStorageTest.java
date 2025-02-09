/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.storage.rocksdb;

import static org.apache.ignite.internal.configuration.ConfigurationTestUtils.fixConfiguration;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Path;
import org.apache.ignite.configuration.schemas.store.DataRegionConfiguration;
import org.apache.ignite.configuration.schemas.store.RocksDbDataRegionChange;
import org.apache.ignite.configuration.schemas.store.RocksDbDataRegionConfigurationSchema;
import org.apache.ignite.configuration.schemas.table.HashIndexConfigurationSchema;
import org.apache.ignite.configuration.schemas.table.TableConfiguration;
import org.apache.ignite.internal.configuration.testframework.ConfigurationExtension;
import org.apache.ignite.internal.configuration.testframework.InjectConfiguration;
import org.apache.ignite.internal.storage.AbstractPartitionStorageTest;
import org.apache.ignite.internal.storage.engine.DataRegion;
import org.apache.ignite.internal.storage.engine.StorageEngine;
import org.apache.ignite.internal.storage.engine.TableStorage;
import org.apache.ignite.internal.testframework.WorkDirectory;
import org.apache.ignite.internal.testframework.WorkDirectoryExtension;
import org.apache.ignite.internal.util.IgniteUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Storage test implementation for {@link RocksDbPartitionStorage}.
 */
@ExtendWith(WorkDirectoryExtension.class)
@ExtendWith(ConfigurationExtension.class)
public class RocksDbPartitionStorageTest extends AbstractPartitionStorageTest {
    private final StorageEngine engine = new RocksDbStorageEngine();

    private TableStorage table;

    private DataRegion dataRegion;

    /**
     * Before each.
     */
    @BeforeEach
    public void setUp(
            @WorkDirectory Path workDir,
            @InjectConfiguration(polymorphicExtensions = RocksDbDataRegionConfigurationSchema.class) DataRegionConfiguration dataRegionCfg,
            @InjectConfiguration(polymorphicExtensions = HashIndexConfigurationSchema.class) TableConfiguration tableCfg
    ) throws Exception {
        dataRegionCfg.change(cfg ->
                cfg.convert(RocksDbDataRegionChange.class).changeSize(16 * 1024).changeWriteBufferSize(16 * 1024)
        ).get();

        dataRegionCfg = fixConfiguration(dataRegionCfg);

        dataRegion = engine.createDataRegion(dataRegionCfg);

        assertThat(dataRegion, is(instanceOf(RocksDbDataRegion.class)));

        dataRegion.start();

        table = engine.createTable(workDir, tableCfg, dataRegion);

        assertThat(table, is(instanceOf(RocksDbTableStorage.class)));

        table.start();

        storage = table.getOrCreatePartition(0);

        assertThat(storage, is(instanceOf(RocksDbPartitionStorage.class)));
    }

    /**
     * After each.
     */
    @AfterEach
    public void tearDown() throws Exception {
        IgniteUtils.closeAll(
                storage,
                table == null ? null : table::stop,
                dataRegion == null ? null : dataRegion::stop,
                engine::stop
        );
    }
}
