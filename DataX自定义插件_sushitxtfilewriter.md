# DataX自定义插件-SushiTxtFileWriter

## 描述

> dataX自带的txtfilewriter暂不支持滚动(设置单文件最大行数）写入，即如出现极大的数据量，生成的文件大小会极大，故增加了该模块

## 实现方式

1. idea导入项目

   ```
   https://github.com/alibaba/DataX.git
   ```

   > 因为github的访问速度过慢，这里中间访问加了一层gitee。
   >
   > 方式如下:
   >
   > 登陆gitee，点击右上角的+号，点击下方导入已有仓库，输入上述链接，点击创建，待创建完毕后，找到对应的项目点击 克隆/下载 复制链接，在ideal中导入即可

2. 新建模块

   目录结构可以仿照其他模块,内部的配置文件也需要对应调整

3.  编译打包

   修改主工程目录下的**package.xml**和**pom.xml**，添加新增的模块。

   执行以下指令即可在项目目录下看到target包

   ```shell
   mvn -U clean package assembly:assembly -Dmaven.test.skip=true
   ```

4. 本地启动

   执行入口**Engine.java**

   需要在main方法中添加如下两行代码

   ```java
    public static void main(String[] args) throws Exception {
           int exitCode = 0;
           try {
               //设置运行的datax的家目录 即上述target包路径
               System.setProperty("datax.home", "/Users/sushi/IdeaProjects/DataX/target/datax/datax");
               //设置datax的运行脚本信息
               args = new String[]{"-mode", "standalone", "-jobid", "-1", "-job", "/Users/sushi/Desktop/stream2sushitxt.json"};
   
               Engine.entry(args);
   ```

## sushitxtfilewriter介绍

demo执行脚本

```json
{
    "setting": {},
    "job": {
        "setting": {
            "speed": {
                "channel": 1
            }
        },
        "content": [
            {
                "reader": {

          "name": "streamreader",

          "parameter": {

            "sliceRecordCount": 100000003,

            "column": [

              {

                "type": "long",

                "value": "10"

              },

              {

                "type": "string",

                "value": "hello，DataX"

              }

            ]

          }

        },
                "writer": {
                    "name": "sushitxtfilewriter",
                    "parameter": {
                        "path": "/Users/sushi/Desktop/datax_data",
                        "fileName": "test.csv",
                        "writeMode": "append",
                        "dateFormat": "yyyy-MM-dd",
                      	"maxRowNum" : 1000
                    }
                }
            }
        ]
    }
}
```

对比txtfilewriter的不同

- 写入的参数和txtfilewriter基本相同，新增**maxRowNum**，去除了**compress**

- 新增了**maxRowNum**配置，非必填，为空表示不分文件

- 修改了文件名

  - 原文件名 fileName+uuid 例如test.csv${uuid}

  - 修改后的文件名 fileNamePrefix + splitNum + yyyyMMddHHmmss + FileNum +fileNameSuffix

- 取消了压缩功能 即参数**compress**失效

- 新增了临时文件机制，即先生成临时文件.tmp，待文件写入完成后再将后缀去除，避免还未写入完成就被读取