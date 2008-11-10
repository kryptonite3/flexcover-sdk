package com.allurent.coverage.runtime
{
    /**
     * This class is simply a patch point for plugging in a custom ICoverageAgent
     * to handle the actual coverage recording.
     */
    public class CoverageManager
    {
        public static var agent:ICoverageAgent = new LocalConnectionCoverageAgent();

        /**
         * Exit the application after all data has been flushed out of the agent.
         */
        public static function exit():void
        {
            agent.exit();
        }
    }
}
