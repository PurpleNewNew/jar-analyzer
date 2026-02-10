## 其他资料与原理说明

### 外部资料（历史参考）

以下内容主要用于了解“别人怎么用 jar-analyzer 做审计”，部分 UI/流程可能与当前版本不同：

- [利用 jar-analyzer 分析 CVE-2022-42889](https://www.bilibili.com/video/BV1a94y1E7Nn)
- [某人力系统的代码审计](https://forum.butian.net/share/3109)

### 为什么用 Jar Analyzer

一些典型场景下，IDE 不是最合适的入口：

- 目标只有 `jar/war/classes`，没有源码，且需要跨模块/跨依赖追踪
- 希望把“建库后的数据”沉淀下来，反复查询/DFS/污点，而不是每次从头分析
- 需要对字节码做进一步分析（指令、CFG、Frame 等）

(1) 什么是方法之间的关系

```java
class Test{
    void a(){
        new Test().b();
    }
    
    void b(){
        Test.c();
    }
    
    static void c(){
        // code
    }
}
```

如果当前方法是 `b`

对于 `a` 来说，它的 `callee` 是 `b`

对于 `b` 来说，它的 `caller` 是 `a`

(2) 如何解决接口实现的问题

```java
class Demo{
    void demo(){
        new Test().test();
    }
}

interface Test {
    void test();
}

class Test1Impl implements Test {
    @Override
    public void test() {
        // code
    }
}

class Test2Impl implements Test {
    @Override
    public void test() {
        // code
    }
}
```

现在我们有 `Demo.demo -> Test.test` 数据, 但实际上它是 `Demo.demo -> TestImpl.test`.

静态分析中，接口/虚调用的实际落点需要“分派补全”。一种常见做法是：

- 保留原始调用边：`Demo.demo -> Test.test`
- 再补充分派边：`Test.test -> Test1Impl.test`、`Test.test -> Test2Impl.test`

首先确保数据不会丢失，然后我们可以自行手动分析反编译的代码
- `Demo.demo -> Test.test`
- `Test.test -> Test1Impl.test`/`Test.test -> Test2Impl.test`

(3) 如何解决继承关系

```java
class Zoo{
    void run(){
        Animal dog = new Dog();
        dog.eat();
    }
}

class Animal {
    void eat() {
        // code
    }
}

class Dog extends Animal {
    @Override
    void eat() {
        // code
    }
}

class Cat extends Animal {
    @Override
    void eat() {
        // code
    }
}
```
`Zoo.run -> dog.eat` 的字节码可能是 `INVOKEVIRTUAL Animal.eat ()V`。如果只记录“声明类型”上的调用边：

- `Zoo.run -> Animal.eat`

会丢失对实际实现的可达性（`Dog.eat` / `Cat.eat`）。

因此会补充 override 分派边：

- `Animal.eat -> Dog.eat`
- `Animal.eat -> Cat.eat`

首先确保数据不会丢失，然后我们可以自行手动分析反编译的代码
- `Zoo.run -> Animal.eat`
- `Animal.eat -> Dog.eat`/`Animal.eat -> Cat.eat`

说明：

- 这类补边通常是“保守的”（宁可多一些边，也尽量不漏掉可能的落点），因此可能带来一定误报。
- 实际审计建议结合：类/包过滤、scope（只看 app）、以及后续的 DFS/污点验证来降噪。
