# HBase Architecture
## 1.宏观架构
![](assets/markdown-img-paste-20190428200424980.png)

- **Master**:负责启动的时候分配Region到具体的RegionServer，执行各种管理操作，比如Region的分割和合并，创建表、修改列族配置。挂了之后业务系统也能运行。

- **RegionServer**:RegionServer上有一个或多个Region。我们读取的数据存储在Region。
- **Region**:表的一部分数据。HBase是一个会自动分片的数据库。一个Region就相当于关系型数据库中分区表中的一个分区。

- **HDFS**：HBase不与服务器硬盘直接交互，而是与HDFS交互，HDFS是真正承载数据的载体。

- **ZooKeeper**：ZK重要性超过Master，ZK关闭后不能读取数据，因为读取数据需要的元数据表hbase:meta位置存储在ZK上。

### 1.1 RegionServer架构
![](assets/markdown-img-paste-20190428201536523.png)

- **WAL**：Write-Ahead Log，预写日志，预先写入。当操作到达Region的时候，HBase先把操作写入到WAL里面。HBase会先把数据放到基于内存的Memstore里，等数据到达一定的数量时才刷写(flush)到最终存储的HFile内。在此过程中如果断电或者宕机，可以从WAL中恢复数据。
- **多个Region**：Region相当于一个数据分片。每一个Region都有其实rowkey和结束rowkey，代表了它所存储的row范围。

### 1.2 Region内部结构
![](assets/markdown-img-paste-20190428202132674.png)
  - **多个Store**：每一个Region内部都包含了多个Store实例。一个Store对应一个列族的数据，如果一个表有两个列族，那么在一个Region里面就有两个Store。Store内部有MemStore和HFile两个部分组成。

## 2. 预写日志WAL
  WAL设计用来解决宕机之后操作恢复问题。数据到达Region的时候先写入WAL，然后再被加载到MemStore。就算Region的机器宕掉，由于WAL的数据是存在HDFS上，所以数据不会丢失。

### 2.1 WAL关闭打开
WAL默认开启。关闭方法：
```
Mutation.setDurability(setDurability.SKIP_WAL)

```

Put、Append、Increment、Delete都是Mutation子类，所有都有setDurability()方法。这样可以让数据操作快点，但是不建议这样做。

### 2.2 延迟(异步)同步写入WAL 

```
Mutation.setDurability(Durability.ASYNC_WAL)
```
这样设定后Region会等到条件满足的时候才把操作写入WAL。
时间间隔`hbase.regionserver.optionallogflushinterval`,间隔多久会把操作从内存写入到WAL，默认值是1s。

>异步出错时，没有任何事物保证，如果系统对性能要求极高、对数据一致性要求不高，并且系统的性能瓶颈出现在WAL上的时候，可以考虑用异步写入WAL。否则使用默认的配置即可。

### 2.3 WAL滚动
WAL是一个环状的滚动日志结构，这种结构写入效果最高，并且可以保证空间不会持续变大。

**WAL检查间隔** 由`hbase.regionserver.logroll.period`定义，默认为**1小时**。
检查的内容是把当前WAL中的操作跟实际持久化到HDFS上的操作比较，看哪些操作已经被持久化了，被持久化的操作就会被移动到.oldlogs文件夹内。(在HDFS上)

一个WAL实例包含多个WAL文件。WAL文件最大数量由`hbase.regionserver.maxlog`(默认32)定义。

触发滚动的条件是：
- 当WAL文件所在的块(Block)快要满了。
- 当WAL所占的空间大于或者等于阈值，公式为：(`hbase.regionserver.hlog.blocksize * hbase.regionserver.logroll.mutiplier`)

 `hbase.regionserver.hlog.blocksize`：是标定存储系统的块(Block)大小，如果是基于HDFS的，只需把这个值设定为HDFS的块大小即可。
>不设定blocksize，HBase还是会自己尝试去获取这个参数的值。不过还是建议设置。

`hbase.regionserver.logroll.mutiplier`是一个百分比，默认设定成0.95，意思是95%。如果WAL文件所占的恐惧大于或者等于95%的块大小，则这个WAL文件会被归档到.oldlogs文件夹内。

### 2.4 WAL文件目录
WAL文件创建后存储目录为 `/hbase/.log`，归档后移动到`/hbase/.oldlogs`文件夹。

#### 2.4.1 删除
Master会负责定期去清理.oldlogs文件夹，条件是“当这个WAL不需要作为用来恢复数据的备份时。”判断的条件是“没有任何引用指向这个WAL文件”。

有两种服务可能会引用到WAL文件：
1. **TTL进程**： 该进程会保证WAL文件一直存活达到`bhase.master.logcleaner.ttl`定义的超时时间(默认10分钟)。
2. **备份机制(replication)**：如果开启了HBased备份机制，那么HBase要保证备份集群已经完全不需要这个WAL文件，才会删除这个WAL文件。

#### 2.4.2 Store文件内部结构
![](assets/markdown-img-paste-20190428211322958.png)

- **MemStore**: 每个Store中有一个MemStore实例。数据写入WAL之后就会被放入MemStore.MemStore是内存的存储对象，只有当MemStore满了的时候才会将数据刷写(flush)到HFile中。

- **HFile**: 在Store中有多个HFile。当MemStore满了之后HBase就会在HDFS上生成一个新的HFile，然后把MemStore中的内容写到这个HFile中。HFile直接跟HDFS打交道，是数据存储实体。

