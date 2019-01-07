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
public class ContractTrcToken076 {


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

  @Test(enabled = true)
  public void testOrigin001() {
    PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "originTest";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b506103698061003a6000396000f3006080604052600436106100825763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166317ca5481811461008757806360d10813146100ca57806367e404ce146100f95780638be988cd14610151578063938b5f3214610159578063e8da042414610188578063f8a8fd6d14610190575b600080fd5b34801561009357600080fd5b50d380156100a057600080fd5b50d280156100ad57600080fd5b506100b66101c1565b604080519115158252519081900360200190f35b3480156100d657600080fd5b50d380156100e357600080fd5b50d280156100f057600080fd5b506100b66101e3565b34801561010557600080fd5b50d3801561011257600080fd5b50d2801561011f57600080fd5b50610128610204565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b6100b6610220565b34801561016557600080fd5b50d3801561017257600080fd5b50d2801561017f57600080fd5b50610128610241565b6100b661025d565b34801561019c57600080fd5b50d380156101a957600080fd5b50d280156101b657600080fd5b506101bf61027f565b005b6001547501000000000000000000000000000000000000000000900460ff1681565b60015474010000000000000000000000000000000000000000900460ff1681565b60015473ffffffffffffffffffffffffffffffffffffffff1681565b60015474010000000000000000000000000000000000000000900460ff1690565b60005473ffffffffffffffffffffffffffffffffffffffff1681565b6001547501000000000000000000000000000000000000000000900460ff1690565b600080543273ffffffffffffffffffffffffffffffffffffffff199182168117928390556001805475ff0000000000000000000000000000000000000000001993163390811774ff00000000000000000000000000000000000000001916921474010000000000000000000000000000000000000000029190911791821673ffffffffffffffffffffffffffffffffffffffff92831692909316919091147501000000000000000000000000000000000000000000029190911790555600a165627a7a72305820487fd902512ac719158b1ec219e9e6648b9f47f7f0396d24044f4dca1de4a3c60029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"result2\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"result1\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"sender\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getResult1\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"origin\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getResult2\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"test\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "getResult1()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    Long returnnumber = ByteArray.toLong(ByteArray.fromHexString(ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber == 0);

    txid = PublicMethed.triggerContract(contractAddress,
        "getResult2()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("-------------------------");

    logger.info("infoById:" + infoById);
    Long returnnumber2 = ByteArray.toLong(ByteArray.fromHexString(
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber2 == 0);
  }


}
