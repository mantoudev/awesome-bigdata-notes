# Routine
Learning and exercise notes.

- Java
  - [Java](#Java)
  - [Concurrency](#Concurrency)
  - [JVM](#JVM)

- BigData
  - [HDFS](#HDFS)
  - [MapReduce](#MapReduce)
  - [YARN](#YARN)
  - [Hbase](#Hbase)
  - [Hive](#Hive)
  - [Impala](#Impala)
  - [Spark](#Spark)
  - [Flink](#Flink)
  - [Kylin](#Kylin)

## Java
// Coming soon...

## Concurrency
| #   | Title                                | Tag                  | Desc                               |
| --- | ------------------------------------ | -------------------- | ---------------------------------- |
| 1   | [Visibility & Atomic & Order][2-1]     | Synchronized,MESA    | 可见性、原子性、有序性             |
| 2   | [Java Memory Model][2-2]               | JMM,Visibility,Order | Java内存模型，解决可见性、有序性题 |
| 3   | [Mutex Lock:Atomic][2-3]               | Lock,Atomic          | 互斥锁-解决原子性问题              |
| 4   | [Mutex Lock(2)][2-4]                   | Lock,Atomic          | 互斥锁-一把锁保护多个资源          |
| 5   | [Deadlock][2-5]                        | Deadlock             | 死锁                               |
| 6   | [Wait-Notify][2-6]                     | Lock                 | 等待通知机制                       |
| 7   | [Security、Activity、Performance][2-7] | Lock                  | 安全性、活跃性以及性能问题         |
| 8   | [Monitor][2-8]                         | Thread               | 管程                               |
| 9   | [Thread Lifecycle][2-9]                | Merory Model         | 线程-生命周期                      |
| 10  | [Thread Create][2-10]                  | Thread               | 线程-创建数量                      |
| 11  | [Thread Local Variable][2-11]          | Thread               | 线程-局部变量                      |
| 12  | [OO thinking for concurrency][2-12]    | Thread               | 用面向对象思想写好并发程序         |
| 13  | [Lock][2-13]                           | Lock                 | 隐藏在并发包中的管程               |
| 14  | [Condition][2-14]                      | Condition            | Condition                                 |

## JVM
| #   | Title               | Tag         | Desc   |
| --- | ------------------- | ----------- | ------ |
| 1   | [JK Note][3-1]      | JK          | 笔记   |
| 2   | [Class loader][3-2] | ClassLoader | 类加载 |

## HDFS
// coming soon...

## YARN
// coming soon...

## MapReduce
| #   | Title                  | Tag          | Desc       |
| --- | ---------------------- | ------------ | ---------- |
| 1   | [MR Architecture][6-1] | Architecture | MR体系结构 |

## HBase
| #   | Title                     | Tag                | Desc           |
| --- | ------------------------- | ------------------ | -------------- |
| 1   | [Hbase Architecture][7-1] | Architecture       | HBase 架构     |
| 2   | [Read Write Process][7-2] | Read Write Process | HBase 读写流程 |

## Hive

## Impala
| #   | Title                      | Tag          | Desc       |
| --- | -------------------------- | ------------ | ---------- |
| 1   | [Impala Architecture][9-1] | Architecture | Impala 架构 |

## Spark
// Coming soon...

## Flink
// Coming soon...

## Kylin
// Coming soon...

[2-1]: https://github.com/mantoudev/routine/tree/master/Concurrency/01-Visibility%20%26%20Atomic%20%26%20Order
[2-2]: https://github.com/mantoudev/routine/tree/master/Concurrency/02-Java%20Meroy%20Model
[2-3]: https://github.com/mantoudev/routine/tree/master/Concurrency/03-Mutex%20Lock:Atomic
[2-4]: https://github.com/mantoudev/routine/tree/master/Concurrency/04-Mutex%20Lock(2)
[2-5]: https://github.com/mantoudev/routine/tree/master/Concurrency/05-Deadlock
[2-6]: https://github.com/mantoudev/routine/tree/master/Concurrency/06-Wait-Notify
[2-7]: https://github.com/mantoudev/routine/tree/master/Concurrency/07-Security%E3%80%81Activity%E3%80%81Performance
[2-8]: https://github.com/mantoudev/routine/tree/master/Concurrency/08-Monitor
[2-9]: https://github.com/mantoudev/routine/tree/master/Concurrency/09-Thread:create
[2-10]: https://github.com/mantoudev/routine/tree/master/Concurrency/10-Thread:lifecyle
[2-11]: https://github.com/mantoudev/routine/tree/master/Concurrency/11-Thread:localVariable
[2-12]: https://github.com/mantoudev/routine/tree/master/Concurrency/12-OO%20thinking%20for%20concurrency
[2-13]: https://github.com/mantoudev/routine/tree/master/Concurrency/14-Lock
[2-14]: https://github.com/mantoudev/routine/tree/master/Concurrency/15-Condition

[3-1]: https://github.com/mantoudev/routine/tree/master/JVM/01-JK
[3-2]: https://github.com/mantoudev/routine/tree/master/JVM/02-Class%20Loader

[6-1]: https://github.com/mantoudev/routine/tree/master/MR/01-Architecture

[7-1]: https://github.com/mantoudev/routine/tree/master/HBase/01-Hbase%20Architecture
[7-2]: https://github.com/mantoudev/routine/tree/master/HBase/0Concurrency-Read%20Write%20Process

[9-1]: https://github.com/mantoudev/routine/tree/master/Impala/01-Introduce
