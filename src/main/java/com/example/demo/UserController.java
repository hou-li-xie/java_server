package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.nio.charset.StandardCharsets;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;

    // JWT 密钥（实际项目请放配置文件）
    private static final String JWT_SECRET = "demo_secret_key_1234567890_abcdefg!@#";
    // token 有效期（7天）
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        User user = userRepository.findByUsername(username);
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            result.put("code", 1);
            result.put("msg", "用户不存在");
            return result;
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            result.put("code", 2);
            result.put("msg", "密码错误");
            return result;
        }
        // 生成 JWT token
        String token = Jwts.builder()
                .claim("id", user.getId())
                .claim("username", user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET.getBytes(StandardCharsets.UTF_8))
                .compact();
        // 构造 user 信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("avatar", "https://api.dicebear.com/7.x/adventurer/svg?seed=" + user.getUsername());
        // 返回结果
        result.put("code", 0);
        result.put("msg", "登录成功");
        result.put("token", token);
        result.put("user", userInfo);
        return result;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        // 检查用户名是否已存在
        if (userRepository.findByUsername(username) != null) {
            result.put("code", 1);
            result.put("msg", "用户名已存在");
            return result;
        }
        // bcrypt加密
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        // 生成avatar
        String avatar = "https://api.dicebear.com/7.x/adventurer/svg?seed=" + username;
        // 生成created_at
        String createdAt = new java.util.Date().toLocaleString();
        // 创建用户对象
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setAvatar(avatar);
        user.setCreatedAt(createdAt);
        // 由于User实体没有avatar和created_at字段，这里只返回给前端
        userRepository.save(user);
        // 返回结果
        result.put("code", 0);
        result.put("msg", "注册成功");
        result.put("userId", user.getId());
        return result;
    }

    @GetMapping("/users")
    public Map<String, Object> getAllUsers() {
        Map<String, Object> result = new HashMap<>();
        var users = userRepository.findAll();
        result.put("code", 0);
        result.put("msg", "获取成功");
        result.put("data", users);
        return result;
    }

    @DeleteMapping("/user/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!userRepository.existsById(id)) {
            result.put("code", 1);
            result.put("msg", "用户不存在");
            return result;
        }
        userRepository.deleteById(id);
        result.put("code", 0);
        result.put("msg", "删除成功");
        return result;
    }

    @GetMapping("/menu")
    public Map<String, Object> getMenu() {
        Map<String, Object> result = new HashMap<>();
        // 构建静态菜单数据
        var menuData = java.util.Arrays.asList(
                createMenuItem("/dashboard", "Dashboard", "1.svg", "仪表盘"),

                createParentMenuItem("/sys", "sys", "2.svg", "系统管理", java.util.Arrays.asList(
                        createMenuItem("/user", "user", "3.svg", "用户管理")
                )),

                createParentMenuItem("/material", "material", "2.svg", "素材管理", java.util.Arrays.asList(
                        createMenuItem("/show", "show", "3.svg", "素材展示"),
                        createMenuItem("/upload", "upload", "3.svg", "素材上传")
                )),

                createParentMenuItem("/tools", "tools", "2.svg", "便捷工具", java.util.Arrays.asList(
                        createMenuItem("/convert", "convert", "3.svg", "m3u8转mp4")
                ))
        );

        result.put("code", 0);
        result.put("msg", "菜单数据获取成功");
        result.put("data", menuData);
        return result;
    }

    // 辅助方法：创建单个菜单项
    private Map<String, Object> createMenuItem(String path, String name, String icon, String title) {
        Map<String, Object> item = new HashMap<>();
        item.put("path", path);
        item.put("name", name);
        item.put("icon", icon);
        item.put("title", title);
        return item;
    }

    // 辅助方法：创建包含子菜单的菜单项
    private Map<String, Object> createParentMenuItem(String path, String name, String icon, String title, java.util.List<Map<String, Object>> children) {
        Map<String, Object> item = createMenuItem(path, name, icon, title);
        item.put("children", children);
        return item;
    }

    @GetMapping("/disk-info")
    public Map<String, Object> getDiskInfo() {
        // 获取系统临时目录（可以根据需要替换为其他目录）
        String videoPath = "D:/软件/编程/Code/后台/server/images"; // 替换为你自己的视频目录
        String imagePath = "D:/软件/编程/Code/后台/server/videos"; // 替换为你自己的图片目录

        // 获取磁盘空间信息
        Map<String, Object> diskInfo = new HashMap<>();

        // 获取 video 分区信息
        Map<String, Object> videoInfo = getDiskUsageInfo(videoPath);

        // 获取 image 分区信息
        Map<String, Object> imageInfo = getDiskUsageInfo(imagePath);

        // 构建返回结果
        diskInfo.put("video", videoInfo);
        diskInfo.put("image", imageInfo);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "磁盘信息获取成功");
        result.put("data", diskInfo);

        return result;
    }

    // 辅助方法：获取指定路径的磁盘使用情况
    private Map<String, Object> getDiskUsageInfo(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs(); // 如果目录不存在，尝试创建
        }

        long totalSpace = file.getTotalSpace();
        long freeSpace = file.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        double totalGb = totalSpace / (1024.0 * 1024.0 * 1024.0);
        double freeGb = freeSpace / (1024.0 * 1024.0 * 1024.0);
        double usedGb = usedSpace / (1024.0 * 1024.0 * 1024.0);
        double usagePercent = (usedGb / totalGb) * 100;

        Map<String, Object> info = new HashMap<>();
        info.put("total", totalSpace);
        info.put("free", freeSpace);
        info.put("used", usedSpace);
        info.put("totalFormatted", String.format("%.2f GB", totalGb));
        info.put("freeFormatted", String.format("%.2f GB", freeGb));
        info.put("usedFormatted", String.format("%.2f GB", usedGb));
        info.put("usagePercent", String.format("%.2f", usagePercent));

        return info;
    }
    @GetMapping("/user/{id}")
    public Map<String, Object> getUserInfo(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            result.put("code", 1);
            result.put("msg", "用户不存在");
            return result;
        }

        // 构造用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("createdAt", user.getCreatedAt());

        result.put("code", 0);
        result.put("msg", "获取成功");
        result.put("data", userInfo);
        return result;
    }
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> result = new HashMap<>();

        // 构建配置信息
        Map<String, Object> config = new HashMap<>();

        // 视频配置
        Map<String, Object> videoConfig = new HashMap<>();
        videoConfig.put("allowedTypes", java.util.Arrays.asList(".mp4", ".webm", ".ogg", ".mov", ".avi", ".mkv", ".flv"));
        videoConfig.put("maxSize", 10 * 1024 * 1024 * 1024); // 2GB
        videoConfig.put("maxSizeFormatted", "2 GB");
        videoConfig.put("maxFiles", 10);
        videoConfig.put("chunkSize", 2 * 1024 * 1024); // 2MB
        videoConfig.put("chunkSizeFormatted", "2 MB");
        videoConfig.put("folder", "D:/软件/编程/Code/后台/server/videos"); // 替换为你自己的视频目录

        // 图片配置
        Map<String, Object> imageConfig = new HashMap<>();
        imageConfig.put("allowedTypes", java.util.Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".tiff"));
        imageConfig.put("maxSize", 10 * 1024 * 1024); // 10MB
        imageConfig.put("maxSizeFormatted", "10 MB");
        imageConfig.put("maxFiles", 20);
        imageConfig.put("chunkSize", 512 * 1024); // 512KB
        imageConfig.put("chunkSizeFormatted", "512 KB");
        imageConfig.put("folder", "D:/软件/编程/Code/后台/server/images"); // 替换为你自己的图片目录

        // 添加到主配置
        config.put("video", videoConfig);
        config.put("image", imageConfig);

        result.put("code", 0);
        result.put("msg", "配置获取成功");
        result.put("data", config);

        return result;
    }
}

