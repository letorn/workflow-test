package workflow.data.leave.event;

public enum WaitEventEnum
{
    ASK(ResultEnum.AGREE, ResultEnum.DISAGREE);
    private ResultEnum[] allowedResponses;

    WaitEventEnum(ResultEnum... responseEnums)
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
