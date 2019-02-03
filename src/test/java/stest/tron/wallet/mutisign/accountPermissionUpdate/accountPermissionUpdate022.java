package stest.tron.wallet.mutisign.accountPermissionUpdate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.tron.api.GrpcAPI.Return.response_code.SIGERROR;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class accountPermissionUpdate022 {

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

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] normalAddr001 = ecKey2.getAddress();
  private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
  }

  @Test
  public void test01SignByOwnerKey() {
    // use owner to sign permission transaction
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
    PublicMethed.printAddress(ownerKey);

    logger.info("** update owner permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(ownerKey);

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(1,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** update owner permission to one address");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";
    ownerPermissionKeys.add(testKey002);

    Assert.assertTrue(
        PublicMethedForMutiSign.accountPermissionUpdateWithPermissionId(accountPermissionJson,
            ownerAddress, ownerKey, blockingStubFull, 0,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(1,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());
  }


  @Test
  public void test01SignByOwnerKey02() {
    // use owner to sign normal transaction

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
    PublicMethed.printAddress(ownerKey);

    logger.info("** update owner permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(ownerKey);

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(1,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    ownerPermissionKeys.add(testKey002);

    logger.info("** trigger a normal permission");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 0, ownerKey,
            blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));
  }


  @Test
  public void test02SignByActiveKey() {
    // use active to sign normal transaction
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

    List<String> activePermissionKeys = new ArrayList<>();
    List<String> ownerPermissionKeys = new ArrayList<>();
    PublicMethed.printAddress(ownerKey);

    logger.info("** update active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(normalKey001) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(ownerKey);
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    activePermissionKeys.add(testKey002);
    activePermissionKeys.add(normalKey001);

    logger.info("** update owner permission to two address");
    logger.info("** trigger a normal permission");
    Assert.assertFalse(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 0, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));

    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress, 2, ownerKey,
            blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  }


  @Test
  public void test02SignByActiveKey02() {
    // use active to sign permission transaction
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

    Integer[] ints = {ContractType.AccountPermissionUpdateContract_VALUE};
    String operations = PublicMethedForMutiSign.getOperations(ints);

    List<String> activePermissionKeys = new ArrayList<>();
    List<String> ownerPermissionKeys = new ArrayList<>();
    PublicMethed.printAddress(ownerKey);

    logger.info("** update active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(normalKey001) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(testKey002);
    activePermissionKeys.add(normalKey001);

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(
        PublicMethedForMutiSign.accountPermissionUpdateWithPermissionId(accountPermissionJson,
            ownerAddress, ownerKey, blockingStubFull, 2,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));

    Assert.assertEquals(1,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));
  }


  @Test
  public void test02SignByActiveKey03() {
    // use default active permission to sign permission transaction

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

    List<String> activePermissionKeys = new ArrayList<>();
    List<String> ownerPermissionKeys = new ArrayList<>();
    PublicMethed.printAddress(ownerKey);

    logger.info("** update active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(normalKey001) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(testKey002);
    activePermissionKeys.add(normalKey001);

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertFalse(PublicMethedForMutiSign.accountPermissionUpdateWithPermissionId(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull, 2,
        activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  }


  @Test
  public void test03SignByOtherKey() {
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
    PublicMethed.printAddress(ownerKey);

    logger.info("** update active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(normalKey001);
    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertThat(response.getMessage().toStringUtf8(),
        containsString("it is not contained of permission"));

    Assert.assertEquals(1, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));
  }

  @Test
  public void test04SignByEmptyObjectKey() {
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

    String[] ownerPermissionKeys = new String[0];
    PublicMethed.printAddress(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull, ownerPermissionKeys);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertEquals("validate signature error miss sig or contract",
        response.getMessage().toStringUtf8());

    Assert.assertEquals(1,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));
  }

  @Test
  public void test05SignByEmptyKey() {
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

    String[] ownerPermissionKeys = new String[1];
    PublicMethed.printAddress(ownerKey);
    ownerPermissionKeys[0] = "";

    logger.info("** update active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    boolean ret = false;
    try {
      PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
          ownerAddress, ownerKey, blockingStubFull, ownerPermissionKeys);
    } catch (NumberFormatException e) {
      logger.info("NumberFormatException !");
      ret = true;
    } catch (NullPointerException e) {
      logger.info("NullPointerException !");
      ret = true;
    }

    Assert.assertTrue(ret);
    Assert.assertEquals(1,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));
  }

  @Test
  public void test06SignByStringKey() {
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

    PublicMethed.printAddress(ownerKey);
    String emptyKey = "abc1222";

    logger.info("** update active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + emptyKey + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(emptyKey);

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
          accountPermissionJson, ownerKey.getBytes(), ownerKey, blockingStubFull,
          ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));
    } catch (NullPointerException e) {
      logger.info("NullPointerException !");
      ret = true;
    }
    Assert.assertTrue(ret);
  }


  @Test
  public void test07RepeatUpdateSamePermission() {
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
    ownerPermissionKeys.add(ownerKey);
    PublicMethedForMutiSign
        .recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
    PublicMethedForMutiSign
        .recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
    Assert.assertEquals(1,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));
  }

  @Test
  public void test08SignListMoreThanPermissionKeys() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();
    PublicMethed.printAddress(ownerKey);

    logger.info("** update active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    ownerPermissionKeys.add(testKey002);
    ownerPermissionKeys.add(ownerKey);

    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertEquals("validate signature error Signature count "
        + "is 2 more than key counts of permission : 1", response.getMessage().toStringUtf8());

    Assert.assertEquals(1, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


