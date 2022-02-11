package gsrs.module.substance.services;

import gsrs.springUtils.StaticContextAccessor;
import ix.core.util.FilteredPrintStream;
import ix.core.util.Filters;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Service to that wraps STDOUT and STDERR
 * that some other services will use to hide printlns to the console.
 *
 *
 *
 * Created by katzelda on 9/15/16.
 * @see FilteredPrintStream
 * @since 1.2
 */
@Service
public class ConsoleFilterService{

    private PrintStream oldStdOut, oldStdErr;

    private FilteredPrintStream stdOutFilter, stdErrFilter;

    @EventListener(ContextStartedEvent.class)
    public void onStart() {
       oldStdErr = System.err;
        oldStdOut = System.out;

        stdOutFilter = new FilteredPrintStream(oldStdOut);
        stdErrFilter = new FilteredPrintStream(oldStdErr);

        System.setErr(stdErrFilter);
        System.setOut(stdOutFilter);
    }

    @EventListener(ContextStoppedEvent.class )
    public void onStop() {
        System.setErr(oldStdOut);
        System.setOut(oldStdErr);

        
        //Should we close here?
        stdErrFilter.close();
        stdOutFilter.close();

    }

    public FilteredPrintStream getStdOutOutputFilter(){
        return stdOutFilter;
    }
    public FilteredPrintStream getStdErrOutputFilter(){
        return stdErrFilter;
    }
    
    
    /**
     * Convenience method to run a task while swallowing all StdErr within that
     * thread. The output swallowed can be caught by a provided consumer. 
     * @param r The task to run.
     * @param consumeSwallowed A consumer for the output swallowed.
     */
    public static void runWithSwallowedStdErr(Runnable r, Consumer<Object> consumeSwallowed){
    	 FilteredPrintStream.Filter filterOutCurrentThread = Filters.filterOutCurrentThread();

         try (FilteredPrintStream.FilterSession ignoreThread = StaticContextAccessor.getBean(ConsoleFilterService.class).getStdErrOutputFilter().newFilter(filterOutCurrentThread).withOnSwallowed(consumeSwallowed)){
        	r.run(); 
         }
    }
    public static void runWithSwallowedStdErr(Runnable r){
    	runWithSwallowedStdErr(r,null);
    }
    /**
     * Convenience method to run a task while swallowing StdErr from classes
     * matching a certain regex pattern. The output swalled can be caught by
     * a provided consumer. 
     * @param r The task to run.
     * @param pattern Regex pattern of the Class names to swallow output from.
     * @param consumeSwallowed A consumer for the output swallowed
     */
    public static void runWithSwallowedStdErrFor(Runnable r, String pattern, Consumer<Object> consumeSwallowed){
   	 FilteredPrintStream.Filter filterOutPattern = Filters.filterOutAllClasses(pattern);
        try (FilteredPrintStream.FilterSession ignoreClass = StaticContextAccessor.getBean(ConsoleFilterService.class).getStdErrOutputFilter().newFilter(filterOutPattern).withOnSwallowed(consumeSwallowed)){
        	r.run();	 
        }
    }

	public static void runWithSwallowedStdErrFor(Runnable r, String pattern) {
		runWithSwallowedStdErrFor(r,pattern,null);
	}
	

    
}
