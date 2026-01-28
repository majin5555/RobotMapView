package com.siasun.dianshi.sftp;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import org.apache.poi.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Vector;


/**
 * Created by changjian.song on 2021/7/2 at 9:00
 *
 * @description ：
 */
public class SFTPUtil implements MyProgressMonitor.OnDownloadProgressListener {
    private ChannelSftp sftp;

    private Session session;
    /**
     * SFTP 登录用户名
     */
    private String username;
    /**
     * SFTP 登录密码
     */
    private String password;
    /**
     * 私钥
     */
    private String privateKey;
    /**
     * SFTP 服务器地址IP地址
     */
    private String host;
    /**
     * SFTP 端口
     */
    private int port;

    private long mCurrentProgress;

    private long mSumFileSize;

    public MyProgressMonitor myProgressMonitor;

    private OnSFTPListener mListener;


    /**
     * 构造基于密码认证的sftp对象
     */
    public SFTPUtil(String username, String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    public SFTPUtil(String username, String password, String host) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = 10022;
        myProgressMonitor = new MyProgressMonitor();
        myProgressMonitor.setProgressListener(this);
    }

    /**
     * 远程执行linux指令
     */
    public static void exeLinuxBySSH(String host, int port, String user, String password, String command) throws JSchException, IOException {

        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setConfig("StrictHostKeyChecking", "no");

        session.setPassword(password);
        session.connect();

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand(command);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        String out = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

        channelExec.disconnect();
        session.disconnect();

    }


    public void setSFTPListener(OnSFTPListener listener) {
        mListener = listener;
    }

