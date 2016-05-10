package workflow.persistent.base;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.copperengine.core.Acknowledge;
import org.copperengine.core.AutoWire;
import org.copperengine.core.CopperException;
import org.copperengine.core.DuplicateIdException;
import org.copperengine.core.Interrupt;
import org.copperengine.core.ProcessingEngine;
import org.copperengine.core.Response;
import org.copperengine.core.WaitMode;
import org.copperengine.core.Workflow;
import org.copperengine.core.WorkflowInstanceDescr;
import org.copperengine.core.WorkflowVersion;
import org.copperengine.core.audit.AuditTrail;
import org.copperengine.core.persistent.PersistentWorkflow;
import org.copperengine.management.ProcessingEngineMXBean;
import org.copperengine.management.model.WorkflowInfo;

import workflow.data.base.BaseWorkflowData;

import com.omniselling.common.model.BaseResponse;

public abstract class BasePersistentWorkflow<E extends BaseWorkflowData> extends PersistentWorkflow<E>
{

    private static final long serialVersionUID = -5414921076132557200L;
    private static final Acknowledge.DefaultAcknowledge defaultAck = new Acknowledge.DefaultAcknowledge();
    private static final Acknowledge.BestEffortAcknowledge bestEffortAck = new Acknowledge.BestEffortAcknowledge();
    protected static final int RUN_NOW = 1;

    protected enum LogLevel
    {
        INFO, WARN, ERROR
    }

    protected enum MessageType
    {
        TEXT, JSON, XML, BIN
    }

    protected Logger getLogger()
    {
        return LogManager.getLogger("PWF>" + getClass().getSimpleName());
    }

    protected transient AuditTrail auditTrail;

    @AutoWire
    public void setAuditTrail(AuditTrail auditTrail)
    {
        this.auditTrail = auditTrail;
    }

    /**
     * Define the main flow here, <b>PLEASE FOLLOW</b> Guidelines:<br>
     * <li>Put each step in a private function, each step must have its business meaning</li> <li>Workflow data must
     * extend {@link BaseWorkflowData}</li> <li>Make external calls through adapters, adapters ref to external services
     * and models</li> <li>Use {@link #waitForResponse(long, String) waitForResponse(...)} to wait for Async responses
     * from engine, and {@link #notifyEngineSync(String, Object, String) notifyEngineSync(...)} to send response to
     * engine to resume workflow in wait status</li> <li>In more complicated scenario, you may use
     * {@link Workflow#wait(WaitMode mode, int timeoutMsec, String... correlationIds) wait(...)} to wait for response
     * followed by {@link Workflow#getAndRemoveResponse(String correlationId) getAndRemoveResponse()} to retrieve the
     * {@link Response}, check timeout using {@link Response#isTimeout()}</li> <li>Call {@link Workflow#savepoint()} to
     * save the workflow state, to avoid duplicated execution during process crash/restart, but note that any wait will
     * do this</li> <li>Make sure all subflows have their independent workflow data, to avoid concurrency issues</li>
     * <li>Remember to reload data (i.e. Order) if the data might be changed after a "wait/sleep"</li> <li>Must handle
     * exceptions from external calls, rollback operation by invoking reverse methods when necessary (i.e.
     * externalService.createSomething() reversed by externalService.deleteSomething())</li> <li>if need to use
     * for-loops in workflow, always use <b>for(int i;i&lt;c.size();i++)</b> instead of <strike>for(element:c)</strike>
     * notation</li>
     * 
     * @throws Interrupt
     */
    protected abstract void runFlow() throws Interrupt;

    /**
     * Identifies the workflow's transactionId, this is normally businessNum,orderId which is consistent throughout the
     * workflow
     * 
     * @return
     */
    protected abstract String getTransactionId();

