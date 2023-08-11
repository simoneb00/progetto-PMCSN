/* -------------------------------------------------------------------------
 * This program is a next-event simulation of a multi-server, single-queue
 * service node.  The service node is assumed to be initially idle, no
 * arrivals are permitted after the terminal time STOP and the node is then
 * purged by processing any remaining jobs.
 *
 * Name              : Msq.java (Multi-Server Queue)
 * Authors           : Steve Park & Dave Geyer
 * Translated by     : Jun Wang
 * Language          : Java
 * Latest Revision   : 6-16-06
 * -------------------------------------------------------------------------
 */

import java.lang.*;
import java.text.*;
import java.util.ArrayList;
import java.util.List;

import libraries.*;
import model.*;

import static model.Constants.*;
import static model.Events.*;


class VIPMsqT {
    double current;                   /* current time                       */
    double next;                      /* next (most imminent) event time    */
}

class VIPMsqSum {                      /* accumulated sums of                */
    double service;                   /*   service times                    */
    long served;                    /*   number served                    */
}

class VIPMsqEvent {                     /* the next-event list    */
    double t;                         /*   next event time      */
    int x;                         /*   event status, 0 or 1 */
}


class VIPMsq {
    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = 3 * 3600;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) {

        int streamIndex = 1;
        long numberTicketCheck = 0;   /* number in the ticket check node    */
        long numberPerquisition = 0;  /* number in the perquisition node    */
        int e;                      /* next event index                    */
        int s;                      /* server index                        */
        long indexPerquisition = 0;            /* used to count processed jobs in perquisition        */
        long indexTicketCheck = 0;             /* used to count processed jobs in ticket check        */
        double area = 0.0;
        double areaTicketCheck = 0.0;           /* time integrated number in the node */
        double areaPerquisition = 0.0;           /* time integrated number in the node */
        double service;
        long abandonTicketCheck = 0;
        long abandonPerquisition = 0;
        List<Double> abandonsTicket = new ArrayList<>();
        List<Double> abandonsPerquisition = new ArrayList<>();
        double firstCompletionTicketCheck = 0;
        double firstCompletionPerquisition = 0;

        VIPMsq m = new VIPMsq();
        Rngs r = new Rngs();
        r.plantSeeds(0);


        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 1013, 3600 * f, 3600 * (f + 1) - 1);
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


        int count = 0;

        while ((event[0].x != 0) || (numberTicketCheck + numberPerquisition != 0)) {

            if (!abandonsTicket.isEmpty()) {
                event[DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].t = abandonsTicket.get(0);
                event[DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 1;     // activate abandon
            } else {
                event[DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET].x = 0;     // deactivate abandon
            }

            if (!abandonsPerquisition.isEmpty()){
                event[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].t = abandonsPerquisition.get(0);  //TODO migliora
                event[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 1;
            } else {
                event[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION].x = 0;
            }

            e = m.nextEvent(event);                                         /* next event index */
            t.next = event[e].t;                                            /* next event time  */
            areaTicketCheck += (t.next - t.current) * numberTicketCheck;    /* update integral  */
            areaPerquisition += (t.next - t.current) * numberPerquisition;  /* update integral  */
            t.current = t.next;                                             /* advance the clock*/

            if (e == ARRIVAL_EVENT_VIP_TICKET - 1) {     /* process an arrival */

                numberTicketCheck++;
                event[0].t = m.getArrival(r, t.current);
                if (event[0].t > STOP)
                    event[0].x = 0;
                if (numberTicketCheck <= DEPARTURE_EVENT_VIP_TICKET) {
                    service = m.getService(r, 1/V_TC_SR);
                    s = m.findOneTicketCheck(event);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t = t.current + service;
                    event[s].x = 1;
                }
            }

            else if (e == DEPARTURE_EVENT_VIP_TICKET + ABANDON_EVENT_VIP_TICKET) {    // process an abandon
                //numberTicketCheck--;
                abandonTicketCheck++;
                abandonsTicket.remove(0);
            }

            else if (e == ALL_EVENTS_VIP_TICKET){      /* process a departure (i.e. arrival to vip perquisition) */


                // generate, with probability P6, an abandon
                boolean abandon = generateAbandon(r, streamIndex, P6);
                if (abandon) {  // add an abandon
                    double abandonTime = t.current + 0.01;  // this will be the next abandon time (it must be small in order to execute the abandon as next event)
                    abandonsTicket.add(abandonTime);
                } else {    // arrival in the next servant (single server queue, perquisition)

                    event[ALL_EVENTS_VIP_TICKET].x = 0;

                    /* here a new arrival in the perquisition servant is handled */

                    numberPerquisition++;
                    if (numberPerquisition <= DEPARTURE_EVENT_VIP_PERQUISITION) {   // if false, there's queue
                        service = m.getService(r, 1/V_P_SR);
                        s = m.findOnePerquisition(event);
                        sum[s].service += service;
                        sum[s].served++;
                        event[s].t = t.current + service;
                        event[s].x = 1;

                    }

                    /*  The code below is relative to the single server variant

                    if (numberPerquisition == 1) {  // there is a job to process
                        sum[ALL_EVENTS_VIP_TICKET+ARRIVAL_EVENT_VIP_PERQUISIION].served++;
                        service = m.getService(r, 1/V_P_SR);
                        sum[ALL_EVENTS_VIP_TICKET+ARRIVAL_EVENT_VIP_PERQUISIION].service += service;
                        event[ALL_EVENTS_VIP_TICKET+ARRIVAL_EVENT_VIP_PERQUISIION].t = t.current + service;
                        event[ALL_EVENTS_VIP_TICKET+ARRIVAL_EVENT_VIP_PERQUISIION].x = 1;

                        if (firstCompletionPerquisition == 0)
                            firstCompletionPerquisition = t.current;
                    }

                     */
                }
            }

            else if ((ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION <= e) && (e < ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + DEPARTURE_EVENT_VIP_PERQUISITION)) {    // departure from perquisition
                boolean abandon = generateAbandon(r, streamIndex, P7);
                if (abandon) {
                    double abandonTime = t.current + 0.01;
                    abandonsPerquisition.add(abandonTime);
                } else {
                    numberPerquisition--;
                    indexPerquisition++;
                    s = e;

                    if (numberPerquisition >= DEPARTURE_EVENT_VIP_PERQUISITION) {
                        service = m.getService(r, 1/V_P_SR);
                        sum[s].service += service;
                        sum[s].served++;
                        event[s].t = t.current + service;
                    } else {
                        event[s].x = 0;
                    }
                }
            }

            else if (e == ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION) {
                numberPerquisition--;
                abandonPerquisition++;
                abandonsPerquisition.remove(0);
            }

            else {
                if (firstCompletionTicketCheck == 0)
                    firstCompletionTicketCheck = t.current;
                indexTicketCheck++;                                     /* departure from ticket check */
                numberTicketCheck--;
                s = e;
                event[ALL_EVENTS_VIP_TICKET].t = t.current;
                event[ALL_EVENTS_VIP_TICKET].x = 1;

                if (numberTicketCheck >= DEPARTURE_EVENT_VIP_TICKET) {     // there are jobs in queue
                    service = m.getService(r, 1/V_TC_SR);
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
        double ticketCheckMean = 0;
        for (s = 1; s <= DEPARTURE_EVENT_VIP_TICKET; s++) {
            ticketCheckMean += event[s].t;
            if (event[s].t > ticketCheckFinalTime)
                ticketCheckFinalTime = event[s].t;
        }

        double ticketCheckActualTime = ticketCheckFinalTime - firstCompletionTicketCheck;

        System.out.println("  avg # in node ...... =   " + f.format(areaTicketCheck / ticketCheckActualTime));

        System.out.println("# abandons: " + abandonTicketCheck);

        for (s = 1; s <= DEPARTURE_EVENT_VIP_TICKET; s++)          /* adjust area to calculate */
            area -= sum[s].service;              /* averages for the queue   */

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 1; s <= DEPARTURE_EVENT_VIP_TICKET; s++) {
            // todo vedere se con t.current cambia o va meglio
            System.out.print("       " + (s) + "          " + g.format(sum[s].service / ticketCheckActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexTicketCheck));
        }

        System.out.println("");

        /*  the following line is relative to the single server case
        double perquisitionActualTime = event[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION].t - firstCompletionPerquisition;    */

        double perquisitionFinalTime = 0;
        double perquisitionMean = 0;
        for (s = ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION; s <= ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + DEPARTURE_EVENT_VIP_PERQUISITION; s++) {
            perquisitionMean += event[s].t;
            if (event[s].t > perquisitionFinalTime)
                perquisitionFinalTime = event[s].t;
        }

        double perquisitionActualTime = perquisitionFinalTime + firstCompletionPerquisition;

        System.out.println("\nfor " + indexPerquisition + " jobs the VIP perquisition statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(perquisitionActualTime  / indexPerquisition));
        System.out.println("  avg wait ........... =   " + f.format(areaPerquisition / indexPerquisition));
        System.out.println("  avg # in node ...... =   " + f.format(areaPerquisition / perquisitionActualTime));

        System.out.println("# abandons = " + abandonPerquisition);

        areaPerquisition -= sum[ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION].service;              /* averages for the queue   */

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");

        for (s = ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION; s < ALL_EVENTS_VIP_TICKET + ARRIVAL_EVENT_VIP_PERQUISIION + DEPARTURE_EVENT_VIP_PERQUISITION; s++) {
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

    double uniform(double a, double b, Rngs r) {
        /* --------------------------------------------
         * generate a Uniform random variate, use a < b
         * --------------------------------------------
         */
        return (a + (b - a) * r.random());
    }

    double getArrival(Rngs r, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, exponential with rate given by the current time slot
         * --------------------------------------------------------------
         */
        r.selectStream(0);

        int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);

        // todo fix attempt: changed parameter (this should be ok)
        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);
        return (sarrival);
    }


    double getService(Rngs r, double serviceRate) {
        /* ------------------------------
         * generate the next service time, with rate 1/6
         * ------------------------------
         */
        r.selectStream(3);
        // todo fix attempt: changed parameter (this should be ok)
        return (exponential(1 / serviceRate, r));
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
        while (i < DEPARTURE_EVENT_VIP_TICKET) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

    int findOnePerquisition(VIPMsqEvent[] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = 7;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_VIP_TICKET + DEPARTURE_EVENT_VIP_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }


}
