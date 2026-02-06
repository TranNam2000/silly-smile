package com.jrm.onboarding.language;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import com.jrm.R;
import com.jrm.utils.SharedPref;

import java.util.List;
public class LanguageAdapter extends ArrayAdapter<LanguageModel> {
    SharedPref pref ;
    private String savedLang;

    public String getSavedLang() {
        return savedLang;
    }

    public void setSavedLang(String savedLang) {
        this.savedLang = savedLang;
    }

    private View savedLangItem;

    public View getSavedLangItem() {
        return savedLangItem;
    }

    public void setSavedLangItem(View savedLangItem) {
        this.savedLangItem = savedLangItem;
    }

    public LanguageAdapter(@NonNull Context context, @NonNull List<LanguageModel> dataArrayList, String savedLang) {
        super(context, R.layout.item_language_button, dataArrayList);
        this.savedLang = savedLang;
    }
    
    /**
     * Get the position of the saved language in the list
     * @return Position of saved language, or -1 if not found
     */
    public int getSavedLanguagePosition() {
        if (savedLang == null || savedLang.isEmpty()) {
            return -1;
        }
        
        for (int i = 0; i < getCount(); i++) {
            LanguageModel item = getItem(i);
            if (item != null && item.getLangCode().equals(savedLang)) {
                return i;
            }
        }
        return -1;
    }
    // Create a static class that holds the references to the views in your layout
    static private class ViewHolder {
        ImageView langImage;
        TextView langName, langCode;
        RadioButton langRdo;
        MaterialCardView buttonBound;
        com.airbnb.lottie.LottieAnimationView guide;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LanguageModel languageData = getItem(position);

        // Declare a ViewHolder object
        ViewHolder holder;

        // Check if the convertView is null
        if (convertView == null) {
            // If it is null, inflate a new view from the layout
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_language_button, parent, false);

            // Create a new ViewHolder object and assign the views to it
            holder = new ViewHolder();
            holder.langImage = convertView.findViewById(R.id.lang_flag);
            holder.langName = convertView.findViewById(R.id.lang_name);
            holder.langRdo = convertView.findViewById(R.id.lang_rdo);
            holder.buttonBound = convertView.findViewById(R.id.button_bound);
            holder.langCode = convertView.findViewById(R.id.lang_code);
            holder.guide = convertView.findViewById(R.id.guide);

            // Set the holder object as a tag for the view
            convertView.setTag(holder);
        } else {
            // If it is not null, get the holder object from the tag
            holder = (ViewHolder) convertView.getTag();
        }

        // Set the image and name for the views using the holder object
        assert languageData != null;
        
        // Safely set image resource with validation
        try {
            int flagResource = languageData.getFlagImg();
            if (flagResource != 0) {
                holder.langImage.setImageResource(flagResource);
            } else {
                // Use a default flag if resource is invalid
                holder.langImage.setImageResource(R.drawable.flag_us);
            }
        } catch (Exception e) {
            // Log error and use default flag
            com.jrm.utils.Logger.e("Error setting flag image for: " + languageData.getLangCode(), e);
            holder.langImage.setImageResource(R.drawable.flag_us);
        }
        
        holder.langName.setText(languageData.getLangName());
        holder.langCode.setText(languageData.getLangCode());

        if(LanguageModel.getAllLangData().get(position).getLangCode().equals(savedLang)) {
            setActiveButton(convertView);
            savedLangItem = convertView;
        }
        else {
            setDeActiveButton(convertView);
        }

        // Show guide animation for the 4th item (position 3) only in LanguageActivity
        holder.guide.setVisibility(View.GONE);
//        if (position == 0 && getContext() instanceof LanguageActivity) {
//            holder.guide.setVisibility(View.VISIBLE);
//        } else {
//            holder.guide.setVisibility(View.GONE);
//        }

        // Return the view
        return convertView;
    }

    public void setActiveButton(View view){
        if(view != null) {
            ((RadioButton)view.findViewById(R.id.lang_rdo)).setChecked(true);
            view.findViewById(R.id.button_background).setBackgroundColor(getContext().getResources().getColor(R.color.theme_color_secondary));
        }
    }

    public void setDeActiveButton(View view){
        if(view !=null){
            ((RadioButton)view.findViewById(R.id.lang_rdo)).setChecked(false);
            view.findViewById(R.id.button_background).setBackgroundColor(getContext().getResources().getColor(R.color.theme_color_tertiary));
        }
    }
}