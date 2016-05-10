package controller;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.copperengine.core.PersistentProcessingEngine;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import workflow.data.leave.EventResponse;
import workflow.data.leave.LeaveWorkflowData;
import workflow.data.leave.event.ResultEnum;
import workflow.data.leave.event.WaitEventEnum;
import workflow.definition.WorkflowDef;
import workflow.receiver.leave.LeaveRequestReceiver;

@Controller
@RequestMapping("leave/")
public class LeaveController
{

    @Resource
    private PersistentProcessingEngine engine;
    @Resource
    private LeaveRequestReceiver receiver;

    @RequestMapping("askforleave.do")
    @ResponseBody
    public Map<String, Object> askForLeave()
    {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        String wfName = WorkflowDef.LEAVE_WORKFLOW;
        LeaveWorkflowData data = new LeaveWorkflowData();
        data.setBusinessNum("test...");
        try
        {
            engine.run(wfName, data);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        dataMap.put("success", true);
        return dataMap;
    }

    @RequestMapping("approverequest.do")
    @ResponseBody
    public Map<String, Object> approveRequest()
    {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        String correlationId = "test...";
        EventResponse eventResponse = new EventResponse();
        eventResponse.setCorrelationId(correlationId);
        eventResponse.setWaitEventEnum(WaitEventEnum.ASK);
        eventResponse.setResultEnum(ResultEnum.AGREE);
        receiver.approveRequest(correlationId, eventResponse);

        dataMap.put("success", true);
        return dataMap;
    }
}
