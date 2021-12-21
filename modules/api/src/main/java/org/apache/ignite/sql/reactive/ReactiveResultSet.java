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

package org.apache.ignite.sql.reactive;

import java.util.concurrent.Flow;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.ResultSetMetadata;
import org.apache.ignite.sql.SqlRow;

/**
 * Reactive result set provides methods to subscribe to the query results in reactive way.
 *
 * <p>Note: It implies to be used with the reactive framework such as ProjectReactor or R2DBC.
 * @see ResultSet
 */
public interface ReactiveResultSet extends Flow.Publisher<SqlRow> {
    /**
     * Returns metadata for the results if the result contains rows ({@link #hasRowSet()} returns {@code true}).
     *
     * @return Metadata publisher.
     * @see ResultSet#metadata()
     */
    Flow.Publisher<ResultSetMetadata> metadata();

    /**
     * Returns publisher for the flag that determines whether the result of the query execution is a collection of rows, or not.
     *
     * Note: {@code false} value published means the query either is conditional or is an update query.
     *
     * @return HasRowSet flag Publisher.
     * @see ResultSet#hasRowSet()
     */
    Flow.Publisher<Boolean> hasRowSet();

    /**
     * Returns publisher for the number of rows affected by the query.
     *
     * Note: Number of row equals to {@code -1} means method is inapplicable, and the query either is conditional or returns rows.
     *
     * @return Publisher for number of rows.
     * @see ResultSet#updateCount()
     */
    Flow.Publisher<Integer> updateCount();

    /**
     * Returns publisher for a flag which determines whether the query that produce this result was a conditional query, or not.
     *
     * Note: {@code false} value published means the query either returns rows or is an update query.
     *
     * @return AppliedFlag Publisher.
     * @see ResultSet#applied()
     */
    Flow.Publisher<Boolean> applied();
}
