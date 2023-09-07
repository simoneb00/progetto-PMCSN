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

public class Batch {

    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = Double.POSITIVE_INFINITY;        /* terminal (close the door) time  todo verification (only one hour)*/
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

        Batch m = new Batch();
        Rngs r = new Rngs();
        r.plantSeeds(0);

        /* time slots initialization */

        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 12062, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        /* batch parameters */
        int k = 128;
        int b = 1024*7;

        /* lists for batch simulation */

        /* waiting times */
        List<Double> delaysTicketCheck = new ArrayList<>();
        List<Double> delaysFirstPerquisition = new ArrayList<>();
        List<Double> delaysTurnstiles = new ArrayList<>();
        List<Double> delaysSecondPerquisition = new ArrayList<>();

        /* response times */
        List<Double> responseTimesTicketCheck = new ArrayList<>();
        List<Double> responseTimesFirstPerquisition = new ArrayList<>();
        List<Double> responseTimesTurnstiles = new ArrayList<>();
        List<Double> responseTimesSecondPerquisition = new ArrayList<>();

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
        List<Double> allAbandonsTicketCheck = new ArrayList<>();
        List<Double> allAbandonsFirstPerquisition = new ArrayList<>();
        List<Double> allAbandonsSecondPerquisition = new ArrayList<>();

        /* service times */
        List<Double> serviceTimesTicketCheck = new ArrayList<>();
        List<Double> serviceTimesFirstPerquisition = new ArrayList<>();
        List<Double> serviceTimesTurnstiles = new ArrayList<>();
        List<Double> serviceTimesSecondPerquisition = new ArrayList<>();

        /* skips */
        List<Double> skipsCountersFirstPerquisition = new ArrayList<>();
        List<Double> skipsCountersSecondPerquisition = new ArrayList<>();


        double currentBatchStartingTime = 0;
        double currentFirstArrivalTimeTC = 0;
        double currentFirstArrivalTimeFP = 0;
        double currentFirstArrivalTimeT = 0;
        double currentFirstArrivalTimeSP = 0;
        long batchCounter = 0;



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

        while ((events[0].x != 0)) {

            if (indexTicketCheck != 0 && indexTicketCheck % b == 0) {
                /* new batch */
                batchCounter++;

                /* TICKET CHECK */
                responseTimesTicketCheck.add(areaTicketCheck / indexTicketCheck);
                interarrivalsTicketCheck.add((events[ARRIVAL_EVENT_TICKET - 1].t - currentFirstArrivalTimeTC) / indexTicketCheck);
                allAbandonsTicketCheck.add((double) abandonsCounterTicketCheck / b);


                double ticketCheckActualTime = t.current - currentBatchStartingTime;

                avgPopulationsTicketCheck.add(areaTicketCheck / ticketCheckActualTime);

                for (s = 1; s <= SERVERS_TICKET; s++)          /* adjust area to calculate */
                    areaTicketCheck -= sum[s].service;                 /* averages for the queue   */

                delaysTicketCheck.add(areaTicketCheck / indexTicketCheck);

                double sumUtilizations = 0.0;
                double sumServices = 0.0;
                double sumServed = 0.0;


                for (s = 1; s <= SERVERS_TICKET; s++) {
                    sumUtilizations += sum[s].service / ticketCheckActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsTicketCheck.add(sumUtilizations / SERVERS_TICKET);
                serviceTimesTicketCheck.add(sumServices / sumServed);


                areaTicketCheck = 0;
                indexTicketCheck = 0;
                abandonsCounterTicketCheck = 0;

                for (s = 1; s <= SERVERS_TICKET; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }



                /* FIRST PERQUISITION */

                responseTimesFirstPerquisition.add(areaFirstPerquisition / indexFirstPerquisition);
                interarrivalsFirstPerquisition.add((events[ALL_EVENTS_TICKET].t - currentFirstArrivalTimeFP) / indexFirstPerquisition);
                allAbandonsFirstPerquisition.add((double) abandonsCounterFirstPerquisition / b);
                skipsCountersFirstPerquisition.add((double) skipCounterFirstPerquisition);

                double firstPerquisitionActualTime = t.current - currentBatchStartingTime;

                avgPopulationsFirstPerquisition.add(areaFirstPerquisition / firstPerquisitionActualTime);

                for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++)          /* adjust area to calculate */
                    areaFirstPerquisition -= sum[s].service;                                                                /* averages for the queue   */

                delaysFirstPerquisition.add(areaFirstPerquisition / indexFirstPerquisition);

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++) {
                    sumUtilizations += sum[s].service / firstPerquisitionActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsFirstPerquisition.add(sumUtilizations / SERVERS_FIRST_PERQUISITION);
                serviceTimesFirstPerquisition.add(sumServices / sumServed);

                skipCounterFirstPerquisition = 0;
                areaFirstPerquisition = 0;
                indexFirstPerquisition = 0;
                abandonsCounterFirstPerquisition = 0;

                for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }


                /* TURNSTILES */

                responseTimesTurnstiles.add(areaTurnstiles / indexTurnstiles);
                interarrivalsTurnstiles.add((events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t - currentFirstArrivalTimeT) / indexTurnstiles);

                double turnstilesActualTime = t.current - currentBatchStartingTime;

                avgPopulationsTurnstiles.add(areaTurnstiles / turnstilesActualTime);

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++)          /* adjust area to calculate */
                    areaTurnstiles -= sum[s].service;                                                                /* averages for the queue   */

