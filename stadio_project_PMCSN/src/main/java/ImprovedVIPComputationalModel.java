import libraries.Rngs;
import model.TimeSlot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static model.Constants.*;
import static model.Events.*;
import static model.ImprovedEvents.ALL_EVENTS_TICKET;
import static model.ImprovedEvents.ALL_EVENTS_TURNSTILES;
import static model.ImprovedEvents.SERVERS_PERQUISITION;


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

    public static void main(String[] args) {

        int streamIndex = 1;
        long numberTicketCheck = 0;   /* number in the ticket check node    */
        long classOneTotalPopulation = 0; /* job number in perquisition first class node */
        long classTwoTotalPopulation = 0; /* job number in perquisition second class node */
        long firstClassJobInQueue = 0; /* job number in perquisition first class queue */
        long secondClassJobInQueue = 0; /* job number in perquisition second class queue */
        int e;                      /* next event index                    */
        int s;                      /* server index                        */
        long indexPerquisition = 0;            /* used to count processed jobs in perquisition        */
        long indexTicketCheck = 0;             /* used to count processed jobs in ticket check        */
        double areaTicketCheck = 0.0;           /* time integrated number in the node */
        double areaPerquisition = 0.0;           /* time integrated number in the node */
        double service;
        long abandonTicketCheck = 0;
        long abandonPerquisition = 0;
        List<Double> abandonsTicket = new ArrayList<>();
        List<Double> abandonsPerquisition = new ArrayList<>();
        double firstCompletionTicketCheck = 0;
        double perquisitionFirstCompletetion = 0;

        ImprovedVIPComputationalModel m = new ImprovedVIPComputationalModel();
        List<Integer> perquisitionPriorityClassService = new ArrayList<>();

        for (int i = 0; i < SERVERS_VIP_PERQUISITION; i++) {
            perquisitionPriorityClassService.add(0);
        }


        Rngs r = new Rngs();
        r.plantSeeds(0);


        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 922, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        VIPMsqEvent[] event = new VIPMsqEvent[ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION];
        VIPMsqSum[] sum = new VIPMsqSum[ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION];
        for (s = 0; s < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION; s++) {
            event[s] = new VIPMsqEvent();
            sum[s] = new VIPMsqSum();
        }

        VIPMsqT t = new VIPMsqT();
        t.current = START;

        event[0].t = m.getArrival(r, t.current);
        event[0].x = 1;

        for (s = 1; s < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION; s++) {
            event[s].t = START;          /* this value is arbitrary because */
            event[s].x = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served = 0;
        }

        while ((event[0].x != 0) || (numberTicketCheck + classOneTotalPopulation + classTwoTotalPopulation != 0)) {

            if (!abandonsTicket.isEmpty()) {
                event[SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].t = abandonsTicket.get(0);
                event[SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 1;     // activate abandon
            } else {
                event[SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 0;     // deactivate abandon
            }

            if (!abandonsPerquisition.isEmpty()){
                event[ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].t = abandonsPerquisition.get(0);  //TODO migliora
                event[ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 1;
            } else {
                event[ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 0;
            }

            e = m.nextEvent(event);                                         /* next event index */
            t.next = event[e].t;                                            /* next event time  */
            areaTicketCheck += (t.next - t.current) * numberTicketCheck;    /* update integral  */
            areaPerquisition += (t.next - t.current) * (classOneTotalPopulation + classTwoTotalPopulation);  /* update integral  */
            t.current = t.next;                                             /* advance the clock*/

            if (e == ARRIVAL_EVENT_VIP_TICKET - 1) {
                /* ticket check arrival */

                numberTicketCheck++;

                event[0].t = m.getArrival(r, t.current);
                if (event[0].t > STOP)
                    event[0].x = 0;

                if (numberTicketCheck <= SERVERS_VIP_TICKET) {
                    service = m.getService(r,0, V_TC_SR);
                    s = m.findOneTicketCheck(event);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1;
                }
            }

            else if (e == SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET) {
                // ticket check abandon
                abandonTicketCheck++;
                abandonsTicket.remove(0);
            }

            else if (e == ALL_EVENTS_VIP_TICKET){
                /* perquisition arrival */
                event[ALL_EVENTS_VIP_TICKET].x = 0;

                // generate, with probability P4, an abandon
                // (after eventually failed ticket check)
                boolean abandon = generateAbandon(r, streamIndex, P4);
                if (abandon) {  // add an abandon
                    double abandonTime = t.current + 0.01;  // this will be the next abandon time (it must be small in order to execute the abandon as next event)
                    abandonsTicket.add(abandonTime);
                } else{
                    // if there isn't abandon process arrival
                    service = m.getService(r, 4, V_P_SR);
                    if (service < 10) {
                        /* first queue */
                        classOneTotalPopulation++;
                    } else {
                        /* second queue */
                        classTwoTotalPopulation++;
                    }
                    if (classOneTotalPopulation + classTwoTotalPopulation <= SERVERS_VIP_PERQUISITION) {
                        /* the total node population is below the total number of servers */
                        s = m.findPerquisition(event);

                        /* mark s as service for the first/second queue */
                        // bind server index with job type into the array
                        if (service < 10) {
                            try {
                                // small job ( without bags)
                                perquisitionPriorityClassService.set(s - (ALL_EVENTS_VIP_TICKET + 1), 1);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        else {
                            try {
                                // big job
                                perquisitionPriorityClassService.set(s - (ALL_EVENTS_VIP_TICKET + 1), 2);
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
                        if (service<10)
                            firstClassJobInQueue ++;
                        else secondClassJobInQueue++;
                    }


                }


            }

            else if ((ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION <= e) && (e < ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + SERVERS_VIP_PERQUISITION)) {
                boolean isFromFirstQueue = (perquisitionPriorityClassService.get(e - (ALL_EVENTS_VIP_TICKET   + 1)) == 1);
                if (isFromFirstQueue)
                    classOneTotalPopulation--;
                else
                    classTwoTotalPopulation--;

                    // perquisition service
                if (perquisitionFirstCompletetion == 0)
                    perquisitionFirstCompletetion = t.current;



                boolean abandon = generateAbandon(r, streamIndex, P5);
                indexPerquisition++;
                if (abandon) {
                    double abandonTime = t.current + 0.01;
                    abandonsPerquisition.add(abandonTime);
                }


                s = e;
                // Now, there is idle server, so re-generate service time for queued job
                if (firstClassJobInQueue >= 1) {
                    firstClassJobInQueue --;
                    // GENERATE A SERVICE LESS THAN 10 SECONDS
                    do {
                        service = m.getService(r, 160, V_P_SR);
                    } while (!(service < 10));
                    perquisitionPriorityClassService.set(s - (ALL_EVENTS_VIP_TICKET + 1), 1);
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
                    perquisitionPriorityClassService.set(s - (ALL_EVENTS_VIP_TICKET + 1), 2);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1;
                } else
                    event[s].x = 0;
            }


            else if (e == ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION) {
                // perquisition abandon
                abandonPerquisition++;
                abandonsPerquisition.remove(0);
            }

            else {
                /* ticket check service */
                if (firstCompletionTicketCheck == 0)
                    firstCompletionTicketCheck = t.current;
                indexTicketCheck++;
                numberTicketCheck--;
                event[ALL_EVENTS_VIP_TICKET].t = t.current;
                event[ALL_EVENTS_VIP_TICKET].x = 1;

                s = e;
                if (numberTicketCheck >= SERVERS_VIP_TICKET) {     // there are jobs in queue
                    service = m.getService(r, 3,V_TC_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                } else
                    event[s].x = 0;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + indexTicketCheck + " jobs the VIP ticket check statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(event[ARRIVAL_EVENT_VIP_TICKET-1].t / indexTicketCheck));
        System.out.println("  avg wait ........... =   " + f.format(areaTicketCheck / indexTicketCheck));

        double ticketCheckFinalTime = 0;
        for (s = 1; s <= SERVERS_VIP_TICKET; s++) {
            if (event[s].t > ticketCheckFinalTime)
                ticketCheckFinalTime = event[s].t;
        }

        double ticketCheckActualTime = ticketCheckFinalTime - firstCompletionTicketCheck;

        System.out.println("  avg # in node ...... =   " + f.format(areaTicketCheck / ticketCheckActualTime));

        System.out.println("# abandons: " + abandonTicketCheck);

        for (s = 1; s <= SERVERS_VIP_TICKET; s++)          /* adjust area to calculate */
            areaTicketCheck -= sum[s].service;              /* averages for the queue   */

        System.out.println("  avg delay: " + areaTicketCheck / indexTicketCheck);

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");

        double allServices = 0;
        double allServed = 0;

        for (s = 1; s <= SERVERS_VIP_TICKET; s++) {
            System.out.print("       " + (s) + "          " + g.format(sum[s].service / ticketCheckActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexTicketCheck));
            allServices += sum[s].service;
            allServed += sum[s].served;
        }

        System.out.println("  avg service time: " + g.format(allServices / allServed));

        System.out.println("");

        /*  the following line is relative to the single server case
        double perquisitionActualTime = event[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION].t - perquisitionFirstCompletetion;    */

        double perquisitionFinalTime = 0;
        double perquisitionMean = 0;
        for (s = ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION; s <= ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + SERVERS_VIP_PERQUISITION; s++) {
            perquisitionMean += event[s].t;
            if (event[s].t > perquisitionFinalTime)
                perquisitionFinalTime = event[s].t;
        }

        double perquisitionActualTime = perquisitionFinalTime + perquisitionFirstCompletetion;

        System.out.println("\nfor " + indexPerquisition + " jobs the VIP perquisition statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(perquisitionActualTime  / indexPerquisition));
        System.out.println("  avg wait ........... =   " + f.format(areaPerquisition / indexPerquisition));
        System.out.println("  avg # in node ...... =   " + f.format(areaPerquisition / perquisitionActualTime));

        System.out.println("# abandons = " + abandonPerquisition);

        for (s = ALL_EVENTS_VIP_TICKET + 1; s < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION -1; s++) {
            areaPerquisition -= sum[s].service;              /* averages for the queue   */
        }

        System.out.println("  avg delay : " + areaPerquisition / indexPerquisition);

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");

        for (s = ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION; s < ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + SERVERS_VIP_PERQUISITION; s++) {
            System.out.print("       " + (s - 6) + "          " + g.format(sum[s].service / perquisitionActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexPerquisition));
        }

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
        while (i < ALL_EVENTS_VIP_TICKET + ALL_EVENTS_VIP_PERQUISITION - 1) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }

    int findOneTicketCheck(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < SERVERS_VIP_TICKET) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    int findPerquisition(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_VIP_TICKET + 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_VIP_TICKET + SERVERS_VIP_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

}
