package io.notifyhub.core.attachment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Immutable file attachment for notifications (primarily email).
 *
 * <pre>{@code
 * // From bytes
 * Attachment pdf = new Attachment("report.pdf", pdfBytes, "application/pdf");
 *
 * // From file
 * Attachment invoice = Attachment.fromFile(new File("invoice.pdf"));
 *
 * // Usage
 * notify.to(user)
 *     .via(Channel.EMAIL)
 *     .subject("Your invoice")
 *     .content("Please find the invoice attached.")
 *     .attach(invoice)
 *     .send();
 * }</pre>
 */
public final class Attachment {

    private final String fileName;
    private final byte[] content;
    private final String mimeType;

    /**
     * Create an attachment from raw bytes.
     *
     * @param fileName the file name (e.g., "report.pdf")
     * @param content  the file content as bytes
     * @param mimeType the MIME type (e.g., "application/pdf", "image/png")
     */
    public Attachment(String fileName, byte[] content, String mimeType) {
        this.fileName = Objects.requireNonNull(fileName, "fileName must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null").clone();
        this.mimeType = Objects.requireNonNull(mimeType, "mimeType must not be null");
    }

    /**
     * Create an attachment from a {@link File}.
     * The MIME type is detected automatically from the file name.
     *
     * @param file the file to attach
     * @return a new Attachment
     * @throws IllegalArgumentException if the file cannot be read
     */
    public static Attachment fromFile(File file) {
        String mimeType = detectMimeType(file.getName());
        return fromFile(file, mimeType);
    }

    /**
     * Create an attachment from a {@link File} with explicit MIME type.
     *
     * @param file     the file to attach
     * @param mimeType the MIME type
     * @return a new Attachment
     * @throws IllegalArgumentException if the file cannot be read
     */
    public static Attachment fromFile(File file, String mimeType) {
        Objects.requireNonNull(file, "file must not be null");
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new Attachment(file.getName(), bytes, mimeType);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read file: " + file.getAbsolutePath(), e);
        }
    }

    /** The file name (e.g., "report.pdf"). */
    public String getFileName() {
        return fileName;
    }

    /** The file content as bytes. Returns a defensive copy. */
    public byte[] getContent() {
        return content.clone();
    }

    /** The MIME type (e.g., "application/pdf"). */
    public String getMimeType() {
        return mimeType;
    }

    /** Content length in bytes. */
    public int getSize() {
        return content.length;
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "fileName='" + fileName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", size=" + content.length +
                '}';
    }

    private static String detectMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }
}
