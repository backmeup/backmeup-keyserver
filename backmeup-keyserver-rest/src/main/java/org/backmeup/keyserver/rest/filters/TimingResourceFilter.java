package org.backmeup.keyserver.rest.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Timing filter on resources. Monitors how long it takes to process request.
 */
@Provider
public class TimingResourceFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimingResourceFilter.class);
    private static final TimerThreadLocal TIMER = new TimerThreadLocal();

    @Override
    public void filter(ContainerRequestContext request) {
        TIMER.start();
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        try {
            long reqProcessingTimeInMs = TIMER.stop();
            LOGGER.info("Request processing time: " + reqProcessingTimeInMs + "ms");
        } finally {
            TIMER.remove();
        }
    }

}

final class TimerThreadLocal extends ThreadLocal<Long> {
    public long start() {
        long value = currentTimeMillis();
        this.set(value);
        return value;
    }

    public long stop() {
        return currentTimeMillis() - get();
    }

    @Override
    protected Long initialValue() {
        return currentTimeMillis();
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
