package com.example.dto;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CsvResponse {
    private boolean success;
    private String message;
    private Object data; // 修改为Object类型，以支持不同的数据结构

//    public CsvResponse(boolean success, String 数据获取成功, List<CsvResponse> rankingList) {
//    }

    // 用于向后兼容，处理简单的数据列表
    public CsvResponse(boolean success, String message, List<Map<String, String>> data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // 新的构造函数，用于创建包含total和originData的嵌套数据结构
    public CsvResponse(boolean success, String message, List<Map<String, String>> total, List<Map<String, String>> originData) {
        this.success = success;
        this.message = message;
        // 创建一个Map来包含total和originData
        Map<String, List<Map<String, String>>> dataContainer = new HashMap<>();
        dataContainer.put("total", total);
        dataContainer.put("originData", originData);
        this.data = dataContainer;
    }

    // Getter和Setter方法
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
