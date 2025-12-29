package com.widdo.nexus.hadoop.resolver;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

public class NexusHdfsPathResolver {
    
    /**
     * 解析HDFS路径，如果为空则返回默认路径
     * @param fs FileSystem实例
     * @param path 用户输入的路径（可能为空）
     * @return 处理后的有效路径
     */
    public static Path resolvePath(FileSystem fs, String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            // 返回当前用户的主目录
            return new Path(fs.getHomeDirectory().toUri().getPath());
        }
        
        path = path.trim();
        
        // 处理相对路径（相对于home目录）
        if (!path.startsWith("/")) {
            Path home = fs.getHomeDirectory();
            return new Path(home, path);
        }
        
        return new Path(path);
    }
    
    /**
     * 解析命令中的路径参数
     * @param fs FileSystem实例
     * @param command 完整的命令字符串
     * @return 解析后的路径
     */
    public static Path extractAndResolvePath(FileSystem fs, String command) throws IOException {
        // 解析命令，提取路径部分
        String path = extractPathFromCommand(command);
        return resolvePath(fs, path);
    }
    
    /**
     * 从命令中提取路径
     */
    private static String extractPathFromCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "";
        }
        
        command = command.trim();
        
        // 简单解析：获取最后一个非选项参数
        String[] parts = command.split("\\s+");
        
        // 找到ls命令的位置
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("-ls") || parts[i].equals("ls")) {
                // 检查后面的参数
                for (int j = i + 1; j < parts.length; j++) {
                    if (!parts[j].startsWith("-")) {
                        // 找到第一个非选项参数作为路径
                        return parts[j];
                    }
                }
                break;
            }
        }
        
        return "";
    }
}