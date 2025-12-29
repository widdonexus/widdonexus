package com.widdo.nexus.core.result;

/**
 * widdo result interface.
 *
 * @author XYL
 * @date 2022/12/29 17:39
 * @since 263.1.1.0
 */
@SuppressWarnings("ALL")
public interface IResultInterface {

    /**
     * return code of current result.
     *
     * @return java.lang.Integer
     * @author XYL
     * @date 2022/12/29 17:40:09
     **/
    String getCode();

    /**
     * return the message of current result.
     *
     * @return java.lang.String
     * @author XYL
     * @date 2022/12/29 17:40:41
     **/
    String getMsg();

    enum SysResult implements IResultInterface {

        /**
         * success.
         */
        SUCCESS("0", "成功"),

        /**
         * fail.
         */
        FAIL("-1", "失败"),

        /**
         * params error.
         */
        PARAMS_ERROR("-2", "参数异常"),

        /**
         * no access.
         */
        NO_ACCESS("-3", "禁止访问");

        /**
         * code.
         */
        private final String code;

        /**
         * msg.
         */
        private final String msg;

        /**
         * constructor of has two params.One called:code,another called:msg.
         *
         * @param code code
         * @param msg  msg
         */
        SysResult(final String code, final String msg) {
            this.code = code;
            this.msg = msg;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }

    }

    enum StudyResult implements IResultInterface {

        /**
         * success.
         */
        SUCCESS("0", "成功"),

        /**
         * fail.
         */
        FAIL("-1", "失败");

        /**
         * code.
         */
        private final String code;

        /**
         * msg.
         */
        private final String msg;

        /**
         * constructor of has two params.One called:code,another called:msg.
         *
         * @param code code
         * @param msg  msg
         */
        StudyResult(final String code, final String msg) {
            this.code = code;
            this.msg = msg;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }

    }

    enum LifeResult implements IResultInterface {

        /**
         * the result typed success of life.
         */
        SUCCESS("0", "成功"),

        /**
         * the result typed fail of life.
         */
        FAIL("-1", "失败");

        /**
         * code.
         */
        private final String code;

        /**
         * msg.
         */
        private final String msg;

        /**
         * constructor has two params,one called code,another called msg.
         *
         * @param code code
         * @param msg  msg
         */
        LifeResult(final String code, final String msg) {
            this.code = code;
            this.msg = msg;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }

    }

    enum Neo4j implements IResultInterface {

        /**
         * the result typed enum of neo4jResult.
         */
        SUCCESS("0", "成功"),

        /**
         * the result typed enum of neo4jResult.
         */
        FAIL("-1", "失败");

        /**
         * code.
         */
        private final String code;

        /**
         * msg.
         */
        private final String msg;

        /**
         * constructor has two params,one called code,another called msg.
         *
         * @param code code
         * @param msg  msg
         */
        Neo4j(final String code, final String msg) {
            this.code = code;
            this.msg = msg;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }

    }

    enum Hadoop implements IResultInterface {

        /**
         * the result typed enum of neo4jResult.
         */
        SUCCESS("0", "成功"),

        /**
         * the result typed enum of neo4jResult.
         */
        FAIL("-1", "失败");;

        /**
         * code.
         */
        private final String code;

        /**
         * msg.
         */
        private final String msg;

        /**
         * constructor has two params,one called code,another called msg.
         *
         * @param code code
         * @param msg  msg
         */
        Hadoop(final String code, final String msg) {
            this.code = code;
            this.msg = msg;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }

    }

}
