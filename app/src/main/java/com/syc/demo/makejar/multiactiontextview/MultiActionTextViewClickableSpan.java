package com.syc.demo.makejar.multiactiontextview;


import android.text.TextPaint;
import android.text.style.ClickableSpan;

/**
 * Created by yuancunsun on 2017/7/13.
 */

public abstract class MultiActionTextViewClickableSpan extends ClickableSpan {

    private boolean isUnderLineRequired;

    public MultiActionTextViewClickableSpan(boolean isUnderLineRequired) {
        this.isUnderLineRequired = isUnderLineRequired;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.bgColor = 0x66000000;
        ds.setUnderlineText(isUnderLineRequired);
    }

}

