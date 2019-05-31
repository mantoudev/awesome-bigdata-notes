# RDD
## Why RDD?
MR慢是因为DAG的中间计算结果要落盘防止运行结果丢失。

## RDD定义
RDD表示已被分区、不可变的，并能够被并行操作的数据集合。
### 分区
分区代表同一个 RDD 包含的数据被存储在系统的不同节点中，这也是它可以被并行处理的前提。

- 逻辑上，我们可以认为 RDD 是一个大的数组。数组中的每个元素代表一个分区（Partition）。
- 在物理存储中，每个分区指向一个存放在内存或者硬盘中的数据块（Block），而这些数据块是独立的，它们可以被存放在系统中的不同节点。

RDD 只是抽象意义的数据集合，分区内部并不会存储具体的数据。
![](assets/markdown-img-paste-20190525110453163.png)

RDD分区对应index。RDD的ID与分区的index唯一对应。

各节点数据块存放在内存，内存没有空间存入硬盘。

RDD存储数据只读，但是可以修改分区数量。

### 不可变性
RDD是只读的，所包含的分区信息不可改变。但可对现有RDD转换(transformation)操作，得到新的RDD。
RDD与函数式编程的Collection很相似。
```
//读入文本data.txt
lines = sc.textFile("data.txt")
//创建RDD lineLengths，获取每一行字数
lineLengths = lines.map(lambda s: len(s))
//获取文本总字数
totalLength = lineLengths.reduce(lambda a, b: a + b)
```

**依赖关系**：记录中间结果RDD，提升Spark计算效率，错误恢复更容易。第N步错，只需从N-1个RDD开始恢复。

### 并行操作
由于单个RDD分区特性，支持并发，不同节点的数据可被分别处理然后产生新的RDD。

## RDD结构
![](assets/markdown-img-paste-20190525112359233.png)

**SparkContext**:
SparkContext 是所有 Spark 功能的入口，它代表了与 Spark 节点的连接，可以用来创建
RDD 对象以及在节点中的广播变量等。一个线程只有一个 SparkContext。

## 依赖关系
Spark 不需要将每个中间计算结果进行数据复制以防数据丢失，因为每一步产生的 RDD 里都会存储它的依赖关系，即它是通过哪个 RDD 经过哪个转换操作得到的。

窄依赖允许子 RDD 的每个分区可以被并行处理产生，而宽依赖则必须等父 RDD 的所有分区都被计算好之后才能开始处理。

### 窄依赖
父 RDD 的分区可以一一对应到子 RDD 的分区，
![](assets/markdown-img-paste-20190525230216714.png)

### 宽依赖
父 RDD 的每个分区可以被多个子 RDD 的分区使用。
![](assets/markdown-img-paste-20190525230243112.png)

一些转换操作如 map、filter 会产生窄依赖关系，而 Join、groupBy 则会生成宽依赖关系。

map 是将分区里的每一个元素通过计算转化为另一个元素，一个分区里的数据不会跑到两个不同的分区。而 groupBy 则要将拥有所有分区里有相同 Key 的元素放到同一个目标分区，而每一个父分区都可能包含各种 Key 的元素，所以它可能被任意一个子分区所依赖。

### 如此设计的原因
窄依赖可以支持在同一个节点上链式执行多条命令，例如在执行了 map 后，紧接着执行filter。相反，宽依赖需要所有的父分区都是可用的，可能还需要调用类似 MapReduce之类的操作进行跨节点传递。

从失败恢复的角度考虑，窄依赖的失败恢复更有效，因为它只需要重新计算丢失的父分区即可，而宽依赖牵涉到 RDD 各级的多个父分区。

## 总结
弹性分布式数据集作为 Spark 的基本数据抽象，相较于 Hadoop/MapReduce 的数据模型而言，各方面都有很大的提升。

大提升了数据容错性和错误恢复的正确率，使 Spark 更加可靠。

## Q&A 1
Q:窄依赖是指父 RDD 的每一个分区都可以唯一对应子 RDD 中的分区，那么是否意味着子RDD 中的一个分区只可以对应父 RDD 中的一个分区呢？
A:不是。窄依赖的父RDD必须有一个对应的子RDD，也就是说父RDD的一个分区只能被子RDD一个分区使用，但是反过来子RDD的一个分区可以使用父RDD的多个分区。


