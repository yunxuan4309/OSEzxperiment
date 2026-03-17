package com.homework.osexperiment.osexp.model;

import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

/**
 * 生产者类 - 红色小球，负责生产产品
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class Producer {
    // 视觉组件
    private Circle circle;
    
    // 基本信息
    private String name;
    private double startX, startY;
    
    // 运动参数
    private double currentSpeed;        // 当前速度
    private double baseSpeed;           // 基础速度
    private boolean isRunning;          // 是否运行中
    private boolean isPaused;           // 是否暂停
    private boolean isWaiting;          // 是否等待（缓冲区满）
    private boolean hasLock;            // 是否持有锁
    
    // 随机性控制
    private int randomPauseDuration = 0;
    private double pauseProbability = 0.02;
    
    // 生产统计
    private int productsMade = 0;       // 已生产产品数量
    
    /**
     * 构造函数
     */
    public Producer(String name, double startX, double startY, double baseSpeed) {
        this.name = name;
        this.startX = startX;
        this.startY = startY;
        this.baseSpeed = baseSpeed;
        this.currentSpeed = baseSpeed;
        this.isRunning = false;
        this.isPaused = false;
        this.isWaiting = false;
        this.hasLock = false;
        
        // 创建红色小球
        circle = new Circle(20, Color.RED);
        resetPosition();
    }
    
    /**
     * 重置位置
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
     * 暂停
     */
    public void pause() {
        isPaused = true;
    }
    
    /**
     * 恢复
     */
    public void resume() {
        isPaused = false;
    }
    
    /**
     * 停止
     */
    public void stop() {
        isRunning = false;
        isPaused = false;
        isWaiting = false;
        hasLock = false;
        resetPosition();
    }
    
    /**
     * 更新位置和状态
     * @param buffer 缓冲区对象
     */
    public void update(Buffer buffer) {
        if (!isRunning || isPaused) return;
        
        // 处理随机停顿
        handleRandomPause();
        
        // 如果正在停顿中，不移动
        if (randomPauseDuration > 0) return;
        
        // 检查是否需要等待
        if (buffer.isFull()) {
            isWaiting = true;
            System.out.println("[调试] " + name + "：缓冲区已满，等待消费...");
            return;
        } else {
            isWaiting = false;
        }
        
        // 尝试获取锁
        if (!hasLock) {
            hasLock = buffer.tryLock(name);
            if (!hasLock) {
                System.out.println("[调试] " + name + "：未能获取锁，等待...");
                return;
            }
        }
        
        // 生成随机速度（80% ~ 120% 基础速度）
        currentSpeed = baseSpeed * (0.8 + Math.random() * 0.4);
        
        // 向右移动到缓冲区
        double bufferX = 340; // 缓冲区左侧 X 坐标
        if (circle.getCenterX() < bufferX - 20) {
            circle.setCenterX(circle.getCenterX() + currentSpeed);
        } else {
            // 到达缓冲区，生产产品
            Product product = new Product(++productsMade, "产品 #" + productsMade);
            if (buffer.produce(product)) {
                System.out.println("[调试] " + name + " 成功生产了产品 #" + productsMade);
            }
            
            // 释放锁并返回起点
            buffer.unlock();
            hasLock = false;
            resetPosition();
        }
    }
    
    /**
     * 处理随机停顿
     */
    private void handleRandomPause() {
        if (randomPauseDuration == 0 && Math.random() < pauseProbability) {
            randomPauseDuration = 20 + (int)(Math.random() * 40);
            System.out.println("[调试] " + name + " 进入随机停顿，持续 " + randomPauseDuration + " 帧");
        }
        
        if (randomPauseDuration > 0) {
            randomPauseDuration--;
            if (randomPauseDuration == 0) {
                System.out.println("[调试] " + name + " 结束停顿");
            }
        }
    }
    
    /**
     * 获取当前速度描述
     */
    public String getSpeedDescription() {
        return String.format("%.1f", currentSpeed);
    }
    
    /**
     * 获取状态描述
     */
    public String getStatus() {
        if (isWaiting) return "等待（缓冲区满）";
        if (hasLock) return "生产中...";
        if (isPaused) return "暂停";
        if (!isRunning) return "待机";
        return "生产中";
    }
    
    /**
     * 获取小球组件
     */
    public Circle getCircle() {
        return circle;
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
    
    /**
     * 检查是否等待
     */
    public boolean isWaiting() {
        return isWaiting;
    }
}
