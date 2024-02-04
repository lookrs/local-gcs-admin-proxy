package org.llk.gcsAdmin.controller;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.*;
import org.apache.commons.lang3.StringUtils;
import org.llk.gcsAdmin.dto.FileDescription;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("gcs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class GcsController {


    private final Storage storage;

    public GcsController() {
        storage = StorageOptions.newBuilder()
                .setHost("http://localhost:9023")
                .setProjectId("test-project")
                .setCredentials(NoCredentials.getInstance())
                .build().getService();
    }

    @GetMapping("/buckets")
    @ResponseBody
    public ResponseEntity<List<String>> listBuckets() {
        List<Bucket> buckets = storage.list().streamAll().toList();
        List<String> bucketNames = buckets.stream().map(Bucket::getName).collect(Collectors.toList());
        return ResponseEntity.ok(bucketNames);
    }

    @PostMapping("/files")
    @ResponseBody
    public ResponseEntity<List<FileDescription>> listFiles(@RequestBody Map<String, String> requestBody) {
        String bucketName = requestBody.get("bucketName");
        List<Blob> blobs = storage.get(bucketName).list().streamAll().toList();
        List<String> fileNames = blobs.stream().map(BlobInfo::getName).toList();
        List<FileDescription> targetList = fileNames.stream().map(this::getFileDescriptionInfo).distinct().toList();
        return ResponseEntity.ok(targetList);
    }

    private FileDescription getFileDescriptionInfo(String targetName) {
        Path path = Paths.get(targetName);
        String firstDeep = path.getName(0).toString();
        if (!StringUtils.contains(firstDeep, '.')) {
            return new FileDescription(firstDeep, false);
        } else {
            return new FileDescription(firstDeep, true);
        }
    }

    @PostMapping("/searchFiles")
    @ResponseBody
    public ResponseEntity<List<FileDescription>> searchFiles(@RequestBody Map<String, String> requestBody) {
        String bucketName = requestBody.get("bucketName");
        String prefix = requestBody.get("prefix");
        if (StringUtils.isEmpty(bucketName)) {
            return ResponseEntity.ok(List.of(new FileDescription("未选择bucket。", false)));
        }
        List<Blob> blobs = storage.get(bucketName).list(Storage.BlobListOption.prefix(prefix)).streamAll().toList();
        List<FileDescription> fileNames = blobs.stream().map(BlobInfo::getName).map(
                fileName -> {
                    String targetFileName;
                    if (StringUtils.isEmpty(prefix)) {
                        targetFileName = fileName;
                    } else {
                        targetFileName = fileName.substring(prefix.length() + 1);
                    }
                    return getFileDescriptionInfo(targetFileName);
                }).distinct().toList();
        return ResponseEntity.ok(fileNames);
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<String> uploadFile(@RequestParam String bucketName, @RequestParam String filePath, @RequestParam("file") MultipartFile file) throws IOException {
        BlobId blobId;
        if (StringUtils.isEmpty(filePath)) {
            blobId = BlobId.of(bucketName, Objects.requireNonNull(file.getOriginalFilename()));
        } else {
            blobId = BlobId.of(bucketName, filePath.concat("/").concat(Objects.requireNonNull(file.getOriginalFilename())));
        }
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        Blob blob = storage.create(blobInfo, file.getBytes());
        return ResponseEntity.ok("File uploaded successfully: " + blob.getName());
    }

    @GetMapping("/downloadFile")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String bucketName, @RequestParam String fileName) {
        if (StringUtils.isAnyEmpty(bucketName, fileName)) {
            return ResponseEntity.ok(new byte[0]);
        }
        BlobId blobId = BlobId.of(bucketName, fileName);
        Blob blob = storage.get(blobId);
        byte[] content = blob.getContent();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @GetMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam String bucketName, @RequestParam String fileName) {
        if (StringUtils.isAnyEmpty(bucketName, fileName)) {
            return ResponseEntity.ok("参数传递有误");
        }
        BlobId blobId = BlobId.of(bucketName, fileName);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            return ResponseEntity.ok("File deleted successfully: " + fileName);
        } else {
            return ResponseEntity.ok("File not found or unable to delete: " + fileName);
        }
    }

    @GetMapping("/createBucket")
    public ResponseEntity<String> createBucket(@RequestParam String bucketName) {
        Bucket bucket = storage.create(BucketInfo.newBuilder(bucketName).build());
        return ResponseEntity.ok("Bucket created successfully: " + bucket.getName());
    }
}
