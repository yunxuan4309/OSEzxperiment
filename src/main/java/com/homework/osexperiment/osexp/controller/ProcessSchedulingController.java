package com.homework.osexperiment.osexp.controller;

import com.homework.osexperiment.osexp.model.Process;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import com.homework.osexperiment.osexp.util.SceneSwitcher;
import com.homework.osexperiment.osexp.model.*;

import java.util.List;
import java.util.ArrayList;
import javafx.collections.FXCollections;

/**
 * 实验三：进程调度模拟 - 控制器
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class ProcessSchedulingController {
    
    @FXML private VBox canvasContainer;
    @FXML private Pane animationArea;
    @FXML private VBox cpuArea;
    @FXML private VBox ganttArea;
    
    // 算法选择
    @FXML private RadioButton radioFCFS;
    @FXML private RadioButton radioSJF;
    @FXML private RadioButton radioRR;
    @FXML private RadioButton radioPriority;
    @FXML private ToggleGroup algorithmGroup;
    @FXML private Spinner<Integer> spinnerTimeSlice;
    @FXML private Label lblTimeSliceSetting;  // 时间片设置标签
    
    // RR 算法信息显示
    @FXML private HBox rrInfoBox;
    @FXML private Label lblMaxRounds;
    @FXML private Label lblUnfinishedProcesses;
    
    // 进程添加
    @FXML private TextField txtProcessName;
    @FXML private Spinner<Integer> spinnerBurstTime;
    @FXML private ComboBox<Integer> comboPriority;
    @FXML private Button btnAddProcess;
    @FXML private Button btnClearQueue;
    
    // 队列显示
    @FXML private HBox readyQueueContainer;
    
    // 保存进程框的引用，用于动态更新
    private List<VBox> processBoxes = new ArrayList<>();
    
    // 控制按钮
    @FXML private Button btnStart;
    @FXML private Button btnPause;
    @FXML private Button btnReset;
    
    // 性能统计
    @FXML private Label lblAvgWaitTime;
    @FXML private Label lblAvgTurnaroundTime;
    @FXML private Label lblCpuUsage;
    
    // 模型对象
    private Scheduler scheduler;
    private AnimationTimer animationTimer;
    private boolean isRunning = false;
    private int currentStep = 0;
    private List<Process> scheduleResult;
    
    // 甘特图相关
    private int ganttCurrentTime = 0;  // 当前时间
    private VBox executionOrderContainer;  // 执行顺序显示容器
    
    // 进程颜色（4 个进程固定颜色）
    private final Color[] PROCESS_COLORS = {
        Color.RED,      // 进程 A - 红色
        Color.BLUE,     // 进程 B - 蓝色
        Color.LIMEGREEN, // 进程 C - 绿色
        Color.GOLD      // 进程 D - 黄色
    };
    private int colorIndex = 0;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("[调试] 进程调度实验界面已加载");
        
        // 初始化调度器
        scheduler = new Scheduler(Scheduler.Algorithm.FCFS, 2);
        
        // 初始化进程框列表
        processBoxes = new ArrayList<>();
        
        // 初始化 Spinner
        spinnerTimeSlice.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));
        
        // 监听 RR 算法选择，显示/隐藏时间片设置
        radioRR.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (spinnerTimeSlice != null && lblTimeSliceSetting != null) {
                spinnerTimeSlice.setVisible(isSelected);
                spinnerTimeSlice.setDisable(!isSelected);
                lblTimeSliceSetting.setVisible(isSelected);
            }
        });
        
        // 初始隐藏时间片设置（默认选中 FCFS）
        spinnerTimeSlice.setVisible(false);
        spinnerTimeSlice.setDisable(true);
        
        spinnerBurstTime.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        
        // 默认选中优先级 3
        comboPriority.getSelectionModel().select(2);
        
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
        
        // 更新显示（初始显示）
        updateStatusDisplayWithAnimation(0);
        
        // 初始隐藏 RR 信息显示
        if (rrInfoBox != null) {
            rrInfoBox.setVisible(false);
            rrInfoBox.setManaged(false);
        }
    }
    
    /**
     * 初始化动画计时器
     */
    private void initAnimationTimer() {
        animationTimer = new AnimationTimer() {
            private long lastUpdateTime = 0;
            private static final long UPDATE_INTERVAL = 500_000_000; // 0.5 秒更新一次（纳秒）
            
            @Override
            public void handle(long now) {
                if (isRunning && scheduleResult != null && !scheduleResult.isEmpty()) {
                    // 控制更新频率
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
        
        // 更新 CPU 显示（传入当前步骤，用于计算剩余时间）
        updateCpuDisplayWithAnimation(currentProcess, currentStep);
        
        // 更新甘特图
        updateGanttChart(currentProcess, currentStep);
        
        // 更新就绪队列显示（动态刷新剩余时间）
        updateStatusDisplayWithAnimation(currentStep);
        
        currentStep++;
    }
    
    /**
     * 更新 CPU 区域显示（动画版本，根据执行进度计算剩余时间）
     */
    private void updateCpuDisplayWithAnimation(Process process, int step) {
        cpuArea.getChildren().clear();
        
        // CPU 区域标题
        Label cpuTitle = new Label("🖥️ CPU 执行区");
        cpuTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        
        // 创建进程小球
        Circle processCircle = new Circle(40, process.getColor());
        
        // 进程名标签
        Label lblName = new Label(process.getName());
        lblName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // 计算当前剩余时间：从当前步骤到结束该进程还需要执行的时间
        int remainingTime = calculateRemainingTime(process, step);
        Label lblTime = new Label(remainingTime + "ms");
        lblTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffcc00;");
        
        // 剩余时间说明标签
        Label lblTimeNote = new Label("(当前剩余运行时间)");
        lblTimeNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-font-style: italic;");
        
        cpuArea.getChildren().addAll(cpuTitle, processCircle, lblName, lblTime, lblTimeNote);
    }
    
    /**
     * 计算指定步骤时进程的剩余时间
     * @param process 目标进程
     * @param currentStep 当前执行的步骤
     * @return 剩余需要执行的时间（毫秒），如果进程已完成则返回 0
     */
    private int calculateRemainingTime(Process process, int currentStep) {
        // 如果调度结果为空，返回进程的总运行时间
        if (scheduleResult == null || scheduleResult.isEmpty()) {
            return process.getBurstTime();
        }
        
        // 统计从当前步骤开始，该进程在调度结果中还需要出现多少次
        int count = 0;
        for (int i = currentStep; i < scheduleResult.size(); i++) {
            if (scheduleResult.get(i).getName().equals(process.getName())) {
                count++;
            }
        }
        
        // 返回实际剩余次数（可以为 0，表示进程已完成）
        return count;
    }
    
    /**
     * 更新甘特图（改为显示执行顺序）
     */
    private void updateGanttChart(Process process, int timeUnit) {
        // 如果是第一步，初始化显示
        if (timeUnit == 0) {
            initExecutionOrderDisplay();
        }
        
        // 高亮当前执行的进程
        highlightCurrentProcess(process);
        
        ganttCurrentTime++;
    }
    
    /**
     * 初始化执行顺序显示
     */
    private void initExecutionOrderDisplay() {
        executionOrderContainer.getChildren().clear();
        
        // 标题
        Label titleLabel = new Label("📋 执行顺序：");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        executionOrderContainer.getChildren().add(titleLabel);
        
        // 创建流程容器
        HBox flowContainer = new HBox(15);
        flowContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        flowContainer.setPrefWidth(ganttArea.getWidth() - 20);  // 设置宽度，让内容可以换行
        
        // 为调度结果中的每个进程创建小球和箭头（去重处理）
        Process lastProcess = null;
        int index = 0;
        for (int i = 0; i < scheduleResult.size(); i++) {
            Process p = scheduleResult.get(i);
            
            // 跳过与前一个相同的进程（去重）
            if (lastProcess != null && p.getName().equals(lastProcess.getName())) {
                continue;
            }
            
            // 添加箭头（除了第一个）
            if (index > 0) {
                Label arrow = new Label("➜");
                arrow.setStyle("-fx-font-size: 20px; -fx-text-fill: #667eea;");
                flowContainer.getChildren().add(arrow);
            }
            
            // 创建进程小球
            Circle circle = new Circle(25, p.getColor());
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(2);
            
            // 添加标签
            StackPane ballWithLabel = new StackPane();
            ballWithLabel.setAlignment(javafx.geometry.Pos.CENTER);
            
            Label lblName = new Label(p.getName());
            lblName.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            ballWithLabel.getChildren().addAll(circle, lblName);
            ballWithLabel.setUserData(i);  // 保存时间索引
            
            flowContainer.getChildren().add(ballWithLabel);
            
            lastProcess = p;
            index++;
        }
        
        executionOrderContainer.getChildren().add(flowContainer);
        
        // 添加说明
        Label infoLabel = new Label("💡 提示：红色小球表示正在执行的进程");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        executionOrderContainer.getChildren().add(infoLabel);
    }
    
    /**
     * 高亮当前执行的进程
     */
    private void highlightCurrentProcess(Process currentProcess) {
        // 清除所有高亮
        for (javafx.scene.Node node : executionOrderContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox container = (HBox) node;
                for (javafx.scene.Node child : container.getChildren()) {
                    if (child instanceof StackPane) {
                        StackPane ballWithLabel = (StackPane) child;
                        Circle circle = (Circle) ballWithLabel.getChildren().get(0);
                        
                        // 恢复原始颜色
                        Integer timeIndex = (Integer) ballWithLabel.getUserData();
                        if (timeIndex != null && timeIndex < scheduleResult.size()) {
                            Process p = scheduleResult.get(timeIndex);
                            circle.setFill(p.getColor());
                            circle.setRadius(25);
                            circle.setStroke(Color.WHITE);
                            circle.setStrokeWidth(2);
                        }
                    }
                }
            }
        }
        
        // 高亮第 currentStep 个小球（精确对应）
        if (currentStep < scheduleResult.size()) {
            for (javafx.scene.Node node : executionOrderContainer.getChildren()) {
                if (node instanceof HBox) {
                    HBox container = (HBox) node;
                    for (javafx.scene.Node child : container.getChildren()) {
                        if (child instanceof StackPane) {
                            StackPane ballWithLabel = (StackPane) child;
                            Integer timeIndex = (Integer) ballWithLabel.getUserData();
                            
                            if (timeIndex != null && timeIndex == currentStep) {
                                Circle circle = (Circle) ballWithLabel.getChildren().get(0);
                                circle.setFill(Color.RED);
                                circle.setRadius(30);
                                circle.setStroke(Color.YELLOW);
                                circle.setStrokeWidth(3);
                                return;  // 找到后立即返回
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 计算并显示性能指标
     */
    private void calculateAndDisplayMetrics() {
        Scheduler.PerformanceMetrics metrics = scheduler.calculateMetrics();
        
        lblAvgWaitTime.setText(String.format("%.2f ms", metrics.getAvgWaitTime()));
        lblAvgTurnaroundTime.setText(String.format("%.2f ms", metrics.getAvgTurnaroundTime()));
        lblCpuUsage.setText(String.format("%.1f%%", metrics.getCpuUsage()));
    }
    
    /**
     * 更新状态显示 (动画版本，根据执行进度计算剩余时间)
     */
    private void updateStatusDisplayWithAnimation(int currentStep) {
        // 如果进程框列表为空或者进程数量变化，重新创建
        if (processBoxes.size() != scheduler.getProcessList().size()) {
            processBoxes.clear();
            readyQueueContainer.getChildren().clear();
                
            for (Process p : scheduler.getProcessList()) {
                VBox processBox = createProcessBox(p);
                processBoxes.add(processBox);
                readyQueueContainer.getChildren().add(processBox);
            }
        } else {
            // 只更新文本内容，根据动画进度计算剩余时间
            // 注意：只有 RR 算法才需要动态更新剩余时间
            RadioButton selectedRadio = (RadioButton) algorithmGroup.getSelectedToggle();
            boolean isRR = (selectedRadio == radioRR);
                
            for (int i = 0; i < scheduler.getProcessList().size(); i++) {
                Process p = scheduler.getProcessList().get(i);
                VBox box = processBoxes.get(i);
                // 找到第三个子节点 (Label)
                if (box.getChildren().size() >= 3) {
                    Label lblInfo = (Label) box.getChildren().get(2);
                    if (isRR) {
                        // RR 算法：计算从当前步骤开始该进程还需要执行的时间
                        int remainingTime = calculateRemainingTime(p, currentStep);
                        if (remainingTime > 0) {
                            lblInfo.setText("剩余:" + remainingTime + "ms");
                            lblInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: #333;");
                        } else {
                            // 进程已完成
                            lblInfo.setText("✓ 已完成");
                            lblInfo.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
                        }
                    } else {
                        // 其他算法：保持显示运行时间和优先级（不更新）
                        lblInfo.setText(p.getBurstTime() + "ms,P" + p.getPriority());
                    }
                }
            }
        }
    }
    
    /**
     * 创建进程显示框
     */
    private VBox createProcessBox(Process process) {
        VBox box = new VBox(5);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-radius: 8; -fx-padding: 8;");
        
        // 进程小球
        Circle circle = new Circle(20, process.getColor());
        
        // 进程信息
        Label lblName = new Label(process.getName());
        lblName.setStyle("-fx-font-weight: bold;");
        
        // 根据当前选中的算法决定显示内容
        RadioButton selectedRadio = (RadioButton) algorithmGroup.getSelectedToggle();
        Label lblInfo;
        if (selectedRadio == radioRR) {
            // RR 算法：显示剩余时间
            lblInfo = new Label("剩余:" + process.getBurstTime() + "ms");
        } else {
            // 其他算法：显示运行时间和优先级
            lblInfo = new Label(process.getBurstTime() + "ms,P" + process.getPriority());
        }
        lblInfo.setStyle("-fx-font-size: 10px;");
        
        box.getChildren().addAll(circle, lblName, lblInfo);
        return box;
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
        int priority = comboPriority.getValue();
        
        // 检查是否超过 4 个进程
        if (scheduler.getProcessList().size() >= 4) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("最多只能添加 4 个进程！");
            alert.showAndWait();
            return;
        }
        
        // 创建进程
        Color color = PROCESS_COLORS[colorIndex % PROCESS_COLORS.length];
        Process process = new Process(name, burstTime, priority, color);
        scheduler.addProcess(process);
        
        // 更新显示（传入 step=0，因为还没开始调度）
        updateStatusDisplayWithAnimation(0);
        
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
        processBoxes.clear();
        colorIndex = 0;
        cpuArea.getChildren().clear();
        ganttArea.getChildren().clear();
        readyQueueContainer.getChildren().clear();
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
        
        // 获取选中的算法
        RadioButton selectedRadio = (RadioButton) algorithmGroup.getSelectedToggle();
        if (selectedRadio == radioFCFS) {
            scheduler.setAlgorithm(Scheduler.Algorithm.FCFS);
        } else if (selectedRadio == radioSJF) {
            scheduler.setAlgorithm(Scheduler.Algorithm.SJF);
        } else if (selectedRadio == radioRR) {
            scheduler.setAlgorithm(Scheduler.Algorithm.RR);
            scheduler.setTimeSlice(spinnerTimeSlice.getValue());
        } else if (selectedRadio == radioPriority) {
            scheduler.setAlgorithm(Scheduler.Algorithm.PRIORITY);
        }
        
        // 重置所有进程的状态（恢复剩余时间）
        for (Process p : scheduler.getProcessList()) {
            p.setRemainingTime(p.getBurstTime());
            p.setWaitingTime(0);
            p.setTurnaroundTime(0);
        }
        
        // 执行调度
        scheduleResult = scheduler.execute();
        currentStep = 0;
        
        // 打印调度结果用于调试（仅 RR 算法）
        if (selectedRadio == radioRR) {
            System.out.println("[调试] 调度结果长度：" + scheduleResult.size());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < scheduleResult.size(); i++) {
                sb.append(scheduleResult.get(i).getName());
                if (i < scheduleResult.size() - 1) sb.append("->");
            }
            System.out.println("[调试] 调度序列：" + sb.toString());
        }
        
        // 如果是 RR 算法，更新 UI 显示统计信息
        if (selectedRadio == radioRR) {
            updateRRInfoDisplay();
        }
        
        // 开始动画
        isRunning = true;
        btnStart.setDisable(true);
        btnPause.setDisable(false);
        animationTimer.start();
        
        System.out.println("[调试] 开始调度，算法：" + scheduler.getAlgorithm());
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
        scheduleResult = null;
        ganttCurrentTime = 0;
        animationTimer.stop();
        
        // 重置显示
        cpuArea.getChildren().clear();
        ganttArea.getChildren().clear();
        
        // 重新添加执行顺序容器
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
        
        // 重新显示进程队列（重置时）
        updateStatusDisplayWithAnimation(0);
        
        System.out.println("[调试] 重置调度");
    }
    
    /**
     * 重置性能指标
     */
    private void resetMetrics() {
        lblAvgWaitTime.setText("-- ms");
        lblAvgTurnaroundTime.setText("-- ms");
        lblCpuUsage.setText("--%");
        
        // 重置 RR 信息显示
        if (rrInfoBox != null) {
            rrInfoBox.setVisible(false);
            rrInfoBox.setManaged(false);
        }
    }
    
    /**
     * 更新 RR 算法信息显示
     */
    private void updateRRInfoDisplay() {
        if (rrInfoBox != null && lblMaxRounds != null && lblUnfinishedProcesses != null) {
            int timeSlice = scheduler.getTimeSlice();
            int unfinishedCount = scheduler.getRrUnfinishedCount();
            
            lblMaxRounds.setText("⏱️ 时间片：" + timeSlice + "ms");
            lblUnfinishedProcesses.setText("⏳ 未完成进程：" + unfinishedCount + "个");
            
            // 根据未完成进程数设置颜色
            if (unfinishedCount > 0) {
                lblUnfinishedProcesses.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f5576c;");
            } else {
                lblUnfinishedProcesses.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
            }
            
            rrInfoBox.setVisible(true);
            rrInfoBox.setManaged(true);
        }
    }
    
    /**
     * 返回主菜单
     */
    @FXML
    private void handleBackToMain() {
        System.out.println("[调试] 从进程调度实验返回主菜单");
        animationTimer.stop();
        SceneSwitcher.switchScene(canvasContainer, "/fxml/MainMenu.fxml", "操作系统实验模拟系统");
    }
}
