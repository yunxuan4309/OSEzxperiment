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
        PRIORITY  // 优先级调度
    }
    
    private Algorithm algorithm;      // 当前算法
    private int timeSlice;            // 时间片大小（RR 用）
    private List<Process> processList; // 进程列表
    private List<Process> scheduleResult; // 调度结果序列
    
    // RR 算法统计信息
    private int rrMaxRounds = 2;        // RR 最大循环轮数
    private int rrUnfinishedCount = 0;  // RR 未完成进程数
    
    /**
     * 构造函数
     */
    public Scheduler(Algorithm algorithm, int timeSlice) {
        this.algorithm = algorithm;
        this.timeSlice = timeSlice;
        this.processList = new ArrayList<>();
        this.scheduleResult = new ArrayList<>();
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
    }
    
    /**
     * 执行调度，返回调度结果序列
     */
    public List<Process> execute() {
        scheduleResult.clear();
        
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