Q:如果子 RDD 的一个分区需要由父RDD 中若干个分区计算得来，是否还算窄依赖？
A:算。只有当子RDD分区依赖的父RDD分区不被其他子RDD分区依赖，这样的计算就是窄依赖，否则是宽依赖。

## RDD结构
### Checkpoint
计算过程中，耗时的RDD可以缓存到硬盘或者HDFS，标记这个RDD被检查点处理过，并清空依赖关系，新建依赖于CheckpointRDD的依赖关系。

CheckpointRDD可以用来从硬盘中读取RDD和生成新的分区信息。当某个子 RDD 需要错误恢复时，回溯至该 RDD，发现它被检查点记录过，就可以直接去硬盘中读取这个 RDD，而无需再向前回溯计算。

### Storage Level
- MEMORY_ONLY:默认。只缓存在内存中，如果内存空间不够则不缓存多出来的部分。
- MEMORY_AND_DISK：缓存在内存中，如果空间不够则缓存在硬盘中。
- DISK_ONLY：只缓存在硬盘中。
- MEMORY_ONLY_2 和 MEMORY_AND_DISK_2 等：与上面的级别功能相同，只不过每个分区在集群中两个节点上建立副本。

### Iterator
迭代函数（Iterator）和计算函数（Compute）是用来表示 RDD 怎样通过父 RDD 计算得到的。

迭代函数会首先判断缓存中是否有想要计算的 RDD，如果有就直接读取，如果没有，就查数向上递归，查找父 RDD 进行计算。

## RDD Transformation转换
将一个RDD转换为另一个RDD。eg：map，filter，groupByKey

### Map
与 MapReduce 中的 map 一样，它把一个 RDD 中的所有数据通过一个函数，映射成一个新的
RDD，任何原 RDD 中的元素在新 RDD 中都有且只有一个元素与之对应。
```
rdd = sc.parallelize(["b", "a", "c"])
rdd2 = rdd.map(lambda x: (x, 1)) // [('b', 1), ('a', 1), ('c', 1)]
```

### Filter
是选择原 RDD 里所有数据中满足某个特定条件的数据，去返回一个新的
RDD。如下例所示，通过 filter，只选出了所有的偶数。
```
rdd = sc.parallelize([1, 2, 3, 4, 5])
rdd2 = rdd.filter(lambda x: x % 2 == 0) // [2, 4]
```

### mapPartitions
mapPartitions 是 map 的变种。不同于 map 的输入函数是应用于 RDD 中每个元素，
mapPartitions 的输入函数是应用于 RDD 的每个分区，也就是把每个分区中的内容作为整体来
处理的，所以输入函数的类型是 Iterator[T] => Iterator[U]。
```
rdd = sc.parallelize([1, 2, 3, 4], 2)
def f(iterator): yield sum(iterator)
rdd2 = rdd.mapPartitions(f) // [3, 7]
```

### groupByKey
groupByKey 和 SQL 中的 groupBy 类似，是把对象的集合按某个 Key 来归类，返回的 RDD 中
每个 Key 对应一个序列。
```
rdd = sc.parallelize([("a", 1), ("b", 1), ("a", 2)])
rdd.groupByKey().collect()
//"a" [1, 2]
//"b" [1]
```

## RDD Action
### Collect
RDD 中的动作操作 collect 与函数式编程中的 collect 类似，它会以数组的形式，返回
RDD 的所有元素。需要注意的是，collect 操作只有在输出数组所含的数据数量较小时使
用，因为所有的数据都会载入到程序的内存中，如果数据量较大，会占用大量 JVM 内存，
导致内存溢出。

```
rdd = sc.parallelize(["b", "a", "c"])
rdd.map(lambda x: (x, 1)).collect() // [('b', 1), ('a', 1), ('c', 1)]
```
上述转换操作中所有的例子，最后都需要将 RDD 的元素 collect 成数组才能得到
标记好的输出。

### Reduce
与 MapReduce 中的 reduce 类似，它会把 RDD 中的元素根据一个输入函数聚合起来。
```
from operator import add
sc.parallelize([1, 2, 3, 4, 5]).reduce(add) // 15
```

### Count
Count 会返回 RDD 中元素的个数。
```
sc.parallelize([2, 3, 4]).count() // 3
```