- WAL存储在HDFS上，MemStore存储在内存中，HFile存储在HDFS中
- 数据先写入WAL，再放入MemStore，最后持久化到HFile中。

**问题：**数据在进入HFile之前已经被存储到HDFS一次了，为什么还需要被放入MemStore?
**A**:
因为HDFS上的文件只能创建、追加、删除，不能修改。对于一个数据库来说，按顺序的存放数据是性能的保障。使用内存先将数据整理成顺序存放，然后一起写入硬盘。
MemStore存在的意义是维持数据按照rowkey顺序排列，而不是做一个缓存。

## 3. MemStore
数据写入WAL之后会被加载到MemStore中去。MemStore的大小增加到超过一定阀值的时候就会被刷写到HDFS上，以HFile的形式被持久化起来。

设计MemStore原因：
1. 由于HDFS文件不可修改，为了让数据顺序存储从而提高读取效率，HBase使用了LSM树结构来存储数据。数据会现在Memstore中整理陈LSM树，最后再刷写到HFile上。读取数据不是限度Memstore再读磁盘，如果开了缓存，BlockCache,先读BlockCache，读不到时再读HFile+Memstore。

2. 优化数据的存储。如果一个数据添加后马上就删除了，刷写的时候就可以直接不把这个数据写到HDFS上。

LSM树是B+树的改进，关注的重心是“如何在频繁的数据改动下保持系统读取速度的稳定性”，核心在于尽量保证数据是顺序存储到磁盘上的，并且会有频率的进行整理，确保其顺序性，最大程度的保证数据读取的稳定性能。

每一次的刷写都会产生新的HFile，由于HDFS的特性，这个文件不可修改。

## 4. HFile
HFile是数据存储的实际载体，我们创建的所有表、列等数据都存储在HFile里面。
![](assets/markdown-img-paste-20190428221103304.png)

HFile由一个个的块组成。HBase中一个块默认大小为64KB，由列族上BLOCKSIZE属性定义。这些块区分了不同的角色：
- **Data**：数据块。每个HFile有多个数据块。存储在HBase表中的数据就在这里。Data块是可选的，但是几乎很难看到不包含Data块HFile。
- **Meta**：元数据块。Meta块是可选的，Meta块只有在文件关闭的时候才会写入。Meta块存储了该HFile文件的元数据信息，在v2之前布隆过滤器的信息直接放在Meta里面存储，v2之后分离出来单独存储。
- **FileInfo**: 文件信息，也是一种数据存储块，必选。FileInfo只有在文件关闭的时候写入，存储是这个文件的信息，比如最后一个Key(Last Key)，平均的Key长度(Avg Key Len)等。
- **DataIndex**：存储Data块索引信息的块文件，Data快的偏移值(offset)，可选的。
- **MetaIndex**：存储Meta块索引信息的块文件，可选，有Meta块才有MetaIndex。
- **Trailer**：必选，存储了FileInfo、DataIndex、MetaIndex块的偏移值。

### 4.1 Data块
![](assets/markdown-img-paste-20190428223342391.png)

Data数据块第一位存储的块类型，后面存储的是多个KeyValue键值对，也就是单元格(Cell是一个接口)的实现类。

## 5. KeyValue类
![](assets/markdown-img-paste-20190428224121373.png)

最后一部分存储的是Value，前面存储的是与该单元格相关的元数据信息。

如果value很小，那么单元格中大部分空间都是rowkey、column family、column等元数据；如果列族和列名字很长，大部分空间都用来存储这些信息了。

可以通过压缩算法来节省存储列族、列等信息的空间。压缩和解压会带来性能损耗。如果是数据主要是归档数据，对读写性能要求不高，可以适当使用。

## 6. 增删改查
HBase是一个可以随机读写的数据库，基于的持久化层HDFS要么新增，要么整个删除，不能修改。所以增删改查的真相是：
- 新增：HBase在HDFS上新增一条数据；
- 修改：HBase在HDFS上新增一条数据，版本号大于之前数据；
- 删除：HBase新增一条数据，这条数据没有value，类型为DELETE，称为墓碑标记(Tombstone)

### 6.1 删除
HBase在使用过程中积累了很多增改查操作，数据的连续性和顺序性会被破坏，为了提升性能，HBase每隔一段时间都会进行一次合并(Compaction)，合并的对象为HFile。合并分为两种：
- minor Compaction
- major Compaction

在HBase进行major compaction的时候，会吧多个HFile合并成一个HFile，打上墓碑标记的记录在合并中会被忽略。在产生新的HFile的时候就没有这条记录了。

## 7. 数据单元层次图
![](assets/markdown-img-paste-2019042911351092.png)
- 一个RegionServer包含多个Region，划分规则是：一个表的一段键值在一个RegionServer上产生一个Region。如果一行的数据量太大，HBase会把这个Region根据列族切分到不同的机器上去。
- 一个Region包含多个Store，划分规则为：一个列族分为一个Store,如果一个表只有一个列族，那么这个表在这个机器上的每一个Region里面都只有一个Store。
- 一个Store里面只有一个Memstore。
- 一个Store里面有多个HFile(StoreFile是HFile的抽象对象，所以如果说到StoreFile就等于HFile)。每次Memstore的刷写(flush)就产生一个新的HFile。
