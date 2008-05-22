package com.allurent.coverage.runtime
{
    import flash.events.TimerEvent;
    import flash.system.fscommand;
    import flash.utils.Timer;
    import flash.utils.getDefinitionByName;
    
    /**
     * This class provides overall coverage recording support for an instrumented application.
     * It is a base class, designed to be extended.
     */
    public class AbstractCoverageAgent implements ICoverageAgent
    {
        /**
         * Time in milliseconds between flushes of coverage data to its destination.
         * Shortening this interval increases the overall volume of data considerably.
         */
        public var flushDelay:uint = 1000;

        // flag indicating that connection is known to be non-functioning,
        // should fall back to trace logging or nothing at all.
        public var broken:Boolean = false;

        // Accumulated coverage information since the last flush.  Keys
        // are coverage keys, and values are execution counts.
        private var coverageMap:Object = {};

        // flag indicating attempted setup of connection
        private var initialized:Boolean = false;

        // "escrow" list of coverage maps that were not successfully written
        // to the LocalConnection and may need to be written to the tracelog as a fallback.
        protected var escrow:Array = [];

        // flag indicating that a call to exit() was made.
        private var stopped:Boolean = false;
        
        // Timer instance used to drive periodic flushing of coverage data
        private var flushTimer:Timer = createFlushTimer(); 
        
        /**
         * Record the execution of a single coverage key; called by
         * the global coverage() function.
         */
        public function recordCoverage(key:String):void
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
        public function flushCoverageData():void
        {
            // Attempt to initialize the local connection if not yet set up.
            //
            if (!initialized)
            {
                initializeAgent();
                initialized = true;
            }
            
            try
            {
                // First push our coverageMap on the escrow list, since we might need
                // to try to log it to the trace log later if this write fails.
                escrow.push(coverageMap);

                if (!broken)
                {
                    sendCoverageMap(coverageMap);
                }
            }
            catch (e:Error)
            {
                // In the case of a runtime error, blow off the connection and trace.
                //
                trace(e.message);
                broken = true;
            }

            coverageMap = {};
        }
        
        /**
         * Request the application to exit after all pending data has been written.
         * This call also signals a remote receiver that it can process remaining data
         * and itself exit.
         */
        public function exit():void
        {
            flushCoverageData();
            if (!broken)
            {
                requestExit();
            }
            stopped = true;
            checkForExit();
        }

        /**
         * Check to see if we are able  
         * 
         */
        protected function checkForExit():void
        {
            // If we've stopped collecting data, and there are either no more operations
            // pending or the agent is broken, exit.
            //
            if (stopped && ((!operationsPending) || broken))
            {
                var nativeApp:Object = null;

                try {
                    // AIR quit routine.
                    nativeApp = getDefinitionByName("flash.desktop.NativeApplication");
                    if (nativeApp != null)
                    {
                        nativeApp.nativeApplication.exit();
                        return;    // probably unnecessary!
                    }
                }
                catch (e:Error)
                {
                }

                // Flash Player quit routine
                fscommand("quit", "");
            }
        }
        
        /////////////////////
        // ABSTRACT METHODS
        /////////////////////

        /**
         * Flush all outstanding coverage data to this agent's destination.
         */
        public function initializeAgent():void
        {
        }

        /**
         * Send a map of coverage keys and execution counts to this agent's destination.
         * @param map an Object whose keys are coverage elements and values are execution counts.
         */
        public function sendCoverageMap(map:Object):void
        {
        }
        
        /**
         * Request the destination of this agent to terminate its execution. 
         */
        public function requestExit():void
        {
        }

        /**
         * Obtain a flag indicating whether there are any outstanding send or exit operations. 
         */
        public function get operationsPending():Boolean
        {
            return false;
        }

        ////////////////////
        // PRIVATE METHODS
        ////////////////////
        
        private function handleFlushTimer(e:TimerEvent):void
        {
            flushCoverageData();
        }
        
        private function createFlushTimer():Timer
        {
            var t:Timer = new Timer(flushDelay, 0);
            t.addEventListener(TimerEvent.TIMER, handleFlushTimer);
            t.start();
            return t;
        }
    }
}
