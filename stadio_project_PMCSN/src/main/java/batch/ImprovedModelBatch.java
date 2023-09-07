package batch;

import libraries.Rngs;
import model.TimeSlot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static batch.Batch.writeFile;
import static model.Constants.TC_SR;
import static model.ImprovedEvents.*;

import static model.Constants.*;

// TODO  UPDATE THIS WHEN EVENTS CHANGE
/*
IMRPOVED MODEL EVENTS
-TICKET CHECK-
0 = ARRIVO
1-10 = SERVIZIO
11 = ABBANDONO

-TORNELLI-
12 = ARRIVO
13-20 = SERVIZIO

-PERQUISITION-
21 = ARRIVO
22-41 = SERVIZIO
42 =  SKIP
43 = ABBANDONO

-TORNELLO ABBONATO-
44 = ARRIVO
45 = SERVIZIO

-PERQUISIZIONE ABBONATO-
46 = ARRIVO
47-48 = SERVIZIO
49 = ABBANDONO
 */


class ImprovedModelBatch {

    static double START = 0.0;            /* initial (open the door)        */
    static double STOP = Double.POSITIVE_INFINITY;        /* terminal (close the door) time */
    static double sarrival = START;

    static List<TimeSlot> slotList = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        /* batch simulation parameters */
        int b = 1024 * 7;
        int k = 128;

        /* lists to save batch data */

        /* ticket check */
        List<Double> responseTimesTicketCheck = new ArrayList<>();
        List<Double> interarrivalsTicketCheck = new ArrayList<>();
        List<Double> delaysTicketCheck = new ArrayList<>();
        List<Double> avgPopulationsTicketCheck = new ArrayList<>();
        List<Double> allAbandonsTicketCheck = new ArrayList<>();
        List<Double> utilizationsTicketCheck = new ArrayList<>();
        List<Double> serviceTimesTicketCheck = new ArrayList<>();

        /* turnstiles */
        List<Double> responseTimesTurnstiles = new ArrayList<>();
        List<Double> interarrivalsTurnstiles = new ArrayList<>();
        List<Double> delaysTurnstiles = new ArrayList<>();
        List<Double> avgPopulationsTurnstiles = new ArrayList<>();
        List<Double> utilizationsTurnstiles = new ArrayList<>();
        List<Double> serviceTimesTurnstiles = new ArrayList<>();

        /* perquisition */
        List<Double> responseTimesPerquisition = new ArrayList<>();
        List<Double> interarrivalsPerquisition = new ArrayList<>();
        List<Double> delaysPerquisition = new ArrayList<>();
        List<Double> avgPopulationsPerquisition = new ArrayList<>();
        List<Double> allAbandonsPerquisition = new ArrayList<>();
        List<Double> skipsCountersPerquisition = new ArrayList<>();
        List<Double> utilizationsPerquisition = new ArrayList<>();
        List<Double> serviceTimesPerquisition = new ArrayList<>();

        /* subscribed turnstiles */
        List<Double> responseTimesSubTurnstiles = new ArrayList<>();
        List<Double> interarrivalsSubTurnstiles = new ArrayList<>();
        List<Double> delaysSubTurnstiles = new ArrayList<>();
        List<Double> avgPopulationsSubTurnstiles = new ArrayList<>();
        List<Double> utilizationsSubTurnstiles = new ArrayList<>();
        List<Double> serviceTimesSubTurnstiles = new ArrayList<>();


        /* subscribed perquisition */
        List<Double> responseTimesSubPerquisition = new ArrayList<>();
        List<Double> interarrivalsSubPerquisition = new ArrayList<>();
        List<Double> delaysSubPerquisition = new ArrayList<>();
        List<Double> avgPopulationsSubPerquisition = new ArrayList<>();
        List<Double> allAbandonsSubPerquisition = new ArrayList<>();
        List<Double> utilizationsSubPerquisition = new ArrayList<>();
        List<Double> serviceTimesSubPerquisition = new ArrayList<>();


