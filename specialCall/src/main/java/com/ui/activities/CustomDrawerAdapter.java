package com.ui.activities;

/**
 * Created by rony on 06/02/2016.
 */
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.special.app.R;

public class CustomDrawerAdapter extends ArrayAdapter<DrawerItem> {

    Context context;
    List<DrawerItem> drawerItemList;
    int layoutResID;

    public CustomDrawerAdapter(Context context, int layoutResourceID,
                               List<DrawerItem> listItems) {
        super(context, layoutResourceID, listItems);
        this.context = context;
        this.drawerItemList = listItems;
        this.layoutResID = layoutResourceID;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        DrawerItemHolder drawerHolder;
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            drawerHolder = new DrawerItemHolder();

            view = inflater.inflate(layoutResID, parent, false);
            drawerHolder.ItemName = (TextView) view
                    .findViewById(R.id.drawer_itemName);



            drawerHolder.icon = (ImageView) view.findViewById(R.id.drawer_icon);

            view.setTag(drawerHolder);

        } else {
            drawerHolder = (DrawerItemHolder) view.getTag();

        }

        DrawerItem dItem = (DrawerItem) this.drawerItemList.get(position);

        drawerHolder.icon.setImageDrawable(view.getResources().getDrawable(
                dItem.getImgResID()));
        if(!dItem.getItemName().isEmpty())
        { drawerHolder.ItemName.setText(dItem.getItemName());

            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(150, 150);
            layoutParams.gravity = Gravity.RIGHT;
            drawerHolder.icon.setLayoutParams(layoutParams);


        }
        else {
            drawerHolder.ItemName.setClickable(false);
            drawerHolder.ItemName.setVisibility(View.INVISIBLE);
            drawerHolder.icon.setClickable(false);
            drawerHolder.icon.setBackgroundColor(Color.parseColor("#cbe4f0"));
            view.setBackgroundColor(Color.parseColor("#cbe4f0"));

            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL | Gravity.CENTER;

            layoutParams.width = 300;
            layoutParams.height = 300;

            drawerHolder.icon.setLayoutParams(layoutParams);
            view.setClickable(false);



        }

        return view;
    }

    private static class DrawerItemHolder {
        TextView ItemName;
        ImageView icon;
    }
}