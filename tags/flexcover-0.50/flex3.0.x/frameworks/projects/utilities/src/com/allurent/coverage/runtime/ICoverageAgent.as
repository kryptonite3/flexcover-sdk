package com.allurent.coverage.runtime
{
    /**
     * This interface abstracts coverage recording support for an instrumented application.
     */
    public interface ICoverageAgent
    {
        /**
         * Record the execution of a single coverage key; called by
         * the global coverage() function.
         * @param key the coverage key whose execution is to be recorded.
         */
        function recordCoverage(key:String):void;
        
        /**
         * Flush all outstanding coverage data to the LocalConnection.
         */
        function flushCoverageData():void;
        
        /**
         * Request the application to exit after all pending data has been written.
         * This call also signals a remote receiver that it can process remaining data
         * and itself exit.
         */
        function exit():void;
    }
}
