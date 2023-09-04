import libraries.Rngs;
import model.TimeSlot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static model.Constants.*;
import static model.Events.*;


//  TODO  UPDATE THIS WHEN EVENTS CHANGE
/*
-TICKET CHECK-
 0: ARRIVO
 1-4: SERVIZIO
 5: ABBANDONO

-PERQUISIZIONI-
 6: ARRIVO
 7-11: SERVIZIO
 12: ABBANDONO
 */

 class ImprovedVIPComputationalModel {

    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = 3 * 3600;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        int streamIndex = 1;
        long classOneTotalPopulation = 0; /* job number in perquisition first class node */
        long classTwoTotalPopulation = 0; /* job number in perquisition second class node */
        long firstClassJobInQueue = 0; /* job number in perquisition first class queue */
        long secondClassJobInQueue = 0; /* job number in perquisition second class queue */
        int e;                      /* next event index                    */
        int s;                      /* server index                        */
        long indexServer = 0;            /* used to count processed jobs in vip whole node        */
        long indexServerWait = 0;  /* used to count processed jobs in vip single queue        */


        double areaServer = 0.0;           /* time integrated number in the node */
        double areaServerWait = 0.0;
        double service;
        long abandonCheck = 0;
        List<Double> abandon = new ArrayList<>();
        double serverFirstCompletetion = 0;

        ImprovedVIPComputationalModel m = new ImprovedVIPComputationalModel();
        List<Integer> serverPriorityClassService = new ArrayList<>();

        for (int i = 0; i < IMPROVED_VIP_SERVERS; i++) {
            serverPriorityClassService.add(0);
        }


        Rngs r = new Rngs();
        r.plantSeeds(0);


        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 922, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        VIPMsqEvent[] event = new VIPMsqEvent[ALL_EVENTS_VIP_IMPROVED];
        VIPMsqSum[] sum = new VIPMsqSum[ALL_EVENTS_VIP_IMPROVED];

        for (s = 0; s < ALL_EVENTS_VIP_IMPROVED; s++) {
            event[s] = new VIPMsqEvent();
            sum[s] = new VIPMsqSum();
        }

        VIPMsqT t = new VIPMsqT();
        t.current = START;

        event[0].t = m.getArrival(r, t.current);
        event[0].x = 1;

        for (s = 1; s < IMPROVED_VIP_SERVERS; s++) {
            event[s].t = START;          /* this value is arbitrary because */
            event[s].x = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served = 0;
        }

        while ((event[0].x != 0) || (classOneTotalPopulation + classTwoTotalPopulation != 0)) {

            if (!abandon.isEmpty()) {
                event[IMPROVED_VIP_SERVERS + IMPROVED_VIP_ABANDON_EVENT].t = abandon.get(0);
                event[IMPROVED_VIP_SERVERS + IMPROVED_VIP_ABANDON_EVENT].x = 1;     // activate abandon
            } else {
                event[IMPROVED_VIP_SERVERS + IMPROVED_VIP_ABANDON_EVENT].x = 0;     // deactivate abandon
            }

            e = m.nextEvent(event);                                         /* next event index */
            t.next = event[e].t;                                            /* next event time  */
            areaServer += (t.next - t.current) * (classOneTotalPopulation + classTwoTotalPopulation);  /* update integral  */
            areaServerWait += (t.next - t.current) * (firstClassJobInQueue + secondClassJobInQueue);
            t.current = t.next;                                             /* advance the clock*/

            if (e == IMPROVED_VIP_ARRIVAL_EVENT - 1) {
                /* vip arrival */
                event[0].t = m.getArrival(r, t.current);

                if (event[0].t > STOP)
                    event[0].x = 0;

//                if (numberTicketCheck <= IMPROVED_VIP_SERVERS) {
//                    service = m.getService(r, 0, V_TC_SR);
//                    s = m.findOneTicketCheck(event);
//                    sum[s].service += service;
//                    sum[s].served++;
//                    event[s].t = t.current + service;
//                    event[s].x = 1;
//                }
//            }else if(true){

                event[ALL_EVENTS_VIP_IMPROVED - 1].x = 0;

                // generate, with probability P4, an abandon
                // (after eventually failed ticket check)
                boolean isAbandon = generateAbandon(r, streamIndex, P6);
                if (isAbandon) {  // add an abandon
                    double abandonTime = t.current + 0.01;  // this will be the next abandon time (it must be small in order to execute the abandon as next event)
                    abandon.add(abandonTime);
                } else{
                    // if there isn't abandon process arrival
                    service = m.getService(r, 4, VIP_MEAN_SERVICE_TIME);
                    if (service < 10) {
                        /* first queue */
                        classOneTotalPopulation++;
                    } else {
                        /* second queue */
                        classTwoTotalPopulation++;
                    }
                    if (classOneTotalPopulation + classTwoTotalPopulation <= IMPROVED_VIP_SERVERS) {
                        /* the total node population is below the total number of servers */
                        s = m.findServer(event);

                        /* mark s as service for the first/second queue */
                        // bind server index with job type into the array
                        if (service < 10) {
                            try {
                                // small job ( without bags)
                                serverPriorityClassService.set(s - 1, 1);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        else {
                            try {
                                // big job
                                serverPriorityClassService.set(s - 1, 2);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                        sum[s].service += service;
                        sum[s].served++;
                        event[s].t = t.current + service;
                        event[s].x = 1;
                    }else{
                        //UPDATE THE NUMBER OF JOBS IN QUEUE
                        indexServerWait++;

                        if (service<10)
                            firstClassJobInQueue ++;
                        else secondClassJobInQueue++;
                    }


                }
            }else if (e == IMPROVED_VIP_SERVERS + IMPROVED_VIP_ABANDON_EVENT) {
                // ticket check abandon
                abandonCheck++;
                abandon.remove(0);
            }
            else if ( (e >= IMPROVED_VIP_ARRIVAL_EVENT) && (e <= IMPROVED_VIP_SERVERS) ) {
                boolean isFromFirstQueue = (serverPriorityClassService.get(e - 1) == 1);
                if (isFromFirstQueue)
                    classOneTotalPopulation--;
                else
                    classTwoTotalPopulation--;

                    // perquisition service
                if (serverFirstCompletetion == 0)
                    serverFirstCompletetion = t.current;



                boolean isAbandon = generateAbandon(r, streamIndex, P6);
                indexServer++;
                if (isAbandon) {
                    double abandonTime = t.current + 0.01;
                    abandon.add(abandonTime);
                }


                s = e;
                // Now, there is idle server, so re-generate service time for queued job
                if (firstClassJobInQueue >= 1) {
                    firstClassJobInQueue --;
                    // GENERATE A SERVICE LESS THAN 10 SECONDS
                    do {
                        service = m.getService(r, 160, V_P_SR);
                    } while (!(service < 10));
                    serverPriorityClassService.set(s - 1, 1);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1;
                } else if (secondClassJobInQueue >= 1) {
                    secondClassJobInQueue --;
                    // GENERATE A SERVICE GREATER THEN 10 SECONDS
                    do {
                        service = m.getService(r, 160, V_P_SR);
                    } while ((service < 10));
                    serverPriorityClassService.set(s - 1, 2);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1;
                } else
                    event[s].x = 0;
            }else{
                throw new Exception("Unexpected behaviour!");
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + indexServer + " jobs the VIP queue statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(event[IMPROVED_VIP_ARRIVAL_EVENT-1].t / indexServer));
        System.out.println("  avg wait ........... =   " + f.format(areaServerWait / indexServerWait));
        System.out.println("  avg response time .. =   " + f.format(areaServer / indexServer));

        double serverFinalTime = 0;
        for (s = 1; s <= IMPROVED_VIP_SERVERS; s++) {
            if (event[s].t > serverFinalTime)
                serverFinalTime = event[s].t;
        }

        double serverActualTime = serverFinalTime - serverFirstCompletetion;

        System.out.println("  avg # in node ...... =   " + f.format(areaServer / serverActualTime));

        System.out.println("# abandons: " + abandonCheck);

        for (s = 1; s <= IMPROVED_VIP_SERVERS; s++)          /* adjust area to calculate */
            areaServer -= sum[s].service;              /* averages for the queue   */

        System.out.println("  avg delay: " + areaServer / indexServer);

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");

        double allServices = 0;
        double allServed = 0;

        for (s = 1; s <= IMPROVED_VIP_SERVERS; s++) {
            System.out.print("       " + (s) + "          " + g.format(sum[s].service / serverActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexServer));
            allServices += sum[s].service;
            allServed += sum[s].served;
        }

        System.out.println("  avg service time: " + g.format(allServices / allServed));


        System.out.println("");
    }
    // this function generate a true value with (percentage * 100) % probability, oth. false
    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(1 + streamIndex);
        return rngs.random() <= percentage;
    }


    double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }


    double getArrival(Rngs r, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, exponential with rate given by the current time slot
         * --------------------------------------------------------------
         */
        r.selectStream(0);

        int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);
        // int index = 0;  // todo verification step (forcing the first timeslot)

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);
        return (sarrival);
    }


    double getService(Rngs r, int streamIndex, double serviceTime) {
        r.selectStream(streamIndex);
        return (exponential(serviceTime, r));
    }

    int nextEvent(VIPMsqEvent[] event) {
        /* ---------------------------------------
         * return the index of the next event type
         * ---------------------------------------
         */
        int e;
        int i = 0;

        while (event[i].x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */
        e = i;
        while (i < ALL_EVENTS_VIP_IMPROVED - 1) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }


    int findServer(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = 0;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < SERVERS_VIP_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

}
