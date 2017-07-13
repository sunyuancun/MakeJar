package com.syc.demo.makejar.multiactiontextview;


import android.text.SpannableStringBuilder;

/**
 * Created by yuancunsun on 2017/7/13.
 */
public class InputObject {

    /**
     * start index
     */
    private int startSpan;
    /**
     * end index
     */
    private int endSpan;
    /**
     * append entire sub text into SpannableStringBuilder
     */
    private SpannableStringBuilder stringBuilder;
    /**
     * Call back to handle click event
     */
    private MultiActionTextviewClickListener multiActionTextviewClickListener;
    /**
     * input object which we want when click event occur.
     */
    private Object inputObject;
    /**
     * uniquely identify on which part of text view click.
     */
    private int OperationType;

    private int textColor;

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public Object getInputObject() {
        return inputObject;
    }

    public void setInputObject(Object inputObject) {
        this.inputObject = inputObject;
    }

    public int getOperationType() {
        return OperationType;
    }

    public void setOperationType(int operationType) {
        OperationType = operationType;
    }

    public int getStartSpan() {
        return startSpan;
    }

    public void setStartSpan(int startSpan) {
        this.startSpan = startSpan;
    }

    public int getEndSpan() {
        return endSpan;
    }

    public void setEndSpan(int endSpan) {
        this.endSpan = endSpan;
    }

    public SpannableStringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public void setStringBuilder(SpannableStringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public MultiActionTextviewClickListener getMultiActionTextviewClickListener() {
        return multiActionTextviewClickListener;
    }

    public void setMultiActionTextviewClickListener(
            MultiActionTextviewClickListener multiActionTextviewClickListener) {
        this.multiActionTextviewClickListener = multiActionTextviewClickListener;
    }

}

