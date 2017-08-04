package com.enums;

import com.mediacallz.app.R;

public enum ModelObject {

    CONTACTS( R.string.contacts_online_page , R.layout.contacts_layout_page),
    HISTORY( R.string.call_history_page , R.layout.history_layout_page);

    private int mTitleResId;
    private int mLayoutResId;

    ModelObject( int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public int getTitleResId() {
        return mTitleResId;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }

}