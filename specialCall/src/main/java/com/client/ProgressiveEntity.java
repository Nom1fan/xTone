package com.client;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Mor on 17/09/2016.
 */
public class ProgressiveEntity implements HttpEntity {
    private HttpEntity httpEntity;
    private long fileSize;
    private ProgressListener progressListener;

    public ProgressiveEntity(HttpEntity httpEntity, long fileSize, ProgressListener progressListener) {
        this.httpEntity = httpEntity;
        this.fileSize = fileSize;
        this.progressListener = progressListener;
    }

    @Override
    public void consumeContent() throws IOException {
        httpEntity.consumeContent();
    }

    @Override
    public InputStream getContent() throws IOException,
            IllegalStateException {
        return httpEntity.getContent();
    }

    @Override
    public Header getContentEncoding() {
        return httpEntity.getContentEncoding();
    }

    @Override
    public long getContentLength() {
        return httpEntity.getContentLength();
    }

    @Override
    public Header getContentType() {
        return httpEntity.getContentType();
    }

    @Override
    public boolean isChunked() {
        return httpEntity.isChunked();
    }

    @Override
    public boolean isRepeatable() {
        return httpEntity.isRepeatable();
    }

    @Override
    public boolean isStreaming() {
        return httpEntity.isStreaming();
    } // CONSIDER put a _real_ delegator into here!

    @Override
    public void writeTo(OutputStream outstream) throws IOException {

        class ProxyOutputStream extends FilterOutputStream {
            /**
             * @author Stephen Colebourne
             */

            public ProxyOutputStream(OutputStream proxy) {
                super(proxy);
            }

            public void write(int idx) throws IOException {
                out.write(idx);
            }

            public void write(byte[] bts) throws IOException {
                out.write(bts);
            }

            public void write(byte[] bts, int st, int end) throws IOException {
                out.write(bts, st, end);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                out.close();
            }
        } // CONSIDER import this class (and risk more Jar File Hell)

        class ProgressiveOutputStream extends ProxyOutputStream {
            private long totalSent;
            private int percentage;
            private int lastPercentagePublished;

            private ProgressiveOutputStream(OutputStream proxy) {
                super(proxy);
                totalSent = 0;
            }

            public void write(byte[] bts, int st, int end) throws IOException {

                out.write(bts, st, end);
                totalSent += end;
                percentage = ((int) ((totalSent / (float) fileSize) * 100));
                if (percentage - lastPercentagePublished > 0) {
                    progressListener.reportProgress(percentage);
                    lastPercentagePublished = percentage;
                }
            }
        }

        httpEntity.writeTo(new ProgressiveOutputStream(outstream));
    }

}
