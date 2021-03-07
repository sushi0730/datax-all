## 介绍
txtfilewritersushi为txtfilewriter的改进版本
主要增加了 
    文件支持滚动，当文件大于某个size值或者行数值
- 配置文件需要增加
    - maxRowNum 每个文件的最大行数
- 部分字段将会失效
    - compress 文本压缩类型，默认不填写意味着没有压缩。支持压缩类型为zip、lzo、lzop、tgz、bzip2。如有必要，可以继续改造

    