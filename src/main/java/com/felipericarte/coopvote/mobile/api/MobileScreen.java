package com.felipericarte.coopvote.mobile.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Mensagem interpretada pelo cliente para montar uma tela mobile.
 */
@Schema(oneOf = {FormScreen.class, SelectionScreen.class})
public sealed interface MobileScreen permits FormScreen, SelectionScreen {
}
