package stest.tron.wallet.multiSign.accountPermissionUpdate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;
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
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.Sha256Hash;

@Slf4j
public class accountPermissionUpdate013 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

  private final String contractTRONdiceAddr = "TMYcx6eoRXnePKT1jVn25ZNeMNJ6828HWk";

//  private ECKey ecKey1 = new ECKey(Utils.getRandom());
//  private byte[] ownerAddress = ecKey1.getAddress();
//  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private String ownerKey  = "99454aca732c9335c32fedb56ee552f4e410d61a1390763f458187e162a4840b";
  private byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

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
  public void test01SignByOwnerKey() {
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
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]}]";

    ownerPermissionKeys.add(ownerKey);
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    logger.info("** update owner permission to one address");
    accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]}]";

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    ownerPermissionKeys.remove(testKey002);
    recoverAccountPermission(ownerKey, ownerPermissionKeys);
  }

  @Test
  public void test02SignByActiveKey() {
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
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]}]";

    ownerPermissionKeys.add(ownerKey);
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    logger.info("** update owner permission to two address");
    accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]}]";

    activePermissionKeys.add(testKey002);

    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        activePermissionKeys.toArray(new String[activePermissionKeys.size()]));

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertThat(response.getMessage().toStringUtf8(),
        containsString("it is not contained of permission"));

    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
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
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]}]";

    ownerPermissionKeys.add(normalKey001);
    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertThat(response.getMessage().toStringUtf8(),
        containsString("it is not contained of permission"));

    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));
    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));
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

    logger.info("** update active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]}]";

    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull, ownerPermissionKeys);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertEquals("validate signature error miss sig or contract",
        response.getMessage().toStringUtf8());

    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));
    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));
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
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]}]";

    boolean ret = false;
    try {
      PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
          ownerAddress, ownerKey, blockingStubFull, ownerPermissionKeys);
    } catch (NullPointerException e) {
      logger.info("NullPointerException !");
      ret = true;
    }

    Assert.assertTrue(ret);
    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));
    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));
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
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]}]";

    ownerPermissionKeys.add(emptyKey);

    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertThat(response.getMessage().toStringUtf8(),
        containsString("it is not contained of permission"));

    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));
    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));
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
    recoverAccountPermission(ownerKey, ownerPermissionKeys);
    recoverAccountPermission(ownerKey, ownerPermissionKeys);
    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));
    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));
    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());
  }

  @Test
  public void test08SignListMoreThanPermissionKeys() {
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
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2}]}]";

    ownerPermissionKeys.add(testKey002);
    ownerPermissionKeys.add(ownerKey);

    GrpcAPI.Return response = PublicMethedForMutiSign.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(SIGERROR, response.getCode());
    Assert.assertEquals("validate signature error Signature count "
            + "is 2 more than key counts of permission : 1", response.getMessage().toStringUtf8());

    Assert.assertEquals(0, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

  }


  public void recoverAccountPermission(String ownerKey, List<String> ownerPermissionKeys) {
    logger.info("** recover account permissions");

    PublicMethed.printAddress(ownerKey);
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\",\"keys\":["
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


