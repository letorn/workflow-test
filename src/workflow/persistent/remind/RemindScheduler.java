package workflow.persistent.remind;

import org.copperengine.core.WorkflowDescription;

import workflow.data.remind.RemindSchedulerData;
import workflow.definition.SchedulerDef;
import workflow.persistent.base.BaseScheduler;

/**
 * 定时任务-提醒
 * 
 * @author weitangli
 *
 */
@WorkflowDescription(alias = SchedulerDef.REMIND_SCHEDULER, majorVersion = 1, minorVersion = 0, patchLevelVersion = 0)
public class RemindScheduler extends BaseScheduler<RemindSchedulerData>
{

    private static final long serialVersionUID = -8949515603434837909L;

    @Override
    protected void performTask(RemindSchedulerData data)
    {
        getLogger().info("Have a rest....");
    }

}
