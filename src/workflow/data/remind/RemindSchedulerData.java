package workflow.data.remind;

import java.util.concurrent.TimeUnit;

import workflow.data.scheduler.BaseSchedulerData;
import workflow.enumeration.scheduler.TriggerType;

public class RemindSchedulerData extends BaseSchedulerData
{
    private static final long serialVersionUID = 314596021147941568L;

    private String businessNum;

    public RemindSchedulerData(String cronExp)
    {
        super(cronExp);
    }

    public RemindSchedulerData(int repeatInterval, TimeUnit timeUnit, String poolId, int priority)
    {
        super(repeatInterval, timeUnit, poolId, priority);
    }

    public RemindSchedulerData(int repeatInterval, TimeUnit timeUnit)
    {
        super(repeatInterval, timeUnit);
    }

    public RemindSchedulerData(TriggerType triggerType, int repeatInterval, TimeUnit timeUnit, String poolId,
            int priority)
    {
        super(triggerType, repeatInterval, timeUnit, poolId, priority);
    }

    public String getBusinessNum()
    {
        return businessNum;
    }

    public void setBusinessNum(String businessNum)
    {
        this.businessNum = businessNum;
    }

}
