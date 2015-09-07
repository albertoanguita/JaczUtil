package jacz.util.io.csv.test;

import jacz.util.io.csv.CSV;

/**
 * csv test
 */
public class Test {

    private static final String separator = "º";

    private static final String nullValue = "@null@";

    public static void main(String[] args) throws Exception {

        String path = "D:\\Proyectos\\JacuzziPeerEngineClient\\examples\\configs\\user_0\\databases\\integrated\\CSV_PersonLibrary.csv";

        CSV csv = CSV.load(path, separator, nullValue);

        System.out.println("END");
    }
}
