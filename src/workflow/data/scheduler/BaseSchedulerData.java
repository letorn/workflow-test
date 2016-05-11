package workflow.data.scheduler;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import workflow.data.base.BaseWorkflowData;
import workflow.enumeration.scheduler.SchedulerStatus;
import workflow.enumeration.scheduler.TriggerType;

public class BaseSchedulerData extends BaseWorkflowData
{
    private static final long serialVersionUID = 996110103988881630L;

    /**
     * 任务触发类型, 参照{@link TriggerType}
     */
    final private TriggerType triggerType;

    //    /**
    //     * 第一次运行需要等待的时间,如果为空就直接等repeatInterval+timeunit 好像没什么卵用
    //     */
    //    private int firstRunDelaySec;
    /**
     * 检查是否可运行任务的等待时间
     */
    private int checkInterval;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 当前执行次数
     */
    private int runCount = 0;

    /**
     * 总执行次数, -1 就会永远重复下去
     */
    private int runCountMax = -1;

    /**
     * TIME_REPEAT 使用兼容的cron expression
     */
    private String cronExp;

    /**
     * 下次执行任务的时间点, 如果当前时间大于此时间将执行任务
     */
    private Date triggerTime;
    private Date lastRunTime;

    /**
     * specific poolId to run on, if null, will run on default pool
     */
    private String poolId;
    /**
     * if>0, will change to such priority on start
     */
    private int priority = -1;

    private SchedulerStatus status;

    /**
     * 如果设为true则不会在当前时间比下次执行时间晚的情况下立即触发任务 例子: 上次触发时间是8:30分, 设定是每30分钟触发一次, 然后因为系统维护, 启动的时候当然已经9:10了
     * 如果noCatchUp=true则下次执行时间是9:30, 如果false则马上会执行
     */
    private boolean noCatchUp = false;

    /**
     * TriggerType=REPEAT_INTERVAL Scheduler on default pool with default priority and default everything:
     * triggerType=REPEAT_INTERVAL, runCountMax=-1
     * 
     * @param repeatInterval
     * @param timeUnit
     */
    public BaseSchedulerData(int repeatInterval, TimeUnit timeUnit)
    {
        this(TriggerType.INTERVAL_REPEAT, repeatInterval, timeUnit, null, -1);
        this.timeUnit = timeUnit;
        this.checkInterval = repeatInterval;
    }

    /**
     * TriggerType=REPEAT_INTERVAL Scheduler on specified pool and priority, runCountMax default to -1 (unlimited)
     * 
     * @param repeatInterval
     * @param timeUnit
     * @param poolId
     * @param priority
     */
    public BaseSchedulerData(int repeatInterval, TimeUnit timeUnit, String poolId, int priority)
    {
        this(TriggerType.INTERVAL_REPEAT, repeatInterval, timeUnit, poolId, priority);
        this.timeUnit = timeUnit;
        this.checkInterval = repeatInterval;
    }

    /**
     * TriggerType=TIME_REPEAT on default pool with default priority
     * 
     * @cronExp linux cron expression
     * @param timeUnit
     * @param repeatIntervalSec
     */
    public BaseSchedulerData(String cronExp)
    {
        this(TriggerType.TIME_REPEAT, 0, null, cronExp, -1);
        this.cronExp = cronExp;
    }

    /**
     * Create a scheduler with specific triggerType, pool and priority, <b>don't forget to set corresponding
     * properties</b>
     * 
     * @param triggerType
     * @param repeatInterval
     * @param timeUnit
     * @param poolId
     * @param priority
     */
    public BaseSchedulerData(TriggerType triggerType, int repeatInterval, TimeUnit timeUnit, String poolId, int priority)
    {
        this.triggerType = triggerType;
        this.timeUnit = timeUnit;
        this.checkInterval = repeatInterval;
        this.poolId = poolId;
        this.priority = priority;
        this.status = SchedulerStatus.STARING;
    }

    public String getPoolId()
    {
        return poolId;
    }

    public void setPoolId(String poolId)
    {
        this.poolId = poolId;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public SchedulerStatus getStatus()
    {
        return status;
    }

    public void setStatus(SchedulerStatus status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return "SchedulerData [triggerType=" + triggerType + ", checkInterval=" + checkInterval + ", timeUnit="
                + timeUnit + ", runCount=" + runCount + ", runCountMax=" + runCountMax + ", triggerTime=" + triggerTime
                + ", poolId=" + poolId + ", priority=" + priority + ", status=" + status + "]";
    }

    public TriggerType getTriggerType()
    {
        return triggerType;
    }

    public TimeUnit getTimeUnit()
    {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
    }

    public int getCheckInterval()
    {
        return checkInterval;
    }

    public void setCheckInterval(int repeatInterval)
    {
        this.checkInterval = repeatInterval;
    }

    public Date getTriggerTime()
    {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime)
    {
        this.triggerTime = triggerTime;
    }

    public int getRunCount()
    {
        return runCount;
    }

    public void setRunCount(int runCount)
    {
        this.runCount = runCount;
    }

    public int getRunCountMax()
    {
        return runCountMax;
    }

    public void setRunCountMax(int runCountMax)
    {
        this.runCountMax = runCountMax;
    }

    public String getCronExp()
    {
        return cronExp;
    }

    public void setCronExp(String cronExp)
    {
        this.cronExp = cronExp;
    }

    public boolean isNoCatchUp()
    {
        return noCatchUp;
    }

    public void setNoCatchUp(boolean noCatchUp)
    {
        this.noCatchUp = noCatchUp;
    }

    public Date getLastRunTime()
    {
        return lastRunTime;
    }

    public void setLastRunTime(Date lastRunTime)
    {
        this.lastRunTime = lastRunTime;
    }

}
