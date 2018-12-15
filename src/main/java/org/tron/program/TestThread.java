package org.tron.program;

import static java.lang.Thread.*;

import com.google.common.collect.Streams;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.core.SnapshotImpl;

public class TestThread {

  public static void main(String args[]) {
    SnapshotImpl tmp = new SnapshotImpl();
    for (int i = 0;i < 10000; i ++) {
      String input = "tmp"+String.valueOf(i);
      tmp.getDb().put(Key.copyOf(input.getBytes()), Value.copyOf(Value.Operator.PUT, input.getBytes()));
    }
    ExecutorService executorService = Executors.newFixedThreadPool(2);


    executorService.execute(new Runnable() {
      @Override
      public void run() {
        Streams.stream(tmp.getDb());
      }
    });

    executorService.execute(new Runnable() {
      @Override
      public void run() {
        tmp.getDb().forEach(keyValueEntry -> {
          tmp.getDb().remove(keyValueEntry.getKey());
        });
      }
    });
  }


}
