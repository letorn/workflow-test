package workflow.enumeration.scheduler;

public enum TriggerType
{
    /**
     * 一段时间间隔后触发,如果当前时间距离上次执行时间已经超过了定义的间隔则会马上执行
     */
    INTERVAL_REPEAT,

    /**
     * 根据自定义的cronExp来执行任务
     */
    TIME_REPEAT,

    /**
     * 自定规则触发, 需要扩展Scheduler接口
     */
    RULE,
}
