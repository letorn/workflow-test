package workflow.data.base;

/**
 * User defined exception to stop flow execution with the context and error message
 * @author Atomic
 *
 */
public class WorkflowTerminationException extends Exception
{
    private static final long serialVersionUID = 7222304909049414645L;
    private String context;

    public WorkflowTerminationException(String context, String message, Throwable cause)
    {
        super(message, cause);
        this.setContext(context);

    }

    public WorkflowTerminationException(String context, String message)
    {
        super(message);
        this.setContext(context);
    }

    public WorkflowTerminationException(String context, Throwable cause)
    {
        super(cause);
        this.setContext(context);
    }

    public String getContext()
    {
        return context;
    }

    public void setContext(String context)
    {
        this.context = context;
    }

}
