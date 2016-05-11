package workflow.data.scheduler;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import workflow.data.base.BaseWorkflowData;

public class UpdateSchedulerRequest extends BaseWorkflowData
{
    private static final long serialVersionUID = 99346103988881630L;

    private Integer repeatInterval;
    private TimeUnit timeUnit;
    /**
     * 首次执行时间, 如果已经执行过了就没有作用
     */
    private Date firstRunTime;
    /**
     * TIME_REPEAT 类型更新的cron expression
     */
    private String cronExp;
    /**
     * specific poolId to run on, if null, will run on default pool
     */
    private String poolId;
    /**
     * if>0, will change to such priority on start
     */
    private Integer priority;
    private Integer runCountMax;
    private Boolean noCatchUp;

    public String getPoolId()
    {
        return poolId;
    }

    public void setPoolId(String poolId)
    {
        this.poolId = poolId;
    }

    @Override
    public String toString()
    {
        return "UpdateSchedulerRequest [repeatInterval=" + repeatInterval + ", timeUnit=" + timeUnit
                + ", firstRunTime=" + firstRunTime + ", cronExp=" + cronExp + ", poolId=" + poolId + ", priority="
                + priority + ", runCountMax=" + runCountMax + ", noCatchUp=" + noCatchUp + "]";
    }

    public TimeUnit getTimeUnit()
    {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
    }

    public Integer getRepeatInterval()
    {
        return repeatInterval;
    }

    public void setRepeatInterval(Integer repeatInterval)
    {
        this.repeatInterval = repeatInterval;
    }

    public Date getFirstRunTime()
    {
        return firstRunTime;
    }

    public void setFirstRunTime(Date firstRunTime)
    {
        this.firstRunTime = firstRunTime;
    }

    public String getCronExp()
    {
        return cronExp;
    }

    public void setCronExp(String cronExp)
    {
        this.cronExp = cronExp;
    }

    public Integer getPriority()
    {
        return priority;
    }

    public void setPriority(Integer priority)
    {
        this.priority = priority;
    }

    public Integer getRunCountMax()
    {
        return runCountMax;
    }

    public void setRunCountMax(Integer runCountMax)
    {
        this.runCountMax = runCountMax;
    }

    public Boolean getNoCatchUp()
    {
        return noCatchUp;
    }

    public void setNoCatchUp(Boolean noCatchUp)
    {
        this.noCatchUp = noCatchUp;
    }

}
