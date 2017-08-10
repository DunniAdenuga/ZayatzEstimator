/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ZayatzEstimator;

import datafly.DataFly;
import datafly.PrivateTable;
import datafly.TableRow;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

/**
 *
 * @author adenugad
 */
public class ZayatzEstimator {
    PrivateTable myTable;
    double samplingFrac;
    
    public static void main(String[] args) throws FileNotFoundException, SQLException, ClassNotFoundException  {
        DataFly dataFly = new DataFly();
        ZayatzEstimator zayatzEstimator = new ZayatzEstimator();
                
        /****/
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://audgendb.c9az8e0qjbgo.us-east-1.rds.amazonaws.com:5432/data";
        Properties props = new Properties();
        props.setProperty("user", "****");//UPDATE
        props.setProperty("password", "****");//UPDATE
        dataFly.setConn(DriverManager.getConnection(url, props)); //uncomment when connecting to DB
        zayatzEstimator.myTable = dataFly.startGeneralization(dataFly.setup());
        //zayatzEstimator.myTable = dataFly.setup();
        //zayatzEstimator.myTable.printFormat();
        System.out.print("What's your desired sampling fraction ? ");
        Scanner keyboard = new Scanner(System.in);
        zayatzEstimator.samplingFrac = keyboard.nextDouble();
        
        //System.out.println("85! - " + zayatzEstimator.findFactorial(85));
        System.out.printf("Risk Estimate: %.1f percent \n", 
                zayatzEstimator.mainZayatzEstimator(zayatzEstimator.generateRandomSample(zayatzEstimator.myTable)));
        
    } 
    
    public ArrayList<TableRow> generateRandomSample(PrivateTable myTable){
        Random rand = new Random();
        ArrayList<TableRow> sampleTable  = new ArrayList<>();
        int sampleSize = (int)(samplingFrac * myTable.getTableSize());
        //System.out.println("sample size: " + sampleSize);
        ArrayList<Integer> allRandomNumbersGenerated = new ArrayList<>();
        int x = 0;
        
        while(x < sampleSize){
            int randomLoc = rand.nextInt(myTable.getTableSize());
            if(allRandomNumbersGenerated.contains(randomLoc) == false){
                allRandomNumbersGenerated.add(randomLoc);
                sampleTable.add(myTable.getTableRows().get(randomLoc));
                x++;
            }
        }
    //System.out.println(sampleTable);
    return sampleTable;    
    }
    
    //returns an equivalence class and its size
    public HashMap<ArrayList, Integer>  divideIntoEquivalenceClasses(ArrayList<TableRow> sample){
        DataFly dataFly = new DataFly();
        ArrayList<Integer> quasiColNum = dataFly.getQuasiColNum(myTable);
        int i = 0;
        HashMap<ArrayList, Integer> freqSet = new HashMap<>();
        
        while(i < sample.size()){
            //get quasiIden for each row
            ArrayList<String> quasiIden = new ArrayList<>();
            for(int x = 0; x < quasiColNum.size(); x++){
                quasiIden.add(sample.get(i).getData().get(x));
                //System.out.println("quasiIden " + quasiIden);
            }
            if(freqSet.containsKey(quasiIden)){
                freqSet.replace(quasiIden, freqSet.get(quasiIden),freqSet.get(quasiIden)+ 1); 
            }
            else{
                freqSet.put(quasiIden, 1);
            }
            i++;
        }
        //System.out.println(freqSet);
        return freqSet;
    }
    
