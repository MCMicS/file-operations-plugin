package sp.sd.fileoperations;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;
import org.jenkinsci.remoting.RoleSensitive;
import java.io.Serializable;

public class FileCopyOperation extends FileOperation implements Serializable { 
	private final String includes;
	private final String excludes;
	private final String targetLocation;
	private boolean flattenFiles = false;
	@DataBoundConstructor 
	 public FileCopyOperation(String includes, String excludes, String targetLocation, boolean flattenFiles) { 
		this.includes = includes;
		this.excludes = excludes;
		this.targetLocation = targetLocation;
		this.flattenFiles = flattenFiles;
	 }

	 public String getIncludes()
	 {
		 return includes;
	 }
	 public String getExcludes()
	 {
		 return excludes;
	 }
	 public String getTargetLocation()
	 {
		 return targetLocation;
	 }
	 public boolean getFlattenFiles()
	 {
		 return flattenFiles;
	 }
	 public boolean RunOperation(AbstractBuild build, Launcher launcher, BuildListener listener) {
		 boolean result = false;
		 try
			{
				FilePath ws = build.getWorkspace(); 				
				
				try {	
					result = ws.act(new TargetFileCallable(listener, build.getEnvironment(listener).expand(includes), build.getEnvironment(listener).expand(excludes), build.getEnvironment(listener).expand(targetLocation), flattenFiles));				
				}
				catch (Exception e) {
					e.printStackTrace(listener.getLogger());
					return false;
				}
				
			}
			catch(Exception e)
			{
				e.printStackTrace(listener.getLogger());
			}	
			return result;
		} 
 
	private static final class TargetFileCallable implements FileCallable<Boolean> {
		private static final long serialVersionUID = 1;
		private final BuildListener listener;
		private final String resolvedIncludes;
		private final String resolvedExcludes;
		private final String resolvedTargetLocation;
		private boolean iflattenFiles = false;
		public TargetFileCallable(BuildListener Listener, String ResolvedIncludes, String ResolvedExcludes, String ResolvedTargetLocation, boolean flattenFiles) {
			this.listener = Listener;
			this.resolvedIncludes = ResolvedIncludes;	
			this.resolvedExcludes = ResolvedExcludes;
			this.resolvedTargetLocation = ResolvedTargetLocation;
			this.iflattenFiles = flattenFiles;
		}
		@Override public Boolean invoke(File ws, VirtualChannel channel) {
			boolean result = false;
			try {
				FilePath fpWS = new FilePath(ws);
				FilePath fpTL = new FilePath(fpWS, resolvedTargetLocation);
				FilePath[] resolvedFiles = fpWS.list(resolvedIncludes, resolvedExcludes);
				if(resolvedFiles.length == 0)
				{
					listener.getLogger().println("0 files found for include pattern '" + resolvedIncludes + "' and exclude pattern '" + resolvedExcludes +"'");
					result = true;
				}
				else
				{
					for(FilePath item : resolvedFiles)
					{
						listener.getLogger().println(item.getRemote());
					}					
				}
				if(iflattenFiles)
				{
					for(FilePath item : resolvedFiles)
					{
						item.copyTo(new FilePath(fpTL,item.getName()));
					}
					result = true;
				}
				else
				{
					fpWS.copyRecursiveTo(resolvedIncludes, resolvedExcludes, fpTL);
					result = true;
				}
			}
			catch(RuntimeException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				result = false;
			}
			return result;	
		}
		
		@Override  public void checkRoles(RoleChecker checker) throws SecurityException {
                
		}		
	}
 @Extension public static class DescriptorImpl extends FileOperationDescriptor {
 public String getDisplayName() { return "File Copy"; }

 }
}