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

package org.apache.ignite.internal.sql.engine.exec.rel;

import static org.apache.ignite.internal.util.CollectionUtils.nullOrEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.calcite.rel.core.TableModify;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.ignite.internal.schema.BinaryRow;
import org.apache.ignite.internal.sql.engine.exec.ExecutionContext;
import org.apache.ignite.internal.sql.engine.exec.RowHandler;
import org.apache.ignite.internal.sql.engine.schema.InternalIgniteTable;
import org.apache.ignite.internal.sql.engine.type.IgniteTypeFactory;
import org.apache.ignite.internal.table.InternalTable;
import org.apache.ignite.lang.IgniteInternalException;

/**
 * ModifyNode.
 * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
 */
public class ModifyNode<RowT> extends AbstractNode<RowT> implements SingleNode<RowT>, Downstream<RowT> {
    private final InternalIgniteTable table;

    private final TableModify.Operation op;

    private final List<String> cols;

    private final InternalTable tableView;

    private List<BinaryRow> rows = new ArrayList<>(MODIFY_BATCH_SIZE);

    private long updatedRows;

    private int waiting;

    private int requested;

    private boolean inLoop;

    private State state = State.UPDATING;

    /**
     * Constructor.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     *
     * @param ctx  Execution context.
     * @param rowType Rel data type.
     * @param table Table object.
     * @param op Operation/
     * @param cols Update column list.
     */
    public ModifyNode(
            ExecutionContext<RowT> ctx,
            RelDataType rowType,
            InternalIgniteTable table,
            TableModify.Operation op,
            List<String> cols
    ) {
        super(ctx, rowType);

        this.table = table;
        this.op = op;
        this.cols = cols;

        tableView = table.table();
    }

    /** {@inheritDoc} */
    @Override
    public void request(int rowsCnt) throws Exception {
        assert !nullOrEmpty(sources()) && sources().size() == 1;
        assert rowsCnt > 0 && requested == 0;

        checkState();

        requested = rowsCnt;

        if (!inLoop) {
            tryEnd();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void push(RowT row) throws Exception {
        assert downstream() != null;
        assert waiting > 0;
        assert state == State.UPDATING;

        checkState();

        waiting--;

        switch (op) {
            case DELETE:
            case UPDATE:
            case INSERT:
                rows.add(table.toBinaryRow(context(), row, op, cols));

                flushTuples(false);

                break;
            default:
                throw new UnsupportedOperationException(op.name());
        }

        if (waiting == 0) {
            source().request(waiting = MODIFY_BATCH_SIZE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void end() throws Exception {
        assert downstream() != null;
        assert waiting > 0;

        checkState();

        waiting = -1;
        state = State.UPDATED;

        tryEnd();
    }

    /** {@inheritDoc} */
    @Override
    protected void rewindInternal() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    protected Downstream<RowT> requestDownstream(int idx) {
        if (idx != 0) {
            throw new IndexOutOfBoundsException();
        }

        return this;
    }

    private void tryEnd() throws Exception {
        assert downstream() != null;

        if (state == State.UPDATING && waiting == 0) {
            source().request(waiting = MODIFY_BATCH_SIZE);
        }

        if (state == State.UPDATED && requested > 0) {
            flushTuples(true);

            state = State.END;

            inLoop = true;
            try {
                requested--;
                downstream().push(context().rowHandler().factory(long.class).create(updatedRows));
            } finally {
                inLoop = false;
            }
        }

        if (state == State.END && requested > 0) {
            requested = 0;
            downstream().end();
        }
    }

    private void flushTuples(boolean force) {
        if (nullOrEmpty(rows) || !force && rows.size() < MODIFY_BATCH_SIZE) {
            return;
        }

        List<BinaryRow> rows = this.rows;
        this.rows = new ArrayList<>(MODIFY_BATCH_SIZE);

        // TODO: IGNITE-15087 Implement support for transactional SQL
        switch (op) {
            case INSERT:
                Collection<BinaryRow> duplicates = tableView.insertAll(rows, null).join();

                if (!duplicates.isEmpty()) {
                    IgniteTypeFactory typeFactory = context().getTypeFactory();
                    RowHandler.RowFactory<RowT> rowFactory = context().rowHandler().factory(
                            context().getTypeFactory(),
                            table.descriptor().insertRowType(typeFactory)
                    );

                    throw new IgniteInternalException(
                            "Failed to INSERT some keys because they are already in cache. "
                                    + "[rows=" + duplicates.stream()
                                    .map(binRow -> table.toRow(context(), binRow, rowFactory, null))
                                    .map(context().rowHandler()::toString)
                                    .collect(Collectors.toList()) + ']'
                    );
                }

                break;
            case UPDATE:
                tableView.upsertAll(rows, null).join();

                break;
            case DELETE:
                tableView.deleteAll(rows, null).join();

                break;
            default:
                throw new AssertionError();
        }

        updatedRows += rows.size();
    }

    private enum State {
        UPDATING,

        UPDATED,

        END
    }
}
