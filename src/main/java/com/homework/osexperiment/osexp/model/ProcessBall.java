package com.homework.osexperiment.osexp.model;

import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

/**
 * 进程小球基类
 * 
 * @author 谢云轩
 * @version 1.0
 */
public abstract class ProcessBall {
    // 小球视觉组件
    protected Circle circle;
    
    // 基本信息
    protected String name;           // 进程名称
    protected Color color;           // 颜色
    protected double startX, startY; // 起始位置
    
    // 运动参数
    protected double speed;          // 当前速度（像素/帧）
    protected double baseSpeed;      // 基础速度
    protected boolean isRunning;     // 是否正在运行
    protected boolean isPaused;      // 是否暂停
    
    // 随机性控制
    protected int pauseCounter = 0;         // 停顿计数器
    protected int randomPauseDuration = 0;  // 随机停顿持续时间
    protected double pauseProbability = 0.02; // 停顿概率（2%）
    
    /**
     * 构造函数
     */
    public ProcessBall(String name, Color color, double startX, double startY, double baseSpeed) {
        this.name = name;
        this.color = color;
        this.startX = startX;
        this.startY = startY;
        this.baseSpeed = baseSpeed;
        this.speed = baseSpeed;
        this.isRunning = false;
        this.isPaused = false;
        
        // 创建小球
        circle = new Circle(20, color);
        resetPosition();
    }
    
    /**
     * 重置位置到起点
     */
    public void resetPosition() {
        circle.setCenterX(startX);
        circle.setCenterY(startY);
    }
    
    /**
     * 开始运行
     */
    public void start() {
        isRunning = true;
        isPaused = false;
    }
    
    /**
     * 暂停运行
     */
    public void pause() {
        isPaused = true;
    }
    
    /**
     * 恢复运行
     */
    public void resume() {
        isPaused = false;
    }
    
    /**
     * 停止运行
     */
    public void stop() {
        isRunning = false;
        isPaused = false;
        resetPosition();
    }
    
    /**
     * 更新位置（由子类实现具体逻辑）
     */
    public abstract void update();
    
    /**
     * 获取速度（考虑资源竞争等因素）
     */
    public abstract double getCurrentSpeed();
    
    /**
     * 处理随机停顿
     */
    protected void handleRandomPause() {
        if (!isRunning || isPaused) return;
        
        // 检查是否需要进入停顿状态
        if (randomPauseDuration == 0 && Math.random() < pauseProbability) {
            // 随机停顿 30-80 帧（约 0.5-1.3 秒）
            randomPauseDuration = 30 + (int)(Math.random() * 50);
            System.out.println("[调试] " + name + " 进入随机停顿，持续 " + randomPauseDuration + " 帧");
        }
        
        // 如果正在停顿中，减少计数器
        if (randomPauseDuration > 0) {
            randomPauseDuration--;
            if (randomPauseDuration == 0) {
                System.out.println("[调试] " + name + " 结束停顿，继续运行");
            }
        }
    }
    
    /**
     * 检查是否到达终点
     */
    public boolean hasReachedEnd(double endX) {
        return circle.getCenterX() > endX;
    }
    
    /**
     * 获取小球组件
     */
    public Circle getCircle() {
        return circle;
    }
    
    /**
     * 获取名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置速度
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    
    /**
     * 检查是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 检查是否暂停
     */
    public boolean isPaused() {
        return isPaused;
    }
}
