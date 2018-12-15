package org.tron.program;

import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.core.SnapshotImpl;

class RunnableDemo implements Runnable {
  private Thread t;
  private String threadName;
  static    HashDB db = new HashDB();
  RunnableDemo( String name) {
    threadName = name;
    System.out.println("Creating " +  threadName );
  }

  public void run() {

  }

  public void start () {

    if (t == null) {
      t = new Thread (this, threadName);
      t.start ();
    }
  }
}
