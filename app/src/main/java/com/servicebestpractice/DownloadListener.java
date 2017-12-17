package com.servicebestpractice;

/**
 * Created by hejiao on 2017/12/17.
 */

public interface DownloadListener {
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanceled();
}
