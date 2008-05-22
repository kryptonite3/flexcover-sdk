package
{
    import com.allurent.coverage.runtime.CoverageManager;
    
    public function coverage(key:String):void
    {
        CoverageManager.agent.recordCoverage(key);
    }
}
