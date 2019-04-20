package com.mantoudev.concurrent.thread;

/**
 * @Description: TODO
 * @Author: Zeng
 * @Version: v1.0
 * @Date: 2019/4/17
 */
public class LifeRecyle {

    /**
     * 下面代码的本意是当前线程被中断之后，退出while(true)，你觉得这段代码是否正确呢?
     */
    public void func1() {
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
    }

    /**
     * 可能出现无限循环，线程在sleep期间被打断了，抛出一个InterruptedException异常，try catch捕捉 此异常，应该重置一下中断标示，
     * 因为抛出异常后，中断标示会自动清除掉!
     */
    public void func2() {
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
}