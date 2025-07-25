package com.example.csv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CsvDataRepository extends JpaRepository<CsvData, Long> {
    List<CsvData> findAllByOrderByCreatedAtDesc();
}
