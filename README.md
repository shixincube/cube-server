# Cube Server

**Cube** “时信魔方”是面向开发者的实时协作开发框架，其是 **Cooperative** ，**Ultrafast** ，**Best-practice** 和 **Efficient** 的首字母缩写。意在为开发者和用户提供专业、可靠的协作产品和技术支持，帮助客户快速部署和构建在线协作场景。

支持的操作系统和软件包括：Windows、Linux 、macOS 、Android、iOS、Chrome、Firefox 等。

Cube 提供的协作功能包括：

* 即时消息（Instant Messaging / IM）。支持卡片消息、通知消息、文件消息和自定义消息等。
* 实时多人语音/多人视频（Multi-party RTC）。支持自适应码率、超低延迟等，支持实时图像识别等。
* 超大规模(100+)会议 （Video Conference）。支持会议控制、演讲模式，自定义 MCU 和 SFU 布局等。
* 群组管理（Group management）。支持集成式管理和扩展组织架构等。
* 共享桌面（Remote Desktop Sharing）。支持无缝集成白板等。
* 云端文件存储（Cloud File Storage）。支持无缝集成文档在线协作等。
* 实时白板（Realtime Whiteboard）。支持集成媒体回放、远程桌面和文档分享等。
* 视频直播（Live video）。支持第三方推流和 CDN ，无缝支持会议直播和回放等。
* 互动课堂（Online Classroom）。支持实时课堂互动和在线习题、考试。
* 电子邮件管理与代收发（Email management）。
* 在线文档协作（Online Document Collaboration）。支持 Word、PowerPoint、Excel 等主流格式文多人在写协作。
* 安全与运维管理（Operation and Maintenance management）。所有数据通道支持加密，可支持国密算法等。
* 风控管理（Risk Management）。对系统内所有文本、图片、视频、文件等内容进行包括 NLP、OCR、IR 等技术手段的风险控制和预警等。


## 功能展示

| 即时消息 |
|:----:|
|![IM](https://static.shixincube.com/cube/assets/showcase/im.gif)|

| 视频聊天(1) | 视频聊天(2) |
|:----:|:----:|
|![VideoChat1](https://static.shixincube.com/cube/assets/showcase/videochat_1.gif)|![VideoChat2](https://static.shixincube.com/cube/assets/showcase/videochat_2.gif)|

| 多人视频聊天(1) | 多人视频聊天(2) |
|:----:|:----:|
|![VideoChat3](https://static.shixincube.com/cube/assets/showcase/videochat_3.gif)|![VideoChat4](https://static.shixincube.com/cube/assets/showcase/videochat_4.gif)|

| 会议 |
|:----:|
|![Conf100](https://static.shixincube.com/cube/assets/showcase/screen_conference.jpg)|
|![ConfTile](https://static.shixincube.com/cube/assets/showcase/screen_conference_tile.jpg)|
|![StartConf](https://static.shixincube.com/cube/assets/showcase/start_conference.gif)|

| 共享桌面 |
|:----:|
|![ScreenSharing](https://static.shixincube.com/cube/assets/showcase/screen_sharing.gif)|

| 云端文件存储 |
|:----:|
|![CFS](https://static.shixincube.com/cube/assets/showcase/cloud_file.gif)|

| 白板 |
|:----:|
|![Whiteboard](https://static.shixincube.com/cube/assets/showcase/whiteboard.gif)|

| 直播 |
|:----:|
|![Live](https://static.shixincube.com/cube/assets/showcase/live.gif)|

| 在线课堂 |
|:----:|
|![OnlineClassroom](https://static.shixincube.com/cube/assets/showcase/online_classroom.gif)|

| 文档协作 |
|:----:|
|![DocCollaboration](https://static.shixincube.com/cube/assets/showcase/doc_collaboration_excel.gif)|
|![DocCollaboration](https://static.shixincube.com/cube/assets/showcase/doc_collaboration.gif)|


## 简介

Cube Server 是 Cube 的服务器端项目。 Cube 服务器端由三大构件组成：

1. 网关服务层。
2. 功能业务单元与集群总线。
3. 存储管理。


## 如何从源代码构建项目

Cube Server 目前支持的操作系统包括：**Ubuntu** 、**CentOS** 、**Debian** 、**Fedora** 和 **openSUSE** 。

如果您需要集群部署请参看 Cube 的技术白皮书和部署手册，该手册可以从 [Cube Manual](https://gitee.com/shixinhulian/cube-manual) 中获取。

### 1. 工具与软件准备

您需要在您的开发环境中正确安装以下工具：

1. 安装 [Java SE](https://www.oracle.com/java/technologies/javase-downloads.html) 。建议从 Oracle 官网下载安装包后，按照安装程序引导进行安装。Cube Server 需要的最低版本为 Java SE 8 。

2. 安装 [Apache Ant](http://ant.apache.org/) 。

  适用 Ubuntu 的安装命令：`sudo apt-get install ant`

  适用 CentOS 的安装命令：`yum -y install ant`

3. 安装 [gcc](http://gcc.gnu.org/) 、[make](http://www.gnu.org/software/make/) 、[cmake](https://cmake.org/) 等。

  适用 Ubuntu 的安装命令：`sudo apt-get install build-essential`

  适用 CentOS 的安装命令：`yum groupinstall "Development Tools" "Development Libraries"`


### 2. 下载工程源码和依赖库

从 [cube-server](https://gitee.com/shixinhulian/cube-server) 获得 Cube Server 的源代码。克隆 [cube-server](https://gitee.com/shixinhulian/cube-server) 代码库：

  `git clone https://gitee.com/shixinhulian/cube-server.git`

从 [cube-server-dependencies](https://gitee.com/shixinhulian/cube-server-dependencies) 获得 Cube Server 需要的依赖库。克隆 [cube-server-dependencies](https://gitee.com/shixinhulian/cube-server-dependencies) 代码库：

  `git clone https://gitee.com/shixinhulian/cube-server-dependencies.git`


**需要注意以下事项：**

 * *cube-server* 和 *cube-server-dependencies* 目录同级。
 * 不能修改 *cube-server-dependencies* 的工程目录名。


 就绪的工程目录结构如下：

```
├── cube                            # 您创建的用于放置 Cube Server 的目录
    ├── cube-server                 # cube-server 代码库目录
    └── cube-server-dependencies    # cube-server-dependencies 代码库目录
```


### 3. 运行构建命令

从代码库下载代码之后，进入 *cube-server* 项目目录依次执行以下步骤来进行项目构建。

1. 执行构建命令： `ant build-all` 。如果需要构建 DEBUG 版本，使用命令：`ant build-all-debug` 。

 执行构建命令之后，会在项目目录的 `build` 子目录下生成 Cube Server 的工程输出文件。

2. 执行部署命令：`ant deploy` ，将编译成功的工程文件安装到部署目录下。


### 4. 启动与停止服务器

1. 启动服务。进入 `deploy` 目录，执行 `start.sh` 脚本。

```shell
cd deploy
./start.sh
```

2. 停止服务器。进入 `deploy` 目录，执行 `stop.sh` 脚本。

```shell
cd deploy
./stop.sh
```


## 获得帮助

您可以访问 [时信魔方官网](https://www.shixincube.com/) 获得更多信息。如果您在使用 Cube 的过程中需要帮助可以发送邮件到 [cube@spap.com](mailto:cube@spap.com) 。
