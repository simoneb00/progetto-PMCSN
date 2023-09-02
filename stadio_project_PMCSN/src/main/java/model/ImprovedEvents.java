package model;

public class ImprovedEvents {
    //---- VIP EVENTS ----
    // Ticket check
    public static int ARRIVAL_EVENT_VIP_TICKET = 1;
    public static int SERVERS_VIP_TICKET = 4;
    public static int ABANDON_EVENT_VIP_TICKET = 1;

    // Perquisition
    public static int ARRIVAL_EVENT_VIP_PERQUISIION = 1;
    public static int SERVERS_VIP_PERQUISITION = 5;
    public static int ABANDON_EVENT_VIP_PERQUISITION = 1;


    //---- NOT VIP EVENTS ----
    // Ticket check
    public static int ARRIVAL_EVENT_TICKET = 1;
    public static int SERVERS_TICKET = 10;
    public static int ABANDON_EVENT_TICKET = 1;


    // Turnstiles
    public static int ARRIVAL_EVENT_TURNSTILES = 1;
    public static int SERVERS_TURNSTILES = 8;


    //  Perquisition
    public static int ARRIVAL_EVENT_PERQUISIION = 1;
    public static int SERVERS_PERQUISITION = 20;
    public static int SKIP_EVENT_SECOND_PERQUISITION = 1;
    public static int ABANDON_EVENT_SECOND_PERQUISITION = 1;

    // ---- ALL EVENTS ----
    public static int ALL_EVENTS_VIP_TICKET = ARRIVAL_EVENT_VIP_TICKET + SERVERS_VIP_TICKET + ABANDON_EVENT_VIP_TICKET;
    public static int ALL_EVENTS_VIP_PERQUISITION = ARRIVAL_EVENT_VIP_PERQUISIION + SERVERS_VIP_PERQUISITION + ABANDON_EVENT_VIP_PERQUISITION;
    public static int ALL_EVENTS_TICKET = ARRIVAL_EVENT_TICKET + SERVERS_TICKET + ABANDON_EVENT_TICKET;
    public static int ALL_EVENTS_TURNSTILES = ARRIVAL_EVENT_TURNSTILES + SERVERS_TURNSTILES;
    public static int ALL_EVENTS_PERQUISITION = ARRIVAL_EVENT_PERQUISIION + SERVERS_PERQUISITION + ABANDON_EVENT_SECOND_PERQUISITION + SKIP_EVENT_SECOND_PERQUISITION;

}
