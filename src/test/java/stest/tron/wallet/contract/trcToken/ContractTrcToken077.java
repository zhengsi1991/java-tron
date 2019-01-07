package stest.tron.wallet.contract.trcToken;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken077 {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForGrammarAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = false)
  public void testAddress001() {
    PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AddressTest";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5060b6806100396000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416636241c1d881146043575b600080fd5b348015604e57600080fd5b50d38015605a57600080fd5b50d28015606657600080fd5b50606d607f565b60408051918252519081900360200190f35b6040513090819052905600a165627a7a723058207faa3a33e3b875c23563f2fb8ee4506affcd21d818c8399a71dd76fa973f487d0029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"addressTest\",\"outputs\":[{\"name\":\"addressValue\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "addressTest()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
//    Long returnnumber = ByteArray.toLong(ByteArray.fromHexString(ByteArray.toHexString(
//        infoById.get().getContractResult(0).toByteArray())));

  }

  @Test(enabled = true)
  public void testAddress002() {
    PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "AddressTest";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b506092806100396000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416636241c1d881146043575b600080fd5b6049605b565b60408051918252519081900360200190f35b6040513090819052905600a165627a7a723058200245dccce3c678939aba5ad813f3fd8d551ef14f01409a8e36bc3dac301113c40029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"addressTest\",\"outputs\":[{\"name\":\"addressValue\",\"type\":\"bytes32\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "addressTest()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);

    byte[] a = infoById.get().getContractResult(0).toByteArray();
    byte[] b = subByte(a, 11, 1);
    byte[] c = subByte(a, 0, 11);
    byte[] e = "41".getBytes();
    byte[] d = subByte(a, 12, 20);

    logger.info("a:" + ByteArray.toHexString(a));

    logger.info("b:" + ByteArray.toHexString(b));
    logger.info("c:" + ByteArray.toHexString(c));

    logger.info("d:" + ByteArray.toHexString(d));

    logger.info("41" + ByteArray.toHexString(d));
    String exceptedResult = "41" + ByteArray.toHexString(d);
    String realResult = ByteArray.toHexString(b);
    Assert.assertEquals(realResult, "00");
    Assert.assertNotEquals(realResult, "41");

    Assert.assertEquals(exceptedResult, ByteArray.toHexString(contractAddress));

  }

  public byte[] subByte(byte[] b, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;

  }


}
