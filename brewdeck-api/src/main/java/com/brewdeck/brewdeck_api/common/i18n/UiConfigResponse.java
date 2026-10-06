package com.brewdeck.brewdeck_api.common.i18n;

import java.util.List;

/** Public, user-independent UI settings the web server needs before anyone logs in. */
public record UiConfigResponse(List<String> languages) {}
