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
public class ContractTrcToken036 {

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
        .assertTrue(PublicMethed.sendcoin(dev001Address, 9999000000L, fromAddress,
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
    String contractName = "tokenTest";
    String code = "60806040526101b4806100136000396000f3006080604052600436106100405763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632849fa4f8114610042575b005b61004073ffffffffffffffffffffffffffffffffffffffff600435166024356040805134815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d2815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d3815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160405173ffffffffffffffffffffffffffffffffffffffff831690d280156108fc0291d390600081818185878a8ad0945050505050158015610140573d6000803e3d6000fd5b5060405173ffffffffffffffffffffffffffffffffffffffff8316903480156108fc02916000818181858888f19350505050158015610183573d6000803e3d6000fd5b5050505600a165627a7a72305820ddc15bf8809c92f9c6b6175f2ce1245ef1d65e5fbeccc2bb2772980cedee1eee0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"tokenValue\",\"type\":\"uint256\"}],\"name\":\"transferTokenWithPure\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"log\",\"type\":\"event\"}]";
    //"[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"failTransferTokenError\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"failTransferTokenRevert\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
    byte[] transferTokenContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // devAddress transfer token to userAddress
    PublicMethed
        .transferAsset(transferTokenContractAddress, tokenId.toByteArray(), 100, dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethed.sendcoin(transferTokenContractAddress, 100, fromAddress,
            testKey002, blockingStubFull));
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
    Long user001AddressAddressBalance = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance);

    // user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "transferTokenWithPure(address,uint256)",
        param, false, 10, 1000000000L, tokenId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
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
    Long afterAssetIssueUserAddress = getAssetIssueValue(user001Address, tokenId, blockingStubFull);
    Long afteruser001AddressAddressBalance = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);
    logger.info("afterContractAddressBalance:" + afteruser001AddressAddressBalance);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(beforeBalance - 10 == afterBalance);
    Assert.assertTrue(beforeAssetIssueDevAddress - 10 == afterAssetIssueDevAddress);
    Assert.assertTrue(beforeAssetIssueUserAddress + 10 == afterAssetIssueUserAddress);
    Assert.assertTrue(user001AddressAddressBalance + 10 == afteruser001AddressAddressBalance);

