package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/tools")
public class ToolsController {

    private static final Logger logger = LoggerFactory.getLogger(ToolsController.class);

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
}
