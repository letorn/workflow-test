package workflow.adapter.leave;

import workflow.data.leave.event.WaitEventEnum;

public interface LeaveAdapter
{

    String createEvent(String businessNum, String workflowId, WaitEventEnum event, String correlationId);
}
