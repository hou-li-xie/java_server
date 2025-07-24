package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/tools")
public class ToolsController {

    private static final Logger logger = LoggerFactory.getLogger(ToolsController.class);

    private final MediaFileRepository mediaFileRepository;

    // 使用构造函数注入替代字段注入
    public ToolsController(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    // 定义视频和图片文件扩展名
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>();
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>();
    static {
        VIDEO_EXTENSIONS.add(".mp4");
        VIDEO_EXTENSIONS.add(".webm");
        VIDEO_EXTENSIONS.add(".ogg");
        VIDEO_EXTENSIONS.add(".mov");
        VIDEO_EXTENSIONS.add(".avi");
        VIDEO_EXTENSIONS.add(".mkv");
        VIDEO_EXTENSIONS.add(".flv");
        VIDEO_EXTENSIONS.add(".wmv");
        VIDEO_EXTENSIONS.add(".m4v");

        IMAGE_EXTENSIONS.add(".jpg");
        IMAGE_EXTENSIONS.add(".jpeg");
        IMAGE_EXTENSIONS.add(".png");
        IMAGE_EXTENSIONS.add(".gif");
        IMAGE_EXTENSIONS.add(".bmp");
        IMAGE_EXTENSIONS.add(".webp");
        IMAGE_EXTENSIONS.add(".svg");
        IMAGE_EXTENSIONS.add(".tiff");
        IMAGE_EXTENSIONS.add(".ico");
    }

    @PostMapping("/run-cmd")
    public Map<String, Object> runCmd(@RequestBody Map<String, Object> requestBody) {
        logger.info("Received run-cmd request with body: {}", requestBody);

        Map<String, Object> result = new HashMap<>();

        // 获取命令参数
        String cmd = (String) requestBody.get("cmd");
        if (cmd == null || cmd.isEmpty()) {
            logger.warn("Missing or empty 'cmd' parameter");
            result.put("code", 1);
            result.put("error", "缺少或空的cmd参数");
            return result;
        }

        logger.info("Executing command: {}", cmd);

        try {
            // 创建进程构建器
            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows系统
                processBuilder = new ProcessBuilder("cmd", "/c", cmd);
            } else {
                // Unix/Linux/Mac系统
                processBuilder = new ProcessBuilder("bash", "-c", cmd);
            }

            // 设置工作目录为用户主目录
            processBuilder.directory(new java.io.File(System.getProperty("user.home")));

            // 合并错误输出到标准输出
            processBuilder.redirectErrorStream(true);

            logger.debug("ProcessBuilder command: {}", String.join(" ", processBuilder.command()));

            // 启动进程
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // 逐行读取输出
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("Command output line: {}", line);
            }

            // 等待进程结束，设置超时时间
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            int exitCode;

            if (finished) {
                exitCode = process.exitValue();
                logger.info("Command finished with exit code: {}", exitCode);
            } else {
                // 超时处理
                process.destroyForcibly();
                logger.warn("Command execution timed out");
                result.put("code", 1);
                result.put("error", "命令执行超时");
                return result;
            }

            // 构造返回结果
            result.put("code", 0);
            result.put("output", output.toString());
            result.put("exitCode", exitCode);

        } catch (IOException e) {
            logger.error("IO exception while executing command", e);
            result.put("code", 1);
            result.put("error", "IO异常: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Command execution interrupted", e);
            result.put("code", 1);
            result.put("error", "命令执行被中断: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while executing command", e);
            result.put("code", 1);
            result.put("error", "未知错误: " + e.getMessage());
        }

