# WechatSupporter

## Usage

You need to package the source code into a jar, and run `java -jar` it, the whole modules will run one by one.

## Details and Structure

### db
- DBManager: the configuration of the database manger
- DBOperation: the basic operations of database
- DBPool: the encapsulation of operations of database pool
- DBServer: the encapsulation of operations of database

### recover
- AccountDataRecover: resume data by account names
- DateDataRecover: resume data by dates
- HTMLRecover: recover html codes

### util
- ClassUtil: utils for classes
- ExcelUtil: utils for reading excels
- GroupUtil: utils for operate groups in gsdata
- NetUtil: utils for urls operations
- TimeUtil: utils for formatting the date

### main
- WechatSupporter: the starter of all modules
- AccountInfoUpdater: update the information of all accounts
- CrawlArticle: crawl all accounts' articles daily
- LogExtracter: parse the logs of proxy server
- GsDataProvider: get the amount of reading and like for every article using the api of gsdata
- DataProvider: get the amount of reading and like for every article using the api of tecent
- Counter: count the amount of reading and like in groups totally
- SyncMan: synchronize the local database with the aliyun database

### log4j.properties
the configuration of the log4j

### proxool.xml
the configuration of the proxool database pool