package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// 将MergeRequest类定义在Controller类外部
class MergeRequest {
    private String fileType;
    private String fileName;
    private int totalChunks;

    // Constructors
    public MergeRequest() {}

    public MergeRequest(String fileType, String fileName, int totalChunks) {
        this.fileType = fileType;
        this.fileName = fileName;
        this.totalChunks = totalChunks;
    }

    // Getters and Setters
    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    @Override
    public String toString() {
        return "MergeRequest{" +
                "fileType='" + fileType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", totalChunks=" + totalChunks +
                '}';
    }
}

@RestController
//@RequestMapping("/api")
public class ChunkUploadController {

    private static final Logger logger = LoggerFactory.getLogger(ChunkUploadController.class);

    @PostMapping("/chunk-upload")
    public Map<String, Object> chunkUpload(
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam("fileType") String fileType,
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkIndex") String chunkIndex,
            @RequestParam(value = "totalChunks", required = false) String totalChunks,
            @RequestParam(value = "fileSize", required = false) Long fileSize,
            @RequestParam(value = "identifier", required = false) String identifier,
            @RequestParam(value = "relativePath", required = false) String relativePath) {

        Map<String, Object> result = new HashMap<>();

        logger.info("Received chunk upload request: fileName={}, fileType={}, chunkIndex={}",
                fileName, fileType, chunkIndex);

        // 定义基础目录和临时目录
        String baseTempDir = "D:/软件/编程/Code/后台/server"; // 项目基础目录
        String tempDir;

        // 根据文件类型设置临时目录
        if ("image".equals(fileType)) {
            tempDir = baseTempDir + "/images/temp";
        } else if ("video".equals(fileType)) {
            tempDir = baseTempDir + "/videos/temp";
        } else {
            result.put("success", false);
            result.put("error", "不支持的文件类型");
            return result;
        }

        // 创建临时目录（如果不存在）
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 构建分片文件名
        String chunkFileName = fileName + ".part" + chunkIndex;
        Path destinationPath = Paths.get(tempDir, chunkFileName);

        // 保存分片文件
        try {
            chunk.transferTo(destinationPath);
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("chunkIndex", chunkIndex);
            result.put("totalChunks", totalChunks);
            result.put("fileSize", fileSize);
            result.put("identifier", identifier);
            result.put("relativePath", relativePath);
            result.put("path", destinationPath.toString().replace("\\", "/"));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "分片保存失败: " + e.getMessage());
            logger.error("Chunk save failed", e);
        }

