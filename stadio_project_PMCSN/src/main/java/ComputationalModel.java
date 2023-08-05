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


class MsqT {
    double current;                   /* current time                       */
    double next;                      /* next (most imminent) event time    */
}

class MsqSum {                      /* accumulated sums of                */
    double service;                   /*   service times                    */
    long   served;                    /*   number served                    */
}

class MsqEvent{                     /* the next-event list    */
    double t;                         /*   next event time      */
    int    x;                         /*   event status, 0 or 1 */
}


class Msq {
    static double START   = 0.0;            /* initial (open the door)        */
    static double STOP    = 20000.0;        /* terminal (close the door) time */
    static int    SERVERS = 4;              /* number of servers              */

    static double sarrival = START;

    static List<TimeSlot> slotlist = new ArrayList<TimeSlot>();

    public static void main(String[] args) {

        long   numberTicketCheck = 0;   /* number in the ticket check node    */
        long   numberPerquisition = 0;  /* number in the perquisition node    */
        int    e;                      /* next event index                    */
        int    s;                      /* server index                        */
        long   index  = 0;             /* used to count processed jobs        */
        double area = 0.0;
        double areaTicketCheck   = 0.0;           /* time integrated number in the node */
        double areaPerquisition   = 0.0;           /* time integrated number in the node */
        double service;
        long abandonTicketCheck = 0;
        long abandonPerquisition = 0;

        Msq m = new Msq();
        Rngs r = new Rngs();
        r.plantSeeds(0);

        for (int f=0 ; f< 3; f++){
            TimeSlot slot = new TimeSlot(PERCENTAGE[f], 1013, 3600*f, 3600*(f+1)-1);
            slotlist.add(slot);
        }

        MsqEvent [] event = new MsqEvent [ALL_EVENTS_VIP_TICKET+ALL_EVENTS_VIP_PERQUISITION];
        MsqSum [] sum = new MsqSum [ALL_EVENTS_VIP_TICKET+ALL_EVENTS_VIP_PERQUISITION];
        for (s = 0; s < ALL_EVENTS_VIP_TICKET+ALL_EVENTS_VIP_PERQUISITION; s++) {
            event[s] = new MsqEvent();
            sum [s]  = new MsqSum();
        }

        MsqT t = new MsqT();

        t.current    = START;
        event[0].t   = m.getArrival(r);
        event[0].x   = 1;
        for (s = 1; s < ALL_EVENTS_VIP_TICKET+ALL_EVENTS_VIP_PERQUISITION; s++) {
            event[s].t     = START;          /* this value is arbitrary because */
            event[s].x     = 0;              /* all servers are initially idle  */
            sum[s].service = 0.0;
            sum[s].served  = 0;
        }

        while ((event[0].x != 0) || (numberTicketCheck + numberPerquisition != 0)) {
            e         = m.nextEvent(event);                /* next event index */
            t.next    = event[e].t;                        /* next event time  */
            areaTicketCheck     += (t.next - t.current) * numberTicketCheck;     /* update integral  */
            areaPerquisition += (t.next - t.current) * numberPerquisition;     /* update integral  */
            t.current = t.next;                            /* advance the clock*/

            if (e == ARRIVAL_EVENT_TICKET-1) {                                  /* process an arrival*/
                numberTicketCheck++;
                //TODO FASCE ORARIE SU GET ARRIVAL, SETTA I LAMBDA
                event[0].t        = m.getArrival(r);
                if (event[0].t > STOP)
                    event[0].x      = 0;
                if (numberTicketCheck <= DEPARTURE_EVENT_VIP_TICKET) {
                    //TODO GET SERVICE ESPONENZIALE
                    service         = m.getService(r);
                    s               = m.findOne(event);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                    event[s].x      = 1;
                }
            }
            // todo modella il servizio del biglietto e gli abbandoni con gen. random
            else {                                         /* process a departure */
                index++;                                     /* from server s       */
                number--;
                s                 = e;
                if (number >= SERVERS) {
                    service         = m.getService(r);
                    sum[s].service += service;
                    sum[s].served++;
                    event[s].t      = t.current + service;
                }
                else
                    event[s].x      = 0;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + index + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(event[0].t / index));
        System.out.println("  avg wait ........... =   " + f.format(area / index));
        System.out.println("  avg # in node ...... =   " + f.format(area / t.current));

        for (s = 1; s <= SERVERS; s++)          /* adjust area to calculate */
            area -= sum[s].service;              /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(area / index));
        System.out.println("  avg # in queue ..... =   " + f.format(area / t.current));
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (s = 1; s <= SERVERS; s++) {
            System.out.print("       " + s + "          " + g.format(sum[s].service / t.current) + "            ");
            System.out.println(f.format(sum[s].service / sum[s].served) + "         " + g.format(sum[s].served / (double)index));
        }

        System.out.println("");
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

    double getArrival(Rngs r) {
        /* --------------------------------------------------------------
         * generate the next arrival time, with rate 1/2
         * --------------------------------------------------------------
         */
        r.selectStream(0);
        sarrival += exponential(2.0, r);
        return (sarrival);
    }


    double getService(Rngs r) {
        /* ------------------------------
         * generate the next service time, with rate 1/6
         * ------------------------------
         */
        r.selectStream(1);
        return (uniform(2.0, 10.0, r));
    }

    int nextEvent(MsqEvent [] event) {
        /* ---------------------------------------
         * return the index of the next event type
         * ---------------------------------------
         */
        int e;
        int i = 0;

        while (event[i].x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */
        e = i;
        while (i < ALL_EVENTS_VIP_TICKET+ALL_EVENTS_VIP_PERQUISITION-1) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */
            if ((event[i].x == 1) && (event[i].t < event[e].t))
                e = i;
        }
        return (e);
    }

    int findOne(MsqEvent [] event) {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;
        int i = 1;

        while (event[i].x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        //todo why +1, why i starts from 1?
        while (i < DEPARTURE_EVENT_VIP_TICKET+1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event[i].x == 0) && (event[i].t < event[s].t))
                s = i;
        }
        return (s);
    }

}