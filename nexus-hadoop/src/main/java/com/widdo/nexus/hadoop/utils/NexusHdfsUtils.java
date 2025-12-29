package com.widdo.nexus.hadoop.utils;

import lombok.Data;
import org.apache.hadoop.fs.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NexusHdfsUtils {

    /**
     * 文件信息类
     */
    @Data
    public static class FileInfo {
        private String path;
        private String name;
        private long size;
        private boolean isDirectory;
        private String permissions;
        private String owner;
        private String group;
        private Date modificationTime;
        private long blockSize;
        private short replication;
    }

    /**
     * 通用的 ls 方法，可以选择是否包含目录
     */
    public static List<FileInfo> ls(FileSystem fs, Path path,
                                          boolean recursive,
                                          boolean includeDirectories) throws IOException {
        List<FileInfo> results = new ArrayList<>();
        
        if (recursive) {
            // 递归模式
            if (includeDirectories) {
                // 包含目录的递归列表
                listRecursiveWithDirs(fs, path, results);
            } else {
                // 只包含文件的递归列表
                RemoteIterator<LocatedFileStatus> iterator = fs.listFiles(path, true);
                while (iterator.hasNext()) {
                    results.add(convertToFileInfo(iterator.next()));
                }
            }
        } else {
            // 非递归模式
            FileStatus[] statuses = fs.listStatus(path);
            for (FileStatus status : statuses) {
                if (includeDirectories || !status.isDirectory()) {
                    results.add(convertToFileInfo(status));
                }
            }
        }
        
        return results;
    }
    
    /**
     * 递归列出所有内容（包括目录）
     */
    private static void listRecursiveWithDirs(FileSystem fs, Path path, 
                                              List<FileInfo> results) throws IOException {
        FileStatus[] statuses = fs.listStatus(path);
        
        for (FileStatus status : statuses) {
            results.add(convertToFileInfo(status));
            
            // 如果是目录，递归处理
            if (status.isDirectory()) {
                listRecursiveWithDirs(fs, status.getPath(), results);
            }
        }
    }
    
    /**
     * 转换为文件信息对象
     */
    private static FileInfo convertToFileInfo(FileStatus status) {
        FileInfo info = new FileInfo();
        info.setPath(status.getPath().toString());
        info.setName(status.getPath().getName());
        info.setSize(status.getLen());
        info.setDirectory(status.isDirectory());
        info.setPermissions(status.getPermission().toString());
        info.setOwner(status.getOwner());
        info.setGroup(status.getGroup());
        info.setModificationTime(new Date(status.getModificationTime()));
        
        if (status instanceof LocatedFileStatus) {
            LocatedFileStatus located = (LocatedFileStatus) status;
            info.setBlockSize(located.getBlockSize());
            info.setReplication(located.getReplication());
        }
        
        return info;
    }
}