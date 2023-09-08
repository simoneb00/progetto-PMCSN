
import batch.Estimate;
import libraries.Rngs;
import model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

import static batch.Batch.writeFile;
import static model.Constants.*;
import static model.Constants.TC_SR;
import static model.Events.*;



public class FiniteHorizonSimulation {

    /*  Statistics :
     *  * Response times
     *  * Delays
     *  * Utilizations
     *  * Populations
     *  * Interarrival times
     *  * Service times
     *  * Queue populations
     */

    /* lists for statistics */
    static List<Double> ticketCheckRTs = new ArrayList<>();
    static List<Double> firstPerquisitionRTs = new ArrayList<>();
    static List<Double> turnstilesRTs = new ArrayList<>();
    static List<Double> secondPerquisitionRTs = new ArrayList<>();

    static List<Double> ticketCheckDelays = new ArrayList<>();
    static List<Double> firstPerquisitionDelays = new ArrayList<>();
    static List<Double> turnstilesDelays = new ArrayList<>();
    static List<Double> secondPerquisitionDelays = new ArrayList<>();

    static List<Double> ticketCheckUtilizations = new ArrayList<>();
    static List<Double> firstPerquisitionUtilizations = new ArrayList<>();
    static List<Double> turnstilesUtilizations = new ArrayList<>();
    static List<Double> secondPerquisitionUtilizations = new ArrayList<>();

    static List<Double> ticketCheckPopulations = new ArrayList<>();
    static List<Double> firstPerquisitionPopulations = new ArrayList<>();
    static List<Double> turnstilesPopulations = new ArrayList<>();
    static List<Double> secondPerquisitionPopulations = new ArrayList<>();

    static List<Double> ticketCheckInterarrivals = new ArrayList<>();
    static List<Double> firstPerquisitionInterarrivals = new ArrayList<>();
    static List<Double> turnstilesInterarrivals = new ArrayList<>();
    static List<Double> secondPerquisitionInterarrivals = new ArrayList<>();

    static List<Double> ticketCheckSTs = new ArrayList<>();
    static List<Double> firstPerquisitionSTs = new ArrayList<>();
    static List<Double> turnstilesSTs = new ArrayList<>();
    static List<Double> secondPerquisitionSTs = new ArrayList<>();

    static List<Double> ticketCheckQueuePopulations = new ArrayList<>();
    static List<Double> firstPerquisitionQueuePopulations = new ArrayList<>();
    static List<Double> turnstilesQueuePopulations = new ArrayList<>();
    static List<Double> secondPerquisitionQueuePopulations = new ArrayList<>();

    static List<Double> skipsCountersFirstPerquisition = new ArrayList<>();
    static List<Double> skipsCountersSecondPerquisition = new ArrayList<>();

    static final double START = 0.0;            /* initial (open the door)        */
    static final double STOP = 3 * 3600;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    static long simulation(long seed, Rngs r) {

        sarrival = START;
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
        r.plantSeeds(seed);

        /* time slots initialization */

        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 12062, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        /* events array initialization */
        MsqEvent[] events = new MsqEvent[
                ALL_EVENTS_TICKET +
                        ALL_EVENTS_FIRST_PERQUISITION +
                        ALL_EVENTS_TURNSTILES +
                        ALL_EVENTS_SECOND_PERQUISITION];


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
        events[0].t = getArrival(r, t.current);
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

            e = nextEvent(events);    /* next event index */
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
                events[ARRIVAL_EVENT_TICKET - 1].t = getArrival(r, t.current);
                if (events[ARRIVAL_EVENT_TICKET - 1].t > STOP)
                    events[ARRIVAL_EVENT_TICKET - 1].x = 0;

                /* if there's no queue, put a job on service */
                if (numberTicketCheck <= SERVERS_TICKET) {
                    service = getService(r, 0, TC_SR);
                    s = findOneTicketCheck(events);
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
                        service = getService(r, 32, P_SR);
                        s = findOneFirstPerquisition(events);
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
                    service = getService(r, 32, P_SR);
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
                    service = getService(r, 96, T_SR);
                    s = findOneTurnstiles(events);
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
                    service = getService(r, 96, T_SR);
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
                    service = getService(r, 160, P_SR);
                    s = findOneSecondPerquisition(events);
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
                    service = getService(r, 160, P_SR);
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
                    service = getService(r, 0, TC_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;
                }
            }

        }

        /* save statistics */

        /* TICKET CHECK */

        ticketCheckInterarrivals.add(events[ARRIVAL_EVENT_TICKET - 1].t / indexTicketCheck);
        ticketCheckRTs.add(areaTicketCheck / indexTicketCheck);

        double ticketCheckFinalTime = 0;
        for (s = 1; s <= SERVERS_TICKET; s++) {
            if (events[s].t > ticketCheckFinalTime)
                ticketCheckFinalTime = events[s].t;
        }

        double ticketCheckActualTime = ticketCheckFinalTime - ticketCheckFirstCompletion;

        ticketCheckPopulations.add(areaTicketCheck / ticketCheckActualTime);

        for (s = 1; s <= SERVERS_TICKET; s++)          /* adjust area to calculate */
            areaTicketCheck -= sum[s].service;                 /* averages for the queue   */

