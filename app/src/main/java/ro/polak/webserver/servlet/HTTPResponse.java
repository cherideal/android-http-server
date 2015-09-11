package ro.polak.webserver.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.io.InputStream;

import ro.polak.utilities.Utilities;
import ro.polak.webserver.HTTPResponseHeaders;
import ro.polak.webserver.Statistics;
import ro.polak.webserver.WebServer;
import ro.polak.webserver.controller.MainController;

import android.content.Context;

/**
 * Represents HTTP response
 *
 * @author Piotr Polak <a href="http://www.polak.ro/">www.polak.ro</a>
 * @version 1.0.1/13.04.2008
 */
public class HTTPResponse {

    private Socket socket;
    private HTTPResponseHeaders headers;
    private OutputStream out;
    private PrintWriter printWriter = null;
    private boolean headersFlushed = false;

    /**
     * Class constructor
     *
     * @param socket - socket to be written
     */
    public HTTPResponse(Socket socket) {
        this.socket = socket;
        headers = new HTTPResponseHeaders();
        try {
            out = socket.getOutputStream();
        } catch (Exception e) { /* e.printStackTrace(); */
        }
    }

    /**
     * Writes byte array to the output
     *
     * @param byteArray byte array
     */
    public void write(byte[] byteArray) {
        try {
            Statistics.addBytesSend(byteArray.length);
            out.write(byteArray);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes string to the output
     *
     * @param s String to be written
     */
    public void write(String s) {
        write(s.getBytes());
    }

    /**
     * Sets cookie
     *
     * @param cookieName           name of the cookie
     * @param cookieValue          String value of the cookie
     * @param cookieExpiresSeconds expire time in seconds
     * @param cookiePath           path for the cookie
     */
    public void setCookie(String cookieName, String cookieValue, int cookieExpiresSeconds, String cookiePath) {
        String cookie = cookieName + "=" + Utilities.URLEncode(cookieValue);

        if (cookieExpiresSeconds != 0) {
            cookie += "; expires="
                    + WebServer.sdf.format(new java.util.Date(System.currentTimeMillis()
                    + (cookieExpiresSeconds * 1000)));
        }

        if (cookiePath != null) {
            cookie += "; path=" + cookiePath;
        }

        headers.setHeader("Set-Cookie", cookie);
    }

    /**
     * Sets cookie, expires when browser closed
     *
     * @param cookieName  name of the cookie
     * @param cookieValue String value of the cookie
     */
    public void setCookie(String cookieName, String cookieValue) {
        setCookie(cookieName, cookieValue, 0, null);
    }

    /**
     * Sets cookie
     * <p/>
     * Use negative time to remove cookie
     *
     * @param cookieName        name of the cookie
     * @param cookieValue       String value of the cookie
     * @param timeToLiveSeconds time to live in seconds
     */
    public void setCookie(String cookieName, String cookieValue, int timeToLiveSeconds) {
        setCookie(cookieName, cookieValue, timeToLiveSeconds, null);
    }

    /**
     * Removes cookie
     *
     * @param cookieName name of the cookie
     */
    public void removeCookie(String cookieName) {
        setCookie(cookieName, "", -1, null);
    }

    /**
     * Flushes headers, returns false when headers already flushed.
     * <p/>
     * Can be called once per responce, after the fisrt call it "locks"
     *
     * @return true if headers flushed
     */
    public boolean flushHeaders() {
        if (headersFlushed) {
            return false; // Prevents double flushing
        }
        headersFlushed = true;
        write(headers.toString());
        // headers = null; // TODO check why this must be null? 25.11.2009
        return true;
    }

    /**
     * Returns a boolean indicating if the response has been committed. A
     * commited response has already had its status code and headers written.
     *
     * @return a boolean indicating if the response has been committed
     */
    public boolean isCommitted() {
        return headersFlushed;
    }

    /**
     * Flushes the output
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        this.out.flush();
    }

    /**
     * Redirects the request to the specified location.
     *
     * @param location - relative or absolute path (URL)
     */
    public void sendRedirect(String location) {
        headers.setStatus(HTTPResponseHeaders.STATUS_MOVED_PERMANENTLY);
        headers.setHeader("Location", location);
        this.flushHeaders();
    }

    /**
     * Flushes headers and serves the specified file
     *
     * @param file file to be served
     */
    public void serveFile(File file) {
        this.flushHeaders();

        MainController.getInstance().println("Serving file " + file.getPath());

        FileInputStream file_input = null;
        int buffer_read_n_bytes = 0;
        byte[] buffer = new byte[512];

        try {
            file_input = new FileInputStream(file);

            try {

                while ((buffer_read_n_bytes = file_input.read(buffer)) != -1) { // While
                    // reading
                    // from
                    // file
                    this.out.write(buffer, 0, buffer_read_n_bytes); // Writing
                    // to buffer
                    this.out.flush(); // Flushing the buffer
                    Statistics.addBytesSend(buffer_read_n_bytes);
                }
                this.out.flush(); // Flushing remaining buffer

            } catch (IOException e) {
            }

            try {
                file_input.close();
            } // Closing file input stream
            catch (IOException e) {
            }
        } catch (FileNotFoundException e) {
        }

    }

    public void serveAsset(String asset) {
        this.flushHeaders();

        MainController.getInstance().println("Serving asset " + asset);

        int buffer_read_n_bytes = 0;
        byte[] buffer = new byte[512];

        try {
            InputStream file_input = ((Context) MainController.getInstance().getContext()).getResources().getAssets().open(asset);

            try {

                while ((buffer_read_n_bytes = file_input.read(buffer)) != -1) { // While
                    // reading
                    // file
                    this.out.write(buffer, 0, buffer_read_n_bytes); // Writing
                    // to buffer
                    this.out.flush(); // Flushing the buffer
                    Statistics.addBytesSend(buffer_read_n_bytes);
                }
                this.out.flush(); // Flushing remaining buffer

            } catch (IOException e) {
            }

            try {
                file_input.close();
            } // Closing file input stream
            catch (IOException e) {
                android.util.Log.i("HTTP", e.getMessage());
            }
        } catch (Exception e) {
            android.util.Log.i("HTTP", e.getMessage());
        }
    }

    // /**
    // * Flushes headers and serves the specified file
    // *
    // * @param file file to be served
    // */
    // public void serveFile(File file)
    // {
    // this.flushHeaders();
    //
    // String filename = file.getAbsolutePath();
    //
    // FileInputStream file_input = null;
    // int buffer_read_n_bytes = 0;
    // byte[] buffer = new byte[8192]; //8192 4096 2048
    //
    //
    // try {
    // byte[] buffer2 = WebServer.jmemcache.get(filename);
    //
    //
    // this.out.write(buffer2, 0, buffer2.length);
    // this.out.flush();
    // System.out.println("From cache");
    // Statistics.addBytesSend(buffer2.length);
    // return;
    // }
    // catch(ro.polak.jmemcache.NoSuchKeyException e)
    // {
    // //e.printStackTrace();
    // }
    // catch(Exception e)
    // {}
    //
    // int index = WebServer.jmemcache.reserveKey( filename);
    //
    //
    // try {
    //
    // file_input = new FileInputStream(file);
    //
    //
    // try {
    //
    // while( true ) { // While reading from file
    //
    //
    // if( (buffer_read_n_bytes = file_input.read(buffer)) == -1) break;
    //
    //
    // WebServer.jmemcache.append(index,buffer, 0, buffer_read_n_bytes);
    //
    //
    // this.out.write(buffer, 0, buffer_read_n_bytes);
    // this.out.flush();
    // Statistics.addBytesSend(buffer_read_n_bytes);
    // }
    // this.out.flush(); // Flushing remaining buffer
    //
    // } catch(IOException e) {}
    //
    //
    // try { file_input.close(); } // Closing file input stream
    // catch(IOException e) {}
    //
    // } catch(FileNotFoundException e) {}
    //
    // }

    /**
     * Sets content type
     *
     * @param contentType content type
     */
    public void setContentType(String contentType) {
        this.headers.setContentType(contentType);
    }

    /**
     * Sets response header
     *
     * @param headerName  name of the header
     * @param headerValue value of the header
     */
    public void setHeader(String headerName, String headerValue) {
        this.headers.setHeader(headerName, headerValue);
    }

    /**
     * Sets keepAlive
     *
     * @param keepAlive true for keep alive connection
     */
    public void setKeepAlive(boolean keepAlive) {
        this.headers.setKeepAlive(keepAlive);
    }

    /**
     * Sets content length
     *
     * @param length length of content
     */
    public void setContentLength(int length) {
        this.headers.setContentLength(length);
    }

    /**
     * Sets content length
     *
     * @param length length of content
     */
    public void setContentLength(long length) {
        this.headers.setContentLength(length);
    }

    public HTTPResponseHeaders getHeaders() {
        return headers;
    }

    /**
     * Sets status of response
     *
     * @param status status code and message
     */
    public void setStatus(String status) {
        this.headers.setStatus(status);
    }

    /**
     * Closes the socket
     */
    public void close() throws IOException {
        this.out.close();
        this.out = null;
        this.printWriter = null;
        this.socket.close();
    }

    /**
     * Returns request's print writer
     *
     * @return request's print writer
     */
    public PrintWriter getPrintWriter() {
        if (this.printWriter == null) {
            this.printWriter = new PrintWriter();
        }

        return printWriter;
    }
}