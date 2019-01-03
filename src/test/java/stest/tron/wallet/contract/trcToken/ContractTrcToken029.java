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
public class ContractTrcToken029 {

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
    String contractName = "tokenTest";
    String code = "608060405261034f806100136000396000f30060806040526004361061004b5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416630dbe671f8114610050578063c3e911d4146100a8575b600080fd5b34801561005c57600080fd5b50d3801561006957600080fd5b50d2801561007657600080fd5b5061007f6100b8565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b6100b66004356024356100d4565b005b60005473ffffffffffffffffffffffffffffffffffffffff1681565b60006100de6101de565b604051809103906000f0801580156100fa573d6000803e3d6000fd5b5090508073ffffffffffffffffffffffffffffffffffffffff1663071f65a883856040518363ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018083815260200182815260200192505050602060405180830381600087803b15801561017457600080fd5b505af1158015610188573d6000803e3d6000fd5b505050506040513d602081101561019e57600080fd5b50506000805473ffffffffffffffffffffffffffffffffffffffff191673ffffffffffffffffffffffffffffffffffffffff929092169190911790555050565b604051610135806101ef83390190560060806040526000805561011e806100176000396000f30060806040526004361060525763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663071f65a881146054578063890eba68146074578063f96339301460b0575b005b606060043560243560da565b604080519115158252519081900360200190f35b348015607f57600080fd5b50d38015608b57600080fd5b50d28015609757600080fd5b50609e60e6565b60408051918252519081900360200190f35b34801560bb57600080fd5b50d3801560c757600080fd5b50d2801560d357600080fd5b50609e60ec565b50506009600090815590565b60005481565b600054905600a165627a7a72305820a699d9eec788d2a222b703af447d9c2ddffb626fc07a57834af8e3f9bce64dc90029a165627a7a72305820626050611353c2f66f9b94e3935c21fd4dad7cc494b7459f3597451e1b6004de0029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"a\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"id\",\"type\":\"trcToken\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"transferTokenWithSameName\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    byte[] transferTokenContractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
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
    Long beforeBalanceContractAddress = PublicMethed.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();
    Long beforeUserBalance = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
    logger.info("beforeBalanceContractAddress:" + beforeBalanceContractAddress);
    logger.info("beforeUserBalance:" + beforeUserBalance);

    // user trigger A to transfer token to B
    String param = "\"" + tokenId1.toStringUtf8() + "\",\"1\"";

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "transferTokenWithSameName(trcToken,uint256)",
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
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);
    logger.info("afterUserBalance:" + afterUserBalance);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress == beforeAssetIssueUserAddress);
    Assert.assertEquals(afterBalanceContractAddress, beforeBalanceContractAddress);
    Assert.assertEquals(afterUserBalance, beforeUserBalance);
    Assert.assertTrue(afterAssetIssueContractAddress == beforeAssetIssueContractAddress);

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


