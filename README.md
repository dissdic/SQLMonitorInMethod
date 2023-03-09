# 说明书
## 有什么用
在实际的工作当中，有时会出现，在开发测试环境代码运行流畅，但是到了生产环境运行耗时很长的情况。

大概率是数据量的差异导致，但是种种原因，日志中定位不到具体的SQL，或者拿不到他具体的执行时长，数据库的慢查询记录又没开启或者太多难以定位，此时此插件可以派作用场

## 怎么用
步骤一：在需要监控的方法上加一个注解

步骤二：监控的方法在运行完了后会触发一个事件，所有的监控信息都在这个事件存的对象里，所以需要注册一个事件监听器来获取跟处理这些监控信息
## 实例代码
```
//添加注解
@MonitorSQLInMethod
public void methodName(...){}

//注册事件监听器
@Component
class YouListener implements ApplicationListener<MonitorSQLInMethodEvent> {
    @Override
    public void onApplicationEvent(MonitorSQLInMethodEvent event) {
        MonitorSQLInMethodFactor factor = (MonitorSQLInMethodFactor)event.getSource();
        //获取和处理数据
    }
}
```

# GET STARTED
## what's it
This is a simply used middleware for monitoring the basic statistics like SQL,executed duration,executed time of queries executed in a method's invocation process.
## how to use
Step 1: Just add an annotation on the method you want to monitor

Step 2: An application event will be published after the method which is monitored finish invoking, in order to obtain and process the statistics,just register a event listener to do so.
## Example
```
//add the annotation
@MonitorSQLInMethod
public void methodName(...){}

//register the event listener
@Component
class YouListener implements ApplicationListener<MonitorSQLInMethodEvent> {
    @Override
    public void onApplicationEvent(MonitorSQLInMethodEvent event) {
        MonitorSQLInMethodFactor factor = (MonitorSQLInMethodFactor)event.getSource();
        //process the MonitorSQLInMethodEvent instance
    }
}
```
