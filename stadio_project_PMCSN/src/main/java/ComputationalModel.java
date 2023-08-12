import libraries.Rngs;
import model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

import static model.Constants.TC_SR;
import static model.Events.*;

import static model.Constants.*;



/*
 *  Network:
 *  ----> ticket check (M|M|5) -----------> first perquisition (M|M|3) ----------> turnstiles (8 M|M|1) ----> second perquisition (M|M|2) -------------->
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
    static double STOP = 3 * 3600;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) {
        /* stream index for the rng */
        int streamIndex = 1;

        /* population counter for every node */
        long numberTicketCheck = 0;
        long numberFirstPerquisition = 0;
        long[] numberTurnstiles = {0, 0, 0, 0, 0, 0, 0, 0};
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
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 1013, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        /* events array initialization */
        MsqEvent[] events = new MsqEvent[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION];

        /*  indexes:
         *  0: arrival at the ticket check
         *  1, 2, 3, 4, 5: ticket check service
         *  6: abandon after ticket check
         *  7: arrival at the first perquisition
         *  8, 9, 10: first perquisition service
         *  11: abandon after first perquisition
         *  12, 13, 14, 15, 16, 17, 18, 19: arrival at one of the turnstiles
         *  20, 21, 22, 23, 24, 25, 26, 27: turnstiles service
         *  28: arrival at the second perquisition
         *  29, 30: second perquisition service
         *  31: abandon after second perquisition
         */

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

        int populationTurnstiles = 0;

        /* START ITERATION */
        while ((events[0].x != 0) || (numberTicketCheck + numberFirstPerquisition + populationTurnstiles + numberSecondPerquisition != 0)) {

            for (long num: numberTurnstiles) {
                populationTurnstiles += num;
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

            /* todo: is this correct? */
            long totalNumberTurnstiles = 0;
            for (long num : numberTurnstiles) {
                totalNumberTurnstiles += num;
            }
            areaTurnstiles += (t.next - t.current) * totalNumberTurnstiles;

            areaSecondPerquisition += (t.next - t.current) * numberSecondPerquisition;

            t.current = t.next;     /* advance the clock */

            if (e == ARRIVAL_EVENT_TICKET - 1) {    /* process an arrival (to the ticket check) */
                numberTicketCheck++;

                /* generate the next arrival */
                events[0].t = m.getArrival(r, t.current);
                if (events[0].t > STOP)
                    events[0].x = 0;

                /* if there's no queue, put a job on service */
                if (numberTicketCheck <= DEPARTURE_EVENT_TICKET) {
                    service = m.getService(r, 1/TC_SR);
                    s = m.findOneTicketCheck(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                }
            }

            else if (e == DEPARTURE_EVENT_TICKET + ABANDON_EVENT_TICKET) {      /* process an abandon (following the ticket check) */
                abandonsCounterTicketCheck++;
                abandonsTicketCheck.remove(0);
            }

            else if (e == ALL_EVENTS_TICKET) {      /* process a departure from ticket check (i.e. arrival to first perquisition) */

                events[ALL_EVENTS_TICKET].x = 0;    // todo is this correct?

                /* generate an abandon with probability P1 */
                boolean abandon = generateAbandon(r, streamIndex, P1);

                /* skip first perquisition with probability P4 */
                boolean skip = generateAbandon(r, streamIndex + 1, P4);

                if (abandon) {

                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsTicketCheck.add(abandonTime);

                } else if (skip) {

                    skipCounterFirstPerquisition++;

                    /* the first perquisition is skipped, so we generate an arrival at the turnstiles */
                    events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t = t.current;
                    events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].x = 1;

                } else {
                    /* no abandon -> arrival at the first perquisition */

                    numberFirstPerquisition++;
                    if (numberFirstPerquisition <= DEPARTURE_EVENT_FIRST_PERQUISITION) {
                        service = m.getService(r, 1 / P_SR);
                        s = m.findOneFirstPerquisition(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;
                    }
                }
            } else if ((e > ALL_EVENTS_TICKET) && (e <= ALL_EVENTS_TICKET + DEPARTURE_EVENT_FIRST_PERQUISITION)) {  /* service on one of the first perquisition servers (e = 8, 9, 10) */

                if (firstPerquisitionFirstCompletion == 0)
                    firstPerquisitionFirstCompletion = t.current;

                indexFirstPerquisition++;
                numberFirstPerquisition--;

                /* generate an arrival to one of the turnstiles */
                s = m.findOneTurnstiles(numberTurnstiles, events);
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + s].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + s].x = 1;

                /* if there's queue, put a job on service on this server */
                s = e;
                if (numberFirstPerquisition >= DEPARTURE_EVENT_FIRST_PERQUISITION) {
                    service = m.getService(r, 1/P_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;
                }

            } else if (e == ALL_EVENTS_TICKET + DEPARTURE_EVENT_FIRST_PERQUISITION + ABANDON_EVENT_FIRST_PERQUISITION) {    /* abandon after first perquisition */

                abandonsCounterFirstPerquisition++;
                abandonsFirstPerquisition.remove(0);

            } else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION) && (e < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + DEPARTURE_EVENT_TURNSTILES)) {  /* departure from first perquisition (i.e. arrival at turnstiles) */

                events[e].x = 0;

                /* generate an abandon with probability P2 */
                boolean abandon = generateAbandon(r, streamIndex, P2);
                if (abandon) {

                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsFirstPerquisition.add(abandonTime);

                } else {

                    /* arrival at the e-th turnstile */
                    int index = e - 12;
                    numberTurnstiles[index]++;

                    if (numberTurnstiles[index] == 1) {
                        /* the new job can be processed on this server */
                        sum[e + DEPARTURE_EVENT_TURNSTILES].served++;
                        service = m.getService(r, 1/T_SR);
                        sum[e + DEPARTURE_EVENT_TURNSTILES].service += service;
                        events[e + DEPARTURE_EVENT_TURNSTILES].t = t.current + service;
                        events[e + DEPARTURE_EVENT_TURNSTILES].x = 1;
                    }
                }

            } else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ARRIVAL_EVENT_TURNSTILES) && (e < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ARRIVAL_EVENT_TURNSTILES + DEPARTURE_EVENT_TURNSTILES)) {

                /* service at the turnstile with index (e - 20) */
                indexTurnstiles++;
                int index = e - 20;
                numberTurnstiles[index]--;

                /* generate an arrival at the second perquisition center */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].x = 1;

                s = e;
                if (numberTurnstiles[index] >= 1) {
                    /* there's a job in queue to be processed */
                    service = m.getService(r, 1/P_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES) {

                /* arrival at the second perquisition node */
                boolean skip = generateAbandon(r, streamIndex, P5);
                if (skip) {
                    continue;
                } else {
                    /* no skip, the job enters the node */
                    events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].x = 0;

                    numberSecondPerquisition++;
                    if (numberSecondPerquisition <= DEPARTURE_EVENT_SECOND_PERQUISITION) {
                        /* no queue -> the job can be processed immediately */
                        service = m.getService(r, 1/P_SR);
                        s = m.findOneSecondPerquisition(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;
                    }
                }

            } else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_SECOND_PERQUISIION)
                        && (e < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_SECOND_PERQUISIION + DEPARTURE_EVENT_SECOND_PERQUISITION)
            ) {
                /* second perquisition service */
                boolean abandon = generateAbandon(r, streamIndex, P3);
                if (abandon) {
                    double abandonTime = t.current + 0.01;
                    abandonsSecondPerquisition.add(abandonTime);
                } else {
                    numberSecondPerquisition--;
                    indexSecondPerquisition++;

                    s = e;
                    if (numberSecondPerquisition >= DEPARTURE_EVENT_SECOND_PERQUISITION) {
                        service = m.getService(r, 1/P_SR);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                    } else {
                        events[s].x = 0;
                    }
                }
            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 1) {
                /* abandon after second perquisition */
                abandonsCounterSecondPerquisition++;
                abandonsSecondPerquisition.remove(0);

            } else {    /* ticket check server (i.e, e = 1, ..., 5) */
                if (ticketCheckFirstCompletion == 0)
                    ticketCheckFirstCompletion = t.current;

                indexTicketCheck++;
                numberTicketCheck--;

                /* generate an arrival at the first perquisition */
                events[ALL_EVENTS_TICKET].t = t.current;
                events[ALL_EVENTS_TICKET].x = 1;

                /* if there's queue, put a job in queue on service on this server */
                s = e;
                if (numberTicketCheck >= DEPARTURE_EVENT_TICKET) {
                    service = m.getService(r, 1/TC_SR);
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

    }

    // this function generate a true value with (percentage * 100) % probability, oth. false
    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(1 + streamIndex);
        return rngs.random() <= percentage;
    }

    /* this function returns the available turnstile server idle longest  */
    int findOneTurnstiles(long[] numTurnstiles, MsqEvent[] events) {
        int s = 0;
        for (int i = 0; i < numTurnstiles.length; i++) {
            if ((numTurnstiles[i] <= numTurnstiles[s]) && (events[i].t < events[s].t))
                s = i;
        }
        return s;
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
        while (i < DEPARTURE_EVENT_TICKET) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
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
        while (i < ALL_EVENTS_TICKET + DEPARTURE_EVENT_FIRST_PERQUISITION) {         /* now, check the others to find which   */
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
        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_SECOND_PERQUISIION + 1) {         /* now, check the others to find which   */
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


