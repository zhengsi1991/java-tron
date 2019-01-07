package stest.tron.wallet.contract.trcToken.version322;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken074 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static final long TotalSupply = 1000L;
  private byte[] transferTokenContractAddress = null;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 11000_000_000L, fromAddress,
        testKey002, blockingStubFull));
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    Assert.assertTrue(PublicMethed.unFreezeBalance(fromAddress, testKey002, 1,
        dev001Address, blockingStubFull));
    Assert.assertTrue(PublicMethed.unFreezeBalance(fromAddress, testKey002, 0,
        dev001Address, blockingStubFull));
  }

  public static long getFreezeBalanceCount(byte[] accountAddress, String ecKey, Long targetEnergy,
      WalletGrpc.WalletBlockingStub blockingStubFull, String msg) {
    if(msg != null) {
      logger.info(msg);
    }
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(accountAddress,
        blockingStubFull);

    Account info = PublicMethed.queryAccount(accountAddress, blockingStubFull);

    Account getAccount = PublicMethed.queryAccount(ecKey, blockingStubFull);

    long balance = info.getBalance();
    long frozenBalance = info.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    long totalEnergyLimit = resourceInfo.getTotalEnergyLimit();
    long totalEnergyWeight = resourceInfo.getTotalEnergyWeight();
    long energyUsed = resourceInfo.getEnergyUsed();
    long energyLimit = resourceInfo.getEnergyLimit();

    logger.info("Balance:" + balance);
    logger.info("frozenBalance: " + frozenBalance);
    logger.info("totalEnergyLimit: " + totalEnergyLimit);
    logger.info("totalEnergyWeight: " + totalEnergyWeight);
    logger.info("energyUsed: " + energyUsed);
    logger.info("energyLimit: " + energyLimit);

    if (energyUsed > energyLimit) {
      targetEnergy = energyUsed - energyLimit + targetEnergy;
    }

    logger.info("targetEnergy: " + targetEnergy);
    if (totalEnergyWeight == 0) {
      return 1000_000L;
    }

    // totalEnergyLimit / (totalEnergyWeight + needBalance) = needEnergy / needBalance
    BigInteger totalEnergyWeightBI = BigInteger.valueOf(totalEnergyWeight);
    long needBalance = totalEnergyWeightBI.multiply(BigInteger.valueOf(1_000_000))
        .multiply(BigInteger.valueOf(targetEnergy))
        .divide(BigInteger.valueOf(totalEnergyLimit - targetEnergy)).longValue();

    logger.info("[Debug]getFreezeBalanceCount, needBalance: " + needBalance);

    if (needBalance < 1000000L) {
      needBalance = 1000000L;
      logger.info("[Debug]getFreezeBalanceCount, needBalance less than 1 TRX, modify to: " + needBalance);
    }
    return needBalance;
  }

  public static Long getAssetIssueValue(byte[] dev001Address, ByteString assetIssueId,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Long assetIssueCount = 0L;
    Account contractAccount = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Map<String, Long> createAssetIssueMap = contractAccount.getAssetV2Map();
    for (Map.Entry<String, Long> entry : createAssetIssueMap.entrySet()) {
      if (assetIssueId.toStringUtf8().equals(entry.getKey())) {
        assetIssueCount = entry.getValue();
      }
    }
    return assetIssueCount;
  }

  private ByteString testCreateAssetIssue(byte[] accountAddress, String priKey) {
    ByteString assetAccountId = null;
    ByteString addressBS1 = ByteString.copyFrom(accountAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {

      long start = System.currentTimeMillis() + 2000;
      long end = System.currentTimeMillis() + 1000000000;

      //Create a new AssetIssue success.
      Assert.assertTrue(PublicMethed.createAssetIssue(accountAddress, tokenName, TotalSupply, 1,
          10000, start, end, 1, description, url, 100000L,100000L,
          1L,1L, priKey, blockingStubFull));

      Account getAssetIdFromThisAccount = PublicMethed.queryAccount(accountAddress,blockingStubFull);
      assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

      logger.info("The token name: " + tokenName);
      logger.info("The token ID: " + assetAccountId.toStringUtf8());

    } else {
      logger.info("This account already create an assetisue");
      Optional<AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      tokenName = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());
    }
    return assetAccountId;
  }


  private List<String> getStrings(byte[] data){
    int index = 0;
    List<String> ret = new ArrayList<>();
    while(index < data.length){
      ret.add(byte2HexStr(data, index, 32));
      index += 32;
    }
    return ret;
  }

  public static String byte2HexStr(byte[] b, int offset, int length) {
    String stmp="";
    StringBuilder sb = new StringBuilder("");
    for (int n= offset; n<offset + length && n < b.length; n++) {
      stmp = Integer.toHexString(b[n] & 0xFF);
      sb.append((stmp.length()==1)? "0"+stmp : stmp);
    }
    return sb.toString().toUpperCase().trim();
  }

  @Test
  public void deployTransferTokenContract() {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(dev001Address, dev001Key, 130000L,
            blockingStubFull, null), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    assetAccountId = testCreateAssetIssue(dev001Address, dev001Key);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() +
        ", devAssetCountBefore: " + devAssetCountBefore);

    String contractName = "transferTokenContract";
    String code = "608060405260e5806100126000396000f300608060405260043610603e5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663dfc0db988114606f575b34d2d37f70536dc19abf7e0862a7afc0755f548bfd5f0b40cbd9fc87fa19b2e50b454a5660405160405180910390a4005b6078600435607a565b005b604080513481529051d291d3913385d1917fe8e04df6a82498bd5fb1fe7f2168f525c54ee1003b3a56638c858e94fd9e4854919081900360200190a4505600a165627a7a72305820dca795a21bfc9451314c14306b0220175611298bdb59c8b2cfdb1120cb3fda390029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"tokenId\",\"type\":\"trcToken\"}],\"name\":\"getToken\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"logFallback\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"name\":\"\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"\",\"type\":\"uint256\"},{\"indexed\":true,\"name\":\"\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"logGetToken\",\"type\":\"event\"}]";

    String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 100;
    long callValue = 5;

    String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callValue, 0, 10000, tokenId, tokenValue,
            null, dev001Key, dev001Address, blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() +
        ", devAssetCountAfter: " + devAssetCountAfter);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Assert.assertTrue(PublicMethed.transferAsset(transferTokenContractAddress,
        assetAccountId.toByteArray(), 100L, dev001Address, dev001Key, blockingStubFull));

    Long contractAssetCount = getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: " + contractAssetCount);

