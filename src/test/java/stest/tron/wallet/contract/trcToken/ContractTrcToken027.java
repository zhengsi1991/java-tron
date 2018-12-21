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
public class ContractTrcToken027 {

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
    String contractName = "BTest";
    String code = "6080604052610145806100136000396000f3006080604052600436106100275763ffffffff60e060020a600035041663cb35b7e28114610029575b005b61002773ffffffffffffffffffffffffffffffffffffffff600435811690602435166044356064358373ffffffffffffffffffffffffffffffffffffffff1660405180807f7472616e7328616464726573732c75696e743235362c747263546f6b656e2900815250601f019050604051809103902060e060020a90048484846040518463ffffffff1660e060020a028152600401808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200183815260200182815260200193505050506000604051808303816000875af150505050505050505600a165627a7a72305820f80979e10a32bf6d6995716d7c5b159944473c2db9ac4ca5e780cfcd620261260029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"callCAddress\",\"type\":\"address\"},{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"transC\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] BTestAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);

    String contractName1 = "CTest";
    String code1 = "608060405260d9806100126000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416634f53d8a881146040575b005b603e73ffffffffffffffffffffffffffffffffffffffff6004351660243560443560405173ffffffffffffffffffffffffffffffffffffffff84169083156108fc029084908490600081818185878a8ad094505050505015801560a7573d6000803e3d6000fd5b505050505600a165627a7a723058203db4692a33e354b8bb6b6274f3c25ca36facafc80e996629c2b7c8e79ef40f2e0029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"trans\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] CTestAddress = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    String contractName2 = "tokenTest";
    String code2 = "6080604052610257806100136000396000f3006080604052600436106100325763ffffffff60e060020a6000350416636409a1c0811461003457806383efe58114610067575b005b61003273ffffffffffffffffffffffffffffffffffffffff6004358116906024358116906044351660643560843561009a565b61003273ffffffffffffffffffffffffffffffffffffffff60043581169060243581169060443516606435608435610163565b604080517f7472616e734328616464726573732c616464726573732c75696e743235362c7481527f7263546f6b656e290000000000000000000000000000000000000000000000006020820152815190819003602801812063ffffffff60e060020a91829004908116909102825273ffffffffffffffffffffffffffffffffffffffff8781166004840152868116602484015260448301869052606483018590529251928816929091608480820192600092909190829003018183875af1505050505050505050565b604080517f7472616e734328616464726573732c616464726573732c75696e743235362c7481527f7263546f6b656e290000000000000000000000000000000000000000000000006020820152815190819003602801812063ffffffff60e060020a91829004908116909102825273ffffffffffffffffffffffffffffffffffffffff87811660048401528681166024840152604483018690526064830185905292519288169290916084808201926000929091908290030181865af45050505050505050505600a165627a7a72305820554cb78d3d6082a8b93092e5edd2b66b83a47707eb8cbbfdbb06a0806fce72840029";
    String abi2 = "[{\"constant\":false,\"inputs\":[{\"name\":\"callBAddress\",\"type\":\"address\"},{\"name\":\"callCAddress\",\"type\":\"address\"},{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"testIndelegateCall\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"callBddress\",\"type\":\"address\"},{\"name\":\"callAddressC\",\"type\":\"address\"},{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"testIndelegateCall\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] transferTokenContractAddress = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert
        .assertTrue(PublicMethed.sendcoin(transferTokenContractAddress, 1000000000L, fromAddress,
            testKey002, blockingStubFull));

    // devAddress transfer token to userAddress
    PublicMethed
        .transferAsset(transferTokenContractAddress, tokenId.toByteArray(), 100, dev001Address,
            dev001Key,
            blockingStubFull);
    PublicMethed
        .transferAsset(BTestAddress, tokenId.toByteArray(), 100, dev001Address,
            dev001Key,
            blockingStubFull);
    PublicMethed
        .transferAsset(CTestAddress, tokenId.toByteArray(), 100, dev001Address,
            dev001Key,
            blockingStubFull);

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

    Long beforeAssetIssueContractAddress = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long beforeAssetIssueBAddress = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long beforeAssetIssueCAddress = getAssetIssueValue(CTestAddress, tokenId,
        blockingStubFull);
    Long beforeBalanceContractAddress = PublicMethed.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();
    Long beforeUserBalance = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueBAddress:" + beforeAssetIssueBAddress);
    logger.info("beforeAssetIssueCAddress:" + beforeAssetIssueCAddress);

    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
    logger.info("beforeBalanceContractAddress:" + beforeBalanceContractAddress);
    logger.info("beforeUserBalance:" + beforeUserBalance);

    // 1.user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(BTestAddress) + "\",\"" + Base58.encode58Check(CTestAddress) +
            "\",\"" + Base58.encode58Check(transferTokenContractAddress) +
            "\",1,\"" + tokenId
            .toStringUtf8()
            + "\"";

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "testIndelegateCall(address,address,address,uint256,trcToken)",
        param, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterAssetIssueDevAddress = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long afterAssetIssueContractAddress = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueCAddress = getAssetIssueValue(CTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress = getAssetIssueValue(user001Address, tokenId, blockingStubFull);
    Long afterBalanceContractAddress = PublicMethed.queryAccount(transferTokenContractAddress,
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
    logger.info("afterAssetIssueCAddress:" + afterAssetIssueCAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);
    logger.info("afterUserBalance:" + afterUserBalance);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress == beforeAssetIssueUserAddress);
    Assert.assertEquals(afterBalanceContractAddress, beforeBalanceContractAddress);
    Assert.assertTrue(afterAssetIssueContractAddress == beforeAssetIssueContractAddress + 1);
    Assert.assertTrue(afterAssetIssueBAddress == beforeAssetIssueBAddress);
    Assert.assertTrue(afterAssetIssueCAddress == beforeAssetIssueCAddress - 1);

    ByteString tokenIdNotHave = ByteString.copyFromUtf8("haha");

    //3. user trigger A to transfer token to B
    String param1 =
        "\"" + Base58.encode58Check(BTestAddress) + "\",\"" + Base58.encode58Check(CTestAddress) +
            "\",\"" + Base58.encode58Check(transferTokenContractAddress) +
            "\",1,\"" + tokenId1
            .toStringUtf8()
            + "\"";

    String triggerTxid1 = PublicMethed.triggerContract(transferTokenContractAddress,
        "testIndelegateCall(address,address,address,uint256,trcToken)",
        param1, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter1 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
    Long afterAssetIssueDevAddress1 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    Long afterAssetIssueContractAddress1 = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress1 = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueCAddress1 = getAssetIssueValue(CTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress1 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afterBalanceContractAddress1 = PublicMethed.queryAccount(transferTokenContractAddress,
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
    logger.info("afterAssetIssueCAddress1:" + afterAssetIssueCAddress1);
    logger.info("afterAssetIssueUserAddress1:" + afterAssetIssueUserAddress1);
    logger.info("afterBalanceContractAddress1:" + afterBalanceContractAddress1);
    logger.info("afterUserBalance1:" + afterUserBalance1);

    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress == afterAssetIssueUserAddress1);
    Assert.assertEquals(afterBalanceContractAddress, afterBalanceContractAddress1);
    Assert.assertTrue(afterAssetIssueContractAddress == afterAssetIssueContractAddress1);
    Assert.assertTrue(afterAssetIssueBAddress == afterAssetIssueBAddress1);
    Assert.assertTrue(afterAssetIssueCAddress == afterAssetIssueCAddress1);

    //4. user trigger A to transfer token to B
    String param2 =
        "\"" + Base58.encode58Check(BTestAddress) + "\",\"" + Base58.encode58Check(CTestAddress) +
            "\",\"" + Base58.encode58Check(transferTokenContractAddress) +
            "\",10000000,\"" + tokenId
            .toStringUtf8()
            + "\"";

    String triggerTxid2 = PublicMethed.triggerContract(transferTokenContractAddress,
        "testIndelegateCall(address,address,address,uint256,trcToken)",
        param2, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter2 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
    Long afterAssetIssueDevAddress2 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    Long afterAssetIssueContractAddress2 = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress2 = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueCAddress2 = getAssetIssueValue(CTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress2 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afterBalanceContractAddress2 = PublicMethed.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();
    Long afterUserBalance2 = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("afterBalance2:" + afterBalance2);
    logger.info("afterEnergyUsed2:" + afterEnergyUsed2);
    logger.info("afterNetUsed2:" + afterNetUsed2);
    logger.info("afterFreeNetUsed2:" + afterFreeNetUsed2);
    logger.info("afterAssetIssueCount2:" + afterAssetIssueDevAddress2);
    logger.info("afterAssetIssueDevAddress2:" + afterAssetIssueContractAddress2);
    logger.info("afterAssetIssueBAddress2:" + afterAssetIssueBAddress2);
    logger.info("afterAssetIssueCAddress2:" + afterAssetIssueCAddress2);
    logger.info("afterAssetIssueUserAddress2:" + afterAssetIssueUserAddress2);
    logger.info("afterBalanceContractAddress2:" + afterBalanceContractAddress2);
    logger.info("afterUserBalance2:" + afterUserBalance2);

    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(triggerTxid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress1 == afterAssetIssueUserAddress2);
    Assert.assertEquals(afterBalanceContractAddress1, afterBalanceContractAddress2);
    Assert.assertTrue(afterAssetIssueContractAddress1 == afterAssetIssueContractAddress2);
    Assert.assertTrue(afterAssetIssueBAddress1 == afterAssetIssueBAddress2);
    Assert.assertTrue(afterAssetIssueCAddress1 == afterAssetIssueCAddress2);

    //5. user trigger A to transfer token to B
    String param3 =
        "\"" + Base58.encode58Check(BTestAddress) + "\",\"" + Base58.encode58Check(CTestAddress) +
            "\",\"" + Base58.encode58Check(transferTokenContractAddress) +
            "\",1,\"" + tokenId
            .toStringUtf8()
            + "\"";

    String triggerTxid3 = PublicMethed.triggerContract(transferTokenContractAddress,
        "testIndelegateCall(address,address,address,uint256,trcToken)",
        param3, false, 0, 1000000000L, tokenId1
            .toStringUtf8(),
        1, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter3 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance3 = infoafter3.getBalance();
    Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
    Long afterAssetIssueDevAddress3 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
    Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    Long afterAssetIssueContractAddress3 = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress3 = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueCAddress3 = getAssetIssueValue(CTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress3 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afterBalanceContractAddress3 = PublicMethed.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();
    Long afterUserBalance3 = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("afterBalance3:" + afterBalance3);
    logger.info("afterEnergyUsed3:" + afterEnergyUsed3);
    logger.info("afterNetUsed3:" + afterNetUsed3);
    logger.info("afterFreeNetUsed3:" + afterFreeNetUsed3);
    logger.info("afterAssetIssueCount3:" + afterAssetIssueDevAddress3);
    logger.info("afterAssetIssueDevAddress3:" + afterAssetIssueContractAddress3);
    logger.info("afterAssetIssueBAddress3:" + afterAssetIssueBAddress3);
    logger.info("afterAssetIssueCAddress3:" + afterAssetIssueCAddress3);
    logger.info("afterAssetIssueUserAddress3:" + afterAssetIssueUserAddress3);
    logger.info("afterBalanceContractAddress3:" + afterBalanceContractAddress3);
    logger.info("afterUserBalance3:" + afterUserBalance3);

    Optional<TransactionInfo> infoById3 = PublicMethed
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    Assert.assertTrue(triggerTxid3 == null);
    Assert.assertTrue(afterAssetIssueUserAddress2 == afterAssetIssueUserAddress3);
    Assert.assertEquals(afterBalanceContractAddress2, afterBalanceContractAddress3);
    Assert.assertTrue(afterAssetIssueContractAddress2 == afterAssetIssueContractAddress3);
    Assert.assertTrue(afterAssetIssueBAddress2 == afterAssetIssueBAddress3);
    Assert.assertTrue(afterAssetIssueCAddress2 == afterAssetIssueCAddress3);

    //6. user trigger A to transfer token to B
    String param4 =
        "\"" + Base58.encode58Check(BTestAddress) + "\",\"" + Base58.encode58Check(CTestAddress) +
            "\",\"" + Base58.encode58Check(transferTokenContractAddress) +
            "\",1,\"" + tokenId
            .toStringUtf8()
            + "\"";

    String triggerTxid4 = PublicMethed.triggerContract(transferTokenContractAddress,
        "testIndelegateCall(address,address,address,uint256,trcToken)",
        param3, false, 0, 1000000000L, tokenId
            .toStringUtf8(),
        100000000, dev001Address, dev001Key,
        blockingStubFull);
    Account infoafter4 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter4 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance4 = infoafter4.getBalance();
    Long afterEnergyUsed4 = resourceInfoafter4.getEnergyUsed();
    Long afterAssetIssueDevAddress4 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed4 = resourceInfoafter4.getNetUsed();
    Long afterFreeNetUsed4 = resourceInfoafter4.getFreeNetUsed();
    Long afterAssetIssueContractAddress4 = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress4 = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueCAddress4 = getAssetIssueValue(CTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress4 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afterBalanceContractAddress4 = PublicMethed.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();
    Long afterUserBalance4 = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("afterBalance4:" + afterBalance4);
    logger.info("afterEnergyUsed4:" + afterEnergyUsed4);
    logger.info("afterNetUsed4:" + afterNetUsed4);
    logger.info("afterFreeNetUsed4:" + afterFreeNetUsed4);
    logger.info("afterAssetIssueCount4:" + afterAssetIssueDevAddress4);
    logger.info("afterAssetIssueDevAddress4:" + afterAssetIssueContractAddress4);
    logger.info("afterAssetIssueBAddress4:" + afterAssetIssueBAddress4);
    logger.info("afterAssetIssueCAddress4:" + afterAssetIssueCAddress4);
    logger.info("afterAssetIssueUserAddress4:" + afterAssetIssueUserAddress4);
    logger.info("afterBalanceContractAddress4:" + afterBalanceContractAddress4);
    logger.info("afterUserBalance4:" + afterUserBalance4);

    Optional<TransactionInfo> infoById4 = PublicMethed
        .getTransactionInfoById(triggerTxid4, blockingStubFull);
    Assert.assertTrue(triggerTxid4 == null);
    Assert.assertTrue(afterAssetIssueUserAddress3 == afterAssetIssueUserAddress4);
    Assert.assertEquals(afterBalanceContractAddress3, afterBalanceContractAddress4);
    Assert.assertTrue(afterAssetIssueContractAddress3 == afterAssetIssueContractAddress4);
    Assert.assertTrue(afterAssetIssueBAddress3 == afterAssetIssueBAddress4);
    Assert.assertTrue(afterAssetIssueCAddress3 == afterAssetIssueCAddress4);

    //2. user trigger A to transfer token to B
    String param5 =
        "\"" + Base58.encode58Check(BTestAddress) + "\",\"" + Base58.encode58Check(CTestAddress) +
            "\",\"" + Base58.encode58Check(transferTokenContractAddress) +
            "\",1,\"" + tokenId
            .toStringUtf8()
            + "\"";

    String triggerTxid5 = PublicMethed.triggerContract(transferTokenContractAddress,
        "testIndelegateCall(address,address,address,uint256,trcToken)",
        param3, false, 0, 1000000000L, tokenId
            .toStringUtf8(),
        1, dev001Address, dev001Key,
        blockingStubFull);
    Account infoafter5 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter5 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance5 = infoafter5.getBalance();
    Long afterEnergyUsed5 = resourceInfoafter5.getEnergyUsed();
    Long afterAssetIssueDevAddress5 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed5 = resourceInfoafter5.getNetUsed();
    Long afterFreeNetUsed5 = resourceInfoafter5.getFreeNetUsed();
    Long afterAssetIssueContractAddress5 = getAssetIssueValue(transferTokenContractAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueBAddress5 = getAssetIssueValue(BTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueCAddress5 = getAssetIssueValue(CTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress5 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afterBalanceContractAddress5 = PublicMethed.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();
    Long afterUserBalance5 = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("afterBalance5:" + afterBalance5);
    logger.info("afterEnergyUsed5:" + afterEnergyUsed5);
    logger.info("afterNetUsed5:" + afterNetUsed5);
    logger.info("afterFreeNetUsed5:" + afterFreeNetUsed5);
    logger.info("afterAssetIssueCount5:" + afterAssetIssueDevAddress5);
    logger.info("afterAssetIssueDevAddress5:" + afterAssetIssueContractAddress5);
    logger.info("afterAssetIssueBAddress5:" + afterAssetIssueBAddress5);
    logger.info("afterAssetIssueCAddress5:" + afterAssetIssueCAddress5);
    logger.info("afterAssetIssueUserAddress5:" + afterAssetIssueUserAddress5);
    logger.info("afterBalanceContractAddress5:" + afterBalanceContractAddress5);
    logger.info("afterUserBalance5:" + afterUserBalance5);

    Optional<TransactionInfo> infoById5 = PublicMethed
        .getTransactionInfoById(triggerTxid5, blockingStubFull);
    Assert.assertTrue(infoById5.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress4 == afterAssetIssueUserAddress5);
    Assert.assertEquals(afterBalanceContractAddress4, afterBalanceContractAddress5);
    Assert.assertTrue(afterAssetIssueContractAddress4 + 2 == afterAssetIssueContractAddress5);
    Assert.assertTrue(afterAssetIssueBAddress4 == afterAssetIssueBAddress5);
    Assert.assertTrue(afterAssetIssueCAddress4 - 1 == afterAssetIssueCAddress5);
    Assert.assertTrue(afterAssetIssueDevAddress4 - 1 == afterAssetIssueDevAddress5);

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



