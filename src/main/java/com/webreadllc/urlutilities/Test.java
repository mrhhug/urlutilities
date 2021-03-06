package com.webreadllc.urlutilities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author michael
 */
public class Test {

    private final String[] args;

    //give these to an admin in a gui for configuration
    private final String inputBaseName;
    private final String outputBaseName;
    private final String configSuffix;
    private final String jsonSuffix;
    private final String home;

    public Test(String[] args) {
        this.args = args;
        this.inputBaseName = "urls";
        this.outputBaseName = "sizes";
        this.configSuffix = ".txt";
        this.jsonSuffix = ".json";
	this.home = System.getProperty("user.dir");
        //this.home = System.getProperty("user.home");
    }

    public void test() {
        testSizer();
        testLocalConfig();
        testLocalJson();
	getTotalSize();
    }

    private void testSizer() {
        try{
            System.out.println(new SizeThread("http://google.com/").call());
            System.out.println(new SizeThread("http://stackoverflow.com").call());
            //FTP is common enough to get it's own method.. but this is just an exercise
            System.out.println(new SizeThread("ftp://anonymous:ftp@speedtest.tele2.net/1KB.zip;type=d").call());
        } catch (Exception e) {
            System.out.println("Failed test");
            e.printStackTrace();
        }
    }

    private void testLocalConfig() {
        String inputPath = home + File.separator + inputBaseName + configSuffix;
        String outputPath = home + File.separator + outputBaseName + configSuffix;

        List<String> ret = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(inputPath));
            for ( String i : lines) {
                //thread pool doesn't really provide much value here and would probobly freak out casuals
                //large collections of data typically come as json / xml / yaml
                //garunteed order might matter too
                ret.add(new SizeThread(i).call().toString());
            }
            Files.write(Paths.get(outputPath), ret);
        } catch (Exception e) {
            System.out.println("Failed test");
            e.printStackTrace();
        }
    }

    private void testLocalJson() {
        String inputPath = home + File.separator + inputBaseName + jsonSuffix;
        String outputPath = home + File.separator + outputBaseName + jsonSuffix;

        //Gson is..... it works...
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>(){}.getType();
        //put water in the pool
        ExecutorService pool = Executors.newCachedThreadPool();
        List<Future<SizeReturn>> swimmers = new ArrayList<>();
        //i would ask for clarification and heuristics if this was more than an exercise
        //JSON arrays are ordered so preserve order
        List<SizeReturn> finishers = new ArrayList<>();

        try {
            List<String> fromJson = gson.fromJson(Files.newBufferedReader(Paths.get(inputPath)),type);
            for( String i : fromJson)
                swimmers.add(pool.submit(new SizeThread(i)));
            pool.shutdown();
            for( Future<SizeReturn> i : swimmers)
                finishers.add(i.get());
            Files.write(Paths.get(outputPath), gson.toJson(finishers).getBytes());

        } catch (Exception e) {
            System.out.println("Failed test");
            e.printStackTrace();
        }

    }

    private void getTotalSize() {
	long total = 0;
	String baseUrl = "https://www.archlinux.org/";
	try {
	    Document doc = Jsoup.connect(baseUrl).get();
	    Elements linkedContent = doc.select("[src]");
	    for ( Element i : linkedContent) {
		total += new SizeThread(i.absUrl("src")).call().size;
	    }
	    ///don't get off by one errors, remember the parent
	    total += new SizeThread(baseUrl).call().size;
	    System.out.println( 1 + linkedContent.size() + " urls contained "+total+" bytes ");
	} catch (Exception e) {
	    System.out.println("Failed test");
            e.printStackTrace();
	}
    }
}