package environzen.dev;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class NavigationTask extends AsyncTask<String, Void, JSONArray> {

    private NavigationActivity navigationActivity;

    public NavigationTask(NavigationActivity navigationActivity) {
        this.navigationActivity = navigationActivity;
    }

    @Override
    protected JSONArray doInBackground(String... params) {

        String str = params[0];
        URLConnection urlConn = null;
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(str);
            urlConn = url.openConnection();
            bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            return new JSONObject(stringBuffer.toString()).getJSONArray("features").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
        } catch (Exception ex) {
            Log.e("App", "yourDataTask", ex);
            return null;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPostExecute(JSONArray response) {
        if (response != null) {
            ArrayList<String> coordinates = new ArrayList<String>();
            int len = response.length();
            for (int i = 0; i < len; i++) {
                try {
                    coordinates.add(response.get(i).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            navigationActivity.onTaskCompleted(coordinates);
        }
    }
}
