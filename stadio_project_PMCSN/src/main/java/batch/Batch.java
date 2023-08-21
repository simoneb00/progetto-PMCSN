package batch;

import libraries.Rngs;
import model.TimeSlot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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


/*  Statistiche:
    * tempo di attesa
    * tempo di risposta
    * utilizzazione
    * numero di job nel sistema
    * tempo di interarrivo
    * numero di abbandoni
    * tempi di servizio
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

class Batch {

    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = Double.POSITIVE_INFINITY;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) {

        long batchCounter = 0;
        int k = 128;
        int b = 1024;

        /* lists for batch simulation */

        /* waiting times */
        List<Double> delaysTicketCheck = new ArrayList<>();
        List<Double> delaysFirstPerquisition = new ArrayList<>();
        List<Double> delaysTurnstiles = new ArrayList<>();
        List<Double> delaysSecondPerquisition = new ArrayList<>();

        /* response times */
        List<Double>  responseTimesTicketCheck = new ArrayList<>();
        List<Double>  responseTimesFirstPerquisition = new ArrayList<>();
        List<Double>  responseTimesTurnstiles = new ArrayList<>();
        List<Double>  responseTimesSecondPerquisition = new ArrayList<>();

        /* utilizations */
        List<Double> utilizationsTicketCheck = new ArrayList<>();
        List<Double> utilizationsFirstPerquisition = new ArrayList<>();
        List<Double> utilizationsTurnstiles = new ArrayList<>();
        List<Double> utilizationsSecondPerquisition = new ArrayList<>();

        /* system populations */
        List<Double> avgPopulationsTicketCheck = new ArrayList<>();
        List<Double> avgPopulationsFirstPerquisition = new ArrayList<>();
        List<Double> avgPopulationsTurnstiles = new ArrayList<>();
        List<Double> avgPopulationsSecondPerquisition = new ArrayList<>();

        /* interarrivals */
        List<Double> interarrivalsTicketCheck = new ArrayList<>();
        List<Double> interarrivalsFirstPerquisition = new ArrayList<>();
        List<Double> interarrivalsTurnstiles = new ArrayList<>();
        List<Double> interarrivalsSecondPerquisition = new ArrayList<>();

        /* abandons */
        List<Long> allAbandonsTicketCheck = new ArrayList<>();
        List<Long> allAbandonsFirstPerquisition = new ArrayList<>();
        List<Long> allAbandonsTurnstiles = new ArrayList<>();
        List<Long> allAbandonsSecondPerquisition = new ArrayList<>();

        /* service times */
        List<Double> serviceTimesTicketCheck = new ArrayList<>();
        List<Double> serviceTimesFirstPerquisition = new ArrayList<>();
        List<Double> serviceTimesTurnstiles = new ArrayList<>();
        List<Double> serviceTimesSecondPerquisition = new ArrayList<>();


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

        /* skip counter for perquisitions */
        long skipCounterFirstPerquisition = 0;
        long skipCounterSecondPerquisition = 0;

        /* first completion for every node */
        double ticketCheckFirstCompletion = 0;
        double firstPerquisitionFirstCompletion = 0;
        double turnstilesFirstCompletion = 0;
        double secondPerquisitionFirstCompletion = 0;

        Batch m = new Batch();
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

        double currentBatchStartingTime = 0;
        double currentFirstArrivalTimeTC = 0;
        double currentFirstArrivalTimeFP = 0;
        double currentFirstArrivalTimeT = 0;
        double currentFirstArrivalTimeSP = 0;

        /* START ITERATION */
        while ((events[0].x != 0)) {

            if (indexTicketCheck != 0 && indexTicketCheck % b == 0) {
                /* new batch */

                batchCounter++;


                /* TICKET CHECK */
                responseTimesTicketCheck.add(areaTicketCheck / indexTicketCheck);
                interarrivalsTicketCheck.add((events[ARRIVAL_EVENT_TICKET -1].t - currentFirstArrivalTimeTC) / indexTicketCheck);
                allAbandonsTicketCheck.add(abandonsCounterTicketCheck);


                double ticketCheckActualTime = t.current - currentBatchStartingTime;

                avgPopulationsTicketCheck.add(areaTicketCheck / ticketCheckActualTime);

                for (s = 1; s <= DEPARTURE_EVENT_TICKET; s++)          /* adjust area to calculate */
                    areaTicketCheck -= sum[s].service;                 /* averages for the queue   */

                delaysTicketCheck.add(areaTicketCheck / indexTicketCheck);

                double sumUtilizations = 0.0;
                double sumServices = 0.0;
                double sumServed = 0.0;


                for (s = 1; s <= DEPARTURE_EVENT_TICKET; s++) {
                    sumUtilizations += sum[s].service / ticketCheckActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsTicketCheck.add(sumUtilizations / DEPARTURE_EVENT_TICKET);
                serviceTimesTicketCheck.add(sumServices / sumServed);


                areaTicketCheck = 0;
                indexTicketCheck = 0;
                abandonsCounterTicketCheck = 0;

                for (s = 1; s <= DEPARTURE_EVENT_TICKET; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }



                /* FIRST PERQUISITION */

                responseTimesFirstPerquisition.add(areaFirstPerquisition / indexFirstPerquisition);
                interarrivalsFirstPerquisition.add((events[ALL_EVENTS_TICKET].t - currentFirstArrivalTimeFP) / indexFirstPerquisition);
                allAbandonsFirstPerquisition.add(abandonsCounterFirstPerquisition);


                double firstPerquisitionActualTime = t.current - currentBatchStartingTime;

                avgPopulationsFirstPerquisition.add(areaFirstPerquisition / firstPerquisitionActualTime);

                for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET  + DEPARTURE_EVENT_FIRST_PERQUISITION; s++)          /* adjust area to calculate */
                    areaFirstPerquisition -= sum[s].service;                                                                /* averages for the queue   */

                delaysFirstPerquisition.add(areaFirstPerquisition / indexFirstPerquisition);

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + DEPARTURE_EVENT_FIRST_PERQUISITION; s++) {
                    sumUtilizations += sum[s].service / firstPerquisitionActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsFirstPerquisition.add(sumUtilizations / DEPARTURE_EVENT_FIRST_PERQUISITION);
                serviceTimesFirstPerquisition.add(sumServices / sumServed);


                areaFirstPerquisition = 0;
                indexFirstPerquisition = 0;
                abandonsCounterFirstPerquisition = 0;

                for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + DEPARTURE_EVENT_FIRST_PERQUISITION; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }


                /* TURNSTILES */

                responseTimesTurnstiles.add(areaTurnstiles / indexTurnstiles);
                interarrivalsTurnstiles.add((events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t - currentFirstArrivalTimeT) / indexTurnstiles);

                double turnstilesActualTime = t.current - currentBatchStartingTime;

                avgPopulationsTurnstiles.add(areaTurnstiles / turnstilesActualTime);

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET  + ALL_EVENTS_FIRST_PERQUISITION + DEPARTURE_EVENT_TURNSTILES; s++)          /* adjust area to calculate */
                    areaTurnstiles -= sum[s].service;                                                                /* averages for the queue   */

                delaysTurnstiles.add(Math.max(0, areaTurnstiles / indexTurnstiles));

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET  + ALL_EVENTS_FIRST_PERQUISITION + DEPARTURE_EVENT_TURNSTILES; s++) {
                    sumUtilizations += sum[s].service / turnstilesActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsTurnstiles.add(sumUtilizations / DEPARTURE_EVENT_TURNSTILES);
                serviceTimesTurnstiles.add(sumServices / sumServed);

                areaTurnstiles = 0;
                indexTurnstiles = 0;

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET  + ALL_EVENTS_FIRST_PERQUISITION + DEPARTURE_EVENT_TURNSTILES; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }

                /* SECOND PERQUISITION */

                responseTimesSecondPerquisition.add(areaSecondPerquisition / indexSecondPerquisition);
                interarrivalsSecondPerquisition.add((events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t - currentFirstArrivalTimeSP) / indexSecondPerquisition);
                allAbandonsSecondPerquisition.add(abandonsCounterSecondPerquisition);


                double secondPerquisitionActualTime = t.current - currentBatchStartingTime;

                avgPopulationsSecondPerquisition.add(areaSecondPerquisition / secondPerquisitionActualTime);

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + DEPARTURE_EVENT_SECOND_PERQUISITION; s++)          /* adjust area to calculate */
                    areaSecondPerquisition -= sum[s].service;                                                                /* averages for the queue   */

                delaysSecondPerquisition.add(areaSecondPerquisition / indexSecondPerquisition);

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + DEPARTURE_EVENT_SECOND_PERQUISITION; s++) {
                    sumUtilizations += sum[s].service / secondPerquisitionActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsSecondPerquisition.add(sumUtilizations / DEPARTURE_EVENT_SECOND_PERQUISITION);
                serviceTimesSecondPerquisition.add(sumServices / sumServed);


                areaSecondPerquisition = 0;
                indexSecondPerquisition = 0;
                abandonsCounterSecondPerquisition = 0;

                for (s = ALL_EVENTS_TICKET +ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + DEPARTURE_EVENT_SECOND_PERQUISITION; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }


                /* final updates */
                currentBatchStartingTime = t.current;
                currentFirstArrivalTimeTC = events[0].t;
                currentFirstArrivalTimeFP = events[ALL_EVENTS_TICKET].t;
                currentFirstArrivalTimeT = events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t;
                currentFirstArrivalTimeSP = events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t;
            }

            if (batchCounter == k)
                break;


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
                events[0].t = m.getArrival(r, t.current);
                if (events[0].t > STOP)
                    events[0].x = 0;

                /* if there's no queue, put a job on service */
                if (numberTicketCheck <= DEPARTURE_EVENT_TICKET) {
                    service = m.getService(r, TC_SR);
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


                /* skip first perquisition with probability x %, depending on the number of people in queue */
                boolean skip = generateSkip(r, streamIndex, numberFirstPerquisition - DEPARTURE_EVENT_FIRST_PERQUISITION);

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
                        service = m.getService(r, P_SR);
                        s = m.findOneFirstPerquisition(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;
                    }
                }
            }
            else if ((e > ALL_EVENTS_TICKET) && (e <= ALL_EVENTS_TICKET + DEPARTURE_EVENT_FIRST_PERQUISITION)) {  /* service on one of the first perquisition servers (e = 8, 9, 10) */

                if (firstPerquisitionFirstCompletion == 0)
                    firstPerquisitionFirstCompletion = t.current;

                indexFirstPerquisition++;
                numberFirstPerquisition--;

                /* generate an arrival at the turnstiles */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].x = 1;

                /* if there's queue, put a job on service on this server */
                s = e;
                if (numberFirstPerquisition >= DEPARTURE_EVENT_FIRST_PERQUISITION) {
                    service = m.getService(r, P_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;
                }

            }
            else if (e == ALL_EVENTS_TICKET + DEPARTURE_EVENT_FIRST_PERQUISITION + ABANDON_EVENT_FIRST_PERQUISITION) {    /* abandon after first perquisition */

                abandonsCounterFirstPerquisition++;
                abandonsFirstPerquisition.remove(0);

            }
            else if ((e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION)) {  /* departure from first perquisition (i.e. arrival at turnstiles) */

                events[e].x = 0;

                /* generate an abandon with probability P2 */
                boolean abandon = generateAbandon(r, streamIndex, P2);
                if (abandon) {

                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsFirstPerquisition.add(abandonTime);

                } else {

                    /* no abandon -> arrival at the turnstiles */

                    numberTurnstiles++;
                    if (numberTurnstiles <= DEPARTURE_EVENT_TURNSTILES) {
                        service = m.getService(r, T_SR);
                        s = m.findOneTurnstiles(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;
                    }
                }

            }
            else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ARRIVAL_EVENT_TURNSTILES) && (e < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ARRIVAL_EVENT_TURNSTILES + DEPARTURE_EVENT_TURNSTILES)) {

                /* service at the turnstile e */

                if (turnstilesFirstCompletion == 0)
                    turnstilesFirstCompletion = t.current;

                indexTurnstiles++;
                numberTurnstiles--;


                /* generate an arrival at the second perquisition center */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].x = 1;

                s = e;
                if (numberTurnstiles >= DEPARTURE_EVENT_TURNSTILES) {
                    /* there's a job in queue to be processed */
                    service = m.getService(r, T_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }

            }
            else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES) {

                events[e].x = 0;

                /* arrival at the second perquisition node */
                boolean skip = generateSkip(r, streamIndex, numberSecondPerquisition - DEPARTURE_EVENT_SECOND_PERQUISITION);
                if (skip) {
                    skipCounterSecondPerquisition++;
                    continue;
                } else {
                    /* no skip, the job enters the node */
                    events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].x = 0;

                    numberSecondPerquisition++;
                    if (numberSecondPerquisition <= DEPARTURE_EVENT_SECOND_PERQUISITION) {
                        /* no queue -> the job can be processed immediately */
                        service = m.getService(r, P_SR);
                        s = m.findOneSecondPerquisition(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;
                    }
                }

            }
            else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_SECOND_PERQUISIION)
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
                        service = m.getService(r, P_SR);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                    } else {
                        events[s].x = 0;
                    }
                }
            }
            else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + ALL_EVENTS_SECOND_PERQUISITION - 1) {
                /* abandon after second perquisition */
                abandonsCounterSecondPerquisition++;
                abandonsSecondPerquisition.remove(0);

            }
            else {    /* ticket check service */
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
                    service = m.getService(r, TC_SR);
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


        /* BATCH SIMULATION RESULTS */

        System.out.println("Completed " + batchCounter + " batches");

        /* TICKET CHECK */

        double allRTs = 0;
        for (double rt : responseTimesTicketCheck) {
            allRTs += rt;
        }
        System.out.println("Mean response time for ticket check: " + allRTs / responseTimesTicketCheck.size());

        double allDelays = 0;
        for (double delay : delaysTicketCheck) {
            allDelays += delay;
        }
        System.out.println("Average queueing time for ticket check: " + allDelays / delaysTicketCheck.size());

        double allUtilizations = 0;
        for (double u : utilizationsTicketCheck) {
            allUtilizations += u;
        }
        System.out.println("Avg utilization for ticket check: " + allUtilizations / utilizationsTicketCheck.size());

        double allInterarrivals = 0;
        for (double i : interarrivalsTicketCheck) {
            allInterarrivals += i;
        }
        System.out.println("Avg interarrivals for ticket check: " + allInterarrivals / interarrivalsTicketCheck.size());

        double allAbandons = 0;
        for (double a : allAbandonsTicketCheck) {
            allAbandons += a;
        }
        System.out.println("Avg abandons for ticket check: " + allAbandons / allAbandonsTicketCheck.size());

        double allServiceTimes = 0;
        for (double st : serviceTimesTicketCheck) {
            allServiceTimes += st;
        }
        System.out.println("Avg service time for ticket check: " + allServiceTimes / serviceTimesTicketCheck.size());

        double allPopulations = 0;
        for (double pop : avgPopulationsTicketCheck) {
            allPopulations += pop;
        }
        System.out.println("Avg population for ticket check: " + allPopulations / avgPopulationsTicketCheck.size());


        System.out.println("");


        /* FIRST PERQUISITION */

        allRTs = 0;
        for (double rt : responseTimesFirstPerquisition) {
            allRTs += rt;
        }
        System.out.println("Mean response time for first perquisition: " + allRTs / responseTimesFirstPerquisition.size());

        allDelays = 0;
        for (double delay : delaysFirstPerquisition) {
            allDelays += delay;
        }
        System.out.println("Average queueing time for first perquisition: " + allDelays / delaysFirstPerquisition.size());

        allUtilizations = 0;
        for (double u : utilizationsFirstPerquisition) {
            allUtilizations += u;
        }
        System.out.println("Avg utilization for first perquisition: " + allUtilizations / utilizationsFirstPerquisition.size());

        allInterarrivals = 0;
        for (double i : interarrivalsFirstPerquisition) {
            allInterarrivals += i;
        }
        System.out.println("Avg interarrivals for first perquisition: " + allInterarrivals / interarrivalsFirstPerquisition.size());

        allAbandons = 0;
        for (double a : allAbandonsFirstPerquisition) {
            allAbandons += a;
        }
        System.out.println("Avg abandons for first perquisition: " + allAbandons / allAbandonsFirstPerquisition.size());

        allServiceTimes = 0;
        for (double st : serviceTimesFirstPerquisition) {
            allServiceTimes += st;
        }
        System.out.println("Avg service time for first perquisition: " + allServiceTimes / serviceTimesFirstPerquisition.size());

        allPopulations = 0;
        for (double pop : avgPopulationsFirstPerquisition) {
            allPopulations += pop;
        }
        System.out.println("Avg population for first perquisition: " + allPopulations / avgPopulationsFirstPerquisition.size());


        System.out.println("");

        /* TURNSTILES */

        allRTs = 0;
        for (double rt : responseTimesTurnstiles) {
            allRTs += rt;
        }
        System.out.println("Mean response time for turnstiles: " + allRTs / responseTimesTurnstiles.size());

        allDelays = 0;
        for (double delay : delaysTurnstiles) {
            allDelays += delay;
        }
        System.out.println("Average queueing time for turnstiles: " + allDelays / delaysTurnstiles.size());

        allUtilizations = 0;
        for (double u : utilizationsTurnstiles) {
            allUtilizations += u;
        }
        System.out.println("Avg utilization for turnstiles: " + allUtilizations / utilizationsTurnstiles.size());

        allInterarrivals = 0;
        for (double i : interarrivalsTurnstiles) {
            allInterarrivals += i;
        }
        System.out.println("Avg interarrivals for turnstiles: " + allInterarrivals / interarrivalsTurnstiles.size());

        allServiceTimes = 0;
        for (double st : serviceTimesTurnstiles) {
            allServiceTimes += st;
        }
        System.out.println("Avg service time for turnstiles: " + allServiceTimes / serviceTimesTurnstiles.size());

        allPopulations = 0;
        for (double pop : avgPopulationsTurnstiles) {
            allPopulations += pop;
        }
        System.out.println("Avg population for turnstiles: " + allPopulations / avgPopulationsTurnstiles.size());


        System.out.println("");

        /* SECOND PERQUISITION */

        allRTs = 0;
        for (double rt : responseTimesSecondPerquisition) {
            allRTs += rt;
        }
        System.out.println("Mean response time for second perquisition: " + allRTs / responseTimesSecondPerquisition.size());

        allDelays = 0;
        for (double delay : delaysSecondPerquisition) {
            allDelays += delay;
        }
        System.out.println("Average queueing time for second perquisition: " + allDelays / delaysSecondPerquisition.size());

        allUtilizations = 0;
        for (double u : utilizationsSecondPerquisition) {
            allUtilizations += u;
        }
        System.out.println("Avg utilization for second perquisition: " + allUtilizations / utilizationsSecondPerquisition.size());

        allInterarrivals = 0;
        for (double i : interarrivalsSecondPerquisition) {
            allInterarrivals += i;
        }
        System.out.println("Avg interarrivals for second perquisition: " + allInterarrivals / interarrivalsSecondPerquisition.size());

        allAbandons = 0;
        for (double a : allAbandonsSecondPerquisition) {
            allAbandons += a;
        }
        System.out.println("Avg abandons for second perquisition: " + allAbandons / allAbandonsSecondPerquisition.size());

        allServiceTimes = 0;
        for (double st : serviceTimesSecondPerquisition) {
            allServiceTimes += st;
        }
        System.out.println("Avg service time for second perquisition: " + allServiceTimes / serviceTimesSecondPerquisition.size());

        allPopulations = 0;
        for (double pop : avgPopulationsSecondPerquisition) {
            allPopulations += pop;
        }
        System.out.println("Avg population for second perquisition: " + allPopulations / avgPopulationsSecondPerquisition.size());


        File directory = new File("batch_reports");
        BufferedWriter bw = null;

        try {
            if (!directory.exists())
                directory.mkdirs();

            File file = new File(directory, "delay_ticket_check.csv");

            if (!file.exists())
                file.createNewFile();

            FileWriter writer = new FileWriter(file);
            bw = new BufferedWriter(writer);


            for (int i = 0; i < delaysTicketCheck.size(); i++) {
                bw.append(String.valueOf(delaysTicketCheck.get(i)));
                bw.append("\n");
                bw.flush();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                bw.flush();
                bw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // todo others


        System.out.println("");

    }


    static boolean generateSkip(Rngs rngs, int streamIndex, long queueSize) {
        rngs.selectStream(2 + streamIndex);
        double percentage = Math.min(0.8, (queueSize - 5.0) / 10.0);
        return rngs.random() <= percentage;
    }


    // this function generate a true value with (percentage * 100) % probability, oth. false
    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(1 + streamIndex);
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

        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + DEPARTURE_EVENT_TURNSTILES) {         /* now, check the others to find which   */
            i++;                                             /* has been idle longest                 */
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


        while (i < DEPARTURE_EVENT_TICKET) {         /* now, check the others to find which   */
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
        r.selectStream(1);

        // int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);
        int index = 0;  /* forcing the first time slot, for the verification step  todo verification step*/

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);

        return (sarrival);
    }

    double getService(Rngs r, double serviceTime) {
        r.selectStream(3);
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