        double currentBatchStartingTime = 0;
        double currentFirstArrivalTimeTC = 0;
        double currentFirstArrivalTimeT = 0;
        double currentFirstArrivalTimeP = 0;
        double currentFirstArrivalTimeST = 0;
        double currentFirstArrivalTimeSP = 0;
        long batchCounter = 0;

        /* stream index for the rng */
        int streamIndex = 1;

        /* population counter for every node */
        long numberTicketCheck = 0;
        long numberTurnstiles = 0;
        long numberSubscribedTurnstiles = 0;
        long numberSubscribedPerquisition = 0;
        long classOneTotalPopulation = 0;
        long classTwoTotalPopulation = 0;
        long firstClassJobInQueue = 0;
        long secondClassJobInQueue = 0;

        int e;      /* next event index */
        int s;      /* server index */

        /* processed jobs counter for every node */
        long indexTicketCheck = 0;
        long indexTurnstiles = 0;
        long indexSubscribedTurnstiles = 0;
        long indexSubscribedPerquisition = 0;
        long indexPerquisition = 0;

        /* time integrated number for every node */
        double areaTicketCheck = 0.0;
        double areaTurnstiles = 0.0;
        double areaSubscribedTurnstiles = 0.0;
        double areaPerquisition = 0.0;
        double areaSubscribedPerquisition = 0.0;

        double service;     /* it will contain the service times */

        /* abandons counter for ticket check and perquisitions */
        long abandonsCounterTicketCheck = 0;
        long abandonsCounterPerquisition = 0;
        long abandonsCounterSubscribedPerquisition = 0;

        /* abandons list for ticket check and perquisitions */
        List<Double> abandonsTicketCheck = new ArrayList<>();
        List<Double> abandonsPerquisition = new ArrayList<>();
        List<Double> abandonsSubscribedPerquisition = new ArrayList<>();

        /* skip list for perquisitions */
        List<Double> skipsPerquisition = new ArrayList<>();

        List<Integer> perquisitionPriorityClassService = new ArrayList<>();

        for (int i = 0; i < SERVERS_PERQUISITION; i++) {
            perquisitionPriorityClassService.add(0);
        }

        /* skip counter for perquisitions */
        long skipCounterPerquisition = 0;

        /* first completion for every node */
        double ticketCheckFirstCompletion = 0;

        double turnstilesFirstCompletion = 0;
        double subscribedTurnstilesFirstCompletion = 0;
        double subscribedPerquisitionFirstCompletion = 0;
        double perquisitionFirstCompletion = 0;
        ImprovedModelBatch m = new ImprovedModelBatch();
        Rngs r = new Rngs();
        r.plantSeeds(0);

        /* time slots initialization */

