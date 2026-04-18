package com.homework.osexperiment.osexp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 调度器类 - 实现 4 种调度算法
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class Scheduler {
    
    // 调度算法枚举
    public enum Algorithm {
        FCFS,     // 先来先服务
        SJF,      // 短作业优先
        RR,       // 时间片轮转
        PRIORITY, // 优先级调度
        MLFQ      // 多级反馈队列
    }
    
    private Algorithm algorithm;      // 当前算法
    private int timeSlice;            // 时间片大小（RR 用）
    private List<Process> processList; // 进程列表
    private List<Process> scheduleResult; // 调度结果序列
    
    // RR 算法统计信息
    private int rrMaxRounds = 2;        // RR 最大循环轮数
    private int rrUnfinishedCount = 0;  // RR 未完成进程数
    
    // MLFQ 配置（3级队列）
    private static final int MLFQ_LEVELS = 3;           // 队列层级数
    private int[] mlfqTimeSlices = {3, 3, 3};          // 各级队列的累计时间片上限
    @SuppressWarnings("unchecked")
    private Queue<Process>[] mlfqQueues = new LinkedList[MLFQ_LEVELS]; // 多级队列
    
    /**
     * 构造函数
     */
    public Scheduler(Algorithm algorithm, int timeSlice) {
        this.algorithm = algorithm;
        this.timeSlice = timeSlice;
        this.processList = new ArrayList<>();
        this.scheduleResult = new ArrayList<>();
        
        // 初始化 MLFQ 队列
        for (int i = 0; i < MLFQ_LEVELS; i++) {
            mlfqQueues[i] = new LinkedList<>();
        }
    }
    
    /**
     * 添加进程
     */
    public void addProcess(Process process) {
        processList.add(process);
    }
    
    /**
     * 清空所有进程
     */
    public void clear() {
        processList.clear();
        scheduleResult.clear();
        
        // 清空 MLFQ 队列
        for (int i = 0; i < MLFQ_LEVELS; i++) {
            mlfqQueues[i].clear();
        }
    }
    
    /**
     * 执行调度，返回调度结果序列
     */
    public List<Process> execute() {
        scheduleResult.clear();
        
        // MLFQ 需要特殊处理：先重置所有进程状态
        if (algorithm == Algorithm.MLFQ) {
            for (Process p : processList) {
                p.setRemainingTime(p.getBurstTime());
                p.setCurrentQueueLevel(0);
                p.setTotalExecutedTime(0);
                p.setWaitingTime(0);
                p.setTurnaroundTime(0);
            }
        }
        
        switch (algorithm) {
            case FCFS:
                scheduleFCFS();
                break;
            case SJF:
                scheduleSJF();
                break;
            case RR:
                scheduleRR();
                break;
            case PRIORITY:
                schedulePriority();
                break;
            case MLFQ:
                scheduleMLFQ();
                break;
        }
        
        return scheduleResult;
    }
    
    /**
     * 算法 1：先来先服务（FCFS）
     */
    private void scheduleFCFS() {
        // 按到达顺序执行（假设都同时到达）
        for (Process p : processList) {
            scheduleResult.add(p);
        }
    }
    
    /**
     * 算法 2：短作业优先（SJF）
     */
    private void scheduleSJF() {
        // 复制一份并排序
        List<Process> sorted = new ArrayList<>(processList);
        Collections.sort(sorted);
        
        for (Process p : sorted) {
            scheduleResult.add(p);
        }
    }
    
    /**
     * 算法 3：时间片轮转（RR）- 取消循环次数限制，直到所有进程完成
     */
    private void scheduleRR() {
        Queue<Process> queue = new LinkedList<>(processList);
        int iterations = 0;
        int maxPossibleIterations = processList.stream().mapToInt(Process::getBurstTime).sum(); // 理论最大执行次数
        
        while (!queue.isEmpty()) {
            Process current = queue.poll();
            iterations++;
            
            // 安全检查：防止死循环（理论上不应该超过总时间）
            if (iterations > maxPossibleIterations * 2) {
                System.out.println("[警告] RR 算法检测到异常循环，强制结束");
                break;
            }
            
            if (current.getRemainingTime() > timeSlice) {
                // 需要多个时间片
                for (int i = 0; i < timeSlice; i++) {
                    scheduleResult.add(current);
                }
                current.setRemainingTime(current.getRemainingTime() - timeSlice);
                queue.offer(current); // 排到队尾
            } else {
                // 最后一个时间片
                for (int i = 0; i < current.getRemainingTime(); i++) {
                    scheduleResult.add(current);
                }
                current.setRemainingTime(0);
                // 不再加入队列，该进程已完成
            }
        }
        
        // 统计未完成进程数（此时所有进程都已处理完）
        rrUnfinishedCount = 0;
        
        System.out.println("[提示] RR 算法执行完成，总调度步数：" + scheduleResult.size());
    }
    
    /**
     * 算法 4：优先级调度
     */
    private void schedulePriority() {
        // 按优先级排序（数字越小优先级越高）
        List<Process> sorted = new ArrayList<>(processList);
        Collections.sort(sorted, Comparator.comparingInt(Process::getPriority));
        
        for (Process p : sorted) {
            scheduleResult.add(p);
        }
    }
    
/**
     * 算法 5：多级反馈队列调度（MLFQ）
     * 规则：
     * 1. 每执行一个进程，队列累计+1ms
     * 2. 队列累计到3ms时，切换到下一级队列
     * 3. 进程执行一次没完成，立即降级到下一级队列
     */
    @SuppressWarnings("unchecked")
    private void scheduleMLFQ() {
        for (int i = 0; i < MLFQ_LEVELS; i++) {
            mlfqQueues[i].clear();
        }

        List<Process> workingProcesses = new ArrayList<>();
        for (Process p : processList) {
            Process copy = new Process(p.getName(), p.getBurstTime(), p.getPriority(), p.getColor());
            workingProcesses.add(copy);
        }

        for (Process p : workingProcesses) {
            mlfqQueues[0].offer(p);
        }

        boolean hasUnfinished = true;
        int safetyCounter = 0;
        int maxIterations = workingProcesses.stream().mapToInt(Process::getBurstTime).sum() * 3;

        int currentLevel = 0;
        int[] queueExecuted = {0, 0, 0};

        while (hasUnfinished && safetyCounter < maxIterations) {
            hasUnfinished = false;
            safetyCounter++;

            // 找非空队列
            if (mlfqQueues[currentLevel].isEmpty()) {
                boolean found = false;
                for (int level = 0; level < MLFQ_LEVELS; level++) {
                    if (!mlfqQueues[level].isEmpty()) {
                        currentLevel = level;
                        found = true;
                        break;
                    }
                }
                if (!found) break;
            }

            Process p = mlfqQueues[currentLevel].poll();
            queueExecuted[currentLevel]++;

            scheduleResult.add(p);
            p.setRemainingTime(p.getRemainingTime() - 1);
            p.setTotalExecutedTime(p.getTotalExecutedTime() + 1);

            System.out.println("[MLFQ] t=" + (scheduleResult.size()-1) + ": 执行 " + p.getName() +
                             " (Q" + currentLevel + ", 队列累计:" + queueExecuted[currentLevel] + "/3ms)");

            if (p.getRemainingTime() == 0) {
                System.out.println("[MLFQ] ✓ 进程 " + p.getName() + " 完成");
                hasUnfinished = true;
            } else {
                // 立即降级
                if (currentLevel < MLFQ_LEVELS - 1) {
                    mlfqQueues[currentLevel + 1].offer(p);
                    System.out.println("[MLFQ] ↓ 进程 " + p.getName() + " 降级到 Q" + (currentLevel + 1));
                } else {
                    mlfqQueues[currentLevel].offer(p);
                }
                hasUnfinished = true;
            }

            // 检查队列累计是否满
            if (queueExecuted[currentLevel] >= 3) {
                if (currentLevel < MLFQ_LEVELS - 1) {
                    System.out.println("[MLFQ] ⏩ Q" + currentLevel + " 累计满，切到 Q" + (currentLevel + 1));
                    currentLevel++;
                } else {
                    System.out.println("[MLFQ] 🔄 Q2 累计满，切回 Q0");
                    currentLevel = 0;
                }
            }
        }

        if (safetyCounter >= maxIterations) {
            System.err.println("[警告] 异常循环");
        }

        for (Process copy : workingProcesses) {
            for (Process original : processList) {
                if (original.getName().equals(copy.getName())) {
                    original.setCurrentQueueLevel(copy.getCurrentQueueLevel());
                    original.setTotalExecutedTime(copy.getTotalExecutedTime());
                    original.setRemainingTime(copy.getRemainingTime());
                    break;
                }
            }
        }

        System.out.println("[提示] MLFQ 完成，总步数：" + scheduleResult.size());
    }

    /**
     * 计算性能指标
     */
    public PerformanceMetrics calculateMetrics() {
        int totalWaitTime = 0;
        int totalTurnaroundTime = 0;
        int currentTime = 0;
        
        // 保存原始的 remainingTime，避免影响 UI 显示
        int[] originalRemainingTimes = new int[processList.size()];
        for (int i = 0; i < processList.size(); i++) {
            originalRemainingTimes[i] = processList.get(i).getRemainingTime();
        }
        
        // 重置剩余时间用于计算
        for (Process p : processList) {
            p.setRemainingTime(p.getBurstTime());
        }
        
        // 模拟执行过程计算等待时间
        for (Process scheduled : scheduleResult) {
            // 找到对应的原进程
            for (Process p : processList) {
                if (p.getName().equals(scheduled.getName())) {
                    if (p.getRemainingTime() == p.getBurstTime()) {
                        // 第一次执行，计算等待时间
                        p.setWaitingTime(currentTime - p.getArrivalTime());
                    }
                    
                    p.setRemainingTime(p.getRemainingTime() - 1);
                    currentTime++;
                    
                    if (p.getRemainingTime() == 0) {
                        // 完成，计算周转时间
                        p.setTurnaroundTime(currentTime - p.getArrivalTime());
                    }
                    break;
                }
            }
        }
        
        // 恢复原始的 remainingTime
        for (int i = 0; i < processList.size(); i++) {
            processList.get(i).setRemainingTime(originalRemainingTimes[i]);
        }
        
        // 计算平均值
        for (Process p : processList) {
            totalWaitTime += p.getWaitingTime();
            totalTurnaroundTime += p.getTurnaroundTime();
        }
        
        int count = processList.size();
        double avgWaitTime = count > 0 ? (double) totalWaitTime / count : 0;
        double avgTurnaroundTime = count > 0 ? (double) totalTurnaroundTime / count : 0;
        
        // CPU 利用率（假设没有空闲时间）
        double cpuUsage = count > 0 ? 100.0 : 0;
        
        return new PerformanceMetrics(avgWaitTime, avgTurnaroundTime, cpuUsage);
    }
    
    /**
     * 获取算法
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }
    
    /**
     * 设置算法
     */
    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }
    
    /**
     * 获取时间片
     */
    public int getTimeSlice() {
        return timeSlice;
    }
    
    /**
     * 设置时间片
     */
    public void setTimeSlice(int timeSlice) {
        this.timeSlice = timeSlice;
    }
    
    /**
     * 获取进程列表
     */
    public List<Process> getProcessList() {
        return processList;
    }
    
    /**
     * 获取调度结果
     */
    public List<Process> getScheduleResult() {
        return scheduleResult;
    }
    
    /**
     * 获取 RR 最大循环轮数
     */
    public int getRrMaxRounds() {
        return rrMaxRounds;
    }
    
    /**
     * 获取 RR 未完成进程数
     */
    public int getRrUnfinishedCount() {
        return rrUnfinishedCount;
    }
    
    /**
     * 获取 MLFQ 队列数量
     */
    public int getMlfqLevels() {
        return MLFQ_LEVELS;
    }
    
    /**
     * 获取 MLFQ 指定层级的时间片
     */
    public int getMlfqTimeSlice(int level) {
        if (level >= 0 && level < MLFQ_LEVELS) {
            return mlfqTimeSlices[level];
        }
        return 0;
    }
    
    /**
     * 获取 MLFQ 指定层级的队列
     */
    public Queue<Process> getMlfqQueue(int level) {
        if (level >= 0 && level < MLFQ_LEVELS) {
            return mlfqQueues[level];
        }
        return null;
    }
    
    /**
     * 性能指标类
     */
    public static class PerformanceMetrics {
        private double avgWaitTime;           // 平均等待时间
        private double avgTurnaroundTime;     // 平均周转时间
        private double cpuUsage;              // CPU 利用率
        
        public PerformanceMetrics(double avgWaitTime, double avgTurnaroundTime, double cpuUsage) {
            this.avgWaitTime = avgWaitTime;
            this.avgTurnaroundTime = avgTurnaroundTime;
            this.cpuUsage = cpuUsage;
        }
        
        public double getAvgWaitTime() {
            return avgWaitTime;
        }
        
        public double getAvgTurnaroundTime() {
            return avgTurnaroundTime;
        }
        
        public double getCpuUsage() {
            return cpuUsage;
        }
    }
}
