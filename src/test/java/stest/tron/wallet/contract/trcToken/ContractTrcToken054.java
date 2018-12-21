package stest.tron.wallet.contract.trcToken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken054 {

  private AtomicLong count = new AtomicLong();
  private AtomicLong errorCount = new AtomicLong();
  private long startTime = System.currentTimeMillis();

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long TotalSupply = 10000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

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

    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }

  public ByteString createAssetissue(byte[] devAddress, String devKey, String tokenName) {

    ByteString assetAccountId = null;
    ByteString addressBS1 = ByteString.copyFrom(devAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      Long start = System.currentTimeMillis() + 2000;
      Long end = System.currentTimeMillis() + 1000000000;

      logger.info("The token name: " + tokenName);

      //Create a new AssetIssue success.
      Assert.assertTrue(PublicMethed.createAssetIssue(devAddress, tokenName, TotalSupply, 1,
          100, start, end, 1, description, url, 10000L, 10000L,
          1L, 1L, devKey, blockingStubFull));

      Account getAssetIdFromThisAccount = PublicMethed.queryAccount(devAddress, blockingStubFull);
      assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    } else {
      logger.info("This account already create an assetisue");
      Optional<AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      tokenName = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0)
          .getName().toByteArray());
    }
    return assetAccountId;
  }

  @Test(enabled = true)
  public void continueRun() {

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] dev001Address = ecKey1.getAddress();
    String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] user001Address = ecKey2.getAddress();
    String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert
        .assertTrue(PublicMethed.sendcoin(dev001Address, 2048000000, fromAddress,
            testKey002, blockingStubFull));
    Assert
        .assertTrue(PublicMethed.sendcoin(user001Address, 4048000000L, fromAddress,
            testKey002, blockingStubFull));

    // freeze balance
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(dev001Address, 204800000,
        3, 1, dev001Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(user001Address, 2048000000,
        3, 1, user001Key, blockingStubFull));

    String tokenName = "testAI_" + randomInt(10000, 90000);
    ByteString tokenId = createAssetissue(user001Address, user001Key, tokenName);
    int i = randomInt(6666666, 9999999);
    ByteString tokenId1 = ByteString.copyFromUtf8(String.valueOf(i));
    Long existTokenIDLong = Long.valueOf(tokenId.toStringUtf8()) - 1;
    ByteString existTokenID = ByteString.copyFromUtf8(String.valueOf(existTokenIDLong));
    // devAddress transfer token to A
    PublicMethed.transferAsset(dev001Address, tokenId.toByteArray(), 101, user001Address,
        user001Key, blockingStubFull);

    // deploy transferTokenContract
    String contractName = "transferTokenContract";
    String code = "6080604052610118806100136000396000f30060806040526004361060485763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633be9ece78114604d57806371dc08ce146074575b600080fd5b607273ffffffffffffffffffffffffffffffffffffffff600435166024356044356098565b005b607a60e4565b60408051938452602084019290925282820152519081900360600190f35b60405173ffffffffffffffffffffffffffffffffffffffff84169082156108fc029083908590600081818185878a8ad094505050505015801560de573d6000803e3d6000fd5b50505050565bd3d2349091925600a165627a7a72305820b8d4f8ea5443a03d615ea5dfe7a7435498522f9c7abeb25583d953ee2d20be4a0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"id\",\"type\":\"trcToken\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"TransferTokenTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"msgTokenValueAndTokenIdTest\",\"outputs\":[{\"name\":\"\",\"type\":\"trcToken\"},{\"name\":\"\",\"type\":\"uint256\"},{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]\n";
    byte[] transferTokenContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 10000, tokenId.toStringUtf8(),
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // devAddress transfer token to userAddress
    PublicMethed
        .transferAsset(transferTokenContractAddress, tokenId.toByteArray(), 100, user001Address,
            user001Key,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(user001Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(user001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long beforeAssetIssueCount = getAssetIssueValue(user001Address, tokenId, blockingStubFull);
    Long beforeAssetIssueContractAddress = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long beforeAssetIssueDev = getAssetIssueValue(dev001Address, tokenId,
        blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueCount);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
    Long callvalueF = 0L;
    Long tokenvalueF = 0L;
    String fakeTokenId = Long.toString(1000000);
    String fakeTokenId2 = Long.toString(1000001);

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "msgTokenValueAndTokenIdTest()", "#", false, callvalueF,
        1000000000L, tokenId.toStringUtf8(), tokenvalueF, user001Address, user001Key,
        blockingStubFull);
    String triggerTxid1 = PublicMethed.triggerContract(transferTokenContractAddress,
        "msgTokenValueAndTokenIdTest()", "#", false, callvalueF,
        1000000000L, fakeTokenId, tokenvalueF, user001Address, user001Key,
        blockingStubFull);

    String triggerTxid2 = PublicMethed.triggerContract(transferTokenContractAddress,
        "msgTokenValueAndTokenIdTest()", "#", false, callvalueF,
        1000000000L, existTokenID.toStringUtf8(), tokenvalueF, user001Address, user001Key,
        blockingStubFull);

    String triggerTxid3 = PublicMethed.triggerContract(transferTokenContractAddress,
        "msgTokenValueAndTokenIdTest()", "#", false, callvalueF,
        1000000000L, fakeTokenId2, tokenvalueF, user001Address, user001Key,
        blockingStubFull);
    Account infoafter = PublicMethed.queryAccount(user001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(user001Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterAssetIssueCount = getAssetIssueValue(user001Address, tokenId, blockingStubFull);
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long afterAssetIssueContractAddress = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueDev = getAssetIssueValue(dev001Address, tokenId,
        blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueCount);
    logger.info("afterAssetIssueContractAddress:" + afterAssetIssueContractAddress);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    List<String> retList = getStrings(infoById.get().getContractResult(0).toByteArray());
    Long id = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));
    Long tokenValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));
    Long callValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(2)));
    logger.info("id:" + id);
    logger.info("tokenValue:" + tokenValue);
    logger.info("callValue:" + callValue);

    Assert.assertEquals((tokenId.toStringUtf8()), String.valueOf(id));
    Assert.assertTrue(callvalueF == callValue);
    Assert.assertTrue(tokenvalueF == tokenValue);

    List<String> retList1 = getStrings(infoById1.get().getContractResult(0).toByteArray());
    Long id1 = ByteArray.toLong(ByteArray.fromHexString(retList1.get(0)));
    Long tokenValue1 = ByteArray.toLong(ByteArray.fromHexString(retList1.get(1)));
    Long callValue1 = ByteArray.toLong(ByteArray.fromHexString(retList1.get(2)));
    logger.info("id1:" + id1);
    logger.info("tokenValue1:" + tokenValue1);
    logger.info("callValue1:" + callValue1);
    Assert.assertEquals(fakeTokenId, String.valueOf(id1));
    Assert.assertTrue(callvalueF == callValue1);
    Assert.assertTrue(tokenvalueF == tokenValue1);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(triggerTxid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    List<String> retList2 = getStrings(infoById2.get().getContractResult(0).toByteArray());
    Long id2 = ByteArray.toLong(ByteArray.fromHexString(retList2.get(0)));
    Long tokenValue2 = ByteArray.toLong(ByteArray.fromHexString(retList2.get(1)));
    Long callValue2 = ByteArray.toLong(ByteArray.fromHexString(retList2.get(2)));
    logger.info("id2:" + id2);
    logger.info("tokenValue:" + tokenValue2);
    logger.info("callValue:" + callValue2);
    Assert.assertEquals(existTokenID.toStringUtf8(), String.valueOf(id2));
    Assert.assertTrue(callvalueF == callValue2);
    Assert.assertTrue(tokenvalueF == tokenValue2);

    Optional<TransactionInfo> infoById3 = PublicMethed
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    List<String> retList3 = getStrings(infoById3.get().getContractResult(0).toByteArray());
    Long id3 = ByteArray.toLong(ByteArray.fromHexString(retList3.get(0)));
    Long tokenValue3 = ByteArray.toLong(ByteArray.fromHexString(retList3.get(1)));
    Long callValue3 = ByteArray.toLong(ByteArray.fromHexString(retList3.get(2)));
    logger.info("id1:" + id3);
    logger.info("tokenValue1:" + tokenValue3);
    logger.info("callValue1:" + callValue3);
    Assert.assertEquals(fakeTokenId2, String.valueOf(id3));
    Assert.assertTrue(callvalueF == callValue3);
    Assert.assertTrue(tokenvalueF == tokenValue3);
    Assert.assertEquals(beforeAssetIssueCount, afterAssetIssueCount);
    Assert.assertTrue(beforeAssetIssueContractAddress == afterAssetIssueContractAddress);
    Assert.assertTrue(beforeAssetIssueDev == afterAssetIssueDev);

  }

  private List<String> getStrings(byte[] data) {
    int index = 0;
    List<String> ret = new ArrayList<>();
    while (index < data.length) {
      ret.add(byte2HexStr(data, index, 32));
      index += 32;
    }
    return ret;
  }

  public static String byte2HexStr(byte[] b, int offset, int length) {
    String stmp = "";
    StringBuilder sb = new StringBuilder("");
    for (int n = offset; n < offset + length && n < b.length; n++) {
      stmp = Integer.toHexString(b[n] & 0xFF);
      sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
    }
    return sb.toString().toUpperCase().trim();
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
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
}


