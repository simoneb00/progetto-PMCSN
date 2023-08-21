package batch;

/* ----------------------------------------------------------------------
 * This program reads a data sample from a text file in the format
 *                         one data point per line 
 * and calculates an interval estimate for the mean of that (unknown) much 
 * larger set of data from which this sample was drawn.  The data can be 
 * either discrete or continuous.  A compiled version of this program 
 * supports redirection and can used just like program uvs.c. 
 * 
 * Name              : Estimate.java (Interval Estimation) 
 * Authors           : Steve Park & Dave Geyer 
 * Translated By     : Richard Dutton & Jun Wang
 * Language          : Java
 * Latest Revision   : 6-16-06 
 * ----------------------------------------------------------------------
 */

import java.lang.Math;
import java.io.*;
import java.text.*;
import java.util.StringTokenizer;

public class Estimate{
    
    static final double LOC = 0.95;    /* level of confidence,        */ 
                                       /* use 0.95 for 95% confidence */

    public static void main(String[] args)
    {
	long   n    = 0;                     /* counts data points */
	double sum  = 0.0;
	double mean = 0.0;
	double data;
	double stdev;
	double u, t, w;
	double diff;
	
	String line = "";
	
	Rvms rvms = new Rvms();

	BufferedReader br = null;
	File file = new File("batch_reports/delay_ticket_check.csv");
		try {

			FileInputStream inputStream = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(inputStream));

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	try{
	    line = br.readLine();
	    
	    while (line!=null) {         /* use Welford's one-pass method */
			StringTokenizer tokenizer = new StringTokenizer(line);
			if(tokenizer.hasMoreTokens()){
		    	data = Double.parseDouble(tokenizer.nextToken());
		    
		    n++;                 /* and standard deviation        */
		    diff  = data - mean;
		    sum  += diff * diff * (n - 1.0) / n;
		    mean += diff / n;
		}  
		
		line = br.readLine();
		
	    }
	}catch(IOException e){
	    System.err.println(e);
	    System.exit(1);
	}
	
	stdev  = Math.sqrt(sum / n);
	
	DecimalFormat df = new DecimalFormat("###0.00");
	
	if (n > 1) {
	    u = 1.0 - 0.5 * (1.0 - LOC);              /* interval parameter  */
	    t = rvms.idfStudent(n - 1, u);            /* critical value of t */
	    w = t * stdev / Math.sqrt(n - 1);         /* interval half width */

	    System.out.print("\nbased upon " + n + " data points");
	    System.out.print(" and with " + (int) (100.0 * LOC + 0.5) + 
		"% confidence\n");
	    System.out.print("the expected value is in the interval ");
	    System.out.print( df.format(mean) + " +/- " + df.format(w) + "\n");
	}
	else{
	    System.out.print("ERROR - insufficient data\n");
	}
    }
}
