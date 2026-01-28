package com.jrm.onboarding.language;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.jrm.R;
import com.jrm.utils.remote_config.RemoteConfigManager;
public class LanguageModel {
    private int flagImg;
    private String langCode, langName;

    public LanguageModel(int flagImg, String langCode, String langName) {
        this.flagImg = flagImg;
        this.langCode = langCode;
        this.langName = langName;
    }

    public int getFlagImg() {
        return flagImg;
    }
    
    public boolean isValidFlagResource() {
        return flagImg != 0;
    }

    public void setFlagImg(int flagImg) {
        this.flagImg = flagImg;
    }

    public String getLangCode() {
        return langCode;
    }

    public void setLangCode(String langCode) {
        this.langCode = langCode;
    }

    public String getLangName() {
        return langName;
    }

    public void setLangName(String langName) {
        this.langName = langName;
    }

    public static List<LanguageModel> getAllLangData() {
        List<LanguageModel> defaultLanguages = Arrays.asList(
                new LanguageModel(R.drawable.flag_us, "en", "English"),
                new LanguageModel(R.drawable.flag_es, "es", "Español"),
                new LanguageModel(R.drawable.flag_pt, "pt", "Português"),
                new LanguageModel(R.drawable.hindi_flag, "hi", "हिंदी"),
                new LanguageModel(R.drawable.flag_sa, "ar", "العربية"),
                new LanguageModel(R.drawable.flag_vi, "vi", "Tiếng Việt"),
                new LanguageModel(R.drawable.flag_fr, "fr", "Français"),
                new LanguageModel(R.drawable.flag_ja, "ja", "日本語"),
                new LanguageModel(R.drawable.flag_ko, "ko", "한국인"),
                new LanguageModel(R.drawable.flag_it, "it", "Italiano"),
                new LanguageModel(R.drawable.german, "de", "Deutsch"),
                new LanguageModel(R.drawable.flag_nl, "nl", "Nederlands"),
                new LanguageModel(R.drawable.flag_pl, "pl", "Polski"),
                new LanguageModel(R.drawable.flag_ru, "ru", "Русский")
        );

        // Get remote config order
        RemoteConfigManager remoteConfigManager = RemoteConfigManager.getInstance();
        if (remoteConfigManager != null) {
            List<String> remoteOrder = remoteConfigManager.getLanguageOrderList();
            
            if (!remoteOrder.isEmpty()) {
                return sortLanguagesByRemoteOrder(defaultLanguages, remoteOrder);
            }
        }
        
        return defaultLanguages;
    }

    private static List<LanguageModel> sortLanguagesByRemoteOrder(List<LanguageModel> languages, List<String> order) {
        // Create a map for quick lookup
        Map<String, LanguageModel> languageMap = new HashMap<>();
        for (LanguageModel lang : languages) {
            languageMap.put(lang.getLangCode(), lang);
        }
        
        List<LanguageModel> sortedLanguages = new ArrayList<>();
        
        // Add languages in the order specified by remote config
        for (String langCode : order) {
            LanguageModel lang = languageMap.get(langCode);
            if (lang != null) {
                sortedLanguages.add(lang);
                languageMap.remove(langCode); // Remove to avoid duplicates
            }
        }
        
        // Add remaining languages that weren't in the remote order
        for (LanguageModel lang : languages) {
            if (languageMap.containsKey(lang.getLangCode())) {
                sortedLanguages.add(lang);
            }
        }
        
        return sortedLanguages;
    }

    public static LanguageModel getLanguageByCode(String langcode) {
        List<LanguageModel> Languages = getAllLangData();
        for (int i = 0; i < Languages.size(); i++) {
            LanguageModel lang = Languages.get(i);
            if (lang.getLangCode().equals(langcode)) {
                return lang;
            }
        }
        return Languages.get(0);
    }
}