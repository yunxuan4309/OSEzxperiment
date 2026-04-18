package com.homework.osexperiment.osexp.model;

import javafx.scene.paint.Color;

/**
 * 进程类 - 表示被调度的进程
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class Process implements Comparable<Process> {
    // 基本信息
    private String name;              // 进程名
    private int burstTime;            // 运行时间（ms）
    private int priority;             // 优先级（1-5，1 最高）
    
    // 颜色标识
    private Color color;              // 进程颜色
    
    // 状态信息
    private int remainingTime;        // 剩余运行时间
    private int waitingTime;          // 等待时间
    private int turnaroundTime;       // 周转时间
    private int arrivalTime;          // 到达时间
    
    // 动画位置
    private double currentPositionX;  // 当前 X 位置
    private double currentPositionY;  // 当前 Y 位置
    
    // MLFQ 专用字段
    private int currentQueueLevel;    // 当前所在队列层级（0为最高优先级）
    private int totalExecutedTime;    // 已执行总时间（用于统计）
    private int executedInCurrentQueue; // 在当前队列已执行的时间（用于判断降级）
    
    /**
     * 构造函数
     */
    public Process(String name, int burstTime, int priority, Color color) {
        this.name = name;
        this.burstTime = burstTime;
        this.priority = priority;
        this.color = color;
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.arrivalTime = 0;
        this.currentPositionX = 0;
        this.currentPositionY = 0;
        this.currentQueueLevel = 0;
        this.totalExecutedTime = 0;
        this.executedInCurrentQueue = 0;
    }
    
    /**
     * 比较方法（用于 SJF 排序）
     */
    @Override
    public int compareTo(Process other) {
        return Integer.compare(this.burstTime, other.burstTime);
    }
    
    /**
     * 获取进程名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取运行时间
     */
    public int getBurstTime() {
        return burstTime;
    }
    
    /**
     * 获取优先级
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 获取颜色
     */
    public Color getColor() {
        return color;
    }
    
    /**
     * 获取剩余时间
     */
    public int getRemainingTime() {
        return remainingTime;
    }
    
    /**
     * 设置剩余时间
     */
    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }
    
    /**
     * 获取等待时间
     */
    public int getWaitingTime() {
        return waitingTime;
    }
    
    /**
     * 设置等待时间
     */
    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }
    
    /**
     * 获取周转时间
     */
    public int getTurnaroundTime() {
        return turnaroundTime;
    }
    
    /**
     * 设置周转时间
     */
    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }
    
    /**
     * 获取到达时间
     */
    public int getArrivalTime() {
        return arrivalTime;
    }
    
    /**
     * 设置到达时间
     */
    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
    
    /**
     * 获取当前位置 X
     */
    public double getCurrentPositionX() {
        return currentPositionX;
    }
    
    /**
     * 设置当前位置 X
     */
    public void setCurrentPositionX(double currentPositionX) {
        this.currentPositionX = currentPositionX;
    }
    
    /**
     * 获取当前位置 Y
     */
    public double getCurrentPositionY() {
        return currentPositionY;
    }
    
    /**
     * 设置当前位置 Y
     */
    public void setCurrentPositionY(double currentPositionY) {
        this.currentPositionY = currentPositionY;
    }
    
    /**
     * 获取当前队列层级（MLFQ用）
     */
    public int getCurrentQueueLevel() {
        return currentQueueLevel;
    }
    
    /**
     * 设置当前队列层级（MLFQ用）
     */
    public void setCurrentQueueLevel(int currentQueueLevel) {
        this.currentQueueLevel = currentQueueLevel;
    }
    
    /**
     * 获取已执行总时间（MLFQ用）
     */
    public int getTotalExecutedTime() {
        return totalExecutedTime;
    }
    
    /**
     * 设置已执行总时间（MLFQ用）
     */
    public void setTotalExecutedTime(int totalExecutedTime) {
        this.totalExecutedTime = totalExecutedTime;
    }
    
    /**
     * 获取在当前队列已执行的时间（MLFQ用）
     */
    public int getExecutedInCurrentQueue() {
        return executedInCurrentQueue;
    }
    
    /**
     * 设置在当前队列已执行的时间（MLFQ用）
     */
    public void setExecutedInCurrentQueue(int executedInCurrentQueue) {
        this.executedInCurrentQueue = executedInCurrentQueue;
    }
    
    /**
     * 增加在当前队列的执行时间（MLFQ用）
     */
    public void incrementExecutedInCurrentQueue() {
        this.executedInCurrentQueue++;
    }
    
    /**
     * 重置在当前队列的执行时间（降级时调用）
     */
    public void resetExecutedInCurrentQueue() {
        this.executedInCurrentQueue = 0;
    }
    
    @Override
    public String toString() {
        return name + "(" + burstTime + "ms,P" + priority + ")";
    }
}
