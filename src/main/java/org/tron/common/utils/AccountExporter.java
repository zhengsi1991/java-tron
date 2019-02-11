package org.tron.common.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
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
import org.tron.protos.Protocol.AccountType;

@Slf4j
@Component
public class AccountExporter {
  public static final String FILE_NAME = "accounts.csv";
  public static final AtomicLong EXPORT_NUM = new AtomicLong(0);
  private static WalletOnSolidity walletOnSolidity;

  @Autowired
  public void setWalletOnSolidity(WalletOnSolidity walletOnSolidity) {
    AccountExporter.walletOnSolidity = walletOnSolidity;
  }

  public static void export(BlockStore blockStore, Iterable<Entry<byte[], AccountCapsule>> iterable) {
    walletOnSolidity.futureGetWithoutTimeout(() -> {
      List<BlockCapsule> blockList = blockStore.getBlockByLatestNum(1);
      if (CollectionUtils.isNotEmpty(blockList) && blockList.get(0).getNum() == EXPORT_NUM.get()) {
        exportAll(iterable);
        exportNormal(iterable);
        exportContract(iterable);
        exportAssetIssue(iterable);
        exportMissing(iterable);
      }
    });
  }

  private static void exportAll(Iterable<Entry<byte[], AccountCapsule>> iterable) {
    AtomicLong total = new AtomicLong(0);
    Map<String, Long> accounts = Streams.stream(iterable)
        .map(e -> Maps.immutableEntry(Wallet.encode58Check(e.getKey()), e.getValue().getBalance() + 
                                                                        e.getValue().getFrozenBalance() +
                                                                        e.getValue().getEnergyFrozenBalance() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy()))
        .peek(e -> {
          if (e.getValue() >= 0) total.getAndAdd(e.getValue());
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));

    try (CSVPrinter printer = new CSVPrinter(new FileWriter("block_" + EXPORT_NUM.get() + "_all_" + FILE_NAME),
        CSVFormat.EXCEL.withHeader("address", "balance"))) {
      printer.printRecord("total", total.get());
      accounts.forEach((k, v) -> {
            String address = k;
            long balance = v;
            try {
              printer.printRecord(address, balance);
            } catch (Exception e1) {
              logger.error("address {} write error.", address);
            }
          });
    } catch (Exception e) {
      logger.error("export error", e);
    }
  }

  private static void exportNormal(Iterable<Entry<byte[], AccountCapsule>> iterable) {
    AtomicLong total = new AtomicLong(0);
    Map<String, Long> accounts = Streams.stream(iterable)
        .filter(e -> e.getValue().getType() == AccountType.Normal)   // 0: Normal
        .map(e -> Maps.immutableEntry(Wallet.encode58Check(e.getKey()), e.getValue().getBalance() + 
                                                                        e.getValue().getFrozenBalance() +
                                                                        e.getValue().getEnergyFrozenBalance() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy()))
        .peek(e -> {
          if (e.getValue() >= 0) total.getAndAdd(e.getValue());
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));

    try (CSVPrinter printer = new CSVPrinter(new FileWriter("block_" + EXPORT_NUM.get() + "_normal_" + FILE_NAME),
        CSVFormat.EXCEL.withHeader("address", "balance"))) {
      printer.printRecord("total", total.get());
      accounts.forEach((k, v) -> {
            String address = k;
            long balance = v;
            try {
              printer.printRecord(address, balance);
            } catch (Exception e1) {
              logger.error("address {} write error.", address);
            }
          });
    } catch (Exception e) {
      logger.error("export error", e);
    }
  }

  private static void exportContract(Iterable<Entry<byte[], AccountCapsule>> iterable) {
    AtomicLong total = new AtomicLong(0);
    Map<String, Long> accounts = Streams.stream(iterable)
        .filter(e -> e.getValue().getType() == AccountType.Contract)   // 2: Contract
        .map(e -> Maps.immutableEntry(Wallet.encode58Check(e.getKey()), e.getValue().getBalance() + 
                                                                        e.getValue().getFrozenBalance() +
                                                                        e.getValue().getEnergyFrozenBalance() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy() +
                                                                        e.getValue().getDelegatedFrozenBalanceForBandwidth()))
        .peek(e -> {
          if (e.getValue() >= 0) total.getAndAdd(e.getValue());
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));

    try (CSVPrinter printer = new CSVPrinter(new FileWriter("block_" + EXPORT_NUM.get() + "_contract_" + FILE_NAME),
        CSVFormat.EXCEL.withHeader("address", "balance"))) {
      printer.printRecord("total", total.get());
      accounts.forEach((k, v) -> {
            String address = k;
            long balance = v;
            try {
              printer.printRecord(address, balance);
            } catch (Exception e1) {
              logger.error("address {} write error.", address);
            }
          });
    } catch (Exception e) {
      logger.error("export error", e);
    }
  }

  private static void exportAssetIssue(Iterable<Entry<byte[], AccountCapsule>> iterable) {
    AtomicLong total = new AtomicLong(0);
    Map<String, Long> accounts = Streams.stream(iterable)
        .filter(e -> e.getValue().getType() == AccountType.AssetIssue)   // 1: AssetIssue
        .map(e -> Maps.immutableEntry(Wallet.encode58Check(e.getKey()), e.getValue().getBalance() + 
                                                                        e.getValue().getFrozenBalance() +
                                                                        e.getValue().getEnergyFrozenBalance() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy()))
        .peek(e -> {
          if (e.getValue() >= 0) total.getAndAdd(e.getValue());
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));

    try (CSVPrinter printer = new CSVPrinter(new FileWriter("block_" + EXPORT_NUM.get() + "_assetissue_" + FILE_NAME),
        CSVFormat.EXCEL.withHeader("address", "balance"))) {
      printer.printRecord("total", total.get());
      accounts.forEach((k, v) -> {
            String address = k;
            long balance = v;
            try {
              printer.printRecord(address, balance);
            } catch (Exception e1) {
              logger.error("address {} write error.", address);
            }
          });
    } catch (Exception e) {
      logger.error("export error", e);
    }
  }

  private static void exportMissing(Iterable<Entry<byte[], AccountCapsule>> iterable) {
    AtomicLong total = new AtomicLong(0);
    Map<String, Long> accounts = Streams.stream(iterable)
        .filter(e -> (e.getValue().getType() != AccountType.AssetIssue) &&
                     (e.getValue().getType() != AccountType.Contract) && 
                     (e.getValue().getType() != AccountType.Normal))   // ?
        .map(e -> Maps.immutableEntry(Wallet.encode58Check(e.getKey()), e.getValue().getBalance() + 
                                                                        e.getValue().getFrozenBalance() +
                                                                        e.getValue().getEnergyFrozenBalance() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy() +
                                                                        e.getValue().getDelegatedFrozenBalanceForEnergy()))
        .peek(e -> {
          if (e.getValue() >= 0) total.getAndAdd(e.getValue());
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));

    try (CSVPrinter printer = new CSVPrinter(new FileWriter("block_" + EXPORT_NUM.get() + "_missing_" + FILE_NAME),
        CSVFormat.EXCEL.withHeader("address", "balance"))) {
      printer.printRecord("total", total.get());
      accounts.forEach((k, v) -> {
            String address = k;
            long balance = v;
            try {
              printer.printRecord(address, balance);
            } catch (Exception e1) {
              logger.error("address {} write error.", address);
            }
          });
    } catch (Exception e) {
      logger.error("export error", e);
    }
  }
}
