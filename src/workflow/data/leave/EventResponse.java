package workflow.data.leave;

import java.io.Serializable;

import workflow.data.leave.event.ResultEnum;
import workflow.data.leave.event.WaitEventEnum;

/**
 * 用于回复的时候返回一个事件的处理结果
 * 
 * @author weitangli
 *
 */
public class EventResponse implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -4763655776191813250L;

    /**
     * 对应事件的关联ID(correlationId), 应该跟跟等待事件的关联ID一致
     */
    private String correlationId;

    /**
     * 所等待的事件枚举
     */
    private WaitEventEnum waitEventEnum;
    /**
     * 回复结果的枚举, 应该是waitEventEnum所允许的结果
     */
    private ResultEnum resultEnum;

    public String getCorrelationId()
    {
        return correlationId;
    }

    public void setCorrelationId(String correlationId)
    {
        this.correlationId = correlationId;
    }

    public WaitEventEnum getWaitEventEnum()
    {
        return waitEventEnum;
    }

    public void setWaitEventEnum(WaitEventEnum waitEventEnum)
    {
        this.waitEventEnum = waitEventEnum;
    }

    public ResultEnum getResultEnum()
    {
        return resultEnum;
    }

    public void setResultEnum(ResultEnum resultEnum)
    {
        this.resultEnum = resultEnum;
    }

}
