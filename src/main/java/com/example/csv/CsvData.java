package com.example.csv;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "csv_data")
public class CsvData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private String time;
    private String amount;
    private String account;
    private String book;
    private String currency;
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public CsvData() {
    }

    public CsvData(String category, String time, String amount, String account,
                   String book, String currency, String remark) {
        this.category = category;
        this.time = time;
        this.amount = amount;
        this.account = account;
        this.book = book;
        this.currency = currency;
        this.remark = remark;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
