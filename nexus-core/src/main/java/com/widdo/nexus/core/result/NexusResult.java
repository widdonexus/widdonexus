package com.widdo.nexus.core.result;

import com.widdo.nexus.core.exception.NexusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * widdo result wrapper.
 *
 * @author XYL
 * @date 2022/12/29 17:47
 * @since 263.1.1.0
 */
@SuppressWarnings("ALL")
public class NexusResult extends HashMap<String, Object> {

    /**
     * the key of result called code.
     */
    public static final String CODE = "code";
    /**
     * the key of result called msg.
     */
    public static final String MSG = "msg";
    /**
     * the key of result called data.
     */
    public static final String DATA = "data";
    private static final Logger LOG = LoggerFactory.getLogger(NexusResult.class);

    /**
     * constructor has one param called msg.
     *
     * @param msg msg
     */
    public NexusResult(final String msg) {
        this.put(MSG, msg);
    }

    /**
     * constructor has two params,one called code,another called msg.
     *
     * @param code code
     * @param msg  msg
     */
    public NexusResult(final String code, final String msg) {
        this.put(CODE, code);
        this.put(MSG, msg);
    }

    /**
     * constructor has three params, one called code, one called msg,another called data.
     *
     * @param code code
     * @param msg  msg
     * @param data data
     */
    public NexusResult(final String code, final String msg, final Object data) {
        this.put(CODE, code);
        this.put(MSG, msg);
        this.put(DATA, data);
    }

    /**
     * the method which have one param called {@link IResultInterface} to build
     * NexusResult.
     *
     * @param iResultInterface iResultInterface
     * @return an instance type of widdoResult
     */
    public static NexusResult response(IResultInterface iResultInterface) {
        return new NexusResult(iResultInterface.getCode(), iResultInterface.getMsg());
    }

    /**
     * the method which has two params,one called {@link IResultInterface},another called
     * data.
     *
     * @param iResultInterface iResultInterface
     * @param data             data
     * @return an instance type of widdoResult
     */
    public static NexusResult response(IResultInterface iResultInterface, Object data) {
        return new NexusResult(iResultInterface.getCode(), iResultInterface.getMsg(), data);
    }

    /**
     * the method which has one param, called {@link BaseException}.
     *
     * @param baseException baseException
     * @return an instance type of widdoResult
     */
    public static NexusResult response(NexusException baseException) {
        return new NexusResult(baseException.getErrorCode(), baseException.getMessage());
    }
}
