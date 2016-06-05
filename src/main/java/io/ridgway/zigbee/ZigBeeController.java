package io.ridgway.zigbee;

import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZBNodeDiscover;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import com.rapplogic.xbee.util.ByteUtils;
import io.ridgway.zigbee.utils.Sleep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static io.ridgway.zigbee.utils.StringColor.cyan;
import static io.ridgway.zigbee.utils.StringColor.purple;
import static io.ridgway.zigbee.utils.StringColor.yellow;

public class ZigBeeController implements AutoCloseable {

    private static final Logger L = LoggerFactory.getLogger(ZigBeeController.class);

    private final XBee xbee = new XBee();

    private final DiscoveryThread discoveryThread = new DiscoveryThread();

    public ZigBeeController(final String port, final int baudRate) throws XBeeException {
        L.info("Creating ZigBeeController on {} / {}", port, baudRate);
        xbee.open(port, baudRate);
    }

    public void start() {
        L.info("start");
        discoveryThread.start();
        xbee.addPacketListener(this::handlePacket);
    }


    private void handlePacket(final XBeeResponse response) {
        if (response instanceof ZNetRxResponse) {
            //TODO: Executor pool
            new Thread(() -> handleRxResponse((ZNetRxResponse) response)).start();
        } else {
            L.info(purple("Ignoring packet: " + response.getClass().getName()));
        }
    }

    private void handleRxResponse(final ZNetRxResponse response) {
        final int[] data = response.getData();
        final char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) data[i];
        }
        final String str = new String(chars);
        System.out.println(yellow(str + " from " + response.getRemoteAddress64()));

        if (!str.startsWith("Hi")) {
            return;
        }

        final XBeeAddress64 addr64 = new XBeeAddress64(0, 0, 0, 0, 0, 0, 0xff, 0xff);

        // create an array of arbitrary data to send
        final int[] payload = new int[]{'X', 'B', 'e', 'e'};

        // first request we just send 64-bit address.  we get 16-bit network address with status response
        final ZNetTxRequest request = new ZNetTxRequest(addr64, payload);
        request.setFrameId(xbee.getNextFrameId());
        System.err.println("sending tx " + request + " to " + addr64);

        System.err.println("request packet bytes (base 16) " + ByteUtils.toBase16(request.getXBeePacket().getPacket()));

        try {
            final long start = System.currentTimeMillis();
            System.err.println("Send");
            final XBeeResponse resp = xbee.sendSynchronous(request, 10000);
            final ZNetTxStatusResponse txResp = (ZNetTxStatusResponse) resp;

            System.err.println("received response " + txResp);

            //System.err.println("status response bytes:" + ByteUtils.toBase16(response.getPacketBytes()));

            if (txResp.getDeliveryStatus() == ZNetTxStatusResponse.DeliveryStatus.SUCCESS) {
                // the packet was successfully delivered
                if (txResp.getRemoteAddress16().equals(XBeeAddress16.ZNET_BROADCAST)) {
                    // specify 16-bit address for faster routing?.. really only need to do this when it changes
                    request.setDestAddr16(txResp.getRemoteAddress16());
                }
            } else {
                // packet failed.  log error
                // it's easy to create this error by unplugging/powering off your remote xbee.  when doing so I get: packet failed due to error: ADDRESS_NOT_FOUND
                System.err.println("packet failed due to error: " + txResp.getDeliveryStatus());
            }

            // I get the following message: Response in 75, Delivery status is SUCCESS, 16-bit address is 0x08 0xe5, retry count is 0, discovery status is SUCCESS
            System.err.println("Response in " + (System.currentTimeMillis() - start) + ", Delivery status is " + txResp.getDeliveryStatus() + ", 16-bit address is " + ByteUtils.toBase16(txResp.getRemoteAddress16().getAddress()) + ", retry count is " + txResp.getRetryCount() + ", discovery status is " + txResp.getDeliveryStatus());
        } catch (final XBeeTimeoutException xte) {
            System.err.println("Timeout");
        } catch (final Exception e) {
            System.err.println("Error (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        xbee.close();
    }

    public class DiscoveryThread extends Thread {

        public DiscoveryThread() {
            super("DiscoveryThread");
        }

        @Override
        public void run() {
            while (true) {
                L.info("Running discovery...");
                work();
                Sleep.milliseconds(60000);
            }
        }

        private void work() {
            try {
                discoverNodes();
            } catch (final XBeeException e) {
                L.error("Exception discovering nodes: {}", e.getMessage(), e);
            }
        }

        private void discoverNodes() throws XBeeException {
            final AtCommandResponse nodeTimeout = (AtCommandResponse) xbee.sendSynchronous(new AtCommand("NT"));
            final int nodeDiscoveryTimeout = ByteUtils.convertMultiByteToInt(nodeTimeout.getValue()) * 100;

            L.info("Node discovery timeout is {} milliseconds. Sending discover command...", nodeDiscoveryTimeout);
            xbee.sendAsynchronous(new AtCommand("ND"));

            // NOTE: increase NT if you are not seeing all your nodes reported
            final List<? extends XBeeResponse> responses = xbee.collectResponses(nodeDiscoveryTimeout);
            L.info("Discovery time up, captured {} responses", responses.size());

            final List<ZBNodeDiscover> nodes = responses.stream()
                    .filter(r -> r instanceof AtCommandResponse)
                    .map(r -> (AtCommandResponse) r)
                    .filter(r -> r.getCommand().equals("ND") && r.getValue() != null && r.getValue().length > 0)
                    .map(ZBNodeDiscover::parse)
                    .collect(Collectors.toList());

            nodes.forEach(zbNodeDiscover -> System.out.println(cyan("Node Discover is " + zbNodeDiscover)));
        }
    }

}


