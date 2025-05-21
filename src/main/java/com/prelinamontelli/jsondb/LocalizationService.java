package com.prelinamontelli.jsondb;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ArrayList;

public class LocalizationService {
    private static final String BUNDLE_NAME = "messages";
    private Locale currentLocale;
    private ResourceBundle resourceBundle;

    // 支持的语言及其对应的Locale对象
    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
            Locale.ENGLISH, // en
            Locale.SIMPLIFIED_CHINESE // zh_CN (zh)
    );

    public LocalizationService() {
        // 默认使用系统区域设置，如果不支持，则回退到英文
        Locale systemLocale = Locale.getDefault();
        if (isLocaleSupported(systemLocale)) {
            this.currentLocale = systemLocale;
        } else {
            // 尝试仅匹配语言代码
            Locale langOnlyLocale = new Locale(systemLocale.getLanguage());
            if (isLocaleSupported(langOnlyLocale)){
                this.currentLocale = langOnlyLocale;
            } else {
                this.currentLocale = Locale.ENGLISH; // 默认回退到英文
            }
        }
        loadBundle();
    }

    private void loadBundle() {
        try {
            // ResourceBundle 会根据 currentLocale 自动选择 messages_en.properties 或 messages_zh_CN.properties 等
            this.resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Warning: Could not load resource bundle for locale " + currentLocale + ". Defaulting to system behavior (may be English or crash if no base bundle).");
            // 尝试加载基础包 (messages.properties) 或英文包作为最终回退
            try {
                this.resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT); // ROOT 通常是基础包
            } catch (MissingResourceException mre) {
                 System.err.println("Critical: Base resource bundle '"+BUNDLE_NAME+"' is missing. Application messages will not work.");
                 // 在这种情况下，getString 方法会抛出异常，或者我们可以让它返回键名
                 // 为简单起见，这里不进一步处理，依赖于调用者处理 MissingResourceException
                 throw mre; // 重新抛出，让应用知道本地化失败
            }
        }
    }

    public String getMessage(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("Warning: Missing translation for key '" + key + "' in locale '" + currentLocale + "'. Returning key name.");
            return key; // 返回键名作为回退
        }
    }

    public String getMessage(String key, Object... args) {
        try {
            String pattern = resourceBundle.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            System.err.println("Warning: Missing translation for key '" + key + "' in locale '" + currentLocale + "'. Returning formatted key name.");
            //尝试用参数格式化键名，如果键名本身是格式字符串的话
            try{
                return MessageFormat.format(key, args);
            } catch(IllegalArgumentException iae){
                return key; // 如果键名不是有效的格式字符串，则只返回键名
            }
        }
    }

    public boolean setLanguage(String languageCode) {
        Locale requestedLocale = null;
        if ("en".equalsIgnoreCase(languageCode)) {
            requestedLocale = Locale.ENGLISH;
        } else if ("zh".equalsIgnoreCase(languageCode)) {
            requestedLocale = Locale.SIMPLIFIED_CHINESE;
        }

        if (requestedLocale != null && isLocaleSupported(requestedLocale)) {
            if (!this.currentLocale.equals(requestedLocale)) {
                this.currentLocale = requestedLocale;
                loadBundle();
            }
            return true;
        } else {
            return false;
        }
    }
    
    public Locale getCurrentLocale(){
        return this.currentLocale;
    }

    public static List<String> getSupportedLanguageCodes() {
        List<String> codes = new ArrayList<>();
        for (Locale locale : SUPPORTED_LOCALES) {
            codes.add(locale.getLanguage());
        }
        return codes;
    }

    private boolean isLocaleSupported(Locale locale) {
        // 完全匹配 (语言和国家/地区)
        if (SUPPORTED_LOCALES.contains(locale)) {
            return true;
        }
        // 仅匹配语言代码
        for (Locale supported : SUPPORTED_LOCALES) {
            if (supported.getLanguage().equals(locale.getLanguage())) {
                return true;
            }
        }
        return false;
    }
} 