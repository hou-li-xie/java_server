package com.example.csv;

import com.example.dto.CsvResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CsvService {

    private static final Map<String, String> KEY_MAPPING = new HashMap<>();
    private final CsvDataRepository csvDataRepository;

    static {
        KEY_MAPPING.put("\uFEFF分类", "category");
        KEY_MAPPING.put("时间", "time");
        KEY_MAPPING.put("金额", "amount");
        KEY_MAPPING.put("账户", "account");
        KEY_MAPPING.put("账本", "book");
        KEY_MAPPING.put("货币", "currency");
        KEY_MAPPING.put("备注", "remark");
    }

    @Autowired
    public CsvService(CsvDataRepository csvDataRepository) {
        this.csvDataRepository = csvDataRepository;
    }

    public List<Map<String, String>> parseCsvFile(MultipartFile file) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV文件为空");
            }

            List<String> headers = parseLine(headerLine);
            List<String> englishHeaders = new ArrayList<>();

            for (String header : headers) {
                englishHeaders.add(KEY_MAPPING.getOrDefault(header.trim(), header.trim()));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                List<String> values = parseLine(line);
                Map<String, String> record = new LinkedHashMap<>();

                for (int i = 0; i < Math.min(englishHeaders.size(), values.size()); i++) {
                    record.put(englishHeaders.get(i), values.get(i));
                }

                records.add(record);
            }
        }

        return records;
    }

    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString().trim());
        return fields;
    }

    public List<Map<String, String>> filterDataByDateRange(List<Map<String, String>> data,
                                                           String startDateStr, String endDateStr) {
        if ((startDateStr == null || startDateStr.isEmpty()) &&
                (endDateStr == null || endDateStr.isEmpty())) {
            return data;
        }

        List<Map<String, String>> filteredData = new ArrayList<>();

        for (Map<String, String> record : data) {
            String timeStr = record.get("time");
            if (timeStr == null || timeStr.isEmpty()) {
                continue;
            }

            String recordDateStr = timeStr.length() >= 10 ? timeStr.substring(0, 10) : timeStr;
            boolean afterStart = startDateStr == null || startDateStr.isEmpty() ||
                    recordDateStr.compareTo(startDateStr) >= 0;
            boolean beforeEnd = endDateStr == null || endDateStr.isEmpty() ||
                    recordDateStr.compareTo(endDateStr) <= 0;

            if (afterStart && beforeEnd) {
                filteredData.add(record);
            }
        }

        return filteredData;
    }

    public List<Map<String, String>> aggregateDataByDate(List<Map<String, String>> data) {
        Map<String, Map<String, Double>> aggregatedMap = new LinkedHashMap<>();

        for (Map<String, String> record : data) {
            String date = record.get("time");
            String amountStr = record.get("amount");

            if (date == null || amountStr == null) {
                continue;
            }

            try {
                String dateStr = date.length() >= 10 ? date.substring(0, 10) : date;
                double amount = Double.parseDouble(amountStr);

                aggregatedMap.putIfAbsent(dateStr, new HashMap<>());
                Map<String, Double> dailyStats = aggregatedMap.get(dateStr);

                if (amount >= 0) {
                    dailyStats.put("income", dailyStats.getOrDefault("income", 0.0) + amount);
                } else {
                    dailyStats.put("expense", dailyStats.getOrDefault("expense", 0.0) + amount);
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : aggregatedMap.entrySet()) {
            Map<String, String> aggregatedRecord = new LinkedHashMap<>();
            aggregatedRecord.put("time", entry.getKey());

            Map<String, Double> dailyStats = entry.getValue();
            aggregatedRecord.put("income", formatDouble(dailyStats.getOrDefault("income", 0.0)));
            aggregatedRecord.put("expense", formatDouble(dailyStats.getOrDefault("expense", 0.0)));

            // 设置其他字段为空
            Arrays.asList("category", "account", "book", "currency", "remark")
                    .forEach(field -> aggregatedRecord.put(field, ""));

            result.add(aggregatedRecord);
        }

        return result;
    }

    private String formatDouble(double value) {
        return new BigDecimal(value).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    public void saveOriginDataToDatabase(List<Map<String, String>> data) {
        List<CsvData> csvDataList = new ArrayList<>();

        for (Map<String, String> record : data) {
            CsvData csvData = new CsvData(
                    record.get("category"),
                    record.get("time"),
                    record.get("amount"),
                    record.get("account"),
                    record.get("book"),
                    record.get("currency"),
                    record.get("remark")
            );
            csvDataList.add(csvData);
        }

        csvDataRepository.deleteAll();
        csvDataRepository.saveAll(csvDataList);
    }

    public List<Map<String, String>> getOriginDataFromDatabase() {
        List<CsvData> csvDataList = csvDataRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, String>> result = new ArrayList<>();

        for (CsvData csvData : csvDataList) {
            Map<String, String> record = new LinkedHashMap<>();
            record.put("category", csvData.getCategory());
            record.put("time", csvData.getTime());
            record.put("amount", csvData.getAmount());
            record.put("account", csvData.getAccount());
            record.put("book", csvData.getBook());
            record.put("currency", csvData.getCurrency());
            record.put("remark", csvData.getRemark());
            result.add(record);
        }

        return result;
    }

    public Map<String, Double> getExpenseCategoryAmount() {
        List<Map<String, String>> originData = getOriginDataFromDatabase();
        Map<String, Double> categoryExpenseMap = new HashMap<>();

        for (Map<String, String> record : originData) {
            String category = record.get("category");
            String amountStr = record.get("amount");

            if (category == null || amountStr == null) {
                continue;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount < 0) {
                    double absAmount = Math.abs(amount);
                    categoryExpenseMap.put(category,
                            categoryExpenseMap.getOrDefault(category, 0.0) + absAmount);
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        Map<String, Double> formattedMap = new LinkedHashMap<>();
        categoryExpenseMap.forEach((k, v) ->
                formattedMap.put(k, new BigDecimal(v).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));

        return formattedMap;
    }

    public List<Map<String, Object>> getExpenseCategoryRanking() {
        List<Map<String, String>> originData = getOriginDataFromDatabase();
        Map<String, Map<String, Object>> categoryStatsMap = new HashMap<>();

        for (Map<String, String> record : originData) {
            String category = record.get("category");
            String amountStr = record.get("amount");

            if (category == null || amountStr == null) {
                continue;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount < 0) {
                    double absAmount = Math.abs(amount);

                    categoryStatsMap.putIfAbsent(category, new HashMap<>());
                    Map<String, Object> stats = categoryStatsMap.get(category);

                    stats.put("amount", (Double) stats.getOrDefault("amount", 0.0) + absAmount);
                    stats.put("count", (Integer) stats.getOrDefault("count", 0) + 1);
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        List<Map<String, Object>> rankingList = new ArrayList<>();
        categoryStatsMap.forEach((category, stats) -> {
            Map<String, Object> rankingItem = new LinkedHashMap<>();
            rankingItem.put("category", category);
            rankingItem.put("amount", stats.get("amount"));
            rankingItem.put("count", stats.get("count"));
            rankingList.add(rankingItem);
        });

        rankingList.sort((a, b) ->
                Double.compare((Double) b.get("amount"), (Double) a.get("amount")));

        return rankingList;
    }
}