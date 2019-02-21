package org.tron.core.services.interceptor;

import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "interceptor")
public class GrpcInterceptor implements ServerInterceptor {
  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {
    String socketString = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString();
    logger.info("### @@ {}: {}", call.getMethodDescriptor().getFullMethodName(), socketString);
    return next.startCall(call, headers);
  }
}