        logger.info("Command execution result: {}", result);
        return result;
    }

    @PostMapping("/scan-media-files")
    public Map<String, Object> scanMediaFiles() {
        logger.info("Starting media files scan");

        Map<String, Object> result = new HashMap<>();

        try {
            // 定义要扫描的目录
            String videoPath = "D:/软件/编程/Code/后台/server/videos";
            String imagePath = "D:/软件/编程/Code/后台/server/images";

            // 清空现有数据
            mediaFileRepository.deleteAll();
            logger.info("Cleared existing media files from database");

            List<MediaFile> mediaFiles = new ArrayList<>();

            // 扫描视频文件
            scanDirectory(videoPath, "video", mediaFiles);

            // 扫描图片文件
            scanDirectory(imagePath, "image", mediaFiles);

            // 保存到数据库
            List<MediaFile> savedFiles = mediaFileRepository.saveAll(mediaFiles);

            result.put("code", 0);
            result.put("msg", "扫描完成");
            result.put("data", Map.of(
                    "totalFiles", savedFiles.size(),
                    "videoFiles", savedFiles.stream().filter(f -> "video".equals(f.getFileType())).count(),
                    "imageFiles", savedFiles.stream().filter(f -> "image".equals(f.getFileType())).count()
            ));

            logger.info("Scan completed. Total files: {}, Video files: {}, Image files: {}",
                    savedFiles.size(),
                    savedFiles.stream().filter(f -> "video".equals(f.getFileType())).count(),
                    savedFiles.stream().filter(f -> "image".equals(f.getFileType())).count());

        } catch (Exception e) {
            logger.error("Error scanning media files", e);
            result.put("code", 1);
            result.put("error", "扫描失败: " + e.getMessage());
        }

        return result;
    }

    private void scanDirectory(String directoryPath, String fileType, List<MediaFile> mediaFiles) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                logger.warn("Directory does not exist: {}", directoryPath);
                return;
            }

            if (!Files.isDirectory(path)) {
                logger.warn("Path is not a directory: {}", directoryPath);
                return;
            }

            // 使用 try-with-resources 确保 Stream 被正确关闭
            try (var paths = Files.walk(path)) {
                paths.filter(Files::isRegularFile)
                        .forEach(file -> {
                            String fileName = file.getFileName().toString();
                            String extension = getFileExtension(fileName).toLowerCase();

                            if (("video".equals(fileType) && VIDEO_EXTENSIONS.contains(extension)) ||
                                    ("image".equals(fileType) && IMAGE_EXTENSIONS.contains(extension))) {

                                try {
                                    long fileSize = Files.size(file);
                                    MediaFile mediaFile = new MediaFile(fileName, fileType, fileSize);
                                    mediaFiles.add(mediaFile);
                                    logger.debug("Added file: {} ({} bytes)", fileName, fileSize);
                                } catch (IOException e) {
                                    logger.warn("Could not get size for file: {}", file, e);
                                }
                            }
                        });
            }

            logger.info("Scanned directory: {} for {} files", directoryPath, fileType);

        } catch (IOException e) {
            logger.error("Error scanning directory: {}", directoryPath, e);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }
    @GetMapping("/videos")
    public Map<String, Object> getVideos() {
        logger.info("Fetching all videos");

        Map<String, Object> result = new HashMap<>();

        try {
            List<MediaFile> videos = mediaFileRepository.findByFileType("video");

            // 为每个视频文件获取时长并更新到数据库
            for (MediaFile video : videos) {
                try {
                    // 只有当时长为空时才获取新的时长，避免重复计算
                    if (video.getDuration() == null) {
                        String baseDir = "D:/软件/编程/Code/后台/server";
                        String videoPath = baseDir + "/videos/" + video.getFileName();
                        Double durationInSeconds = getVideoDuration(videoPath);

                        // 转换为分钟并保存到数据库
                        Double durationInMinutes = durationInSeconds != null ? durationInSeconds / 60.0 : null;

                        // 更新到数据库（以分钟为单位）
                        video.setDuration(durationInMinutes);
                        mediaFileRepository.save(video);
                    }
                } catch (Exception e) {
                    logger.warn("无法获取或更新视频时长: {}, 错误: {}", video.getFileName(), e.getMessage());
                    // 即使获取时长失败，也要确保数据库记录可以正常返回
                }
            }

            // 构造返回结果
            result.put("code", 0);
            result.put("msg", "获取成功");
            result.put("data", videos);

            logger.info("Returned {} videos", videos.size());

        } catch (Exception e) {
            logger.error("Error fetching videos", e);
            result.put("code", 1);
            result.put("error", "获取失败: " + e.getMessage());
        }

        return result;
    }


    @GetMapping("/images")
    public Map<String, Object> getImages() {
        logger.info("Fetching all images");

        Map<String, Object> result = new HashMap<>();

        try {
            List<MediaFile> images = mediaFileRepository.findByFileType("image");

            // 图片的时长设为null
            for (MediaFile image : images) {
                image.setDuration(null);
            }

            // 构造返回结果
            result.put("code", 0);
            result.put("msg", "获取成功");
            result.put("data", images);

            logger.info("Returned {} images", images.size());

        } catch (Exception e) {
            logger.error("Error fetching images", e);
            result.put("code", 1);
            result.put("error", "获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取视频文件的时长
     * @param videoFilePath 视频文件路径
     * @return 视频时长（秒），如果出错则返回null
     */
    private Double getVideoDuration(String videoFilePath) {
        try {
            // 检查文件是否存在
            Path path = Paths.get(videoFilePath);
            if (!Files.exists(path)) {
                logger.warn("视频文件不存在: {}", videoFilePath);
                return null;
            }

            // 检查probe命令是否可用
            ProcessBuilder versionCheck = new ProcessBuilder("probe", "-version");
            Process versionProcess = versionCheck.start();
            int versionExitCode = versionProcess.waitFor();

            if (versionExitCode != 0) {
                logger.warn("probe命令不可用，无法获取视频时长");
                return null;
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "probe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=print_wrappers=1:node=1",
                    videoFilePath
            );

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && !output.isEmpty()) {
                String durationStr = output.toString().trim();
                if (!durationStr.isEmpty()) {
                    return Double.parseDouble(durationStr);
                }
            } else {
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line);
                    }
                }
                logger.warn("获取视频时长失败，退出码: {}, 错误输出: {}", exitCode, errorOutput);
            }
        } catch (NumberFormatException e) {
            logger.warn("解析视频时长失败: {}", videoFilePath, e);
        } catch (Exception e) {
            logger.warn("获取视频时长失败: {}, 错误: {}", videoFilePath, e.getMessage());
        }
        return null;
    }

    /*
      删除图片或视频文件接口
      @param id 文件ID
     * @return 删除结果
     */
    /**
     * 删除图片或视频文件接口
     * @param id 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/delete-media/{id}")
    public Map<String, Object> deleteMediaFile(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取文件信息并检查是否存在
            MediaFileInfo fileInfo = getMediaFileInfo(id, result);
            if (fileInfo == null) {
                // 错误信息已在getMediaFileInfo中设置
                return result;
            }

            // 确定文件存储路径
            String baseDir = "D:/软件/编程/Code/后台/server";
            String fileDir = getFileDirectoryByType(fileInfo.getFileType(), baseDir);

            if (fileDir == null) {
                result.put("success", false);
                result.put("error", "不支持的文件类型: " + fileInfo.getFileType());
                return result;
            }

            // 构建完整文件路径
            Path filePath = Paths.get(fileDir, fileInfo.getFileName());

            // 删除本地文件
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            } else {
                logger.warn("文件不存在于文件系统中: {}", filePath);
            }

            // 删除数据库记录
            mediaFileRepository.deleteById(id);

            result.put("success", true);
            result.put("message", "文件删除成功");

            logger.info("成功删除文件: ID={}, 文件名={}, 类型={}", id, fileInfo.getFileName(), fileInfo.getFileType());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "删除文件时出错: " + e.getMessage());
            logger.error("删除文件时出错, ID={}", id, e);
        }

        return result;
    }

    /**
     * 编辑媒体文件名称接口
     * @param id 文件ID
     * @param newName 新文件名（通过请求参数或请求体传递）
     * @return 编辑结果
     */
    @PutMapping("/rename-media/{id}")
    public Map<String, Object> renameMediaFile(@PathVariable Long id,
                                               @RequestParam(required = false) String newName,
                                               @RequestBody(required = false) Map<String, String> requestBody) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 确定newName的值，优先从请求参数获取，其次从请求体获取
            String actualNewName = newName;
            if (actualNewName == null && requestBody != null && requestBody.containsKey("newName")) {
                actualNewName = requestBody.get("newName");
            }

            // 获取文件信息并检查是否存在
            MediaFileInfo fileInfo = getMediaFileInfo(id, result);
            if (fileInfo == null) {
                // 错误信息已在getMediaFileInfo中设置
                return result;
            }

            // 获取文件类型和原文件名
            String fileType = fileInfo.getFileType();
            String oldFileName = fileInfo.getFileName();

            // 确保新文件名不为空
            if (actualNewName == null || actualNewName.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "新文件名不能为空");
                return result;
            }

            // 保留原文件扩展名
            String fileExtension = "";
            int lastDotIndex = oldFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                fileExtension = oldFileName.substring(lastDotIndex);
            }

            // 如果新文件名没有扩展名，则加上原扩展名
            if (actualNewName.lastIndexOf('.') <= 0) {
                actualNewName = actualNewName + fileExtension;
            }

            // 确定文件存储路径
            String baseDir = "D:/软件/编程/Code/后台/server";
            String fileDir = getFileDirectoryByType(fileType, baseDir);

            if (fileDir == null) {
                result.put("success", false);
                result.put("error", "不支持的文件类型: " + fileType);
                return result;
            }

            // 构建完整文件路径
            Path oldFilePath = Paths.get(fileDir, oldFileName);
            Path newFilePath = Paths.get(fileDir, actualNewName);

            // 检查原文件是否存在
            if (!Files.exists(oldFilePath)) {
                result.put("success", false);
                result.put("error", "原文件在文件系统中不存在");
                return result;
            }

            // 检查新文件名是否已存在
            if (Files.exists(newFilePath)) {
                result.put("success", false);
                result.put("error", "同名文件已存在");
                return result;
            }

            // 重命名本地文件
            Files.move(oldFilePath, newFilePath);

            // 更新数据库记录
            fileInfo.getMediaFile().setFileName(actualNewName);
            mediaFileRepository.save(fileInfo.getMediaFile());

            result.put("success", true);
            result.put("message", "文件重命名成功");
            result.put("newName", actualNewName);

            logger.info("成功重命名文件: ID={}, 原文件名={}, 新文件名={}", id, oldFileName, actualNewName);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "重命名文件时出错: " + e.getMessage());
            logger.error("重命名文件时出错, ID={}", id, e);
        }

        return result;
    }

    /**
     * 获取媒体文件信息并检查是否存在
     * @param id 文件ID
     * @param result 结果Map，用于设置错误信息
     * @return 文件信息对象，如果文件不存在则返回null
     */
    private MediaFileInfo getMediaFileInfo(Long id, Map<String, Object> result) {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null) {
            result.put("success", false);
            result.put("error", "文件不存在");
            return null;
        }

        return new MediaFileInfo(mediaFile.getId(), mediaFile.getFileName(), mediaFile.getFileType(), mediaFile);
    }

    /**
     * 媒体文件信息类
     */
    private static class MediaFileInfo {
        private final Long id;
        private final String fileName;
        private final String fileType;
        private final MediaFile mediaFile;

        public MediaFileInfo(Long id, String fileName, String fileType, MediaFile mediaFile) {
            this.id = id;
            this.fileName = fileName;
            this.fileType = fileType;
            this.mediaFile = mediaFile;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFileType() {
            return fileType;
        }

        public MediaFile getMediaFile() {
            return mediaFile;
        }

        public Long getId() {
            return id;
        }

    }

    /*
      根据文件类型获取对应的目录路径
      @param fileType 文件类型
     * @param baseDir 基础目录
     * @return 对应的目录路径，如果文件类型不支持则返回null
     */

    /**
     * 根据文件类型获取对应的目录路径
     * @param fileType 文件类型
     * @param baseDir 基础目录
     * @return 对应的目录路径，如果文件类型不支持则返回null
     */
    private String getFileDirectoryByType(String fileType, String baseDir) {
        if ("image".equals(fileType)) {
            return baseDir + "/images";
        } else if ("video".equals(fileType)) {
            return baseDir + "/videos";
        } else {
            return null;
        }
    }

}
