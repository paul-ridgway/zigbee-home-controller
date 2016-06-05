package io.ridgway.zigbee;

public class ZigBeeApp {


    private static volatile boolean shutdown = false;

    public static void main(final String[] args) throws Exception {


        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.err.println("Shutdown hook caught!");
                System.err.flush();
            }
        });

        try (final ZigBeeController zc = new ZigBeeController("/dev/ttyUSB0", 9600)) {
            zc.start();
             while (!shutdown) {
                Thread.sleep(100);
            }
        }
//// this is the Serial High (SH) + Serial Low (SL) of the remote XBee
//        XBeeAddress64 addr64 = new XBeeAddress64(0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0, 1);
//
//        // Turn on DIO0 (Pin 20)
//        RemoteAtRequest request = new RemoteAtRequest(addr64, "D0", new int[]{XBeePin.Capability.DIGITAL_OUTPUT_HIGH.getValue()});
//
//        xbee.sendAsynchronous(request);
//
//        RemoteAtResponse response = (RemoteAtResponse) xbee.getResponse();
//
//        if (response.isOk()) {
//            System.out.println("Successfully turned on DIO0");
//        } else {
//            System.out.println("Attempt to turn on DIO0 failed.  Status: " + response.getStatus());
//        }
//
//// shutdown the serial port and associated threads
//        xbee.close();

    }


}

