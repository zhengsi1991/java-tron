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
public class ContractTrcToken035 {

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
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b506101638061003a6000396000f30060806040526004361061004b5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416634957baa18114610050578063e308c52714610079575b600080fd5b61007773ffffffffffffffffffffffffffffffffffffffff600435166024356044356100a0565b005b61007773ffffffffffffffffffffffffffffffffffffffff600435166024356044356100ea565b60405173ffffffffffffffffffffffffffffffffffffffff84169083156108fc029084908490600081818185878a8ad09450505050501580156100e7573d6000803e3d6000fd5b50fe5b60405173ffffffffffffffffffffffffffffffffffffffff84169083156108fc029084908490600081818185878a8ad0945050505050158015610131573d6000803e3d6000fd5b50600080fd00a165627a7a723058204c2d6334ac3ea724949482094951c3b7d11fea1e87b58110d562ef47ddd45c7b0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"failTransferTokenError\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"failTransferTokenRevert\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
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

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);

    // user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(user001Address) + "\",1,\"" + tokenId1
            .toStringUtf8()
            + "\"";

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "failTransferTokenError(address,uint256,trcToken)",
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
    Long afterAssetIssueUserAddress = getAssetIssueValue(user001Address, tokenId, blockingStubFull);

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(beforeAssetIssueDevAddress, afterAssetIssueDevAddress);
    Assert.assertEquals(beforeAssetIssueUserAddress, afterAssetIssueUserAddress);
    Assert.assertEquals(beforeAssetIssueContractAddress, afterAssetIssueContractAddress);
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


