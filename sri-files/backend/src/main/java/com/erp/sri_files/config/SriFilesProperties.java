package com.erp.sri_files.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sri-files")
public class SriFilesProperties {

    private final Processing processing = new Processing();
    private final Storage storage = new Storage();
    private final Sri sri = new Sri();

    public Processing getProcessing() {
        return processing;
    }

    public Storage getStorage() {
        return storage;
    }

    public Sri getSri() {
        return sri;
    }

    public static class Processing {
        private int batchSize = 20;
        private int maxRetries = 5;
        private String recibidosCron = "0 */1 * * * *";

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public String getRecibidosCron() {
            return recibidosCron;
        }

        public void setRecibidosCron(String recibidosCron) {
            this.recibidosCron = recibidosCron;
        }
    }

    public static class Storage {
        private String root = "data/sri-files";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    public static class Sri {
        private short defaultEnvironment = 1;

        public short getDefaultEnvironment() {
            return defaultEnvironment;
        }

        public void setDefaultEnvironment(short defaultEnvironment) {
            this.defaultEnvironment = defaultEnvironment;
        }
    }
}
