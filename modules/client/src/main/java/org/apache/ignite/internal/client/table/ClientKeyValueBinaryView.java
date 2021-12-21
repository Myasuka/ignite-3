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

package org.apache.ignite.internal.client.table;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.internal.client.proto.ClientMessageUnpacker;
import org.apache.ignite.internal.client.proto.ClientOp;
import org.apache.ignite.lang.NullableValue;
import org.apache.ignite.table.InvokeProcessor;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Client key-value view implementation for binary user-object representation.
 */
public class ClientKeyValueBinaryView implements KeyValueView<Tuple, Tuple> {
    /** Underlying table. */
    private final ClientTable tbl;

    /**
     * Constructor.
     *
     * @param tbl Table.
     */
    public ClientKeyValueBinaryView(ClientTable tbl) {
        assert tbl != null;

        this.tbl = tbl;
    }

    /** {@inheritDoc} */
    @Override
    public Tuple get(@Nullable Transaction tx, @NotNull Tuple key) {
        return getAsync(tx, key).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Tuple> getAsync(@Nullable Transaction tx, @NotNull Tuple key) {
        Objects.requireNonNull(key);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutInOpAsync(
                ClientOp.TUPLE_GET,
                (schema, out) -> tbl.writeTuple(key, schema, out, true),
                ClientTable::readValueTuple);
    }

    /** {@inheritDoc} */
    @Override
    public Map<Tuple, Tuple> getAll(@Nullable Transaction tx, @NotNull Collection<Tuple> keys) {
        return getAllAsync(tx, keys).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Map<Tuple, Tuple>> getAllAsync(@Nullable Transaction tx, @NotNull Collection<Tuple> keys) {
        Objects.requireNonNull(keys);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutInOpAsync(
                ClientOp.TUPLE_GET_ALL,
                (s, w) -> tbl.writeTuples(keys, s, w, true),
                tbl::readKvTuplesNullable,
                Collections.emptyMap());
    }

    /**
     * This method is not supported, {@link #get(Transaction, Tuple)} must be used instead.
     *
     * <p>Because of binary view doesn't allow null values, binary view method {@link #get(Transaction, Tuple)} returns {@code null} if and
     * only if no value exists for the given key. Thus, this method is redundant.
     *
     * @throws UnsupportedOperationException unconditionally.
     */
    @Override
    public NullableValue<Tuple> getNullable(@Nullable Transaction tx, @NotNull Tuple key) {
        throw new UnsupportedOperationException("Binary view doesn't allow null values.");
    }

    /**
     * This method is not supported, {@link #getAsync(Transaction, Tuple)} must be used instead.
     *
     * <p>Because of binary view doesn't allow null values, binary view method {@link #get(Transaction, Tuple)} returns {@code null} if and
     * only if no value exists for the given key. Thus, this method is redundant.
     *
     * @throws UnsupportedOperationException unconditionally.
     */
    @Override
    public @NotNull CompletableFuture<NullableValue<Tuple>> getNullableAsync(@Nullable Transaction tx, @NotNull Tuple key) {
        throw new UnsupportedOperationException("Binary view doesn't allow null values.");
    }

    /** {@inheritDoc} */
    @Override
    public Tuple getOrDefault(@Nullable Transaction tx, @NotNull Tuple key, @NotNull Tuple defaultValue) {
        return getOrDefaultAsync(tx, key, defaultValue).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Tuple> getOrDefaultAsync(@Nullable Transaction tx, @NotNull Tuple key, @NotNull Tuple defaultValue) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(defaultValue);

        return tbl.doSchemaOutInOpAsync(
                ClientOp.TUPLE_GET,
                (schema, out) -> tbl.writeTuple(key, schema, out, true),
                (schema, in) -> Objects.requireNonNullElse(ClientTable.readValueTuple(schema, in), defaultValue));
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(@Nullable Transaction tx, @NotNull Tuple key) {
        return containsAsync(tx, key).join();
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Boolean> containsAsync(@Nullable Transaction tx, @NotNull Tuple key) {
        Objects.requireNonNull(key);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_CONTAINS_KEY,
                (schema, out) -> tbl.writeTuple(key, schema, out, true),
                ClientMessageUnpacker::unpackBoolean);
    }

    /** {@inheritDoc} */
    @Override
    public void put(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        putAsync(tx, key, val).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Void> putAsync(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        Objects.requireNonNull(key);
        // TODO: Transactions IGNITE-15240
        // TODO IGNITE-15194: Convert Tuple to a schema-order Array as a first step.
        // If it does not match the latest schema, then request latest and convert again.
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_UPSERT,
                (s, w) -> tbl.writeKvTuple(key, val, s, w, false),
                r -> null);
    }

    /** {@inheritDoc} */
    @Override
    public void putAll(@Nullable Transaction tx, @NotNull Map<Tuple, Tuple> pairs) {
        putAllAsync(tx, pairs).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Void> putAllAsync(@Nullable Transaction tx, @NotNull Map<Tuple, Tuple> pairs) {
        Objects.requireNonNull(pairs);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_UPSERT_ALL,
                (s, w) -> tbl.writeKvTuples(pairs, s, w),
                r -> null);
    }

    /** {@inheritDoc} */
    @Override
    public Tuple getAndPut(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        return getAndPutAsync(tx, key, val).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Tuple> getAndPutAsync(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutInOpAsync(
                ClientOp.TUPLE_GET_AND_UPSERT,
                (s, w) -> tbl.writeKvTuple(key, val, s, w, false),
                ClientTable::readValueTuple);
    }

    /** {@inheritDoc} */
    @Override
    public boolean putIfAbsent(@Nullable Transaction tx, @NotNull Tuple key, @NotNull Tuple val) {
        return putIfAbsentAsync(tx, key, val).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Boolean> putIfAbsentAsync(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_INSERT,
                (s, w) -> tbl.writeKvTuple(key, val, s, w, false),
                ClientMessageUnpacker::unpackBoolean);
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(@Nullable Transaction tx, @NotNull Tuple key) {
        return removeAsync(tx, key).join();
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(@Nullable Transaction tx, @NotNull Tuple key, @NotNull Tuple val) {
        return removeAsync(tx, key, val).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Boolean> removeAsync(@Nullable Transaction tx, @NotNull Tuple key) {
        Objects.requireNonNull(key);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_DELETE,
                (s, w) -> tbl.writeTuple(key, s, w, true),
                ClientMessageUnpacker::unpackBoolean);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Boolean> removeAsync(@Nullable Transaction tx, @NotNull Tuple key, @NotNull Tuple val) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(val);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_DELETE_EXACT,
                (s, w) -> tbl.writeKvTuple(key, val, s, w, false),
                ClientMessageUnpacker::unpackBoolean);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Tuple> removeAll(@Nullable Transaction tx, @NotNull Collection<Tuple> keys) {
        return removeAllAsync(tx, keys).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Collection<Tuple>> removeAllAsync(@Nullable Transaction tx, @NotNull Collection<Tuple> keys) {
        Objects.requireNonNull(keys);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutInOpAsync(
                ClientOp.TUPLE_DELETE_ALL,
                (s, w) -> tbl.writeTuples(keys, s, w, true),
                (schema, in) -> tbl.readTuples(schema, in, true),
                Collections.emptyList());
    }

    /** {@inheritDoc} */
    @Override
    public Tuple getAndRemove(@Nullable Transaction tx, @NotNull Tuple key) {
        return getAndRemoveAsync(tx, key).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Tuple> getAndRemoveAsync(@Nullable Transaction tx, @NotNull Tuple key) {
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutInOpAsync(
                ClientOp.TUPLE_GET_AND_DELETE,
                (s, w) -> tbl.writeTuple(key, s, w, true),
                ClientTable::readValueTuple);
    }

    /** {@inheritDoc} */
    @Override
    public boolean replace(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        return replaceAsync(tx, key, val).join();
    }

    /** {@inheritDoc} */
    @Override
    public boolean replace(@Nullable Transaction tx, @NotNull Tuple key, Tuple oldVal, Tuple newVal) {
        return replaceAsync(tx, key, oldVal, newVal).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Boolean> replaceAsync(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        Objects.requireNonNull(key);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_REPLACE,
                (s, w) -> tbl.writeKvTuple(key, val, s, w, false),
                ClientMessageUnpacker::unpackBoolean);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Boolean> replaceAsync(@Nullable Transaction tx, @NotNull Tuple key, Tuple oldVal, Tuple newVal) {
        Objects.requireNonNull(key);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutOpAsync(
                ClientOp.TUPLE_REPLACE_EXACT,
                (s, w) -> {
                    tbl.writeKvTuple(key, oldVal, s, w, false);
                    tbl.writeKvTuple(key, newVal, s, w, true);
                },
                ClientMessageUnpacker::unpackBoolean);
    }

    /** {@inheritDoc} */
    @Override
    public Tuple getAndReplace(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        return getAndReplaceAsync(tx, key, val).join();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CompletableFuture<Tuple> getAndReplaceAsync(@Nullable Transaction tx, @NotNull Tuple key, Tuple val) {
        Objects.requireNonNull(key);
        // TODO: Transactions IGNITE-15240
        return tbl.doSchemaOutInOpAsync(
                ClientOp.TUPLE_GET_AND_REPLACE,
                (s, w) -> tbl.writeKvTuple(key, val, s, w, false),
                ClientTable::readValueTuple);
    }

    /** {@inheritDoc} */
    @Override
    public <R extends Serializable> R invoke(
            @Nullable Transaction tx,
            @NotNull Tuple key,
            InvokeProcessor<Tuple, Tuple, R> proc,
            Serializable... args
    ) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull <R extends Serializable> CompletableFuture<R> invokeAsync(
            @Nullable Transaction tx,
            @NotNull Tuple key,
            InvokeProcessor<Tuple, Tuple, R> proc,
            Serializable... args
    ) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /** {@inheritDoc} */
    @Override
    public <R extends Serializable> Map<Tuple, R> invokeAll(
            @Nullable Transaction tx,
            @NotNull Collection<Tuple> keys,
            InvokeProcessor<Tuple, Tuple, R> proc,
            Serializable... args
    ) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull <R extends Serializable> CompletableFuture<Map<Tuple, R>> invokeAllAsync(
            @Nullable Transaction tx,
            @NotNull Collection<Tuple> keys,
            InvokeProcessor<Tuple, Tuple, R> proc,
            Serializable... args
    ) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