    /**
     * Find proportion of equivalence classes in the population that are of size j
     * Instead of population, we estimate using sample 
     * @param sample
     * @param j
     * @return 
     */
    public BigDecimal findPJ(ArrayList<TableRow> sample, int j){
        HashMap<ArrayList, Integer> equivClasses =  divideIntoEquivalenceClasses(sample);
        //System.out.println(equivClasses);
        BigDecimal noOfTimes = BigDecimal.ZERO;//number of times there's an equivalence class with size j
        Integer[] equivClassSizes = new Integer[equivClasses.size()]; 
        equivClassSizes = equivClasses.values().toArray(equivClassSizes);
        
        //System.out.println("J - " + j);
        for (Integer equivClassSize : equivClassSizes) {
            //System.out.println("equivClassSize - " + equivClassSize);
            if (equivClassSize == j) {
                //System.out.println("madeIt!");
               noOfTimes =  noOfTimes.add(BigDecimal.ONE);
                //System.out.println("noOfTimes - " + noOfTimes);
            }
        }
        //System.out.println("noOftimes, J - " + noOfTimes + ", " + j);
        //System.out.println("noOfTimes/equivClasses.size - "
                //+ noOfTimes.divide(new BigDecimal(equivClasses.size()) , 2, RoundingMode.HALF_UP));
        return noOfTimes.divide(new BigDecimal(equivClasses.size()) , 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Find the probability 
     * that an equivalence class of size C in the population will be 
     * represented by an equivalence class of size 1 in the sample 
     * Follows hypergeometric distribution formula
     * @param sample
     * @param c
     * @return 
     */
    public BigDecimal findProb(ArrayList<TableRow> sample, int c){
        BigDecimal top = findCombination(c, 1).multiply(
                findCombination(myTable.getTableSize()-c, sample.size()-1));
        BigDecimal bottom = findCombination(myTable.getTableSize(), sample.size());
        //System.out.println("top- " + top);
        //System.out.println("bottom- " + bottom);
        return top.divide(bottom, 2,RoundingMode.HALF_UP );
    }
    
    public BigDecimal findFactorial(int n){
        BigDecimal product;
       // System.out.println("n - " + n);
        if(n == 0){
            product =  new BigDecimal(1);
        }
        else{
        product =  new BigDecimal(n);    
        for(int i = n - 1; i > 0; i--){
            product = product.multiply(new BigDecimal(i));
        }
        }
       // System.out.println("factorialProduct - " + product);
        return product;
    }
    
    public BigDecimal findCombination(int x, int y){
       // System.out.println("x - " + x);
       // System.out.println("y - " + y);
        return findFactorial(x).divide(findFactorial(y).multiply(findFactorial(x-y)));
    }
    
    /**
     * calculate the probability that a sample unique 
     * is also a population unique
     * @param sample
     * @return 
     */
    public BigDecimal findMainProb(ArrayList<TableRow> sample){
        //System.out.println("findProb(sample, 1) " +findProb(sample, 1));
        //System.out.println("findPJ(sample, 1) " +findPJ(sample, 1));
        BigDecimal top = findPJ(sample, 1).multiply(findProb(sample, 1));
        BigDecimal sumOfBottoms = new BigDecimal(0);
        int maxEquivClassSize = max(sample);
        for(int i = 1; i <= maxEquivClassSize; i++){
             //System.out.println("findPJ(sample, i) " +findPJ(sample, i));
            //System.out.println("findProb(sample, i) " +findProb(sample, i));
            sumOfBottoms = sumOfBottoms.add
                    (findPJ(sample, i).multiply(findProb(sample, i)));
        }
        //System.out.println("top: " + top);
        //System.out.println("sumOfBottoms: " + sumOfBottoms);
        return top.divide(sumOfBottoms, 2, RoundingMode.HALF_UP);
    }

    private int max(ArrayList<TableRow> sample) {
        HashMap<ArrayList, Integer> equivClasses =  divideIntoEquivalenceClasses(sample);
        Integer[] equivClassSizes = new Integer[equivClasses.size()]; 
        equivClassSizes = equivClasses.values().toArray(equivClassSizes);
        
        int maxi = equivClassSizes[0];
        for(int i = 1; i < equivClassSizes.length; i++){
            if(equivClassSizes[i] > maxi){
                maxi = equivClassSizes[i];
            }
        }
        return maxi;
    }
    
    /**
     * Find the number of equivalence classes of size i in the sample , 
     * @param sample
     * @param i
     * @return 
     */
    public double findSI(ArrayList<TableRow> sample, int i){
        HashMap<ArrayList, Integer> equivClasses =  divideIntoEquivalenceClasses(sample);
        int noOfTimes = 0;//number of times there's an equivalence class with size j
        Integer[] equivClassSizes = new Integer[equivClasses.size()]; 
        equivClassSizes = equivClasses.values().toArray(equivClassSizes);
        
        for (Integer equivClassSize : equivClassSizes) {
            if (equivClassSize == i) {
                noOfTimes++;
            }
        }
        return noOfTimes;
    } 
    
    public BigDecimal mainZayatzEstimator(ArrayList<TableRow> sample){
           //System.out.println(divideIntoEquivalenceClasses(sample));

        return new BigDecimal(findSI(sample, 1)).multiply(findMainProb(sample)).multiply(
               new BigDecimal(myTable.getTableSize()/sample.size())).multiply(new BigDecimal(100))
                .divide(new BigDecimal(myTable.getTableSize()), 2, RoundingMode.HALF_UP);
    }
    
}
