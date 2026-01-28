package com.jrm.onboarding.onboarding;

import android.content.Context;

import com.jrm.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OnboardingModel {
    private String title, subTitle;
    private int image;
    private Context context;
    public OnboardingModel() {
    }

    public OnboardingModel(String title, int image) {
        this.title = title;
        this.image = image;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public OnboardingModel(String title, String subTitle, int image) {
        this.title = title;
        this.subTitle = subTitle;
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public static List<OnboardingModel> getAllItemData(Context context) {
        return new ArrayList<>(Arrays.asList(
                new OnboardingModel(context.getString(R.string.obd_title1),context.getString(R.string.obd_detail1), R.drawable.obd1),
                new OnboardingModel(context.getString(R.string.obd_title2),context.getString(R.string.obd_detail2), R.drawable.obd2),
                new OnboardingModel(context.getString(R.string.obd_title3),context.getString(R.string.obd_detail3), R.drawable.obd3)
        ));
    }
}