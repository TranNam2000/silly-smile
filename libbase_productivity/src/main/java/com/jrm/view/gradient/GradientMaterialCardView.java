package com.jrm.view.gradient;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jrm.R;
import com.jrm.base.tracking.TrackableCardView;

public class GradientMaterialCardView extends TrackableCardView {

    private int startColor;
    private int endColor;
    private int centerColor;
    private int centerColor2;
    private int angle;
    private boolean isGradientEnabled = true;

    private Paint gradientPaint;
    private RectF gradientRect;

    public GradientMaterialCardView(@NonNull Context context) {
        super(context);
        init(null);
    }

    public GradientMaterialCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public GradientMaterialCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientRect = new RectF();

        // Đặt WillNotDraw thành false để có thể vẽ gradient
        setWillNotDraw(false);

        if (attrs != null) {
            try (TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GradientMaterialCardView)) {
                startColor = a.getColor(R.styleable.GradientMaterialCardView_cardStartColor,
                        Color.parseColor("#FF6B9D"));
                endColor = a.getColor(R.styleable.GradientMaterialCardView_cardEndColor, Color.parseColor("#C06C84"));
                centerColor = a.getColor(R.styleable.GradientMaterialCardView_cardCenterColor, 0);
                centerColor2 = a.getColor(R.styleable.GradientMaterialCardView_cardCenterColor2, 0);
                angle = a.getInt(R.styleable.GradientMaterialCardView_cardAngle, 0);
                isGradientEnabled = a.getBoolean(R.styleable.GradientMaterialCardView_enableCardGradient, true);
            }
        }
    }

    /**
     * Tắt gradient và sử dụng màu solid
     */
    public void turnOffGradient(int solidColor) {
        isGradientEnabled = false;
        setCardBackgroundColor(solidColor);
        invalidate();
    }

    /**
     * Bật gradient
     */
    public void turnOnGradient() {
        isGradientEnabled = true;
        invalidate();
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
            invalidate();
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
            invalidate();
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
            invalidate();
        }
    }

    /**
     * Cập nhật góc của gradient
     * Các góc hỗ trợ: 0, 45, 90, 135, 180, 225, 270, 315
     */
    public void setGradientAngle(int angle) {
        this.angle = angle;
        if (isGradientEnabled) {
            invalidate();
        }
    }

    /**
     * Cập nhật toàn bộ gradient (2 màu)
     */
    public void updateGradient(int startColor, int endColor, int angle) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.centerColor = 0;
        this.centerColor2 = 0;
        this.angle = angle;
        if (isGradientEnabled) {
            invalidate();
        }
    }

    /**
     * Cập nhật toàn bộ gradient (3 màu)
     */
    public void updateGradient(int startColor, int centerColor, int endColor, int angle) {
        this.startColor = startColor;
        this.centerColor = centerColor;
        this.endColor = endColor;
        this.centerColor2 = 0;
        this.angle = angle;
        if (isGradientEnabled) {
            invalidate();
        }
    }

    /**
     * Cập nhật toàn bộ gradient (4 màu)
     */
    public void updateGradient(int startColor, int centerColor, int centerColor2, int endColor, int angle) {
        this.startColor = startColor;
        this.centerColor = centerColor;
        this.centerColor2 = centerColor2;
        this.endColor = endColor;
        this.angle = angle;
        if (isGradientEnabled) {
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradientRect.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (isGradientEnabled && getWidth() > 0 && getHeight() > 0) {
            // Vẽ gradient background
            Shader shader = createGradientShader();
            gradientPaint.setShader(shader);

            // Vẽ với corner radius
            float cornerRadius = getRadius();
            canvas.drawRoundRect(gradientRect, cornerRadius, cornerRadius, gradientPaint);
        }

        super.onDraw(canvas);
    }

    private Shader createGradientShader() {
        float x0 = 0, y0 = 0, x1 = 0, y1 = 0;
        float width = getWidth();
        float height = getHeight();

        switch (angle) {
            case 0: // Left to Right
                x0 = 0;
                y0 = 0;
                x1 = width;
                y1 = 0;
                break;
            case 45: // Bottom-left to Top-right
                x0 = 0;
                y0 = height;
                x1 = width;
                y1 = 0;
                break;
            case 90: // Top to Bottom
                x0 = 0;
                y0 = 0;
                x1 = 0;
                y1 = height;
                break;
            case 135: // Bottom-right to Top-left
                x0 = width;
                y0 = height;
                x1 = 0;
                y1 = 0;
                break;
            case 180: // Right to Left
                x0 = width;
                y0 = 0;
                x1 = 0;
                y1 = 0;
                break;
            case 225: // Top-right to Bottom-left
                x0 = width;
                y0 = 0;
                x1 = 0;
                y1 = height;
                break;
            case 270: // Bottom to Top
                x0 = 0;
                y0 = height;
                x1 = 0;
                y1 = 0;
                break;
            case 315: // Top-left to Bottom-right
                x0 = 0;
                y0 = 0;
                x1 = width;
                y1 = height;
                break;
        }

        int[] colors;
        if (centerColor != 0 && centerColor2 != 0) {
            // 4 colors gradient
            colors = new int[] { endColor, centerColor2, centerColor, startColor };
        } else if (centerColor != 0) {
            // 3 colors gradient
            colors = new int[] { endColor, centerColor, startColor };
        } else {
            // 2 colors gradient
            colors = new int[] { endColor, startColor };
        }

        return new LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP);
    }

    public boolean isGradientEnabled() {
        return isGradientEnabled;
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
}