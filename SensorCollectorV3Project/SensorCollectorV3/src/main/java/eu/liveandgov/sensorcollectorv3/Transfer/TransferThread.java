package eu.liveandgov.sensorcollectorv3.Transfer;

import android.content.Context;
import android.util.Log;

import org.jeromq.ZMQ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import eu.liveandgov.sensorcollectorv3.Persistence.FilePersistor;
import eu.liveandgov.sensorcollectorv3.Persistence.Persistor;
import eu.liveandgov.sensorcollectorv3.Persistence.PersistorThread;

/**
 * Created by hartmann on 9/20/13.
 */
public class TransferThread implements Runnable {
    private static final String LOG_TAG = "TT";
    private ZMQ.Socket outSocket;
    private Persistor persistor;
    private boolean flag = false;

    private Thread thread;
    private static TransferThread instance;

    /* Singleton Pattern */
    private TransferThread(Persistor persistor){
        this.outSocket = ZMQ.context().socket(ZMQ.PUSH);
        this.outSocket.setHWM(1000); // do not buffer messages. Block directly
        this.persistor = persistor;
        this.thread = new Thread(this);
    }

    public static void setup(){
        instance = new TransferThread(PersistorThread.getInstance().getPersistor());
    }

    public static TransferThread getInstance(){
        return instance;
    }


    public void doTransfer() throws IOException {
        Log.i(LOG_TAG, "Starting Transfer");
        persistor.blockPush();
        File out = persistor.getFile();
        BufferedReader reader = new BufferedReader(new FileReader(out));

        String line;
        int lineCount = 0;
        boolean finished = false;
        int attempt = 0;
        int retires = 100;
        while (true){
            line  = reader.readLine();
            lineCount += 1;

            if (line == null) break;
            do {

                finished = outSocket.send(line, ZMQ.NOBLOCK);

                if (!finished) {
                    // blocked
                    if (attempt < retires){
                        attempt += 1;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    } else {
                        Log.i(LOG_TAG, "Transfer Failed after " + lineCount + " lines.");
                        persistor.unblockPush();
                        return;
                    }
                } else {
                    attempt = 0;
                }

                break;
            } while (true);
        }
        Log.i(LOG_TAG, "Finished Transfer");
        persistor.reset();
    }

    public synchronized void trigger(){
        this.flag = true;
    }

    @Override
    public void run() {
        outSocket.connect("tcp://141.26.71.84:5555");

        while (true){
            if (flag){
                flag = false;
                try {
                    doTransfer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        thread.start();
    }
}