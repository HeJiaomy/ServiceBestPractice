package com.servicebestpractice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask downloadTask;
    private String downloadUrl;

    private DownloadListener downloadListener= new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("DownLoading...",progress));
        }

        @Override
        public void onSuccess() {
            downloadTask= null;
            //下载成功时将前台服务通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("DownLoad Success",-1));
            Toast.makeText(DownloadService.this,"DownLoad Success",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask= null;
            //下载失败时，将前台服务通知关闭，并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("DownLoad Failed",-1));
            Toast.makeText(DownloadService.this,"DownLoad Failed",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask= null;
            Toast.makeText(DownloadService.this,"DownLoad Paused",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask= null;
            stopForeground(true);
            Toast.makeText(DownloadService.this,"DownLoad Canceled",Toast.LENGTH_SHORT).show();
        }
    };

    private DownloadBinder mBinder= new DownloadBinder();

    class DownloadBinder extends Binder{
        public void startDownload(String url){
            if (downloadTask== null){
                downloadUrl= url;
                downloadTask= new DownloadTask(downloadListener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("DownLoading...",0));
                Toast.makeText(DownloadService.this,"DownLoading...",Toast.LENGTH_SHORT).show();
            }
        }

        public void pauseDownload(){
            if (downloadTask!= null){
                downloadTask.pauseDownLoad();
            }
        }

        public void cancelDownload(){
            if (downloadTask != null){
                downloadTask.cancelDownLoad();
            }else {
                if (downloadUrl!= null){
                    //取消下载时需将文件删除，并将通知关闭
                    String fileName= downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file= new File(directory+fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this,"Canceled",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
      return mBinder;
    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title,int progress){
        Intent intent= new Intent(this,MainActivity.class);
        PendingIntent pi= PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder= new NotificationCompat.Builder(this,null);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress>=0){
            //当progress大于或等于0时才显示进度
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }
}
