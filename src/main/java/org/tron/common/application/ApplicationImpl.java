package org.tron.common.application;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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
    Map<String,Integer> map= new TreeMap<String, Integer>(
      new Comparator<String>() {
        public int compare(String obj1, String obj2) {
          // 降序排序
          return obj2.compareTo(obj1);
        }
      });

    List<String> strList = new ArrayList<String>();
    strList.add("41bac7378c4265ad2739772337682183b8864f517a");
    strList.add("4138E3E3A163163DB1F6CFCECA1D1C64594DD1F0CA");
    strList.add("418A445FACC2AA94D72292EBBCB2A611E9FD8A6C6E");
    strList.add("41B3EEC71481E8864F0FC1F601B836B74C40548287");
    strList.add("41F29F57614A6B201729473C837E1D2879E9F90B8E");
    strList.add("41C4BC4D7F64DF4FD3670CE38E1A60080A50DA85CF");

    strList.add("41E425FE49E76E7573B7AA746AC92FBF795B6B660E");
    strList.add("41C05142FD1CA1E03688A43585096866AE658F2CB2");
    strList.add("417312080619A24D38A2029B724FF5C84D8F2E4483");
    strList.add("41B487CDC02DE90F15AC89A68C82F44CBFE3D915EA");
    strList.add("41FC45DA0E51966BD1AF2CB1E0F66633F160603A8B");
    strList.add("41D376D829440505EA13C9D1C455317D51B62E4AB6");

    strList.add("418C66E4883782B793FCF2DCB92B23EECE57769499");
    strList.add("41138F49159E14514FCD94123C890DB4017D052A74");
    strList.add("4103D10C315E718469E4F722D419B8E68C580D21A5");
    strList.add("4168E4FE723FD4CE2A5F477FA909FBC332DAA182E0");
    strList.add("4156666312038141C3D42D54D8774B176A75F4E38D");

    strList.add("41997FE4C3EDED578F7E1C6D6EDD561EEFE13D4F6A");
    strList.add("417BBB661ACCB717BC398202A0713C93966205BFB4");
    strList.add("41A12B770C11EF829E87978CFB045D76BA4E1D2EF0");
    strList.add("415555CAA1ADD8D7DDE489B5A0BFB3BBF9784BC824");
    strList.add("4182AD049B511AE51CBE60C9AACB31B5F56A80C152");
    strList.add("419D23D9E9CDEA86C82EE531A6F0A3A417D0F89208");
    strList.add("419F1F87615C2CECE9E6EB5105F91641838620B886");
    strList.add("41A94E66FC4052F74EAB7477CA7CDD672C0585ECC7");
    strList.add("41256457AAD6551D6F28D0798B7CD44BAB5F410F38");


    List<BlockCapsule> value =  dbManager.getBlockStore().getLimitNumber(4000000 , 1000000);
    for (int m = 0; m < strList.size(); m ++) {
      long lastNumber = 0;
      System.out.println("address:"+ strList.get(m));
        for (int j = 0; j < value.size(); j ++ ){
          BlockCapsule bl = value.get(j);
          String tmp =  ByteArray.toHexString(bl.getWitnessAddress().toByteArray());
          if (tmp.equals(strList.get(m))) {
            if (lastNumber != 0 &&  bl.getNum() - lastNumber > 27 ){
              Date date = new Date(bl.getTimeStamp());
              if (map.containsKey(formatter.format(date))){
                map.put(formatter.format(date),map.get(formatter.format(date))+1);
              }else{
                map.put(formatter.format(date),1);
              }
              System.out.println( formatter.format(date)+ " last: "+ lastNumber + "now: " +  bl.getNum());
            }
            System.out.println("blocknumber"+ bl.getNum());
            lastNumber = bl.getNum();
          }
        }


    }
    for(String key:map.keySet())
    {
      System.out.println("Key: "+key +" Value: "+map.get(key));
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
