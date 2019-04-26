# Lock和Condition(下):Dubbo如何用管程实现异步转同步?
Lock 有别于 synchronized 隐式锁的三个特性:
- 能够响应中断
- 支持超时
- 非阻塞地获取锁。

**Condition 实现了管程模型里面的条件变量。**
Java 语言内置的管程里只有一个条件变 量，而Lock&Condition 实现的管程是支持多个条件变量的。

在很多并发场景下，支持多个条件变量能够让我们的并发程序可读性更好，实现起来也更容易。例如，实现一个阻塞队列，就需要两个条件变量。

## 1. 利用两个条件变量快速实现阻塞队列
一个阻塞队列，需要两个条件变量，一个是队列不空(空队列不允许出队)，另一个是队列不满
(队列已满不允许入队)。

```
public class BlockedQueue<T>{
  final Lock lock =
  new ReentrantLock();
  // 条件变量:队列不满
  final Condition notFull = lock.newCondition();
  // 条件变量:队列不空
  final Condition notEmpty = lock.newCondition();
  //入队
  void enq(T x) {
    lock.lock();
    try {
      while (队列已满){
        // 等待队列不满
        notFull.await();
      }
      // 省略入队操作... 、
      // 入队后, 通知可出队
      notEmpty.signal();
    }finally {
      lock.unlock();
      }
    }

  //出队
  void deq(){
    lock.lock();
    try {
      while (队列已空){
        // 等待队列不空
        notEmpty.await();
      }
      // 省略出队操作...
      // 出队后，通知可入队
      notFull.signal();
    }finally {
      lock.unlock();
    }
  }
}
```

Lock 和 Condition 实现的管程，线程等待和通知需要调用 await()、 signal()、signalAll()，它们的语义和 wait()、notify()、notifyAll() 是相同的。

但是不一样的是，Lock&Condition 实现的管程里只能使用前面的 await()、signal()、signalAll()，而后面的 wait()、notify()、notifyAll() 只有在 synchronized 实现的管程里才能使用。
