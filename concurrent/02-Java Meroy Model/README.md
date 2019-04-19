# Java内存模型：解决可见性、有序性问题
导致可见性的原因是缓存，导致有序性的原因是编译优化，那解决可见性、 有序性最直接的办法就是**按需禁用缓存和编译优化**，

Java 内存模型规范了 JVM 如何提供按需禁用缓存和编译优化的 方法。具体来说，这些方法包括
- volatile
- synchronized
- final
- 六项 Happens-Before 规则

## 1. volatile
volatile 关键字并不是 Java 语言的特产，古老的 C 语言里也有，它最原始的意义就是禁用 CPU 缓存。

声明一个 volatile 变量 volatile int x = 0，它表达的是:告诉编译器， 对这个变量的读写，不能使用 CPU 缓存，必须从内存中读取或者写入。

## 2. Happens-Before
前面一个操作的结果对后续操作是可见的。
Happens-Before 约束了编译器的优化行为，虽允许编译器优化，但是要求编译器优化后一定遵守 Happens- Before 规则。

### 2.1 顺序性规则
在一个线程中，按照程序顺序，前面的操作 Happens-Before 于后续的任意 操作。

程序前面对某个变量的修改一定是对后续操作可见的。

```
Class VolatileExample {
  int x=0;
  volatile boolean v = false;

  public void writer() {
    x = 42;
    v = true;
  }

  public void reader() {
    if (v == true) {
      // 这里 x 会是多少呢?
    }
  }
}
```
按照程序的顺序，第 6 行代码 “x = 42;” Happens-Before 于第 7 行代码 “v = true;”，这就是规则 1 的内容，也比 较符合单线程里面的思维:程序前面对某个变量的修改一定是对后续操作可见的。
