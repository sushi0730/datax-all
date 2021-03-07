package com.alibaba.datax.plugin.writer.sushitxtfilewriter;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.unstructuredstorage.writer.*;
import com.alibaba.datax.plugin.unstructuredstorage.writer.Key;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * @Author sushi
 * @create 2021-03-06 6:18 PM
 */
public class SushiStorageWriterUtil {
    private SushiStorageWriterUtil() {

    }

    private static final Logger LOG = LoggerFactory
            .getLogger(SushiStorageWriterUtil.class);

    public static void writeToFile(RecordReceiver lineReceiver, Configuration config, TaskPluginCollector taskPluginCollector) {
        try {
            SushiStorageWriterUtil.doWriteToFile(lineReceiver,
                    config, taskPluginCollector);
        } catch (NullPointerException e) {
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.RUNTIME_EXCEPTION,
                    "运行时错误, 自求多福，嘻嘻", e);
        }
    }


    private static void doWriteToFile(RecordReceiver lineReceiver,
                                      Configuration config,
                                      TaskPluginCollector taskPluginCollector) {

        String nullFormat = config.getString(Key.NULL_FORMAT);

        // 兼容format & dataFormat
        String dateFormat = config.getString(Key.DATE_FORMAT);
        DateFormat dateParse = null; // warn: 可能不兼容
        if (StringUtils.isNotBlank(dateFormat)) {
            dateParse = new SimpleDateFormat(dateFormat);
        }

        // warn: default false
        String fileFormat = config.getString(Key.FILE_FORMAT,
                Constant.FILE_FORMAT_TEXT);

        String delimiterInStr = config.getString(Key.FIELD_DELIMITER);
        if (null != delimiterInStr && 1 != delimiterInStr.length()) {
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE,
                    String.format("仅仅支持单字符切分, 您配置的切分为 : [%s]", delimiterInStr));
        }
        if (null == delimiterInStr) {
            LOG.warn(String.format("您没有配置列分隔符, 使用默认值[%s]",
                    Constant.DEFAULT_FIELD_DELIMITER));
        }

        // warn: fieldDelimiter could not be '' for no fieldDelimiter
        char fieldDelimiter = config.getChar(Key.FIELD_DELIMITER,
                Constant.DEFAULT_FIELD_DELIMITER);
        // 获取编码
        String encoding = SushiStorageWriterUtil.getEncoding(config);
        String timeStamp = TimeUtils.getTimeStringFromTimestampMillis(System.currentTimeMillis());
        boolean isRunning = true;
        // 获取单页最大行数
        Integer maxRowNum = 0;
        Integer pageNum = 1;
        try {
            maxRowNum = config.getInt(com.alibaba.datax.plugin.writer.sushitxtfilewriter.Key.MAX_ROW_NUM, 0);
        } catch (Exception e) {
            throw DataXException.asDataXException(SushiTxtFileWriterErrorCode.ILLEGAL_VALUE, String.format("请填写正确的maxRowNum"));
        }
        // 分文件写入文件
        for(int page = 0; isRunning; page ++) {
            long rowNum = 0;
            String fileFullPath = SushiStorageWriterUtil.buildTmpFilePath(config, timeStamp, page);
            LOG.info(String.format("write to file : [%s]", fileFullPath));
            Writer writer = null;
            try {
                // 提前读一条，如果为空则直接停止 不创建文件 避免（rowNum % maxRowNum = 0）创建空文件的情况
                Record record = lineReceiver.getFromReader();
                if(record == null) {
                    break;
                }
                // 获取输出流
                writer = SushiStorageWriterUtil.getWriter(fileFullPath, encoding);
                UnstructuredWriter unstructuredWriter = TextCsvWriterManager
                        .produceUnstructuredWriter(fileFormat, fieldDelimiter, writer);


                List<String> headers = config.getList(Key.HEADER, String.class);
                if (null != headers && !headers.isEmpty()) {
                    unstructuredWriter.writeOneRecord(headers);
                }
                // 写第一行数据
                UnstructuredStorageWriterUtil.transportOneRecord(record,
                        nullFormat, dateParse, taskPluginCollector,
                        unstructuredWriter);
                rowNum ++;
                if(maxRowNum > 0 && rowNum % maxRowNum == 0 ) {
                    break;
                }
                // 写第二行开始的数据
                while ((record = lineReceiver.getFromReader()) != null) {
                    UnstructuredStorageWriterUtil.transportOneRecord(record,
                            nullFormat, dateParse, taskPluginCollector,
                            unstructuredWriter);
                    rowNum ++;
                    if(maxRowNum > 0 && rowNum % maxRowNum == 0 ) {
                        break;
                    }
                }
                if(record == null) {
                    // 数据全读取完成
                    isRunning = false;
                }
            } catch (IOException ioe) {
                throw DataXException.asDataXException(
                        SushiTxtFileWriterErrorCode.Write_FILE_IO_ERROR,
                        String.format("无法创建待写文件 : [%s]", fileFullPath), ioe);
            } finally {
                IOUtils.closeQuietly(writer);
                try {
                    new File(fileFullPath).renameTo(new File(fileFullPath.substring(0, fileFullPath.lastIndexOf("."))));
                } catch (Exception e) {
                    throw DataXException.asDataXException(
                            SushiTxtFileWriterErrorCode.Write_FILE_IO_ERROR,
                            String.format("重命名文件失败 : [%s]", fileFullPath));
                }
            }
        }



        // warn:由调用方控制流的关闭
        // IOUtils.closeQuietly(unstructuredWriter);
    }

    private static String getEncoding(Configuration config) {
        String encoding = config.getString(Key.ENCODING,
                Constant.DEFAULT_ENCODING);
        if (StringUtils.isBlank(encoding)) {
            LOG.warn(String.format("您配置的encoding为[%s], 使用默认值[%s]", encoding,
                    Constant.DEFAULT_ENCODING));
            encoding = Constant.DEFAULT_ENCODING;
        }
        return encoding;
    }

    private static Writer getWriter(String fileFullPath, String encoding) {

        BufferedWriter writer = null;

        try {
            File newFile = new File(fileFullPath);
            newFile.createNewFile();
            OutputStream outputStream = new FileOutputStream(newFile);
            writer = new BufferedWriter(new OutputStreamWriter(
                    outputStream, encoding));

        } catch (SecurityException se) {
            throw DataXException.asDataXException(
                    SushiTxtFileWriterErrorCode.SECURITY_NOT_ENOUGH,
                    String.format("您没有权限创建文件  : [%s]", fileFullPath));
        } catch (UnsupportedEncodingException uee) {
            throw DataXException
                    .asDataXException(
                            UnstructuredStorageWriterErrorCode.Write_FILE_WITH_CHARSET_ERROR,
                            String.format("不支持的编码格式 : [%s]", encoding), uee);
        } catch (IOException ioe) {
            throw DataXException.asDataXException(
                    SushiTxtFileWriterErrorCode.Write_FILE_IO_ERROR,
                    String.format("无法创建待写文件 : [%s]", fileFullPath), ioe);
        } catch (NullPointerException e) {
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.RUNTIME_EXCEPTION,
                    "运行时错误, 自求多福吧，嘻嘻", e);
        }
        return writer;

    }

    /**
     * 生成文件名 文件名形如 name_splitNum_time_page.csv.tmp
     * @param config
     * @param timeStr
     * @param page
     * @return
     */
    private static String buildTmpFilePath(Configuration config,String timeStr, Integer page) {
        String path = config.getString(com.alibaba.datax.plugin.writer.sushitxtfilewriter.Key.PATH);
        String fileName = config
                .getString(com.alibaba.datax.plugin.unstructuredstorage.writer.Key.FILE_NAME);
        String fileNamePrefix = fileName.substring(0, fileName.indexOf("."));
        String fileNameSuffix = fileName.substring(fileName.indexOf("."));
        boolean isEndWithSeparator = false;
        switch (IOUtils.DIR_SEPARATOR) {
            case IOUtils.DIR_SEPARATOR_UNIX:
                isEndWithSeparator = path.endsWith(String
                        .valueOf(IOUtils.DIR_SEPARATOR));
                break;
            case IOUtils.DIR_SEPARATOR_WINDOWS:
                isEndWithSeparator = path.endsWith(String
                        .valueOf(IOUtils.DIR_SEPARATOR_WINDOWS));
                break;
            default:
                break;
        }
        if (!isEndWithSeparator) {
            path = path + IOUtils.DIR_SEPARATOR;
        }
        return String.format("%s%s_%s_%s_%s%s.tmp", path, fileNamePrefix,config.get(com.alibaba.datax.plugin.writer.sushitxtfilewriter.Key.SPLIT_NUM), timeStr, page.toString(), fileNameSuffix);
    }
}
