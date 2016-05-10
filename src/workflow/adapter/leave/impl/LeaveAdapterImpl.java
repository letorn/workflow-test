package workflow.adapter.leave.impl;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import workflow.adapter.base.BaseWorkflowAdapterImpl;
import workflow.adapter.leave.LeaveAdapter;
import workflow.data.leave.EventResponse;
import workflow.data.leave.event.WaitEventEnum;
import workflow.receiver.leave.LeaveRequestReceiver;

@Service("leaveAdapter")
public class LeaveAdapterImpl extends BaseWorkflowAdapterImpl implements LeaveAdapter, LeaveRequestReceiver
{
    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public String createEvent(String businessNum, String workflowId, WaitEventEnum event, String correlationId)
    {
        correlationId = correlationId == null ? createUUID() : correlationId;
        //TODO create an event and send message
        logger.info("TODO create an event and send message, event=" + event + ", wfId=" + workflowId + ", businessNum="
                + businessNum + ", cid=" + correlationId);
        return correlationId;
    }

    @Override
    public void approveRequest(String cid, EventResponse reply)
    {
        notifyEngineSync(reply.getCorrelationId(), reply);
    }

}
