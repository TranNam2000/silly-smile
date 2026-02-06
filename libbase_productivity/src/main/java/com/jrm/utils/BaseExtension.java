package com.jrm.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import com.jrm.utils.Logger;

import com.jrm.base.BaseEventLogger;

import java.util.Locale;

public class BaseExtension {
    public static void showActivity(Context context, Class<?> activity, Bundle bundle) {
        Intent intent = new Intent(context, activity);
        intent.putExtras(bundle != null ? bundle : new Bundle());
        context.startActivity(intent);
    }

    public static void showActivityWithAd(Activity context, Class<?> activity, Bundle bundle) {
        AdsHelper.showInterPreload(context, BaseEventLogger.getCurrentScreen(), new Runnable() {
            public void run() {
                Intent intent = new Intent(context, activity);
                intent.putExtras(bundle != null ? bundle : new Bundle());
                context.startActivity(intent);
            }
        });
    }

    public static void showActivity(Context context, Class<?> activity, Bundle bundle, int delay) {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(context, activity);
            intent.putExtras(bundle != null ? bundle : new Bundle());
            context.startActivity(intent);
        }, delay); // Delay in milliseconds
    }

    public static String getLocalizedText(Context context, String localeId, int stringId) {
        try {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            Locale locale = new Locale(localeId);
            config.locale = locale;
            Context configContext = context.createConfigurationContext(config); // Corrected line
            Resources resources = configContext.getResources(); // Get resources from the config context
            return resources.getString(stringId);
        } catch (Resources.NotFoundException e) {
            Logger.e("String resource not found for locale: " + localeId + ", ID: " + stringId, e);
            return context.getString(stringId); // Fallback to default locale
        } catch (IllegalArgumentException e) {
            Logger.e("Invalid locale ID: " + localeId, e);
            return context.getString(stringId); // Fallback to default locale
        }
    }
}
