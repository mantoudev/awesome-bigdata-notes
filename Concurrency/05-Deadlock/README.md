# 死锁
一组互相竞争资源的线程因互相等待，导致“永久”阻塞的现象。

## 1. 预防死锁

### 1.1 死锁出现的四个条件
1. 互斥，共享资源 X 和 Y 只能被一个线程占用;
2. 占有且等待，线程 T1 已经取得共享资源 X，在等待共享资源 Y 的时候，不释放共享资源 X;
3. 不可抢占，其他线程不能强行抢占线程 T1 占有的资源;
4. 循环等待，线程 T1 等待线程 T2 占有的资源，线程 T2 等待线程 T1 占有的资源，就是循环等待。

也就是说只要我们破坏其中一个，就可以成功避免死锁的发生.

其中，互斥这个条件我们没有办法破坏，因为我们用锁为的就是互斥。不过其他三个条件都是有
办法破坏掉的，到底如何做呢?
1. 对于“占用且等待”这个条件，我们可以一次性申请所有的资源，这样就不存在等待了。
2. 对于“不可抢占”这个条件，占用部分资源的线程进一步申请其他资源时，如果申请不到，可
 以主动释放它占有的资源，这样不可抢占这个条件就破坏掉了。
3. 对于“循环等待”这个条件，可以靠按序申请资源来预防。所谓按序申请，是指资源是有线性
 顺序的，申请的时候可以先申请资源序号小的，再申请资源序号大的，这样线性化后自然就不
 存在循环了。

## 2. 避免死锁
### 2.1 破坏占用且等待条件
一次申请所有资源

 ![](assets/markdown-img-paste-20190421205811945.png)

 对应到编程领域，“同时申请”这个操作是一个临界区，我们也需要一个角色(Java 里面的类) 来管理这个临界区，我们就把这个角色定为 Allocator。它有两个重要功能，分别是:**同时申请资源 apply() 和同时释放资源 free()**。账户 Account 类里面持有一个 Allocator 的单例(必须是 单例，只能由一个人来分配资源)。当账户 Account 在执行转账操作的时候，首先向 Allocator 同时申请转出账户和转入账户这两个资源，成功后再锁定这两个资源;当转账操作执行完，释放锁之后，我们需通知 Allocator 同时释放转出账户和转入账户这两个资源。具体的代码实现如下。
 ```
 class Allocator {
   private List<Object> als = new ArryaList<>();
   // 一次性申请所有资源
   synchronized boolean apply(Object from, Object to){
     if(als.contains(from) || als.contains(to)){
       return false;
     } else {
       als.add(from);
       als.add(to);
     }
     return true;
   }

   // 归还资源
   synchronized void free(Object from, Object to){
     als.remove(from);
     als.remove(to);
   }
 }

class Account {
  // actr 应该为单例
  private Allocator actr;
  private int balance;

  //转账
  void transfer(Account target, int amt){
    //一次性申请转出账户和转入账户，直到成功
    while(!actr.apply(this, target));
    try{
      //锁定转出账户
      synchronized(this){
        //锁定转入账户
        synchronized(target){
          if (this.balance > amt){
            this.balance   -= amt;
            target.balance += amt;
          }
        }
      }
    } finally {
      actr.free(this, target);
    }
  }
}
 ```

### 2.2 破坏不可抢占条件
synchronized做不到，因为synchronized 申请资源的时候，如果申请不到，线程直接进入阻塞状态了，而线程进入阻塞状态，啥都干不了，也释放不了线程已经占有的资源。

java.util.concurrent 这个包下面提供的 Lock 是可以轻松解决这个问题的。

### 2.3 破坏循环等待条件
破坏这个条件，需要对资源进行排序，然后按序申请资源。

这个实现非常简单，我们假设每个账户都有不同的属性 id，这个 id 可以作为排序字段，申请的时候，我们可以按照从小到大的顺序来申请。比如下面代码中，1~6处的代码对转出账户(this)和转入账户(target)排序，然后按照序号从小到大的顺序锁定账户。这样就不存在“循环”等待了。

```
class Account {
  private int id;
  private int balance;
  // 转账
  void transfer(Account target, int amt){
    // 排序
    Account left  = this;
    Account right = target;
    if (this.id > target.id){
      left  = target;
      right = this;
    }

    // 锁定序号小的账户
    synchronized（left){
      // 锁定序号大的账户
      synchronized(right){
        if (this.balance > amt){
          this.balance   -= amt;
          target.balance += amt;
        }
      }
    }
   }
}
```
