### Elasticsearch/DB data importer

> Here is the date importer to export data from hive and import them to Elasticsearch cluster or Database.

#### 项目打包命令：
`mvn clean package -DskipTests`

#### 后台启动命令：
`nohup java -jar data-importer-1.0.jar --spring.config.location=conf/application-prod.properties >/dev/null 2>&1 &`
​	【当然也可以通过脚本startup.sh来启动】

#### 打包后文件目录结构：

1. 项目生成的jar包 `data-importer.jar`
2. 同目录下存放启动脚本`startup.sh`
3. 同目录下建立目录 `conf` , `conf`目录下有项目的启动配置文件`application-prod.properties`，有导入的记录配置文件`config`, 另外有存放全部导入配置文件的目录`sub-conf`, 里面的导入配置以`.ei`结尾。当然，`application-prod.properties`文件可以有多份，文件名也可以自定义，只要在启动时，命令里对应的文件名或者启动脚本的对应文件名和需要用到的启动配置文件名匹配上就可以。
4. 另外，项目在启动后会在同目录下生成一个`logs`的目录，存放项目运行产生的日志，这些日志文件以天为单位分割，旧的日志文件会自动压缩，且达到一定数量或者大小后会自动清除。


#### 配置文件说明：
`conf`目录是导入程序配置文件的存放目录，当程序打包成jar包时，`conf`文件夹和jar包放在同一目录下。

1. `application-prod.properties`是项目的启动配置文件，文件名可以自定义（需要和启动命令匹配），里面内容主要包括了Oracle数据库的配置、Elasticsearch的使用配置、启动的线程数这些配置项，把这些配置项外置的目的是灵活配置项目，去掉运行环境发生变化时需要重新打包项目的必要。
2. `config`文件是程序的主配置文件，有两个property，分别是`importer-conf`和`updated-time`。
   - `importer-conf`是存放子配置文件的目录，默认是在`config`文件同目录下的`sub-config`目录下，即`conf-d: ./conf/sub-config`
   - `updated-time`是用于记录对应每个子配置文件的上次执行导入的时间，用作程序更新数据的的SQL条件。该时间记录的是查询数据的SQL开始执行前的时间，并且在程序导入完成后才写入到`config`文件，所以不会有缺漏数据的情况。
3. `sub-config`文件夹里是导入配置的配置文件，统一以`.ei`作为后缀名，用Json对内容进行描述，并且有统一的内容格式。


注意，对于插入到数据库部分的数据，遇到主键重复的情况默认采取忽略操作，如果有需要更新的需求，可以把语句描述为：
```sql
BEGIN
  INSERT SQL;
EXCEPTION WHEN DUP_VAL_ON_INDEX THEN
  UPDATE SQL;
END;
```

