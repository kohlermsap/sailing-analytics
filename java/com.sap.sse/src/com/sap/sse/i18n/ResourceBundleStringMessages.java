package com.sap.sse.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import com.sap.sse.i18n.impl.NullResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;

/**
 * Allow server-side internationalization similar to GWT client-side by using property files.
 * 
 * Get Locale in GWT-Context by calling
 * 
 * <pre>
 * LocaleInfo.getCurrentLocale().getLocaleName();
 * </pre>
 * 
 * Then transform back to {@link Locale} on server by calling
 * 
 * <pre>
 * ResourceBundleStringMessages.Util.getLocaleFor(localeInfoName);
 * </pre>
 */
public interface ResourceBundleStringMessages {
    static final ResourceBundleStringMessages NULL = new NullResourceBundleStringMessages();
    
    static ResourceBundleStringMessages create(String resourceBaseName, ClassLoader resourceClassLoader, String encoding) {
        return new ResourceBundleStringMessagesImpl(resourceBaseName, resourceClassLoader, encoding);
    }
    
    static ResourceBundleStringMessages create(String resourceBaseName, ClassLoader resourceClassLoader) {
        return new ResourceBundleStringMessagesImpl(resourceBaseName, resourceClassLoader);
    }

    String getResourceBaseName();

    String get(Locale locale, String messageKey);

    String get(Locale locale, String messageKey, String... parameters);

    static final class Util {
        private static final Locale FALLBACK_LOCALE = Locale.ROOT;

        public static Control createControl(String encoding) {
            return new Control() {
                @Override
                public Locale getFallbackLocale(String baseName, Locale locale) {
                    return locale.equals(FALLBACK_LOCALE) ? null : FALLBACK_LOCALE;
                }

                @Override
                public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                        boolean reload) throws IllegalAccessException, InstantiationException, IOException {
                    String localeExt = "";
                    if (!locale.getLanguage().isEmpty()) {
                        localeExt = "_" + locale.getLanguage();
                    }
                    if (format.contains("class")) {
                        return null;
                    }
                    String classPathFilePath = baseName + localeExt + ".properties";
                    try (InputStream is = loader.getResourceAsStream(classPathFilePath);) {
                        if (is == null) {
                            return null;
                        }
                        return new PropertyResourceBundle(new InputStreamReader(is, encoding));
                    }
                }
            };

        }

        public static Locale getLocaleFor(String localeInfoName) {
            return Locale.forLanguageTag(localeInfoName);
        }

        private Util() {
        }
    }
}
