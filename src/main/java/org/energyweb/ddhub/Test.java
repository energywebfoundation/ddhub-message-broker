package org.energyweb.ddhub;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Test
{
    private static Base64 base64 = new Base64();

    public static void signRequestSK(HttpURLConnection request, String account, String key) throws Exception
    {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = fmt.format(Calendar.getInstance().getTime()) + " GMT";

        StringBuilder sb = new StringBuilder();
        sb.append("PUT\n"); // method
        sb.append('\n'); // content encoding
        sb.append('\n'); // content language
        sb.append('\n'); // content length
        sb.append('\n'); // md5 (optional)
        sb.append('\n'); // content type
        sb.append('\n'); // legacy date
        sb.append('\n'); // if-modified-since
        sb.append('\n'); // if-match
        sb.append('\n'); // if-none-match
        sb.append('\n'); // if-unmodified-since
        sb.append('\n'); // range
        sb.append("x-ms-date:" + date + '\n'); // headers
        sb.append("x-ms-version:2020-02-10\n");
        sb.append("/" + account + request.getURL().getPath() + "\ncomp:expiry");

        System.out.println(sb.toString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(base64.decode(key), "HmacSHA256"));
        String authKey = new String(base64.encode(mac.doFinal(sb.toString().getBytes("UTF-8"))));
        String auth = "SharedKey " + account + ":" + authKey;
        request.setRequestProperty("x-ms-date", date);
        request.setRequestProperty("x-ms-version", "2020-02-10");
        request.setRequestProperty("x-ms-expiry-option", "RelativeTonow");
        request.setRequestProperty("x-ms-expiry-time", "30000");
        request.setRequestProperty("x-ms-blob-type", "BlockBlob");
        request.setRequestProperty("Authorization", auth);
        request.setRequestMethod("PUT");
        request.setDoOutput(true);
        OutputStream os = request.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
        osw.write("");
        osw.flush();
        osw.close();
       
        System.out.println(auth);
    }

    public static void signRequestSKL(HttpURLConnection request, String account, String key) throws Exception
    {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = fmt.format(Calendar.getInstance().getTime()) + " GMT";

        StringBuilder sb = new StringBuilder();
        sb.append("GET\n"); // method
        sb.append('\n'); // md5 (optional)
        sb.append('\n'); // content type
        sb.append('\n'); // legacy date
        sb.append("x-ms-date:" + date + '\n'); // headers
        sb.append("x-ms-version:2020-02-10\n");
        sb.append("/" + account + request.getURL().getPath() + "?comp=expiry");

        //System.out.println(sb.toString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(base64.decode(key), "HmacSHA256"));
        String authKey = new String(base64.encode(mac.doFinal(sb.toString().getBytes("UTF-8"))));
        String auth = "SharedKeyLite " + account + ":" + authKey;
        request.setRequestProperty("x-ms-date", date);
        request.setRequestProperty("x-ms-version", "2020-02-10");
        request.setRequestProperty("x-ms-expiry-option", "NeverExpire");
//        request.setRequestProperty("x-ms-lease-id", "NeverExpire");
//        request.setRequestProperty("x-ms-expiry-time", "30000");
//        request.setRequestProperty("x-ms-blob-type", "BlockBlob");
        request.setRequestProperty("Authorization", auth);
        request.setRequestMethod("GET");
        request.setDoOutput(true);
        OutputStream os = request.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
        osw.write("");
        osw.flush();
        osw.close();
        System.out.println(request.getHeaderField("Content-Length"));
        System.out.println(auth);
    }



    public static void main(String args[]) throws Exception
    {
        String account = "vcaemo";
        String key = "lS5Zh7D4CMGcwFVOrJQzfUzRgV5B9Hetrn3iOXEf/G64+MHuC/tuXdpx5K83LqjbIgEgKyIrM/83tUdyANeVlA==";
        HttpURLConnection connection = (HttpURLConnection) (new URL("https://" + account + ".blob.core.windows.net/vcfile/1_azure_test/1_azure_test.621612808693423f36039098/621c74fe1a702950e629f7c6?comp=list")).openConnection();
        signRequestSKL(connection, account, key);
        connection.connect();
        System.out.println(connection.getResponseCode());
        System.out.println(connection.getResponseMessage());
        System.out.println(connection.getRequestMethod());
        System.out.println(connection.getContentLength());

        connection = (HttpURLConnection) (new URL("https://" + account + ".blob.core.windows.net/vcfile/1_azure_test/1_azure_test.621612808693423f36039098/621c74fe1a702950e629f7c6?comp=expiry")).openConnection();
        signRequestSK(connection, account, key);
        connection.connect();
        System.out.println(connection.getResponseMessage());
    }
}