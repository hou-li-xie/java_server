package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
//@RequestMapping("/api")
public class SmartUploadController {

    @PostMapping("/smart-upload")
    public Map<String, Object> smartUpload(@RequestParam("files") MultipartFile[] files) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> summary = new HashMap<>();

        int totalFiles = files.length;
        int successCount = 0;
        long totalSize = 0L;

        List<Map<String, Object>> uploadedList = new ArrayList<>();
        List<Map<String, Object>> errorList = new ArrayList<>();

        // 图片扩展名列表
        List<String> imageExtensions = Arrays.asList(
                ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".tiff"
        );

        // 视频扩展名列表
        List<String> videoExtensions = Arrays.asList(
                ".mp4", ".webm", ".ogg", ".mov", ".avi", ".mkv", ".flv"
        );

        // ✅ 修改为你的实际路径
        String baseUploadDir = "D:/软件/编程/Code/后台/server"; // 保持 base 为项目根目录

        // ✅ 图片和视频目录改为指定路径
        String imageUploadDir = baseUploadDir + "/images"; // D:\软件\编程\Code\后台\server\images
        String videoUploadDir = baseUploadDir + "/videos"; // D:\软件\编程\Code\后台\server\videos

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                errorList.add(Map.of(
                        "filename", file.getOriginalFilename(),
                        "error", "文件为空"
                ));
                continue;
            }

            String originalName = file.getOriginalFilename();
            String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();

            // 判断文件类型并设置存储目录
            String fileType;
            String targetDir;

            if (imageExtensions.contains(ext)) {
                fileType = "image";
                targetDir = imageUploadDir;
            } else if (videoExtensions.contains(ext)) {
                fileType = "video";
                targetDir = videoUploadDir;
            } else {
                errorList.add(Map.of(
                        "filename", originalName,
                        "error", "不支持的文件类型: " + originalName
                ));
                continue;
            }

            // 创建目标目录
            File dir = new File(targetDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String basename = originalName.substring(0, originalName.lastIndexOf("."));
            String unique = System.currentTimeMillis() + "_" + Integer.toHexString(new Random().nextInt());
            String savedName = basename + "_" + unique + ext;

            // 保存文件
            try {
                Path destinationPath = Paths.get(targetDir, savedName);
                file.transferTo(destinationPath);

                // 统计信息
                long size = file.getSize();
                totalSize += size;
                successCount++;

                // 构建返回信息
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("originalName", originalName);
                fileInfo.put("savedName", savedName);
                fileInfo.put("size", size);
                fileInfo.put("sizeFormatted", formatFileSize(size));
                fileInfo.put("path", destinationPath.toString().replace("\\", "/"));
                fileInfo.put("mimeType", file.getContentType());
                fileInfo.put("uploadedAt", new Date());

                uploadedList.add(fileInfo);
            } catch (Exception e) {
                errorList.add(Map.of(
                        "filename", originalName,
                        "error", "文件保存失败: " + e.getMessage()
                ));
            }
        }

        // 构建返回结果
        summary.put("total", totalFiles);
        summary.put("success", successCount);
        summary.put("failed", totalFiles - successCount);
        summary.put("totalSize", totalSize);
        summary.put("totalSizeFormatted", formatFileSize(totalSize));

        result.put("success", errorList.isEmpty());
        result.put("uploaded", uploadedList);
        result.put("errors", errorList);
        result.put("summary", summary);

        return result;
    }

    // 文件大小格式化方法
    private String formatFileSize(long size) {
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        double formattedSize = size / Math.pow(1024, digitGroups);
        return String.format("%.2f %s", formattedSize, units[digitGroups]);
    }
}