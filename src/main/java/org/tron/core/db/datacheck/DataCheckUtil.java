package org.tron.core.db.datacheck;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.RevokingStore;
import org.tron.core.db2.core.RevokingDBWithCachingNewValue;
import org.tron.core.db2.core.SnapshotManager;

//@Slf4j
@Component
public class DataCheckUtil {

  private static final Logger logger = LoggerFactory.getLogger(DataCheckUtil.class);
  @Getter
  @Autowired
  private RevokingDatabase db;

  public void doDataCheck(BlockCapsule block) {
    if (block.getNum() % Args.getInstance().getDataCheckPoint() != 0) {
      return;
    }

    ArrayList<StoreCheckPoint> alist = new ArrayList<>();
    //do the data check when the checkpoint's block comming
    if (db.getClass() == SnapshotManager.class) {
      List<RevokingDBWithCachingNewValue> stores = ((SnapshotManager) db).getDbs();
      for (RevokingDBWithCachingNewValue store : stores) {
        StoreCheckPoint scp = new StoreCheckPoint(store.getDbName());
        Iterator<Entry<byte[], byte[]>> iterator = store.iterator();
        for (; iterator.hasNext(); ) {
          Entry<byte[], byte[]> entry = iterator.next();
          scp.getKeyList().add(entry.getKey());
          scp.getValueList().add(entry.getValue());
        }
        alist.add(scp);
      }
    } else if (db.getClass() == RevokingStore.class) {
      List<LevelDbDataSourceImpl> stores = ((RevokingStore) db).getDbs();

      for (LevelDbDataSourceImpl store : stores) {
        StoreCheckPoint scp = new StoreCheckPoint(store.getDBName());
        Iterator<Entry<byte[], byte[]>> iterator = store.iterator();
        for (; iterator.hasNext(); ) {
          Entry<byte[], byte[]> entry = iterator.next();
          scp.getKeyList().add(entry.getKey());
          scp.getValueList().add(entry.getValue());
        }
        alist.add(scp);
      }
    }

    logger.info("{} block datacheckpoint.", block.getNum());
    for (StoreCheckPoint scp : alist) {
      logger.info(String.valueOf(scp));
    }
  }

  private void dataCheck() {

  }
}