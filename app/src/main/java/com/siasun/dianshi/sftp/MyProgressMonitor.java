package com.siasun.dianshi.sftp;


import com.jcraft.jsch.SftpProgressMonitor;

/**
 * Created by changjian.song on 2021/7/12 at 16:11
 *
 * @description ：
 */
public class MyProgressMonitor implements SftpProgressMonitor {

    private long transfered;
    private OnDownloadProgressListener mListener;

    public void setProgressListener(OnDownloadProgressListener listener) {
        mListener = listener;
    }

    @Override
    public boolean count(long count) {
        transfered = transfered + count;

        if (transfered < 1024) {
//            System.out.println("Currently transferred total size: " + transfered + " bytes");
        }

        if ((transfered > 1024) && (transfered < 1048576)) {
//            System.out.println("Currently transferred total size: " + new DecimalFormat("0.000").format(transfered / 1024) + "K bytes");
        } else {
//            System.out.println("Currently transferred total size: " + new DecimalFormat("0.000").format(transfered / 1024 / 1024) + "M bytes");
        }

        mListener.currentProgress(transfered);
        return true;
    }

    public void resetProgress(){
        transfered = 0;
    }

    @Override
    public void end() {
        System.out.println("Transferring done.");
    }

    @Override
    public void init(int op, String src, String dest, long max) {
        System.out.println("Transferring begin.");
    }

    public interface OnDownloadProgressListener {
        void currentProgress(long progress);
    }
} 