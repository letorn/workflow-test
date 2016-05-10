package workflow.data.base;

import java.io.Serializable;

/**
 * Basic Workflow Data class that should be extended for all workflow data
 * Currently it's for synchronizing subflows 
 *  
 * @author Atomic
 *
 */
public abstract class BaseWorkflowData implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = 4234745228791309946L;
    private String parentWorkflowInstanceId;

    /**
     * internal only - should only be used by BasePersistentWorkflow
     * @return
     */
    @Deprecated
    public String getParentWorkflowInstanceId()
    {
        return parentWorkflowInstanceId;
    }

    /**
     * internal only - should only be used by BasePersistentWorkflow
     * @param parentWorkflowInstanceId
     */
    @Deprecated
    public void setParentWorkflowInstanceId(String parentWorkflowInstanceId)
    {
        this.parentWorkflowInstanceId = parentWorkflowInstanceId;
    }

}
