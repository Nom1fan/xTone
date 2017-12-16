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

import com_international.data.objects.CallHistoryRecord;
import com_international.enums.CallRecordType;
import com_international.mediacallz.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rony on 31/07/2017.
 */

public class CallRecordsAdapter extends ArrayAdapter<CallHistoryRecord> implements Filterable {
    private List<CallHistoryRecord> allRecords;
    private List<CallHistoryRecord> dynamicRecords;
    private List<CallHistoryRecord> arrayOfRecords;

    CallRecordsAdapter(Context context, List<CallHistoryRecord> allRecords, List<CallHistoryRecord> copyOfAllRecords) {
        super(context, 0, allRecords);
        this.allRecords = allRecords;
        arrayOfRecords = new ArrayList<>(copyOfAllRecords);
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
                    List<CallHistoryRecord> found = new ArrayList<>();
                    dynamicRecords = new ArrayList<>(arrayOfRecords);
                    for (CallHistoryRecord record : dynamicRecords) {
                        if (record.getNameOrNumber().toLowerCase().contains(constraint)) {
                            found.add(record);
                        }
                    }
                    result.values = found;
                    result.count = found.size();
                } else {
                    result.values = dynamicRecords;
                    if (dynamicRecords != null)
                        result.count = dynamicRecords.size();
                }
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                if (results.values != null)
                    for (CallHistoryRecord record : (List<CallHistoryRecord>) results.values) {
                        add(record);
                    }
                notifyDataSetChanged();
            }
        };
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position

        CallHistoryRecord callRecord = new CallHistoryRecord();
        callRecord = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.call_record_row, parent, false);
        }
        // Lookup view for data population
        TextView tvNameOrPhone = (TextView) convertView.findViewById(R.id.call_name_or_number);
        TextView tvDateAndTime = (TextView) convertView.findViewById(R.id.call_record_date_time);
        TextView tvDuration = (TextView) convertView.findViewById(R.id.call_record_duration);
        ImageView callRecordDirectionImage = (ImageView) convertView.findViewById(R.id.call_record_direction);
        // Populate the data into the template view using the data object
        tvNameOrPhone.setText(callRecord != null ? callRecord.getNameOrNumber() : null);
        tvDateAndTime.setText(callRecord != null ? callRecord.getDateAndTime() : null);
        tvDuration.setText(callRecord != null ? callRecord.getDuration() : null);

        if (callRecord.getCallType() != null) {
            if (callRecord != null && callRecord.getCallType().equals(CallRecordType.INCOMING)) {
                callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_incoming);
                callRecordDirectionImage.setTag("incoming");
            } else if (callRecord != null && callRecord.getCallType().equals(CallRecordType.OUTGOING)) {
                callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_outgoing);
                callRecordDirectionImage.setTag("outgoing");
            } else if (callRecord != null && callRecord.getCallType().equals(CallRecordType.MISSED)) {
                callRecordDirectionImage.setImageResource(android.R.drawable.sym_call_missed);
                callRecordDirectionImage.setTag("missed");
            } else {
                callRecordDirectionImage.setImageResource(android.R.drawable.ic_menu_help);
                callRecordDirectionImage.setTag("UKNOWN");
            }
        } else {
            callRecordDirectionImage.setImageResource(android.R.drawable.ic_menu_help);
            callRecordDirectionImage.setTag("UKNOWN");
        }
        // Return the completed view to render on screen
        return convertView;
    }
}
