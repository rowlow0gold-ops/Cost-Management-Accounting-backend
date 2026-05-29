package com.demo.cost.security;

import org.springframework.web.multipart.MultipartFile;

/** Single point of policy for accepted uploads. Currently only Excel imports.
 *  Combined with spring.servlet.multipart.max-file-size for the byte limit. */
public final class UploadGuard {
    private UploadGuard() {}

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    public static void requireExcel(MultipartFile f) {
        if (f == null || f.isEmpty()) throw new IllegalArgumentException("파일이 비어 있습니다");
        if (f.getSize() > MAX_BYTES) throw new IllegalArgumentException("파일이 너무 큽니다 (최대 5MB)");
        String name = f.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("xlsx 파일만 업로드할 수 있습니다");
        }
        String ct = f.getContentType();
        if (ct != null && !ct.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                       && !ct.equals("application/octet-stream")) {
            throw new IllegalArgumentException("Excel 파일이 아닙니다");
        }
    }
}
