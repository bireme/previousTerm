package br.bireme.prvtrm;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Heitor Barbieri
 */
public class Test {

    public static void main(String[] args) throws IOException {
        final int size = 100; //50000;
        final MongoIndexInfo mii = new MongoIndexInfo("lil", 
            "/home/heitor/Projetos/DocumentSimilarity/lil", "tit","abs");
        final Set<MongoIndexInfo> set = new HashSet<MongoIndexInfo>();        
        set.add(mii);
        final PreviousTerm prev = new PreviousTerm(set, size);
        List<String> prevs;

        String start = "cadit"; //"cabíveis";//"cac"; //"cacinoma";
        System.out.println("*** " + start);
        final List<String> next = prev.getNextTerms("lil", start);
        for (String elem : next) {
            System.out.println(elem);
        }

        System.out.println("==================================================");

        //guessPreviousString("wygodzinskyorum", "wygodzinskyorum");

        String startp = next.get(next.size() - 1);//"cadragesimo";//next.get(next.size() - 1);//"caisse"; //"cait";//"cacnl1a4"; //next.get(next.size() - 1).getTerm();//"readytm";//"wylie"; //x";

long initTime = (new GregorianCalendar()).getTimeInMillis();
        prevs = prev.getPreviousTerms("lil", startp);
long endTime = (new GregorianCalendar()).getTimeInMillis();

        for (String elem : prevs) {
            System.out.println(elem);
        }
        System.out.println("==>" + startp);
        prev.close();

        /*
        System.out.println("==================================================");

        startp = "cacnl1a4"; //next.get(next.size() - 1).getTerm();//"readytm";//"wylie"; //x";
        prevs = prev.getPreviousTerms(startp, null, size-1);
        for (PreviousTerm.TermElem elem : prevs) {
            System.out.println(elem.getTerm() + " [" + elem.getTotal() + "]");
        }
        System.out.println(startp);
        prev.close();
        */
        System.out.println("Diftime = " + ((endTime - initTime) / 1000));
    }


}
