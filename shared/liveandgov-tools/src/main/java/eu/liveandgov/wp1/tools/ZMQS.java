package eu.liveandgov.wp1.tools;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import eu.liveandgov.wp1.pipeline.impl.Catcher;
import eu.liveandgov.wp1.pipeline.impl.LinesIn;
import eu.liveandgov.wp1.pipeline.impl.LinesOut;
import eu.liveandgov.wp1.pipeline.impl.ZMQServer;
import org.zeromq.ZMQ;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by Lukas Härtel on 10.02.14.
 */
public class ZMQS {
    public static void main(String[] rawArgs) {
        // Analyze arguments
        final Multimap<String, String> args = ToolsCommon.commands(
                ToolsCommon.oneOf(
                        "help",
                        "zerotopic"
                ),
                ToolsCommon.sequentialShorthand(
                        "i", "interval",
                        "m", "mode",
                        "t", "topic",
                        "zt", "zerotopic",
                        "?", "help"
                ), rawArgs);


        // Get main parameter
        final String address = ToolsCommon.end(rawArgs);

        if (!Iterables.isEmpty(args.get("help")) || address == null) {
            System.out.println("usage: [options] address");
            System.out.println("  Opens a ZMQ server with the options specified");
            System.out.println("  options:");
            System.out.println("    -i, --interval INTEGER       Poll interval for the response reader");
            System.out.println("    -m, --mode STRING or INTEGER ZMQ socket mode");
            System.out.println("    -t, --topic STRING           Adds a topic to the subscriptions");
            System.out.println("    -zt, --zerotopic             Adds the zero length topic");
            System.out.println("    -?, --help                   Displays the help");
        }


        if (address != null) {
            // Convert arguments
            final long interval = Long.valueOf(Iterables.getFirst(args.get("interval"), "50"));
            final String mode = Iterables.getFirst(args.get("mode"), "PULL");
            final int zmqMode;
            if ("REQ".equalsIgnoreCase(mode))
                zmqMode = ZMQ.REQ;
            else if ("REP".equalsIgnoreCase(mode))
                zmqMode = ZMQ.REP;
            else if ("DEALER".equalsIgnoreCase(mode))
                zmqMode = ZMQ.DEALER;
            else if ("ROUTER".equalsIgnoreCase(mode))
                zmqMode = ZMQ.ROUTER;
            else if ("PUB".equalsIgnoreCase(mode))
                zmqMode = ZMQ.PUB;
            else if ("SUB".equalsIgnoreCase(mode))
                zmqMode = ZMQ.SUB;
            else if ("PUSH".equalsIgnoreCase(mode))
                zmqMode = ZMQ.PUSH;
            else if ("PULL".equalsIgnoreCase(mode))
                zmqMode = ZMQ.PULL;
            else if ("PAIR".equalsIgnoreCase(mode))
                zmqMode = ZMQ.PAIR;
            else
                zmqMode = Integer.valueOf(mode);

            final ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(1);

            // Producer of input data
            LinesIn lip = new LinesIn();

            // Catcher of unsupported operation exception thrown when sending is not allowed
            Catcher<String> ccr = new Catcher<String>();
            ccr.registerException(UnsupportedOperationException.class);

            // ZMQ server
            ZMQServer zcp = new ZMQServer(ex, interval, zmqMode, address);

            // Result writer
            LinesOut loc = new LinesOut(System.out);

            for (String topic : args.get("topic"))
                zcp.subscribe(topic);

            if (!args.get("zerotopic").isEmpty())
                zcp.subscribe("");

            lip.setConsumer(ccr);
            ccr.setConsumer(zcp);
            zcp.setConsumer(loc);

            lip.readFrom(System.in);
        }
    }
}
