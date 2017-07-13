package com.syc.demo.makejar.multiactiontextview;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * @author Ajay Sahani:
 * @seeA class which is responsible for handling multiple type of click on
 * single TextView and use different color for text section on which we
 * want click action.
 */
public class MultiActionTextView {

    public static void setSpannableText(TextView textView,
                                        SpannableStringBuilder stringBuilder, int highLightTextColor) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(stringBuilder, BufferType.SPANNABLE);
        textView.setLinkTextColor(highLightTextColor);
    }

    /**
     * @param inputObject :should not null Method responsible for creating click able
     *                    part in TextView without hyper link.
     */
    public static void addActionOnTextViewWithLink(final InputObject inputObject) {
        inputObject.getStringBuilder().setSpan(
                new MultiActionTextViewClickableSpan(true) {
                    @Override
                    public void onClick(View widget) {
                        if (inputObject.getMultiActionTextviewClickListener() != null) {
                            inputObject.getMultiActionTextviewClickListener()
                                    .onTextClick(inputObject);
                        }
                    }
                }, inputObject.getStartSpan(), inputObject.getEndSpan(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        //用颜色标记文本
        inputObject.getStringBuilder().setSpan(new ForegroundColorSpan(inputObject.getTextColor()),
                inputObject.getStartSpan(), inputObject.getEndSpan(),
                //setSpan时需要指定的 flag,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE(前后都不包括).
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * @param inputObject :should not null Method responsible for creating click able
     *                    part in TextView with hyper link.
     */
    public static void addActionOnTextViewWithoutLink(
            final InputObject inputObject) {
        //用颜色标记文本
        inputObject.getStringBuilder().setSpan(new ForegroundColorSpan(inputObject.getTextColor()),
                inputObject.getStartSpan(), inputObject.getEndSpan(),
                //setSpan时需要指定的 flag,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE(前后都不包括).
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static void addImageSpanOnTextView(
            final InputObject inputObject, Drawable drawable) {
        //用颜色标记文本
        inputObject.getStringBuilder().setSpan(new ImageSpan(drawable),
                inputObject.getStartSpan(), inputObject.getEndSpan(),
                //setSpan时需要指定的 flag,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE(前后都不包括).
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

}
