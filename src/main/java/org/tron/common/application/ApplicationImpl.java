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
    Map<String,Integer> map= new TreeMap<String, Integer>(
      new Comparator<String>() {
        public int compare(String obj1, String obj2) {
          // 降序排序
          return obj2.compareTo(obj1);
        }
      });

    long startNumber = 3800000;
    for (int i = 0; i < 10000; i ++ ) {
      List<BlockCapsule> value =  dbManager.getBlockStore().getLimitNumber(startNumber + i * 1000, 1000);
      if (value.size() == 0) break;
      for (int j = 0; j < value.size(); j ++ ){
        BlockCapsule bl = value.get(j);
        Date date = new Date(bl.getTimeStamp());
        if (map.containsKey(formatter.format(date))){
          map.put(formatter.format(date),map.get(formatter.format(date))+1);
        }else{
          map.put(formatter.format(date),1);
        }
      }
    }

    for(String key:map.keySet())
    {
      System.out.println("1Key: "+key +" Value: "+(28800-map.get(key)));
    }

    logger.info("******** tmp ********");

    Map<Long,List<String>> mapHash= new TreeMap<Long, List<String>>();

    for (int i = 0; i < 10000; i ++ ) {
      List<BlockCapsule> value =  dbManager.getBlockStore().getLimitNumber(startNumber + i * 1000, 1000);
      if (value.size() == 0) break;
      for (int j = 0; j < value.size(); j ++ ){
        BlockCapsule bl = value.get(j);
        long time =  bl.getTimeStamp() / 1000 / 3600 / 6;
        //System.out.println("===" + time);
        if (mapHash.containsKey(time)) {
          List<String> arrayList = mapHash.get(time);
          String tmp =  ByteArray.toHexString(bl.getWitnessAddress().toByteArray());
          arrayList.add(tmp.toLowerCase());
          mapHash.put(time, arrayList);
        } else {
          List<String> arrayList = new ArrayList<String>();
          String tmp =  ByteArray.toHexString(bl.getWitnessAddress().toByteArray());
          arrayList.add(tmp);
          mapHash.put(time, arrayList);
        }
      }
    }
    Map<String,Map<String, Double>> mapHash1= new TreeMap<String, Map<String, Double>>();

    for (Long time : mapHash.keySet()) {
      List<String> tmp = mapHash.get(time);
      Map<String,Integer> tmap = new HashMap<String, Integer>();
      for (String p : tmp) {
        if (tmap.containsKey(p)){
          tmap.put(p,tmap.get(p)+1);
        }else{
          tmap.put(p,1);
        }
      }
      Date date = new Date(time *1000 *3600 * 6);
      String nowDay = formatter.format(date);

      for(String p : tmap.keySet()) {
        System.out.println("time "+  formatter1.format(date) +"Key: "+p +" Value: "+(tmap.get(p)));
        double number = 266.66  - tmap.get(p);
        if (mapHash1.containsKey(formatter.format(date)) == false) {
          Map<String, Double> tmpHash = new TreeMap<String, Double>();
          tmpHash.put(p, number);
          mapHash1.put(formatter.format(date), tmpHash);
        } else {
          Map<String, Double> tmpHash = mapHash1.get(formatter.format(date));
          if (tmpHash.containsKey(p)){
            tmpHash.put(p, tmpHash.get(p) + number);
          } else {
            tmpHash.put(p , number);
          }
          mapHash1.put(formatter.format(date), tmpHash);
        }
      }
    }
    for (String p : mapHash1.keySet()) {
      System.out.println("date" + p);
      Map<String, Double> value = mapHash1.get(p);
      for(String address : value.keySet()) {
        System.out.println("address: "+address +" Value: "+ value.get(address));
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
