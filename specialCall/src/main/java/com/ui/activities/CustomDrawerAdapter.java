package com.ui.activities;

/**
 * Created by rony on 06/02/2016.
 */

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediacallz.app.R;

import java.util.List;

public class CustomDrawerAdapter extends ArrayAdapter<DrawerItem> {

    private List<DrawerItem> _drawerItemList;
    private int _layoutResID;

    public CustomDrawerAdapter(Context context, int layoutResourceID,
                               List<DrawerItem> listItems) {
        super(context, layoutResourceID, listItems);
        this._drawerItemList = listItems;
        this._layoutResID = layoutResourceID;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        DrawerItemHolder drawerHolder;
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            drawerHolder = new DrawerItemHolder();

            view = inflater.inflate(_layoutResID, parent, false);
            drawerHolder.ItemName = (TextView) view
                    .findViewById(R.id.drawer_itemName);


            drawerHolder.icon = (ImageView) view.findViewById(R.id.drawer_icon);

            view.setTag(drawerHolder);

        } else {
            drawerHolder = (DrawerItemHolder) view.getTag();

        }

        DrawerItem dItem = this._drawerItemList.get(position);

        if (!dItem.getItemName().isEmpty()) {
            drawerHolder.icon.setImageDrawable(view.getResources().getDrawable(
                    dItem.getImgResID()));


            drawerHolder.ItemName.setText(dItem.getItemName());

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(150, 150);
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            layoutParams.setMargins(20, 0, 20, 0);
            drawerHolder.icon.setLayoutParams(layoutParams);
        }else
        {
            drawerHolder.ItemName.setText(getContext().getResources().getString(R.string.side_menu));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            //layoutParams.setMargins(50, 0, 50, 0);
            drawerHolder.ItemName.setLayoutParams(layoutParams);

            view.setClickable(false);
            view.setBackgroundColor(0xb000a0d1);
        }

        return view;
    }

    private static class DrawerItemHolder {
        TextView ItemName;
        ImageView icon;
    }
}