package workflow.receiver.leave;

import workflow.data.leave.EventResponse;

public interface LeaveRequestReceiver
{
    public void approveRequest(String cid, EventResponse reply);
}
