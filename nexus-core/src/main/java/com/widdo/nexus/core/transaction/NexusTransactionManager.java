package com.widdo.nexus.core.transaction;

import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
import com.widdo.nexus.core.log.LogMessages;
import com.widdo.nexus.core.log.NexusLogger;
import org.slf4j.MDC;

public class NexusTransactionManager {
    private static final NexusLogger log = NexusLogger.getLogger(NexusTransactionManager.class);

    public NexusTransaction beginTransaction(NexusDatabaseAdapter adapter) {
        NexusTransaction transaction = adapter.beginTransaction();
        String txId = transaction.txId();

        MDC.put("transactionId", txId);
        log.debug(LogMessages.TRANSACTION_START, txId);
        MDC.remove("transactionId");

        return new LoggingTransaction(transaction, txId);
    }

    private class LoggingTransaction implements NexusTransaction {
        private final NexusTransaction delegate;
        private final String txId;

        public LoggingTransaction(NexusTransaction delegate, String txId) {
            this.delegate = delegate;
            this.txId = txId;
        }

        @Override
        public void commit() {
            MDC.put("transactionId", txId);
            try {
                delegate.commit();
                log.debug(LogMessages.TRANSACTION_COMMIT, txId);
            } finally {
                MDC.remove("transactionId");
            }
        }

        @Override
        public void rollback() {
            MDC.put("transactionId", txId);
            try {
                delegate.rollback();
                log.debug(LogMessages.TRANSACTION_ROLLBACK, txId);
            } finally {
                MDC.remove("transactionId");
            }
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() throws Exception {
            MDC.put("transactionId", txId);
            try {
                delegate.close();
                log.debug(LogMessages.TRANSACTION_CLOSE, txId);
            } finally {
                MDC.remove("transactionId");
            }
        }

        @Override
        public String txId() {
            return txId;
        }
    }
}