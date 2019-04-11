package org.tron.core.db;

import com.typesafe.config.ConfigObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.entity.WitnessVote;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Vote;

@Slf4j(topic = "DB")
@Component
public class AccountStore extends TronStoreWithRevoking<AccountCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

  @Autowired
  private AccountStore(@Value("account") String dbName) {
    super(dbName);
  }

  @Override
  public AccountCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
  }

  /**
   * Max TRX account.
   */
  public AccountCapsule getSun() {
    return getUnchecked(assertsAddress.get("Sun"));
  }

  /**
   * Min TRX account.
   */
  public AccountCapsule getBlackhole() {
    return getUnchecked(assertsAddress.get("Blackhole"));
  }

  /**
   * Get foundation account info.
   */
  public AccountCapsule getZion() {
    return getUnchecked(assertsAddress.get("Zion"));
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Wallet.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  public HashMap<String, WitnessVote> countWitnessCount(BlockCapsule block) {
    long round = block.getNum();
    HashMap<String, WitnessVote> roundWitness = new HashMap<>();

    Iterator<Map.Entry<byte[], AccountCapsule>> iterator = this.iterator();
    while (iterator.hasNext()) {
      Map.Entry<byte[], AccountCapsule> entry = iterator.next();
      String voterAddress = ByteArray.toHexString(entry.getValue().getAddress().toByteArray());

      List<Vote> voteList = entry.getValue().getVotesList();
      if (voteList.size() > 0) {
        for (Protocol.Vote vote : voteList) {
          String witnessAddress = ByteArray.toHexString(vote.getVoteAddress().toByteArray());
          String key = String.valueOf(round) + "_" + witnessAddress;

          if (roundWitness.containsKey(key)) {
            WitnessVote voterCount = roundWitness.get(key);
            voterCount.add(voterAddress, vote.getVoteCount());
          } else {
            WitnessVote voterCount = new WitnessVote();
            voterCount.add(voterAddress, vote.getVoteCount());
            roundWitness.put(key, voterCount);
          }
        }
      }
    }

    return roundWitness;
  }

}
