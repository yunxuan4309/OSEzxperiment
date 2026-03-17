package com.homework.osexperiment.osexp.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import com.homework.osexperiment.osexp.util.SceneSwitcher;
import com.homework.osexperiment.osexp.model.*;

/**
 * 实验一：进程并发模拟 - 控制器
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class ProcessConcurrencyController {
    
    @FXML
    private VBox canvasContainer;
    
    @FXML
    private Pane animationArea;
    
    @FXML
    private Label lblNetworkStatus;
    
    @FXML
    private Label lblSpeedStatus;
    
    @FXML
    private CheckBox chkProcessA;
    
    @FXML
    private CheckBox chkProcessB;
    
    @FXML
    private CheckBox chkProcessC;
    
    @FXML
    private CheckBox chkProcessD;
    
    @FXML
    private CheckBox chkProcessE;
    
    @FXML
    private Button btnStart;
    
    @FXML
    private Button btnPause;
    
    // 进程小球列表
    private IndependentProcess processA;  // 红色 - 打印
    private IndependentProcess processB;  // 蓝色 - 计算
    private SharedResourceProcess processC;  // 绿色 - 网络 1
    private SharedResourceProcess processD;  // 黄色 - 网络 2
    private SharedResourceProcess processE;  // 紫色 - 网络 3
    
    // 资源管理器
    private ResourceManager resourceManager;
    
    // 动画计时器
    private AnimationTimer animationTimer;
    
    // 运行状态
    private boolean isRunning = false;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("[调试] 进程并发实验界面已加载");
        
        // 初始化资源管理器
        resourceManager = new ResourceManager();
        
        // 初始化所有进程小球
        initializeProcesses();
        
        // 注册网络进程到资源管理器
        resourceManager.registerProcess(processC);
        resourceManager.registerProcess(processD);
        resourceManager.registerProcess(processE);
        
        // 将小球添加到动画区域
        animationArea.getChildren().addAll(
            processA.getCircle(),
            processB.getCircle(),
            processC.getCircle(),
            processD.getCircle(),
            processE.getCircle()
        );
        
        // 初始化动画循环
        initAnimationTimer();
        
        // 设置按钮初始状态
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        
        // 更新状态显示
        updateStatusDisplay();
    }
    
    /**
     * 初始化所有进程小球
     */
    private void initializeProcesses() {
        // Pane 尺寸：680 x 260，小球半径 20
        // 为了让所有小球都在框内，Y 坐标范围应该是：20 ~ 240
        // 5 个小球均匀分布，间距 = (260 - 40) / 5 = 44
        
        // 独立进程 - 基础速度 3.0
        processA = new IndependentProcess("进程 A (打印)", Color.RED, 50, 30, 3.0);
        processB = new IndependentProcess("进程 B (计算)", Color.BLUE, 50, 75, 3.0);
        
        // 共享资源进程 - 基础速度 4.0（独占时最快）
        processC = new SharedResourceProcess("进程 C (网络 1)", Color.LIMEGREEN, 50, 120, 4.0);
        processD = new SharedResourceProcess("进程 D (网络 2)", Color.GOLD, 50, 165, 4.0);
        processE = new SharedResourceProcess("进程 E (网络 3)", Color.BLUEVIOLET, 50, 210, 4.0);
    }
    
    /**
     * 初始化动画计时器
     */
    private void initAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateProcesses();
                updateStatusDisplay();
            }
        };
    }
    
    /**
     * 更新所有进程的位置
     */
    private void updateProcesses() {
        // 根据复选框状态控制进程的启用/禁用
        updateProcessState(processA, chkProcessA.isSelected());
        updateProcessState(processB, chkProcessB.isSelected());
        updateProcessState(processC, chkProcessC.isSelected());
        updateProcessState(processD, chkProcessD.isSelected());
        updateProcessState(processE, chkProcessE.isSelected());
        
        // 更新位置 - 只有在运行且未暂停时才更新
        if (isRunning && !btnPause.getText().equals("▶️ 继续")) {
            processA.update();
            processB.update();
            processC.update();
            processD.update();
            processE.update();
        }
    }
    
    /**
     * 更新单个进程的状态
     */
    private void updateProcessState(ProcessBall process, boolean isEnabled) {
        if (isEnabled) {
            // 如果之前没运行，则启动
            if (!process.isRunning()) {
                process.start();
            } else if (process.isPaused() && !btnPause.isDisabled()) {
                process.resume();
            }
        } else {
            // 禁用进程
            process.stop();
        }
    }
    
    /**
     * 更新状态显示
     */
    private void updateStatusDisplay() {
        int activeCount = resourceManager.getTotalActiveNetworkProcesses();
        lblNetworkStatus.setText("🌐 网络带宽占用：" + activeCount + "/3");
        lblSpeedStatus.setText("⚡ 网络进程速度：" + resourceManager.getSpeedDescription());
    }
    
    /**
     * 开始按钮处理
     */
    @FXML
    private void handleStart() {
        System.out.println("[调试] 点击了开始按钮");
        isRunning = true;
        btnStart.setDisable(true);
        btnPause.setDisable(false);
        animationTimer.start();
        
        // 确保所有选中的进程都在运行
        if (chkProcessA.isSelected()) processA.start();
        if (chkProcessB.isSelected()) processB.start();
        if (chkProcessC.isSelected()) processC.start();
        if (chkProcessD.isSelected()) processD.start();
        if (chkProcessE.isSelected()) processE.start();
    }
    
    /**
     * 暂停按钮处理
     */
    @FXML
    private void handlePause() {
        System.out.println("[调试] 点击了暂停按钮");
        
        if (btnPause.getText().equals("⏸️ 暂停")) {
            // 暂停
            btnPause.setText("▶️ 继续");
            processA.pause();
            processB.pause();
            processC.pause();
            processD.pause();
            processE.pause();
        } else {
            // 继续
            btnPause.setText("⏸️ 暂停");
            if (chkProcessA.isSelected()) processA.resume();
            if (chkProcessB.isSelected()) processB.resume();
            if (chkProcessC.isSelected()) processC.resume();
            if (chkProcessD.isSelected()) processD.resume();
            if (chkProcessE.isSelected()) processE.resume();
        }
    }
    
    /**
     * 重置按钮处理
     */
    @FXML
    private void handleReset() {
        System.out.println("[调试] 点击了重置按钮");
        isRunning = false;
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        btnPause.setText("⏸️ 暂停");
        animationTimer.stop();
        
        // 重置所有进程位置
        processA.stop();
        processB.stop();
        processC.stop();
        processD.stop();
        processE.stop();
        
        // 重新初始化
        initializeProcesses();
        
        // 更新状态
        updateStatusDisplay();
    }
    
    /**
     * 返回主菜单
     */
    @FXML
    private void handleBackToMain() {
        System.out.println("[调试] 从进程并发实验返回主菜单");
        animationTimer.stop();
        SceneSwitcher.switchScene(canvasContainer, "/fxml/MainMenu.fxml", "操作系统实验模拟系统");
    }
}
