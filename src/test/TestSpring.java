package test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring-context.xml" })
public class TestSpring
{

    @Test
    public void test() throws Exception
    {
        System.out.println("...");
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(3000);
                    System.out.println("after 3000 ms");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
        waitting();
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
