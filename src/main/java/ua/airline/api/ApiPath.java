package ua.airline.api;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

record ApiPath(String identity, boolean stats) {
    static ApiPath parse(HttpServletRequest request) {
        String prefix = request.getContextPath() + request.getServletPath();
        String path = request.getRequestURI().substring(prefix.length());
        if (path.isEmpty() || path.equals("/")) {
            return new ApiPath(null, false);
        }
        String segment = path.substring(1);
        if (segment.contains("/")) {
            throw ApiException.notFound();
        }
        // Split the raw URI before decoding, so an encoded slash stays part of the identity.
        String identity = URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8);
        return new ApiPath(identity, identity.equals("stats"));
    }

    boolean collection() {
        return identity == null;
    }

    void requireCollection() {
        if (!collection()) {
            throw ApiException.methodNotAllowed();
        }
    }

    String requireIdentity() {
        if (collection() || stats) {
            throw ApiException.methodNotAllowed();
        }
        return identity;
    }

    static String location(HttpServletRequest request, String identity) {
        return request.getContextPath() + request.getServletPath() + "/"
                + URLEncoder.encode(identity, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
