package workflow.data.scheduler;

import java.io.Serializable;
import java.util.Date;

import workflow.enumeration.scheduler.ResultEnum;
import workflow.enumeration.scheduler.SchedulerEventEnum;

/**
 * 用于回复的时候返回一个事件的处理结果
 * 
 * @author Atomic
 *
 */
public class IntruptEvent implements Serializable
{

    private static final long serialVersionUID = -7287331560952332081L;
    /**
     * 事件id
     */
    private Long id;
    /**
     * 对应事件的关联ID(correlationId), 应该跟跟等待事件的关联ID一致
     */
    private final String correlationId;

    /**
     * 回复结果的枚举, 应该是waitEventEnum所允许的结果
     */
    private final ResultEnum resultEnum;

    /**
     * 处理人的accountId
     */
    final private Long processedBy;

    /**
     * 发送此回复的对象的关联ID,sender正在等待此ID, 可以为空 optional
     */
    private String senderCId;
    /**
     * 发送此回复的对象(sender)的关联Event的id,sender正在等待此事件的回复, 可以为空 optional
     */
    private Long senderEventId;

    /**
     * 所等待的事件枚举
     */
    private SchedulerEventEnum waitEventEnum;
    /**
     * 处理的时间
     */
    private Date processedTime;
    /**
     * 额外的处理说明,可以为空
     */
    private String note;

    /**
     * Used when ResultEnum = ResultEnum.UPDATE
     */
    private UpdateSchedulerRequest updateRequest;

    public IntruptEvent(String correlationId, ResultEnum resultEnum, Long processedBy)
    {
        super();
        this.correlationId = correlationId;
        this.resultEnum = resultEnum;
        this.processedBy = processedBy;
    }

    public String getCorrelationId()
    {
        return correlationId;
    }

    public ResultEnum getResultEnum()
    {
        return resultEnum;
    }

    public Date getProcessedTime()
    {
        return processedTime;
    }

    public void setProcessedTime(Date timeStamp)
    {
        this.processedTime = timeStamp;
    }

    public Long getProcessedBy()
    {
        return processedBy;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getSenderCId()
    {
        return senderCId;
    }

    public void setSenderCId(String replyCId)
    {
        this.senderCId = replyCId;
    }

    public Long getSenderEventId()
    {
        return senderEventId;
    }

    public void setSenderEventId(Long senderEventId)
    {
        this.senderEventId = senderEventId;
    }

    @Override
    public String toString()
    {
        return "IntruptEvent [id=" + id + ", correlationId=" + correlationId + ", senderCId=" + senderCId
                + ", senderEventId=" + senderEventId + ", waitEventEnum=" + waitEventEnum + ", resultEnum="
                + resultEnum + ", processedTime=" + processedTime + ", processedBy=" + processedBy + ", note=" + note
                + ", updateRequest=" + updateRequest + "]";
    }

    public UpdateSchedulerRequest getUpdateRequest()
    {
        return updateRequest;
    }

    public void setUpdateRequest(UpdateSchedulerRequest updateRequest)
    {
        this.updateRequest = updateRequest;
    }

}
