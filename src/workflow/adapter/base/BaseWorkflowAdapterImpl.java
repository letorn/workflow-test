package workflow.adapter.base;

import org.copperengine.core.Acknowledge;
import org.copperengine.core.ProcessingEngine;
import org.copperengine.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the default adapterImpl, all adapters should extend this
 * 
 * @author Atomic
 *
 */
public abstract class BaseWorkflowAdapterImpl
{
    @Autowired
    protected ProcessingEngine engine;

    private static final Acknowledge.DefaultAcknowledge defaultAck = new Acknowledge.DefaultAcknowledge();

    /**
     * Use this utility method to send response to engine and resume workflow It will not return until engine
     * acknowledge
     * 
     * @param response
     */
    protected final <R> void notifyEngineSync(Response<R> response)
    {
        engine.notify(response, defaultAck);
        defaultAck.waitForAcknowledge();
    }

    /**
     * Use this utility method to send response to engine and resume workflow It will not return until engine
     * acknowledges
     * 
     * @param correlationId
     * @param R
     *            data, response.getResponse()
     */
    protected final <R> void notifyEngineSync(String correlationId, R data)
    {
        Response<R> response = new Response<>(correlationId, data, null);
        engine.notify(response, defaultAck);
        defaultAck.waitForAcknowledge();
    }

    /**
     * Use this utility method to return response to engine and resume workflow User might implement their own
     * Acknowledge for custom callback functions
     * 
     * @param response
     * @param ack
     */
    protected final <R> void notifyEngine(Response<R> response, Acknowledge ack)
    {
        engine.notify(response, ack);
    }

    protected final String createUUID()
    {
        return engine.createUUID();
    }
}
