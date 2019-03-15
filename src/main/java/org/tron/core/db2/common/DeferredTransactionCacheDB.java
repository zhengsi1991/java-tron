package org.tron.core.db2.common;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import java.util.Comparator;
import java.util.TreeMap;
import org.tron.common.utils.ByteUtil;
import org.tron.core.db.common.WrappedByteArray;

@Slf4j(topic = "DB")
public class DeferredTransactionCacheDB implements DB<byte[], byte[]>, Flusher {
  private final int MAX_DEFERRED_TRANSACTION = 100000000;

  private Map<Key, byte[]> db = new TreeMap<Key, byte[]>(new Comparator<Key>() {
    @Override
    public int compare(Key o1, Key o2) {
      return ByteUtil.greater(o1.getBytes(), o2.getBytes())? 1 : -1;
    }
  });

  int size = 0;

  @Override
  public synchronized  byte[] get(byte[] key) {
    return db.get(Key.of(key));
  }

  @Override
  public synchronized  void put(byte[] key, byte[] value) {
    if (key == null || value == null || size >= MAX_DEFERRED_TRANSACTION) {
      logger.error("put deferred transaction {} failed, too many pending.");
      return;
    }

    size ++;
    db.put(Key.copyOf(key), value);
  }

  @Override
  public synchronized  long size() {
    return db.size();
  }

  @Override
  public synchronized  boolean isEmpty() {
    return db.isEmpty();
  }

  @Override
  public synchronized  void remove(byte[] key) {
    if (key != null) {
      db.remove(Key.of(key));
      size --;
    }
  }

  @Override
  public synchronized Iterator<Entry<byte[],byte[]>> iterator() {
    return Iterators.transform(db.entrySet().iterator(),
        e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue()));
  }

  @Override
  public synchronized void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach((k, v) -> this.put(k.getBytes(), v.getBytes()));
  }

  @Override
  public void close() {
    reset();
    db = null;
  }

  @Override
  public void reset() {
    db.clear();
  }

  public List<byte[]> getPrevious(byte[] key, long limit, int precision) {
    List<byte[]>  result = new ArrayList<>();
    for (Map.Entry<Key, byte[]> entry : db.entrySet()) {
      if (ByteUtil.lessOrEquals(ByteUtil.parseBytes(entry.getKey().getBytes(), 0, precision), key)) {
        result.add(entry.getValue());
        limit --;
      } else {
        break;
      }
      if (limit <= 0) break;
    }
    return result;
  }
}
