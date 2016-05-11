package workflow.persistent.base;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.copperengine.core.AutoWire;
import org.copperengine.core.Interrupt;
import org.copperengine.core.Response;
import org.copperengine.core.util.Backchannel;
import org.quartz.CronExpression;
import org.springframework.scheduling.support.CronSequenceGenerator;

import workflow.data.scheduler.BaseSchedulerData;
import workflow.data.scheduler.IntruptEvent;
import workflow.data.scheduler.UpdateSchedulerRequest;
import workflow.enumeration.scheduler.SchedulerStatus;
import workflow.enumeration.scheduler.TriggerType;

import com.omniselling.common.enumeration.LanguageCode;

public abstract class BaseScheduler<D extends BaseSchedulerData> extends BasePersistentWorkflow<D>
{

    private static final long serialVersionUID = -7651364632952155680L;
    private static final Logger logger = LogManager.getLogger(BaseScheduler.class);
    protected static TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");
    protected static LanguageCode defaultLanguage = LanguageCode.EN_US;
    private transient Backchannel schedulerBackChannel;

    @AutoWire
    public void setSchedulerBackChannel(Backchannel schedulerBackChannel)
    {
        this.schedulerBackChannel = schedulerBackChannel;
    }

    @Override
    protected void runFlow() throws Interrupt
    {
        getData().setStatus(SchedulerStatus.RUNNING);
        D data = getData();
        final TriggerType triggerType = data.getTriggerType();
        final String cid = getTransactionId();
        String poolId = data.getPoolId();
        //String context = "Scheduler.runFlow";

        if (StringUtils.isNotEmpty(poolId))
        {
            setProcessorPoolId(poolId);
        }
        data.setPoolId(getProcessorPoolId());

        int priority = data.getPriority();
        if (priority > 0)
        {
            setPriority(priority);
        }
        data.setPriority(getPriority());
        data.setLastRunTime(new Date());

        //检查数据
        validateData(data);

        for (boolean canTerminate = false; !canTerminate;)
        {
            long repeatMs = evaluateNextTimeoutInterval(data, data.isNoCatchUp());
            evaluateTriggerTime(data, triggerType, repeatMs);
            List<Response<IntruptEvent>> interruptResList = waitForResponses(repeatMs, cid);
            for (Response<IntruptEvent> interruptRes : interruptResList)
            {
                //处理返回事件
                canTerminate = handleResponse(data, interruptRes);
                if (canTerminate)
                {
                    break;
                }
            }
        }

        //终止
        terminateScheduler(data);
        return;
    }

