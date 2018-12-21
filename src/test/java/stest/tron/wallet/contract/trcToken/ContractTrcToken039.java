package stest.tron.wallet.contract.trcToken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken039 {

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
        .assertTrue(PublicMethed.sendcoin(dev001Address, 4048000000L, fromAddress,
            testKey002, blockingStubFull));
    logger.info(
        "dev001Address:" + Base58.encode58Check(dev001Address));
    Assert
        .assertTrue(PublicMethed.sendcoin(user001Address, 4048000000L, fromAddress,
            testKey002, blockingStubFull));
    logger.info(
        "user001Address:" + Base58.encode58Check(user001Address));

    // freeze balance
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(dev001Address, 204800000,
        3, 1, dev001Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(user001Address, 2048000000,
        3, 1, user001Key, blockingStubFull));

    String tokenName = "testAI_" + randomInt(10000, 90000);
    ByteString tokenId = createAssetissue(dev001Address, dev001Key, tokenName);
    int i = randomInt(6666666, 9999999);
    ByteString tokenId1 = ByteString.copyFromUtf8(String.valueOf(i));

    // deploy transferTokenContract
    int originEnergyLimit = 50000;

    String contractName = "ProxyTest";
    String code = "60806040526101bc806100136000396000f30060806040526004361061004b5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633659cfe681146100965780635c60da1b146100e0575b60005473ffffffffffffffffffffffffffffffffffffffff1680151561007057600080fd5b6040513660008237600081368185600019f43d6000833e808015610092573d83f35b3d83fd5b3480156100a257600080fd5b50d380156100af57600080fd5b50d280156100bc57600080fd5b506100de73ffffffffffffffffffffffffffffffffffffffff60043516610138565b005b3480156100ec57600080fd5b50d380156100f957600080fd5b50d2801561010657600080fd5b5061010f610174565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b6000805473ffffffffffffffffffffffffffffffffffffffff191673ffffffffffffffffffffffffffffffffffffffff92909216919091179055565b60005473ffffffffffffffffffffffffffffffffffffffff16815600a165627a7a7230582047b47a8f57880f0c2c028e44406b1868bbee2151938629caad7b65668d6a8a140029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"_address\",\"type\":\"address\"}],\"name\":\"upgradeTo\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"implementation\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    //"[{\"constant\":false,\"inputs\":[],\"name\":\"AssertError\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] proxyTestAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            1000L, 0, originEnergyLimit, tokenId.toStringUtf8(),
            1000, null, dev001Key, dev001Address,
            blockingStubFull);
    String contractName1 = "ATest";
    String code1 = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5060de806100396000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416630455012181146043575b600080fd5b606860043573ffffffffffffffffffffffffffffffffffffffff60243516604435606a565b005b60405173ffffffffffffffffffffffffffffffffffffffff83169084156108fc029085906000818181858888f1935050505015801560ac573d6000803e3d6000fd5b505050505600a165627a7a72305820542cde58d75afb60ec2226b95c592a973666b56931d8ea8d7ed0d0a6bd16e7ea0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"trans\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
    byte[] ATestAddress = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);

    String contractName2 = "BTest";
    String code2 = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5060e2806100396000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416630455012181146043575b600080fd5b606860043573ffffffffffffffffffffffffffffffffffffffff60243516604435606a565b005b60405173ffffffffffffffffffffffffffffffffffffffff83169084156108fc029085908490600081818185878a8ad094505050505015801560b0573d6000803e3d6000fd5b505050505600a165627a7a72305820cccc1489247eb5366214a034107ebfbea955aac5229e03907a82321a1a4484240029";
    String abi2 = "[{\"constant\":false,\"inputs\":[{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"trans\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
    byte[] BTestAddress = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // devAddress transfer token to userAddress

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long beforeAssetIssueDevAddress = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long beforeAssetIssueUserAddress = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);

    Long beforeAssetIssueContractAddress = getAssetIssueValue(proxyTestAddress, tokenId,
        blockingStubFull);
    Long beforeAssetIssueBAddress = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long beforeAssetIssueAAddress = getAssetIssueValue(ATestAddress, tokenId,
        blockingStubFull);
    Long beforeBalanceContractAddress = PublicMethed.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
    Long beforeUserBalance = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueBAddress:" + beforeAssetIssueBAddress);

    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
    logger.info("beforeBalanceContractAddress:" + beforeBalanceContractAddress);
    logger.info("beforeUserBalance:" + beforeUserBalance);
    String param =
        "\"" + Base58.encode58Check(ATestAddress) + "\"";
    String param1 =
        "\"" + "1" + "\",\"" + Base58.encode58Check(user001Address) + "\",\"" + tokenId
            .toStringUtf8()
            + "\"";

    String triggerTxid = PublicMethed.triggerContract(proxyTestAddress,
        "upgradeTo(address)",
        param, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);
    String triggerTxid1 = PublicMethed.triggerContract(proxyTestAddress,
        "trans(uint256,address,trcToken)",
        param1, false, 0, 1000000000L, tokenId
            .toStringUtf8(),
        1, dev001Address, dev001Key,
        blockingStubFull);
    Account infoafter = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterAssetIssueDevAddress = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long afterAssetIssueContractAddress = getAssetIssueValue(proxyTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueAAddress = getAssetIssueValue(ATestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress = getAssetIssueValue(user001Address, tokenId, blockingStubFull);
    Long afterBalanceContractAddress = PublicMethed.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
    Long afterUserBalance = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueBAddress:" + afterAssetIssueBAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);
    logger.info("afterUserBalance:" + afterUserBalance);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress == beforeAssetIssueUserAddress);
    Assert.assertTrue(afterBalanceContractAddress == beforeBalanceContractAddress - 1);
    Assert.assertTrue(afterAssetIssueContractAddress == beforeAssetIssueContractAddress + 1);
    Assert.assertTrue(afterAssetIssueDevAddress == beforeAssetIssueDevAddress - 1);
    Assert.assertTrue(afterUserBalance == beforeUserBalance + 1);
    Assert.assertTrue(afterAssetIssueUserAddress == afterAssetIssueUserAddress);
    Assert.assertTrue(afterAssetIssueBAddress == beforeAssetIssueBAddress);

    Account info1;
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    info1 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance1 = info1.getBalance();
    Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
    Long beforeNetUsed1 = resourceInfo1.getNetUsed();
    Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    Long beforeAssetIssueDevAddress1 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long beforeAssetIssueUserAddress1 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);

    Long beforeAssetIssueContractAddress1 = getAssetIssueValue(proxyTestAddress, tokenId,
        blockingStubFull);
    Long beforeAssetIssueBAddress1 = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);

    Long beforeBalanceContractAddress1 = PublicMethed.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
    Long beforeUserBalance1 = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();
    logger.info("beforeBalance1:" + beforeBalance1);
    logger.info("beforeEnergyUsed1:" + beforeEnergyUsed1);
    logger.info("beforeNetUsed1:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed1:" + beforeFreeNetUsed1);
    logger.info("beforeAssetIssueContractAddress1:" + beforeAssetIssueContractAddress1);
    logger.info("beforeAssetIssueBAddress1:" + beforeAssetIssueBAddress1);

    logger.info("beforeAssetIssueDevAddress1:" + beforeAssetIssueDevAddress1);
    logger.info("beforeAssetIssueUserAddress1:" + beforeAssetIssueUserAddress1);
    logger.info("beforeBalanceContractAddress1:" + beforeBalanceContractAddress1);
    logger.info("beforeUserBalance1:" + beforeUserBalance1);
    String param3 =
        "\"" + Base58.encode58Check(BTestAddress) + "\"";
    String param2 =
        "\"" + "1" + "\",\"" + Base58.encode58Check(user001Address) + "\",\"" + tokenId
            .toStringUtf8()
            + "\"";

    String triggerTxid2 = PublicMethed.triggerContract(proxyTestAddress,
        "upgradeTo(address)",
        param3, false, 0, 1000000000L, tokenId
            .toStringUtf8(),
        1, dev001Address, dev001Key,
        blockingStubFull);
    String triggerTxid3 = PublicMethed.triggerContract(proxyTestAddress,
        "trans(uint256,address,trcToken)",
        param2, false, 0, 1000000000L, tokenId
            .toStringUtf8(),
        1, dev001Address, dev001Key,
        blockingStubFull);
    Account infoafter1 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
    Long afterAssetIssueDevAddress1 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    Long afterAssetIssueContractAddress1 = getAssetIssueValue(proxyTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress1 = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);

    Long afterAssetIssueUserAddress1 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afterBalanceContractAddress1 = PublicMethed.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
    Long afterUserBalance1 = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterEnergyUsed1:" + afterEnergyUsed1);
    logger.info("afterNetUsed1:" + afterNetUsed1);
    logger.info("afterFreeNetUsed1:" + afterFreeNetUsed1);
    logger.info("afterAssetIssueCount1:" + afterAssetIssueDevAddress1);
    logger.info("afterAssetIssueDevAddress1:" + afterAssetIssueContractAddress1);
    logger.info("afterAssetIssueBAddress1:" + afterAssetIssueBAddress1);
    logger.info("afterAssetIssueUserAddress1:" + afterAssetIssueUserAddress1);
    logger.info("afterBalanceContractAddress1:" + afterBalanceContractAddress1);
    logger.info("afterUserBalance1:" + afterUserBalance);

    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress1 == beforeAssetIssueUserAddress1);
    Assert.assertTrue(afterBalanceContractAddress1 == beforeBalanceContractAddress1 - 1);
    Assert.assertTrue(afterAssetIssueContractAddress1 == beforeAssetIssueContractAddress1 + 1);
    Assert.assertTrue(afterAssetIssueDevAddress1 == beforeAssetIssueDevAddress1 - 1);
    Assert.assertTrue(afterUserBalance1 == beforeUserBalance1 + 1);
    Assert.assertTrue(afterAssetIssueUserAddress1 == afterAssetIssueUserAddress1);
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


