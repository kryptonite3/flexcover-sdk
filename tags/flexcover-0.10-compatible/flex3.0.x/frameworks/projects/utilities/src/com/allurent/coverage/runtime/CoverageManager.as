package com.allurent.coverage.runtime
{
    import flash.events.AsyncErrorEvent;
    import flash.events.ErrorEvent;
    import flash.events.SecurityErrorEvent;
    import flash.events.StatusEvent;
    import flash.events.TimerEvent;
    import flash.net.LocalConnection;
    import flash.system.fscommand;
    import flash.utils.Timer;
    import flash.utils.getDefinitionByName;
    
    /**
     * This class provides overall coverage recording support for an instrumented application.
     */
    public class CoverageManager
    {
        public static const COVERAGE_TRACE_PREFIX:String = "__coverageData__ ";

        /**
         * LocalConnection name to be used by coverage recording.  Must be modified
         * prior to first coverage output.
         */
        public static var connectionName:String = "_flexcover";

        /**
         * Time in milliseconds between flushes of coverage data to its destination.
         * Shortening this interval increases the overall volume of data considerably.
         */
        public static var flushDelay:uint = 1000;

        /**
         * Flag enabling trace logging of coverage data; default value is false
         * to avoid slowing down apps that are not connected to any data capture.
         */
        public static var traceEnabled:Boolean = false;

        // Accumulated coverage information since the last flush.  Keys
        // are coverage keys, and values are execution counts.
        private static var coverageMap:Object = {};

        // LocalConnection used for writing coverage data
        private static var connection:LocalConnection;

        // flag indicating attempted setup of connection
        private static var connectionSetUp:Boolean = false;

        // flag indicating that connection is known to be non-functioning,
        // should fall back to trace logging or nothing at all.
        private static var connectionBroken:Boolean = false;

        // counter of pending writes to local connection, important
        // in exiting the application only after all coverage data has been written
        private static var pendingWrites:int = 0;

        // "escrow" list of coverage maps that were not successfully written
        // to the LocalConnection and may need to be written to the tracelog as a fallback.
        private static var escrow:Array = [];

        // flag indicating that a call to exit() was made.
        private static var stopped:Boolean = false;
        
        // Timer instance used to drive periodic flushing of coverage data
        private static var flushTimer:Timer = createFlushTimer(); 
        
        // handler function names on client end of LocalConnection
        private static const DATA_HANDLER:String = "coverageData";
        private static const EXIT_HANDLER:String = "coverageEnd";

        // Maximum total key length allowed before forced LC send packet
        private static const MAX_SEND_LENGTH:uint = 10000;
        
        /**
         * Record the execution of a single coverage key; called by
         * the global coverage() function.
         */
        public static function recordCoverage(key:String):void
        {        
            if (isNaN(coverageMap[key]++))
            {
                // The map must not have contained this key yet, so enter an
                // execution count of 1.  Subsequent calls will autoincrement without
                // returning NaN.
                //
                coverageMap[key] = 1;
            }
        }
        
        /**
         * Flush all outstanding coverage data to the LocalConnection.
         */
        public static function flushCoverageData():void
        {
            // Attempt to initialize the local connection if not yet set up.
            //
            if (!connectionSetUp)
            {
                connection = new LocalConnection();
                connection.addEventListener(AsyncErrorEvent.ASYNC_ERROR, handleError);
                connection.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
                connection.addEventListener(StatusEvent.STATUS, handleStatus);
                connectionSetUp = true;
            }
            try
            {
                // First push our coverageMap on the escrow list, since we might need
                // to try to log it to the trace log later if this write fails.
                escrow.push(coverageMap);

                if (connectionBroken)
                {
                    // Our connection is known to be trashed, so trace the coverage
                    // information or toss it as per the traceEnabled flag.
                    //
                    traceCoverage();
                }
                else
                {
                    // Repeatedly accumulate MAX_SEND_LENGTH worth of coverage key/value
                    // pairs in tempMap, sending over the LocalConnection as this limit
                    // is reached.  This is a crude attempt to avoid exceeding the data size limit
                    // inherent in LocalConnection.
                    //
                    var i:uint = 0;
                    var tempMap:Object = {};
                    for (var key:String in coverageMap)
                    {
                        tempMap[key] = coverageMap[key];
                        if ((i += key.length) > MAX_SEND_LENGTH)
                        {
                            connection.send(connectionName, DATA_HANDLER, tempMap);
                            pendingWrites++;
                            tempMap = {};
                            i = 0;
                        }
                    }

                    // We might have some keys left over that didn't hit the max limit.
                    //
                    connection.send(connectionName, DATA_HANDLER, tempMap);
                    pendingWrites++;
                }
            }
            catch (e:Error)
            {
                // In the case of a runtime error, blow off the connection and trace.
                //
                trace(e.message);
                connectionBroken = true;
                traceCoverage();
            }

            coverageMap = {};
        }
        
        /**
         * Request the application to exit after all pending data has been written.
         * This call also signals a remote receiver that it can process remaining data
         * and itself exit.
         */
        public static function exit():void
        {
            flushCoverageData();
            if (!connectionBroken)
            {
                connection.send(connectionName, EXIT_HANDLER);
                pendingWrites++;
            }
            stopped = true;
            checkForExit();
        }

        private static function handleError(e:ErrorEvent):void
        {
            trace(e.text);
        }
        
        private static function handleStatus(e:StatusEvent):void
        {
            if (e.level == "error")
            {
                // Something went awry.
                // TODO: This may write more data to the log than was actually lost,
                //    but the assumption for now is that the LC either works all the time or
                //    fails all the time.
                //
                connectionBroken = true;
                traceCoverage();
            }
            else if (e.level == "status")
            {
                // guess we were able to send OK
                escrow = [];
                pendingWrites--;
            }
            checkForExit();
        }
        
        private static function checkForExit():void
        {
            if ((pendingWrites == 0 || connectionBroken) && stopped)
            {
                var nativeApp:Object = getDefinitionByName("flash.desktop.NativeApplication");
                if (nativeApp != null)
                {
                    nativeApp.nativeApplication.exit();
                }
                else
                {
                    fscommand("quit", "");
                }
            }
        }
        
        private static function handleFlushTimer(e:TimerEvent):void
        {
            flushCoverageData();
        }
        
        private static function createFlushTimer():Timer
        {
            var t:Timer = new Timer(flushDelay, 0);
            t.addEventListener(TimerEvent.TIMER, handleFlushTimer);
            t.start();
            return t;
        }
        
        private static function traceCoverage():void
        {
            if (traceEnabled)
            {
                for each (var map:Object in escrow)
                {
                    for (var key:String in map)
                    {
                        trace(COVERAGE_TRACE_PREFIX + key + " " + map[key]);
                    }
                }
            }
            escrow = [];
        }
    }
}
