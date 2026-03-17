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
    private boolean isProducing = false;  // 是否正在生产
    private boolean isConsuming = false;  // 是否正在消费
    
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
    }
    
    /**
     * 初始化动画计时器
     */
    private void initAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 如果正在生产，自动生产产品
                if (isProducing) {
                    autoProduce();
                }
                // 如果正在消费，自动消费产品
                if (isConsuming) {
                    autoConsume();
                }
                // 更新缓冲区可视化
                updateBufferVisual();
            }
        };
    }
    
    /**
     * 自动生产（由动画计时器调用）
     */
    private void autoProduce() {
        // 检查缓冲区是否已满
        if (buffer.isFull()) {
            System.out.println("[提示] 缓冲区已满，停止生产");
            stopProduce();
            lblProducerStatus.setText("⚠️ 缓冲区已满，已停止");
            return;
        }
        
        // 尝试获取锁（如果消费者正在使用，则等待）
        if (buffer.isLocked() && buffer.getLockedBy().contains("消费者")) {
            lblProducerStatus.setText("⏳ 等待锁（消费者使用中）...");
            return;
        }
        
        // 生产产品
        productCounter++;
        lastProductInfo = "产品 #" + productCounter;
        Product product = new Product(productCounter, "产品 #" + productCounter);
        
        if (buffer.produce(product)) {
            System.out.println("[成功] 生产了 " + lastProductInfo);
            lblProducerStatus.setText("✅ 生产中..." + lastProductInfo);
            lblLastProduct.setText("最后生产：" + lastProductInfo);
            
            // 添加生产者小球
            addProducerBall(productCounter);
            
            // 延迟后移除生产者小球并添加到缓冲区
            javafx.application.Platform.runLater(() -> {
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
                delay.setOnFinished(e -> {
                    removeProducerBall();
                });
                delay.play();
            });
        }
        
        // 暂停一下再生产（模拟生产时间）
        try {
            Thread.sleep(800); // 0.8 秒生产一个
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 自动消费（由动画计时器调用）
     */
    private void autoConsume() {
        // 检查缓冲区是否为空
        if (buffer.isEmpty()) {
            System.out.println("[提示] 缓冲区为空，停止消费");
            stopConsume();
            lblConsumerStatus.setText("⚠️ 缓冲区为空，已停止");
            return;
        }
        
        // 尝试获取锁（如果生产者正在使用，则等待）
        if (buffer.isLocked() && buffer.getLockedBy().contains("生产者")) {
            lblConsumerStatus.setText("⏳ 等待锁（生产者使用中）...");
            return;
        }
        
        // 消费产品
        Product product = buffer.consume();
        if (product != null) {
            lastConsumeInfo = "产品 #" + product.getId();
            System.out.println("[成功] 消费了 " + lastConsumeInfo);
            lblConsumerStatus.setText("✅ 消费中..." + lastConsumeInfo);
            lblLastConsume.setText("最后消费：" + lastConsumeInfo);
            
            // 添加消费者小球
            addConsumerBall(product.getId());
        }
        
        // 暂停一下再消费（模拟消费时间）
        try {
            Thread.sleep(1000); // 1 秒消费一个
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 停止生产
     */
    private void stopProduce() {
        isProducing = false;
        btnProduce.setText("▶️ 开始生产");
        btnProduce.setStyle("-fx-background-color: linear-gradient(to right, #f093fb, #f5576c);");
    }
    
    /**
     * 停止消费
     */
    private void stopConsume() {
        isConsuming = false;
        btnConsume.setText("▶️ 开始消费");
        btnConsume.setStyle("-fx-background-color: linear-gradient(to right, #4facfe, #00f2fe);");
    }
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
        
        // 更新锁定状态显示
        if (buffer.isLocked()) {
            String lockedBy = buffer.getLockedBy();
            if (lockedBy.contains("生产者")) {
                bufferVisual.setStyle("-fx-border-color: red; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");
                lblBufferLock.setText("🔒 生产者使用中");
                lblBufferLock.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else {
                bufferVisual.setStyle("-fx-border-color: blue; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");
                lblBufferLock.setText("🔒 消费者使用中");
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
            // 正在生产中，点击后停止
            stopProduce();
            lblProducerStatus.setText("🛑 已停止生产");
        } else {
            // 开始生产
            isProducing = true;
            btnProduce.setText("⏹️ 停止生产");
            btnProduce.setStyle("-fx-background-color: linear-gradient(to right, #ff6b6b, #ee5a6f);");
            lblProducerStatus.setText("▶️ 开始生产...");
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
            // 正在消费中，点击后停止
            stopConsume();
            lblConsumerStatus.setText("🛑 已停止消费");
        } else {
            // 开始消费
            isConsuming = true;
            btnConsume.setText("⏹️ 停止消费");
            btnConsume.setStyle("-fx-background-color: linear-gradient(to right, #45b7d1, #29a3c4);");
            lblConsumerStatus.setText("▶️ 开始消费...");
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
