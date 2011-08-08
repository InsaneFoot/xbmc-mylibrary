package utilities;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

public class ReaderTimeout implements Runnable, Constants
{

    private int timeoutSec;
    private BufferedReader reader;
    private boolean timedOut;
    private boolean CANCELED = false;
    private List<String> lines = new ArrayList<String>();
    private Thread readerThread;
    long lastLineRead;
    public ReaderTimeout(BufferedReader reader, int seconds)
    {
        this.reader = reader;
        this.timedOut = false;
        this.timeoutSec = seconds;
        this.lastLineRead = System.currentTimeMillis();
        if(timeoutSec < 0) timeoutSec = 0;
        readerThread = new Thread(this);
        readerThread.start();
        timeout();
    }

    public void timeout()
    {
        //Config.log(DEBUG, "ReaderTimeout thread started for "+ timeoutSec + " second timeout....");        

        while(!CANCELED)
        {
            try
            {
                Thread.sleep(50);
                long now = System.currentTimeMillis();
                long secondsPassed = (now-lastLineRead) / 1000;
                if(secondsPassed >= timeoutSec)
                {
                    if(!CANCELED) forceClose();
                    break;
                }
            }

            catch(Exception x)
            {
                Config.log(WARNING, "Failed while waiting for Reader Timeout: "+ x,x);
                cancelTimeout();
                break;
            }
        }
        Config.log(DEBUG,"ReaderTimeout exited, cancelled = "+ CANCELED+", timedOut = "+ timedOut);

        if(!CANCELED && !hasTimedOut())//if neither one of these are true, getLines() would never end. Force one to true (shouldn't happen)
            cancelTimeout();
    }

    public List<String>  getLines()
    {
        while(!hasTimedOut() && !CANCELED)
        {
            try{Thread.sleep(50);}catch(Exception x){timedOut=true;}
        }
        if(timedOut)
            Config.log(WARNING, "Reader timed out after "+ timeoutSec +" seconds. Data may be incomplete.");

        return lines;
    }
    private void cancelTimeout()
    {        
        CANCELED = true;
    }

    //reads lines until Thread.stop() is called or it exits gracefully
    public void run()
    {
        try
        {
           String line;
           while((line=reader.readLine()) != null)
           {
               lastLineRead = System.currentTimeMillis();
               lines.add(line);               
           }
           reader.close();
           //reached the end gracefully
           cancelTimeout();
        }

        catch(Exception x)
        {
            Config.log(ERROR, "Error while reading from BufferedReader: "+x,x);
        }
    }

    public boolean hasTimedOut()
    {
        return timedOut;
    }
    
    private void forceClose()
    {
        Config.log(INFO, "Attempting to force closed input stream because it timed out.");
        timedOut = true;
        try
        {
            Config.log(WARNING, "Killing Reader thread because it timed out");
            readerThread.stop();
        }
        catch(Exception x)
        {
            Config.log(WARNING, "Failed to close reader: "+x,x);
        }
    }
}