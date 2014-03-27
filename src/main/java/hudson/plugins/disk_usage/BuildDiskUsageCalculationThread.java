package hudson.plugins.disk_usage;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AperiodicWork;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import jenkins.model.Jenkins;

/**
 * A Thread responsible for gathering disk usage information
 * 
 * @author dvrzalik
 */
@Extension
public class BuildDiskUsageCalculationThread extends DiskUsageCalculation {   
    
    //last scheduled task;
    private static DiskUsageCalculation currentTask;
    
    private static boolean executing;
      
    public BuildDiskUsageCalculationThread(){        
        super("Calculation of builds disk usage"); 
    }   
    
    @Override
    public void execute(TaskListener listener) throws IOException, InterruptedException { 
        DiskUsagePlugin plugin = Jenkins.getInstance().getPlugin(DiskUsagePlugin.class);
        if(plugin.getConfiguration().isCalculationBuildsEnabled()  && !isExecuting()){
            executing = true;
            try{
                List<Item> items = new ArrayList<Item>();
                ItemGroup<? extends Item> itemGroup = Jenkins.getInstance();
                items.addAll(DiskUsageUtil.getAllProjects(itemGroup));

                for (Object item : items) {
                    if (item instanceof AbstractProject) {
                        AbstractProject project = (AbstractProject) item;
                        if (!project.isBuilding()) {

                            List<AbstractBuild> builds = project.getBuilds();
                            for(AbstractBuild build : builds){  
                                try{
                                    DiskUsageUtil.calculateDiskUsageForBuild(build);  
                                }
                                catch(Exception e){
                                    logger.log(Level.WARNING, "Error when recording disk usage for " + project.getName(), e);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error when recording disk usage for builds", ex);
            }
            executing=false;
        }
    }
    
    public CronTab getCronTab() throws ANTLRException{
        String cron = Jenkins.getInstance().getPlugin(DiskUsagePlugin.class).getConfiguration().getCountIntervalForBuilds();
        CronTab tab = new CronTab(cron);
        return tab;
    }   

    @Override
    public AperiodicWork getNewInstance() {   
        if(currentTask!=null){
            currentTask.cancel();
        }
        else{
            cancel();
        }
        currentTask = new BuildDiskUsageCalculationThread();
        return currentTask;
    }

    @Override
    public DiskUsageCalculation getLastTask() {
        return currentTask;
    }

    @Override
    public boolean isExecuting() {
        return executing;
    }
    
}
