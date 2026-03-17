# 操作系统实验模拟系统 - 使用说明

## 项目简介

这是一个基于 JavaFX 开发的操作系统课程实验可视化模拟系统，包含三个实验：
- **实验一：进程并发模拟** - 使用图形化方式模拟进程的并发执行
- **实验二：生产者与消费者问题** - 可视化演示生产者 - 消费者模型
- **实验三：进程调度模拟** - 模拟不同进程调度算法的执行过程

## 环境要求

- **JDK 版本**：Java 17 或更高版本
- **构建工具**：Maven 3.6+
- **JavaFX 版本**：17.0.6（已通过 Maven 依赖自动管理）

## 运行方式

### 方式一：使用批处理文件（推荐）

双击项目根目录下的 `run.bat` 文件即可自动启动程序：

```bash
run.bat
```

### 方式二：使用 Maven 命令

在项目根目录下执行以下命令：

```bash
mvn clean javafx:run
```

**命令说明：**
- `clean` - 清理之前的编译结果
- `javafx:run` - 使用 JavaFX Maven 插件启动应用

### 方式三：在 IntelliJ IDEA 中配置运行

1. 点击菜单栏 **Run** → **Edit Configurations...**
2. 点击 **+** 号 → 选择 **Maven**
3. 配置如下：
   - **Command line**: `javafx:run`
   - **Working directory**: 选择项目根目录（`D:\java\OSExperiment`）
4. 点击 **OK** 保存配置
5. 运行该 Maven 配置即可启动程序

## 使用方法

1. 程序启动后，会显示主菜单界面
2. 点击三个按钮之一进入对应的实验
3. 在实验界面中可以查看实验演示（当前为框架，具体功能待实现）
4. 点击 **返回主菜单** 按钮可回到主界面选择其他实验

## 项目结构

```
OSExperiment/
├── src/main/
│   ├── java/com/homework/osexperiment/osexp/
│   │   ├── APP.java                    # 主启动类
│   │   └── controller/                 # 控制器包
│   │       ├── MainMenuController.java          # 主菜单控制器
│   │       ├── ProcessConcurrencyController.java # 实验一控制器
│   │       ├── ProducerConsumerController.java   # 实验二控制器
│   │       └── ProcessSchedulingController.java  # 实验三控制器
│   │
│   └── resources/
│       ├── fxml/                       # FXML 界面文件
│       │   ├── MainMenu.fxml
│       │   ├── ProcessConcurrency.fxml
│       │   ├── ProducerConsumer.fxml
│       │   └── ProcessScheduling.fxml
│       │
│       └── css/                        # 样式文件
│           └── style.css
│
├── pom.xml                             # Maven 配置文件
└── run.bat                             # Windows 启动脚本
```

## 常见问题

### 问题 1：运行时提示"缺少 JavaFX 运行时组件"

**原因**：JavaFX 17+ 需要使用模块路径加载，不能直接通过类路径运行。

**解决方法**：必须使用 Maven 的 `javafx:run` 插件启动，不要直接在 IDEA 中右键运行 `APP.java`。

### 问题 2：Maven 命令无法识别

**原因**：Maven 未安装或未配置环境变量。

**解决方法**：
1. 确保已安装 Maven 3.6+
2. 配置 `MAVEN_HOME` 环境变量
3. 将 Maven 的 `bin` 目录添加到 `PATH` 环境变量

### 问题 3：编译错误

**解决方法**：
1. 确保 JDK 版本为 17 或更高
2. 执行 `mvn clean install` 重新构建项目
3. 检查 IDE 的项目 SDK 设置是否为 Java 17

## 开发说明

### 添加新的实验功能

1. 在 `src/main/resources/fxml/` 创建新的 FXML 界面文件
2. 在 `src/main/java/.../controller/` 创建对应的 Controller 类
3. 在主菜单中添加相应的入口按钮

### 修改样式

编辑 `src/main/resources/css/style.css` 文件，可以自定义界面颜色、字体等样式。

## 技术栈

- **Java** 17
- **JavaFX** 17.0.6
- **Maven** 项目管理
- **FXML** 界面布局

## 作者信息

- **开发者**：谢云轩
- **学号**：632307060623
- **联系方式**：QQ 1721476339

---

**祝实验顺利！** 🎉
