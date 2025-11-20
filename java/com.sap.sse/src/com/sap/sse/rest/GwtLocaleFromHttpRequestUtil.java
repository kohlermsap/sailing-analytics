package com.sap.sse.rest;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

public class GwtLocaleFromHttpRequestUtil {
    public static Optional<String> getLocaleFromHttpRequest(HttpServletRequest httpServletRequest) {
        Optional<String> locale = Optional.empty();
        final Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
        if (parameterMap != null && parameterMap.containsKey("locale")) {
            locale = Optional.of(parameterMap.get("locale")[0]);
        } else {
            if (httpServletRequest.getCookies() != null) {
                for (final Cookie cookie : httpServletRequest.getCookies()) {
                    if (cookie.getName().equals("GWT_LOCALE")) {
                        locale = Optional.of(cookie.getValue());
                        break;
                    }
                }
            }
            if (!locale.isPresent()) {
                final String acceptLanguageHeader = httpServletRequest.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
                if (acceptLanguageHeader != null) {
                    locale = Optional.of(getFirstFromAcceptLanguage(acceptLanguageHeader));
                }
            }
        }
        return locale;
    }
    
    private static String getFirstFromAcceptLanguage(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null) return null;
        String[] langs = acceptLanguageHeader.split(",");
        final String result;
        if (langs.length > 0) {
            result = langs[0].split(";")[0].trim();
        } else {
            result = null;
        }
        return result;
    }
}
