package com.felipericarte.coopvote.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import org.junit.jupiter.api.Test;

class MobilePropertiesTest {

    @Test
    void shouldBuildAbsoluteCallbackFromConfiguredDomain() {
        MobileProperties properties = new MobileProperties(URI.create("https://mobile.example:8443"));
        MobileCallbackUrlFactory factory = new MobileCallbackUrlFactory(properties);

        assertThat(factory.create("/agendas/new"))
                .isEqualTo("https://mobile.example:8443/api/v1/mobile/agendas/new");
    }

    @Test
    void shouldRejectInvalidCallbackConfiguration() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MobileProperties(null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MobileProperties(URI.create("ftp://mobile.example")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MobileProperties(URI.create("/relative")));
    }

    @Test
    void shouldRejectCallbackPathWithoutLeadingSlash() {
        MobileCallbackUrlFactory factory = new MobileCallbackUrlFactory(
                new MobileProperties(URI.create("http://localhost:8080")));

        assertThatIllegalArgumentException().isThrownBy(() -> factory.create("agendas"));
        assertThatIllegalArgumentException().isThrownBy(() -> factory.create(null));
    }
}
