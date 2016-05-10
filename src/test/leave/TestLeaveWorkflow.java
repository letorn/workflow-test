package test.leave;

import org.copperengine.core.PersistentProcessingEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import workflow.data.leave.EventResponse;
import workflow.data.leave.LeaveWorkflowData;
import workflow.data.leave.event.ResultEnum;
import workflow.data.leave.event.WaitEventEnum;
import workflow.definition.WorkflowDef;
import workflow.receiver.leave.LeaveRequestReceiver;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring-context.xml"/*, "/spring-workflow.xml"*/})
public class TestLeaveWorkflow
{

    @Autowired
    PersistentProcessingEngine engine;
    @Autowired()
    private LeaveRequestReceiver receiver;

    @Test
    public void askForLeave() throws Exception
    {
        String wfName = WorkflowDef.LEAVE_WORKFLOW;
        LeaveWorkflowData data = new LeaveWorkflowData();
        engine.run(wfName, data);
        waitting();
    }

    @Test
    public void approveRequest() throws Exception
    {
        String correlationId = "834846c4-84e4-4cdb-84bc-0502722efc7d";
        EventResponse eventResponse = new EventResponse();
        eventResponse.setCorrelationId(correlationId);
        eventResponse.setWaitEventEnum(WaitEventEnum.ASK);
        eventResponse.setResultEnum(ResultEnum.AGREE);

        receiver.approveRequest(correlationId, eventResponse);
    }

    private void waitting()
    {
        Boolean sync = true;
        synchronized (sync)
        {
            try
            {
                sync.wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

}
