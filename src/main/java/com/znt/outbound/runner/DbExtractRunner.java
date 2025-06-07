package com.znt.outbound.runner;

import com.znt.outbound.service.GenericExtractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component      
@RequiredArgsConstructor
@Slf4j
public class DbExtractRunner implements CommandLineRunner {

    private final GenericExtractService extractService;

    @Override
    public void run(String... args) {
        int n = extractService.extract();
        System.out.println(">>> 已寫入 TMP 表 " + n + " 筆");
    }
}