    /**
     * 构造基于秘钥认证的sftp对象
     */
    public SFTPUtil(String username, String host, int port, String privateKey) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.privateKey = privateKey;
    }

    public SFTPUtil() {
    }


    /**
     * 连接sftp服务器
     */
    public void login() {
        try {
            JSch jsch = new JSch();
            if (privateKey != null) {
                jsch.addIdentity(privateKey);// 设置私钥
            }

            session = jsch.getSession(username, host, port);

            if (password != null) {
                session.setPassword(password);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");

            session.setConfig(config);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();

            sftp = (ChannelSftp) channel;

        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭连接 server
     */
    public void logout() {
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
            }
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }


    /**
     * 将输入流的数据上传到sftp作为文件。文件完整路径=basePath+directory
     *
     * @param directory    上传到该目录
     * @param sftpFileName sftp端文件名
     */
    public boolean upload(String directory, String sftpFileName, InputStream input) throws SftpException {
        try {
            if (directory != null && !"".equals(directory)) {
                sftp.cd(directory);
            }
            sftp.put(input, sftpFileName);  //上传文件
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    public void cd(String directory) throws SftpException {
        if (directory != null && !"".equals(directory) && !"/".equals(directory)) {
            sftp.cd(directory);
        }

    }


    /**
     * 下载文件。
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件
     * @param saveFile     存在本地的路径
     */
    public void download(String directory, String downloadFile, String saveFile) {
        System.out.println("download:" + directory + " downloadFile:" + downloadFile + " saveFile:" + saveFile);

        File file = null;
        try {
            if (directory != null && !"".equals(directory)) {
                sftp.cd(directory);
            }
            file = new File(saveFile);
            sftp.get(downloadFile, new FileOutputStream(file));
        } catch (SftpException e) {
            e.printStackTrace();
            if (file != null) {
                file.delete();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if (file != null) {
                file.delete();
            }
        }

    }

    /**
     * 下载文件，并显示下载进度
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件
     * @param saveFile     存在本地的路径
     */
    public boolean downloadShowProgress(String directory, String downloadFile, String saveFile, MyProgressMonitor myProgressMonitor) {
        OutputStream out = null;
        File file = null; //待保存的本地文件
        try {
            file = new File(saveFile);
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            if (directory != null && !"".equals(directory)) {
                sftp.cd(directory);
            }
            //添加回调函数监控进度
            InputStream is = sftp.get(downloadFile, myProgressMonitor);
            byte[] buff = new byte[1024 * 50];
            int read;
            if (is != null) {
                System.out.println("Start to read input stream");
                do {
                    read = is.read(buff, 0, buff.length);
                    if (read > 0) {
                        if (out != null) {
                            out.write(buff, 0, read);
                        }
                    }
                    if (out != null) {
                        out.flush();
                    }
                } while (read >= 0);
                System.out.println("input stream read done.");
                is.close();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return file.delete();
        } finally {
            sftp.quit();
            session.disconnect();
        }

    }

    /**
     * 下载文件
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件名
     * @return 字节数组
     */
    public byte[] download(String directory, String downloadFile) throws SftpException, IOException {
        if (directory != null && !"".equals(directory)) {
            sftp.cd(directory);
        }
        InputStream is = sftp.get(downloadFile);

        byte[] fileData = IOUtils.toByteArray(is);

        return fileData;
    }

    //获取文件大小
    public long getFileSize(String srcSftpFilePath) {
        long fileSize;//文件大于等于0则存在
        try {
            SftpATTRS sftpATTRS = sftp.lstat(srcSftpFilePath);
            fileSize = sftpATTRS.getSize();
        } catch (Exception e) {
            fileSize = -1;//获取文件大小异常
            if (e.getMessage().equalsIgnoreCase("no such file")) {
                fileSize = -2;//文件不存在
            }
        }
        return fileSize;
    }


    /**
     * 删除文件
     *
     * @param directory  要删除文件所在目录
     * @param deleteFile 要删除的文件
     */
    public void delete(String directory, String deleteFile) throws SftpException {
        if (directory != null && !"".equals(directory)) {
            sftp.cd(directory);
        }
        sftp.rm(deleteFile);
    }


    /**
     * 列出目录下的文件
     *
     * @param directory 要列出的目录
     */
    public Vector<?> listFiles(String directory) throws SftpException {
        return sftp.ls(directory);
    }

    @Override
    public void currentProgress(long progress) {
        mCurrentProgress = progress;
        System.out.println("当前下载进度 ============> " + (mCurrentProgress * 100 / mSumFileSize) + " %");
    }

    /**
     * 获取当前下载进度
     *
     * @return
     */
    public long currentProgress() {
        return (mCurrentProgress * 100 / mSumFileSize);
    }

    public interface OnSFTPListener {
        void downloadError();
    }

    private double[] ominus_d(double[] x, double[] res) {

        double c = Math.cos(x[2]);
        double s = Math.sin(x[2]);
        res[0] = -c * x[0] - s * x[1];
        res[1] = s * x[0] - c * x[1];
        res[2] = -x[2];
        System.out.println("ominus_d result , res[0] = " + res[0] +
                ",res[1] = " + res[1] +
                ",res[2] = " + res[2] * 180 / Math.PI);
        return res;
    }

    private double[] oplus_d(double[] x1, double[] x2, double[] res) {

        double c = Math.cos(x1[2]);
        double s = Math.sin(x1[2]);
        double x = x1[0] + c * x2[0] - s * x2[1];
        double y = x1[1] + s * x2[0] + c * x2[1];
        double theta = x1[2] + x2[2];
        res[0] = x;
        res[1] = y;
        res[2] = theta;
        System.out.println("oplus_d result , res[0] = " + res[0] +
                ",res[1] = " + res[1] +
                ",res[2] = " + res[2] * 180 / Math.PI);
        return res;
    }


    //上传文件测试
   /* public static void main(String[] args) throws SftpException, IOException, JSchException {

        SFTPUtil sftp = new SFTPUtil("root", "", "192.168.3.101", 22);

        sftp.login();

        String serverFilePath = "/home/root/CarryBoy";//\home\proembed
        String serverFileName = "123.jff";//ReflectorPoints.dx

        sftp.mSumFileSize = sftp.getFileSize(serverFilePath + "/" + serverFileName);
        System.out.println("mSumFileSize = " + sftp.mSumFileSize);


//        sftp.download(serverFilePath, serverFileName, "C:/Users/guke/Desktop/" + serverFileName);//"./" + serverFileName
        boolean flag = sftp.downloadShowProgress(serverFilePath
                , serverFileName
                , "C:/Users/guke/Desktop/" + serverFileName
                , sftp.myProgressMonitor);
        if (!flag) {
            System.out.println("下载失败，请重试");
        }

        sftp.logout();

        exeLinuxBySSH("192.168.3.101",22,"root","","cat /home/root/CarryBoy/Bootload_Mrc04.json");

    }*/


} 