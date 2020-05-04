/********************************************************************************/
/*										*/
/*	Test cases for http server						*/
/*										*/
/********************************************************************************/


package edu.brown.cs.spr.sample.httpserver;


import org.junit.*;
import java.net.*;
import javax.net.ssl.*;
import java.io.*;


public class HttpServerTest {

private static final int port = 22375;

private HttpServer	the_server;



@Before
public void setup()
{
   the_server = new HttpServer(port);
   the_server.start();
}


@After
public void shutdown()
{
   the_server.stop();
   the_server = null;
}




@Test
public void testOk() throws Exception
{
   URL u = new URL("http://localhost:" + port + "/");
   HttpURLConnection conn = (HttpURLConnection) u.openConnection();
   int sts = conn.getResponseCode();
   Assert.assertEquals(sts,200);
}


@Test
public void testFile() throws Exception
{
   URL u = new URL("http://localhost:" + port + "/index.html");
   HttpURLConnection conn = (HttpURLConnection) u.openConnection();
   int sts = conn.getResponseCode();
   Reader rdr = new InputStreamReader(conn.getInputStream());
   StringBuffer buf = new StringBuffer();
   char [] b1 = new char[8192];
   for ( ; ; ) {
      int rln = rdr.read(b1);
      if (rln <= 0) break;
      buf.append(b1);
    }

   Assert.assertEquals(sts,200);

   String cnts = buf.toString();
   Assert.assertTrue(cnts.startsWith("<html>"));
}



@Test
public void testTLS() throws Exception
{
   URL u = new URL("https://localhost:" + port + "/");
   HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();
   int sts = conn.getResponseCode();
   Assert.assertEquals(sts,200);
}








}	// end of class HttpServerTest



/* end of HttpServerTest.java */



















