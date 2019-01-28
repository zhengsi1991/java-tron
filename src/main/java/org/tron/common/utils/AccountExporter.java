package org.tron.common.utils;

import java.io.FileWriter;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.BlockStore;
import org.tron.core.services.interfaceOnSolidity.WalletOnSolidity;

@Slf4j
@Component
public class AccountExporter {
  public static final String FILE_NAME = "account.csv";
  public static final AtomicLong EXPORT_NUM = new AtomicLong(0);
  private static WalletOnSolidity walletOnSolidity;

  @Autowired
  public void setWalletOnSolidity(WalletOnSolidity walletOnSolidity) {
    AccountExporter.walletOnSolidity = walletOnSolidity;
  }

  public static void export(BlockStore blockStore, Iterable<Entry<byte[], AccountCapsule>> iterable) {
    Long blockNum = walletOnSolidity.futureGetWithoutTimeout(() -> {
      List<BlockCapsule> blockList = blockStore.getBlockByLatestNum(1);
      if (CollectionUtils.isNotEmpty(blockList)) {
        return blockList.get(0).getNum();
      }
      return 0L;
    });
    if (blockNum != null && blockNum == EXPORT_NUM.get()) {
      export(iterable);
    }
  }

  private static void export(Iterable<Entry<byte[], AccountCapsule>> iterable) {
    try (CSVPrinter printer = new CSVPrinter(new FileWriter(FILE_NAME), CSVFormat.EXCEL.withHeader("address", "balance"))) {
      iterable.forEach(e -> {
        try {
          printer.printRecord(Wallet.encode58Check(e.getKey()), e.getValue().getBalance());
        } catch (Exception e1) {
          logger.error("address {} write error.", Wallet.encode58Check(e.getKey()));
        }
      });
    } catch (Exception e) {
      logger.error("export error", e);
    }
  }
}
