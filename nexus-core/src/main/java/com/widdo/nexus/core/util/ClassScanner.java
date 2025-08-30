package com.widdo.nexus.core.util;

import com.widdo.nexus.core.exception.NexusExceptionHelper;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * ClassScanner.
 * <p>
 * 类扫描器工具，用于查找指定包下的所有类
 *
 * @author XYL
 * @date 2025/08/27 20:42
 * @since 0.0.1-SNAPSHOT
 */
public class ClassScanner {

    /**
     * 查找指定包下的所有类
     *
     * @param packageName 包名
     * @return 类集合
     */
    public static Set<Class<?>> findClasses(String packageName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            Set<Class<?>> classes = new HashSet<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    classes.addAll(findClassesInDirectory(new File(resource.getFile()), packageName));
                } else if ("jar".equals(resource.getProtocol())) {
                    // 处理JAR文件中的类（简化实现）
                    System.out.println("JAR scanning not fully implemented");
                }
            }

            return classes;
        } catch (Exception e) {
            throw NexusExceptionHelper.configError("Failed to scan package: " + packageName, e);
        }
    }

    /**
     * 在目录中查找类
     */
    private static Set<Class<?>> findClassesInDirectory(File directory, String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackageName = packageName + "." + file.getName();
                classes.addAll(findClassesInDirectory(file, subPackageName));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    System.err.println("Class not found: " + className);
                }
            }
        }

        return classes;
    }
}
