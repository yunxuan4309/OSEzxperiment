package com.homework.osexperiment.osexp.model;

import java.util.concurrent.Semaphore;

/**
 * AND 信号量类 - 模拟操作系统的 Swait/Ssignal 操作
 * 支持同时申请/释放多个信号量资源（原子操作）
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class AndSemaphore {
    private Semaphore[] semaphores;
    private String[] names;
    
    // 信号量索引常量
    private static final int MUTEX_INDEX = 0;
    private static final int EMPTY_INDEX = 1;
    private static final int FULL_INDEX = 2;
    
    // 用于 UI 显示的状态标志（volatile 保证可见性）
    private volatile boolean mutexLocked = false;  // mutex 是否被锁定
    
    /**
     * 构造函数
     * @param permits 每个信号量的初始值
     * @param names 每个信号量的名称
     */
    public AndSemaphore(int[] permits, String[] names) {
        if (permits.length != names.length) {
            throw new IllegalArgumentException("信号量数量和名称数量必须一致");
        }
        this.semaphores = new Semaphore[permits.length];
        this.names = names.clone();
        for (int i = 0; i < permits.length; i++) {
            this.semaphores[i] = new Semaphore(permits[i]);
        }
    }
    
    /**
     * Swait 操作 - 同时申请所有信号量（原子操作）
     * 要么全部获取成功，要么全部不获取（避免死锁）
     * 
     * @throws InterruptedException 如果线程被中断
     */
    public void swait() throws InterruptedException {
        swaitForAll();
    }
    
    /**
     * 生产者专用的 Swait - 只获取 empty 和 mutex
     */
    public void swaitForProducer() throws InterruptedException {
        // 生产者只需要 empty (index=1) 和 mutex (index=0)
        int[] indices = {EMPTY_INDEX, MUTEX_INDEX};
        swaitForIndices(indices);
    }
    
    /**
     * 消费者专用的 Swait - 只获取 full 和 mutex
     */
    public void swaitForConsumer() throws InterruptedException {
        // 消费者只需要 full (index=2) 和 mutex (index=0)
        int[] indices = {FULL_INDEX, MUTEX_INDEX};
        swaitForIndices(indices);
    }
    
    /**
     * 根据指定的索引数组获取信号量
     */
    private void swaitForIndices(int[] indices) throws InterruptedException {
        boolean success = false;
        int retryCount = 0;
        
        while (!success) {
            // 检查是否被中断
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("线程被中断");
            }
            
            int acquiredCount = 0;
            boolean allAcquired = true;
            int[] acquiredIndices = new int[indices.length];
            
            // 尝试非阻塞地获取指定的信号量
            for (int i = 0; i < indices.length; i++) {
                int idx = indices[i];
                boolean acquired = semaphores[idx].tryAcquire();
                if (acquired) {
                    acquiredIndices[acquiredCount] = idx;
                    acquiredCount++;
                } else {
                    allAcquired = false;
                    break;
                }
            }
            
            // 如果全部获取成功
            if (allAcquired && acquiredCount == indices.length) {
                success = true;
                // 如果获取了 mutex，设置锁定标志
                for (int idx : indices) {
                    if (idx == MUTEX_INDEX) {
                        mutexLocked = true;
                        break;
                    }
                }
                logSwaitSuccessForIndices(indices);
            } else {
                // 失败则释放已获取的信号量
                for (int i = 0; i < acquiredCount; i++) {
                    semaphores[acquiredIndices[i]].release();
                }
                
                // 每20次重试输出一次日志，避免刷屏
                retryCount++;
                if (retryCount % 20 == 1) {
                    System.out.println("[AND信号量-Swait] 等待中... (第" + retryCount + "次重试)");
                }
                
                // 短暂等待后重试（避免忙等待）
                Thread.sleep(50);
            }
        }
    }
    
    /**
     * 为所有信号量的 Swait（保留原有功能）
     */
    private void swaitForAll() throws InterruptedException {
        boolean success = false;
        int retryCount = 0;
        
        while (!success) {
            // 检查是否被中断
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("线程被中断");
            }
            
            int acquiredCount = 0;
            boolean allAcquired = true;
            
            // 尝试非阻塞地获取所有信号量
            for (int i = 0; i < semaphores.length; i++) {
                boolean acquired = semaphores[i].tryAcquire();
                if (acquired) {
                    acquiredCount++;
                    System.out.println("  [DEBUG] 成功获取 " + names[i] + " (当前值: " + semaphores[i].availablePermits() + ")");
                } else {
                    allAcquired = false;
                    System.out.println("  [DEBUG] 无法获取 " + names[i] + " (当前值: " + semaphores[i].availablePermits() + ")");
                    break;
                }
            }
            
            // 如果全部获取成功
            if (allAcquired && acquiredCount == semaphores.length) {
                success = true;
                logSwaitSuccess();
            } else {
                // 失败则释放已获取的信号量
                releaseAll(acquiredCount);
                
                // 每10次重试输出一次日志，避免刷屏
                retryCount++;
                if (retryCount % 10 == 1) {
                    System.out.println("[AND信号量-Swait] 第" + retryCount + "次重试，已获取:" + acquiredCount + "/" + semaphores.length);
                }
                
                // 短暂等待后重试（避免忙等待）
                Thread.sleep(50);
            }
        }
    }
    
    /**
     * Ssignal 操作 - 同时释放所有信号量
     */
    public void ssignal() {
        ssignalForAll();
    }
    
    /**
     * 生产者专用的 Ssignal - 释放 mutex 和 full
     */
    public void ssignalForProducer() {
        // 生产者释放 mutex (index=0) 和 full (index=2)
        int[] indices = {MUTEX_INDEX, FULL_INDEX};
        ssignalForIndices(indices);
    }
    
    /**
     * 消费者专用的 Ssignal - 释放 mutex 和 empty
     */
    public void ssignalForConsumer() {
        // 消费者释放 mutex (index=0) 和 empty (index=1)
        int[] indices = {MUTEX_INDEX, EMPTY_INDEX};
        ssignalForIndices(indices);
    }
    
    /**
     * 根据指定的索引数组释放信号量
     */
    private void ssignalForIndices(int[] indices) {
        // 如果释放了 mutex，清除锁定标志
        for (int idx : indices) {
            if (idx == MUTEX_INDEX) {
                mutexLocked = false;
                break;
            }
        }
        
        for (int idx : indices) {
            semaphores[idx].release();
        }
        logSsignalForIndices(indices);
    }
    
    /**
     * 释放所有信号量（保留原有功能）
     */
    private void ssignalForAll() {
        for (Semaphore sem : semaphores) {
            sem.release();
        }
        logSsignal();
    }
    
    /**
     * 释放指定数量的已获取信号量
     */
    private void releaseAll(int count) {
        for (int i = 0; i < count; i++) {
            semaphores[i].release();
        }
    }
    
    /**
     * 获取某个信号量的当前值
     * @param index 信号量索引
     * @return 信号量的可用许可数
     */
    public int getSemaphoreValue(int index) {
        // 对于 mutex 信号量，使用状态标志确保 UI 能看到锁定状态
        if (index == MUTEX_INDEX) {
            return mutexLocked ? 0 : 1;
        }
        return semaphores[index].availablePermits();
    }
    
    /**
     * 获取信号量名称
     * @param index 信号量索引
     * @return 信号量名称
     */
    public String getSemaphoreName(int index) {
        return names[index];
    }
    
    /**
     * 获取信号量数量
     * @return 信号量数量
     */
    public int getSemaphoreCount() {
        return semaphores.length;
    }
    
    // ==================== 日志记录方法 ====================
    
    private void logSwaitSuccess() {
        StringBuilder sb = new StringBuilder("[AND信号量-Swait] 成功获取: ");
        for (String name : names) {
            sb.append(name).append(", ");
        }
        System.out.println(sb.toString());
    }
    
    private void logSwaitWaiting() {
        System.out.println("[AND信号量-Swait] 资源不足，等待中...");
    }
    
    private void logSsignal() {
        StringBuilder sb = new StringBuilder("[AND信号量-Ssignal] 释放: ");
        for (String name : names) {
            sb.append(name).append(", ");
        }
        System.out.println(sb.toString());
    }
    
    private void logSwaitSuccessForIndices(int[] indices) {
        StringBuilder sb = new StringBuilder("[AND信号量-Swait] 成功获取: ");
        for (int idx : indices) {
            sb.append(names[idx]).append(", ");
        }
        System.out.println(sb.toString());
    }
    
    private void logSsignalForIndices(int[] indices) {
        StringBuilder sb = new StringBuilder("[AND信号量-Ssignal] 释放: ");
        for (int idx : indices) {
            sb.append(names[idx]).append(", ");
        }
        System.out.println(sb.toString());
    }
}
