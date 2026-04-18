package com.homework.osexperiment.osexp.controller;

import com.homework.osexperiment.osexp.model.Process;
import com.homework.osexperiment.osexp.model.Scheduler;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.homework.osexperiment.osexp.util.SceneSwitcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 实验四：多级反馈队列调度算法 - 控制器
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class MLFQExperimentController {
    
    @FXML private VBox canvasContainer;
    @FXML private Pane animationArea;
    @FXML private VBox cpuArea;
    @FXML private VBox ganttArea;
    
    // 进程添加
    @FXML private TextField txtProcessName;
    @FXML private Spinner<Integer> spinnerBurstTime;
    @FXML private Button btnAddProcess;
    @FXML private Button btnClearQueue;
    
    // 三级队列容器
    @FXML private HBox queue0Container;  // Q0 - 最高优先级
    @FXML private HBox queue1Container;  // Q1 - 中优先级
    @FXML private HBox queue2Container;  // Q2 - 最低优先级
    
    // 等待区容器（新添加的进程先放在这里）
    @FXML private HBox waitingAreaContainer;
    
    // 队列累计时间标签
    @FXML private Label lblQueue0Time;
    @FXML private Label lblQueue1Time;
    @FXML private Label lblQueue2Time;
    
    // 控制按钮
    @FXML private Button btnStart;
    @FXML private Button btnPause;
    @FXML private Button btnReset;
    
    // 性能统计
    @FXML private Label lblAvgWaitTime;
    @FXML private Label lblAvgTurnaroundTime;
    @FXML private Label lblCpuUsage;
    @FXML private Label lblDemotionCount;  // 降级次数
    
    // 模型对象
    private Scheduler scheduler;
    private AnimationTimer animationTimer;
    private boolean isRunning = false;
    private int currentStep = 0;
    private List<Process> scheduleResult;
    
    // 甘特图相关
    private int ganttCurrentTime = 0;
    private VBox executionOrderContainer;
    
    // 进程颜色（4个进程固定颜色）
    private final Color[] PROCESS_COLORS = {
        Color.RED,       // 进程 A - 红色
        Color.BLUE,      // 进程 B - 蓝色
        Color.LIMEGREEN, // 进程 C - 绿色
        Color.GOLD       // 进程 D - 黄色
    };
    private int colorIndex = 0;
    
    // 降级计数器
    private int totalDemotions = 0;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("[调试] MLFQ实验界面已加载");
        
        // 初始化调度器（使用MLFQ算法）
        scheduler = new Scheduler(Scheduler.Algorithm.MLFQ, 1);
        
        // 初始化 Spinner
        spinnerBurstTime.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        
        // 设置按钮初始状态
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        btnReset.setDisable(false);
        
        // 初始化动画
        initAnimationTimer();
        
        // 初始化执行顺序显示容器
        executionOrderContainer = new VBox(10);
        executionOrderContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        executionOrderContainer.setStyle("-fx-padding: 10;");
        ganttArea.getChildren().add(executionOrderContainer);
        
        // 更新显示
        updateAllQueuesDisplay();
        
        System.out.println("[调试] MLFQ配置: 3级队列, 时间片=[3ms, 3ms, 3ms]");
    }
    
    /**
     * 初始化动画计时器
     */
    private void initAnimationTimer() {
        animationTimer = new AnimationTimer() {
            private long lastUpdateTime = 0;
            private static final long UPDATE_INTERVAL = 1_000_000_000L; // 1秒更新一次（10亿纳秒）

            @Override
            public void handle(long now) {
                if (isRunning && scheduleResult != null && !scheduleResult.isEmpty()) {
                    if (now - lastUpdateTime > UPDATE_INTERVAL) {
                        animateStep();
                        lastUpdateTime = now;
                    }
                }
            }
        };
    }
    
