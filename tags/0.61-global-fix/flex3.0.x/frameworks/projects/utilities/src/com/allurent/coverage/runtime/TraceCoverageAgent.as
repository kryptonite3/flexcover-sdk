package com.allurent.coverage.runtime
{
    /**
     * This class provides trace-log-based coverage recording support
     * for an instrumented application.
     */
    public class TraceCoverageAgent extends AbstractCoverageAgent
    {
        public static const COVERAGE_TRACE_PREFIX:String = "__coverage__";

        /**
         * Send a map of coverage keys and execution counts to this agent's destination.
         * @param map an Object whose keys are coverage elements and values are execution counts.
         */
        override public function sendCoverageMap(map:Object):void
        {
            for (var key:String in map)
            {
                trace(COVERAGE_TRACE_PREFIX, key, map[key]);
            }
        }
    }
}
