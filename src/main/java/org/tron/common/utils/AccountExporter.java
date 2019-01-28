package org.tron.common.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
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
  @Autowired
  private static WalletOnSolidity walletOnSolidity;

  public static void export(BlockStore blockStore, Iterable<Entry<byte[], AccountCapsule>> iterable) {
    walletOnSolidity.futureGet(() -> {
      List<BlockCapsule> blockList = blockStore.getBlockByLatestNum(1);
      if (CollectionUtils.isNotEmpty(blockList)
          && blockList.get(0).getNum() == EXPORT_NUM.get()) {
        export(iterable);
      }
    });
  }

  private static void export(Iterable<Entry<byte[], AccountCapsule>> iterable) {
    try {
      BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_NAME),
          StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
      CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("address", "balance"));

      iterable.forEach(e -> {
        try {
          csvPrinter.printRecord(Wallet.encode58Check(e.getKey()), e.getValue().getBalance());
        } catch (IOException e1) {
          logger.error("address {} write error.", Wallet.encode58Check(e.getKey()));
        }
      });
      csvPrinter.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
