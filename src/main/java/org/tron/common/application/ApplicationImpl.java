package org.tron.common.application;

import com.googlecode.cqengine.query.simple.In;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.net.node.Node;
import org.tron.core.net.node.NodeDelegate;
import org.tron.core.net.node.NodeDelegateImpl;
import org.tron.core.net.node.NodeImpl;

@Slf4j
@Component
public class ApplicationImpl implements Application {

  @Autowired
  private NodeImpl p2pNode;

  private BlockStore blockStoreDb;
  private ServiceContainer services;
  private NodeDelegate nodeDelegate;

  @Autowired
  private Manager dbManager;

  private boolean isProducer;


  private void resetP2PNode() {
    p2pNode.listen();
    p2pNode.syncFrom(null);
  }

  @Override
  public void setOptions(Args args) {
    // not used
  }

  @Override
  @Autowired
  public void init(Args args) {
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
    nodeDelegate = new NodeDelegateImpl(dbManager);
  }

  @Override
  public void addService(Service service) {
    services.add(service);
  }

  @Override
  public void initServices(Args args) {
    services.init(args);
  }

  /**
   * start up the app.
   */
  public void startup() {
    p2pNode.setNodeDelegate(nodeDelegate);
    resetP2PNode();
  }

  @Override
  public void shutdown() {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd HH");
    Map<String,List<String>> map= new TreeMap<String, List<String>>();

    long startNumber = 4900000;
    for (int i = 0; i < 10000; i ++ ) {
      List<BlockCapsule> value =  dbManager.getBlockStore().getLimitNumber(startNumber + i * 1000, 1000);
      if (value.size() == 0) break;
      for (int j = 0; j < value.size(); j ++ ){
        BlockCapsule bl = value.get(j);
        Date date = new Date(bl.getTimeStamp());
        if (map.containsKey(formatter.format(date))){
          List<String> tmp = map.get(formatter.format(date));
          tmp.add(ByteArray.toHexString(bl.getWitnessAddress().toByteArray()));
          map.put(formatter.format(date),tmp);
        }else{
          List<String> tmp = new ArrayList<String>();
          tmp.add(ByteArray.toHexString(bl.getWitnessAddress().toByteArray()));
          map.put(formatter.format(date),tmp);
        }
      }
    }

    for(String key:map.keySet())
    {
      List<String> tmp = map.get(key);
      System.out.println("1Key: "+key );
      Map<String,Integer> map2= new TreeMap<String, Integer>();
      for (String p : tmp) {
        if (map2.containsKey(p)) {
          Integer q = map2.get(p);
          map2.put(p, q + 1);
        } else {
          map2.put(p, 1);
        }
      }
      for (String p : map2.keySet()) {
        System.out.println("address: "+p + " value " +(1066 - map2.get(p)) );
      }

    }



    logger.info("******** begin to shutdown ********");
    synchronized (dbManager.getRevokingStore()) {
      closeRevokingStore();
      closeAllStore();
    }
    closeConnection();
    dbManager.stopRepushThread();
    logger.info("******** end to shutdown ********");
  }

  @Override
  public void startServices() {
    services.start();
  }

  @Override
  public void shutdownServices() {
    services.stop();
  }

  @Override
  public Node getP2pNode() {
    return p2pNode;
  }

  @Override
  public BlockStore getBlockStoreS() {
    return blockStoreDb;
  }

  @Override
  public Manager getDbManager() {
    return dbManager;
  }

  public boolean isProducer() {
    return isProducer;
  }

  public void setIsProducer(boolean producer) {
    isProducer = producer;
  }

  private void closeConnection() {
    logger.info("******** begin to shutdown connection ********");
    try {
      p2pNode.close();
    } catch (Exception e) {
      logger.info("failed to close p2pNode. " + e);
    } finally {
      logger.info("******** end to shutdown connection ********");
    }
  }

  private void closeRevokingStore() {
    dbManager.getRevokingStore().shutdown();
  }

  private void closeAllStore() {
//    if (dbManager.getRevokingStore().getClass() == SnapshotManager.class) {
//      ((SnapshotManager) dbManager.getRevokingStore()).getDbs().forEach(IRevokingDB::close);
//    } else {
//      dbManager.closeAllStore();
//    }
    dbManager.closeAllStore();
  }

}