    /**
     * Utility script to get current method name
     * 
     * @return
     */
    protected static String getCurrentMethodName()
    {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    @Override
    public final void main() throws Interrupt
    {
        String log = "WF Instance " + getId() + " priority: " + getPriority() + " started";
        getLogger().info(log);
        auditLog(LogLevel.INFO, getClass().getSimpleName() + ".main", "Started");

        //execute the main workflow
        runFlow();
        //notify parent if it's part of a subflow
        //TODO: make this a property of Workflow 
        if (hasParentFlow())
        {
            notifyParent();
        }
        log = "WF Instance " + getId() + " ended";
        getLogger().info(log);
        auditLog(LogLevel.INFO, getClass().getSimpleName() + ".main", "Ended");

    }

    /**
     * Put the workflow to sleep for specified seconds, save workflow state and release the thread
     * 
     * @param seconds
     * @throws Interrupt
     */
    protected final void sleepWithState(int seconds) throws Interrupt
    {
        getLogger().info("Sleeping " + seconds + " seconds up to next try...");
        wait(WaitMode.ALL, seconds * 1000, getEngine().createUUID());
    }

    protected String createUUID()
    {
        return getEngine().createUUID();
    }

    @SuppressWarnings("deprecation")
    private void notifyParent()
    {
        notifyEngineSync(getId(), null, "ParentId:" + getData().getParentWorkflowInstanceId());
    }

    /**
     * check if the current workflow have a parent flow waiting
     * 
     * @return
     */
    @SuppressWarnings("deprecation")
    protected final boolean hasParentFlow()
    {
        return getData().getParentWorkflowInstanceId() != null;
    }

    /**
     * Send a response to process engine without checking if anyone received it
     * 
     * @param correlationId
     * @param data
     *            - actual response data
     * @param metadata
     *            - extra message
     */
    protected <R> void notifyEngineAsync(String correlationId, R data, String metadata)
    {
        if (StringUtils.isEmpty(correlationId))
        {
            throw new IllegalArgumentException("correlationId cannot be null or empty");
        }
        Response<R> response = new Response<R>(correlationId, data, null, false, metadata, null, null);
        notifyEngine(response, bestEffortAck);
    }

    /**
     * Send a response to process engine without checking if anyone received it
     * 
     * @param response
     *            - gives more control to the caller, use with care
     */
    protected <R> void notifyEngineAsync(Response<R> response)
    {
        notifyEngine(response, bestEffortAck);
    }

    /**
     * Send a response to process engine and wait for its acknowledgement
     * 
     * @param correlationId
     * @param data
     *            - actual response data
     * @param metadata
     *            - extra message to save to response table
     */
    protected <R> void notifyEngineSync(String correlationId, R data, String metadata)
    {
        if (StringUtils.isEmpty(correlationId))
        {
            throw new IllegalArgumentException("correlationId cannot be null or empty");
        }
        Response<R> response = new Response<R>(correlationId, data, null, false, metadata, null, null);
        notifyEngine(response, defaultAck);
        defaultAck.waitForAcknowledge();
    }

    /**
     * Send a response to process engine and wait for its acknowledgement
     * 
     * @param correlationId
     * @param data
     *            - actual response data
     * @param metadata
     *            - extra message to save to response table
     */
    protected <R> void notifyEngineSync(String correlationId, Exception e, String metadata)
    {
        if (StringUtils.isEmpty(correlationId))
        {
            throw new IllegalArgumentException("correlationId cannot be null or empty");
        }
        Response<R> response = new Response<R>(correlationId, null, e, false, metadata, null, null);
        notifyEngine(response, defaultAck);
        defaultAck.waitForAcknowledge();
    }

    /**
     * Send a response to process engine, this method gives total control to the caller, caller may choose to call
     * Acknowledge's interface for custom behaviors after notify engine, see {@link Acknowledge} for more detail
     * 
     * @param response
     *            - type of {@link Response}
     * @param ack
     *            - see {@link Acknowledge} for more detail
     */
    protected <R> void notifyEngine(Response<R> response, Acknowledge ack)
    {
        getEngine().notify(response, ack);
    }

    /**
     * Avoid using this in custom workflow, use provided template methods whenever possible
     */
    @Override
    @Deprecated
    public final ProcessingEngine getEngine()
    {
        return super.getEngine();
    }

    /**
     * Start the subflow specified by workflowId, running the latest version, with priority 5(default)
     * 
     * @param workflowId
     * @param subflow
     *            data, should be a subset or derived from main process' workflow data
     * @return subflow instanceId
     * @throws DuplicateIdException
     * @throws CopperException
     */
    @SuppressWarnings("deprecation")
    protected final <D extends BaseWorkflowData> String startSubflow(String workflowId, D data) throws CopperException
    {
        getLogger().info("Subflow " + workflowId + " starting...");
        data.setParentWorkflowInstanceId(getId());
        String instanceId = getEngine().run(workflowId, data);
        return instanceId;
    }

    /**
     * Start the subflow specified by all possible parameters
     * 
     * @param workflowId
     * @param data
     * @param priority
     * @param poolId
     * @param version
     *            (major,minor,patchlevel)
     * @return
     * @throws DuplicateIdException
     * @throws CopperException
     */
    @SuppressWarnings("deprecation")
    protected final <D extends BaseWorkflowData> String startSubflow(String workflowId, D data, Integer priority,
            WorkflowVersion version) throws DuplicateIdException, CopperException
    {
        getLogger().info(
                "Subflow " + workflowId + "(priority=" + priority + ",version=" + version.format() + ") starting...");
        data.setParentWorkflowInstanceId(getId());
        WorkflowInstanceDescr<D> wfInstanceDescr = new WorkflowInstanceDescr<D>(workflowId, data, null, priority, null,
                version);
        String instanceId = getEngine().run(wfInstanceDescr);
        return instanceId;
    }

    /**
     * Start the subflow specified by workflowId , instanceId and data
     * 
     * @param workflowId
     * @param data
     * @param instanceId
     * @return
     * @throws DuplicateIdException
     * @throws CopperException
     */
    @SuppressWarnings("deprecation")
    protected final <D extends BaseWorkflowData> String startSubflow(String workflowId, D data, String instanceId)
            throws DuplicateIdException, CopperException
    {
        getLogger().info("Subflow " + workflowId);
        data.setParentWorkflowInstanceId(getId());
        WorkflowInstanceDescr<D> wfInstanceDescr = new WorkflowInstanceDescr<D>(workflowId, data, instanceId, null,
                null);
        getEngine().run(wfInstanceDescr);
        return instanceId;
    }

    /**
     * start a workflow and wait for it to finish
     * 
     * @param workflowId
     * @param data
     * @param timeoutMsec
     * @return Response for the subflow's workflow instanceId, check Response.isTimeout() for timeout
     * @throws Interrupt
     */
    protected final <D extends BaseWorkflowData, R> Response<R> startSubflowAndWait(String workflowId, D data,
            int timeoutMsec) throws Interrupt
    {
        try
        {
            String instanceId = startSubflow(workflowId, data);
            return waitForResponse(timeoutMsec, instanceId);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //error
        return null;
    }

    /**
     * Use this when there could be multiple responses of the same correlationId
     * 
     * @param timeoutMs
     * @param correlationId
     * @return List of Responses for the waited correlationId, for timeout
     * @throws Interrupt
     */
    protected final <T> List<Response<T>> waitForResponses(long timeoutMs, String correlationId) throws Interrupt
    {
        if (StringUtils.isEmpty(correlationId))
        {
            throw new IllegalArgumentException("correlationId cannot be null or empty");
        }
        getLogger().info("Waiting for correlationId=[" + correlationId + "]'s notification");

        wait(WaitMode.ALL, timeoutMs, TimeUnit.MILLISECONDS, correlationId);

        List<Response<T>> resultData = getAndRemoveResponses(correlationId);
        if (resultData.isEmpty())
        {
            getLogger().info(correlationId + " returned empty list, probabably timeout");
        }
        else
        {
            getLogger().info(correlationId + " returned " + resultData.size() + " responses");
        }
        return resultData;
    }

    /**
     * Note: <b>Don't</b> Use this method when there could be multiple responses of the same correlationId at the same
     * time, use {@link #waitForResponses} instead
     * 
     * @param timeoutMs
     * @param correlationId
     * @return Response for the waited correlationId, check Response.isTimeout() for timeout
     * @throws Interrupt
     */
    protected final <T> Response<T> waitForResponse(long timeoutMs, String correlationId) throws Interrupt
    {
        if (StringUtils.isEmpty(correlationId))
        {
            throw new IllegalArgumentException("correlationId cannot be null or empty");
        }
        getLogger().info("Waiting for correlationId=[" + correlationId + "]'s notification");

        wait(WaitMode.ALL, timeoutMs, TimeUnit.MILLISECONDS, correlationId);

        Response<T> resultData = getAndRemoveResponse(correlationId);
        if (resultData.isTimeout())
        {
            getLogger().info(correlationId + " response timeout");
        }
        else
        {
            getLogger().info(correlationId + " returned with data " + resultData.getResponse());
        }
        return resultData;
    }

    /**
     * 
     * @param timeoutSec
     * @param correlationId
     * @return List of the responses for the waited correlationId
     * @throws Interrupt
     *             protected final <T> List<Response<T>> waitForResponses(int timeoutSec, String correlationId) throws
     *             Interrupt { if (StringUtils.isEmpty(correlationId)) { throw new IllegalArgumentException(
     *             "correlationId cannot be null or empty"); } getLogger().info("Waiting for correlationId=[" +
     *             correlationId + "]'s notification");
     * 
     *             wait(WaitMode.ALL, timeoutSec * 1000, correlationId);
     * 
     *             List<Response<T>> resultData = getAndRemoveResponses(correlationId); getLogger().info(correlationId +
     *             " returned " + resultData.size() + " reponses"); return resultData; }
     */

    /**
     * This simply waits for all subprocesses to finish, without getting the responses for each
     * 
     * @param instances
     * @throws Interrupt
     */
    protected final void waitForAllSubflows(String... instances) throws Interrupt
    {
        if (instances == null || instances.length == 0)
        {
            throw new IllegalArgumentException("Subflow instances cannot be null or empty");

        }
        getLogger().info("Waiting for " + Arrays.toString(instances) + " to finish");

        waitForAll(instances);

        for (int i = 0; i < instances.length; i++)
        {
            getAndRemoveResponse(instances[i]);
        }
        getLogger().info("Subflows " + Arrays.toString(instances) + " finished");
    }

    /**
     * Minimum parameters to adding an auditlog, async style
     * 
     * @param level
     *            - see {@link LogLevel}
     * @param context
     *            - mandatory,maxLength=64 use it for the business step, like sendSms
     * @param message
     */
    protected void auditLog(LogLevel level, String context, String message)
    {
        auditLog(level, context, message, MessageType.TEXT, createUUID(), null);
    }

    /**
     * Returns immediately after queuing the log message (async style) Typical parameters to adding an audit log NOTE:
     * log it only when needed
     * 
     * @param level
     *            - see {@link LogLevel}
     * @param context
     *            - mandatory,maxLength=64 use it for the business step, like sendSms
     * @param message
     * @param correlationId
     *            - optional, the id used for wait/notify
     */
    protected void auditLog(LogLevel level, String context, String message, String correlationId)
    {
        auditLog(level, context, message, MessageType.TEXT, createUUID(), correlationId);
    }

    /**
     * Returns immediately after queuing the log message, full parameters NOTE: log it only when needed
     * 
     * @param level
     *            - mandatory
     * @param context
     *            - mandatory,maxLength=64, use it for the business step, like sendSms,
     * @param message
     *            - mandatory
     * @param messageType
     *            - default TEXT, see {@link MessageType}
     * @param conversationId
     *            - maxLength=128, default will be created using UUID
     * @param correlationId
     *            - maxLength=128, the id used for wait/notify default to null
     */
    protected void auditLog(LogLevel level, String context, String message, MessageType messageType,
            String conversationId, String correlationId)
    {

        if (level == null)
        {
            throw new IllegalArgumentException("LogLevel cannot be null");
        }
        if (context == null)
        {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (conversationId == null)
        {
            conversationId = createUUID();
        }
        if (messageType == null)
        {
            messageType = MessageType.TEXT;
        }

        auditTrail.asynchLog(level.ordinal(), new Date(), conversationId, context, getId(), correlationId,
                getTransactionId(), message, messageType.name());

    }

    /**
     * writes an event to the audit trail log and returns after the log message is written to the underlying storage.
     * See sync version of {@link #auditLog(LogLevel, String, String, MessageType, String, String)}
     * 
     * @param level
     * @param context
     *            - mandatroy, maxLength=64
     * @param message
     * @param messageType
     * @param conversationId
     *            - maxLength=128
     * @param correlationId
     *            - maxLength=128
     */
    protected void auditLogSync(LogLevel level, String context, String message, MessageType messageType,
            String conversationId, String correlationId)
    {

        if (level == null)
        {
            throw new IllegalArgumentException("LogLevel cannot be null");
        }

        if (context == null)
        {
            throw new IllegalArgumentException("context cannot be null");
        }

        if (messageType == null)
        {
            messageType = MessageType.TEXT;
        }
        if (conversationId == null)
        {
            conversationId = createUUID();
        }
        auditTrail.synchLog(level.ordinal(), new Date(), conversationId, context, getId(), correlationId,
                getTransactionId(), message, messageType.name());

    }

    /**
     * Throw runtime exception to stop the workflow when unexpected exception occur
     * 
     * @param context
     * @param res
     * @param correlationId
     */
    protected void throwOnError(String context, BaseResponse<?> res, String correlationId)
    {
        if (res.hasError())
        {
            throwRuntimeException(res.getErrorsToString(), context, correlationId);
        }
    }

    /**
     * Throw runtime exception to stop the workflow when unexpected exception occur
     * 
     * @param context
     * @param res
     */
    protected void throwOnError(String context, BaseResponse<?> res)
    {
        if (res.hasError())
        {
            throwRuntimeException(res.getErrorsToString(), context);
        }
    }

    protected void throwRuntimeException(String msg, String context)
    {
        throwRuntimeException(msg, context, null);
    }

    protected void throwRuntimeException(String msg, String context, String correlationId)
    {
        auditLog(LogLevel.ERROR, getClass().getSimpleName() + "-" + context, msg, correlationId);
        throw new RuntimeException(msg);
    }

    protected void throwRuntimeException(Throwable cause, String context, String correlationId)
    {
        auditLog(LogLevel.ERROR, getClass().getSimpleName() + "-" + context, cause.getMessage(), correlationId);
        throw new RuntimeException(cause);
    }

    protected boolean getBooleanValue(BaseResponse<Boolean> boolRes)
    {
        return boolRes != null && boolRes.getData() != null && boolRes.getData().booleanValue();
    }

    protected int getIntValue(BaseResponse<Integer> intRes)
    {
        if (intRes != null && intRes.getData() != null)
        {
            return intRes.getData();
        }
        return 0;
    }

    protected long getLongValue(BaseResponse<Long> longRes)
    {
        if (longRes != null && longRes.getData() != null)
        {
            return longRes.getData();
        }
        return 0;
    }

    protected boolean isWorkflowActive(final String workflowInstanceId)
    {
        WorkflowInfo wfi = getActiveWorkflow(workflowInstanceId);
        return wfi != null;
    }

    /**
     * internal use
     * 
     * @param workflowInstanceId
     * @return
     */
    protected WorkflowInfo getActiveWorkflow(final String workflowInstanceId)
    {
        ProcessingEngineMXBean engineMXBean = (ProcessingEngineMXBean) getEngine();
        WorkflowInfo wfi = engineMXBean.queryActiveWorkflowInstance(workflowInstanceId);
        return wfi;
    }
}
