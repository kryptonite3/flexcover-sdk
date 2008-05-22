package com.allurent.coverage.runtime
{
    import flash.events.AsyncErrorEvent;
    import flash.events.ErrorEvent;
    import flash.events.SecurityErrorEvent;
    import flash.events.StatusEvent;
    import flash.net.LocalConnection;
    
    /**
     * This class provides overall coverage recording support for an instrumented application
     * using a LocalConnection and its built-in serialization support for objects.
     */
    public class LocalConnectionCoverageAgent extends AbstractCoverageAgent
    {
        /**
         * LocalConnection name to be used by coverage recording.  Must be modified
         * prior to first coverage output.
         */
        public var connectionName:String;

        // LocalConnection used for writing coverage data
        private var connection:LocalConnection;

        // counter of pending writes to local connection, important
        // in exiting the application only after all coverage data has been written
        private var pendingWrites:int = 0;

        // default connection name
        public static const DEFAULT_CONNECTION_NAME:String = "_flexcover";
        
        // handler function names on client end of LocalConnection
        private static const DATA_HANDLER:String = "coverageData";
        private static const EXIT_HANDLER:String = "coverageEnd";

        // Maximum total key length allowed before forced LC send packet
        private static const MAX_SEND_LENGTH:uint = 10000;
        
        /**
         * Create a LocalConnectionCoverageAgent.
         * 
         * @param connectionName the name of the LocalConnection to be used.
         * 
         */
        public function LocalConnectionCoverageAgent(connectionName:String = null)
        {
            this.connectionName = (connectionName != null)
                ? connectionName
                : DEFAULT_CONNECTION_NAME;
        }
        
        /**
         * Flush all outstanding coverage data to this agent's destination.
         */
        override public function initializeAgent():void
        {
            connection = new LocalConnection();
            connection.addEventListener(AsyncErrorEvent.ASYNC_ERROR, handleConnectionError);
            connection.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleConnectionError);
            connection.addEventListener(StatusEvent.STATUS, handleConnectionStatus);
        }

        /**
         * Send a map of coverage keys and execution counts to this agent's destination.
         * @param map an Object whose keys are coverage elements and values are execution counts.
         */
        override public function sendCoverageMap(map:Object):void
        {
            // Repeatedly accumulate MAX_SEND_LENGTH worth of coverage key/value
            // pairs in tempMap, sending over the LocalConnection as this limit
            // is reached.  This is a crude attempt to avoid exceeding the data size limit
            // inherent in LocalConnection.
            //
            var i:uint = 0;
            var tempMap:Object = {};
            for (var key:String in map)
            {
                tempMap[key] = map[key];
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
        
        /**
         * Request the destination of this agent to terminate its execution. 
         */
        override public function requestExit():void
        {
            connection.send(connectionName, EXIT_HANDLER);
            pendingWrites++;
        }

        /**
         * Obtain a flag indicating whether there are any outstanding send or exit operations. 
         */
        override public function get operationsPending():Boolean
        {
            return (pendingWrites > 0);
        }

        private function handleConnectionError(e:ErrorEvent):void
        {
            trace(e.text);
        }
        
        private function handleConnectionStatus(e:StatusEvent):void
        {
            if (e.level == "error")
            {
                // Something went awry.
                // TODO: This may write more data to the log than was actually lost,
                //    but the assumption for now is that the LC either works all the time or
                //    fails all the time.
                //
                broken = true;
            }
            else if (e.level == "status")
            {
                // guess we were able to send OK
                escrow = [];
                pendingWrites--;
            }
            checkForExit();
        }
    }
}
