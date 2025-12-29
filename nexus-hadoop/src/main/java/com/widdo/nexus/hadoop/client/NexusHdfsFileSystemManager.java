package com.widdo.nexus.hadoop.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.atomic.AtomicInteger;

public class NexusHdfsFileSystemManager {
    private static volatile FileSystem fs;
    private static final AtomicInteger referenceCount = new AtomicInteger(0);
    private static final Object lock = new Object();

    /**
     * 获取 FileSystem 实例（线程安全）
     */
    public static FileSystem getFileSystem(String nnInsideAddr, String username) throws Exception {
        synchronized (lock) {
            if (fs == null) {
                Configuration conf = new Configuration();
                // 根据环境变量配置 HDFS
                if (nnInsideAddr != null && !nnInsideAddr.trim().isEmpty()) {
                    conf.set("fs.defaultFS", nnInsideAddr);
                }

                if (StringUtils.hasLength(username)) {

                    // 使用 UserGroupInformation 模拟用户
                    UserGroupInformation ugi = UserGroupInformation.createRemoteUser(username);

                    // 以指定用户身份执行操作
                    fs = ugi.doAs((PrivilegedExceptionAction<FileSystem>) () ->
                            FileSystem.get(conf));
                } else {
                    fs = FileSystem.get(conf);
                }
                referenceCount.set(0);
            }
            referenceCount.incrementAndGet();
            return fs;
        }
    }

    /**
     * 释放 FileSystem 引用
     */
    public static void releaseFileSystem() {
        synchronized (lock) {
            if (referenceCount.decrementAndGet() <= 0 && fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    System.err.println("Error closing FileSystem: " + e.getMessage());
                } finally {
                    fs = null;
                }
            }
        }
    }

    /**
     * 安全关闭所有资源（应用程序关闭时调用）
     */
    public static void shutdown() {
        synchronized (lock) {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    System.err.println("Error closing FileSystem during shutdown: " + e.getMessage());
                } finally {
                    fs = null;
                    referenceCount.set(0);
                }
            }
        }
    }
}