package org.acme.backend.interceptor;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Prioritized;
import org.jboss.logging.Logger;

/**
 * Global gRPC server interceptor for request/response logging.
 * Implements Prioritized to ensure execution order (lower priority = earlier execution).
 */
@ApplicationScoped
@GlobalInterceptor
public class LoggingInterceptor implements ServerInterceptor, Prioritized {

    private static final Logger LOG = Logger.getLogger(LoggingInterceptor.class);

    // Priority: execute early to capture accurate timing
    private static final int INTERCEPTOR_PRIORITY = 100;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {

        String fullMethodName = serverCall.getMethodDescriptor().getFullMethodName();
        long startTimeNanos = System.nanoTime();

        LOG.infof("▶️ gRPC Call Started: %s", fullMethodName);

        // Wrap the call to intercept close() for error logging
        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(serverCall) {
            @Override
            public void close(Status status, Metadata trailers) {
                if (!status.isOk()) {
                    LOG.warnf("⚠️ gRPC Call Failed: %s | Status: %s (%d) | Description: %s",
                            fullMethodName,
                            status.getCode(),
                            status.getCode().value(),
                            status.getDescription());
                }
                super.close(status, trailers);
            }
        };

        // Start the call and wrap the listener for lifecycle events
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(wrappedCall, metadata);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            @Override
            public void onMessage(ReqT message) {
                LOG.debugf("📨 Message Received [%s]: %s", fullMethodName, message);
                super.onMessage(message);
            }

            @Override
            public void onHalfClose() {
                LOG.debugf("⏸️ gRPC Call Half Closed: %s (client done sending)", fullMethodName);
                super.onHalfClose();
            }

            @Override
            public void onCancel() {
                long durationMillis = calculateDurationMillis(startTimeNanos);
                LOG.warnf("❌ gRPC Call Cancelled: %s after %dms", fullMethodName, durationMillis);
                super.onCancel();
            }

            @Override
            public void onComplete() {
                long durationMillis = calculateDurationMillis(startTimeNanos);
                LOG.infof("✅ gRPC Call Completed: %s in %dms", fullMethodName, durationMillis);
                super.onComplete();
            }
        };
    }

    /**
     * Calculates duration in milliseconds from nanoTime start point.
     * nanoTime is preferred over currentTimeMillis for duration calculations
     * as it's not affected by system clock changes.
     */
    private long calculateDurationMillis(long startTimeNanos) {
        return (System.nanoTime() - startTimeNanos) / 1_000_000;
    }

    @Override
    public int getPriority() {
        return INTERCEPTOR_PRIORITY;
    }
}
