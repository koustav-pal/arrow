/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.arrow.vector;

import java.util.Iterator;

import com.google.flatbuffers.FlatBufferBuilder;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.util.TransferPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import io.netty.buffer.ArrowBuf;

public abstract class BaseValueVector implements ValueVector {
  private static final Logger logger = LoggerFactory.getLogger(BaseValueVector.class);

  public static final String MAX_ALLOCATION_SIZE_PROPERTY = "arrow.vector.max_allocation_bytes";
  public static final int MAX_ALLOCATION_SIZE = Integer.getInteger(MAX_ALLOCATION_SIZE_PROPERTY, Integer.MAX_VALUE);
  public static final int INITIAL_VALUE_ALLOCATION = 4096;

  protected final BufferAllocator allocator;
  protected final String name;

  protected BaseValueVector(String name, BufferAllocator allocator) {
    this.allocator = Preconditions.checkNotNull(allocator, "allocator cannot be null");
    this.name = name;
  }

  @Override
  public String toString() {
    return super.toString() + "[name = " + name + ", ...]";
  }

  @Override
  public void clear() {
    getMutator().reset();
  }

  @Override
  public void close() {
    clear();
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return getTransferPair(name, allocator);
  }

  public abstract static class BaseAccessor implements ValueVector.Accessor {
    protected BaseAccessor() { }

    @Override
    public boolean isNull(int index) {
      return false;
    }

    @Override
    // override this in case your implementation is faster, see BitVector
    public int getNullCount() {
      int nullCount = 0;
      for (int i = 0; i < getValueCount(); i++) {
        if (isNull(i)) {
          nullCount ++;
        }
      }
      return nullCount;
    }
  }

  public abstract static class BaseMutator implements ValueVector.Mutator {
    protected BaseMutator() { }

    @Override
    public void generateTestData(int values) {}

    //TODO: consider making mutator stateless(if possible) on another issue.
    @Override
    public void reset() {}
  }

  @Override
  public Iterator<ValueVector> iterator() {
    return Iterators.emptyIterator();
  }

  public static boolean checkBufRefs(final ValueVector vv) {
    for(final ArrowBuf buffer : vv.getBuffers(false)) {
      if (buffer.refCnt() <= 0) {
        throw new IllegalStateException("zero refcount");
      }
    }

    return true;
  }

  @Override
  public BufferAllocator getAllocator() {
    return allocator;
  }
}