### CountByKey
仅适用于 Key-Value pair 类型的 RDD，返回具有每个 key 的计数的 <Key, Count> 的 map。
```
rdd = sc.parallelize([("a", 1), ("b", 1), ("a", 1)])
sorted(rdd.countByKey().items()) // [('a', 2), ('b', 1)]
```

## 惰性求值
转换是生成新的 RDD，动作是把 RDD 进行计算生成一个结果。

转换操作，它只是生成新的 RDD，并且记录依赖关系。
但是 Spark 并不会立刻计算出新 RDD 中各个分区的数值。直到遇到一个动作时，数据才会
被计算，并且输出结果给 Driver。

比如，在之前的例子中，你先对 RDD 进行 map 转换，再进行 collect 动作，这时 map 后
生成的 RDD 不会立即被计算。只有当执行到 collect 操作时，map 才会被计算。而且，
map 之后得到的较大的数据量并不会传给 Driver，只有 collect 动作的结果才会传递给
Driver。

### 优势
假设，你要从一个很大的文本文件中筛选出包含某个词语的行，然后返回第一个这样的文本
行。你需要先读取文件 textFile() 生成 rdd1，然后使用 filter() 方法生成 rdd2，最后是行动
操作 first()，返回第一个元素。

以实际上，Spark 是在行动操作 first() 的时候开始真正的运算：只扫描第一个匹配的行，
不需要读取整个文件。所以，惰性求值的设计可以让 Spark 的运算更加高效和快速。

Spark 在每次转换操作的时候，使用了新产生的 RDD 来记录计算逻辑，这样就把作用在 RDD
上的所有计算逻辑串起来，形成了一个链条。当对 RDD 进行动作时，Spark 会从计算链的最后
一个 RDD 开始，依次从上一个 RDD 获取数据并执行计算逻辑，最后输出结果。

## RDD持久化(缓存)
每当我们对 RDD 调用一个新的 action 操作时，整个 RDD 都会从头开始运算。因此，如果
某个 RDD 会被反复重用的话，每次都从头计算非常低效，我们应该对多次使用的 RDD 进
行一个持久化操作。

Spark 的 `persist()` 和 `cache()`  方法支持将 RDD 的数据缓存至内存或硬盘中，这样当下次
对同一 RDD 进行 Action 操作时，可以直接读取 RDD 的结果，大幅提高了 Spark 的计算效率。
```
rdd = sc.parallelize([1, 2, 3, 4, 5])
rdd1 = rdd.map(lambda x: x+5)
rdd2 = rdd1.filter(lambda x: x % 2 == 0)
rdd2.persist()
count = rdd2.count() // 3
first = rdd2.first() // 6
rdd2.unpersist()
```
我们对 RDD2 进行了多个不同的 action 操作。由于在第
四行我把 RDD2 的结果缓存在内存中，所以无论是 count 还是 first，Spark 都无需从一开
始的 rdd 开始算起了。
在缓存 RDD 的时候，它所有的依赖关系也会被一并存下来。所以持久化的 RDD 有自动的
容错机制。如果 RDD 的任一分区丢失了，通过使用原先创建它的转换操作，它将会被自动
重算。
持久化可以选择不同的存储级别。正如我们讲 RDD 的结构时提到的一样，有
MEMORY_ONLY，MEMORY_AND_DISK，DISK_ONLY 等。cache() 方法会默认取 MEMORY_ONLY 这一级
别。

## 总结
Spark 在每次转换操作的时候使用了新产生的 RDD 来记录计算逻辑，这样就把作用在 RDD上的所有计算逻辑串起来形成了一个链条，但是并不会真的去计算结果。当对 RDD 进行动作Action 时，Spark 会从计算链的最后一个 RDD 开始，利用迭代函数（Iterator）和计算函数（Compute），依次从上一个 RDD 获取数据并执行计算逻辑，最后输出结果。

此外，我们可以通过将一些需要复杂计算和经常调用的 RDD 进行持久化处理，从而提升计算
效率。

### RDD持久化操作与Checkpoint区别
Checkpoint会清空该RDD的依赖关系，并新建一个CheckpointRDD依赖关系，让该RDD依赖，并保存在磁盘或HDFS文件系统中，当数据恢复时，可通过CheckpointRDD读取RDD进行数据计算；持久化RDD会保存依赖关系和计算结果至内存中，可用于后续计算。
