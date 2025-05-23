/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.api.dataview;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.TableAggregateFunction;
import org.apache.flink.table.types.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link DataView} that provides {@link List}-like functionality in state entries.
 *
 * <p>A {@link ListView} can be backed by a Java {@link ArrayList} or can leverage Flink's state
 * backends depending on the context. In many unbounded data scenarios, the {@link ListView}
 * delegates all calls to a {@link ListState} instead of the {@link ArrayList}.
 *
 * <p>For aggregating functions, the view can be used as a field in the accumulator of an {@link
 * AggregateFunction} or {@link TableAggregateFunction} when large amounts of data are expected.
 * Aggregate functions might be used at various locations (pre-aggregation, combiners, merging of
 * window slides, etc.) for some of these locations the data view is not backed by state but {@link
 * ArrayList}.
 *
 * <p>For process table functions, the view can be used as a top-level state entry. Data views in
 * PTFs are always backed by state.
 *
 * <p>Note: Elements of a {@link ListView} must not be null. For heap-based state backends, {@code
 * hashCode/equals} of the original (i.e. external) class are used. However, the serialization
 * format will use internal data structures.
 *
 * <p>The {@link DataType} of the view's elements is reflectively extracted from the accumulator
 * definition. This includes the generic argument {@code T} of this class. If reflective extraction
 * is not successful, it is possible to use a {@link DataTypeHint} on top the accumulator field. It
 * will be mapped to the underlying collection.
 *
 * <p>The following examples show how to specify an {@link AggregateFunction} with a {@link
 * ListView}:
 *
 * <pre>{@code
 *  public class MyAccumulator {
 *
 *    public ListView<String> list = new ListView<>();
 *
 *    // or explicit:
 *    // @DataTypeHint("ARRAY < STRING >")
 *    // public ListView<String> list = new ListView<>();
 *
 *    public long count = 0L;
 *  }
 *
 *  public class MyAggregateFunction extends AggregateFunction<String, MyAccumulator> {
 *
 *   @Override
 *   public MyAccumulator createAccumulator() {
 *     return new MyAccumulator();
 *   }
 *
 *   public void accumulate(MyAccumulator accumulator, String id) {
 *     accumulator.list.add(id);
 *     accumulator.count++;
 *   }
 *
 *   @Override
 *   public String getValue(MyAccumulator accumulator) {
 *     // return the count and the joined elements
 *     return count + ": " + String.join("|", acc.list.get());
 *   }
 * }
 *
 * }</pre>
 *
 * @param <T> element type
 */
@PublicEvolving
public class ListView<T> implements DataView {

    private List<T> list = new ArrayList<>();

    /**
     * Creates a list view.
     *
     * <p>The {@link DataType} of the contained elements is reflectively extracted.
     */
    public ListView() {
        // default constructor
    }

    /** Returns the entire view's content as an instance of {@link List}. */
    public List<T> getList() {
        return list;
    }

    /** Replaces the entire view's content with the content of the given {@link List}. */
    public void setList(List<T> list) {
        this.list = list;
    }

    /**
     * Returns an iterable of the list view.
     *
     * @throws Exception Thrown if the system cannot get data.
     * @return The iterable of the list.
     */
    public Iterable<T> get() throws Exception {
        return list;
    }

    /**
     * Adds the given value to the list.
     *
     * @throws Exception Thrown if the system cannot add data.
     * @param value The element to be appended to this list view.
     */
    public void add(T value) throws Exception {
        list.add(value);
    }

    /**
     * Adds all of the elements of the specified list to this list view.
     *
     * @throws Exception Thrown if the system cannot add all data.
     * @param list The list with the elements that will be stored in this list view.
     */
    public void addAll(List<T> list) throws Exception {
        this.list.addAll(list);
    }

    /**
     * Removes the given value from the list.
     *
     * @param value The element to be removed from this list view.
     */
    public boolean remove(T value) throws Exception {
        return list.remove(value);
    }

    /** Removes all elements from this list view. */
    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListView)) {
            return false;
        }
        final ListView<?> listView = (ListView<?>) o;
        return getList().equals(listView.getList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getList());
    }

    // --------------------------------------------------------------------------------------------
    // Utilities
    // --------------------------------------------------------------------------------------------

    /** Utility method for creating a {@link DataType} of {@link ListView} explicitly. */
    public static DataType newListViewDataType(DataType elementDataType) {
        return DataTypes.STRUCTURED(
                ListView.class,
                DataTypes.FIELD("list", DataTypes.ARRAY(elementDataType).bridgedTo(List.class)));
    }
}
