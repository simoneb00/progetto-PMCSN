import libraries.Rngs;
import model.TimeSlot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static model.Constants.TC_SR;
import static model.Events.*;

import static model.Constants.*;



/*
 *  Network:
 *  ----> ticket check ----------------------> first perquisition ------------> turnstiles ---------------------> second perquisition ------------------>
 *                               |      |                               ^    |                            |                                  ^   |
 *                               | P1   |              P4               |    | P2                         |                P5                |   | P3
 *                               V      |_______________________________|    V                            |__________________________________|   V
 */


class MsqT {
    double current;                   /* current time                       */
    double next;                      /* next (most imminent) event time    */
}

class MsqSum {                      /* accumulated sums of                */
    double service;                   /*   service times                    */
    long served;                    /*   number served                    */
}

class MsqEvent {                     /* the next-event list    */
    double t;                         /*   next event time      */
    int x;                         /*   event status, 0 or 1 */
}

class Msq {

    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = 1 * 3600;        /* terminal (close the door) time  todo verification (only one hour)*/
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) {
        /* stream index for the rng */
        int streamIndex = 1;

        /* population counter for every node */
        long numberTicketCheck = 0;
        long numberFirstPerquisition = 0;
        long numberTurnstiles = 0;
        long numberSecondPerquisition = 0;

        int e;      /* next event index */
        int s;      /* server index */

        /* processed jobs counter for every node */
        long indexTicketCheck = 0;
        long indexFirstPerquisition = 0;
        long indexTurnstiles = 0;
        long indexSecondPerquisition = 0;

        /* time integrated number for every node */
        double areaTicketCheck = 0.0;
        double areaFirstPerquisition = 0.0;
        double areaTurnstiles = 0.0;
        double areaSecondPerquisition = 0.0;

        double service;     /* it will contain the service times */

        /* abandons counter for ticket check and perquisitions */
        long abandonsCounterTicketCheck = 0;
        long abandonsCounterFirstPerquisition = 0;
        long abandonsCounterSecondPerquisition = 0;

        /* abandons list for ticket check and perquisitions */
        List<Double> abandonsTicketCheck = new ArrayList<>();
        List<Double> abandonsFirstPerquisition = new ArrayList<>();
        List<Double> abandonsSecondPerquisition = new ArrayList<>();

        /* skip list for perquisitions */
        List<Double> skipsFirstPerquisition = new ArrayList<>();
        List<Double> skipsSecondPerquisition = new ArrayList<>();

        /* skip counter for perquisitions */
        long skipCounterFirstPerquisition = 0;
        long skipCounterSecondPerquisition = 0;

        /* first completion for every node */
        double ticketCheckFirstCompletion = 0;
        double firstPerquisitionFirstCompletion = 0;
        double turnstilesFirstCompletion = 0;
        double secondPerquisitionFirstCompletion = 0;

        Msq m = new Msq();
        Rngs r = new Rngs();
        r.plantSeeds(0);

        /* time slots initialization */

        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 12062, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        /* events array initialization */
        MsqEvent[] events = new MsqEvent[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION];


        /* sum array initialization (to keep track of services) */
        MsqSum[] sum = new MsqSum[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION];
        for (s = 0; s < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION; s++) {
            events[s] = new MsqEvent();
            sum[s] = new MsqSum();
        }

        /* clock initialization */
        MsqT t = new MsqT();
        t.current = START;

        /* generating the first arrival */
        events[0].t = m.getArrival(r, t.current);
        events[0].x = 1;

        /* all other servers are initially idle */
        for (s = 1; s < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION; s++) {
            events[s].t = START;
            events[s].x = 0;
            sum[s].service = 0.0;
            sum[s].served = 0;
        }



        /* START ITERATION */

        while ((events[0].x != 0) || (numberTicketCheck + numberFirstPerquisition + numberTurnstiles + numberSecondPerquisition != 0)) {

            /* skip */
            if (!skipsFirstPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 2].t = skipsFirstPerquisition.get(0);
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 2].x = 1;
            } else {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 2].x = 0;
            }

            if (!skipsSecondPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 2].t = skipsSecondPerquisition.get(0);
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 2].x = 1;
            } else {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 2].x = 0;
            }

            /* abandons */
            if (!abandonsTicketCheck.isEmpty()) {
                events[ALL_EVENTS_TICKET - 1].t = abandonsTicketCheck.get(0);
                events[ALL_EVENTS_TICKET - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET - 1].x = 0;
            }

            if (!abandonsFirstPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 1].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 1].x = 0;
            }

            if (!abandonsSecondPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 1].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 1].x = 0;
            }

            e = m.nextEvent(events);    /* next event index */
            t.next = events[e].t;       /* next event time */


            /* update integrals */
            areaTicketCheck += (t.next - t.current) * numberTicketCheck;
            areaFirstPerquisition += (t.next - t.current) * numberFirstPerquisition;
            areaTurnstiles += (t.next - t.current) * numberTurnstiles;
            areaSecondPerquisition += (t.next - t.current) * numberSecondPerquisition;


            t.current = t.next;     /* advance the clock */

            if (e == ARRIVAL_EVENT_TICKET - 1) {    /* process an arrival (to the ticket check) */
                numberTicketCheck++;

                /* generate the next arrival */
                events[ARRIVAL_EVENT_TICKET - 1].t = m.getArrival(r, t.current);
                if (events[ARRIVAL_EVENT_TICKET - 1].t > STOP)
                    events[ARRIVAL_EVENT_TICKET - 1].x = 0;

                /* if there's no queue, put a job on service */
                if (numberTicketCheck <= SERVERS_TICKET) {
                    service = m.getService(r, 0, TC_SR);
                    s = m.findOneTicketCheck(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                }
            } else if (e == ALL_EVENTS_TICKET - 1) {      /* process an abandon (following the ticket check) */

                abandonsCounterTicketCheck++;
                abandonsTicketCheck.remove(0);

            } else if (e == ALL_EVENTS_TICKET) {      /* arrival at first perquisition */

                events[ALL_EVENTS_TICKET].x = 0;

                /* generate an abandon with probability P1 */
                boolean abandon = generateAbandon(r, streamIndex, P1);

                if (abandon) {
                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsTicketCheck.add(abandonTime);

                } else {
                    /* no abandon -> arrival at the first perquisition */

                    numberFirstPerquisition++;
                    if (numberFirstPerquisition <= SERVERS_FIRST_PERQUISITION) {
                        service = m.getService(r, 32, P_SR);
                        s = m.findOneFirstPerquisition(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;
                    }
                }
            } else if ((e > ALL_EVENTS_TICKET) && (e <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION)) {  /* service on one of the first perquisition servers */

                /* skip first perquisition with probability x %, depending on the number of people in queue */
                boolean skip = generateSkip(r, streamIndex, numberFirstPerquisition - SERVERS_FIRST_PERQUISITION);

                if (skip) {

                    double skipTime = t.current + 0.01;
                    skipsFirstPerquisition.add(skipTime);

                } else {

                    if (firstPerquisitionFirstCompletion == 0)
                        firstPerquisitionFirstCompletion = t.current;

                    indexFirstPerquisition++;
                    numberFirstPerquisition--;

                    /* generate an abandon with probability P2 */
                    boolean abandon = generateAbandon(r, streamIndex, P2);
                    if (abandon) {

                        /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                        double abandonTime = t.current + 0.01;
                        abandonsFirstPerquisition.add(abandonTime);

                    } else {
                        /* generate an arrival at the turnstiles */
                        events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t = t.current;
                        events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].x = 1;
                    }
                }

                /* if there's queue, put a job on service on this server */
                s = e;
                if (numberFirstPerquisition >= SERVERS_FIRST_PERQUISITION) {
                    service = m.getService(r, 32, P_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;

                }

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 2) {    /* skip first perquisition */
                indexFirstPerquisition++;
                numberFirstPerquisition--;
                skipCounterFirstPerquisition++;
                skipsFirstPerquisition.remove(0);

                /* generate an arrival at turnstiles */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].x = 1;

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION - 1) {    /* abandon after first perquisition */

                abandonsCounterFirstPerquisition++;
                abandonsFirstPerquisition.remove(0);

            } else if ((e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION)) {  /* arrival at turnstiles */

                events[e].x = 0;

                numberTurnstiles++;
                if (numberTurnstiles <= SERVERS_TURNSTILES) {
                    service = m.getService(r, 96, T_SR);
                    s = m.findOneTurnstiles(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;

                }

            } else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ARRIVAL_EVENT_TURNSTILES) && (e < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ARRIVAL_EVENT_TURNSTILES + SERVERS_TURNSTILES)) {

                /* service at the turnstile e */

                if (turnstilesFirstCompletion == 0)
                    turnstilesFirstCompletion = t.current;

                indexTurnstiles++;
                numberTurnstiles--;

                /* generate an arrival at the second perquisition center */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].x = 1;

                s = e;
                if (numberTurnstiles >= SERVERS_TURNSTILES) {
                    /* there's a job in queue to be processed */
                    service = m.getService(r, 96, T_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES) {    /* arrival at second perquisition */

                /* arrival at the second perquisition node */

                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].x = 0;

                numberSecondPerquisition++;
                if (numberSecondPerquisition <= SERVERS_SECOND_PERQUISITION) {
                    /* no queue -> the job can be processed immediately */
                    service = m.getService(r, 160, P_SR);
                    s = m.findOneSecondPerquisition(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                }

            } else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_SECOND_PERQUISIION)
                    && (e < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_SECOND_PERQUISIION + SERVERS_SECOND_PERQUISITION)
            ) {

                boolean skip = generateSkip(r, streamIndex, numberSecondPerquisition - SERVERS_SECOND_PERQUISITION);
                if (skip) {

                    double skipTime = t.current + 0.01;
                    skipsSecondPerquisition.add(skipTime);

                } else {

                    if (secondPerquisitionFirstCompletion == 0)
                        secondPerquisitionFirstCompletion = t.current;

                    indexSecondPerquisition++;
                    numberSecondPerquisition--;

                    /* abandons are generated only if the perquisition has not been skipped */
                    boolean abandon = generateAbandon(r, streamIndex, P3);
                    if (abandon) {
                        double abandonTime = t.current + 0.02;      // 0.02 not to overlap an eventual skip
                        abandonsSecondPerquisition.add(abandonTime);
                    }
                }

                s = e;
                if (numberSecondPerquisition >= SERVERS_SECOND_PERQUISITION) {
                    service = m.getService(r, 160, P_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }


            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 2) {
                numberSecondPerquisition--;
                indexSecondPerquisition++;
                skipCounterSecondPerquisition++;
                skipsSecondPerquisition.remove(0);

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 1) {
                /* abandon after second perquisition */
                abandonsCounterSecondPerquisition++;
                abandonsSecondPerquisition.remove(0);

            } else {    /* ticket check service */
                if (ticketCheckFirstCompletion == 0)
                    ticketCheckFirstCompletion = t.current;

                indexTicketCheck++;
                numberTicketCheck--;

                /* generate an arrival at the first perquisition */
                events[ALL_EVENTS_TICKET].t = t.current;
                events[ALL_EVENTS_TICKET].x = 1;

                /* if there's queue, put a job in queue on service on this server */
                s = e;
                if (numberTicketCheck >= SERVERS_TICKET) {
                    service = m.getService(r, 0, TC_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;
                }
            }

        }

        System.out.println("END OF ITERATION");

        /* the code below prints the simulation's statistics, for every node */

        DecimalFormat f = new DecimalFormat("###0.0000");
        DecimalFormat g = new DecimalFormat("###0.000000");


        /* TICKET CHECK */

        System.out.println("\nfor " + indexTicketCheck + " jobs the ticket check statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ARRIVAL_EVENT_TICKET - 1].t / indexTicketCheck));
        System.out.println("  avg response time .. =   " + f.format(areaTicketCheck / indexTicketCheck));

        double ticketCheckFinalTime = 0;
        double ticketCheckMean = 0;
        for (s = 1; s <= SERVERS_TICKET; s++) {
            ticketCheckMean += events[s].t;
            if (events[s].t > ticketCheckFinalTime)
                ticketCheckFinalTime = events[s].t;
        }

        double ticketCheckActualTime = ticketCheckFinalTime - ticketCheckFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaTicketCheck / ticketCheckActualTime));

        System.out.println("# abandons: " + abandonsCounterTicketCheck);

        for (s = 1; s <= SERVERS_TICKET; s++)          /* adjust area to calculate */
            areaTicketCheck -= sum[s].service;                 /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(areaTicketCheck / indexTicketCheck));
        System.out.println("  avg # in queue ..... =   " + f.format(areaTicketCheck / ticketCheckActualTime));

        double sumUtilizations = 0.0;
        double sumServices = 0.0;
        double sumServed = 0.0;

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 1; s <= SERVERS_TICKET; s++) {
            sumUtilizations += sum[s].service / ticketCheckActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
            System.out.print("       " + (s) + "          " + g.format(sum[s].service / ticketCheckActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexTicketCheck));
        }

        System.out.println("Mean utilization: " + g.format(sumUtilizations / SERVERS_TICKET));
        System.out.println("Avg service time = " + g.format(sumServices / sumServed));

        System.out.println("");


        /* FIRST PERQUISITION */

        System.out.println("\nfor " + indexFirstPerquisition + " jobs the first perquisition statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ALL_EVENTS_TICKET].t / indexFirstPerquisition));
        System.out.println("  avg response time ........... =   " + f.format(areaFirstPerquisition / indexFirstPerquisition));

        double firstPerquisitionFinalTime = 0;
        double firstPerquisitionMean = 0;
        for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++) {
            firstPerquisitionMean += events[s].t;
            if (events[s].t > firstPerquisitionFinalTime)
                firstPerquisitionFinalTime = events[s].t;
        }

        double firstPerquisitionActualTime = firstPerquisitionFinalTime - firstPerquisitionFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaFirstPerquisition / firstPerquisitionActualTime));

        System.out.println("# abandons: " + abandonsCounterFirstPerquisition);
        System.out.println("# skips: " + skipCounterFirstPerquisition);

        for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++)          /* adjust area to calculate */
            areaFirstPerquisition -= sum[s].service;                                                                     /* averages for the queue   */


        System.out.println("  avg delay .......... =   " + f.format(areaFirstPerquisition / indexFirstPerquisition));
        System.out.println("  avg # in queue ..... =   " + f.format(areaFirstPerquisition / firstPerquisitionActualTime));

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++) {
            System.out.print("       " + (s - ALL_EVENTS_TICKET) + "          " + g.format(sum[s].service / firstPerquisitionActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexFirstPerquisition));
            sumUtilizations += sum[s].service / firstPerquisitionActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }
        System.out.println("avg utilization = " + g.format(sumUtilizations / SERVERS_FIRST_PERQUISITION));
        System.out.println("avg service = " + g.format(sumServices / sumServed));

        System.out.println("");


        /* TURNSTILES */

        System.out.println("\nfor " + indexTurnstiles + " jobs the turnstiles statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t / indexTurnstiles));
        System.out.println("  avg response time ........... =   " + f.format(areaTurnstiles / indexTurnstiles));

        double turnstilesFinalTime = 0;
        double turnstilesMean = 0;
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++) {
            turnstilesMean += events[s].t;
            if (events[s].t > turnstilesFinalTime)
                turnstilesFinalTime = events[s].t;
        }

        double turnstilesActualTime = turnstilesFinalTime - turnstilesFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaTurnstiles / turnstilesActualTime));


        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++)
            /* adjust area to calculate */
            areaTurnstiles -= sum[s].service;                                                                     /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(areaTurnstiles / indexTurnstiles));
        System.out.println("  avg # in queue ..... =   " + f.format(areaTurnstiles / indexTurnstiles));

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++) {
            System.out.print("       " + (s - ALL_EVENTS_TICKET - ALL_EVENTS_FIRST_PERQUISITION) + "          " + g.format(sum[s].service / turnstilesActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexTurnstiles));
            sumUtilizations += sum[s].service / turnstilesActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }
        System.out.println("avg utilization = " + g.format(sumUtilizations / SERVERS_TURNSTILES));
        System.out.println("avg service = " + g.format(sumServices / sumServed));

        System.out.println("");

        /* SECOND PERQUISITION */

        System.out.println("\nfor " + indexSecondPerquisition + " jobs the second perquisition statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t / indexSecondPerquisition));
        System.out.println("  avg response time .. =   " + f.format(areaSecondPerquisition / indexSecondPerquisition));

        double secondPerquisitionFinalTime = 0;
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++) {
            if (events[s].t > secondPerquisitionFinalTime)
                secondPerquisitionFinalTime = events[s].t;
        }

        double secondPerquisitionActualTime = secondPerquisitionFinalTime - secondPerquisitionFirstCompletion;

        System.out.println("  avg # in node ...... =   " + f.format(areaSecondPerquisition / secondPerquisitionActualTime));

        System.out.println("# abandons: " + abandonsCounterSecondPerquisition);
        System.out.println("# skips: " + skipCounterSecondPerquisition);

        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++)
            /* adjust area to calculate */
            areaSecondPerquisition -= sum[s].service;                                                                    /* averages for the queue   */

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++) {
            System.out.print("       " + (s - ALL_EVENTS_TICKET - ALL_EVENTS_FIRST_PERQUISITION - ALL_EVENTS_TURNSTILES) + "          " + g.format(sum[s].service / secondPerquisitionActualTime) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double) indexSecondPerquisition));
        }

        System.out.println("");

    }


    static boolean generateSkip(Rngs rngs, int streamIndex, long queueSize) {
        rngs.selectStream(64);
        double percentage = Math.min(0.8, (queueSize - 25.0) / 10.0);
        return rngs.random() <= percentage;
    }


    // this function generate a true value with (percentage * 100) % probability, oth. false
    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(128);
        return rngs.random() <= percentage;
    }

    /* this function returns the available turnstile server idle longest  */
    int findOneTurnstiles(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;

        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES) {         /* now, check the others to find which   */
            i++;                                                                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }

    int findOneTicketCheck(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = 1;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;


        while (i < SERVERS_TICKET) {         /* now, check the others to find which   */
            i++;                                     /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }

    int findOneFirstPerquisition(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + 1;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                      /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }

    int findOneSecondPerquisition(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_SECOND_PERQUISIION;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                      /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
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
        r.selectStream(192);

        // int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);
        int index = 0;  /* forcing the first time slot, for the verification step  todo verification step*/

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);

        return (sarrival);
    }

    double getService(Rngs r, int streamIndex, double serviceTime) {
        r.selectStream(streamIndex);
        return (exponential(serviceTime, r));
    }

    int nextEvent(MsqEvent[] event) {
        /* ---------------------------------------
         * return the index of the next event type
         * ---------------------------------------
         */
        int e;
        int i = 0;

        while (event[i].x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */
        e = i;
        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 1) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }


}