        return result;
    }

    // 同时支持@RequestParam和@RequestBody两种方式
    @PostMapping("/merge-chunks")
    public Map<String, Object> mergeChunks(
            @RequestParam(value = "fileType", required = false) String fileTypeParam,
            @RequestParam(value = "fileName", required = false) String fileNameParam,
            @RequestParam(value = "totalChunks", required = false) Integer totalChunksParam,
            @RequestBody(required = false) MergeRequest mergeRequest) {

        logger.info("Received merge request: fileTypeParam={}, fileNameParam={}, totalChunksParam={}, mergeRequest={}",
                fileTypeParam, fileNameParam, totalChunksParam, mergeRequest);

        Map<String, Object> result = new HashMap<>();

        // 确定参数来源（优先使用RequestBody，其次是RequestParam）
        String fileType, fileName;
        int totalChunks;

        if (mergeRequest != null) {
            fileType = mergeRequest.getFileType();
            fileName = mergeRequest.getFileName();
            totalChunks = mergeRequest.getTotalChunks();
        } else if (fileTypeParam != null && fileNameParam != null && totalChunksParam != null) {
            fileType = fileTypeParam;
            fileName = fileNameParam;
            totalChunks = totalChunksParam;
        } else {
            result.put("success", false);
            result.put("error", "缺少必需参数: fileType, fileName, totalChunks");
            logger.warn("Missing required parameters in merge request");
            return result;
        }

        // 参数验证
        if (fileType == null || fileType.isEmpty()) {
            result.put("success", false);
            result.put("error", "文件类型不能为空");
            logger.warn("File type is empty");
            return result;
        }

        if (fileName == null || fileName.isEmpty()) {
            result.put("success", false);
            result.put("error", "文件名不能为空");
            logger.warn("File name is empty");
            return result;
        }

        if (totalChunks <= 0) {
            result.put("success", false);
            result.put("error", "分片总数必须大于0");
            logger.warn("Invalid totalChunks: {}", totalChunks);
            return result;
        }

        // 定义基础目录和临时目录
        String baseDir = "D:/软件/编程/Code/后台/server"; // 项目基础目录
        String tempDir;
        String targetDir;

        // 根据文件类型设置目录路径，确保与分片上传时使用相同的路径
        if ("image".equals(fileType)) {
            tempDir = baseDir + "/images/temp";
            targetDir = baseDir + "/images";
        } else if ("video".equals(fileType)) {
            tempDir = baseDir + "/videos/temp";
            targetDir = baseDir + "/videos";
        } else {
            result.put("success", false);
            result.put("error", "不支持的文件类型: " + fileType);
            logger.warn("Unsupported file type: {}", fileType);
            return result;
        }

        logger.info("Using temp directory: {}", tempDir);
        logger.info("Using target directory: {}", targetDir);

        // 创建目标目录（如果不存在）
        File dir = new File(targetDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                result.put("success", false);
                result.put("error", "无法创建目标目录: " + targetDir);
                logger.error("Failed to create target directory: {}", targetDir);
                return result;
            }
        }

        // 获取文件名和扩展名
        String ext = "";
        String baseName = fileName;
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            ext = fileName.substring(lastDotIndex);
            baseName = fileName.substring(0, lastDotIndex);
        } else {
            logger.warn("File name does not contain extension: {}", fileName);
        }

        String finalFileName = baseName + "_" + System.currentTimeMillis() + ext;
        Path finalFilePath = Paths.get(targetDir, finalFileName);

        // 合并分片文件
        try {
            // 检查所有分片是否存在
            logger.info("Checking for {} chunks in directory: {}", totalChunks, tempDir);
            for (int i = 0; i < totalChunks; i++) {
                String chunkFileName = fileName + ".part" + i;
                Path chunkPath = Paths.get(tempDir, chunkFileName);

                logger.debug("Checking for chunk file: {}", chunkPath);

                if (!Files.exists(chunkPath)) {
                    String errorMsg = String.format("缺少分片: %s (完整路径: %s, 分片索引: %d)",
                            chunkFileName, chunkPath.toString(), i);
                    throw new IOException(errorMsg);
                }
            }

            Files.createFile(finalFilePath); // 创建最终文件
            logger.info("Created final file: {}", finalFilePath);

            try (OutputStream output = Files.newOutputStream(finalFilePath)) {
                for (int i = 0; i < totalChunks; i++) {
                    String chunkFileName = fileName + ".part" + i;
                    Path chunkPath = Paths.get(tempDir, chunkFileName);

                    if (!Files.exists(chunkPath)) {
                        String errorMsg = String.format("缺少分片: %s (完整路径: %s, 分片索引: %d)",
                                chunkFileName, chunkPath.toString(), i);
                        throw new IOException(errorMsg);
                    }

                    // 读取并写入分片
                    byte[] chunkData = Files.readAllBytes(chunkPath);
                    output.write(chunkData);
                    logger.debug("Written chunk {} to final file", i);

                    // 删除已合并的分片
                    Files.delete(chunkPath);
                    logger.debug("Deleted chunk file: {}", chunkPath);
                }
            }

            // 构建成功响应
            result.put("success", true);
            result.put("fileName", finalFileName);
            result.put("path", finalFilePath.toString().replace("\\", "/"));
            logger.info("Successfully merged file: {}", finalFileName);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "合并分片失败: " + e.getMessage());
            logger.error("Merge chunks failed", e);
        }

        return result;
    }
}
