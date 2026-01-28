package com.siasun.dianshi.ftp;


import static com.siasun.dianshi.network.constant.HttpConstantKt.KEY_NEY_IP;

import android.util.Log;

import com.siasun.dianshi.framework.log.LogUtil;
import com.siasun.dianshi.sftp.SFTPUtil;
import com.tencent.mmkv.MMKV;


import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by jian.shi on 2018/9/26.
 *
 * @Email shijian1@siasun.com
 * <p>
 * fix by majin on 2023/12/22
 */

public class FTPManager {


    /**
     * MRC05
     */
    private final String MRC05_USER_NAME = "siasun";
    private final String MRC05_PWD = "siasun";
    private final int PORT = 21;

    private FTPClient ftpClient = null;

    private static FTPManager INSTANCE;

    public static FTPManager getInstance() {
        if (INSTANCE == null) {
            synchronized (FTPManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FTPManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 初始化ftp服务器
     */
    private boolean initFtpClient() {
        ftpClient = new FTPClient();
        ftpClient.setControlEncoding("utf-8");
        ftpClient.setConnectTimeout(3 * 1000);
        try {
            String ip = MMKV.defaultMMKV().decodeString(KEY_NEY_IP, "192.168.3.101");
            LogUtil.INSTANCE.d("IP " + ip);
            ftpClient.connect(ip, PORT); //连接ftp服务器
            ftpClient.login(MRC05_USER_NAME, MRC05_PWD);//登录ftp服务器
            return FTPReply.isPositiveCompletion(ftpClient.getReplyCode());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传文件
     *
     * @param pathname       ftp服务保存地址
     * @param fileName       上传到ftp的文件名
     * @param originFilename 待上传文件的名称（绝对地址） *
     * @return
     */
    public synchronized boolean uploadFile(String pathname, String fileName, String originFilename) {
        boolean flag = false;
        InputStream inputStream = null;
        try {
            LogUtil.INSTANCE.i("=======开始上传");
            LogUtil.INSTANCE.i("FTP服务器文件目录  " + pathname);
            LogUtil.INSTANCE.i("文件名称  " + fileName);
            LogUtil.INSTANCE.i("上传后的文件路径  " + originFilename);
            boolean initFtpClient = initFtpClient();
            LogUtil.INSTANCE.d("初始化ftp服务器" + initFtpClient);
            if (initFtpClient) {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                inputStream = new FileInputStream(new File(originFilename));
                if (!existFile(pathname)) {
                    createDirecroty(pathname);
                }
                ftpClient.changeWorkingDirectory(pathname);
                flag = ftpClient.storeFile(fileName, inputStream);

                if (!flag) {
                    Log.i("ftp", "ReplyCode: " + ftpClient.getReplyCode());
                    Log.i("ftp", "Reply: " + ftpClient.getReplyString());
                }
                if (flag) LogUtil.INSTANCE.i("=======上传文件成功");
                //向控制器发送同步指令
                Log.i("ftp", "ip:  " + MMKV.defaultMMKV().decodeString(KEY_NEY_IP, "192.168.3.101"));
                SFTPUtil.exeLinuxBySSH(MMKV.defaultMMKV().decodeString(KEY_NEY_IP, "192.168.3.101"), 22, MRC05_USER_NAME, MRC05_PWD, "sync");
                ftpClient.logout();
            }
        } catch (Exception e) {
            //Log.d("catch", "uploadFile: ");
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    /**
     * 上传文件
     *
     * @param pathname    ftp服务保存地址
     * @param fileName    上传到ftp的文件名
     * @param inputStream 输入文件流
     * @return
     */
    public boolean uploadFile(String pathname, String fileName, InputStream inputStream) {
        boolean flag = false;
        try {
            if (initFtpClient()) {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                if (!existFile(pathname)) {
                    createDirecroty(pathname);
                }
                ftpClient.changeWorkingDirectory(pathname);
                ftpClient.storeFile(fileName, inputStream);
                inputStream.close();
                ftpClient.logout();
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    //改变目录路径
    public boolean changeWorkingDirectory(String directory) {
        boolean flag = true;
        try {
            flag = ftpClient.changeWorkingDirectory(directory);
            if (flag) {
                System.out.println("进入文件夹" + directory + " 成功！");

            } else {
                System.out.println("进入文件夹" + directory + " 失败！开始创建文件夹");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return flag;
    }

    //创建多层目录文件，如果有ftp服务器已存在该文件，则不创建，如果无，则创建
    public boolean createDirecroty(String remote) throws IOException {
        String directory = remote + "/";
        // 如果远程目录不存在，则递归创建远程服务器目录
        if (!directory.equalsIgnoreCase("/") && !changeWorkingDirectory(directory)) {
            int start = 0;
            int end = 0;
            if (directory.startsWith("/")) {
                start = 1;
            }
            end = directory.indexOf("/", start);
            String path = "";
            StringBuilder paths = new StringBuilder();
            while (true) {
                String subDirectory = new String(remote.substring(start, end).getBytes("GBK"), "iso-8859-1");
                path = path + "/" + subDirectory;
                if (!existFile(path)) {
                    if (makeDirectory(subDirectory)) {
                        changeWorkingDirectory(subDirectory);
                    } else {
                        System.out.println("创建目录[" + subDirectory + "]失败");
                        changeWorkingDirectory(subDirectory);
                    }
                } else {
                    changeWorkingDirectory(subDirectory);
                }
                paths.append("/").append(subDirectory);
                start = end + 1;
                end = directory.indexOf("/", start);
                // 检查所有目录是否创建完毕
                if (end <= start) {
                    break;
                }
            }
        }
        return true;
    }

    //判断ftp服务器文件是否存在
    public boolean existFile(String path) throws IOException {
        boolean flag = false;
        FTPFile[] ftpFileArr = ftpClient.listFiles(path);
        if (ftpFileArr.length > 0) {
            flag = true;
        }
        return flag;
    }

    //创建目录
    public boolean makeDirectory(String dir) {
        boolean flag = true;
        try {
            flag = ftpClient.makeDirectory(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 获取文件夹下所有文件名 *
     *
     * @param pathname FTP服务器文件目录 * media/sda1/mapfile
     * @return
     */
    public List<String> getFileAllName(String pathname) {
        List<String> listName = new ArrayList<>();
        try {
            if (initFtpClient()) {
                //切换FTP目录
                ftpClient.changeWorkingDirectory(pathname);
                FTPFile[] ftpFiles = ftpClient.listFiles();
                for (FTPFile file : ftpFiles) {
                    listName.add(file.getName());
                }
                ftpClient.logout();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return listName;
    }

    /**
     * 下载文件夹下所有文件 *
     *
     * @param pathname  FTP服务器文件目录 * media/sda1/mapfile
     * @param localpath 下载后的文件路径 *
     * @return
     */
    public boolean downloadFileAll(String pathname, String localpath) {
        boolean flag = false;
        OutputStream os = null;
        try {
            boolean initFtpClient = initFtpClient();
            LogUtil.INSTANCE.d("下载pad音频文件 初始化ftp服务器" + initFtpClient);
            if (initFtpClient) {
                //切换FTP目录
                ftpClient.changeWorkingDirectory(pathname);
                FTPFile[] ftpFiles = ftpClient.listFiles();
                for (FTPFile file : ftpFiles) {
                    File localFile = new File(localpath + "/" + file.getName());
                    os = new FileOutputStream(localFile);
                    ftpClient.retrieveFile(file.getName(), os);
                    flag = true;
                }
                ftpClient.logout();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != os) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    /**
     * 下载文件 *
     *
     * @param pathname  FTP服务器文件目录 *
     * @param filename  文件名称 *
     * @param localpath 下载后的文件路径 *
     * @return
     */
    public synchronized boolean downloadFile(String pathname, String filename, String localpath) {
        boolean flag = false;
        OutputStream os = null;
        try {
            LogUtil.INSTANCE.i("=======开始下载");
            LogUtil.INSTANCE.i("FTP服务器文件目录  " + pathname);
            LogUtil.INSTANCE.i("文件名称  " + filename);
            LogUtil.INSTANCE.i("下载后的文件路径  " + localpath);
            boolean initFtpClient = initFtpClient();
            LogUtil.INSTANCE.d("初始化ftp服务器" + initFtpClient);
            if (initFtpClient) {
                //切换FTP目录
                boolean changeWorking = ftpClient.changeWorkingDirectory(pathname);
                LogUtil.INSTANCE.d("切换FTP目录" + changeWorking);

                FTPFile[] ftpFiles = ftpClient.listFiles();
                LogUtil.INSTANCE.d("ftpFiles.length" + ftpFiles.length);
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

                for (FTPFile file : ftpFiles) {
                    if (filename.equalsIgnoreCase(file.getName())) {
                        File localFile = new File(localpath + "/" + file.getName());
                        os = new FileOutputStream(localFile);
                        ftpClient.retrieveFile(file.getName(), os);
                        LogUtil.INSTANCE.i("=======结束下载");
                        flag = true;
                        break;
                    }
                }
                ftpClient.logout();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.INSTANCE.i(" initFtpClient() e" + e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.INSTANCE.i("ftpClient.isConnected() e" + e);
                }
            }
            if (null != os) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.INSTANCE.i("OutputStream() e" + e);
                }
            }
        }
        return flag;
    }

    /**
     * 下载文件 *
     *
     * @param pathname  FTP服务器文件目录 * media/sda1/mapfile
     * @param filename  文件名称 *
     * @param localpath 下载后的文件路径 *
     * @return
     */
    public synchronized boolean downloadZipFile(String pathname, String filename, String localpath) {
        boolean flag = false;
        OutputStream os = null;
        LogUtil.INSTANCE.i("=======开始下载");
        LogUtil.INSTANCE.i("FTP服务器文件目录  " + pathname);
        LogUtil.INSTANCE.i("文件名称  " + filename);
        LogUtil.INSTANCE.i("下载后的文件路径  " + localpath);
        try {
            boolean initFtpClient = initFtpClient();
            LogUtil.INSTANCE.i("初始化ftp服务器" + initFtpClient);
            if (initFtpClient) {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                //切换FTP目录
                ftpClient.changeWorkingDirectory(pathname);
                FTPFile[] ftpFiles = ftpClient.listFiles();
                for (FTPFile file : ftpFiles) {
                    if (filename.equalsIgnoreCase(file.getName())) {
                        File localFile = new File(localpath + "/" + file.getName());
                        os = new FileOutputStream(localFile);
//                        os = new ZipOutputStream(new FileOutputStream(localFile));
                        ftpClient.retrieveFile(file.getName(), os);
                        flag = true;
                        break;
                    }
                }
                ftpClient.logout();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.INSTANCE.i("initFtpClient() e" + e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.INSTANCE.i("ftpClient.isConnected() e" + e);
                }
            }
            if (null != os) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.INSTANCE.i("OutputStream() e" + e);
                }
            }
        }
        return flag;
    }

    /**
     * 删除文件夹下所有文件*
     *
     * @param pathname FTP服务器保存目录 *
     * @param filename 要删除的文件名称 *
     * @return
     */
    public boolean delDirFileList(String pathname, String filename) {
        boolean flag = false;
        try {
            initFtpClient();
            //切换FTP目录
            ftpClient.changeWorkingDirectory(pathname);
            FTPFile[] ftpFileList = ftpClient.listFiles(filename);
            if (ftpFileList != null && ftpFileList.length > 0) {
                for (FTPFile file : ftpFileList) {
                    if (file.isFile()) {
                        Log.d("delete", "fileName: " + file.getName());
                        try {
                            ftpClient.deleteFile(filename + file.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            ftpClient.logout();
            flag = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }


    /**
     * 删除文件 *
     *
     * @param pathname FTP服务器保存目录 *
     * @param filename 要删除的文件名称 *
     * @return
     */
    public boolean deleteFile(String pathname, String filename) {
        boolean flag = false;
        try {
            initFtpClient();
            //切换FTP目录
            ftpClient.changeWorkingDirectory(pathname);
            ftpClient.dele(filename);
            ftpClient.logout();
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }
}