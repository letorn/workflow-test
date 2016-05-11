package controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.copperengine.core.PersistentProcessingEngine;
import org.copperengine.core.WorkflowInstanceDescr;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import workflow.data.remind.RemindSchedulerData;
import workflow.definition.SchedulerDef;

@Controller
@RequestMapping("remind/")
public class RemindController
{

    @Resource
    private PersistentProcessingEngine engine;

    @RequestMapping("setupremind.do")
    @ResponseBody
    public Map<String, Object> setupRemind()
    {
        Map<String, Object> dataMap = new HashMap<String, Object>();

        String workflow = SchedulerDef.REMIND_SCHEDULER;
        RemindSchedulerData data = new RemindSchedulerData(3000, TimeUnit.MILLISECONDS);
        data.setRunCountMax(3);
        WorkflowInstanceDescr<RemindSchedulerData> instanceDescr = new WorkflowInstanceDescr<>(workflow, data, null,
                null, null, null);

        try
        {
            engine.run(instanceDescr);
        }
        catch (Exception e)
        {
            dataMap.put("success", false);
            e.printStackTrace();
        }

        dataMap.put("success", true);
        return dataMap;
    }

}
