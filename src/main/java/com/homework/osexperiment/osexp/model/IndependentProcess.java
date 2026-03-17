package com.homework.osexperiment.osexp.model;

import javafx.scene.paint.Color;

/**
 * 独立进程 - 不受资源竞争影响
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class IndependentProcess extends ProcessBall {
    
    /**
     * 构造函数
     */
    public IndependentProcess(String name, Color color, double startX, double startY, double baseSpeed) {
        super(name, color, startX, startY, baseSpeed);
    }
    
    @Override
    public void update() {
        if (!isRunning() || isPaused()) return;
        
        // 处理随机停顿
        handleRandomPause();
        
        // 如果正在停顿中，不移动
        if (randomPauseDuration > 0) return;
        
        // 独立进程保持基础速度
        this.speed = baseSpeed;
        
        // 更新位置：从左向右移动
        circle.setCenterX(circle.getCenterX() + speed);
        
        // 如果到达右边界，重置到左边
        if (hasReachedEnd(startX + 650)) {
            resetPosition();
            System.out.println("[调试] " + name + " 完成一轮运行，重新开始");
        }
    }
    
    @Override
    public double getCurrentSpeed() {
        return baseSpeed;
    }
}
