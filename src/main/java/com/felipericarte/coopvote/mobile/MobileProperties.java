package com.felipericarte.coopvote.mobile;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do domínio usado nos callbacks enviados ao aplicativo mobile.
 */
@ConfigurationProperties("coopvote.mobile")
public record MobileProperties(URI callbackBaseUrl) {

    public MobileProperties {
        if (callbackBaseUrl == null
                || callbackBaseUrl.getHost() == null
                || !("http".equalsIgnoreCase(callbackBaseUrl.getScheme())
                        || "https".equalsIgnoreCase(callbackBaseUrl.getScheme()))) {
            throw new IllegalArgumentException("callbackBaseUrl must be an absolute HTTP(S) URL");
        }
    }
}
