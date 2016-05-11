package workflow.enumeration.scheduler;

public enum SchedulerEventEnum
{
    INTRUPT(ResultEnum.PAUSE, ResultEnum.RESUME, ResultEnum.TERMINATE, ResultEnum.UPDATE, ResultEnum.QUERY,
            ResultEnum.RUNNOW);
    private ResultEnum[] allowedResponses;

    SchedulerEventEnum(ResultEnum... responseEnums)
    {
        allowedResponses = responseEnums;
    }

    public ResultEnum[] getAllowedResponses()
    {
        return allowedResponses;
    }

    public boolean isAllowed(ResultEnum checkEnum)
    {
        if (allowedResponses == null)
        {
            return false;
        }

        for (ResultEnum response : allowedResponses)
        {
            if (response == checkEnum)
            {
                return true;
            }
        }
        return false;
    }

}
