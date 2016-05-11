package workflow.enumeration.base;

import com.omniselling.common.ErrorCodeEnum;

public enum WorkflowErrorCode implements ErrorCodeEnum
{
    WORKFLOW_EXIST, WORKFLOW_DISABLED, FAIL_START_WORKFLOW, FAIL_NOTIFY_ENGINE, FAIL_QUERY_WORKFLOW,
}
