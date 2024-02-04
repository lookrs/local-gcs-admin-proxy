package org.llk.gcsAdmin.service;

import com.google.cloud.NoCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GcsUtil {

    private static Storage storageClient;

    static  {
        String fakeGcsExternalUrl = "http://localhost:9023";
        storageClient = StorageOptions.newBuilder()
                .setHost(fakeGcsExternalUrl)
                .setProjectId("test-project")
                .setCredentials(NoCredentials.getInstance())
                .build().getService();
    }

    public static boolean createBucket(String bucketName) {
        Bucket bucket = storageClient.create(BucketInfo.newBuilder(bucketName).build());
        return Objects.nonNull(bucket);
    }

    public static List<String> getAllFilesByBucket(String bucketName, String filePrefix) {
        File folder = new File(filePrefix);
        String[] files = folder.list();
        if (files != null) {
            return Arrays.asList(files);
        } else {
            return null;
        }
    }

    public static boolean uploadFile(String bucketName, String fileName, byte[] content) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        Blob blob = storageClient.create(blobInfo, content);
        return Objects.nonNull(blob);
    }

    public static boolean deleteFile(String bucketName, String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        return storageClient.delete(blobId);
    }

    public static void shouldUploadFileByWriterChannel1() throws IOException {

//        storageClient.create(BucketInfo.newBuilder("sample-bucket2").build());

        WriteChannel channel = storageClient.writer(BlobInfo.newBuilder("sample-bucket2", "path1/path2/abcd1.txt").build());
        channel.write(ByteBuffer.wrap("line1\n".getBytes()));
        channel.write(ByteBuffer.wrap("line2\n".getBytes()));
        channel.close();
    }

    public static void main(String[] args) throws IOException {
        shouldUploadFileByWriterChannel1();
    }
}
