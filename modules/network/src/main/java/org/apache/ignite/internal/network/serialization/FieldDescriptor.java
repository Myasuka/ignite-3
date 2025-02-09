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

package org.apache.ignite.internal.network.serialization;

import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;

/**
 * Field descriptor for the user object serialization.
 */
public class FieldDescriptor {
    /**
     * Name of the field.
     */
    private final String name;

    /**
     * Type of the field.
     */
    private final Class<?> clazz;

    /**
     * Field type's descriptor id.
     */
    private final int typeDescriptorId;

    /**
     * Whether the field is serialized as unshared from the point of view of Java Serialization specification.
     */
    private final boolean unshared;

    /**
     * Accessor for accessing this field.
     */
    private final FieldAccessor accessor;

    /**
     * Constructor.
     */
    public FieldDescriptor(Field field, int typeDescriptorId) {
        this(field.getName(), field.getType(), typeDescriptorId, false, new UnsafeFieldAccessor(field));
    }

    /**
     * Constructor.
     *
     * @param fieldName         field name
     * @param fieldClazz        type of the field
     * @param typeDescriptorId  ID of the descriptor corresponding to field type
     * @param declaringClass    the class in which the field if declared
     */
    public FieldDescriptor(String fieldName, Class<?> fieldClazz, int typeDescriptorId, boolean unshared, Class<?> declaringClass) {
        this(fieldName, fieldClazz, typeDescriptorId, unshared, new UnsafeFieldAccessor(fieldName, declaringClass));
    }

    private FieldDescriptor(String fieldName, Class<?> fieldClazz, int typeDescriptorId, boolean unshared, FieldAccessor accessor) {
        this.name = fieldName;
        this.clazz = fieldClazz;
        this.typeDescriptorId = typeDescriptorId;
        this.unshared = unshared;
        this.accessor = accessor;
    }

    /**
     * Returns field's name.
     *
     * @return Field's name.
     */
    @NotNull
    public String name() {
        return name;
    }

    /**
     * Returns field's type.
     *
     * @return Field's type.
     */
    @NotNull
    public Class<?> clazz() {
        return clazz;
    }

    /**
     * Returns field's type descriptor id.
     *
     * @return Field's type descriptor id.
     */
    public int typeDescriptorId() {
        return typeDescriptorId;
    }

    /**
     * Returns whether the field is serialized as unshared from the point of view of Java Serialization specification.
     *
     * @return whether the field is serialized as unshared from the point of view of Java Serialization specification
     */
    public boolean isUnshared() {
        return unshared;
    }

    /**
     * Returns {@code true} if this field has a primitive type.
     *
     * @return {@code true} if this field has a primitive type
     */
    public boolean isPrimitive() {
        return clazz.isPrimitive();
    }

    /**
     * Returns {@code true} if the field can only host (at runtime) instances of its declared type (and not subtypes),
     * so the runtime marshalling type is known upfront. This is also true for enums, even though technically their values might
     * have subtypes; but we serialize them using their names, so we still treat the type as known upfront.
     *
     * @return {@code true} if the field can only host (at runtime) instances of the declared type that is known upfront
     */
    public boolean isRuntimeTypeKnownUpfront() {
        return Classes.isRuntimeTypeKnownUpfront(clazz);
    }

    /**
     * Returns {@link FieldAccessor} for this field.
     *
     * @return {@link FieldAccessor} for this field
     */
    public FieldAccessor accessor() {
        return accessor;
    }
}
