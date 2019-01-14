package stest.tron.wallet.contract.trcToken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken012 {

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
  private byte[] receiveTokenContractAddress = null;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  
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

    PublicMethed.printAddress(user001Key);

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 5048_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 4048_000_000L, fromAddress,
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

  public static Long getAssetIssueValue(byte[] dev001Address, ByteString assetIssueId, WalletGrpc.WalletBlockingStub blockingStubFull) {
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
  
  private void testCreateAssetIssue(byte[] accountAddress, String priKey) {
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
  }

  @Test
  public void testTrcToken() {
    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);

    logger.info("** deploy transfer token contract");
    deployTransferTokenContract(dev001Address, dev001Key);

    logger.info("** trigger transfer token contract to normal address");
    triggerContract(transferTokenContractAddress, dev001Address, user001Address, user001Key);

    logger.info("** trigger token balance for a normal address");
    triggerTokenBalanceContract(transferTokenContractAddress, dev001Address, user001Address,
        user001Key);
  }

  public void deployTransferTokenContract(byte[] dev001Address, String dev001Key) {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull, null), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    testCreateAssetIssue(dev001Address, dev001Key);

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
    String code = "6080604052610343806100136000396000f3006080604052600436106100985763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663144401f6811461009a5780631ea31431146100b45780632c5c6641146100c8578063544051aa146100dc578063584d4262146100f057806371dc08ce1461011657806393bc91fd14610137578063a730416e1461014b578063acea104514610162575b005b610098600160a060020a0360043516602435604435610176565b610098600160a060020a03600435166101b6565b610098600160a060020a03600435166101f1565b610098600160a060020a0360043516610231565b610104600160a060020a0360043516610270565b60408051918252519081900360200190f35b61011e610281565b6040805192835260208301919091528051918290030190f35b610098600160a060020a0360043516610287565b610104600160a060020a03600435166024356102c7565b610098600160a060020a03600435166102d8565b604051600160a060020a0384169083156108fc029084908490600081818185878a8ad09450505050501580156101b0573d6000803e3d6000fd5b50505050565b604051600160a060020a038216906000906000199082908181818181878a82d09450505050501580156101ed573d6000803e3d6000fd5b5050565b604051600160a060020a0382169060009067800000000000000090620f4241908381818185878a84d09450505050501580156101ed573d6000803e3d6000fd5b604051600160a060020a03821690600090600190680200000000000f4241908381818185878a84d09450505050501580156101ed573d6000803e3d6000fd5b600160a060020a0316620f4241d190565bd3d29091565b604051600160a060020a03821690600090677fffffffffffffff90620f4241908381818185878a84d09450505050501580156101ed573d6000803e3d6000fd5b600160a060020a039190911690d190565b604051600160a060020a038216906108fc90600090678000000000000001908281818185828a8ad09450505050501580156101ed573d6000803e3d6000fd00a165627a7a72305820716217ff39873b1f852cdc214cfb38a4c2337d18fbd1128b69b2866f7f24ae1c0029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"tokenValue\",\"type\":\"uint256\"},{\"name\":\"id\",\"type\":\"trcToken\"}],\"name\":\"transferTokenTest\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTokenTestValueMaxBigInteger\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTokenTestValueOverBigInteger\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTokenTestValueRandomIdBigInteger\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"accountAddress\",\"type\":\"address\"}],\"name\":\"getTokenBalanceTest\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"msgTokenValueAndTokenIdTest\",\"outputs\":[{\"name\":\"\",\"type\":\"trcToken\"},{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTokenTestValueMaxLong\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},{\"name\":\"tokenId\",\"type\":\"trcToken\"}],\"name\":\"getTokenBalnce\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTokenTestValue0IdBigInteger\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";

    String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            assetAccountId.toStringUtf8(), 100, null, dev001Key,
            dev001Address, blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    PublicMethed.waitProduceNextBlock(blockingStubFull);

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

    Assert.assertTrue(PublicMethed.transferAsset(transferTokenContractAddress,
        assetAccountId.toByteArray(), 100L, dev001Address, dev001Key, blockingStubFull));
    Long contractAssetCount = getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: " + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(200), contractAssetCount);
  }


  public void deployRevContract(byte[] dev001Address, String dev001Key) {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(dev001Address, dev001Key, 50000L,
            blockingStubFull, null), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    // before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() +
        ", devAssetCountBefore: " + devAssetCountBefore);

    String contractName = "resultContract";
    String code = "608060405260658060116000396000f30060806040819052d38152d260a0523460c0527fd1ed7a3c020c4f5939654147940a147a8e4e638fa1e8f5664b5efbd1e1f3c4a690606090a10000a165627a7a72305820ea1e6d9606638e6c9e72dcdcc749561b826ec85713c003fae9d4700a2919040f0029";
    String abi = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"log\",\"type\":\"event\"}]";
    String recieveTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 1000, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address, blockingStubFull);

    // after deploy, check account resource
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
        .getTransactionInfoById(recieveTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy receive failed with message: " + infoById.get().getResMessage());
    }

    receiveTokenContractAddress = infoById.get().getContractAddress().toByteArray();

    SmartContract smartContract = PublicMethed
        .getContract(receiveTokenContractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Long contractAssetCount = getAssetIssueValue(receiveTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: " + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  public void triggerContract(byte[] transferTokenContractAddress,
      byte[] receiveTokenAddress, byte[] user001Address, String user001Key) {

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull, null), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.transferAsset(user001Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devEnergyLimitBefore is " + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, devEnergyUsageBefore is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userEnergyLimitBefore is " + Long.toString(userEnergyLimitBefore));
    logger.info("before trigger, userEnergyUsageBefore is " + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));

    Long transferAssetBefore = getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);

    Long receiveAssetBefore = getAssetIssueValue(receiveTokenAddress, assetAccountId, blockingStubFull);
    logger.info("before trigger, receiveTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + receiveAssetBefore);

    String tokenId = assetAccountId.toStringUtf8();
    Long tokenValue = Long.valueOf(1);
    Long callValue = Long.valueOf(0);

    String param = "\"" + Base58.encode58Check(receiveTokenAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("after trigger, userEnergyLimitAfter is " + Long.toString(userEnergyLimitAfter));
    logger.info("after trigger, userEnergyUsageAfter is " + Long.toString(userEnergyUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long originEnergyUsage = infoById.get().getReceipt().getOriginEnergyUsage();

    SmartContract smartContract = PublicMethed.getContract(infoById.get().getContractAddress()
        .toByteArray(), blockingStubFull);

    Long transferAssetAfter = getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", transferAssetAfter is " + transferAssetAfter);

    Long receiveAssetAfter = getAssetIssueValue(receiveTokenAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, resultContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", receiveAssetAfter is " + receiveAssetAfter);

    long consumeURPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeURPercent);

    Assert.assertEquals(originEnergyUsage, devEnergyUsageAfter - devEnergyUsageBefore);
    Assert.assertEquals(energyUsage, userEnergyUsageAfter - userEnergyUsageBefore);
    Assert.assertEquals(energyFee, userBalanceBefore - userBalanceAfter);
    Assert.assertEquals(receiveAssetAfter - receiveAssetBefore, transferAssetBefore + 2L - transferAssetAfter);

//    Assert.assertEquals(1, infoById.get().getInternalTransactionsCount());
//    Assert.assertEquals(1,
//        infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue());
//    Assert.assertEquals(assetAccountId.toStringUtf8(),
//        infoById.get().getInternalTransactions(0).getCallValueInfo(0).getTokenId());
  }


  public void triggerTokenBalanceContract(byte[] tokenBalanceContractAddress,
      byte[] receiveTokenContractAddress, byte[] User001Address, String User001Key) {

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(user001Address, 1000_000_000L,
        0, 1, user001Key, blockingStubFull));

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("before trigger, dev energy limit is " + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, dev energy usage is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, dev balance is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(User001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethed.queryAccount(User001Address,
        blockingStubFull).getBalance();

    logger.info("before trigger, user energy limit is " + Long.toString(userEnergyLimitBefore));
    logger.info("before trigger, user energy usage is " + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, user balance is " + Long.toString(userBalanceBefore));

    String param = "\"" + Base58.encode58Check(receiveTokenContractAddress) + "\",\""
        + assetAccountId.toStringUtf8() + "\"";

    String triggerTxid = PublicMethed.triggerContract(tokenBalanceContractAddress,
        "getTokenBalnce(address,trcToken)",
        param, false, 0, 1000000000L, User001Address,
        User001Key, blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after trigger, devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("after trigger, userEnergyLimitAfter is " + Long.toString(userEnergyLimitAfter));
    logger.info("after trigger, userEnergyUsageAfter is " + Long.toString(userEnergyUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(triggerTxid,
        blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long originEnergyUsage = infoById.get().getReceipt().getOriginEnergyUsage();

    SmartContract smartContract = PublicMethed.getContract(infoById.get().getContractAddress()
        .toByteArray(), blockingStubFull);

    long consumeURPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeURPercent);

//    Assert.assertEquals(originEnergyUsage, devEnergyUsageAfter - devEnergyUsageBefore);
//    Assert.assertEquals(energyUsage, userEnergyUsageAfter - userEnergyUsageBefore);
//    Assert.assertEquals(energyFee, userBalanceBefore - userBalanceAfter);

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }
    logger.info("the receivercontract token: " + ByteArray
        .toLong(infoById.get().getContractResult(0).toByteArray()));
    Long assetIssueCount = getAssetIssueValue(receiveTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("the receivercontract token(getaccount): " + assetIssueCount);
    Assert.assertTrue(assetIssueCount == ByteArray
        .toLong(ByteArray.fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


