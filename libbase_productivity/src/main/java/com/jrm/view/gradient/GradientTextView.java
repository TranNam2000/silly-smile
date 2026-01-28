package com.jrm.view.gradient;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.jrm.R;

public class GradientTextView extends AppCompatTextView {

    private int startColor;
    private int endColor;
    private int centerColor;
    private int centerColor2;
    private int angle;
    private boolean isGradientEnabled = true;

    public GradientTextView(Context context) {
        super(context);
        init(null);
    }

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public GradientTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            try (TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GradientTextView)) {
                startColor = a.getColor(R.styleable.GradientTextView_startColor, Color.BLACK);
                endColor = a.getColor(R.styleable.GradientTextView_endColor, Color.BLACK);
                centerColor = a.getColor(R.styleable.GradientTextView_centerColor, 0);
                centerColor2 = a.getColor(R.styleable.GradientTextView_centerColor2, 0);
                angle = a.getInt(R.styleable.GradientTextView_angle, 0);
                isGradientEnabled = a.getBoolean(R.styleable.GradientTextView_enableGradient, true);
            }
        }
    }

    public void turnOfGradient(int colorRes) {
        isGradientEnabled = false;
        getPaint().setShader(null);
        setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }

    public void turnOnGradient() {
        isGradientEnabled = true;
        requestLayout();
    }

    /**
     * Cập nhật màu gradient (2 màu)
     */
    public void setGradientColors(int startColor, int endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.centerColor = 0;
        this.centerColor2 = 0;
        if (isGradientEnabled) {
            requestLayout();
        }
    }

    /**
     * Cập nhật màu gradient (3 màu)
     */
    public void setGradientColors(int startColor, int centerColor, int endColor) {
        this.startColor = startColor;
        this.centerColor = centerColor;
        this.endColor = endColor;
        this.centerColor2 = 0;
        if (isGradientEnabled) {
            requestLayout();
        }
    }

    /**
     * Cập nhật màu gradient (4 màu)
     */
    public void setGradientColors(int startColor, int centerColor, int centerColor2, int endColor) {
        this.startColor = startColor;
        this.centerColor = centerColor;
        this.centerColor2 = centerColor2;
        this.endColor = endColor;
        if (isGradientEnabled) {
            requestLayout();
        }
    }

    /**
     * Cập nhật góc của gradient
     */
    public void setGradientAngle(int angle) {
        this.angle = angle;
        if (isGradientEnabled) {
            requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (isGradientEnabled && (changed || getPaint().getShader() == null)) {
            getPaint().setShader(getGradient());
        }
    }

    private Shader getGradient() {
        float x0 = 0, y0 = 0, x1 = 0, y1 = 0;
        switch (angle) {
            case 0:
                x0 = 0;
                y0 = 0;
                x1 = getWidth();
                y1 = 0;
                break;
            case 45:
                x0 = 0;
                y0 = getHeight();
                x1 = getWidth();
                y1 = 0;
                break;
            case 90:
                x0 = 0;
                y0 = 0;
                x1 = 0;
                y1 = getHeight();
                break;
            case 135:
                x0 = getWidth();
                y0 = getHeight();
                x1 = 0;
                y1 = 0;
                break;
            case 180:
                x0 = getWidth();
                y0 = 0;
                x1 = 0;
                y1 = 0;
                break;
            case 225:
                x0 = getWidth();
                y0 = 0;
                x1 = 0;
                y1 = getHeight();
                break;
            case 270:
                x0 = 0;
                y0 = getHeight();
                x1 = 0;
                y1 = 0;
                break;
            case 315:
                x0 = 0;
                y0 = 0;
                x1 = getWidth();
                y1 = getHeight();
                break;
        }

        int[] colors;
        if (centerColor != 0 && centerColor2 != 0) {
            // 4 colors gradient
            colors = new int[]{startColor, centerColor, centerColor2, endColor};
        } else if (centerColor != 0) {
            // 3 colors gradient
            colors = new int[]{startColor, centerColor, endColor};
        } else {
            // 2 colors gradient
            colors = new int[]{startColor, endColor};
        }

        return new LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP);
    }

    public int getStartColor() {
        return startColor;
    }

    public int getEndColor() {
        return endColor;
    }

    public int getCenterColor() {
        return centerColor;
    }

    public int getCenterColor2() {
        return centerColor2;
    }

    public int getAngle() {
        return angle;
    }

    public boolean isGradientEnabled() {
        return isGradientEnabled;
    }
}
