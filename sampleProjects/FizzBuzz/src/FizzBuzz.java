/**
 * FizzBuzz - an interview question which is said to weed out many weak
 * programmers.
 *
 *<p> See <a href="http://c2.com/cgi/wiki?FizzBuzzTest">Fizz Buzz Test</a> and
 * <a href="http://imranontech.com/2007/01/24/using-fizzbuzz-to-find-developers-who-grok-coding/">
 * Using FizzBuzz to Find Developers who Grok Coding</a>
 *
 */
public class FizzBuzz {

    /**
     * Return the string representation of the parameter (e.g., for 1 return
     * "1"). This can be generated by Integer.toString(i). But for multiples of
     * three return "Fizz" instead of the number and for the multiples of five
     * return "Buzz". For numbers which are multiples of both three and five
     * return "FizzBuzz".
     * 
     * <p>Replace the throw statement with your implementation.
     */
    public static String fizzBuzz(int i) {
       throw new UnsupportedOperationException("You must implement this");
    }

    /**
     * Write a program that prints the numbers from 1 to 100. But for multiples
     * of three print "Fizz" instead of the number and for the multiples of five
     * print "Buzz". For numbers which are multiples of both three and five print
     * "FizzBuzz".
     * 
     * <p>You do not need to change this method.
     */
    public static void main(String args[]) {
        for(int i = 1; i <= 100; i++)
            System.out.println(fizzBuzz(i));

    }

}