                delaysTurnstiles.add(Math.max(0, areaTurnstiles / indexTurnstiles));

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++) {
                    sumUtilizations += sum[s].service / turnstilesActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsTurnstiles.add(sumUtilizations / SERVERS_TURNSTILES);
                serviceTimesTurnstiles.add(sumServices / sumServed);

                areaTurnstiles = 0;
                indexTurnstiles = 0;

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }

                /* SECOND PERQUISITION */

                responseTimesSecondPerquisition.add(areaSecondPerquisition / indexSecondPerquisition);
                interarrivalsSecondPerquisition.add((events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t - currentFirstArrivalTimeSP) / indexSecondPerquisition);
                allAbandonsSecondPerquisition.add((double) abandonsCounterSecondPerquisition / b);
                skipsCountersSecondPerquisition.add((double) skipCounterSecondPerquisition);

                double secondPerquisitionActualTime = t.current - currentBatchStartingTime;

                avgPopulationsSecondPerquisition.add(areaSecondPerquisition / secondPerquisitionActualTime);

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++)          /* adjust area to calculate */
                    areaSecondPerquisition -= sum[s].service;                                                                /* averages for the queue   */

                delaysSecondPerquisition.add(areaSecondPerquisition / indexSecondPerquisition);

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++) {
                    sumUtilizations += sum[s].service / secondPerquisitionActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsSecondPerquisition.add(sumUtilizations / SERVERS_SECOND_PERQUISITION);
                serviceTimesSecondPerquisition.add(sumServices / sumServed);


                skipCounterSecondPerquisition = 0;
                areaSecondPerquisition = 0;
                indexSecondPerquisition = 0;
                abandonsCounterSecondPerquisition = 0;

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++) {
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


        /* BATCH SIMULATION RESULTS */

        System.out.println("Completed " + batchCounter + " batches");

        System.out.println("");
        

        /* files creation for interval estimation */
        String directory = "batch_reports";

        /* TICKET CHECK */
        writeFile(delaysTicketCheck, directory, "delays_ticket_check");
        writeFile(responseTimesTicketCheck, directory, "response_times_ticket_check");
        writeFile(utilizationsTicketCheck, directory, "utilizations_ticket_check");
        writeFile(avgPopulationsTicketCheck, directory, "populations_ticket_check");
        writeFile(interarrivalsTicketCheck, directory, "interarrivals_ticket_check");
        writeFile(allAbandonsTicketCheck, directory, "abandons_ticket_check");
        writeFile(serviceTimesTicketCheck, directory, "service_times_ticket_check");

        /* FIRST PERQUISITION */
        writeFile(delaysFirstPerquisition, directory, "delays_first_perquisition");
        writeFile(responseTimesFirstPerquisition, directory, "response_times_first_perquisition");
        writeFile(utilizationsFirstPerquisition, directory, "utilizations_first_perquisition");
        writeFile(avgPopulationsFirstPerquisition, directory, "populations_first_perquisition");
        writeFile(interarrivalsFirstPerquisition, directory, "interarrivals_first_perquisition");
        writeFile(allAbandonsFirstPerquisition, directory, "abandons_first_perquisition");
        writeFile(serviceTimesFirstPerquisition, directory, "service_times_first_perquisition");
        writeFile(skipsCountersFirstPerquisition, directory, "skips_first_perquisition");

        /* TURNSTILES */
        writeFile(delaysTurnstiles, directory, "delays_turnstiles");
        writeFile(responseTimesTurnstiles, directory, "response_times_turnstiles");
        writeFile(utilizationsTurnstiles, directory, "utilizations_turnstiles");
        writeFile(avgPopulationsTurnstiles, directory, "populations_turnstiles");
        writeFile(interarrivalsTurnstiles, directory, "interarrivals_turnstiles");
        writeFile(serviceTimesTurnstiles, directory, "service_times_turnstiles");

        /* SECOND PERQUISITION */
        writeFile(delaysSecondPerquisition, directory, "delays_second_perquisition");
        writeFile(responseTimesSecondPerquisition, directory, "response_times_second_perquisition");
        writeFile(utilizationsSecondPerquisition, directory, "utilizations_second_perquisition");
        writeFile(avgPopulationsSecondPerquisition, directory, "populations_second_perquisition");
        writeFile(interarrivalsSecondPerquisition, directory, "interarrivals_second_perquisition");
        writeFile(allAbandonsSecondPerquisition, directory, "abandons_second_perquisition");
        writeFile(serviceTimesSecondPerquisition, directory, "service_times_second_perquisition");
        writeFile(skipsCountersSecondPerquisition, directory, "skips_second_perquisition");


        /* INTERVAL ESTIMATION */

        Estimate estimate = new Estimate();

        List<String> filenames = List.of("response_times_ticket_check", "delays_ticket_check", "utilizations_ticket_check", "interarrivals_ticket_check", "abandons_ticket_check", "service_times_ticket_check", "populations_ticket_check",
                "response_times_first_perquisition", "delays_first_perquisition", "utilizations_first_perquisition", "interarrivals_first_perquisition", "abandons_first_perquisition", "service_times_first_perquisition", "populations_first_perquisition", "skips_first_perquisition",
                "response_times_turnstiles", "delays_turnstiles", "utilizations_turnstiles", "interarrivals_turnstiles", "service_times_turnstiles", "populations_turnstiles",
                "response_times_second_perquisition", "delays_second_perquisition", "utilizations_second_perquisition", "interarrivals_second_perquisition", "abandons_second_perquisition", "service_times_second_perquisition", "populations_second_perquisition", "skips_second_perquisition"
        );

        for (String filename : filenames) {
            estimate.createInterval(directory, filename);
        }

    }

    public static void writeFile(List<Double> list, String directoryName, String filename) {
        File directory = new File(directoryName);
        BufferedWriter bw = null;

        try {
            if (!directory.exists())
                directory.mkdirs();

            File file = new File(directory, filename + ".dat");

            if (!file.exists())
                file.createNewFile();

            FileWriter writer = new FileWriter(file);
            bw = new BufferedWriter(writer);


            for (int i = 0; i < list.size(); i++) {
                bw.append(String.valueOf(list.get(i)));
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
    }


    static boolean generateSkip(Rngs rngs, int streamIndex, long queueSize) {
        if (queueSize > 656)
            System.out.println("");

        rngs.selectStream(64);
        double percentage = Math.min(0.8, (0.444444 * queueSize - 291.555555)/100);
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

        int index = 0;  /* forcing the first time slot, for the verification step */

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


