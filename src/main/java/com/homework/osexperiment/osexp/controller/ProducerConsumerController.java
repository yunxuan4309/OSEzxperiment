package com.homework.osexperiment.osexp.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import com.homework.osexperiment.osexp.util.SceneSwitcher;
import com.homework.osexperiment.osexp.model.*;

/**
 * 实验二：生产者与消费者问题 - 控制器
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class ProducerConsumerController {
    
    @FXML
    private VBox canvasContainer;
    
    @FXML
    private Pane animationArea;
    
    @FXML
    private VBox bufferVisual;
    
    @FXML
    private VBox producerArea;
    
    @FXML
    private VBox consumerArea;
    
    @FXML
    private Label lblBufferStatus;
    
    @FXML
    private Label lblBufferLock;
    
    @FXML
    private Label lblProducerStatus;
    
    @FXML
    private Label lblLastProduct;
    
    @FXML
    private Label lblConsumerStatus;
    
    @FXML
    private Label lblLastConsume;
    
    @FXML
    private Label lblMutexValue;
    
    @FXML
    private Label lblEmptyValue;
    
    @FXML
    private Label lblFullValue;
    
    @FXML
    private Label lblPVOperation;
    
    @FXML
    private Button btnStart;
    
    @FXML
    private Button btnPause;
    
    @FXML
    private Button btnProduce;
    
    @FXML
    private Button btnConsume;
    
    // 模型对象
    private Buffer buffer;              // 缓冲区（容量 5）
    private int productCounter = 0;     // 产品计数器
    private String lastProductInfo = "无";   // 最后生产的产品信息
    private String lastConsumeInfo = "无";   // 最后消费的产品信息
    
    // 小球显示管理
    private javafx.scene.layout.VBox producerBallContainer;  // 生产者区域小球容器
    private javafx.scene.layout.VBox consumerBallContainer;  // 消费者区域小球容器
    
    // 动画计时器
    private AnimationTimer animationTimer;
    
    // 运行状态
    private volatile boolean isProducing = false;  // 是否正在生产
    private volatile boolean isConsuming = false;  // 是否正在消费
    
    // 生产者线程和消费者线程
    private Thread producerThread;
    private Thread consumerThread;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("[调试] 生产者消费者实验界面已加载");
        
        // 初始化缓冲区
        buffer = new Buffer(5);
        
        // 初始化小球容器
        producerBallContainer = new javafx.scene.layout.VBox(5);
        producerBallContainer.setAlignment(javafx.geometry.Pos.CENTER);
        producerBallContainer.setStyle("-fx-padding: 10; -fx-spacing: 5;");
        
        consumerBallContainer = new javafx.scene.layout.VBox(5);
        consumerBallContainer.setAlignment(javafx.geometry.Pos.CENTER);
        consumerBallContainer.setStyle("-fx-padding: 10; -fx-spacing: 5;");
        
        // 将小球容器添加到对应的区域
        if (producerArea != null) {
            producerArea.getChildren().clear();
            producerArea.getChildren().addAll(
                new Label("🏭 生产者"),
                producerBallContainer
            );
            ((Label)producerArea.getChildren().get(0)).setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        }
        
        if (consumerArea != null) {
            consumerArea.getChildren().clear();
            consumerArea.getChildren().addAll(
                new Label("🛒 消费者"),
                consumerBallContainer
            );
            ((Label)consumerArea.getChildren().get(0)).setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        }
        
        // 设置按钮初始状态
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        btnProduce.setDisable(true);
        btnConsume.setDisable(true);
        
        // 初始化动画循环
        initAnimationTimer();
        
        // 更新状态显示
        updateStatusDisplay();
        updateSemaphoreDisplay();
    }
    
    /**
     * 初始化动画计时器
     */
    private void initAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 更新缓冲区可视化
                updateBufferVisual();
                // 更新信号量显示
                updateSemaphoreDisplay();
            }
        };
    }
    
    /**
     * 更新信号量显示
     */
    private void updateSemaphoreDisplay() {
        if (buffer != null && lblMutexValue != null) {
            lblMutexValue.setText(String.valueOf(buffer.getMutexValue()));
            lblEmptyValue.setText(String.valueOf(buffer.getEmptyValue()));
            lblFullValue.setText(String.valueOf(buffer.getFullValue()));
        }
    }
    
    /**
     * 更新 PV 操作日志
     */
    private void updatePVOperationLog(String message) {
        if (lblPVOperation != null) {
            javafx.application.Platform.runLater(() -> {
                lblPVOperation.setText(message);
            });
        }
    }
    
    /**
     * 生产者线程任务
     */
    private Runnable createProducerTask() {
        return () -> {
            try {
                while (isProducing && !Thread.currentThread().isInterrupted()) {
                    // 生成产品
                    productCounter++;
                    Product product = new Product(productCounter, "产品 #" + productCounter);
                    lastProductInfo = "产品 #" + productCounter;
                    
                    // 执行生产（包含 AND 信号量的 P/V 操作）
                    updatePVOperationLog("🔄 生产者执行: Swait(empty, mutex) → 生产 → Ssignal(mutex, full)");
                    buffer.produce(product);
                    
                    // 更新 UI
                    javafx.application.Platform.runLater(() -> {
                        lblLastProduct.setText("最后生产：" + lastProductInfo);
                        lblProducerStatus.setText("✅ 已生产 " + lastProductInfo);
                        addProducerBall(productCounter);
                        
                        // 延迟后移除生产者小球
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                            javafx.util.Duration.millis(500)
                        );
                        delay.setOnFinished(e -> removeProducerBall());
                        delay.play();
                    });
                    
                    // 随机延迟模拟生产时间（300-800ms）
                    Thread.sleep(300 + (int)(Math.random() * 500));
                }
            } catch (InterruptedException e) {
                System.out.println("[调试] 生产者线程被中断");
                Thread.currentThread().interrupt();
            }
        };
    }
    
    /**
     * 消费者线程任务
     */
    private Runnable createConsumerTask() {
        return () -> {
            try {
                while (isConsuming && !Thread.currentThread().isInterrupted()) {
                    // 执行消费（包含 AND 信号量的 P/V 操作）
                    // 如果缓冲区为空，swait 会自动阻塞等待
                    updatePVOperationLog("🔄 消费者执行: Swait(full, mutex) → 消费 → Ssignal(mutex, empty)");
                    Product product = buffer.consume();
                    
                    if (product != null) {
                        lastConsumeInfo = "产品 #" + product.getId();
                        
                        // 更新 UI
                        javafx.application.Platform.runLater(() -> {
                            lblLastConsume.setText("最后消费：" + lastConsumeInfo);
                            lblConsumerStatus.setText("✅ 已消费 " + lastConsumeInfo);
                            addConsumerBall(product.getId());
                        });
                    }
                    
                    // 随机延迟模拟消费时间（400-1000ms）
                    Thread.sleep(400 + (int)(Math.random() * 600));
                }
            } catch (InterruptedException e) {
                System.out.println("[调试] 消费者线程被中断");
                Thread.currentThread().interrupt();
            }
        };
    }
    
    /**
     * 停止生产
     */
    private void stopProduce() {
        isProducing = false;
        if (producerThread != null && producerThread.isAlive()) {
            producerThread.interrupt();
            try {
                producerThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        btnProduce.setText("▶️ 开始生产");
        btnProduce.setStyle("-fx-background-color: linear-gradient(to right, #f093fb, #f5576c);");
        lblProducerStatus.setText("🛑 已停止生产");
        updatePVOperationLog("⏹️ 生产者已停止");
    }
    
    /**
     * 停止消费
     */
    private void stopConsume() {
        isConsuming = false;
        if (consumerThread != null && consumerThread.isAlive()) {
            consumerThread.interrupt();
            try {
                consumerThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        btnConsume.setText("▶️ 开始消费");
        btnConsume.setStyle("-fx-background-color: linear-gradient(to right, #4facfe, #00f2fe);");
        lblConsumerStatus.setText("🛑 已停止消费");
        updatePVOperationLog("⏹️ 消费者已停止");
    }
    /**
     * 更新状态显示
     */
    private void updateStatusDisplay() {
        // 更新缓冲区状态
        lblBufferStatus.setText("📦 缓冲区：" + buffer.size() + "/" + buffer.getCapacity());
        
        // 更新生产者状态
        lblProducerStatus.setText("🏭 生产者：待机");
        lblLastProduct.setText("最后生产：" + lastProductInfo);
        
        // 更新消费者状态
        lblConsumerStatus.setText("🛒 消费者：待机");
        lblLastConsume.setText("最后消费：" + lastConsumeInfo);
        
        // 更新缓冲区可视化
        updateBufferVisual();
    }
    
    /**
     * 更新缓冲区可视化显示
     */
    private void updateBufferVisual() {
        // 清空现有显示
        bufferVisual.getChildren().clear();
        
        int size = buffer.size();
        int capacity = buffer.getCapacity();
        
        // 创建产品小球（从下往上堆叠）
        for (int i = 0; i < size; i++) {
            javafx.scene.shape.Circle productCircle = new javafx.scene.shape.Circle(15, javafx.scene.paint.Color.ORANGE);
            productCircle.setCenterX(50);
            productCircle.setCenterY(230 - i * 40); // 从底部向上
            bufferVisual.getChildren().add(productCircle);
        }
        
        // 根据信号量状态更新边框颜色
        if (buffer.getMutexValue() == 0) {
            // mutex=0 表示有进程在临界区
            if (buffer.getEmptyValue() < buffer.getCapacity()) {
                // 生产者在使用
                bufferVisual.setStyle("-fx-border-color: red; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");
                lblBufferLock.setText("🔒 生产者临界区");
                lblBufferLock.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else {
                // 消费者在使用
                bufferVisual.setStyle("-fx-border-color: blue; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");
                lblBufferLock.setText("🔒 消费者临界区");
                lblBufferLock.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
            }
        } else {
            bufferVisual.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
            lblBufferLock.setText("🔓 未锁定");
            lblBufferLock.setStyle("-fx-text-fill: #5a5a5a; -fx-font-weight: normal;");
        }
    }
    
    /**
     * 添加生产者小球
     */
    private void addProducerBall(int productId) {
        javafx.scene.layout.StackPane ballWithLabel = new javafx.scene.layout.StackPane();
        ballWithLabel.setAlignment(javafx.geometry.Pos.CENTER);
        
        // 创建小球
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(20, javafx.scene.paint.Color.RED);
        circle.setStroke(javafx.scene.paint.Color.WHITE);
        circle.setStrokeWidth(2);
        
        // 添加编号标签
        Label lblId = new Label(String.valueOf(productId));
        lblId.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        ballWithLabel.getChildren().addAll(circle, lblId);
        producerBallContainer.getChildren().add(ballWithLabel);
    }
    
    /**
     * 移除生产者小球（移除第一个）
     */
    private void removeProducerBall() {
        if (!producerBallContainer.getChildren().isEmpty()) {
            producerBallContainer.getChildren().remove(0);
        }
    }
    
    /**
     * 添加消费者小球
     */
    private void addConsumerBall(int productId) {
        javafx.scene.layout.StackPane ballWithLabel = new javafx.scene.layout.StackPane();
        ballWithLabel.setAlignment(javafx.geometry.Pos.CENTER);
        
        // 创建小球
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(20, javafx.scene.paint.Color.BLUE);
        circle.setStroke(javafx.scene.paint.Color.WHITE);
        circle.setStrokeWidth(2);
        
        // 添加编号标签
        Label lblId = new Label(String.valueOf(productId));
        lblId.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        ballWithLabel.getChildren().addAll(circle, lblId);
        consumerBallContainer.getChildren().add(ballWithLabel);
    }
    /**
     * 开始生产按钮处理
     */
    @FXML
    private void handleStartProduce() {
        System.out.println("[调试] 点击了开始生产按钮");
        
        // 如果已经在消费，先停止消费
        if (isConsuming) {
            stopConsume();
        }
        
        // 切换生产状态
        if (isProducing) {
            stopProduce();
        } else {
            isProducing = true;
            btnProduce.setText("⏹️ 停止生产");
            btnProduce.setStyle("-fx-background-color: linear-gradient(to right, #ff6b6b, #ee5a6f);");
            lblProducerStatus.setText("▶️ 启动生产者线程...");
            
            // 创建并启动生产者线程
            producerThread = new Thread(createProducerTask(), "Producer-Thread");
            producerThread.setDaemon(true);
            producerThread.start();
            
            updatePVOperationLog("🚀 生产者线程已启动，准备执行 Swait 操作");
        }
    }
    
    /**
     * 开始消费按钮处理
     */
    @FXML
    private void handleStartConsume() {
        System.out.println("[调试] 点击了开始消费按钮");
        
        // 如果正在生产，先停止生产
        if (isProducing) {
            stopProduce();
        }
        
        // 切换消费状态
        if (isConsuming) {
            stopConsume();
        } else {
            isConsuming = true;
            btnConsume.setText("⏹️ 停止消费");
            btnConsume.setStyle("-fx-background-color: linear-gradient(to right, #45b7d1, #29a3c4);");
            lblConsumerStatus.setText("▶️ 启动消费者线程...");
            
            // 创建并启动消费者线程
            consumerThread = new Thread(createConsumerTask(), "Consumer-Thread");
            consumerThread.setDaemon(true);
            consumerThread.start();
            
            updatePVOperationLog("🚀 消费者线程已启动，准备执行 Swait 操作");
        }
    }
    
    /**
     * 开始按钮处理
     */
    @FXML
    private void handleStart() {
        System.out.println("[调试] 点击了开始实验按钮");
        btnProduce.setDisable(false);
        btnConsume.setDisable(false);
        btnStart.setDisable(true);
        btnPause.setDisable(false);
        animationTimer.start();
        updateStatusDisplay();
    }
    
    /**
     * 暂停按钮处理
     */
    @FXML
    private void handlePause() {
        System.out.println("[调试] 点击了暂停按钮");
        
        // 停止当前的生产或消费
        if (isProducing) {
            stopProduce();
        }
        if (isConsuming) {
            stopConsume();
        }
        
        btnProduce.setDisable(true);
        btnConsume.setDisable(true);
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        lblProducerStatus.setText("🏭 生产者：已暂停");
        lblConsumerStatus.setText("🛒 消费者：已暂停");
    }
    
    /**
     * 重置按钮处理
     */
    @FXML
    private void handleReset() {
        System.out.println("[调试] 点击了重置按钮");
        
        // 停止动画
        animationTimer.stop();
        
        // 停止当前的生产或消费
        if (isProducing) {
            stopProduce();
        }
        if (isConsuming) {
            stopConsume();
        }
        
        isProducing = false;
        isConsuming = false;
        
        btnProduce.setDisable(true);
        btnConsume.setDisable(true);
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        
        // 重置所有数据
        buffer.clear();
        productCounter = 0;
        lastProductInfo = "无";
        lastConsumeInfo = "无";
        
        // 清空小球容器
        if (producerBallContainer != null) {
            producerBallContainer.getChildren().clear();
        }
        if (consumerBallContainer != null) {
            consumerBallContainer.getChildren().clear();
        }
        
        // 更新显示
        updateStatusDisplay();
        updateSemaphoreDisplay();
        updatePVOperationLog("🔄 系统已重置，信号量恢复初始状态");
    }
    
    /**
     * 返回主菜单
     */
    @FXML
    private void handleBackToMain() {
        System.out.println("[调试] 从生产者消费者实验返回主菜单");
        SceneSwitcher.switchScene(canvasContainer, "/fxml/MainMenu.fxml", "操作系统实验模拟系统");
    }
}