        ticketCheckDelays.add(areaTicketCheck / indexTicketCheck);
        ticketCheckQueuePopulations.add(areaTicketCheck / ticketCheckActualTime);

        double sumUtilizations = 0.0;
        double sumServices = 0.0;
        double sumServed = 0.0;

        for (s = 1; s <= SERVERS_TICKET; s++) {
            sumUtilizations += sum[s].service / ticketCheckActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }

        ticketCheckUtilizations.add(sumUtilizations / SERVERS_TICKET);
        ticketCheckSTs.add(sumServices / sumServed);


        /* FIRST PERQUISITION */

        firstPerquisitionInterarrivals.add(events[ALL_EVENTS_TICKET].t / indexFirstPerquisition);
        firstPerquisitionRTs.add(areaFirstPerquisition / indexFirstPerquisition);

        double firstPerquisitionFinalTime = 0;
        for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++) {
            if (events[s].t > firstPerquisitionFinalTime)
                firstPerquisitionFinalTime = events[s].t;
        }

        double firstPerquisitionActualTime = firstPerquisitionFinalTime - firstPerquisitionFirstCompletion;

        firstPerquisitionPopulations.add(areaFirstPerquisition / firstPerquisitionActualTime);

        for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++)          /* adjust area to calculate */
            areaFirstPerquisition -= sum[s].service;                                                                     /* averages for the queue   */


        firstPerquisitionDelays.add(areaFirstPerquisition / indexFirstPerquisition);
        firstPerquisitionQueuePopulations.add(areaFirstPerquisition / firstPerquisitionActualTime);

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;

        for (s = ALL_EVENTS_TICKET + 1; s <= ALL_EVENTS_TICKET + SERVERS_FIRST_PERQUISITION; s++) {
            sumUtilizations += sum[s].service / firstPerquisitionActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }

        firstPerquisitionUtilizations.add(sumUtilizations / SERVERS_FIRST_PERQUISITION);
        firstPerquisitionSTs.add(sumServices / sumServed);

        skipsCountersFirstPerquisition.add((double) skipCounterFirstPerquisition);

        /* TURNSTILES */

        turnstilesInterarrivals.add(events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION].t / indexTurnstiles);
        turnstilesRTs.add(areaTurnstiles / indexTurnstiles);

        double turnstilesFinalTime = 0;
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++) {
            if (events[s].t > turnstilesFinalTime)
                turnstilesFinalTime = events[s].t;
        }

        double turnstilesActualTime = turnstilesFinalTime - turnstilesFirstCompletion;

        turnstilesPopulations.add(areaTurnstiles / turnstilesActualTime);


        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++)
            /* adjust area to calculate */
            areaTurnstiles -= sum[s].service;                                                                     /* averages for the queue   */

        turnstilesDelays.add(areaTurnstiles / indexTurnstiles);
        turnstilesQueuePopulations.add(areaTurnstiles / turnstilesActualTime);

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;

        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + SERVERS_TURNSTILES; s++) {
            sumUtilizations += sum[s].service / turnstilesActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }
        turnstilesUtilizations.add(sumUtilizations / SERVERS_TURNSTILES);
        turnstilesSTs.add(sumServices / sumServed);


        /* SECOND PERQUISITION */

        secondPerquisitionInterarrivals.add(events[ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES].t / indexSecondPerquisition);
        secondPerquisitionRTs.add(areaSecondPerquisition / indexSecondPerquisition);

        double secondPerquisitionFinalTime = 0;
        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++) {
            if (events[s].t > secondPerquisitionFinalTime)
                secondPerquisitionFinalTime = events[s].t;
        }

        double secondPerquisitionActualTime = secondPerquisitionFinalTime - secondPerquisitionFirstCompletion;

        secondPerquisitionPopulations.add(areaSecondPerquisition / secondPerquisitionActualTime);

        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++)
            /* adjust area to calculate */
            areaSecondPerquisition -= sum[s].service;                                                                    /* averages for the queue   */

        secondPerquisitionDelays.add(areaSecondPerquisition / indexSecondPerquisition);
        secondPerquisitionQueuePopulations.add(areaSecondPerquisition / secondPerquisitionActualTime);

        sumUtilizations = 0.0;
        sumServices = 0.0;
        sumServed = 0.0;

        for (s = ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + 1; s <= ALL_EVENTS_TICKET + ALL_EVENTS_FIRST_PERQUISITION + ALL_EVENTS_TURNSTILES + SERVERS_SECOND_PERQUISITION; s++) {
            sumUtilizations += sum[s].service / turnstilesActualTime;
            sumServices += sum[s].service;
            sumServed += sum[s].served;
        }

        secondPerquisitionUtilizations.add(sumUtilizations / SERVERS_SECOND_PERQUISITION);
        secondPerquisitionSTs.add(sumServices / sumServed);

        skipsCountersSecondPerquisition.add((double) skipCounterSecondPerquisition);

        r.selectStream(255);
        return r.getSeed();

    }

    static boolean generateSkip(Rngs rngs, int streamIndex, long queueSize) {
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
    static int findOneTurnstiles(MsqEvent[] events) {
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

    static int findOneTicketCheck(MsqEvent[] events) {
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

    static int findOneFirstPerquisition(MsqEvent[] events) {
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

    static int findOneSecondPerquisition(MsqEvent[] events) {
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

    static double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    static double getArrival(Rngs r, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, exponential with rate given by the current time slot
         * --------------------------------------------------------------
         */
        r.selectStream(192);

        int index = TimeSlotController.timeSlotSwitch(slotList, currentTime);

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);

        return (sarrival);
    }

    static double getService(Rngs r, int streamIndex, double serviceTime) {
        r.selectStream(streamIndex);
        return (exponential(serviceTime, r));
    }

    static int nextEvent(MsqEvent[] event) {
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

    public static void main(String[] args) {
        long[] seeds = new long[1024];
        seeds[0] = 333;
        Rngs r = new Rngs();
        for (int i = 0; i < 150; i++) {
            seeds[i+1] = simulation(seeds[i], r);
            System.out.println(i+1);
        }


        System.out.println(ticketCheckUtilizations);
        System.out.println(firstPerquisitionUtilizations);
        System.out.println(turnstilesUtilizations);
        System.out.println(secondPerquisitionUtilizations);

        /* TICKET CHECK */
        writeFile(ticketCheckDelays, "replication_reports", "delays_ticket_check");
        writeFile(ticketCheckRTs,"replication_reports", "response_times_ticket_check");
        writeFile(ticketCheckUtilizations,"replication_reports", "utilizations_ticket_check");
        writeFile(ticketCheckPopulations,"replication_reports", "populations_ticket_check");
        writeFile(ticketCheckInterarrivals,"replication_reports", "interarrivals_ticket_check");
        writeFile(ticketCheckSTs, "replication_reports", "service_times_ticket_check");

        /* FIRST PERQUISITION */
        writeFile(firstPerquisitionDelays, "replication_reports", "delays_first_perquisition");
        writeFile(firstPerquisitionRTs, "replication_reports", "response_times_first_perquisition");
        writeFile(firstPerquisitionUtilizations, "replication_reports", "utilizations_first_perquisition");
        writeFile(firstPerquisitionPopulations, "replication_reports", "populations_first_perquisition");
        writeFile(firstPerquisitionInterarrivals, "replication_reports", "interarrivals_first_perquisition");
        writeFile(firstPerquisitionSTs, "replication_reports", "service_times_first_perquisition");
        writeFile(firstPerquisitionQueuePopulations, "replication_reports", "queue_populations_first_perquisition");
        writeFile(skipsCountersFirstPerquisition, "replication_reports", "skips_first_perquisition");

        /* TURNSTILES */
        writeFile(turnstilesDelays, "replication_reports", "delays_turnstiles");
        writeFile(turnstilesRTs, "replication_reports", "response_times_turnstiles");
        writeFile(turnstilesUtilizations, "replication_reports", "utilizations_turnstiles");
        writeFile(turnstilesPopulations, "replication_reports", "populations_turnstiles");
        writeFile(turnstilesInterarrivals, "replication_reports", "interarrivals_turnstiles");
        writeFile(turnstilesSTs, "replication_reports", "service_times_turnstiles");

        /* SECOND PERQUISITION */
        writeFile(secondPerquisitionDelays, "replication_reports", "delays_second_perquisition");
        writeFile(secondPerquisitionRTs, "replication_reports", "response_times_second_perquisition");
        writeFile(secondPerquisitionUtilizations, "replication_reports", "utilizations_second_perquisition");
        writeFile(secondPerquisitionPopulations, "replication_reports", "populations_second_perquisition");
        writeFile(secondPerquisitionInterarrivals, "replication_reports", "interarrivals_second_perquisition");
        writeFile(secondPerquisitionSTs, "replication_reports", "service_times_second_perquisition");
        writeFile(secondPerquisitionQueuePopulations, "replication_reports", "queue_populations_second_perquisition");
        writeFile(skipsCountersSecondPerquisition, "replication_reports", "skips_second_perquisition");


        /* INTERVAL ESTIMATION */

        Estimate estimate = new Estimate();

        List<String> filenames = List.of("response_times_ticket_check", "delays_ticket_check", "utilizations_ticket_check", "interarrivals_ticket_check", "service_times_ticket_check", "populations_ticket_check",
                "response_times_first_perquisition", "delays_first_perquisition", "utilizations_first_perquisition", "interarrivals_first_perquisition", "service_times_first_perquisition", "populations_first_perquisition", "queue_populations_first_perquisition", "skips_first_perquisition",
                "response_times_turnstiles", "delays_turnstiles", "utilizations_turnstiles", "interarrivals_turnstiles", "service_times_turnstiles", "populations_turnstiles",
                "response_times_second_perquisition", "delays_second_perquisition", "utilizations_second_perquisition", "interarrivals_second_perquisition", "service_times_second_perquisition", "populations_second_perquisition", "queue_populations_second_perquisition", "skips_second_perquisition"
        );

        for (String filename : filenames) {
            estimate.createInterval("replication_reports", filename);
        }
    }
}
