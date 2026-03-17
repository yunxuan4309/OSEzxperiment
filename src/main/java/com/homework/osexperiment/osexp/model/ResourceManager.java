package com.homework.osexperiment.osexp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源管理器 - 管理网络带宽等共享资源
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class ResourceManager {
    
    // 所有共享资源的进程列表
    private List<SharedResourceProcess> sharedProcesses;
    
    /**
     * 构造函数
     */
    public ResourceManager() {
        this.sharedProcesses = new ArrayList<>();
    }
    
    /**
     * 注册一个共享资源进程
     */
    public void registerProcess(SharedResourceProcess process) {
        if (!sharedProcesses.contains(process)) {
            sharedProcesses.add(process);
            process.setResourceManager(this);
        }
    }
    
    /**
     * 注销一个共享资源进程
     */
    public void unregisterProcess(SharedResourceProcess process) {
        sharedProcesses.remove(process);
    }
    
    /**
     * 获取当前活跃的网络进程数量
     * （正在运行且不在暂停状态的进程）
     */
    public int getActiveNetworkProcesses(SharedResourceProcess excludeProcess) {
        int count = 0;
        
        for (SharedResourceProcess process : sharedProcesses) {
            // 只统计正在运行、未暂停、且不是排除对象的进程
            if (process.isRunning() && !process.isPaused() && process != excludeProcess) {
                count++;
            }
        }
        
        // 至少为 1（自己），避免除以 0
        return Math.max(1, count);
    }
    
    /**
     * 获取所有活跃的网络进程总数（包括指定进程）
     */
    public int getTotalActiveNetworkProcesses() {
        int count = 0;
        
        for (SharedResourceProcess process : sharedProcesses) {
            if (process.isRunning() && !process.isPaused()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 获取网络带宽占用百分比
     */
    public double getBandwidthUsagePercentage() {
        int activeCount = getTotalActiveNetworkProcesses();
        return (activeCount / 3.0) * 100; // 总共 3 个网络进程
    }
    
    /**
     * 根据活跃进程数获取速度描述
     */
    public String getSpeedDescription() {
        int activeCount = getTotalActiveNetworkProcesses();
        
        switch (activeCount) {
            case 0:
                return "无进程";
            case 1:
                return "快速 (100%)";
            case 2:
                return "中速 (50%)";
            case 3:
                return "慢速 (33%)";
            default:
                return "未知";
        }
    }
    
    /**
     * 清空所有进程
     */
    public void clear() {
        sharedProcesses.clear();
    }
}
