package org.tron.core.capsule;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.common.runtime.vm.program.InternalTransaction;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ExchangeCreateContractInfo;
import org.tron.protos.Protocol.SmartContractInfo;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.Protocol.TransactionInfoV2;
import org.tron.protos.Protocol.TransactionInfoV2.code;

public class TransactionInfoV2Capsule implements ProtoCapsule<TransactionInfoV2> {

  private TransactionInfoV2 instance;

  public TransactionInfoV2Capsule(TransactionInfoV2 transactionInfoV2) {
    this.instance = transactionInfoV2;
  }

  public TransactionInfoV2Capsule(byte[] data) throws BadItemException {
    try {
      this.instance = TransactionInfoV2.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("TransactionInfoV2Capsule proto data parse exception");
    }
  }

  // can not create a empty object
  private TransactionInfoV2Capsule() {
  }

  public static TransactionInfoV2Capsule buildInstance(TransactionCapsule trxCap, BlockCapsule block,
      TransactionTrace trace) {
    TransactionInfoV2.Builder transactionInfoBuilder = TransactionInfoV2.newBuilder();

    // common part
    if (Objects.nonNull(block)) {
      transactionInfoBuilder.setBlockNumber(block.getInstance().getBlockHeader().getRawData().getNumber());
      transactionInfoBuilder.setBlockTimestamp(block.getInstance().getBlockHeader().getRawData().getTimestamp());
    }
    transactionInfoBuilder.setReceipt(trace.getReceipt().getReceipt());
    transactionInfoBuilder.setResult(code.SUCESS);
    if (StringUtils.isNoneEmpty(trace.getRuntimeError()) || Objects
        .nonNull(trace.getRuntimeResult().getException())) {
      transactionInfoBuilder.setResult(code.FAILED);
      transactionInfoBuilder.setMessage(ByteString.copyFromUtf8(trace.getRuntimeError()));
    }
    transactionInfoBuilder.setId(ByteString.copyFrom(trxCap.getTransactionId().getBytes()));

    // contract private part
    Transaction.Contract contract = trxCap.getInstance().getRawData().getContract(0);
    ProgramResult programResult = trace.getRuntimeResult();

    switch (contract.getType()) {
      case TriggerSmartContract:
      case CreateSmartContract: {
        ReceiptCapsule traceReceipt = trace.getReceipt();

        SmartContractInfo.Builder infoBuilder = SmartContractInfo.newBuilder();

        long fee =
            programResult.getRet().getFee() + traceReceipt.getEnergyFee() + traceReceipt
                .getNetFee();
        ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
        ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

        infoBuilder.setFee(fee);
        infoBuilder.addContractResult(contractResult);
        infoBuilder.setContractAddress(contractAddress);
        infoBuilder.setWithdrawAmount(programResult.getRet().getWithdrawAmount());
        infoBuilder.setUnfreezeAmount(programResult.getRet().getUnfreezeAmount());
        infoBuilder.setAssetIssueID(programResult.getRet().getAssetIssueID());

        List<SmartContractInfo.Log> logList = new ArrayList<>();
        programResult.getLogInfoList().forEach(
            info -> logList.add(LogInfo.buildLogV2(info)));
        infoBuilder.addAllLog(logList);

        if (Args.getInstance().isSaveInternalTx() && null != programResult.getInternalTransactions()) {
          for (InternalTransaction internalTransaction : programResult
              .getInternalTransactions()) {
            Protocol.InternalTransaction.Builder internalTrxBuilder = Protocol.InternalTransaction
                .newBuilder();
            // set hash
            internalTrxBuilder.setHash(ByteString.copyFrom(internalTransaction.getHash()));
            // set caller
            internalTrxBuilder.setCallerAddress(ByteString.copyFrom(internalTransaction.getSender()));
            // set TransferTo
            internalTrxBuilder
                .setTransferToAddress(ByteString.copyFrom(internalTransaction.getTransferToAddress()));
            //TODO: "for loop" below in future for multiple token case, we only have one for now.
            Protocol.InternalTransaction.CallValueInfo.Builder callValueInfoBuilder =
                Protocol.InternalTransaction.CallValueInfo.newBuilder();
            // trx will not be set token name
            callValueInfoBuilder.setCallValue(internalTransaction.getValue());
            // Just one transferBuilder for now.
            internalTrxBuilder.addCallValueInfo(callValueInfoBuilder);
            internalTransaction.getTokenInfo().forEach((tokenId, amount) -> {
              Protocol.InternalTransaction.CallValueInfo.Builder tokenInfoBuilder =
                  Protocol.InternalTransaction.CallValueInfo.newBuilder();
              tokenInfoBuilder.setTokenId(tokenId);
              tokenInfoBuilder.setCallValue(amount);
              internalTrxBuilder.addCallValueInfo(tokenInfoBuilder);
            });
            // Token for loop end here
            internalTrxBuilder.setNote(ByteString.copyFrom(internalTransaction.getNote().getBytes()));
            internalTrxBuilder.setRejected(internalTransaction.isRejected());
            infoBuilder.addInternalTransactions(internalTrxBuilder);
          }
        }

        transactionInfoBuilder.setInfo(Any.pack(infoBuilder.build()));

        break;
      }
      case ExchangeCreateContract:
      case ExchangeTransactionContract:
      case ExchangeInjectContract:
      case ExchangeWithdrawContract: {
        ExchangeCreateContractInfo.Builder infoBuilder = ExchangeCreateContractInfo.newBuilder();
        infoBuilder.setExchangeId(programResult.getRet().getExchangeId());
        infoBuilder.setExchangeReceivedAmount(programResult.getRet().getExchangeReceivedAmount());
        infoBuilder.setExchangeInjectAnotherAmount(
            programResult.getRet().getExchangeInjectAnotherAmount());
        infoBuilder.setExchangeWithdrawAnotherAmount(
            programResult.getRet().getExchangeWithdrawAnotherAmount());

        transactionInfoBuilder.setInfo(Any.pack(infoBuilder.build()));

        break;
      }
      default:
    }

    return new TransactionInfoV2Capsule(transactionInfoBuilder.build());

  }

  @Override
  public byte[] getData() {
    return this.instance.toByteArray();
  }

  @Override
  public TransactionInfoV2 getInstance() {
    return instance;
  }
}
