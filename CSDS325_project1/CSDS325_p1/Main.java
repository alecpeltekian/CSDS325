//import com.sun.corba.se.spi.orbutil.fsm.Input;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException {

        //Grab port number from portnum
        BufferedReader br = new BufferedReader(new FileReader("portnum"));
        int port = Integer.parseInt(br.readLine());
        // Open the server socket to begin listening for connections
        ServerSocket serverSocket = new ServerSocket(port);

        // Load a cached thread pool to handle incoming connections
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

        while (true) {
            final Socket socket = serverSocket.accept();
            System.out.println("Received connection request from " + socket.getInetAddress().getHostName());
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    OutputStream outputStream = null;
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        outputStream = socket.getOutputStream();

                        Map<String, String> headerMap = new HashMap<String, String>();
                        String requestPath = "";
                        String requestMethod = "";

                        // Load headers into a key value map
                        int i = 0;
                        String line;
                        while (!(line = in.readLine()).equals("")) {
                            if (i == 0) {
                                // The first header line contains the method and path, parse these values and store them in their respective variables
                                requestMethod = line.split(" ")[0];
                                requestPath = line.split(" ")[1];
                            } else {
                                String key = line.split(":")[0];
                                String value = line.split(":")[1];
                                headerMap.put(key, value);
                            }
                            i++;
                        }

                        // Load cookies from headers and store them in a key-value map
                        String cookie = headerMap.get("Cookie");
                        Map<String, String> cookieMap = new HashMap<String, String>();
                        if (headerMap.containsKey("Cookie")) {
                            String[] cookies = headerMap.get("Cookie").replace("\r", "").split(";");

                            for (String c : cookies) {
                                String[] s = c.trim().split("=");
                                String key = s[0];
                                String value = "";
                                if (s.length > 1)
                                    value = s[1];
                                cookieMap.put(key, value);
                            }
                        }

                        //grab contents of each html file
                        String content1 = new String(Files.readAllBytes(Paths.get("test1.html")), StandardCharsets.UTF_8);
                        String content2 = new String(Files.readAllBytes(Paths.get("test2.html")), StandardCharsets.UTF_8);
                        String content3 = new String(Files.readAllBytes(Paths.get("visits.html")), StandardCharsets.UTF_8);
                        String content4 = new String(Files.readAllBytes(Paths.get("errors.html")), StandardCharsets.UTF_8);

                        String body = null;

                        if (requestPath.equals("/akp96/test1.html")) {
                            //print content of test1 if request is test1
                            //set body to content of test1 file
                            body = content1;
                        } else if (requestPath.equals("/akp96/test2.html")) {
                            //print content of test1 if request is test2
                            //set body to content of test2 file
                            body = content2;
                        } else if (requestPath.equals("/akp96/visits.html")) {
                            //print content of test1 if request is visits
                            //set body to content of visits file
                            body = content3;

                        } else {
                            //print 404 error if no html file request
                            System.out.println("404 ERROR -> " + requestPath);
                            //set body to content of error file
                            body = content4;
                        }


                        String response = null;
                        if (body.equals(content4)) {
                            // Build HTTP header response for error page
                            response = "HTTP/1.1 404 File Not Found\r\n" +
                                    "Content-Length: " + body.getBytes().length + "\r\n" +
                                    "Content-Type: text/html; charset-utf-8\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n" + body;
                        } else {
                            // Increment the visits cookie with each successful request
                            String cookieHeader = "Set-Cookie: alec=";
                            int numVisits = 1;
                            try {
                                if (cookieMap.containsKey("alec")) {
                                    numVisits += Integer.parseInt(cookieMap.get("alec"));
                                }
                            } catch (NumberFormatException ex) {
                                System.err.println("Malformed visits cookie");
                            }

                            // Replace {} with the number of visits in visits.html
                            if (requestPath.equals("/akp96/visits.html")) {
                                body = body.replace("{}", String.valueOf(numVisits));
                            }

                            // Build HTTP header response for OK response
                            response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Length: " + body.getBytes().length + "\r\n" +
                                    "Content-Type: text/html; charset-utf-8\r\n" +
                                    "Connection: close\r\n";

                            cookieHeader += numVisits + "; Path=/akp96\r\n";
                            response += cookieHeader + "\r\n" + body;
                        }

                        outputStream.write(response.getBytes());
                        outputStream.flush();

                        // Close and clean up readers streams and sockets
                        in.close();
                        outputStream.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
