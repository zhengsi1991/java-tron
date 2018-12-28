package org.tron.core.db;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionInfoV2Capsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;

@Component
public class TransactionHistoryV2Store extends TronStoreWithRevoking<TransactionInfoV2Capsule> {

  @Autowired
  public TransactionHistoryV2Store(@Value("transactionHistoryV2Store") String dbName) {
    super(dbName);
  }

  @Override
  public TransactionInfoV2Capsule get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionInfoV2Capsule(value);
  }

  @Override
  public void put(byte[] key, TransactionInfoV2Capsule item) {
    if (BooleanUtils.toBoolean(Args.getInstance().getStorage().getTransactionHistoreSwitch())) {
      super.put(key, item);
    }
  }
  
}