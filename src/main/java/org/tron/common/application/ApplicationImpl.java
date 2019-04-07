package org.tron.common.application;

import com.google.common.collect.Streams;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetService;
import org.tron.protos.Protocol.Vote;

@Slf4j(topic = "app")
@Component
public class ApplicationImpl implements Application {

  private BlockStore blockStoreDb;
  private ServiceContainer services;

  @Autowired
  private TronNetService tronNetService;

  @Autowired
  private Manager dbManager;

  private boolean isProducer;

  @Override
  public void setOptions(Args args) {
    // not used
  }

  @Override
  @Autowired
  public void init(Args args) {
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
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
    tronNetService.start();
  }

  @Override
  public void shutdown() {
    List<Entry<byte[], VotesCapsule>> list = Streams.stream(dbManager.getVotesStore()).collect(Collectors.toList());
    Map<String, Long> hmap = new HashMap<>();
    for (Entry<byte[], VotesCapsule> v : list) {
      for (Vote p :v.getValue().getNewVotes()) {
        String address = ByteArray.toHexString(p.getVoteAddress().toByteArray());
        if (hmap.containsKey(address)) {
          hmap.put(address, new Long(0));
        }
        hmap.put(address, hmap.get(address) + p.getVoteCount());
      }

      for (Vote p :v.getValue().getOldVotes()) {
        String address = ByteArray.toHexString(p.getVoteAddress().toByteArray());
        if (hmap.containsKey(address)) {
          hmap.put(address, new Long(0));
        }
        hmap.put(address, hmap.get(address) - p.getVoteCount());
      }
    }

    logger.info("******** result ********");

    Streams.stream(hmap.entrySet()).sorted(Comparator.comparing(Entry::getValue)).forEach(
        stringLongEntry -> {
          logger.info("address: " + stringLongEntry.getKey() + "votes: " + stringLongEntry.getValue());
        }
    );

    logger.info("******** begin to shutdown ********");

     tronNetService.close();
    synchronized (dbManager.getRevokingStore()) {
      closeRevokingStore();
      closeAllStore();
    }
    dbManager.stopRepushThread();
    dbManager.stopRepushTriggerThread();
    EventPluginLoader.getInstance().stopPlugin();
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

  private void closeRevokingStore() {
    logger.info("******** begin to closeRevokingStore ********");
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
