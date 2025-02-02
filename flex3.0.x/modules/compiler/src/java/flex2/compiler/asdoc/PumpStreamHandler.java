/*
 * Copyright  2000-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package flex2.compiler.asdoc;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Copies standard output and error of subprocesses to standard output and
 * error of the parent process.
 *
 * @since Ant 1.2
 */
public class PumpStreamHandler {

    private Thread outputThread;
    private Thread errorThread;
    private StreamPumper inputPump;

    private OutputStream out;
    private OutputStream err;
    private InputStream input;

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     * @param out the output <CODE>OutputStream</CODE>.
     * @param err the error <CODE>OutputStream</CODE>.
     * @param input the input <CODE>InputStream</CODE>.
     */
    public PumpStreamHandler(OutputStream out, OutputStream err,
                             InputStream input) {
        this.out = out;
        this.err = err;
        this.input = input;
    }

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     * @param out the output <CODE>OutputStream</CODE>.
     * @param err the error <CODE>OutputStream</CODE>.
     */
    public PumpStreamHandler(OutputStream out, OutputStream err) {
        this(out, err, null);
    }

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     * @param outAndErr the output/error <CODE>OutputStream</CODE>.
     */
    public PumpStreamHandler(OutputStream outAndErr) {
        this(outAndErr, outAndErr);
    }

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     */
    public PumpStreamHandler() {
        this(System.out, System.err);
    }

    /**
     * Set the <CODE>InputStream</CODE> from which to read the
     * standard output of the process.
     * @param is the <CODE>InputStream</CODE>.
     */
    public void setProcessOutputStream(InputStream is) {
        createProcessOutputPump(is, out);
    }

    /**
     * Set the <CODE>InputStream</CODE> from which to read the
     * standard error of the process.
     * @param is the <CODE>InputStream</CODE>.
     */
    public void setProcessErrorStream(InputStream is) {
        if (err != null) {
            createProcessErrorPump(is, err);
        }
    }

    /**
     * Set the <CODE>OutputStream</CODE> by means of which
     * input can be sent to the process.
     * @param os the <CODE>OutputStream</CODE>.
     */
    public void setProcessInputStream(OutputStream os) {
        if (input != null) {
            inputPump = createInputPump(input, os, true);
        } else {
            try {
                os.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    /**
     * Start the <CODE>Thread</CODE>s.
     */
    public void start() {
        outputThread.start();
        errorThread.start();
        if (inputPump != null) {
            Thread inputThread = new Thread(inputPump);
            inputThread.setDaemon(true);
            inputThread.start();
        }
    }

    /**
     * Stop pumping the streams.
     */
    public void stop() {
        try {
            outputThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
        try {
            errorThread.join();
        } catch (InterruptedException e) {
            // ignore
        }

        if (inputPump != null) {
            inputPump.stop();
        }

        try {
            err.flush();
        } catch (IOException e) {
            // ignore
        }
        try {
            out.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Get the error stream.
     * @return <CODE>OutputStream</CODE>.
     */
    protected OutputStream getErr() {
        return err;
    }

    /**
     * Get the output stream.
     * @return <CODE>OutputStream</CODE>.
     */
    protected OutputStream getOut() {
        return out;
    }

    /**
     * Create the pump to handle process output.
     * @param is the <CODE>InputStream</CODE>.
     * @param os the <CODE>OutputStream</CODE>.
     */
    protected void createProcessOutputPump(InputStream is, OutputStream os) {
        outputThread = createPump(is, os);
    }

    /**
     * Create the pump to handle error output.
     * @param is the <CODE>InputStream</CODE>.
     * @param os the <CODE>OutputStream</CODE>.
     */
    protected void createProcessErrorPump(InputStream is, OutputStream os) {
        errorThread = createPump(is, os);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     */
    protected Thread createPump(InputStream is, OutputStream os) {
        return createPump(is, os, false);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     */
    protected Thread createPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted) {
        final Thread result
            = new Thread(new StreamPumper(is, os, closeWhenExhausted));
        result.setDaemon(true);
        return result;
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream. Used for standard input.
     * @since Ant 1.6.3
     */
    /*protected*/ StreamPumper createInputPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted) {
        StreamPumper pumper = new StreamPumper(is, os, closeWhenExhausted);
        pumper.setAutoflush(true);
        return pumper;
    }

	/**
	 * Copies all data from an input stream to an output stream.
	 *
	 * @since Ant 1.2
	 */
	public class StreamPumper implements Runnable {

	    private static final int SIZE = 128;
	    private InputStream is;
	    private OutputStream os;
	    private boolean finished;
	    private boolean closeWhenExhausted;
	    private boolean autoflush = false;

	    /**
	     * Create a new stream pumper.
	     *
	     * @param is input stream to read data from
	     * @param os output stream to write data to.
	     * @param closeWhenExhausted if true, the output stream will be closed when
	     *        the input is exhausted.
	     */
	    public StreamPumper(InputStream is, OutputStream os,
	                        boolean closeWhenExhausted) {
	        this.is = is;
	        this.os = os;
	        this.closeWhenExhausted = closeWhenExhausted;
	    }

	    /**
	     * Create a new stream pumper.
	     *
	     * @param is input stream to read data from
	     * @param os output stream to write data to.
	     */
	    public StreamPumper(InputStream is, OutputStream os) {
	        this(is, os, false);
	    }

	    /**
	     * Set whether data should be flushed through to the output stream.
	     * @param autoflush if true, push through data; if false, let it be buffered
	     * @since Ant 1.6.3
	     */
	    /*public*/ void setAutoflush(boolean autoflush) {
	        this.autoflush = autoflush;
	    }

	    /**
	     * Copies data from the input stream to the output stream.
	     *
	     * Terminates as soon as the input stream is closed or an error occurs.
	     */
	    public void run() {
	        synchronized (this) {
	            // Just in case this object is reused in the future
	            finished = false;
	        }

	        final byte[] buf = new byte[SIZE];

	        int length;
	        try {
	            while ((length = is.read(buf)) > 0 && !finished) {
	                os.write(buf, 0, length);
	                if (autoflush) {
	                    os.flush();
	                }
	            }
	        } catch (Exception e) {
	            // ignore errors
	        } finally {
	            if (closeWhenExhausted) {
	                try {
	                    os.close();
	                } catch (IOException e) {
	                    // ignore
	                }
	            }
	            synchronized (this) {
	                finished = true;
	                notifyAll();
	            }
	        }
	    }

	    /**
	     * Tells whether the end of the stream has been reached.
	     * @return true is the stream has been exhausted.
	     **/
	    public synchronized boolean isFinished() {
	        return finished;
	    }

	    /**
	     * This method blocks until the stream pumper finishes.
	     * @see #isFinished()
	     **/
	    public synchronized void waitFor()
	        throws InterruptedException {
	        while (!isFinished()) {
	            wait();
	        }
	    }

	    /**
	     * Stop the pumper as soon as possible.
	     * Note that it may continue to block on the input stream
	     * but it will really stop the thread as soon as it gets EOF
	     * or any byte, and it will be marked as finished.
	     * @since Ant 1.6.3
	     */
	    /*public*/ synchronized void stop() {
	        finished = true;
	        notifyAll();
	    }

	}
}