/**
     * 动画单步执行
     */
    private void animateStep() {
        if (currentStep >= scheduleResult.size()) {
            // 调度完成
            isRunning = false;
            btnPause.setDisable(true);
            btnStart.setDisable(false);
            calculateAndDisplayMetrics();
            return;
        }

        Process currentProcess = scheduleResult.get(currentStep);
        int currentLevel = currentProcess.getCurrentQueueLevel();

        // 检查是否是进程降级的时刻
        boolean isDemotion = false;
        int nextLevel = currentLevel;
        if (currentStep > 0) {
            Process prevProcess = scheduleResult.get(currentStep - 1);
            if (prevProcess.getName().equals(currentProcess.getName())) {
                // 同一个进程，检查是否要降级
                int timeSlice = scheduler.getMlfqTimeSlice(currentLevel);
                int executedInCurrentQueue = 1;
                if (executedInCurrentQueue >= timeSlice && currentLevel < 2) {
                    isDemotion = true;
                    nextLevel = currentLevel + 1;
                }
            }
        }

        // 如果是降级时刻，显示弹窗
        if (isDemotion && currentProcess.getRemainingTime() > 0) {
            showDemotionAlert(currentProcess.getName(), currentLevel, nextLevel);
        }

        // 更新 CPU 显示
        updateCpuDisplayWithAnimation(currentProcess, currentStep);

        // 更新甘特图
        updateGanttChart(currentProcess, currentStep);

        // 更新所有队列显示
        updateAllQueuesDisplayWithAnimation(currentStep);

        // 更新队列累计时间显示
        updateQueueTimeLabels(currentStep);

        currentStep++;
    }

    /**
     * 显示降级提示弹窗
     */
    private void showDemotionAlert(String processName, int fromLevel, int toLevel) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("队列调度");
        alert.setHeaderText("进程 " + processName + " 降级");
        alert.setContentText("进程 " + processName + " 已用完 Q" + fromLevel +
                          " 的时间片，即将降级到 Q" + toLevel);
        alert.showAndWait();
    }
    
    /**
     * 更新队列累计时间标签
     * 显示每个队列的累计执行时间（队列累计3ms切换）
     */
    private void updateQueueTimeLabels(int currentStep) {
        if (currentStep == 0 || scheduleResult == null || scheduleResult.isEmpty()) {
            if (lblQueue0Time != null) lblQueue0Time.setText("(0/3ms)");
            if (lblQueue1Time != null) lblQueue1Time.setText("(0/3ms)");
            if (lblQueue2Time != null) lblQueue2Time.setText("(0/3ms)");
            return;
        }

        int currentLevel = 0;
        int[] queueExecuted = {0, 0, 0};

        for (int step = 0; step < currentStep; step++) {
            queueExecuted[currentLevel]++;

            if (queueExecuted[currentLevel] >= 3) {
                if (currentLevel < 2) {
                    currentLevel++;
                } else {
                    currentLevel = 0;
                }
            }
        }

        if (lblQueue0Time != null) {
            lblQueue0Time.setText(String.format("(%d/3ms)", queueExecuted[0]));
        }
        if (lblQueue1Time != null) {
            lblQueue1Time.setText(String.format("(%d/3ms)", queueExecuted[1]));
        }
        if (lblQueue2Time != null) {
            lblQueue2Time.setText(String.format("(%d/3ms)", queueExecuted[2]));
        }
    }
    
    /**
     * 计算进程在指定队列连续执行的次数
     */
    private int countConsecutiveExecutions(String processName, int currentStep, int queueLevel) {
        int count = 0;
        for (int i = currentStep - 1; i >= 0; i--) {
            Process p = scheduleResult.get(i);
            if (p.getName().equals(processName)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    
    /**
     * 更新 CPU 区域显示
     */
    private void updateCpuDisplayWithAnimation(Process process, int step) {
        cpuArea.getChildren().clear();
        
        Label cpuTitle = new Label("🖥️ CPU 执行区");
        cpuTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        
        Circle processCircle = new Circle(40, process.getColor());
        
        Label lblName = new Label(process.getName());
        lblName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        int remainingTime = calculateRemainingTime(process, step);
        Label lblTime = new Label("剩余: " + remainingTime + "ms");
        lblTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffcc00;");
        
        // 计算在当前队列已执行的时间
        int executedInCurrentQueue = calculateExecutedInCurrentQueue(process, step);
        int timeSlice = scheduler.getMlfqTimeSlice(process.getCurrentQueueLevel());
        Label lblQueueInfo = new Label(String.format("Q%d (时间片: %dms, 已用: %dms)", 
            process.getCurrentQueueLevel(), timeSlice, executedInCurrentQueue));
        lblQueueInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");
        
        cpuArea.getChildren().addAll(cpuTitle, processCircle, lblName, lblTime, lblQueueInfo);
    }
    
    /**
     * 计算剩余时间
     */
    private int calculateRemainingTime(Process process, int currentStep) {
        if (scheduleResult == null || scheduleResult.isEmpty()) {
            return process.getBurstTime();
        }
        
        int count = 0;
        for (int i = currentStep; i < scheduleResult.size(); i++) {
            if (scheduleResult.get(i).getName().equals(process.getName())) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 计算进程在当前队列已执行的时间
     */
    private int calculateExecutedInCurrentQueue(Process process, int currentStep) {
        if (scheduleResult == null || scheduleResult.isEmpty()) {
            return 0;
        }
        
        // 从当前步骤往前查找，统计连续执行的次数
        int count = 0;
        for (int i = currentStep - 1; i >= 0; i--) {
            Process p = scheduleResult.get(i);
            if (p.getName().equals(process.getName())) {
                count++;
            } else {
                break; // 遇到其他进程，停止计数
            }
        }
        
        return count;
    }

    /**
     * 更新甘特图（MLFQ不需要执行顺序显示）
     */
    private void updateGanttChart(Process process, int timeUnit) {
        // MLFQ 实验不显示执行顺序，留空或显示简单信息
        if (timeUnit == 0) {
            initGanttDisplay();
        }
        
        // 更新当前执行信息
        updateCurrentExecutionInfo(process, timeUnit);
        ganttCurrentTime++;
    }
    
    /**
     * 初始化甘特图显示
     */
    private void initGanttDisplay() {
        executionOrderContainer.getChildren().clear();
        
        Label titleLabel = new Label("📊 调度状态：");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        executionOrderContainer.getChildren().add(titleLabel);
        
        Label infoLabel = new Label("💡 观察进程在三级队列间的移动和降级过程");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        executionOrderContainer.getChildren().add(infoLabel);
    }
    
    /**
     * 更新当前执行信息
     */
    private void updateCurrentExecutionInfo(Process process, int timeUnit) {
        // 移除旧的状态信息（保留标题和说明）
        if (executionOrderContainer.getChildren().size() > 2) {
            executionOrderContainer.getChildren().remove(2);
        }
        
        Label statusLabel = new Label(String.format("⏱️ 当前时间: %dms | 正在执行: %s (Q%d)", 
            timeUnit, process.getName(), process.getCurrentQueueLevel()));
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        
        executionOrderContainer.getChildren().add(statusLabel);
    }
    
    /**
     * 更新所有队列显示（静态版本）
     */
    private void updateAllQueuesDisplay() {
        // 如果还未开始调度，显示在等待区
        if (scheduleResult == null || scheduleResult.isEmpty()) {
            updateWaitingAreaDisplay();
            queue0Container.getChildren().clear();
            queue1Container.getChildren().clear();
            queue2Container.getChildren().clear();
        } else {
            // 调度进行中，显示在各队列中
            waitingAreaContainer.getChildren().clear();
            updateQueueDisplay(queue0Container, 0);
            updateQueueDisplay(queue1Container, 1);
            updateQueueDisplay(queue2Container, 2);
        }
    }
    
    /**
     * 更新等待区显示
     */
    private void updateWaitingAreaDisplay() {
        waitingAreaContainer.getChildren().clear();
        
        for (Process p : scheduler.getProcessList()) {
            VBox processBox = createProcessBoxForWaiting(p);
            waitingAreaContainer.getChildren().add(processBox);
        }
    }
    
    /**
     * 创建等待区进程显示框
     */
    private VBox createProcessBoxForWaiting(Process process) {
        VBox box = new VBox(5);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-border-radius: 8; -fx-padding: 10; -fx-border-color: #667eea; -fx-border-width: 2;");
        
        Circle circle = new Circle(25, process.getColor());
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);
        
        Label lblName = new Label(process.getName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label lblInfo = new Label("运行时间: " + process.getBurstTime() + "ms");
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        box.getChildren().addAll(circle, lblName, lblInfo);
        return box;
    }
    
    /**
     * 更新所有队列显示（动画版本）
     */
    private void updateAllQueuesDisplayWithAnimation(int currentStep) {
        // 动画期间不显示等待区
        waitingAreaContainer.getChildren().clear();
        
        // 根据当前步骤，计算每个进程的当前队列层级
        updateQueueDisplayBasedOnStep(currentStep);
    }
    
    /**
     * 根据当前步骤更新队列显示
     */
    private void updateQueueDisplayBasedOnStep(int currentStep) {
        // 清空所有队列容器
        queue0Container.getChildren().clear();
        queue1Container.getChildren().clear();
        queue2Container.getChildren().clear();
        
        // 对于每个进程，模拟执行到当前步骤，计算它应该在哪一级队列
        for (Process p : scheduler.getProcessList()) {
            // 如果进程已经完成，不显示在任何队列
            if (calculateRemainingTime(p, currentStep) == 0) {
                continue;
            }
            
            // 模拟执行过程，计算该进程在当前步骤的队列层级
            int queueLevel = simulateQueueLevelForProcess(p, currentStep);
            
            // 创建进程框并添加到对应队列
            VBox processBox = createProcessBoxForQueue(p, queueLevel);
            
            switch (queueLevel) {
                case 0:
                    queue0Container.getChildren().add(processBox);
                    break;
                case 1:
                    queue1Container.getChildren().add(processBox);
                    break;
                case 2:
                    queue2Container.getChildren().add(processBox);
                    break;
            }
        }
    }
    
/**
     * 模拟执行过程，计算指定进程在给定步骤时的队列层级
     * 队列累计3ms切换，进程执行后立即降级
     */
    private int simulateQueueLevelForProcess(Process targetProcess, int currentStep) {
        java.util.Map<String, Integer> processQueue = new java.util.HashMap<>();
        int[] queueExecuted = {0, 0, 0};
        int currentLevel = 0;

        for (int step = 0; step < currentStep && step < scheduleResult.size(); step++) {
            Process p = scheduleResult.get(step);

            // 获取或初始化进程队列
            int prevQueue = processQueue.getOrDefault(p.getName(), 0);
            processQueue.put(p.getName(), prevQueue);

            queueExecuted[currentLevel]++;

            // 进程执行后未完成则降级
            if (prevQueue == currentLevel) {
                // 该进程在当前队列，执行后降级
                if (prevQueue < 2) {
                    processQueue.put(p.getName(), prevQueue + 1);
                }
            }

            // 检查队列累计是否满
            if (queueExecuted[currentLevel] >= 3) {
                if (currentLevel < 2) {
                    currentLevel++;
                } else {
                    currentLevel = 0;
                }
            }
        }

        return processQueue.getOrDefault(targetProcess.getName(), 0);
    }

    /**
     * 创建队列中的进程显示框
     */
    private VBox createProcessBoxForQueue(Process process, int queueLevel) {
        VBox box = new VBox(5);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-radius: 8; -fx-padding: 8;");

        Circle circle = new Circle(20, process.getColor());

        Label lblName = new Label(process.getName());
        lblName.setStyle("-fx-font-weight: bold;");
        
        int remainingTime = calculateRemainingTime(process, getCurrentStep());
        Label lblInfo = new Label("剩余:" + remainingTime + "ms");
        lblInfo.setStyle("-fx-font-size: 10px;");
        
        box.getChildren().addAll(circle, lblName, lblInfo);
        return box;
    }
    
    /**
     * 获取当前步骤
     */
    private int getCurrentStep() {
        return currentStep;
    }
    
    /**
     * 更新单个队列显示
     */
    private void updateQueueDisplay(HBox container, int queueLevel) {
        container.getChildren().clear();
        
        for (Process p : scheduler.getProcessList()) {
            // 只显示在该队列中的进程
            if (p.getCurrentQueueLevel() == queueLevel && p.getRemainingTime() > 0) {
                VBox processBox = createProcessBox(p, queueLevel);
                container.getChildren().add(processBox);
            }
        }
    }
    
    /**
     * 创建进程显示框
     */
    private VBox createProcessBox(Process process, int queueLevel) {
        VBox box = new VBox(5);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-radius: 8; -fx-padding: 8;");
        
        Circle circle = new Circle(20, process.getColor());
        
        Label lblName = new Label(process.getName());
        lblName.setStyle("-fx-font-weight: bold;");
        
        Label lblInfo = new Label("剩余:" + process.getRemainingTime() + "ms");
        lblInfo.setStyle("-fx-font-size: 10px;");
        
        box.getChildren().addAll(circle, lblName, lblInfo);
        return box;
    }
    
    /**
     * 计算并显示性能指标
     */
    private void calculateAndDisplayMetrics() {
        Scheduler.PerformanceMetrics metrics = scheduler.calculateMetrics();
        
        lblAvgWaitTime.setText(String.format("%.2f ms", metrics.getAvgWaitTime()));
        lblAvgTurnaroundTime.setText(String.format("%.2f ms", metrics.getAvgTurnaroundTime()));
        lblCpuUsage.setText(String.format("%.1f%%", metrics.getCpuUsage()));
        
        // 计算总降级次数
        calculateTotalDemotions();
        lblDemotionCount.setText(String.valueOf(totalDemotions));
    }
    
    /**
     * 计算总降级次数
     */
    private void calculateTotalDemotions() {
        totalDemotions = 0;
        for (Process p : scheduler.getProcessList()) {
            // 最终队列层级就是降级次数（从Q0开始）
            totalDemotions += p.getCurrentQueueLevel();
        }
    }
    
    /**
     * 添加进程按钮处理
     */
    @FXML
    private void handleAddProcess() {
        String name = txtProcessName.getText().trim();
        if (name.isEmpty()) {
            name = String.valueOf((char)('A' + scheduler.getProcessList().size()));
        }
        
        int burstTime = spinnerBurstTime.getValue();
        
        // 检查是否超过4个进程
        if (scheduler.getProcessList().size() >= 4) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("最多只能添加4个进程！");
            alert.showAndWait();
            return;
        }
        
        // 创建进程
        Color color = PROCESS_COLORS[colorIndex % PROCESS_COLORS.length];
        Process process = new Process(name, burstTime, 1, color); // 优先级默认为1
        scheduler.addProcess(process);
        
        // 更新显示（显示在等待区）
        updateAllQueuesDisplay();
        
        // 清空输入
        txtProcessName.clear();
        colorIndex++;
        
        System.out.println("[调试] 添加了进程：" + process);
    }
    
    /**
     * 清空队列按钮处理
     */
    @FXML
    private void handleClearQueue() {
        scheduler.clear();
        colorIndex = 0;
        cpuArea.getChildren().clear();
        ganttArea.getChildren().clear();
        
        // 重新添加执行顺序容器
        executionOrderContainer = new VBox(10);
        executionOrderContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        executionOrderContainer.setStyle("-fx-padding: 10;");
        ganttArea.getChildren().add(executionOrderContainer);
        
        // 重置调度结果，让进程回到等待区
        scheduleResult = null;
        updateAllQueuesDisplay();
        resetMetrics();
        
        System.out.println("[调试] 清空了所有进程");
    }
    
    /**
     * 开始调度按钮处理
     */
    @FXML
    private void handleStart() {
        if (scheduler.getProcessList().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("请先添加进程！");
            alert.showAndWait();
            return;
        }
        
        // 注意：不需要手动重置进程状态，Scheduler.scheduleMLFQ() 会自动重置
        
        // 执行调度
        scheduleResult = scheduler.execute();
        currentStep = 0;
        
        // 打印调度结果
        System.out.println("[调试] MLFQ调度结果长度：" + scheduleResult.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(50, scheduleResult.size()); i++) {
            sb.append(scheduleResult.get(i).getName());
            if (i < scheduleResult.size() - 1) sb.append("->");
        }
        if (scheduleResult.size() > 50) sb.append("...");
        System.out.println("[调试] 调度序列：" + sb.toString());
        
        // 开始动画
        isRunning = true;
        btnStart.setDisable(true);
        btnPause.setDisable(false);
        animationTimer.start();
        
        System.out.println("[调试] 开始MLFQ调度");
    }
    
    /**
     * 暂停按钮处理
     */
    @FXML
    private void handlePause() {
        isRunning = false;
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        animationTimer.stop();
        System.out.println("[调试] 暂停调度");
    }
    
    /**
     * 重置按钮处理
     */
    @FXML
    private void handleReset() {
        isRunning = false;
        currentStep = 0;
        scheduleResult = null;  // 重置为null，让进程回到等待区
        ganttCurrentTime = 0;
        animationTimer.stop();
        
        cpuArea.getChildren().clear();
        ganttArea.getChildren().clear();
        
        if (executionOrderContainer == null) {
            executionOrderContainer = new VBox(10);
            executionOrderContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            executionOrderContainer.setStyle("-fx-padding: 10;");
        } else {
            executionOrderContainer.getChildren().clear();
        }
        ganttArea.getChildren().add(executionOrderContainer);
        
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        resetMetrics();
        
        // 重置所有进程状态
        for (Process p : scheduler.getProcessList()) {
            p.setRemainingTime(p.getBurstTime());
            p.setCurrentQueueLevel(0);
            p.setTotalExecutedTime(0);
        }
        
        updateAllQueuesDisplay();
        
        System.out.println("[调试] 重置调度");
    }
    
    /**
     * 重置性能指标
     */
    private void resetMetrics() {
        lblAvgWaitTime.setText("-- ms");
        lblAvgTurnaroundTime.setText("-- ms");
        lblCpuUsage.setText("--%");
        lblDemotionCount.setText("0");
    }
    
    /**
     * 返回主菜单
     */
    @FXML
    private void handleBackToMain() {
        System.out.println("[调试] 从MLFQ实验返回主菜单");
        animationTimer.stop();
        SceneSwitcher.switchScene(canvasContainer, "/fxml/MainMenu.fxml", "操作系统实验模拟系统");
    }
}
