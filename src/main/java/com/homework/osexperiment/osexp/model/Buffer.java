package com.homework.osexperiment.osexp.model;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 缓冲区类 - 带锁管理和容量限制
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class Buffer {
    private Queue<Product> buffer;      // 产品队列
    private int capacity;               // 容量上限
    private boolean isLocked;           // 是否被锁定
    private String lockedBy;            // 被谁锁定（"生产者" 或 "消费者"）
    
    /**
     * 构造函数
     */
    public Buffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new LinkedList<>();
        this.isLocked = false;
        this.lockedBy = null;
    }
    
    /**
     * 尝试获取锁
     * @param requester 请求者名称
     * @return 是否成功获取锁
     */
    public synchronized boolean tryLock(String requester) {
        if (!isLocked) {
            isLocked = true;
            lockedBy = requester;
            System.out.println("[调试] " + requester + " 获取了缓冲区锁");
            return true;
        }
        return false;
    }
    
    /**
     * 释放锁
     */
    public synchronized void unlock() {
        isLocked = false;
        String locker = lockedBy;
        lockedBy = null;
        System.out.println("[调试] " + locker + " 释放了缓冲区锁");
    }
    
    /**
     * 生产产品（放入缓冲区）
     * @param product 产品对象
     * @return 是否成功生产
     */
    public synchronized boolean produce(Product product) {
        if (buffer.size() >= capacity) {
            System.out.println("[调试] 缓冲区已满，无法生产产品 #" + product.getId());
            return false;
        }
        
        buffer.offer(product);
        System.out.println("[调试] 生产了产品 #" + product.getId() + "，当前库存：" + buffer.size());
        return true;
    }
    
    /**
     * 消费产品（从缓冲区取出）
     * @return 产品对象，如果缓冲区为空则返回 null
     */
    public synchronized Product consume() {
        if (buffer.isEmpty()) {
            System.out.println("[调试] 缓冲区为空，无法消费");
            return null;
        }
        
        Product product = buffer.poll();
        System.out.println("[调试] 消费了产品 #" + product.getId() + "，当前库存：" + buffer.size());
        return product;
    }
    
    /**
     * 检查是否为空
     */
    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }
    
    /**
     * 检查是否已满
     */
    public synchronized boolean isFull() {
        return buffer.size() >= capacity;
    }
    
    /**
     * 获取当前产品数量
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
     * 检查是否被锁定
     */
    public boolean isLocked() {
        return isLocked;
    }
    
    /**
     * 获取锁定者名称
     */
    public String getLockedBy() {
        return lockedBy;
    }
    
    /**
     * 清空缓冲区
     */
    public synchronized void clear() {
        buffer.clear();
        unlock();
    }
}
