package com.homework.osexperiment.osexp.model;

/**
 * 产品类 - 表示生产者生产、消费者消费的物品
 * 
 * @author 谢云轩
 * @version 1.0
 */
public class Product {
    private int id;           // 产品 ID
    private String name;      // 产品名称
    
    /**
     * 构造函数
     */
    public Product(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    /**
     * 获取产品 ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * 获取产品名称
     */
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return "产品 #" + id;
    }
}
