package stest.tron.wallet.contract.trcToken;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j

public class ContractTrcToken078 {

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
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForinternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForinternalTxsAddress);
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


  @Test(enabled = true)
  public void testOriginCall001() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName = "AAContract";
    String code = "6080604052610264806100136000396000f30060806040526004361061003d5763ffffffff60e060020a600035041663648efe8b811461003f578063b45f578b14610059578063d818452114610073575b005b61003d600160a060020a036004358116906024351661008d565b61003d600160a060020a036004358116906024351661011c565b61003d600160a060020a03600435811690602435166101a9565b81600160a060020a031660405180807f7472616e73666572546f286164647265737329000000000000000000000000008152506013019050604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051808303816000875af2505050505050565b81600160a060020a031660405180807f7472616e73666572546f286164647265737329000000000000000000000000008152506013019050604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405180830381865af4505050505050565b81600160a060020a031660405180807f7472616e73666572546f286164647265737329000000000000000000000000008152506013019050604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051808303816000875af15050505050505600a165627a7a7230582068a85b5cb5a41f10a7ba8250baed5adf37129ff04399bccae69e483fc85448a90029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\"},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB3\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\"},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\"},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB2\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "BContract";
    String code1 = "6080604052610166806100136000396000f3006080604052600436106100325763ffffffff60e060020a6000350416630223024e8114610034578063a03fa7e314610055575b005b61003273ffffffffffffffffffffffffffffffffffffffff60043516610076565b61003273ffffffffffffffffffffffffffffffffffffffff600435166100f7565b8073ffffffffffffffffffffffffffffffffffffffff16600560405180807f73657449282900000000000000000000000000000000000000000000000000008152506006019050604051809103902060e060020a9004906040518263ffffffff1660e060020a02815260040160006040518083038185885af1505050505050565b60405173ffffffffffffffffffffffffffffffffffffffff82169060009060059082818181858883f19350505050158015610136573d6000803e3d6000fd5b50505600a165627a7a72305820ede28ac9884104396c5d52bbf3f480cb637f61bc331c2dc561670e6d2700ad630029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"setIinC\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName2 = "CContract";
    String code2 = "6080604052610172806100136000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166367e404ce8114610087578063938b5f32146100df575b6040805132815233602082015281517fdaf0d4aa9a5679e832ac921da67b43572b4326ee2565442d3ed255b48cfb5161929181900390910190a1005b34801561009357600080fd5b50d380156100a057600080fd5b50d280156100ad57600080fd5b506100b661010e565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b3480156100eb57600080fd5b50d380156100f857600080fd5b50d2801561010557600080fd5b506100b661012a565b60015473ffffffffffffffffffffffffffffffffffffffff1681565b60005473ffffffffffffffffffffffffffffffffffffffff16815600a165627a7a7230582084426e82a8fde9cefb0ae9f1561ce743354adada27d217c8614c28829eecbcda0029";

    String abi2 = "[{\"constant\":true,\"inputs\":[],\"name\":\"sender\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"origin\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"\",\"type\":\"address\"}],\"name\":\"log\",\"type\":\"event\"}]";
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";

    String txid2 = "";
    txid2 = PublicMethed.triggerContract(contractAddress,
        "sendToB2(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
//    List<String> retList = getStrings(infoById2.get().getLog(0).getData().toByteArray());
//    byte[] originAddres = retList.get(0).getBytes();
//    byte[] senderAddres = retList.get(1).getBytes();
//    logger.info("originAddres:" + ByteArray.toHexString(originAddres));
//    logger.info("senderAddres:" + ByteArray.toHexString(senderAddres));
//
//    byte[] originAddres1 = subByte(originAddres, 12, 20);
//    byte[] senderAddres2 = subByte(senderAddres, 12, 20);
//    String exceptedoriginResult = "41" + ByteArray.toHexString(originAddres1);
//    String exceptedsenderResult = "41" + ByteArray.toHexString(senderAddres2);
//    logger.info("exceptedoriginResult:" + exceptedoriginResult);
//    logger.info("exceptedsenderResult:" + exceptedsenderResult);
//
//    Assert.assertEquals(exceptedoriginResult, ByteArray.toHexString(internalTxsAddress));
//    Assert.assertEquals(exceptedsenderResult, ByteArray.toHexString(contractAddress1));

  }

  @Test(enabled = true)
  public void testOriginDelegatecall001() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName = "AAContract";
    String code = "6080604052610264806100136000396000f30060806040526004361061003d5763ffffffff60e060020a600035041663648efe8b811461003f578063b45f578b14610059578063d818452114610073575b005b61003d600160a060020a036004358116906024351661008d565b61003d600160a060020a036004358116906024351661011c565b61003d600160a060020a03600435811690602435166101a9565b81600160a060020a031660405180807f7472616e73666572546f286164647265737329000000000000000000000000008152506013019050604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051808303816000875af2505050505050565b81600160a060020a031660405180807f7472616e73666572546f286164647265737329000000000000000000000000008152506013019050604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a03168152602001915050600060405180830381865af4505050505050565b81600160a060020a031660405180807f7472616e73666572546f286164647265737329000000000000000000000000008152506013019050604051809103902060e060020a9004826040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051808303816000875af15050505050505600a165627a7a7230582068a85b5cb5a41f10a7ba8250baed5adf37129ff04399bccae69e483fc85448a90029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\"},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB3\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\"},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"called_address\",\"type\":\"address\"},{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"sendToB2\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "BContract";
    String code1 = "6080604052610166806100136000396000f3006080604052600436106100325763ffffffff60e060020a6000350416630223024e8114610034578063a03fa7e314610055575b005b61003273ffffffffffffffffffffffffffffffffffffffff60043516610076565b61003273ffffffffffffffffffffffffffffffffffffffff600435166100f7565b8073ffffffffffffffffffffffffffffffffffffffff16600560405180807f73657449282900000000000000000000000000000000000000000000000000008152506006019050604051809103902060e060020a9004906040518263ffffffff1660e060020a02815260040160006040518083038185885af1505050505050565b60405173ffffffffffffffffffffffffffffffffffffffff82169060009060059082818181858883f19350505050158015610136573d6000803e3d6000fd5b50505600a165627a7a72305820ede28ac9884104396c5d52bbf3f480cb637f61bc331c2dc561670e6d2700ad630029";
    String abi1 = "[{\"constant\":false,\"inputs\":[{\"name\":\"c\",\"type\":\"address\"}],\"name\":\"setIinC\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    byte[] contractAddress1 = PublicMethed
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName2 = "CContract";
    String code2 = "6080604052610172806100136000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166367e404ce8114610087578063938b5f32146100df575b6040805132815233602082015281517fdaf0d4aa9a5679e832ac921da67b43572b4326ee2565442d3ed255b48cfb5161929181900390910190a1005b34801561009357600080fd5b50d380156100a057600080fd5b50d280156100ad57600080fd5b506100b661010e565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b3480156100eb57600080fd5b50d380156100f857600080fd5b50d2801561010557600080fd5b506100b661012a565b60015473ffffffffffffffffffffffffffffffffffffffff1681565b60005473ffffffffffffffffffffffffffffffffffffffff16815600a165627a7a7230582084426e82a8fde9cefb0ae9f1561ce743354adada27d217c8614c28829eecbcda0029";

    String abi2 = "[{\"constant\":true,\"inputs\":[],\"name\":\"sender\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"origin\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"address\"},{\"indexed\":false,\"name\":\"\",\"type\":\"address\"}],\"name\":\"log\",\"type\":\"event\"}]";
    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";

    String txid2 = "";
    txid2 = PublicMethed.triggerContract(contractAddress,
        "sendToB(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
//    List<String> retList = getStrings(infoById2.get().getLog(0).getData().toByteArray());
//    byte[] originAddres = retList.get(0).getBytes();
//    byte[] senderAddres = retList.get(1).getBytes();
//    logger.info("originAddres:" + ByteArray.toHexString(originAddres));
//    logger.info("senderAddres:" + ByteArray.toHexString(senderAddres));
//
//    byte[] originAddres1 = subByte(originAddres, 12, 20);
//    byte[] senderAddres2 = subByte(senderAddres, 12, 20);
//    String exceptedoriginResult = "41" + ByteArray.toHexString(originAddres1);
//    String exceptedsenderResult = "41" + ByteArray.toHexString(senderAddres2);
//    logger.info("exceptedoriginResult:" + exceptedoriginResult);
//    logger.info("exceptedsenderResult:" + exceptedsenderResult);
//
//    Assert.assertEquals(exceptedoriginResult, ByteArray.toHexString(internalTxsAddress));
//    Assert.assertEquals(exceptedsenderResult, ByteArray.toHexString(contractAddress1));

  }

  private List<String> getStrings(byte[] data) {
    int index = 0;
    List<String> ret = new ArrayList<>();
    while (index < data.length) {
      ret.add(byte2HexStr(data, index, 32));
      index += 32;
    }
    return ret;
  }

  public static String byte2HexStr(byte[] b, int offset, int length) {
    String stmp = "";
    StringBuilder sb = new StringBuilder("");
    for (int n = offset; n < offset + length && n < b.length; n++) {
      stmp = Integer.toHexString(b[n] & 0xFF);
      sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
    }
    return sb.toString().toUpperCase().trim();
  }

  public byte[] subByte(byte[] b, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;

  }
}
