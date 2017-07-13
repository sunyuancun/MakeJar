package com.syc.demo.makejar.multiactiontextview;

/**
 * Created by yuancunsun on 2017/7/13.
 */

public interface MultiActionTextviewClickListener {
    /**
     * @param inputObject : Object which we had sent in request getting back when click
     *                    operation occur basically use to identify which part of Text
     *                    clicked. User operation type variable to identify for which
     *                    section of text clicked.
     */
    void onTextClick(InputObject inputObject);


}
