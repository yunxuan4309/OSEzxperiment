package com.homework.osexperiment.osexp.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 场景切换工具类
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class SceneSwitcher {
    
    /**
     * 切换到新场景
     * 
     * @param sourceNode 触发事件的源节点
     * @param fxmlPath FXML 文件路径
     * @param title 窗口标题
     */
    public static void switchScene(Node sourceNode, String fxmlPath, String title) {
        try {
            // 加载 FXML 文件
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource(fxmlPath));
            Parent root = loader.load();
            
            // 获取当前舞台
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            
            // 保持当前窗口大小
            double currentWidth = sourceNode.getScene().getWidth();
            double currentHeight = sourceNode.getScene().getHeight();
            Scene scene = new Scene(root, currentWidth, currentHeight);
            
            // 如果 CSS 文件存在，则加载
            String cssPath = "/css/style.css";
            if (SceneSwitcher.class.getResource(cssPath) != null) {
                scene.getStylesheets().add(cssPath);
            }
            
            // 设置舞台属性
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
            
            System.out.println("[调试] 成功切换到场景：" + fxmlPath);
            
        } catch (Exception e) {
            System.err.println("[错误] 切换场景失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
