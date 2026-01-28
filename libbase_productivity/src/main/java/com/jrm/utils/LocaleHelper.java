package com.jrm.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;


public class LocaleHelper {
    public static Context setLocale(Context context, String language){
        return updateResources(context, language);
    }

    private static Context updateResources(Context contextMain, String language) {
        Context context = contextMain;
        Locale locale;
        locale = new Locale(language);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        context = context.createConfigurationContext(config);
        return context;
    }

    // Method to handle language change.
    private void changeLang(String lang, Context context) {
        if (lang == null || lang.isEmpty()) return;
        Locale myLocale = new Locale(lang);
        Locale.setDefault(myLocale);
        Configuration config = new Configuration();
        config.locale = myLocale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}