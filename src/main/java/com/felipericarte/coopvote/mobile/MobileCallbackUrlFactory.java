package com.felipericarte.coopvote.mobile;

import java.net.URI;
import org.springframework.stereotype.Component;

/**
 * Produz URLs absolutas de callback sem acoplar as telas a um ambiente específico.
 */
@Component
public class MobileCallbackUrlFactory {

    private static final String MOBILE_API_PATH = "/api/v1/mobile";

    private final URI callbackBaseUrl;

    public MobileCallbackUrlFactory(MobileProperties properties) {
        this.callbackBaseUrl = properties.callbackBaseUrl();
    }

    public String create(String relativePath) {
        if (relativePath == null || !relativePath.startsWith("/")) {
            throw new IllegalArgumentException("relativePath must start with '/'");
        }
        return callbackBaseUrl.resolve(MOBILE_API_PATH + relativePath).toString();
    }
}