    String contractName1 = "transferTokenWithPureTest";
    String code1 = "60806040526101db806100136000396000f3006080604052600436106100405763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416639d9e5a388114610042575b005b34801561004e57600080fd5b50d3801561005b57600080fd5b50d2801561006857600080fd5b5061004073ffffffffffffffffffffffffffffffffffffffff600435166024356040805134815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d2815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d3815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160405173ffffffffffffffffffffffffffffffffffffffff831690d280156108fc0291d390600081818185878a8ad0945050505050158015610167573d6000803e3d6000fd5b5060405173ffffffffffffffffffffffffffffffffffffffff8316903480156108fc02916000818181858888f193505050501580156101aa573d6000803e3d6000fd5b5050505600a165627a7a7230582052825d8daae4fb444ce76f049e019c2c66d7b29584058ee7cc88a389e22f41dc0029";
    String abi1 = "[{\"constant\":true,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"tokenValue\",\"type\":\"uint256\"}],\"name\":\"transferTokenWithConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"log\",\"type\":\"event\"}]";
    byte[] transferTokenWithPureTestAddress = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // devAddress transfer token to userAddress
    PublicMethed
        .transferAsset(transferTokenWithPureTestAddress, tokenId.toByteArray(), 100, dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethed.sendcoin(transferTokenWithPureTestAddress, 100, fromAddress,
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info1;
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    info1 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance1 = info1.getBalance();
    Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
    Long beforeNetUsed1 = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    Long beforeAssetIssueDevAddress1 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long beforeAssetIssueUserAddress1 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);

    Long beforeAssetIssueContractAddress1 = getAssetIssueValue(transferTokenContractAddress,
        tokenId,
        blockingStubFull);
    Long user001AddressAddressBalance1 = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance1);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed1);
    logger.info("beforeNetUsed:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed1);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress1);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress1);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress1);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance1);

    // user trigger A to transfer token to B
    String param1 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    String triggerTxid1 = PublicMethed.triggerContract(transferTokenWithPureTestAddress,
        "transferTokenWithConstant(address,uint256)",
        param, false, 10, 1000000000L, tokenId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
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
    Long afterAssetIssueUserAddress1 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afteruser001AddressAddressBalance1 = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterEnergyUsed:" + afterEnergyUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress1);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress1);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress1);
    logger.info("afterContractAddressBalance:" + afteruser001AddressAddressBalance1);

    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(beforeBalance1, afterBalance1);
    Assert.assertEquals(beforeAssetIssueDevAddress1, afterAssetIssueDevAddress1);
    Assert.assertEquals(beforeAssetIssueUserAddress1, afterAssetIssueUserAddress1);
    Assert.assertEquals(user001AddressAddressBalance1, afteruser001AddressAddressBalance1);

    String contractName2 = "transferTokenWithViewTest";
    String code2 = "60806040526101db806100136000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663abecd9838114610042575b005b34801561004e57600080fd5b50d3801561005b57600080fd5b50d2801561006857600080fd5b5061004073ffffffffffffffffffffffffffffffffffffffff600435166024356040805134815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d2815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d3815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160405173ffffffffffffffffffffffffffffffffffffffff831690d280156108fc0291d390600081818185878a8ad0945050505050158015610167573d6000803e3d6000fd5b5060405173ffffffffffffffffffffffffffffffffffffffff8316903480156108fc02916000818181858888f193505050501580156101aa573d6000803e3d6000fd5b5050505600a165627a7a7230582020ddb4a20df32d91af717609926b277d218ac25965d8003ff48adf57a0f10f950029";
    String abi2 = "[{\"constant\":true,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"tokenValue\",\"type\":\"uint256\"}],\"name\":\"transferTokenWithView\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"log\",\"type\":\"event\"}]";
    byte[] transferTokenWithViewAddress = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // devAddress transfer token to userAddress
    PublicMethed
        .transferAsset(transferTokenWithViewAddress, tokenId.toByteArray(), 100, dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethed.sendcoin(transferTokenWithViewAddress, 100, fromAddress,
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info2;
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    info2 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance2 = info2.getBalance();
    Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
    Long beforeNetUsed2 = resourceInfo2.getNetUsed();
    Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    Long beforeAssetIssueDevAddress2 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long beforeAssetIssueUserAddress2 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);

    Long beforeAssetIssueContractAddress2 = getAssetIssueValue(transferTokenWithViewAddress,
        tokenId,
        blockingStubFull);
    Long user001AddressAddressBalance2 = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeAssetIssueContractAddress2:" + beforeAssetIssueContractAddress2);
    logger.info("beforeAssetIssueDevAddress2:" + beforeAssetIssueDevAddress2);
    logger.info("beforeAssetIssueUserAddress2:" + beforeAssetIssueUserAddress2);
    logger.info("user001AddressAddressBalance2:" + user001AddressAddressBalance2);

    // user trigger A to transfer token to B
    String param2 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    String triggerTxid2 = PublicMethed.triggerContract(transferTokenWithViewAddress,
        "transferTokenWithView(address,uint256)",
        param, false, 10, 1000000000L, tokenId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter2 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
    Long afterAssetIssueDevAddress2 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    Long afterAssetIssueContractAddress2 = getAssetIssueValue(transferTokenWithViewAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress2 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afteruser001AddressAddressBalance2 = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterAssetIssueDevAddress2:" + afterAssetIssueDevAddress2);
    logger.info("afterAssetIssueContractAddress2:" + afterAssetIssueContractAddress2);
    logger.info("afterAssetIssueUserAddress2:" + afterAssetIssueUserAddress2);
    logger.info("afteruser001AddressAddressBalance2:" + afteruser001AddressAddressBalance2);

    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(triggerTxid2, blockingStubFull);

    Assert.assertEquals(beforeAssetIssueDevAddress2, afterAssetIssueDevAddress2);
    Assert.assertEquals(beforeAssetIssueUserAddress2, afterAssetIssueUserAddress2);
    Assert.assertEquals(user001AddressAddressBalance2, afteruser001AddressAddressBalance2);

    String contractName3 = "transferTokenWithOutPayableTest";
    String code3 = "60806040526101db806100136000396000f3006080604052600436106100405763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632b38e5478114610042575b005b34801561004e57600080fd5b50d3801561005b57600080fd5b50d2801561006857600080fd5b5061004073ffffffffffffffffffffffffffffffffffffffff600435166024356040805134815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d2815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160408051d3815290517ff82c50f1848136e6c140b186ea0c768b7deda5efffe42c25e96336a90b26c7449181900360200190a160405173ffffffffffffffffffffffffffffffffffffffff831690d280156108fc0291d390600081818185878a8ad0945050505050158015610167573d6000803e3d6000fd5b5060405173ffffffffffffffffffffffffffffffffffffffff8316903480156108fc02916000818181858888f193505050501580156101aa573d6000803e3d6000fd5b5050505600a165627a7a723058203c20218af63eda8b09394f27964892ee199fa89d93e03506f9a0390dc0b9605d0029";
    String abi3 = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"tokenValue\",\"type\":\"uint256\"}],\"name\":\"transferTokenWithOutPayable\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"log\",\"type\":\"event\"}]";
    byte[] transferTokenWithOutPayableTestAddress = PublicMethed
        .deployContract(contractName3, abi3, code3, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

// devAddress transfer token to userAddress
    PublicMethed
        .transferAsset(transferTokenWithViewAddress, tokenId.toByteArray(), 100, dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethed.sendcoin(transferTokenWithViewAddress, 100, fromAddress,
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info3;
    AccountResourceMessage resourceInfo3 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    info3 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance3 = info3.getBalance();
    Long beforeEnergyUsed3 = resourceInfo3.getEnergyUsed();
    Long beforeNetUsed3 = resourceInfo3.getNetUsed();
    Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
    Long beforeAssetIssueDevAddress3 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long beforeAssetIssueUserAddress3 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);

    Long beforeAssetIssueContractAddress3 = getAssetIssueValue(
        transferTokenWithOutPayableTestAddress,
        tokenId,
        blockingStubFull);
    Long user001AddressAddressBalance3 = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance1);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed1);
    logger.info("beforeNetUsed:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed1);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress1);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress1);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress1);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance1);

// user trigger A to transfer token to B
    String param3 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    String triggerTxid3 = PublicMethed.triggerContract(transferTokenWithOutPayableTestAddress,
        "transferTokenWithOutPayable(address,uint256)",
        param, false, 10, 1000000000L, tokenId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter3 = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance3 = infoafter3.getBalance();
    Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
    Long afterAssetIssueDevAddress3 = getAssetIssueValue(dev001Address, tokenId, blockingStubFull);
    Long afterNetUsed3 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    Long afterAssetIssueContractAddress3 = getAssetIssueValue(
        transferTokenWithOutPayableTestAddress, tokenId,
        blockingStubFull);
    Long afterAssetIssueUserAddress3 = getAssetIssueValue(user001Address, tokenId,
        blockingStubFull);
    Long afteruser001AddressAddressBalance3 = PublicMethed
        .queryAccount(user001Address, blockingStubFull).getBalance();

    Optional<TransactionInfo> infoById3 = PublicMethed
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 1);

    Assert.assertEquals(beforeAssetIssueDevAddress3, afterAssetIssueDevAddress3);
    Assert.assertEquals(beforeAssetIssueUserAddress3, afterAssetIssueUserAddress3);
    Assert.assertEquals(user001AddressAddressBalance3, afteruser001AddressAddressBalance3);


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


