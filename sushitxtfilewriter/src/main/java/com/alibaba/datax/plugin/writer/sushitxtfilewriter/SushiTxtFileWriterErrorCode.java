package com.alibaba.datax.plugin.writer.sushitxtfilewriter;
import com.alibaba.datax.common.spi.ErrorCode;

/**
 * Created by haiwei.luo on 14-9-17.
 */
public enum SushiTxtFileWriterErrorCode implements ErrorCode {
    
    CONFIG_INVALID_EXCEPTION("SushiTxtFileWriter-00", "您的参数配置错误."),
    REQUIRED_VALUE("SushiTxtFileWriter-01", "您缺失了必须填写的参数值."),
    ILLEGAL_VALUE("SushiTxtFileWriter-02", "您填写的参数值不合法."),
    Write_FILE_ERROR("SushiTxtFileWriter-03", "您配置的目标文件在写入时异常."),
    Write_FILE_IO_ERROR("SushiTxtFileWriter-04", "您配置的文件在写入时出现IO异常."),
    SECURITY_NOT_ENOUGH("SushiTxtFileWriter-05", "您缺少权限执行相应的文件写入操作.");

    private final String code;
    private final String description;

    private SushiTxtFileWriterErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s].", this.code,
                this.description);
    }

}
