## 1.Java代码怎样运行？

### 1.1 虚拟机
Java语言编译为字节码在JVM上运行。
字节码：Java 字节码指令的操作码（opcode）被固定为一个字节。

### 1.2内存划分
> JDK 1.8之前划分
方法区、堆、PC寄存器、Java方法栈和本地方法栈。Java程序编译而成的class文件，
需要先加载到方法区中，方能在Java虚拟机中运行。


### 1.3 解释
从硬件视角，Java字节码无法直接运行，JVM需要将字节码翻译成机器码。
解释执行：逐条将字节码翻译成机器码并执行。
即时编译（JIT：Just-In-Time compilation）：将方法中包含的所有字节码编译成机器码后再执行。

##### 混合执行
解释执行Java字节码，然后将其中反复执行的热点代码，以方法为单位进行即时编译，翻译成机器码后直接运行在底层硬件之上。

##### 即时编译器
HotSpot内置了多个即时编译器：C1, C2 和 Graal（Java10引入实现性编译器）
- C1：Client编译器。面向对启动性能有要求的客户端GUI程序，采用优化手段相对简单，编译时间较短。
- C2：Server编译器。面向对峰值性能要求高的服务端程序，采用优化手段相对复杂，编译时间长，生成代码执行效率高。

##### 分层编译
Java7开始，HotSpot默认采用分层编译：热点首先被C1编译，然后热点中的热点被C2进一步编译。

HotSpot即时编译在额外编译线程中进行。HotSpot会根据CPU的数量设置编译线程数目，按照1：2的比例配置给C1和C2.


## 2.Java的基础类型
Java包含8中基本类型：


boolean 在Java虚拟机中：true映射为1， false映射为0。

### 2.1 NaN
除了“!=”始终返回 true 之外，所有其他比较结果都会返回 false。 举例来说，“NaN<1.0F”返回 false，而“NaN>=1.0F”同样返回 false。对于任意浮点数 f，不管它是 0 还是 NaN，“f!=NaN”始终会返回 true，而“f==NaN”始终会返回 false。

正无穷：0x7F800000
负无穷：0xFF800000
NaN(Not-a-Number): [0x7F800001, 0x7FFFFFFF]   [0xFF800001, 0xFFFFFFFF]

### 2.2 栈帧
Java虚拟机每调用一个方法，便会创建一个栈帧。

解释栈帧(interpreted frame)：
解释器使用，两个组成部分：
- 局部变量区；
- 字节码操作数栈。

在JVM规范中，局部变量区等价于一个数组，可以用正整数来索引。除了long、double值需要用两个数组
单元来存储之外，其他基本类型以及引用类型值均占用一个数组单元。

在局部变量中，boolean、byte、char、short在栈上占用的空间和int 及 引用类型是一样的。在32位Hotspot中，这些类型在
栈上占用4个字节；在64位HotSpot上占用8个字节。

除long和double外，其他基本类型与引用类型在解释执行的方法栈帧中占用的大小是一致的，但是在对中大小不同。
在将boolean、byte、char、short的值存入字段或数组单元时，Java虚拟机会进行掩码操作。在读取时，Java虚拟机会将其扩展为int；类型。


## 3. JVM加载类
java类型分为两大类：
1. 基本类型（primitive types）：
2. 引用类型（reference types）：类，接口，数组类，泛型参数。
    泛型在编译过程中会被擦除。因此实际只有前三种。数组类由JVM直接生成，其他两种有对应字节流。

分为：加载，链接，初始化。
### 3.1 加载
查找字节流，并且据此创建类的过程。

双亲委派模型(Parents Delegation Model)
好处
java类随着类加载器具备优先级的层次关系。

一个类加载器接收到加载请求时，它会先将请求转发给父类加载器。在父类加载器没有找所请求的类的情况下，该类加载器才会尝试去加载。
1. 启动类加载器（bootstrap loader）：
由C++实现，是虚拟机自身一部分。
JRE 的 lib目录下 jar 包中的类（以及由虚拟机参数 -Xbootclasspath 指定的类）。
2. 扩展类加载器（extension class loader）：
JRE 的 lib/ext 目录下 jar 包中的类（以及由系统变量 java.ext.dirs 指定的类）
3. 应用类加载器（app class loader）：
它负责加载应用程序路径下的类。（这里 程序路径，便是指虚拟机参数 -cp/-classpath、系统变量 java.class.path 或环境变量 CLASSPATH 所指定的路径。）默认情况下，应用程序中包含的类便是由应用类加载器加载的。
 自定义加载类场景：
（1）隔离加载类。
（2）修改类的加载方式。
（3）扩展加载源。从数据库、网络、电视机顶盒加载
（4）防止源码泄露。对class文件加密，加载时利用自己定义加载器解密。t

java 9 变化
java9引入入了模块系统，并且略微更改了上述的类加载器1。扩展类加载器被改名为平台加载器（platform class loader）。Java SE 中除了少数几个关键模块，比如说 java.base 是 类加载器加载之外，其他的模块均由平台类加载器所加载。

### 3.2 链接
1. 验证
确保加载类满足Java虚拟机的约束条件。

2. 准备
为加载的静态字段分配内存。构造其他跟类层次相关的数据结构：比如动态绑定的方法表（实现虚方法）。

3. 解析
非必须。

将符号引用（目标方法所在类名字、目标方法名字、接受参数类型、返回值类型的引用）解析为实际引用。
如果符号引用指向未被加载的类 或 未被记载类的字段或方法，将触发类的加载。

### 3.3 初始化
常量值(ConstantValue):如果直接赋值的静态字段被 final 所修饰，并且它的类型是基本类型或字符串时，那么该字 会被 Java 编译器标记成常量值（ConstantValue），其初始化直接由 Java 虚拟机完成。
<clinit>:除上述之外的直接赋值操作，以及所有静态代码块中的代码，则会被 Java 编译器置于同一方法中 它命名为 < clinit >。

为标记为常量值的字段赋值，以及执行 < clinit > 方法 程。Java 虚拟机会通过加锁来确保类的 < clinit > 方法仅被执行一次。

初始化触发情况：
1. 当虚拟机启动时，初始化用户指定的主类；
2. 当遇到用以新建目标类实例的 new 指令时，初始化 new 指令的目标类；
3. 当遇到调用静态方法的指令时，初始化该静态方法所在的类；
4. 当遇到访问静态字段的指令时，初始化该静态字段所在的类；
5. 子类的初始化会触发父类的初始化；
6. 如果一个接口定义了 default 方法，那么直接实现或者间接实现该接口的类的初始化， 该接口的初始化；
7. 使用反射 API 对某个类进行反射调用时，初始化这个类；
8. 当初次调用 MethodHandle 实例时，初始化该 MethodHandle 指向的方法所在的类。