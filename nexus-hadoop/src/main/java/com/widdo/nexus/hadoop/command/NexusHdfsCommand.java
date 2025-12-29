package com.widdo.nexus.hadoop.command;

import com.widdo.nexus.core.result.IResultInterface;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.hadoop.client.NexusHdfsClient;
import com.widdo.nexus.hadoop.client.NexusHdfsFileSystemManager;
import com.widdo.nexus.hadoop.resolver.NexusHdfsPathResolver;
import com.widdo.nexus.hadoop.utils.NexusHdfsUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.List;

/**
 * NexusHdfsCommand
 *
 * @author XYL
 * @date 2025/12/16 18:30
 * @since 0.0.1-SNAPSHOT
 */
public class NexusHdfsCommand {

    private static FileSystem fs;

    private static String hdfsNnInsideAddr;

    public static void init(String hdfsNnInsideAddr, String username) {
        try {
            NexusHdfsCommand.fs = NexusHdfsFileSystemManager.getFileSystem(hdfsNnInsideAddr, username);
            NexusHdfsCommand.hdfsNnInsideAddr = hdfsNnInsideAddr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * ls.
     *
     * @param username 用户
     * @param path     路径
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/16 18:41:04
     */
    public static NexusResult ls(String username, String path) {
        try {

            if (fs == null) {
                fs = NexusHdfsFileSystemManager.getFileSystem(hdfsNnInsideAddr, username);
            }

            //提取path
            final Path path1 = NexusHdfsPathResolver.extractAndResolvePath(fs, path);

            final List<NexusHdfsUtils.FileInfo> fileInfos = NexusHdfsUtils.ls(fs, path1, true, true);

            return NexusResult.response(IResultInterface.Hadoop.SUCCESS, fileInfos);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 创建目录
     *
     * @param username username
     * @param path     path
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/10 15:23:56
     */
    public static NexusResult mkdirs(String username, String path) {
        // 目录 /xiyou/huaguoshan

        try {

            if (fs == null) {
                fs = NexusHdfsFileSystemManager.getFileSystem(hdfsNnInsideAddr, username);
            }

            fs.mkdirs(new Path(path));

            return NexusResult.response(IResultInterface.Hadoop.SUCCESS);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 上传文件
     *
     * @param username
     * @param delSrc
     * @param overwrite
     * @param sourceList
     * @param target
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/14 19:03:39
     */
    public static NexusResult put(String username, boolean delSrc, boolean overwrite, List<String> sourceList, String target) {
        try {

            if (fs == null) {
                fs = NexusHdfsFileSystemManager.getFileSystem(hdfsNnInsideAddr, username);
            }

            final Path[] paths = NexusHdfsClient.sources(sourceList);
            // 文件上传。参数一：是否删除元数据，参数二：是否允许覆盖，参数三：源文件路径，参数四：目标路径
            fs.copyFromLocalFile(delSrc, overwrite, paths, new Path(target));

            return NexusResult.response(IResultInterface.Hadoop.SUCCESS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下载
     *
     * @param username
     * @param source
     * @param target
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/14 19:10:44
     */
    public static NexusResult get(String username, boolean delSrc, String source, String target, boolean useRawLocalFileSystem) {
        try {
            if (fs == null) {
                fs = NexusHdfsFileSystemManager.getFileSystem(hdfsNnInsideAddr, username);
            }

            //处理source,如果为空则是当前用户主目录
            final Path path = NexusHdfsPathResolver.resolvePath(fs, source);

            // 文件上传。参数一：是否删除元数据，参数二：源文件路径，参数三：目标路径，参数四：？
            fs.copyToLocalFile(delSrc, path, new Path(target), useRawLocalFileSystem);

            return NexusResult.response(IResultInterface.Hadoop.SUCCESS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除.
     *
     * @param path      文件路径
     * @param recursive 是否递归删除
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/17 17:47:36
     */
    public static NexusResult rm(String path, boolean recursive) {

        try {
            final boolean delete = fs.delete(new Path(path), recursive);
            return NexusResult.response(IResultInterface.Hadoop.SUCCESS, delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * hdfs dfs -mv 命令。
     * 把文件移动到别的目录时，需要保证新目录存在
     * 对应的API，包括 rename
     *
     * @param source
     * @param target
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/17 18:58:53
     */
    public static NexusResult mv(String source, String target) {
        try {
            //确保目标路径存在
            final Path targetPath = new Path(target);
            final Path parent = targetPath.getParent();
            fs.mkdirs(parent);

            final boolean rename = fs.rename(new Path(source), targetPath);
            return NexusResult.response(IResultInterface.Hadoop.SUCCESS, rename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * hdfs dfs -cp
     *
     * @param source
     * @param target
     * @return com.widdo.nexus.core.result.NexusResult
     * @author XYL
     * @date 2025/12/17 19:12:36
     */
    public static NexusResult cp(String source, String target) {
        try {
            final Path src = new Path(source);
            final Path dst = new Path(target);
            copyDirectory(fs, src, dst);
            return NexusResult.response(IResultInterface.Hadoop.SUCCESS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 递归复制整个目录
    public static void copyDirectory(FileSystem fs, Path src, Path dst) throws IOException {
        FileStatus[] statuses = fs.listStatus(src);
        fs.mkdirs(dst);

        for (FileStatus status : statuses) {
            Path srcPath = status.getPath();
            Path dstPath = new Path(dst, srcPath.getName());

            if (status.isDirectory()) {
                copyDirectory(fs, srcPath, dstPath);
            } else {
                FileUtil.copy(fs, srcPath, fs, dstPath, false, true,
                        fs.getConf());
            }
        }
    }
}