        for (int f = 0; f < 3; f++) {
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 12062, 3600 * f, 3600 * (f + 1) - 1);
            slotList.add(slot);
        }

        /* events array initialization */
        MsqEvent[] events = new MsqEvent[
                ALL_EVENTS_TICKET +
                        ALL_EVENTS_TURNSTILES +
                        ALL_EVENTS_PERQUISITION +
                        ALL_EVENTS_SUBSCRIBED_TURNSTILES +
                        ALL_EVENTS_SUBSCRIBED_PERQUISITION];


        /* sum array initialization (to keep track of services) */
        MsqSum[] sum = new MsqSum[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION];
        for (s = 0; s < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION; s++) {
            events[s] = new MsqEvent();
            sum[s] = new MsqSum();
        }

        /* clock initialization */
        MsqT t = new MsqT();
        t.current = START;

        /* generating the first arrival */
        events[0].t = m.getArrival(r, 11, t.current);
        events[0].x = 1;

        /* all other servers are initially idle */
        for (s = 1; s < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION; s++) {
            events[s].t = START;
            events[s].x = 0;
            sum[s].service = 0.0;
            sum[s].served = 0;
        }



        /* START ITERATION */

        /* skip */

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


                /* TURNSTILES */

                responseTimesTurnstiles.add(areaTurnstiles / indexTurnstiles);
                interarrivalsTurnstiles.add((events[ALL_EVENTS_TICKET].t - currentFirstArrivalTimeT) / indexTurnstiles);

                double turnstilesActualTime = t.current - currentBatchStartingTime;

                avgPopulationsTurnstiles.add(areaTurnstiles / turnstilesActualTime);

                for (s = ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES; s <= ALL_EVENTS_TICKET + SERVERS_TURNSTILES; s++)          /* adjust area to calculate */
                    areaTurnstiles -= sum[s].service;                                                                /* averages for the queue   */

                delaysTurnstiles.add(Math.max(0, areaTurnstiles / indexTurnstiles));

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES; s <= ALL_EVENTS_TICKET + SERVERS_TURNSTILES; s++) {
                    sumUtilizations += sum[s].service / turnstilesActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsTurnstiles.add(sumUtilizations / SERVERS_TURNSTILES);
                serviceTimesTurnstiles.add(sumServices / sumServed);

                areaTurnstiles = 0;
                indexTurnstiles = 0;

                for (s = ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES; s <= ALL_EVENTS_TICKET + SERVERS_TURNSTILES; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }

                /* PERQUISITION */

                responseTimesPerquisition.add(areaPerquisition / indexPerquisition);
                interarrivalsPerquisition.add((events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].t - currentFirstArrivalTimeP) / indexPerquisition);
                allAbandonsPerquisition.add((double) abandonsCounterPerquisition / b);
                skipsCountersPerquisition.add((double) skipCounterPerquisition);

                double perquisitionActualTime = t.current - currentBatchStartingTime;

                avgPopulationsPerquisition.add(areaPerquisition / perquisitionActualTime);

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION; s++)          /* adjust area to calculate */
                    areaPerquisition -= sum[s].service;                                                                /* averages for the queue   */

                delaysPerquisition.add(areaPerquisition / indexPerquisition);

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION; s++) {
                    sumUtilizations += sum[s].service / perquisitionActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsPerquisition.add(sumUtilizations / SERVERS_PERQUISITION);
                serviceTimesPerquisition.add(sumServices / sumServed);

                skipCounterPerquisition = 0;
                areaPerquisition = 0;
                indexPerquisition = 0;
                abandonsCounterPerquisition = 0;

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }

                /* SUBSCRIBED TURNSTILES */

                responseTimesSubTurnstiles.add(areaSubscribedTurnstiles / indexSubscribedTurnstiles);
                interarrivalsSubTurnstiles.add((events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION].t - currentFirstArrivalTimeST) / indexSubscribedTurnstiles);

                double subTurnstilesActualTime = t.current - currentBatchStartingTime;

                avgPopulationsSubTurnstiles.add(areaSubscribedTurnstiles / subTurnstilesActualTime);

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ARRIVAL_EVENT_SUBSCRIBED_TURNSTILES; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES; s++) {
                    areaSubscribedTurnstiles -= sum[s].service;     /* averages for the queue   */
                }

                delaysSubTurnstiles.add(Math.max(0, areaSubscribedTurnstiles / indexSubscribedTurnstiles));

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;
                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ARRIVAL_EVENT_SUBSCRIBED_TURNSTILES; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES; s++) {
                    sumUtilizations += sum[s].service / subTurnstilesActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsSubTurnstiles.add(sumUtilizations / SERVERS_SUBSCRIBED_TURNSTILES);
                serviceTimesSubTurnstiles.add(sumServices / sumServed);

                areaSubscribedTurnstiles = 0;
                indexSubscribedTurnstiles = 0;

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ARRIVAL_EVENT_SUBSCRIBED_TURNSTILES; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }



                /* SUBSCRIBED PERQUISITION */

                responseTimesSubPerquisition.add(areaSubscribedPerquisition / indexSubscribedPerquisition);
                interarrivalsSubPerquisition.add((events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES].t - currentFirstArrivalTimeSP) / indexSubscribedPerquisition);
                allAbandonsSubPerquisition.add((double) abandonsCounterSubscribedPerquisition / b);

                double subPerquisitionActualTime = t.current - currentBatchStartingTime;

                avgPopulationsSubPerquisition.add(Math.max(0, areaSubscribedPerquisition / subPerquisitionActualTime));

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ARRIVAL_EVENT_SUBSCRIBED_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + SERVERS_SUBSCRIBED_PERQUISITION; s++) {
                    areaSubscribedPerquisition -= sum[s].service;                                                                /* averages for the queue   */
                }

                delaysSubPerquisition.add(areaSubscribedPerquisition / indexSubscribedPerquisition);

                sumUtilizations = 0.0;
                sumServices = 0.0;
                sumServed = 0.0;


                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ARRIVAL_EVENT_SUBSCRIBED_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + SERVERS_SUBSCRIBED_PERQUISITION; s++) {
                    sumUtilizations += sum[s].service / subPerquisitionActualTime;
                    sumServices += sum[s].service;
                    sumServed += sum[s].served;
                }

                utilizationsSubPerquisition.add(sumUtilizations / SERVERS_SUBSCRIBED_PERQUISITION);
                serviceTimesSubPerquisition.add(sumServices / sumServed);

                areaSubscribedPerquisition = 0;
                indexSubscribedPerquisition = 0;
                abandonsCounterSubscribedPerquisition = 0;

                for (s = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ARRIVAL_EVENT_SUBSCRIBED_PERQUISIION; s <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + SERVERS_SUBSCRIBED_PERQUISITION; s++) {
                    sum[s].served = 0;
                    sum[s].service = 0;
                }


                /* final updates */
                currentBatchStartingTime = t.current;
                currentFirstArrivalTimeTC = events[0].t;
                currentFirstArrivalTimeT = events[ALL_EVENTS_TICKET].t;
                currentFirstArrivalTimeP = events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].t;
                currentFirstArrivalTimeST = events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION].t;
                currentFirstArrivalTimeSP = events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES].t;

            }

            if (batchCounter == k)
                break;


            if (!skipsPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2].t = skipsPerquisition.get(0);
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2].x = 1;
            } else {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2].x = 0;
            }

            /* abandons */
            if (!abandonsTicketCheck.isEmpty()) {
                events[ALL_EVENTS_TICKET - 1].t = abandonsTicketCheck.get(0);
                events[ALL_EVENTS_TICKET - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET - 1].x = 0;
            }


            if (!abandonsPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1].x = 0;
            }
            if (!abandonsSubscribedPerquisition.isEmpty()) {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1].x = 1;
            } else {
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1].x = 0;
            }


            e = m.nextEvent(events);    /* next event index */
            t.next = events[e].t;       /* next event time */


            /* update integrals */
            areaTicketCheck += (t.next - t.current) * numberTicketCheck;
            areaTurnstiles += (t.next - t.current) * numberTurnstiles;
            areaPerquisition += (t.next - t.current) * (classOneTotalPopulation + classTwoTotalPopulation);
            areaSubscribedTurnstiles += (t.next - t.current) * numberSubscribedTurnstiles;
            areaSubscribedPerquisition += (t.next - t.current) * numberSubscribedPerquisition;

            t.current = t.next;     /* advance the clock */

            if (e == ARRIVAL_EVENT_TICKET - 1) {
                /* ticket check arrival */
                numberTicketCheck++;

                /* generate the next arrival */
                events[ARRIVAL_EVENT_TICKET - 1].t = m.getArrival(r, 22, t.current);
                if (events[ARRIVAL_EVENT_TICKET - 1].t > STOP)
                    events[ARRIVAL_EVENT_TICKET - 1].x = 0;

                /* if there's no queue, put a job on service */
                if (numberTicketCheck <= SERVERS_TICKET) {
                    service = m.getService(r, 33, TC_SR);
                    s = m.findOneTicketCheck(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                }

            } else if (e == ALL_EVENTS_TICKET - 1) {
                /* ticket check abandon*/

                abandonsCounterTicketCheck++;
                abandonsTicketCheck.remove(0);

            } else if (e == ALL_EVENTS_TICKET) {
                /* turnstiles arrival  */

                events[ALL_EVENTS_TICKET].x = 0;

                /* generate an abandon with probability P1 */
                boolean abandon = generateAbandon(r, 44, P1);

                if (abandon) {
                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsTicketCheck.add(abandonTime);

                } else {
                    /* no abandon -> generate arrival at turnstiles */
                    numberTurnstiles++;
                    if (numberTurnstiles <= SERVERS_TURNSTILES) {
                        service = m.getService(r, 55, T_SR);
                        s = m.findOneTurnstiles(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;

                    }
                }
            } else if ((e >= ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES) && (e < ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES + SERVERS_TURNSTILES)) {

                /* turnstile service */
                if (turnstilesFirstCompletion == 0)
                    turnstilesFirstCompletion = t.current;

                indexTurnstiles++;
                numberTurnstiles--;

                /* generate an arrival at perquisition center */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].x = 1;

                s = e;
                if (numberTurnstiles >= SERVERS_TURNSTILES) {
                    /* there's a job in queue to be processed */
                    service = m.getService(r, 66, T_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES) {
                /*  perquisition arrival */

                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES].x = 0;

                service = m.getService(r, 77, P_SR);
                if (service < 10) {
                    /* first queue */
                    classOneTotalPopulation++;
                } else {
                    /* second queue */
                    classTwoTotalPopulation++;
                }

                if (classOneTotalPopulation + classTwoTotalPopulation <= SERVERS_PERQUISITION) {
                    /* the total node population is below the total number of servers */
                    s = m.findPerquisition(events);

                    /* mark s as service for the first/second queue */
                    // bind server index with job type into the array
                    if (service < 10) {
                        try {
                            // small job ( without bags)
                            perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 1);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        try {
                            // big job
                            perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 2);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                } else {
                    //UPDATE THE NUMBER OF JOBS IN QUEUE
                    if (service < 10)
                        firstClassJobInQueue++;
                    else secondClassJobInQueue++;
                }


            } else if ((e >= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION)
                    && (e < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION + SERVERS_PERQUISITION)
            ) {
                // Perquisition service

                boolean isFromFirstQueue = (perquisitionPriorityClassService.get(e - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1)) == 1);
                boolean isFromSecondQueue = (perquisitionPriorityClassService.get(e - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1)) == 2);

                boolean skip = false;

                if (isFromFirstQueue) {
                    skip = generateSkip(r, 88, classOneTotalPopulation - SERVERS_PERQUISITION);
                } else if (isFromSecondQueue) {
                    skip = generateSkip(r, 99, classTwoTotalPopulation - SERVERS_PERQUISITION);
                } else {
                    throw new Exception("Unexpected behaviour");
                }

                //
                if (isFromFirstQueue)
                    classOneTotalPopulation--;
                else
                    classTwoTotalPopulation--;

                // SKIP caused by congestion
                if (skip) {
                    double skipTime = t.current + 0.01;
                    skipsPerquisition.add(skipTime);
                    perquisitionPriorityClassService.set(e - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 0);

                } else {
                    // Update data of second perquisition queue and generate possible abandon
                    if (perquisitionFirstCompletion == 0)
                        perquisitionFirstCompletion = t.current;

                    // update number o done perquisition
                    indexPerquisition++;

                    /* abandons are generated only if the perquisition has not been skipped */
                    boolean abandon = generateAbandon(r, 110, P3);
                    if (abandon) {
                        double abandonTime = t.current + 0.02;      // 0.02 not to overlap an eventual skip
                        perquisitionPriorityClassService.set(e - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 0);
                        abandonsPerquisition.add(abandonTime);
                    }
                }

                s = e;

                // Now, there is idle server, so re-generate service time for queued job
                if (firstClassJobInQueue >= 1) {
                    firstClassJobInQueue--;
                    // GENERATE A SERVICE LESS THAN 10 SECONDS
                    do {
                        service = m.getService(r, 121, P_SR);
                    } while (!(service < 10));
                    perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 1);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                } else if (secondClassJobInQueue >= 1) {
                    secondClassJobInQueue--;
                    // GENERATE A SERVICE GREATER THEN 10 SECONDS
                    do {
                        service = m.getService(r, 132, P_SR);
                    } while ((service < 10));
                    perquisitionPriorityClassService.set(s - (ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + 1), 2);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;
                } else
                    events[s].x = 0;


            } else if (e == ALL_EVENTS_TICKET + +ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 2) {
                // perquisition SKIP
                indexPerquisition++;
                skipCounterPerquisition++;
                skipsPerquisition.remove(0);

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION - 1) {
                /*  perquisition  abandon  */
                abandonsCounterPerquisition++;
                abandonsPerquisition.remove(0);

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION) {
                // subscriber turnstiles arrival
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION].x = 0;

                /* generate an abandon with probability P1 */
                boolean abandon = generateAbandon(r, 143, P1);

                if (abandon) {
                    /* an abandon must be generated -> we must add it to the abandons list and schedule it */
                    double abandonTime = t.current + 0.01;
                    abandonsTicketCheck.add(abandonTime);

                } else {
                    /* no abandon -> generate arrival at turnstiles */
                    numberSubscribedTurnstiles++;
                    if (numberSubscribedTurnstiles <= SERVERS_SUBSCRIBED_TURNSTILES) {
                        service = m.getService(r, 154, T_SR);
                        s = m.findOneSubscribedTurnstiles(events);
                        sum[s].service += service;
                        sum[s].served++;
                        events[s].t = t.current + service;
                        events[s].x = 1;

                    }
                }

            } else if (e >= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + 1 && e <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES) {
                /* subscribed turnstile service */
                if (subscribedTurnstilesFirstCompletion == 0)
                    subscribedTurnstilesFirstCompletion = t.current;

                indexSubscribedTurnstiles++;
                numberSubscribedTurnstiles--;

                /* generate an arrival at subscribed perquisition center */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES].t = t.current;
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES].x = 1;


                s = e;
                if (numberSubscribedTurnstiles >= SERVERS_SUBSCRIBED_TURNSTILES) {
                    /* there's a job in queue to be processed */
                    service = m.getService(r, 165, T_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    events[s].x = 0;
                }

            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES) {
                /* subscribed perquisition arrival */
                events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES].x = 0;
                numberSubscribedPerquisition++;
                if (numberSubscribedPerquisition <= SERVERS_SUBSCRIBED_PERQUISITION) {
                    service = m.getService(r, 176, P_SR);
                    s = m.findOneSubscribedPerquisition(events);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                    events[s].x = 1;

                }
            } else if (e >= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + 1 && e <= ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + SERVERS_SUBSCRIBED_PERQUISITION) {
                /* subscribed perquisition service */
                if (subscribedPerquisitionFirstCompletion == 0)
                    subscribedPerquisitionFirstCompletion = t.current;

                indexSubscribedPerquisition++;
                numberSubscribedPerquisition--;
                /* abandons are generated only if the perquisition has not been skipped */
                boolean abandon = generateAbandon(r, 187, P3);
                if (abandon) {
                    double abandonTime = t.current + 0.02;      // 0.02 not to overlap an eventual skip
                    abandonsSubscribedPerquisition.add(abandonTime);
                }
                /* if there's queue, put a job in queue on service on this server */
                s = e;
                if (numberSubscribedPerquisition >= SERVERS_SUBSCRIBED_PERQUISITION) {
                    service = m.getService(r, 198, P_SR);
                    sum[s].service += service;
                    sum[s].served++;
                    events[s].t = t.current + service;
                } else {
                    /* if there's no queue, deactivate this server */
                    events[s].x = 0;
                }
            } else if (e == ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1) {
                /* SUBSCRIBED PERQUISITION ABANDON */
                abandonsCounterSubscribedPerquisition++;
                abandonsSubscribedPerquisition.remove(0);
            } else {
                /* ticket check service */
                if (ticketCheckFirstCompletion == 0)
                    ticketCheckFirstCompletion = t.current;

                indexTicketCheck++;
                numberTicketCheck--;

                /*Check if is subscribed*/
                if (isSubscribed(r, 209, P7)) {
                    // generate arrival to dedicated queue
                    events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION].t = t.current;
                    events[ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION].x = 1;
                } else {
                    /* generate an arrival at the turnstiles */
                    events[ALL_EVENTS_TICKET].t = t.current;
                    events[ALL_EVENTS_TICKET].x = 1;
                }

                /* if there's queue, put a job in queue on service on this server */
                s = e;
                if (numberTicketCheck >= SERVERS_TICKET) {
                    service = m.getService(r, 220, TC_SR);
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
        String directory = "improved_model_batch_reports";

        /* TICKET CHECK */
        writeFile(delaysTicketCheck, directory, "delays_ticket_check");
        writeFile(responseTimesTicketCheck, directory, "response_times_ticket_check");
        writeFile(utilizationsTicketCheck, directory, "utilizations_ticket_check");
        writeFile(avgPopulationsTicketCheck, directory, "populations_ticket_check");
        writeFile(interarrivalsTicketCheck, directory, "interarrivals_ticket_check");
        writeFile(allAbandonsTicketCheck, directory, "abandons_ticket_check");
        writeFile(serviceTimesTicketCheck, directory, "service_times_ticket_check");

        /* TURNSTILES */
        writeFile(delaysTurnstiles, directory, "delays_turnstiles");
        writeFile(responseTimesTurnstiles, directory, "response_times_turnstiles");
        writeFile(utilizationsTurnstiles, directory, "utilizations_turnstiles");
        writeFile(avgPopulationsTurnstiles, directory, "populations_turnstiles");
        writeFile(interarrivalsTurnstiles, directory, "interarrivals_turnstiles");
        writeFile(serviceTimesTurnstiles, directory, "service_times_turnstiles");

        /* PERQUISITION */
        writeFile(delaysPerquisition, directory, "delays_perquisition");
        writeFile(responseTimesPerquisition, directory, "response_times_perquisition");
        writeFile(utilizationsPerquisition, directory, "utilizations_perquisition");
        writeFile(avgPopulationsPerquisition, directory, "populations_perquisition");
        writeFile(interarrivalsPerquisition, directory, "interarrivals_perquisition");
        writeFile(allAbandonsPerquisition, directory, "abandons_perquisition");
        writeFile(serviceTimesPerquisition, directory, "service_times_perquisition");
        writeFile(skipsCountersPerquisition, directory, "skips_perquisition");

        /* SUBSCRIBED TURNSTILES */
        writeFile(delaysSubTurnstiles, directory, "delays_sub_turnstiles");
        writeFile(responseTimesSubTurnstiles, directory, "response_times_sub_turnstiles");
        writeFile(utilizationsSubTurnstiles, directory, "utilizations_sub_turnstiles");
        writeFile(avgPopulationsSubTurnstiles, directory, "populations_sub_turnstiles");
        writeFile(interarrivalsSubTurnstiles, directory, "interarrivals_sub_turnstiles");
        writeFile(serviceTimesSubTurnstiles, directory, "service_times_sub_turnstiles");

        /* SUBSCRIBED PERQUISITION */
        writeFile(delaysSubPerquisition, directory, "delays_sub_perquisition");
        writeFile(responseTimesSubPerquisition, directory, "response_times_sub_perquisition");
        writeFile(utilizationsSubPerquisition, directory, "utilizations_sub_perquisition");
        writeFile(avgPopulationsSubPerquisition, directory, "populations_sub_perquisition");
        writeFile(interarrivalsSubPerquisition, directory, "interarrivals_sub_perquisition");
        writeFile(allAbandonsSubPerquisition, directory, "abandons_sub_perquisition");
        writeFile(serviceTimesSubPerquisition, directory, "service_times_sub_perquisition");

        /* INTERVAL ESTIMATION */

        Estimate estimate = new Estimate();

        List<String> filenames = List.of("response_times_ticket_check", "delays_ticket_check", "utilizations_ticket_check", "interarrivals_ticket_check", "abandons_ticket_check", "service_times_ticket_check", "populations_ticket_check",
                "response_times_turnstiles", "delays_turnstiles", "utilizations_turnstiles", "interarrivals_turnstiles", "service_times_turnstiles", "populations_turnstiles",
                "response_times_perquisition", "delays_perquisition", "utilizations_perquisition", "interarrivals_perquisition", "abandons_perquisition", "service_times_perquisition", "populations_perquisition", "skips_perquisition",
                "response_times_sub_turnstiles", "delays_sub_turnstiles", "utilizations_sub_turnstiles", "interarrivals_sub_turnstiles", "service_times_sub_turnstiles", "populations_sub_turnstiles",
                "response_times_sub_perquisition", "delays_sub_perquisition", "utilizations_sub_perquisition", "interarrivals_sub_perquisition", "abandons_sub_perquisition", "service_times_sub_perquisition", "populations_sub_perquisition"
        );

        for (String filename : filenames) {
            estimate.createInterval(directory, filename);
        }

    }


    static boolean generateSkip(Rngs rngs, int streamIndex, long queueSize) {
        rngs.selectStream(64);
        double percentage = Math.min(0.8, (0.444444 * queueSize - 291.555555) / 100);
        return rngs.random() <= percentage;
    }


    // this function generate a true value with (percentage * 100) % probability, oth. false
    static boolean generateAbandon(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(128);
        return rngs.random() <= percentage;
    }

    static boolean isSubscribed(Rngs rngs, int streamIndex, double percentage) {
        rngs.selectStream(224);
        return rngs.random() <= percentage;
    }

    /* this function returns the available turnstile server idle longest  */
    int findOneTurnstiles(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ARRIVAL_EVENT_TURNSTILES;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;

        while (i < ALL_EVENTS_TICKET + SERVERS_TURNSTILES) {         /* now, check the others to find which   */
            i++;                                                                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }

    /* this function returns the available subscribed turnstile server idle longest  */
    int findOneSubscribedTurnstiles(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + 1;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */

        s = i;

        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + SERVERS_SUBSCRIBED_TURNSTILES) {         /* now, check the others to find which   */
            i++;                                                                                             /* has been idle longest                 */
            if ((events[i].x == 0) && (events[i].t < events[s].t))
                s = i;
        }
        return (s);
    }

    /* this function returns the available turnstile server idle longest  */
    int findOneSubscribedPerquisition(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */

        s = i;

        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + SERVERS_SUBSCRIBED_PERQUISITION) {         /* now, check the others to find which   */
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


    int findPerquisition(MsqEvent[] events) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;

        int i = ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ARRIVAL_EVENT_PERQUISIION;

        while (events[i].x == 1)       /* find the index of the first available */
            i++;                      /* (idle) server                         */
        s = i;
        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + SERVERS_PERQUISITION) {         /* now, check the others to find which   */
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

    double getArrival(Rngs r, int streamIndex, double currentTime) {
        /* --------------------------------------------------------------
         * generate the next arrival time, exponential with rate given by the current time slot
         * --------------------------------------------------------------
         */
        r.selectStream(1 + streamIndex);

        int index = 0;  /* forcing the first time slot, for the verification step */

        sarrival += exponential(1 / (slotList.get(index).getAveragePoisson() / 3600), r);

        return (sarrival);
    }

    double getService(Rngs r, int streamIndex, double serviceTime) {
        r.selectStream(1 + streamIndex);
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
        while (i < ALL_EVENTS_TICKET + ALL_EVENTS_TURNSTILES + ALL_EVENTS_PERQUISITION + ALL_EVENTS_SUBSCRIBED_TURNSTILES + ALL_EVENTS_SUBSCRIBED_PERQUISITION - 1) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }


}




