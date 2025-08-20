package org.test;

import io.quarkus.arc.Arc;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

@Path("/hello")
public class GreetingResource {
    static final Logger Log = Logger.getLogger(GreetingResource.class);
    @Inject
    ScheduledExecutorService executor;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws ExecutionException, InterruptedException {
        //set MDC in first thread
        MDC.put("request.id", "1234");
        MDC.put("request.path", "test");
        //logs '(1234, test) request received' -> OK
        Log.info("request received");

        var asyncTask = executor.submit(() -> {
            var ctx = Arc.container().requestContext();
            ctx.activate();
            try {
                MDC.put("request.path", "async test");
                //logs '(1234, async test) Async task completed' -> OK
                Log.info("Async task completed");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.error("Error in sleep", e);
                }
                //clear MDC in second thread
                MDC.clear();
                return 1;

            }finally {
                ctx.terminate();
            }
        });
        asyncTask.get();

        //MDC is cleared in first thread too!
        //logs (, ) request received 2 -> NOT OK
        Log.info("request received 2");

        return "Hello from Quarkus REST";
    }
}
