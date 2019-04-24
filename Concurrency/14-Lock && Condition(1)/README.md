# Lock&Condition(上):隐藏在并发包中的管程
并发编程领域两大核心问题：
- **互斥**：即同一时刻只允许一个线程访问共享资源。 —— Lock
- **同步**：线程之间如何通信、协作。 —— Condition

Java 语言本身提供的 synchronized 也是管程的一种实现，既然 Java 从语言层面已经实现了管 程了，那为什么还要在 SDK 里提供另外一种实现呢

## 1. 再造管程的理由
死锁解决方案 破坏不可抢占条件方案，synchronized没法解决。原因是 synchronized 申请资源的时候，如果申请不到，线程直 接进入阻塞状态了，而线程进入阻塞状态，啥都干不了，也释放不了线程已经占有的资源。但我们希望的是:

>对于“不可抢占”这个条件，占用部分资源的线程进一步申请其他资源时，如果申，可以主动释放它占有的资源，这样不可抢占这个条件就破坏掉了。

重新设计一把互斥锁去解决这个问题，有三种方案；
1. **能响应中断。**：synchronized 的问题是，持有锁 A 后，如果尝试获取锁 B 失败，那么线程就 进入阻塞状态，一旦发生死锁，就没有任何机会来唤醒阻塞的线程。但如果阻塞状态的线程能 够响应中断信号，也就是说当我们给阻塞的线程发送中断信号的时候，能够唤醒它，那它就有 机会释放曾经持有的锁 A。这样就破坏了不可抢占条件了。

2. **支持超时。**：如果线程在一段时间之内没有获取到锁，不是进入阻塞状态，而是返回一个错误， 那这个线程也有机会释放曾经持有的锁。这样也能破坏不可抢占条件。

3. **非阻塞地获取锁。**如果尝试获取锁失败，并不进入阻塞状态，而是直接返回，那这个线程也有 机会释放曾经持有的锁。这样也能破坏不可抢占条件。

这三个方案对应就是Lock接口的三个方法：
```
// 支持中断的API
void lockInterruptibly() throws InterruptedException；

  //支持超时的API
void trylock(long time，TimeUnit unit) throws InterruptedException;

// 支持非阻塞获取锁的API
boolean tryLock();

```
## 2. 如何保持可见性
Java 里多线程的可见性是通过 Happens-Before 规则保证的，而 synchronized 之所以能够保证可见性，也是因为有一条 synchronized 相关的规则:**synchronized 的解锁 Happens-Before 于后续对这个锁的加锁**。那 Java SDK 里面 Lock 靠什么保证可见性呢?例如在下面的代码中，线程 T1 对 value 进 行了 +=1 操作，那后续的线程 T2 能够看到 value 的正确结果吗?

```
class X {
  private final Lock rtl =
  new ReentrantLock();
  int value;
  public void addOne() {
    // 获取锁 rtl.lock();
    try {
      value+=1;
    } finally {
// 保证锁能释放
      rtl.unlock();
    }
  }
}
```
JDK里面的锁利用了volatile 相关的 Happens-Before 规则。Java SDK 里面的 ReentrantLock，内部持有一个 volatile 的成员变量 state，获取锁的时候，会读写 state 的值; 解锁的时候，也会读写 state 的值(简化后的代码如下面所示)。也就是说，在执行 value+=1 之前，程序先读写了一次 volatile 变量 state，在执行 value+=1 之后，又读写了一次 volatile 变量 state。根据相关的 Happens-Before 规则:
1. **顺序性规则**:对于线程 T1，value+=1 Happens-Before 释放锁的操作 unlock();
2. **volatile 变量规则**:由于 state = 1 会先读取 state，所以线程 T1 的 unlock() 操作
Happens-Before 线程 T2 的 lock() 操作;
3. **传递性规则**:线程 T2 的 lock() 操作 Happens-Before 线程 T1 的 value+=1 。

## 3. 可重入锁
**ReentrantLock**：线程可以重复获取同一把锁

例如下面代码中，当线程 T1 执行到 1 处时，已经获取到了锁 rtl ，当在 1 处调用 get() 方法时，会在 2 再次对锁 rtl 执行加锁操作。此时，如果锁 rtl 是可重入的，那么线程 T1 可以再次加锁成功;如果锁 rtl 是不可重入的，那么线程 T1 此时会被阻塞。

**可重入函数**：多个线程可以同时调用该函数

每个线程都能得到正确 结果;同时在一个线程内支持线程切换，无论被切换多少次，结果都是正确的。多线程可以同时 执行，还支持线程切换，这意味着可重入函数是线程安全的。

```
clss X 「
  private final Lock rtl = new ReentrantLock();
  int value;
  public int get(){
    //获取锁
    rtl.lock();
    try {
      return value;
    } finally {
      // 保证锁能释放
      rtl.unlock();
    }
  }

  public void addone(){
    // 获取锁
    rtl.lock();
    try {
      value = 1+ get();
    } finally {
      rtl.unlock();
    }
  }
```

## 4. 公平锁和非公平锁
ReentrantLock 这个类有两个构造函数：
- 无参构造函数：
- 传入fail参数的构造函数：传入true表示要构造一个公平锁，false，构造一个非公平锁。

```
//无参构造函数
publci ReentrantLock{
  sync = new NonfairSync();
}

//根据公平策略参数创建锁
public ReentrantLock(boolean fair){
  sync = fair ? new FairSync() : new NonfairSync();
}
```
入口等待队列，锁都对应着一个等待队列，如果一个线程没有获得锁，就会进入等待队列，当有线程释放锁的时候，就需要从等待队列中唤醒一个等待的线程。
- 公平锁：唤醒的策略就是谁等待的时间长，就唤醒谁，很公平
- 非公平锁：则不提供这个公平保证，有可能等待时间短的线程反而先被唤醒。

## 5. 最佳实践
并发大师 Doug Lea《Java 并发编程:设计原则与模式》一书中，推荐的三个用锁的最佳实践，它们分别是:

1. 永远只在更新对象的成员变量时加锁
2. 永远只在访问可变的成员变量时加锁
3. 永远不在调用其他对象的方法时加锁

其他优化：减少锁的持有时间、减小锁的粒度等等。
