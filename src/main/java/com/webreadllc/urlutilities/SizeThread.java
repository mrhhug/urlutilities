package com.webreadllc.urlutilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

/**
 * @author michael
 */
public class SizeThread implements Callable<SizeReturn> {
    
    private final String potentialURL;

    public SizeThread(String potentialURL) {
        this.potentialURL = potentialURL;
    }

    /**
     * Have personally found Apache HttpComponents far too unstable right now
     * Apache HttpComponents also would not handle generic URLs
     * @throws java.net.MalformedURLException
     */
    @Override
    public SizeReturn call() throws MalformedURLException, IOException {
        Thread.currentThread().setName(potentialURL);
        long bytes;

        URL url = new URL(potentialURL);
        //there are many reasons using HEAD would return -1
        //Content-Length is an optional field
        //could be not HTTP
        bytes = getSizeFromHttpHead(url);
        if(bytes < 0 )
            bytes = getSizeofURL(url);

        return new SizeReturn(potentialURL, bytes);
    }

    /**
     * Ask the http server the size of the file
     */
    private long getSizeFromHttpHead(URL url) throws IOException {
        long bytes;

        try{
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            //this could come off as pompous
            con.setRequestProperty ("User-agent", "Michael Hug");
            con.setRequestMethod("HEAD");
            con.connect();
            bytes = con.getContentLengthLong();
            con.disconnect();
        }
        catch (ClassCastException e){
            bytes = -1;
        }

        return bytes;
    }

    /**
     * Actually transfer the url and count bytes
     */
    private long getSizeofURL(URL url) throws IOException {
        long bytes = 0;
        //is a zero byte return reasonable? should this be -1?
        URLConnection con = url.openConnection();
        InputStream is = con.getInputStream();
        int buff = is.read();
        while ( buff  != -1) {
            bytes += buff;
            buff = is.read();
        }
        //might check for -1 then throw exception?
        return bytes;
    }
}

//make this public for java APIs
class SizeReturn {
    
    String url;
    long size;

    public SizeReturn(String url, long size) {
        this.url = url;
        this.size = size;
    }

    @Override
    public String toString() {
        return url + "," + size;
    }
}