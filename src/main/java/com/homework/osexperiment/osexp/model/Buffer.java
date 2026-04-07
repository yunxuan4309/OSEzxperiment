package com.homework.osexperiment.osexp.model;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 缓冲区类 - 基于 AND 信号量实现生产者-消费者问题
 * 
 * @author 谢云轩
 * @version 2.0
 */
public class Buffer {
    private Queue<Product> buffer;
    private int capacity;
    private AndSemaphore andSemaphore;
    
    // 信号量索引常量
    private static final int MUTEX_INDEX = 0;  // 互斥信号量
    private static final int EMPTY_INDEX = 1;  // 空缓冲区信号量
    private static final int FULL_INDEX = 2;   // 满缓冲区信号量
    
    /**
     * 构造函数
     * @param capacity 缓冲区容量
     */
    public Buffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new LinkedList<>();
        
        // 初始化 AND 信号量：mutex=1, empty=capacity, full=0
        int[] permits = {1, capacity, 0};
        String[] names = {"mutex", "empty", "full"};
        this.andSemaphore = new AndSemaphore(permits, names);
    }
    
    /**
     * 生产者：生产产品（使用 AND 信号量的 Swait/Ssignal）
     * @param product 产品对象
     * @throws InterruptedException 如果线程被中断
     */
    public void produce(Product product) throws InterruptedException {
        System.out.println("\n========== 生产者开始生产 ==========");
        System.out.println("[P操作前] mutex=" + getMutexValue() + 
                          ", empty=" + getEmptyValue() + 
                          ", full=" + getFullValue());
        
        // 生产者只需要获取 empty 和 mutex，不需要 full
        System.out.println("[P操作] Swait(empty, mutex) - 申请空缓冲区和互斥锁");
        andSemaphore.swaitForProducer();
        
        System.out.println("[P操作后] mutex=" + getMutexValue() + 
                          ", empty=" + getEmptyValue() + 
                          ", full=" + getFullValue());
        
        // 临界区：将产品放入缓冲区
        buffer.offer(product);
        System.out.println("[临界区] 生产了产品 #" + product.getId() + 
                          "，当前库存：" + buffer.size());
        
        // 模拟临界区停留时间，让 UI 有机会捕捉到 mutex=0 的状态
        try {
            Thread.sleep(100);  // 保持 100ms，足够 UI 更新
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
        
        // Ssignal(mutex, full) - 释放互斥锁并增加满位
        System.out.println("[V操作] Ssignal(mutex, full) - 释放互斥锁并增加满缓冲区");
        andSemaphore.ssignalForProducer();
        
        System.out.println("[V操作后] mutex=" + getMutexValue() + 
                          ", empty=" + getEmptyValue() + 
                          ", full=" + getFullValue());
        System.out.println("========== 生产者完成生产 ==========\n");
    }
    
    /**
     * 消费者：消费产品（使用 AND 信号量的 Swait/Ssignal）
     * @return 产品对象
     * @throws InterruptedException 如果线程被中断
     */
    public Product consume() throws InterruptedException {
        System.out.println("\n========== 消费者开始消费 ==========");
        System.out.println("[P操作前] mutex=" + getMutexValue() + 
                          ", empty=" + getEmptyValue() + 
                          ", full=" + getFullValue());
        
        // 消费者只需要获取 full 和 mutex，不需要 empty
        System.out.println("[P操作] Swait(full, mutex) - 申请满缓冲区和互斥锁");
        andSemaphore.swaitForConsumer();
        
        System.out.println("[P操作后] mutex=" + getMutexValue() + 
                          ", empty=" + getEmptyValue() + 
                          ", full=" + getFullValue());
        
        // 临界区：从缓冲区取出产品
        Product product = buffer.poll();
        System.out.println("[临界区] 消费了产品 #" + product.getId() + 
                          "，当前库存：" + buffer.size());
        
        // 模拟临界区停留时间，让 UI 有机会捕捉到 mutex=0 的状态
        try {
            Thread.sleep(100);  // 保持 100ms，足够 UI 更新
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
        
        // Ssignal(mutex, empty) - 释放互斥锁并增加空位
        System.out.println("[V操作] Ssignal(mutex, empty) - 释放互斥锁并增加空缓冲区");
        andSemaphore.ssignalForConsumer();
        
        System.out.println("[V操作后] mutex=" + getMutexValue() + 
                          ", empty=" + getEmptyValue() + 
                          ", full=" + getFullValue());
        System.out.println("========== 消费者完成消费 ==========\n");
        
        return product;
    }
    
    /**
     * 检查是否为空
     * @return 如果缓冲区为空返回 true
     */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }
    
    /**
     * 检查是否已满
     * @return 如果缓冲区已满返回 true
     */
    public boolean isFull() {
        return buffer.size() >= capacity;
    }
    
    /**
     * 获取当前产品数量
     * @return 缓冲区中的产品数量
     */
    public synchronized int size() {
        return buffer.size();
    }
    
    /**
     * 获取容量上限
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * 获取 AND 信号量对象
     * @return AND 信号量实例
     */
    public AndSemaphore getAndSemaphore() {
        return andSemaphore;
    }
    
    /**
     * 获取 mutex 信号量值
     * @return mutex 信号量的当前值
     */
    public int getMutexValue() {
        return andSemaphore.getSemaphoreValue(MUTEX_INDEX);
    }
    
    /**
     * 获取 empty 信号量值
     * @return empty 信号量的当前值
     */
    public int getEmptyValue() {
        return andSemaphore.getSemaphoreValue(EMPTY_INDEX);
    }
    
    /**
     * 获取 full 信号量值
     * @return full 信号量的当前值
     */
    public int getFullValue() {
        return andSemaphore.getSemaphoreValue(FULL_INDEX);
    }
    
    /**
     * 清空缓冲区
     */
    public synchronized void clear() {
        buffer.clear();
    }
}
