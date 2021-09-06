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

package org.apache.ignite.query.sql;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Ignite SQL query facade.
 */
public interface IgniteSql {
    /**
     * Creates SQL session.
     *
     * @return Session.
     */
    SqlSession session();

    /**
     * Creates statement.
     *
     * @param sql SQL query template.
     * @return Prepared statement.
     * @throws SQLException If parsing failed.
     */
    SqlStatement prepare(@NotNull String sql);

    /**
     * Kills query by its' id.
     *
     * @param queryID Query id.
     */
    void killQuery(UUID queryID);

    /**
     * Returns statistics facade for table statistics management.
     * <p>
     * Table statistics are used by SQL engine for SQL queries planning.
     *
     * @return Statistics facade.
     */
    // TODO: Do we need this here or move to Table facade?
    IgniteTableStatistics statistics();

    //TODO: Custom function registration. Move to session?
    void registerUserFunction(Class type, String... methodNames); //TODO: Get function details from method annotations.

    void registerUserFunction(Class type);

    void unregistedUserFunction(String functionName);
}

