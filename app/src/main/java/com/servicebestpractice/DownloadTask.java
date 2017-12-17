package com.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by hejiao on 2017/12/17.
 */

public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;

    private DownloadListener listener;

    private boolean isCanceled= false;
    private boolean isPaused=false;
    private int lastProgress;
    public DownloadTask(DownloadListener listener){
        this.listener= listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {

        File file= null;
        InputStream in = null;
        RandomAccessFile saveFile = null;
        try {
            long downloadLength=0;  //记录下载的长度
            String downloadUrl= strings[0];
            String fileName= downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file= new File(directory + fileName);
            if (file.exists()){
                downloadLength= file.length();  //如果文件存在则读取已下载的字节数
            }
            long contentLength= getContentLength(downloadUrl);
            if (contentLength==0){
                return TYPE_FAILED;
            }else if (contentLength== downloadLength){
                //已下载字节和文件总字节相等，说明已经下载完成了
                return TYPE_SUCCESS;
            }
            OkHttpClient client= new OkHttpClient();
            Request request= new Request.Builder()
                    //断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE","bytes="+downloadLength+"-")
                    .url(downloadUrl)
                    .build();
            Response response= client.newCall(request).execute();
            if (response!= null){
                in= response.body().byteStream();
                saveFile= new RandomAccessFile(file,"rw");
                saveFile.seek(downloadLength);  //跳过已下载的字节
                byte[] buff= new byte[1024];
                int len;
                int total = 0;
                while ((len= in.read(buff))!= -1){
                    if (isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else {
                        total+=len;
                        saveFile.write(buff,0,len);
                        //计算已下载的百分比
                        int progress= (int) ((total+downloadLength)*100/contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (in!= null){
                    in.close();
                }
                if (saveFile!= null){
                    saveFile.close();
                }
                if (isCanceled && file!= null){
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client= new OkHttpClient();
        Request request= new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response= client.newCall(request).execute();
        if (response!= null && response.isSuccessful()){
            long contentLength= response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progress= values[0];
        if (progress>lastProgress){
            listener.onProgress(progress);
            lastProgress= progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        switch (integer){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
        }
    }

    public void pauseDownLoad(){
        isPaused= true;
    }

    public void cancelDownLoad(){
        isCanceled=true;
    }
}
