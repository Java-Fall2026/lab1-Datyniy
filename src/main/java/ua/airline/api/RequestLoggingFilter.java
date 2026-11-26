package ua.airline.api;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class RequestLoggingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long started = System.nanoTime();
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            String path = req.getRequestURI();
            if (req.getQueryString() != null) {
                path += "?" + req.getQueryString();
            }
            int status = resp.getStatus();
            if (status >= 500) {
                log.error("{} {} -> {} ({} ms)", req.getMethod(), path, status, elapsed);
            } else if (status >= 400) {
                log.warn("{} {} -> {} ({} ms)", req.getMethod(), path, status, elapsed);
            } else {
                log.info("{} {} -> {} ({} ms)", req.getMethod(), path, status, elapsed);
            }
        }
    }
}
