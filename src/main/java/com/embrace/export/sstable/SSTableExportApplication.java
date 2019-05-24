package com.embrace.export.sstable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"com.embrace.export.sstable"})
@EnableAutoConfiguration()
@EnableAsync
public class SSTableExportApplication {

    public static void main(String[] args) {

        SpringApplication.run(SSTableExportApplication.class, args);

    }
}
