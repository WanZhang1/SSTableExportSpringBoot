package com.embrace.export.sstable.service;

import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTableReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SSTableExportService  implements CommandLineRunner {
    private static Logger logger = LoggerFactory.getLogger(SSTableExportService.class);

    @Autowired
    private  ExportService exportService;

    @Value("${sstable.path}")
    private String sstablePath;

    @Value("${hdfs.path}")
    private String hdfsPath;

    @Override
    public void run(String... args) throws Exception {

        logger.info("============================================================");
        long beginTime = System.currentTimeMillis();

        DatabaseDescriptor.loadSchemas();
        String hdfsFilePath = hdfsPath;
        File file = new File(sstablePath);
        File[] fs = file.listFiles();
        /**
         *异步并发写hdfs文件
         */
        final CountDownLatch cd = new CountDownLatch(fs.length);
        final AtomicInteger failCount = new AtomicInteger(0);


        for(File f:fs){
            if(!f.isDirectory() && f.getName().endsWith("-Data.db")){
                String ssTableFilePath = f.getAbsolutePath();

                if (Schema.instance.getNonSystemTables().size() < 1) {
                    String msg = "no non-system tables are defined";
                    System.err.println(msg);
                    throw new ConfigurationException(msg);
                }
                Descriptor descriptor = Descriptor.fromFilename(ssTableFilePath);
                if (Schema.instance.getCFMetaData(descriptor) == null) {
                    System.err.println(String.format("The provided column family is not part of this cassandra database: keysapce = %s, column family = %s", descriptor.ksname, descriptor.cfname));
                }else{
                    FSDataOutputStream out = getOutputStream(hdfsFilePath+f.getName().replace(".db",""));
                    exportService.export(SSTableReader.open(descriptor), out);
                }
            }
        }
        cd.await();
        logger.info("[{}] totalPage is {}, failed count is {}", failCount.get());

        long endTime = System.currentTimeMillis();
        logger.info(" used time {} minutes", (endTime-beginTime)/60000);

        logger.info("============================================================");

        System.exit(0);
    }

    public  FSDataOutputStream getOutputStream(String toUri) throws IOException {
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(toUri), conf);
        FSDataOutputStream out = fs.create(new Path(toUri));
        return out;
    }




}
