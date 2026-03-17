package com.homework.osexperiment.osexp.model;

import javafx.scene.paint.Color;

/**
 * 共享资源进程 - 速度受资源竞争影响
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class SharedResourceProcess extends ProcessBall {
    
    // 资源管理器引用（用于获取当前活跃的网络进程数量）
    private ResourceManager resourceManager;
    
    /**
     * 构造函数
     */
    public SharedResourceProcess(String name, Color color, double startX, double startY, double baseSpeed) {
        super(name, color, startX, startY, baseSpeed);
    }
    
    /**
     * 设置资源管理器
     */
    public void setResourceManager(ResourceManager manager) {
        this.resourceManager = manager;
    }
    
    @Override
    public void update() {
        if (!isRunning() || isPaused()) return;
        
        // 处理随机停顿
        handleRandomPause();
        
        // 如果正在停顿中，不移动
        if (randomPauseDuration > 0) return;
        
        // 根据资源占用情况计算实际速度
        updateSpeed();
        
        // 更新位置：从左向右移动
        circle.setCenterX(circle.getCenterX() + speed);
        
        // 如果到达右边界，重置到左边
        if (hasReachedEnd(startX + 650)) {
            resetPosition();
            System.out.println("[调试] " + name + " 完成一轮运行，重新开始");
        }
    }
    
    /**
     * 根据资源占用情况更新速度
     */
    private void updateSpeed() {
        if (resourceManager == null) {
            this.speed = baseSpeed;
            return;
        }
        
        // 获取当前活跃的网络进程数量
        int activeCount = resourceManager.getActiveNetworkProcesses(this);
        
        // 速度 = 基础速度 / 活跃进程数
        // 1 个进程：100% 速度
        // 2 个进程：50% 速度
        // 3 个进程：33% 速度
        this.speed = baseSpeed / activeCount;
    }
    
    @Override
    public double getCurrentSpeed() {
        if (resourceManager == null) {
            return baseSpeed;
        }
        
        int activeCount = resourceManager.getActiveNetworkProcesses(this);
        return baseSpeed / activeCount;
    }
}
