package com.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.mediacallz.app.R;
import com.utils.MediaFilesUtils;
import com.utils.UI_Utils;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exceptions.FileDoesNotExistException;
import com.exceptions.FileExceedsMaxSizeException;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;

public class AudioFilesHistoryActivity extends AppCompatActivity implements OnItemClickListener {

    private static final String TAG = AudioFilesHistoryActivity.class.getSimpleName();
    private List<String> _namesInListView = new ArrayList<String>(); // the list that the adapter uses to populate the view
    private BlackListAdapter _ma;
    private ListView _lv;
    private Set<String> _audioFilesSet = new HashSet<String>();
    private MediaPlayer mMediaPlayer;
    private String mChosenAudioFile = "";
    private boolean oneTimeCheckBox;
    private ImageButton UploadBtn;
    private ImageButton CancelBtn;
    private ImageButton PlayPauseBtn;
    private boolean isPreviewDisplaying = false;
    private HashMap<String,String> paths_to_titles;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_history);
        log(Log.INFO, TAG, "onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log(Log.INFO, TAG, "onResume()");

        prepareListViewData();
        prepareListView();
        displayListViewWithNewData();

        prepareChooseButton();
    }

    private void prepareChooseButton() {
        UploadBtn = (ImageButton) findViewById(R.id.audio_upload_btn);
        UploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log(Log.INFO, TAG, "choose Button Pressed");
                returnWithResultIntent();

            }
        });

        CancelBtn = (ImageButton) findViewById(R.id.audio_cancel_btn);
        CancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log(Log.INFO, TAG, "choose Button Pressed");
                finish();

            }
        });

        PlayPauseBtn = (ImageButton) findViewById(R.id.audio_play_pause_btn);
        PlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (isPreviewDisplaying) {
                    playAudioFile();
                } else {
                    stopAudioFile();
                }
            }
        });
    }

    private void stopAudioFile() {

        if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();

        if (mMediaPlayer.isPlaying())
        {
            Log.i(TAG," STOP HISTORY AUDIO FILE");
            try {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;

                isPreviewDisplaying = true;
                PlayPauseBtn.setImageResource(R.drawable.play_preview_anim);
                PlayPauseBtn.invalidate();

                //UploadBtn.setText(getResources().getString(R.string.back));
            } catch (Exception e) {
                e.printStackTrace();
                UI_Utils.callToast(getResources().getString(R.string.choose_audio_file), Color.WHITE, getApplicationContext());
            }

        }

        isPreviewDisplaying = true;
        PlayPauseBtn.setImageResource(R.drawable.play_preview_anim);
        PlayPauseBtn.invalidate();



    }

    private void playAudioFile() {

        if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();

        if (mChosenAudioFile.isEmpty()) {
            UI_Utils.callToast(getResources().getString(R.string.choose_audio_file), Color.WHITE, getApplicationContext());
            return;
        }
        MediaFile.FileType type = null;
        try {
            MediaFile audioFileSelected = new MediaFile(mChosenAudioFile);
            type = audioFileSelected.getFileType();
        } catch (FileInvalidFormatException e) {
            e.printStackTrace();
        } catch (FileExceedsMaxSizeException e) {
            e.printStackTrace();
        } catch (FileDoesNotExistException e) {
            e.printStackTrace();
        } catch (FileMissingExtensionException e) {
            e.printStackTrace();
        }

        if (type != MediaFile.FileType.AUDIO) {
            return;
        }

        try {
            try {
                log(Log.INFO, TAG, "Audio File that will be played: " + mChosenAudioFile);
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(mChosenAudioFile));

            } catch (Exception e) {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(getApplicationContext(),Uri.parse(mChosenAudioFile));
            }

            mMediaPlayer.prepare();
            mMediaPlayer.setLooping(true);
            mMediaPlayer.start();
            Log.i(TAG," START HISTORY AUDIO FILE");



            //UploadBtn.setText(getResources().getString(R.string.upload));

        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to play sound. Exception:" + e.getMessage());
            UI_Utils.callToast(getResources().getString(R.string.choose_audio_file), Color.WHITE, getApplicationContext());

        }

        PlayPauseBtn.setImageResource(R.drawable.stop_preview_anim);
        isPreviewDisplaying = false;
        PlayPauseBtn.invalidate();

    }

    private void prepareListViewData() {
        _audioFilesSet = MediaFilesUtils.getAllAudioHistoryFiles();
        populateContactsToDisplayFromBlockedList(); // populate all contacts to view with checkboxes
    }

    private void displayListViewWithNewData() {
        _ma = new BlackListAdapter();
        _lv.setAdapter(_ma);  // link the listview with the adapter
    }

    private void prepareListView() {
        _lv = (ListView) findViewById(R.id.lv);
        _lv.setOnItemClickListener(this);
        _lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        _lv.setItemsCanFocus(false);
        _lv.setTextFilterEnabled(true);
    }

    @Override
    protected void onPause() {
        log(Log.INFO, TAG, "onPause");


        if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    private void returnWithResultIntent() {
        Intent returnIntent = new Intent();


        if (mChosenAudioFile.isEmpty()) {
            UI_Utils.callToast(getResources().getString(R.string.choose_audio_file), Color.WHITE, getApplicationContext());
            return;
        }

        MediaFile resultFile = createManagedFile(mChosenAudioFile);
        if(resultFile == null)
            finish();


        returnIntent.putExtra(PreviewMediaActivity.RESULT_FILE, resultFile);

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, returnIntent);
        } else {
            getParent().setResult(Activity.RESULT_OK, returnIntent);
        }
        finish();
    }

    private MediaFile createManagedFile(String resultFilePath) {
        MediaFile managedFile = null;
        try {
            managedFile = new MediaFile(resultFilePath);
        } catch(Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to create result managed file");
        }
        return managedFile;
    }


    @Override
    public void onBackPressed() {
        log(Log.INFO, TAG, "onBackPressed");
        super.onBackPressed();
    }


    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        _ma.toggle(arg2);
    }

    public void populateContactsToDisplayFromBlockedList() {

        paths_to_titles = new HashMap<String, String>();
        _namesInListView = new ArrayList<String>();
        for (String name : _audioFilesSet) {

            String fileName = FilenameUtils.getName(name);

            if (!fileName.contains(".nomedia"))
            {
                String Title= "";
                try {
                    String removeThisString;
                    if (fileName.contains("trimmed"))
                            removeThisString = fileName.substring(0, fileName.indexOf("_")+1);
                        else
                            removeThisString = fileName.substring(fileName.lastIndexOf("_"), fileName.lastIndexOf("."));

                    Title = fileName.replace(removeThisString, "");
                }catch(Exception e)
                {
                    e.printStackTrace();
                    Title = name;
                }
                paths_to_titles.put(Title,name);
                _namesInListView.add(Title);

            }
        }
    }

    class BlackListAdapter extends BaseAdapter//,Filterable
    {
        private SparseBooleanArray mCheckStates;
        LayoutInflater mInflater;
        TextView tv;
        String chosenTitle;
        TextView old_tv;

        BlackListAdapter() {
            mCheckStates = new SparseBooleanArray(_namesInListView.size());
            mInflater = (LayoutInflater) AudioFilesHistoryActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return _namesInListView.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
                vi = LayoutInflater.from(getApplicationContext()).inflate(R.layout.row_for_audio_activity, null);  // vi = mInflater.inflate(R.layout.row, null);

            tv = (TextView) vi.findViewById(R.id.textView1);

            if (_namesInListView.get(position) != null)
                tv.setText(_namesInListView.get(position));



                tv.setTextColor(Color.WHITE);

            if (tv.getText().equals(chosenTitle))
                tv.setTextColor(Color.YELLOW);

            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (old_tv!=null)
                        old_tv.setTextColor(Color.WHITE);

                    old_tv =(TextView)v;

                    ((TextView)v).setTextColor(Color.YELLOW);
                    chosenTitle = ((TextView)v).getText().toString();
                    mChosenAudioFile = paths_to_titles.get(chosenTitle);
                    _ma.notifyDataSetChanged();

                   stopAudioFile();

                }
            });
            _ma.notifyDataSetChanged();

            return vi;
        }

        public boolean isChecked(int position) {
            return mCheckStates.get(position, false);
        }

        public void setChecked(int position, boolean isChecked) {
            mCheckStates.put(position, isChecked);
        }

        public void toggle(int position) {
            setChecked(position, !isChecked(position));
        }


    }

}
