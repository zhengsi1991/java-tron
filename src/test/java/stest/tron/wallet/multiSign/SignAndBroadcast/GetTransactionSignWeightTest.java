package stest.tron.wallet.multiSign.SignAndBroadcast;

import static org.hamcrest.CoreMatchers.containsString;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.PERMISSION_ERROR;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.*;
import static stest.tron.wallet.common.client.utils.TransactionUtils.sign;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.transaction.jta.TransactionFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.Sha256Hash;

@Slf4j
public class GetTransactionSignWeightTest {

    private final String testKey002 = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key1");
    private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

    private final String witnessKey001 = Configuration.getByPath("testng.conf")
        .getString("witness.key1");
    private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

    private final String contractTRONdiceAddr = "TMYcx6eoRXnePKT1jVn25ZNeMNJ6828HWk";

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

//    private String ownerKey  = "99454aca732c9335c32fedb56ee552f4e410d61a1390763f458187e162a4840b";
//    private byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    private ECKey ecKey2 = new ECKey(Utils.getRandom());
    private byte[] normalAddr001 = ecKey2.getAddress();
    private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    private ECKey tmpECKey01 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr01 = tmpECKey01.getAddress();
    private String tmpKey01 = ByteArray.toHexString(tmpECKey01.getPrivKeyBytes());

    private ECKey tmpECKey02 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr02 = tmpECKey02.getAddress();
    private String tmpKey02 = ByteArray.toHexString(tmpECKey02.getPrivKeyBytes());

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
      PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    }

    @Test
  public void test01BroadcastMultiSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);

    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight = PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, tmpKey02, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before Sign2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(4, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(4, txWeight.getCurrentWeight());

    recoverAccountPermission(ownerKey, ownerPermissionKeys);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("Signature count is 2 more than key counts of permission : 1",
        txWeight.getResult().getMessage());
  }

  @Test
  public void test02BroadcastMultiSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":5,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    logger.info("** trigger a permission transaction");

    Transaction transaction = PublicMethedForMutiSign.permissionAddKeyWithoutSign("owner",
        tmpAddr01, 1L, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight = PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before sign permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before sign2 permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(5, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(5, txWeight.getCurrentWeight());

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("Signature count is 2 more than key counts of permission : 1", txWeight.getResult().getMessage());
  }

  @Test
  public void test03BroadcastSingleSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, tmpKey02, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));
  }

  @Test
  public void test04BroadcastNotSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":5,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    logger.info("** trigger a permission transaction");

    Transaction transaction = PublicMethedForMutiSign.permissionAddKeyWithoutSign("owner",
        tmpAddr01, 1L, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
  }

  @Test
  public void test05BroadcastMultiSignNotCompletePermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":5,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    logger.info("** trigger a permission transaction");

    Transaction transaction = PublicMethedForMutiSign.permissionAddKeyWithoutSign("owner",
        tmpAddr01, 1L, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
  }

  @Test
  public void test06BroadcastSignFailedTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(
        transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
  }

  @Test
  public void test07BroadcastTimeoutTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());


    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    try {
      Thread.sleep(70000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
  }

  @Test
  public void test08BroadcastEmptyTransaction() {
     PublicMethed.printAddress(ownerKey);

    logger.info("** created an empty transaction");

    Contract.AccountPermissionUpdateContract.Builder builder =
        Contract.AccountPermissionUpdateContract.newBuilder();

    Contract.AccountPermissionUpdateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    Transaction transaction = transactionExtention.getTransaction();

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethed.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(OTHER_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("class java.lang.IndexOutOfBoundsException : Index: 0",
        txWeight.getResult().getMessage());

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(OTHER_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("class java.lang.IndexOutOfBoundsException : Index: 0",
        txWeight.getResult().getMessage());
  }

  @Test
  public void test09BroadcastErrorTransaction() {
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1_000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, ownerKey, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethed.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
  }

  public void recoverAccountPermission(String ownerKey, List<String> ownerPermissionKeys) {
    logger.info("** recover account permissions");

    PublicMethed.printAddress(ownerKey);
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    String accountPermissionJson =
        "[{\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]}]";

    boolean ret = PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Assert.assertTrue(ret);
    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));
    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));
  }


  public static int getPermissionCount(List<Permission> permissionList, String permissionName) {
    int permissionCount = 0;
    for (Permission permission : permissionList) {
      if (permission.getName().equals(permissionName)) {
        permissionCount = permission.getKeysCount();
        break;
      }
    }
    return permissionCount;
  }


  public static List<String> getPermissionAddress(List<Permission> permissionList, String permissionName) {
    List<String> permissionAddress = new ArrayList<>();
    for (Permission permission : permissionList) {
      if (permission.getName().equals(permissionName)) {
        if (permission.getKeysCount() > 0) {
          for (Key key : permission.getKeysList()) {
            permissionAddress.add(encode58Check(key.getAddress().toByteArray()));
          }
        }
        break;
      }
    }
    return permissionAddress;
  }

  public static void printPermissionList(List<Permission> permissionList) {
    String result = "\n";
    result += "[";
    result += "\n";
    int i = 0;
    for (Permission permission : permissionList) {
      result += "permission " + i + " :::";
      result += "\n";
      result += "{";
      result += "\n";
      result += printPermission(permission);
      result += "\n";
      result += "}";
      result += "\n";
      i++;
    }
    result += "]";
    System.out.println(result);
  }

  public static String printPermission(Permission permission) {
    StringBuffer result = new StringBuffer();
    result.append("name: ");
    result.append(permission.getName());
    result.append("\n");
    result.append("threshold: ");
    result.append(permission.getThreshold());
    result.append("\n");
    if (permission.getKeysCount() > 0) {
      result.append("keys:");
      result.append("\n");
      result.append("[");
      result.append("\n");
      for (Key key : permission.getKeysList()) {
        result.append(printKey(key));
      }
      result.append("]");
      result.append("\n");
    }
    return result.toString();
  }

  public static String printKey(Key key) {
    StringBuffer result = new StringBuffer();
    result.append("address: ");
    result.append(encode58Check(key.getAddress().toByteArray()));
    result.append("\n");
    result.append("weight: ");
    result.append(key.getWeight());
    result.append("\n");
    return result.toString();
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