    protected boolean handleResponse(D data, Response<IntruptEvent> interruptRes)
    {
        String context = getCurrentMethodName();
        boolean canTerminate = false;
        final TriggerType triggerType = data.getTriggerType();
        try
        {
            IntruptEvent event = interruptRes.getResponse();
            //说明不是interrupt
            if (interruptRes.isTimeout())
            {
                data.setLastRunTime(new Date());

                performTask(data);

                //判断是否结束
                data.setRunCount(data.getRunCount() + 1);
                if (canTerminate(data, triggerType, data.getRunCountMax(), data.getRunCount()))
                {
                    return true;
                }

            }
            else
            {
                //control event
                String msg;
                logger.info("Received INTRUPT, type=" + event.getResultEnum() + ", operatorId="
                        + event.getProcessedBy());
                auditLog(LogLevel.INFO, context, "Received INTRUPT, type=" + event.getResultEnum() + ", operatorId="
                        + event.getProcessedBy());
                switch (event.getResultEnum())
                {
                case PAUSE:
                    //logger.info("Pausing " + getId());
                    data.setStatus(SchedulerStatus.PAUSED);
                    break;
                case RESUME:
                    if (data.getStatus() == SchedulerStatus.PAUSED)
                    {
                        logger.info("Resuming " + getId());
                        data.setStatus(SchedulerStatus.RUNNING);
                    }
                    break;
                case TERMINATE:
                    //record event
                    //logger.info("Terminating " + getId());
                    msg = "Scheduler has been terminated by control, id=" + event.getProcessedBy() + " note="
                            + event.getNote();
                    auditLog(LogLevel.INFO, context, msg);
                    return true;
                case UPDATE:
                    //logger.info("Updating config " + getId());
                    updateSchedulerConfig(data, event.getUpdateRequest());
                    break;
                case QUERY:
                    //logger.info("Receive query request " + getId() + ", senderCid=" + event.getSenderCId());
                    if (StringUtils.isEmpty(event.getSenderCId()))
                    {
                        auditLog(LogLevel.ERROR, context, "senderCid cannot be empty for query request!",
                                getTransactionId());
                        return false;
                    }
                    schedulerBackChannel.notify(event.getSenderCId(), data);
                    break;
                case RUNNOW:
                    auditLog(LogLevel.INFO, context, "Manual performTask() invoked by " + event.getProcessedBy()
                            + " note=" + event.getNote());
                    performTask(data);
                    break;
                default:
                    msg = "Wrong result " + event.getResultEnum() + " received by " + getId();
                    logger.error(msg);
                    auditLog(LogLevel.ERROR, context, msg);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            logger.error(e);
            e.printStackTrace();
            auditLog(
                    LogLevel.ERROR,
                    context,
                    "Unexpected exception handling response,id=" + interruptRes.getResponseId() + ": " + e.getMessage(),
                    getId());
        }
        return canTerminate;
    }

    /**
     * 计算下一次检查的等待时间
     * 
     * @param data
     * @param noCatchUp
     * @return
     */
    protected long evaluateNextTimeoutInterval(final D data, boolean noCatchUp)
    {
        //paused
        TriggerType triggerType = data.getTriggerType();
        if (data.getStatus() == SchedulerStatus.PAUSED)
        {
            return NO_TIMEOUT;
        }

        //RULE 触发类型完全自订规则        
        if (triggerType == TriggerType.RULE)
        {
            return customTriggerRule(data);
        }

        long resMs;
        Date lastRunTime = data.getLastRunTime();
        //判断是否第一次执行, 如果是的话检查triggerTime, 如果不为空则设为delay
        if (needWaitFirstRun(data))
        {
            resMs = (data.getTriggerTime().getTime() - System.currentTimeMillis());
        }
        else if (triggerType == TriggerType.TIME_REPEAT)
        {
            Date nextRunTime = noCatchUp ? getNextRunTime(data.getCronExp(), new Date()) : getNextRunTime(
                    data.getCronExp(), lastRunTime);
            data.setTriggerTime(nextRunTime);
            resMs = (nextRunTime.getTime() - System.currentTimeMillis());
        }
        else
        {
            //重复任务, 检查间隔 == 执行间隔
            TimeUnit timeUnit = data.getTimeUnit();
            resMs = timeUnit.toMillis(data.getCheckInterval());

            //减去上次执行用掉的时间
            long diffms = System.currentTimeMillis() - lastRunTime.getTime();
            //不追赶的话, 算出要等待的时间 
            if (noCatchUp)
            {
                resMs = -diffms % resMs + resMs;
            }
            else
            {
                resMs -= diffms;
            }
        }

        return convertIntervalMs(resMs);
    }

    private boolean needWaitFirstRun(D data)
    {
        if (data.getRunCount() == 0 && data.getTriggerTime() != null)
        {
            Date firstRunTime = data.getTriggerTime();
            //比当前时间晚的情况下是否马上追跑
            boolean runNow = !data.isNoCatchUp();
            return firstRunTime.getTime() <= System.currentTimeMillis() || runNow;
        }
        return false;

    }

    /**
     * 检查基础SchedulerData数据是否合法, 如有扩展请自己写validation
     * 
     * @param data
     * @return
     */
    protected void validateData(D data)
    {
        String context = getCurrentMethodName();

        TriggerType type = data.getTriggerType();
        if (type == null)
        {
            throwRuntimeException("TriggerType cannot be null", context);
        }
        boolean result;
        switch (type)
        {
        case INTERVAL_REPEAT:
            result = data.getTimeUnit() != null && data.getCheckInterval() >= 0;
            if (!result)
            {
                throwRuntimeException("timeUnit and checkInterval cannot be null", context);
            }

            break;
        case RULE:
            result = customValidationRule(data);
            break;
        case TIME_REPEAT:
            String cronExp = data.getCronExp();
            result = cronExp != null && CronExpression.isValidExpression(cronExp);
            if (!result)
            {
                throwRuntimeException("cronExp = [" + cronExp + "] is invalid for triggerType=TIME_REPEAT", context);
            }
            break;
        default:
            throwRuntimeException("TriggerType is invalid: " + type, context);
            break;
        }
        auditLog(LogLevel.INFO, context, "Validation completed data=" + data);
        return;
    }

    /**
     * 更新scheduler的配置
     * 
     * @param dataUpdate
     */
    protected <U extends UpdateSchedulerRequest> void updateSchedulerConfig(D data, U dataUpdate)
    {
        String context = getCurrentMethodName();
        if (dataUpdate == null)
        {
            auditLog(LogLevel.ERROR, context, "Update Scheduler config failed, updateRequest is null");
            return;
        }
        String cronExp = dataUpdate.getCronExp();
        if (data.getTriggerType() == TriggerType.TIME_REPEAT && cronExp != null)
        {
            if (!CronExpression.isValidExpression(cronExp))
            {
                auditLog(LogLevel.ERROR, context, "cronExpression is invalid: " + cronExp);
                return;
            }
            data.setCronExp(cronExp);
        }

        Integer repeatSec = dataUpdate.getRepeatInterval();
        if (repeatSec != null)
        {
            data.setCheckInterval(repeatSec.intValue());
        }
        if (dataUpdate.getTimeUnit() != null)
        {
            data.setTimeUnit(dataUpdate.getTimeUnit());
        }
        String poolId = dataUpdate.getPoolId();
        if (StringUtils.isNotEmpty(poolId))
        {
            setProcessorPoolId(poolId);
            data.setPoolId(poolId);
        }
        Integer priority = dataUpdate.getPriority();
        if (priority != null && priority.intValue() > 0)
        {
            setPriority(priority.intValue());
            data.setPriority(priority.intValue());
        }

        Integer runCountMax = dataUpdate.getRunCountMax();
        if (runCountMax != null)
        {
            data.setRunCountMax(runCountMax.intValue());
        }

        Date triggerTime = dataUpdate.getFirstRunTime();
        if (triggerTime != null && data.getRunCount() == 0)
        {
            data.setTriggerTime(triggerTime);
        }

        Boolean noCatchUp = dataUpdate.getNoCatchUp();
        if (noCatchUp != null)
        {
            data.setNoCatchUp(noCatchUp);
        }
        setData(data);
        auditLog(LogLevel.INFO, context, "Update Scheduler config success, data= " + data);
        return;
    }

    /**
     * 检查是否可以结束
     * 
     * @param triggerType
     * @param runCountMax
     * @param runCount
     * @return
     */
    protected boolean canTerminate(final D data, final TriggerType triggerType, final int runCountMax,
            final int runCount)
    {
        boolean canTerminate = false;
        if (triggerType == TriggerType.RULE)
        {
            //自定规则
            canTerminate = customTermianteRule(data);
        }
        else if (runCountMax != -1 && runCount >= runCountMax)
        {
            canTerminate = true;
        }
        return canTerminate;
    }

    /**
     * 设置下次执行时间
     * 
     * @param data
     * @param triggerType
     * @param repeatMs
     */
    protected void evaluateTriggerTime(D data, final TriggerType triggerType, final long repeatMs)
    {
        if (repeatMs == NO_TIMEOUT)
        {
            getLogger().info("Next execution time: PAUSED"); //FIXME: remove this later
            auditLog(LogLevel.INFO, getCurrentMethodName(), "Next execution time: PAUSED");
            return;
        }

        Date triggerTime = data.getTriggerTime();

        if (triggerType != TriggerType.TIME_REPEAT)
        {
            triggerTime = new Date(System.currentTimeMillis() + repeatMs);
            data.setTriggerTime(triggerTime);
        }

        getLogger().info("Next execution time: " + triggerTime);//FIXME: remove this later
        auditLog(LogLevel.INFO, getCurrentMethodName(), "Next execution time: " + data.getTriggerTime());
        return;

    }

    private long convertIntervalMs(long resMs)
    {
        if (resMs <= 0)
        {
            //run immediately
            return RUN_NOW;
        }
        else
        {
            return resMs;
        }
    }

    /**
     * Override this method to write your own validation rule
     * 
     * @param data
     * @return true if valid
     */
    protected boolean customValidationRule(D data)
    {
        throwRuntimeException("Unimplemented validation RULE for wfi=" + getId(), getCurrentMethodName(), null);
        return false;
    }

    /**
     * Override this method to write your own trigger rule
     * 
     * @param data
     * @return wait interval in ms
     */
    protected int customTriggerRule(D data)
    {
        throwRuntimeException("Unimplemented trigger RULE for wfi=" + getId(), getCurrentMethodName(), null);
        return 0;
    }

    /**
     * Override this method to write your own terminate rule
     * 
     * @param data
     * @return wait interval in ms
     */
    protected boolean customTermianteRule(D data)
    {
        throwRuntimeException("Unimplemented terminate RULE for wfi=" + getId(), getCurrentMethodName(), null);
        return false;
    }

    /**
     * Override to provide clean up on flow termiantion
     * 
     * @param data
     */
    protected void terminateScheduler(D data)
    {
        String context = getCurrentMethodName();
        //clean up
        //release resource etc...         
        auditLog(LogLevel.INFO, context, "Scheduler termianted data= " + data);
        return;
    }

    protected Date getNextRunTime(String cronExp, Date lastRunTime)
    {
        final CronSequenceGenerator generator = new CronSequenceGenerator(cronExp, DEFAULT_TIME_ZONE);
        return generator.next(lastRunTime);
    }

    protected abstract void performTask(D data);

    @Override
    protected String getTransactionId()
    {
        return getId();
    }

}
