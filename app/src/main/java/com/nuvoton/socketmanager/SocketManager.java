package com.nuvoton.socketmanager;

import android.os.AsyncTask;
import android.util.Log;

import com.nuvoton.nuplayer.FileContent;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;

/**
 * Created by timsheu on 6/3/16.
 */
public class SocketManager {
    private SocketInterface socketInterface = null;
    private URL url;
    private static final String TAG = "SocketManager";
    public static final String CMDSNAPSHOT="0";
    public static final String CMDRSTART="1";
    public static final String CMDCHECKSD="2";
    public static final String CMDSDCAP="3";
    public static final String CMDRSTOP="4";
    public static final String CMDFILELIST="5";

    Timer timer = new Timer();

    private InputStream OpenHttpConnection(String urlString) throws IOException
    {
        InputStream in =null;
        int response = -1;

        url = new URL(urlString);
        URLConnection conn = url.openConnection();

        if(!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");
        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            response = httpConn.getResponseCode();
            if(response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        }
        catch (Exception ex)
        {
            //Log.d("Networking", ex.getLocalizedMessage());
            ex.printStackTrace();
            throw new IOException("Error connceting");
        }
        return  in;
    }
    private String SendGet(String url)
    {

        try {
            InputStream In = OpenHttpConnection(url);
            InputStreamReader isr = new InputStreamReader(In);
            int count = 0;
            int charRead;
            String result = "";
            //while (count == 0) {
            //    count = In.available();
            // }
            char[] buf = new char[64];
            //In.read(b);
            try{
                while((charRead=isr.read(buf))>0){
                    String readString = String.copyValueOf(buf,0,charRead);
                    result+=readString;
                    buf = new char[64];
                }
            }catch (IOException e){
                Log.d(TAG,e.getLocalizedMessage());
            }
            Log.d(TAG, "Response Content from server: " +result);

            In.close();
            return   result;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  null;
    }

    private class SendGetTask extends AsyncTask<String,Void,String> {
        String httpcmd="";
        @Override
        protected String doInBackground(String... params) {
            String result= SendGet(params[0]);
            httpcmd = params[1];
            return result;
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObject;
                if(httpcmd.equals(CMDSNAPSHOT)){
                    jsonObject = new JSONObject(result);
                    if(jsonObject.getString("value").equals("0")) {
                        socketInterface.showToastMessage("Snapshot Success !!");
                    }else{
                        socketInterface.showToastMessage("Snapshot Fail !!");
                    }
                }else if (httpcmd.equals(CMDRSTART)) {
                    jsonObject = new JSONObject(result);
                    if (jsonObject.getString("value").equals("0")) {
                        //Toast.makeText(getBaseContext(), "start record success", Toast.LENGTH_SHORT).show();
                    } else {
                    }
                }else if(httpcmd.equals(CMDRSTOP)){
                    jsonObject = new JSONObject(result);
                    if (jsonObject.getString("value").equals("0")){
                        timer.cancel();
                    } else {
                    }
                }else if(httpcmd.equals(CMDCHECKSD)){
                    jsonObject = new JSONObject(result);
                    if(jsonObject.getString("value").equals("0")) {
                    }
                }else if(httpcmd.equals(CMDSDCAP)){
                    String [] tmp = result.split("\n|\r");

                    jsonObject = new JSONObject(tmp[3]);
                    //Log.d(TAG,jsonObject.getString("unit"));
                    // int unit = iInteger.parseInt(jsonObject.getString("unit"));
                    int available=Integer.parseInt(jsonObject.getString("available"));
                    float avilable1=(((float)available)/(1024*1024));
                    Log.d(TAG, String.valueOf(avilable1));
                }else if (httpcmd.equals(CMDFILELIST)) {
                    String [] fileList = result.split("\n");
                    ArrayList<FileContent> fileContentList = new ArrayList<>();
                    int i = 0;
                    for (String s: fileList) {
                        if (s.contains(".mp4")){
                            String [] temp = s.split("_");
                            String time = new String(temp[2]);
                            String [] temp2 = time.split("\\.");
                            String time2 = new String(temp2[0]);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
                            Date date = sdf.parse(time2);
                            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            FileContent content = new FileContent(s, sdf2.format(date), String.valueOf(i));
                            fileContentList.add(content);
                            i++;
                            Log.d(TAG, "onPostExecute: " + sdf2.format(date));
                        }
                    }
                    socketInterface.updateFileList(fileContentList);
                }else {
                    Log.d(TAG,"other cmd");
                }

                // }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void setSocketInterface(SocketInterface socketInterface){
        this.socketInterface = socketInterface;
    }

    public void executeSendGetTask(String command, String commandType){
        new SendGetTask().execute(command, commandType);
    }
}