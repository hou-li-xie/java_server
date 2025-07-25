package com.example.csv;

import com.example.dto.CsvResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/csv")
@CrossOrigin(origins = "*")
public class CsvController {

    private static final String CSV_MIME_TYPE = "text/csv";
    private static final String CSV_FILE_EXTENSION = ".csv";

    @Autowired
    private CsvService csvService;

    @PostMapping("/upload")
    public ResponseEntity<CsvResponse> uploadAndSaveCsvFile(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            validateFile(file);
            List<Map<String, String>> data = csvService.parseCsvFile(file);
            csvService.saveOriginDataToDatabase(data);
            return buildSuccessResponse("CSV文件解析成功并已保存到数据库", data);
        } catch (IllegalArgumentException e) {
            return buildBadRequestResponse("参数错误: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("解析文件时出错: " + e.getMessage());
        }
    }

    @GetMapping("/data")
    public ResponseEntity<CsvResponse> getProcessedData(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        try {
            List<Map<String, String>> originData = csvService.getOriginDataFromDatabase();
            List<Map<String, String>> filteredData = filterDataIfNeeded(originData, startDate, endDate);
            List<Map<String, String>> total = csvService.aggregateDataByDate(filteredData);
            return buildSuccessResponse("数据获取成功", total, filteredData);
        } catch (Exception e) {
            return buildErrorResponse("获取数据时出错: " + e.getMessage());
        }
    }

    @GetMapping("/expense-category-amount")
    public ResponseEntity<CsvResponse> getExpenseCategoryAmount() {
        try {
            // 1. 从服务层获取原始数据
            Map<String, Double> categoryAmountMap = csvService.getExpenseCategoryAmount();

            // 2. 转换为所需的List<Map<String, String>>格式
            List<Map<String, String>> resultList = new ArrayList<>();

            for (Map.Entry<String, Double> entry : categoryAmountMap.entrySet()) {
                Map<String, String> item = new HashMap<>();
                item.put("category", entry.getKey());

                // 格式化金额为字符串，保留2位小数
                String formattedAmount = String.format("%.2f", entry.getValue());
                item.put("amount", formattedAmount);

                resultList.add(item);
            }

            // 3. 返回转换后的数据
            return ResponseEntity.ok(new CsvResponse(true, "数据获取成功", resultList));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new CsvResponse(false, "获取数据时出错: " + e.getMessage(), null));
        }
    }

    @GetMapping("/expense-category-ranking")
    public ResponseEntity<CsvResponse> getExpenseCategoryRanking() {
        try {
            List<Map<String, Object>> rankingList = csvService.getExpenseCategoryRanking();
            return buildSuccessResponse("数据获取成功", rankingList);
        } catch (Exception e) {
            return buildErrorResponse("获取数据时出错: " + e.getMessage());
        }
    }

    // =============== 私有工具方法 ===============

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传有效的CSV文件");
        }
        if (!isValidCsvFile(file)) {
            throw new IllegalArgumentException("请上传CSV格式的文件");
        }
    }

    private boolean isValidCsvFile(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        return (contentType != null && contentType.equals(CSV_MIME_TYPE)) ||
                (fileName != null && fileName.toLowerCase().endsWith(CSV_FILE_EXTENSION));
    }

    private List<Map<String, String>> filterDataIfNeeded(List<Map<String, String>> originData,
                                                         String startDate, String endDate) {
        if (startDate != null || endDate != null) {
            return csvService.filterDataByDateRange(originData, startDate, endDate);
        }
        return originData;
    }

    private Map<String, Double> formatCategoryAmounts(Map<String, Double> categoryAmountMap) {
        Map<String, Double> formattedMap = new LinkedHashMap<>();
        categoryAmountMap.forEach((key, value) -> {
            BigDecimal bd = BigDecimal.valueOf(value)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            formattedMap.put(key, bd.doubleValue());
        });
        return formattedMap;
    }

    // =============== 响应构建方法 ===============

    private ResponseEntity<CsvResponse> buildSuccessResponse(String message, Object data) {
        return ResponseEntity.ok(new CsvResponse(true, message, (List<Map<String, String>>) data));
    }

    private ResponseEntity<CsvResponse> buildSuccessResponse(String message,
                                                             List<Map<String, String>> total,
                                                             List<Map<String, String>> originData) {
        return ResponseEntity.ok(new CsvResponse(true, message, total, originData));
    }

    private ResponseEntity<CsvResponse> buildBadRequestResponse(String message) {
        return ResponseEntity.badRequest().body(new CsvResponse(false, message, null));
    }

    private ResponseEntity<CsvResponse> buildErrorResponse(String message) {
        return ResponseEntity.internalServerError()
                .body(new CsvResponse(false, message, null));
    }
}