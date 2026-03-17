package com.homework.osexperiment.osexp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import com.homework.osexperiment.osexp.util.SceneSwitcher;

/**
 * 主菜单控制器 - 处理实验选择
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class MainMenuController {
    
    @FXML
    private Button btnProcessConcurrency;
    
    @FXML
    private Button btnProducerConsumer;
    
    @FXML
    private Button btnProcessScheduling;
    
    /**
     * 进入实验一：进程并发模拟
     */
    @FXML
    private void handleProcessConcurrency() {
        System.out.println("[调试] 点击了实验一：进程并发模拟");
        SceneSwitcher.switchScene(btnProcessConcurrency, "/fxml/ProcessConcurrency.fxml", "实验一：进程并发模拟");
    }
    
    /**
     * 进入实验二：生产者与消费者问题
     */
    @FXML
    private void handleProducerConsumer() {
        System.out.println("[调试] 点击了实验二：生产者与消费者问题");
        SceneSwitcher.switchScene(btnProducerConsumer, "/fxml/ProducerConsumer.fxml", "实验二：生产者与消费者问题");
    }
    
    /**
     * 进入实验三：进程调度模拟
     */
    @FXML
    private void handleProcessScheduling() {
        System.out.println("[调试] 点击了实验三：进程调度模拟");
        SceneSwitcher.switchScene(btnProcessScheduling, "/fxml/ProcessScheduling.fxml", "实验三：进程调度模拟");
    }
}
