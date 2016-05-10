package workflow.data.leave;

import workflow.data.base.BaseWorkflowData;

public class LeaveWorkflowData extends BaseWorkflowData
{

    /**
     * 
     */
    private static final long serialVersionUID = 314596021147941568L;
    private String businessNum;

    public String getBusinessNum()
    {
        return businessNum;
    }

    public void setBusinessNum(String businessNum)
    {
        this.businessNum = businessNum;
    }

}
