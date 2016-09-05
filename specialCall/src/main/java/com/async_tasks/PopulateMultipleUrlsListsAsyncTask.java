package com.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.validate.media.ValidateMediaFormatBehavior;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mor on 23/08/2016.
 */
public class PopulateMultipleUrlsListsAsyncTask extends AsyncTask<Void, Void, List<List<String>>> {

    private static final String TAG = PopulateMultipleUrlsListsAsyncTask.class.getSimpleName();
    private static final int READ_TIMEOUT = 10*1000;

    private List<ValidateMediaFormatBehavior> validateMediaFormatBehaviors;
    private PostMultiPopulateListener listener;
    private List<String> urlsToScan;

    public PopulateMultipleUrlsListsAsyncTask(List<ValidateMediaFormatBehavior> validateMediaFormatBehaviors, List<String> urlsToScan, PostMultiPopulateListener listener) {
        this.listener = listener;
        this.urlsToScan = urlsToScan;
        this.validateMediaFormatBehaviors = validateMediaFormatBehaviors;
    }

    @Override
    protected List<List<String>> doInBackground(Void... params) {
        Log.i(TAG, "doInBackground");
        List<List<String>> resultsUrls = new LinkedList<>();
        Document doc;
        String link;
        for (int i = 0; i < urlsToScan.size(); i++) {
            try {
                List resultUrls = new ArrayList();
                doc = Jsoup.connect(urlsToScan.get(i)).timeout(READ_TIMEOUT).get();
                for (Element el : doc.select("td a")) {
                    link = el.attr("href");
                    Log.d(TAG, urlsToScan + link);
                    if(validateMediaFormatBehaviors.get(i).isValidFormatByLink(link))
                        resultUrls.add(urlsToScan.get(i) + link);
                }
                resultsUrls.add(resultUrls);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resultsUrls;
    }

    @Override
    protected void onPostExecute(List<List<String>> resultUrls) {
        listener.constructPostPopulate(resultUrls);
    }

    public interface PostMultiPopulateListener {
        void constructPostPopulate(List<List<String>> urls);
    }

}
