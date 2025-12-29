package com.widdo.nexus.hadoop.adapter;

import com.google.common.collect.Lists;
import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.exception.NexusExceptionHelper;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.support.template.NexusQueryContext;
import com.widdo.nexus.hadoop.command.NexusHdfsCommand;

import java.util.List;
import java.util.Map;

/**
 * NexusHdfsAdapter
 * <p>
 * hdfs私有的接口
 *
 * @author XYL
 * @date 2025/12/09 15:25
 * @since 0.0.1-SNAPSHOT
 */
public class NexusHdfsAdapter implements NexusHadoopAdapter {

    public NexusHdfsAdapter(String hdfsNnInsideAddr, String username) {
        NexusHdfsCommand.init(hdfsNnInsideAddr, username);
    }

    @Override
    public String getDatabaseType() {
        return "HDFS";
    }

    @Override
    public NexusResult execute(String query, Map<String, Object> parameters, NexusQueryContext context) {
        final Map<String, Object> params = (Map<String, Object>) parameters.getOrDefault("params", Map.of());

        final String username = parameters.getOrDefault("username", "root").toString();

        String source = params.getOrDefault("source", "").toString();
        final String target = params.getOrDefault("target", "").toString();

        final String command = context.getCommandInfo().getCommand();

        switch (command) {
            case "ls":
                return NexusHdfsCommand.ls(username, target);
            case "mkdir":
                return NexusHdfsCommand.mkdirs(username, target);
            case "put":
                final boolean delSrc = (boolean) params.getOrDefault("delSrc", false);
                final boolean overwrite = (boolean) params.getOrDefault("overwrite", false);
                final List<String> sourceList = (List<String>) params.getOrDefault("sourceList", Lists.newArrayList());

                return NexusHdfsCommand.put(username, delSrc, overwrite, sourceList, target);
            case "get":
                final boolean delSrc1 = (boolean) params.getOrDefault("delSrc", false);
                final boolean useRawLocalFileSystem = (boolean) params.getOrDefault("delSrc", false);

                return NexusHdfsCommand.get(username, delSrc1, source, target, useRawLocalFileSystem);
            case "rm":
                final boolean recursive = (boolean) params.getOrDefault("recursive", false);
                return NexusHdfsCommand.rm(target, recursive);
            case "mv":
                return NexusHdfsCommand.mv(source, target);
            case "cp":
                return NexusHdfsCommand.cp(source, target);
            default:
                throw NexusExceptionHelper.executionError("Hdfs executing error...");
        }
    }

    @Override
    public <T> T postExecute(NexusResult resultSet, Class<T> resultType) {
        return (T) resultSet;
    }


}
