package com.embrace.export.sstable.service;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.ColumnFamily;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletedColumn;
import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.io.sstable.SSTableIdentityIterator;
import org.apache.cassandra.io.sstable.SSTableReader;
import org.apache.cassandra.io.sstable.SSTableScanner;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

@Service
public class ExportService {
    private static Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static final String RESULT_DELIMITER = ",";


    /**
     *  导出
     */
    // This is necessary to accommodate the test suite since you cannot open a Reader more
    // than once from within the same process.
    @Async("exportExecutor")
    public void export(SSTableReader reader, FSDataOutputStream outs) throws IOException {
        SSTableIdentityIterator row;
        SSTableScanner scanner = reader.getDirectScanner();
        int i = 0;
        // collecting keys to export
        while (scanner.hasNext()) {
            row = (SSTableIdentityIterator) scanner.next();
            if (i != 0)
                outs.write("\n".getBytes());
            serializeRow(row, row.getKey(), outs);
            i++;
        }
        outs.flush();
        outs.close();
        scanner.close();
    }

    /**
     * 序列化row
     */
    private synchronized void serializeRow(SSTableIdentityIterator row, DecoratedKey key, FSDataOutputStream out)
            throws IOException{
        ColumnFamily columnFamily = row.getColumnFamily();
        boolean isSuperCF = columnFamily.isSuper();
        CFMetaData cfMetaData = columnFamily.metadata();
        out.write((new String(key.key.array())+RESULT_DELIMITER).getBytes());
        if (isSuperCF) {
            while (row.hasNext()) {
                IColumn column = row.next();
                serializeColumns(column.getSubColumns().iterator(), out,cfMetaData);
            }
        } else {
            serializeColumns(row, out, cfMetaData);
        }
    }
    /**
     * 序列化指定的columns
     */
    private synchronized void serializeColumns(Iterator<IColumn> columns, FSDataOutputStream out, CFMetaData cfMetaData) throws IOException{
        while (columns.hasNext()) {
            out.write((serializeColumn(columns.next(), cfMetaData)).getBytes());
            if (columns.hasNext())
                out.write(RESULT_DELIMITER.getBytes());
        }
    }

    /**
     * 把指定column 转为 csv 格式
     */
    private synchronized  String serializeColumn(IColumn column, CFMetaData cfMetaData)
    {
        StringBuffer serializedColumn = new StringBuffer();

        ByteBuffer name = ByteBufferUtil.clone(column.name());
        ByteBuffer value = ByteBufferUtil.clone(column.value());

        if (column instanceof DeletedColumn) {
            serializedColumn.append(ByteBufferUtil.bytesToHex(value));
        } else {
            AbstractType<?> validator = cfMetaData.getValueValidator(name);
            serializedColumn.append(validator.getString(value));
        }
        return serializedColumn.toString();
    }



}
