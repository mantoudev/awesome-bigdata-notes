
# 线程生命周期
不同开发语言中线程本质都是对操作系统线程的封装。

## 1. 通用线程生命周期
<img src="assets/markdown-img-paste-20190417142958555.png" hegiht="200" />

1. **初始状态**：指的是线程已经被创建，但是还不允许分配 CPU 执行。这个状态属于编程语言特 有的，不过这里所谓的被创建，仅仅是在编程语言层面被创建，而在操作系统层面，真正的线 程还没有创建。
2. **可运行状态**：指的是线程可以分配 CPU 执行。在这种状态下，真正的操作系统线程已经被成功创建了，所以可以分配 CPU 执行。
3. **运行状态**：当有空闲的 CPU 时，操作系统会将其分配给一个处于可运行状态的线程，被分配到 CPU 的线 程的状态就转换成了运行状态。
4. **休眠状态**：运行状态的线程如果调用一个阻塞的 API(例如以阻塞方式读文件)或者等待某个事件(例如 条件变量)，那么线程的状态就会转换到休眠状态，同时释放 CPU 使用权，休眠状态的线程 永远没有机会获得 CPU 使用权。当等待的事件出现了，线程就会从休眠状态转换到可运行状 态。
5. **终止状态**：线程执行完或者出现异常就会进入终止状态，终止状态的线程不会切换到其他任何状态，进入 终止状态也就意味着线程的生命周期结束了

Java 语言里则把`可运行状态`和`运行状态`合并了，这两个状态在操作系统调度层面有用，而 JVM 层面不关心这两个状态，因为 JVM 把线程调度交给操作系统处理了。

## 2. Java中线程的生命周期
Java语言有六种状态：
1. NEW(初始化状态)
2. RUNNABLE(可运行 / 运行状态)
3. BLOCKED(阻塞状态)
4. WAITING(无时限等待)
5. TIMED_WAITING(有时限等待)
6. TERMINATED(终止状态)

在操作系统层面，Java 线程中的 `BLOCKED`、 `WAITING`、`TIMED_WAITING` 是一种状态，即前面我们提到的**休眠状态**。也就是说只要 Java 线程处于这三种状态之一，那么这个线程就永远没有 CPU 的使用权。

![](assets/markdown-img-paste-20190417144058803.png)

### 2.1 RUNNABLE 与 BLOCKED 的状态转换
只有一种场景会触发这种转换，就是线程等待 synchronized 的隐式锁。synchronized 修饰的 方法、代码块同一时刻只允许一个线程执行，其他线程只能等待，这种情况下，等待的线程就会 从 RUNNABLE 转换到 BLOCKED 状态。而当等待的线程获得 synchronized 隐式锁时，就又会 从 BLOCKED 转换到 RUNNABLE 状态。

#### 线程调用阻塞式API
- **操作系统层面**，线程是会转换到休眠状态的，
- **JVM 层面**，Java 线程的状态会依然保持 `RUNNABLE`状态。

**JVM 层面并不关心操作系统调度相关的状态**，因为在 JVM 看来，等待 CPU 使用权(操作系统层面此时处于可执行状态)与等待 I/O(操作系统层面此时处于休眠状态)没有区别，都是在等待某个资源，所以都归入了 RUNNABLE 状态。

>我们平时所谓的 Java 在调用阻塞式 API 时，线程会阻塞，指的是操作系统线程的状态，并不 是 Java 线程的状态。

### 2.2 RUNNABLE 与 WAITING 的状态转换
#### (1) synchronized
获得 synchronized 隐式锁的线程，调用无参数的 Object.wait() 方法。

#### (2) Thread.join()
调用无参数的 `Thread.join()` 方法。其中的 join() 是一种线程同步方法，例如有一 个线程对象 thread A，当调用 `A.join()` 的时候，执行这条语句的线程会等待 thread A 执行完， 而等待中的这个线程，其状态会从 RUNNABLE 转换到 WAITING。当线程 thread A 执行完，原 来等待它的线程又会从 WAITING 状态转换到 RUNNABLE。

#### (3)LockSupport.park()
调用 `LockSupport.park()` 方法，当前线程会阻塞， 线程的状态会从 RUNNABLE 转换到 WAITING。调用 LockSupport.unpark(Thread thread) 可 唤醒目标线程，目标线程的状态又会从 WAITING 状态转换到 RUNNABLE。

