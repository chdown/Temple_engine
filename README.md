## Template模板生成

快速生成各种代码模板，模板文件位于【设置】-> 插件配置目录下的【`templates`】中

### 1、前置知识

【`templates`】中我们的创建的模板为两层目录

* **第一层**：模板类型，如`Android`、`Flutter`...
* **第二层**：具体的模板，其中包含以下三种文件
  1. 【`select`目录】：在Idea中选择的路径，其下面的模板文件生成在该目录
  2. 【`match`目录】：在整个项目中进行目录匹配，写法为【`res-layout`】,最终文件会生成在匹配的多级目录下
  3. 【`script.json`文件】：通常用于模板创建完成后，执行的一些注册操作

### 2、模板文件

模板格式为 `#{name.xxx}`,其中`name`为我们需要用户自定义的名称，内置语法如下：

| 模板                                              | 说明                                              | 输入或选择       | 结果              |
|-------------------------------------------------| ------------------------------------------------- | ---------------- | ----------------- |
| `#{name.l}`                                     | 转驼峰，首字母小写                                | `UserPage`       | `userPage`        |
| `#{name.ls}`                                    | 转下换线连接                                      | `UserPage`       | `user_page`       |
| `#{name.u}`                                     | 转驼峰，首字母大写                                | `user_page`      | `UserPage`        |
| `#{name.replace(Page,Controller)`               | 替换操作                                          | `UserPage`       | `UserController`  |
| `#{name.subString(4)`                           | 分割取字符串操作                                  | `UserPage`       | `Page`            |
| `#{name.subString(4,5)`                         | 分割取字符串操作                                  | `UserPage`       | `P`               |
| `#{packageName}`                                | 包名，仅支持`Android`、`Dart`                     | /                | `con.xx.xx`       |
| `#{file.p}`                                     | 导包，`file`为完全文件名，仅支持`Android`、`Dart` | /                | `con.xx.xx.xx.xx` |
| `#{name.if(true)}`、`#{name.ifEnd(true)}`、       | `if`用户选择正确的内容生效                        | `true`、`false`  | 对应包裹住的内容  |
| `#{name.switch(xxx)}`、`#{name.switchEnd(xxx)}`、 | `switch`用户选择正确的内容生效                    | `xxx`、`xx`、`x` | 对应包裹住的内容  |

### 3、script.json

* `path`：相对project目录地址及
* `oldText`：被替换文件
* `text`：替换文本

![](https://github.com/chdown/ihs/blob/master/plugin/toolbox/4ujD6w8ilkZEfzK.png?raw=true)
