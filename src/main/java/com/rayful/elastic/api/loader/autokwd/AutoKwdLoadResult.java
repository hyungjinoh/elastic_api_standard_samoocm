package com.rayful.elastic.api.loader.autokwd;

import java.util.ArrayList;
import java.util.List;

/**
 * sql-batches/*.sql 일괄 실행 결과 요약.
 */
public class AutoKwdLoadResult {

    private int totalFiles;
    private int successCount;
    private int failCount;
    private long affectedRows;
    private long elapsedMs;
    private final List<FileResult> files = new ArrayList<>();

    public void addSuccess(String fileName, int affected, long elapsedMs) {
        this.successCount++;
        this.affectedRows += affected;
        this.files.add(new FileResult(fileName, true, affected, elapsedMs, null));
    }

    public void addFailure(String fileName, long elapsedMs, String errorMessage) {
        this.failCount++;
        this.files.add(new FileResult(fileName, false, 0, elapsedMs, errorMessage));
    }

    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

    public int getSuccessCount() { return successCount; }
    public int getFailCount() { return failCount; }
    public long getAffectedRows() { return affectedRows; }

    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }

    public List<FileResult> getFiles() { return files; }

    /** 파일 1건(batch_NNN.sql) 처리 결과. */
    public static class FileResult {
        private final String fileName;
        private final boolean success;
        private final int affectedRows;
        private final long elapsedMs;
        private final String errorMessage;

        public FileResult(String fileName, boolean success, int affectedRows, long elapsedMs, String errorMessage) {
            this.fileName = fileName;
            this.success = success;
            this.affectedRows = affectedRows;
            this.elapsedMs = elapsedMs;
            this.errorMessage = errorMessage;
        }

        public String getFileName() { return fileName; }
        public boolean isSuccess() { return success; }
        public int getAffectedRows() { return affectedRows; }
        public long getElapsedMs() { return elapsedMs; }
        public String getErrorMessage() { return errorMessage; }
    }
}