### 2.3 RUNNABLE 与 TIMED_WAITING 的状态转换
TIMED_WAITING 和 WAITING 状态的区别，仅仅是触发条件多了**超时参数**。

有五种场景会触发这种转换:
1. 调用带超时参数的 `Thread.sleep(long millis)` 方法;
2. 获得 synchronized 隐式锁的线程，调用带超时参数的 Object.wait(long timeout) 方法;
3. 调用带超时参数的 `Thread.join(long millis) 方法`;
4. 调用带超时参数的 `LockSupport.parkNanos(Object blocker, long deadline)` 方法;
5. 调用带超时参数的 `LockSupport.parkUntil(long deadline) `方法。

### 2.4 从 NEW 到 RUNNABLE 状态
Java 刚创建出来的 Thread 对象就是 NEW 状态，而创建 Thread 对象主要有两种方法。一种是 继承 Thread 对象，重写 run() 方法。

#### （1） Thread
示例代码如下:

```
// 自定义线程对象
class MyThread extends Thread {
  public void run() { // 线程需要执行的代码
    .......
  }
}
 // 创建线程对象
 MyThread myThread = new MyThread();
```
#### （2） Runnable
实现 Runnable 接口，重写 run() 方法，并将该实现类
作为创建 Thread 对象的参数。 示例代码如下:

```
// 实现 Runnable 接口
class Runner implements Runnable {
  @Override
  public void run() {
    // 线程需要执行的代码
    ......
  }
}

// 创建线程对象
Thread thread = new Thread(new Runner());

```

NEW 状态的线程，不会被操作系统调度，因此不会执行。Java 线程要执行，就必须转换到 RUNNABLE 状态。从 NEW 状态转换到 RUNNABLE 状态很简单，只要调用线程对象的 start() 方法就可以了。

### 2.5 从 RUNNABLE 到 TERMINATED 状态
TERMINATED状态情况：
1. 执行完run()方法；
2. 执行run()方法的时候抛出异常；
3. stop()方法(已过时)；
4. interrupt()

#### (1)stop() 和 interrupt() 方法区别
stop() 方法会真的杀死线程，不给线程喘息的机会，如果线程持有 synchronized 隐式锁，也不 会释放，那其他线程就再也没机会获得 synchronized 隐式锁，这实在是太危险了。所以该方法 就不建议使用了，类似的方法还有 suspend() 和 resume() 方法，这两个方法同样也都不建议使用了。

interrupt() 方法仅仅是通知线程，线程有机会执行一些后续操作，同时也可以无视这个通知。被 interrupt 的线程，是怎么收到通知的呢?一种是异常，另一种是主动检测。

**异常**
被中断的线程通过异常的方式获得了通知

- 其他线程调用了该线程的 interrupt() 方法。
- 当线程 A 处于 RUNNABLE 状态时，并且阻塞在 java.nio.channels.InterruptibleChannel 上 时，如果其他线程调用线程 A 的 interrupt() 方法，线程 A 会触发 java.nio.channels.ClosedByInterruptException 这个异常;而阻塞在 java.nio.channels.Selector 上时，如果其他线程调用线程 A 的 interrupt() 方法，线程 A 的 java.nio.channels.Selector 会立即返回。

**主动检测**
如果线程 处于 RUNNABLE 状态，并且没有阻塞在某个 I/O 操作上，例如中断计算圆周率的线程 A，这时 就得依赖线程 A 主动检测中断状态了。如果其他线程调用线程 A 的 interrupt() 方法，那么线程 A 可以通过 isInterrupted() 方法，检测是不是自己被中断了。

## Q & A
下面代码的本意是当前线程被中断之后，退出while(true)，你觉得这段代码是否正确呢?
```
Thread th = Thread.currentThread();
while (true) {
    if (th.isInterrupted()) {
        break;
    }
    // 省略业务代码无数
    try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
```

可能出现无限循环，线程在sleep期间被打断了，抛出一个InterruptedException异常，try catch捕捉 此异常，应该重置一下中断标示，因为抛出异常后，中断标示会自动清除掉!

```
Thread th = Thread.currentThread();
        while (true) {
            if (th.isInterrupted()) {
                break;
            }
            // 省略业务代码无数
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
```
