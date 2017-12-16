package com_international.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com_international.data.objects.ContactWrapper;
import com_international.enums.UserStatus;
import com_international.mediacallz.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rony on 31/07/2017.
 */

public class OnlineContactAdapter extends ArrayAdapter<ContactWrapper> implements Filterable {
    private List<ContactWrapper> allContacts;
    private List<ContactWrapper> arrayOfUsers;
    private List<ContactWrapper> dynamicContacts;


    public OnlineContactAdapter(Context context, List<ContactWrapper> allContacts, List<ContactWrapper> copyOfAllContacts) {
        super(context, 0, allContacts);
        this.allContacts = allContacts;
        this.arrayOfUsers = new ArrayList<>(copyOfAllContacts);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();
                if (constraint.toString().length() > 0) {
                    List<ContactWrapper> found = new ArrayList<>();
                    dynamicContacts = new ArrayList<>(arrayOfUsers);
                    for (ContactWrapper contactWrapper : dynamicContacts) {
                        if (contactWrapper.getContact().getName().toLowerCase().contains(constraint) || contactWrapper.getContact().getPhoneNumber().contains(constraint)) {
                            found.add(contactWrapper);
                        }
                    }
                    result.values = found;
                    result.count = found.size();
                } else {
                    result.values = dynamicContacts;
                    if (dynamicContacts != null)
                        result.count = dynamicContacts.size();
                }
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                if (results.values != null)
                    for (ContactWrapper contactWrapper : (List<ContactWrapper>) results.values) {
                        add(contactWrapper);
                    }
                notifyDataSetChanged();
            }
        };
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        ContactWrapper contactWrapper = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.online_contact_row, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.contact_name);
        TextView tvPhone = (TextView) convertView.findViewById(R.id.contact_phone);
        ImageView contactStatusImage = (ImageView) convertView.findViewById(R.id.contact_status);
        // Populate the data into the template view using the data object
        tvName.setText(contactWrapper != null ? contactWrapper.getContact().getName() : null);
        tvPhone.setText(contactWrapper != null ? contactWrapper.getContact().getPhoneNumber() : null);

        if (contactWrapper != null && contactWrapper.getUserStatus().equals(UserStatus.REGISTERED)) {
            contactStatusImage.setImageResource(android.R.drawable.presence_online);
            contactStatusImage.setTag("on");
        } else {
            contactStatusImage.setImageResource(android.R.drawable.presence_invisible);
            contactStatusImage.setTag("off");
        }
        // Return the completed view to render on screen
        return convertView;
    }
}