//    Assert.assertTrue(energyLimit > 0);
//    Assert.assertTrue(energyUsage > 0);
//    Assert.assertEquals(callValue, balanceBefore - balanceAfter);
    Assert.assertEquals(Long.valueOf(tokenValue), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100L + tokenValue), contractAssetCount);

    // get and verify the msg.value and msg.id

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devEnergyLimitBefore is " + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, devEnergyUsageBefore is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    Long transferAssetBefore = getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);

    Long devAssetBefore = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("before trigger, dev001Address has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + devAssetBefore);

    tokenId = Long.toString(Long.MAX_VALUE);

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "getToken(trcToken)", tokenId, false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

//    logger.info("The msg value: " +  getStrings(infoById.get().getContractResult(0).toByteArray()));
    logger.info("The msg value: " + infoById.get().getLogList().get(0).getTopicsList());

    Long msgTokenBalance = ByteArray
        .toLong(infoById.get().getLogList().get(0).getTopicsList().get(1).toByteArray());
    Long msgId = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(2).toByteArray());
    Long msgTokenValue = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(3).toByteArray());

    logger.info("msgTokenBalance: " + msgTokenBalance);
    logger.info("msgId: " + msgId );
    logger.info("msgTokenValue: " + msgTokenValue );

    Assert.assertEquals(Long.valueOf(0), msgTokenBalance);


    tokenId = Long.toString(0);

    triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "getToken(trcToken)", tokenId, false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

//    logger.info("The msg value: " +  getStrings(infoById.get().getContractResult(0).toByteArray()));
    logger.info("The msg value: " + infoById.get().getLogList().get(0).getTopicsList());
    msgTokenBalance = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(1).toByteArray());
    msgId = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(2).toByteArray());
    msgTokenValue = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(3).toByteArray());

    logger.info("msgTokenBalance: " + msgTokenBalance);
    logger.info("msgId: " + msgId );
    logger.info("msgTokenValue: " + msgTokenValue );

    Assert.assertEquals(Long.valueOf(0), msgTokenBalance);


    tokenId = Long.toString(Long.MIN_VALUE);

    triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "getToken(trcToken)", tokenId, false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

//    logger.info("The msg value: " +  getStrings(infoById.get().getContractResult(0).toByteArray()));
    logger.info("The msg value: " + infoById.get().getLogList().get(0).getTopicsList());
     msgTokenBalance = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(1).toByteArray());
     msgId = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(2).toByteArray());
     msgTokenValue = ByteArray.toLong(infoById.get().getLogList().get(0).getTopicsList().get(3).toByteArray());

    logger.info("msgTokenBalance: " + msgTokenBalance);
    logger.info("msgId: " + msgId );
    logger.info("msgTokenValue: " + msgTokenValue );

    Assert.assertEquals(Long.valueOf(0), msgTokenBalance);

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


