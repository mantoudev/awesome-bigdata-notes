package com.mantoudev.concurrent.monitor;

import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description: TODO
 * @Author: Zeng
 * @Version: v1.0
 * @Date: 2019/4/13
 */
public class BlockedQueue<T> {
    final Lock lock = new ReentrantLock();
    //条件变量：队列不满
    final Condition notFull = lock.newCondition();
    //条件变量：队列不空
    final Condition notEmpty = lock.newCondition();

    /**
     * 入队
     *
     * @param x
     */
    void enq(T x) {
        Lock


        lock.lock();
        while (队列已满) {
            try {
                //等待队列不满
                notFull.await();
                //省略入队操作...
                //入队后，通知可以出队
                notEmpty.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 出队
     *
     * @param x
     */
    void deq(T x) {
        lock.lock();
        while (队列已空) { /
            try {
                //等待队列不满
                notEmpty.await();
                //省略入队操作...
                //入队后，通知可以出队
                notFull.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }
}