package workflow.persistent.leave;

import org.copperengine.core.AutoWire;
import org.copperengine.core.Interrupt;
import org.copperengine.core.Response;
import org.copperengine.core.WorkflowDescription;

import workflow.adapter.leave.LeaveAdapter;
import workflow.data.base.WorkflowTerminationException;
import workflow.data.leave.EventResponse;
import workflow.data.leave.LeaveWorkflowData;
import workflow.data.leave.event.ResultEnum;
import workflow.data.leave.event.WaitEventEnum;
import workflow.definition.WorkflowDef;
import workflow.persistent.base.BasePersistentWorkflow;

@WorkflowDescription(alias = WorkflowDef.LEAVE_WORKFLOW, majorVersion = 1, minorVersion = 0, patchLevelVersion = 0)
public class LeaveWorkflow extends BasePersistentWorkflow<LeaveWorkflowData>
{
    private static final long serialVersionUID = 5835141061964392352L;

    private transient LeaveAdapter leaveAdapter;

    @AutoWire
    public void setLeaveAdapter(LeaveAdapter leaveAdapter)
    {
        this.leaveAdapter = leaveAdapter;
    }

    @Override
    protected void runFlow() throws Interrupt
    {
        LeaveWorkflowData workflowData = getData();
        try
        {
            askForLeave(workflowData);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected String getTransactionId()
    {
        return getId();
    }

    protected void askForLeave(LeaveWorkflowData workflowData) throws WorkflowTerminationException, Interrupt
    {
        WaitEventEnum stepEvent = WaitEventEnum.ASK;
        Response<EventResponse> res = createAndWaitEvent(workflowData.getBusinessNum(), NO_TIMEOUT, stepEvent,
                workflowData.getBusinessNum());

        EventResponse eventResponse = res.getResponse();
        ResultEnum resultEnum = eventResponse.getResultEnum();

        switch (resultEnum)
        {
        case AGREE:
            getLogger().info("Something Is Happying");
            break;
        case DISAGREE:
            getLogger().info("Something Is Sadding");
        }
    }

    /**
     * 
     * @param businessNum
     * @param timeoutMs
     *            - use -1 to indicate NO_TIMEOUT
     * @param event
     * @param correlationId
     *            optional
     * @return
     * @throws Interrupt
     */
    private <R extends EventResponse> Response<R> createAndWaitEvent(String businessNum, int timeoutSec,
            WaitEventEnum event, String correlationId) throws Interrupt
    {
        String cId = leaveAdapter.createEvent(businessNum, getId(), event, correlationId);
        return waitForResponse(timeoutSec * 1000, cId);
    }

}
