package com.homework.osexperiment.osexp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX 应用程序主入口
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class APP extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载主菜单 FXML 文件
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainMenu.fxml"));
        Parent root = loader.load();
        
        // 创建场景（增大窗口尺寸）
        Scene scene = new Scene(root, 1400, 900);
        
        // 加载 CSS 样式文件
        String cssPath = "/css/style.css";
        if (getClass().getResource(cssPath) != null) {
            scene.getStylesheets().add(cssPath);
        }
        
        // 设置舞台属性
        primaryStage.setTitle("操作系统实验模拟系统");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);  // 允许调整大小
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
