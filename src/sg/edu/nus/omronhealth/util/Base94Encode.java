package sg.edu.nus.omronhealth.util;

import java.util.Locale;

public class Base94Encode {
	
	// Uses the printable subset of the ASCII table for output
	// ranges from 33('!') to 126('~')
	// ascii 32 which is space (' ') is used as a delimiter, hence not used here.

	final static int BASE = 95-1; 
	final static int ASCII_OFFSET = 33; 
		
    public static int pow(int a, int exp)
    {
            int power = 1;
            for(int c=0;c<exp;c++)
            power*=a;
            return power;
    }
    
    public static String encodeBase94(int a){
		// fixed digits (0-9) to base (95-1)
		// only for up to 4 bits
		// int maths
		int q1 = a / BASE;
		int r1 = a % BASE;
		int q2 = q1 / BASE;
		int r2 = q1 % BASE;
		int q3 = q2 / BASE;
		int r3 = q2 % BASE;
		int q4 = q3 / BASE;
		int r4 = q3 % BASE;

		r1 += ASCII_OFFSET;
		r2 += ASCII_OFFSET;
		r3 += ASCII_OFFSET;
		r4 += ASCII_OFFSET;

		System.out.println("q1:" + q1 + " q2:" + q2+" q3:"+q3+" q4:" +q4);
		System.out.println("r1:" + r1 + " r2:" + r2+" r3:"+r3+" r4:" +r4);
		
		return String.format(Locale.US, "%c%c%c%c", r1,r2,r3,r4);
    }
    
    public static String encodeBase94_data(int a){
		// fixed digits (0-9) to base (95-1)
		// only for up to 5 bits (data is larger)
		// int maths
		int q1 = a / BASE;
		int r1 = a % BASE;
		int q2 = q1 / BASE;
		int r2 = q1 % BASE;
		int q3 = q2 / BASE;
		int r3 = q2 % BASE;
		int q4 = q3 / BASE;
		int r4 = q3 % BASE;
		int q5 = q4 / BASE;
		int r5 = q4 % BASE;

		r1 += ASCII_OFFSET;
		r2 += ASCII_OFFSET;
		r3 += ASCII_OFFSET;
		r4 += ASCII_OFFSET;
		r5 += ASCII_OFFSET;

		System.out.println("q1:" + q1 + " q2:" + q2+" q3:"+q3+" q4:" +q4+" q5: "+q5);
		System.out.println("r1:" + r1 + " r2:" + r2+" r3:"+r3+" r4:" +r4+" r5: "+r5);
		
		return String.format(Locale.US, "%c%c%c%c%c", r1,r2,r3,r4,r5);
    }
    
    public static int decodeBase94(String s){
    	int r1 = s.charAt(0) - ASCII_OFFSET;
    	int r2 = s.charAt(1) - ASCII_OFFSET;
    	int r3 = s.charAt(2) - ASCII_OFFSET;
    	int r4 = s.charAt(3) - ASCII_OFFSET;
    	int r5 = s.charAt(4) - ASCII_OFFSET;
    	System.out.println("r1:" + r1);
    	System.out.println("r2:" + r2);
    	System.out.println("r3:" + r3);
    	System.out.println("r4:" + r4);
    	System.out.println("r5:" + r5);
    	
		int reverse = r1 +
			      r2 * pow(BASE, 1) +
			      r3 * pow(BASE, 2) +
			      r4 * pow(BASE, 3) +
	      		  r5 * pow(BASE, 4) ;
		
		System.out.println("Reverse: " + reverse );
		
    	return reverse;
    }
}
